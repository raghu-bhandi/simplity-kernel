/*
 * Copyright (c) 2016 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.test;

import java.util.Date;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.service.ServiceProtocol;

/**
 * Represents one test case for a service with one specific input and expected
 * output
 *
 * @author simplity.org
 *
 */
public class TestCase {
	/**
	 * unique name given to this test case.
	 */
	String testCaseName;

	/**
	 * service to be tested
	 */
	String serviceName;

	/**
	 * description for documentation
	 */
	String description;

	/**
	 * in case you do not want to specify individual field values but provide a
	 * ready string that is to be provided as request pay-load to the service.
	 * If this is specified, fields ad sheets are not relevant and are ignored
	 */
	String inputJson;
	/**
	 * you can override elements of the JSON or add to it using fields at any
	 * level
	 */
	InputField[] inputFields;

	/**
	 * Specify a qualified attribute to be used to identify a specific item in
	 * the JSON. Fields inside this item are relative to this item
	 */
	InputItem[] inputItems;

	/**
	 * number of errors expected. non-zero implies that we expect this request
	 * to fail, and hence output data expectations are irrelevant.
	 */
	boolean testForFailure;
	/*
	 * output meant for assertions
	 */

	/**
	 * if you want to specify exactly the fields/sheets expected as in the
	 * pay-load format(json). We compare the json elements, and not just a
	 * string comparison to take care of white-space and sequence/order issues
	 */
	String outputJson;
	/**
	 * assertion on fields (with primitive values)
	 */
	OutputField[] outputFields;
	/**
	 * assertions on arrays/lists
	 */
	OutputList[] outputLists;

	/**
	 * assertions on items/objects (which in turn contain fields/lists)
	 */
	OutputItem[] outputItems;

	/**
	 * fields that are used by subsequent test plans
	 */
	ContextField[] fieldsToBeAddedToContext;

	/**
	 * Use java to inspect the result and assert. fully qualified class name
	 * that implements org.simplity.test.Inspector
	 */
	String testClassName;

	/**
	 * how many assertions are you making in this test case? This is meant for reporting purposes
	 */
	int nbrAssertions = 1;
	/**
	 * run this test and report result to the context
	 *
	 * @param ctx
	 * @return error message in case this test fails. null if all OK.
	 */
	public String run(TestContext ctx) {
		String json = this.getInput(ctx);
		Tracer.trace("Input Json : " + json);
		long startedAt = new Date().getTime();
		String msg = null;
		try {
			json = ctx.runService(this.serviceName, json);
			msg = this.assertOutput(json, ctx);
		} catch (Exception e) {
			msg = "Service or serviceTest has a fatal error : "
					+ e.getMessage();
			Tracer.trace(e,
					this.serviceName + " raised fatal error during testing.");
		}
		Tracer.trace("Output JSON : " + json);
		int millis = (int) (new Date().getTime() - startedAt);
		TestResult result = new TestResult(this.serviceName, this.testCaseName,
				millis, msg);
		ctx.addResult(result);
		return msg;

	}

	/**
	 * we carry out all asserts in the output specification. We come out with
	 * the first failure.
	 *
	 * @param output
	 *            json that is returned from service. This is either an array of
	 *            messages or a response json
	 * @return null if it compares well. Error message in case of any trouble
	 */
	String assertOutput(String output, TestContext ctx) {
		JSONObject json = new JSONObject(output);
		boolean succeeded = this.serviceSucceeded(json);
		/*
		 * service succeeded.
		 */
		if (succeeded == false && this.testForFailure == false) {
			return "Service failed while we expected it to succeed.";
		}
		if (this.fieldsToBeAddedToContext != null) {
			for (ContextField field : this.fieldsToBeAddedToContext) {
				field.addToContext(json, ctx);
			}
		}

		if (succeeded && this.testForFailure) {
			return "Service succeeded while we expected it to fail.";
		}

		/*
		 * we are aware that we could be reaching here even after the service
		 * has failed. But if that is what the tester wants us to do...
		 */
		/*
		 * are we expecting a specific json?
		 */
		if (this.outputJson != null) {
			JSONObject expected = new JSONObject(this.outputJson);
			/*
			 * we assert expected attributes, but not bother if there are
			 * others.
			 */
			String msg = expected.agreesWith(json);
			if(msg != null){
				return msg;
			}
		}

		/*
		 * what fields are we expecting?
		 */
		if (this.outputFields != null) {
			for (OutputField field : this.outputFields) {
				String resp = field.match(json, ctx);
				if (resp != null) {
					return resp;
				}
			}
		}

		/*
		 * items
		 */
		if (this.outputItems != null) {
			for (OutputItem item : this.outputItems) {
				String resp = item.match(json, ctx);
				if (resp != null) {
					return resp;
				}
			}
		}

		if (this.outputLists != null) {
			for (OutputList list : this.outputLists) {
				String resp = list.match(json, ctx);
				if (resp != null) {
					return resp;
				}
			}
		}

		/*
		 * last one. Is there a java code?
		 */
		if (this.testClassName != null) {
			String msg = null;
			try {
				Class<?> cls = Class.forName(this.testClassName);
				Object obj = cls.newInstance();
				if (obj instanceof Inspector == false) {
					msg = "Error in test case : " + this.testClassName
							+ " is not an instance of Inspector.";
				} else {
					msg = ((Inspector) obj).test(json, ctx);
				}
			} catch (Exception e) {
				msg = "Error in test case while using class "
						+ this.testClassName + " : " + e.getMessage();
			}
			if (msg != null) {
				return msg;
			}
		}
		/*
		 * fantastic We crossed all hurdles!!!
		 */
		return null;
	}

	/**
	 * @param json
	 * @return
	 */
	private boolean serviceSucceeded(JSONObject json) {
		Object obj = json.opt(ServiceProtocol.REQUEST_STATUS);
		if (obj != null) {
			return ServiceProtocol.STATUS_OK.equals(obj);
		}
		/*
		 * since there is no status indicator, we infer it based on messages
		 */

		obj = json.opt(ServiceProtocol.MESSAGES);
		if (obj == null || obj instanceof JSONArray == false) {
			return true;
		}
		JSONArray msgs = (JSONArray) obj;
		int nbrMsgs = msgs.length();
		for (int i = 0; i < nbrMsgs; i++) {
			obj = msgs.opt(i);
			if (obj instanceof JSONObject) {
				if ("error".equals(((JSONObject) obj).optString("messageType"))) {
					return false;
				}
			}
		}
		return true;

	}

	/**
	 * validate this components and report any errors
	 *
	 * @param vtx
	 * @return number of errors detected
	 */
	@SuppressWarnings("unused")
	public int validate(ValidationContext vtx) {
		int nbr = 0;
		if (this.inputJson != null) {
			try {
				/*
				 * reason for us to put suppressWarning annotation
				 */
				new JSONObject(this.inputJson);
			} catch (Exception e) {
				vtx.addError(
						"inputPayload is not a valid json\n" + this.inputJson);
				nbr++;
			}
		}
		if (this.outputJson != null) {
			try {
				new JSONObject(this.outputJson);
			} catch (Exception e) {
				vtx.addError(
						"inputPayload is not a valid json\n" + this.outputJson);
				nbr++;
			}
			if (this.outputFields != null || this.outputItems != null
					|| this.outputLists != null || this.testForFailure) {
				vtx.addError(
						"outputJson is specified, and hence other assertions on output are not relevant.");
				nbr++;
			}
			return nbr;
		}
		if (this.outputFields != null) {
			for (OutputField field : this.outputFields) {
				nbr += field.validate(vtx);
			}
		}
		if (this.outputItems != null) {
			for (OutputItem item : this.outputItems) {
				nbr += item.validate(vtx);
			}
		}
		if (this.outputLists != null) {
			for (OutputList list : this.outputLists) {
				nbr += list.validate(vtx);
			}
		}
		if (this.inputFields != null) {
			for (InputField field : this.inputFields) {
				nbr += field.validate(vtx);
			}
		}
		if (this.inputItems != null) {
			for (InputItem item : this.inputItems) {
				nbr += item.validate(vtx);
			}
		}
		return nbr;
	}

	/**
	 * @return
	 */
	String getInput(TestContext ctx) {
		JSONObject json;
		if (this.inputJson == null) {
			json = new JSONObject();
		} else {
			json = new JSONObject(this.inputJson);
		}

		if (this.inputItems != null) {
			for (InputItem item : this.inputItems) {
				item.setInputValues(json, ctx);
			}
		}
		if (this.inputFields != null) {
			for (InputField field : this.inputFields) {
				field.setInputValue(json, ctx);
			}
		}
		return json.toString();
	}

	/**
	 * @return service name
	 */
	public String getServiceName() {
		return this.serviceName;
	}
}
