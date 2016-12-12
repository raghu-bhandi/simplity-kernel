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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.test;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.kernel.comp.Component;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.service.ServiceProtocol;

/**
 * Represents one test case for a service with one specific input and expected
 * output
 *
 * @author simplity.org
 *
 */
public class TestCase implements Component {
	/**
	 * unique name given to this test case.
	 */
	String testCaseName;

	/**
	 * unique name given to this test case.
	 */
	String moduleName;

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
	/*
	 * output meant for assertions
	 */

	/**
	 * if you want to specify exactly the fields/sheets expected as in the
	 * pay-load format(josn). We compare the josn elements, and not just a
	 * string comparison to take care of white-space and sequence/order issues
	 */
	String outputJson;

	/**
	 * number of errors expected. non-zero implies that we expect this request
	 * to fail, and hence output data expectations are irrelevant.
	 */
	boolean testForFailure;
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

	ContextField[] fieldsToBeAddedToContext;

	/**
	 * @param output
	 *            json that is returned from service. This is either an array of
	 *            messages or a response json
	 * @return null if it compares well. Error message in case of any trouble
	 */
	String processOutput(String output, TestContext ctx) {
		JSONObject json = new JSONObject(output);
		if (this.fieldsToBeAddedToContext != null) {
			for (ContextField field : this.fieldsToBeAddedToContext) {
				field.addToContext(json, ctx);
			}
		}

		int nbrErrors = this.countErrors(json);
		if (nbrErrors > 0) {
			/*
			 * service failed
			 */
			if (this.testForFailure) {
				/*
				 * test succeeded
				 */
				return null;
			}
			return nbrErrors
					+ " errors found while a successfull request is expected";
		}
		/*
		 * service succeeded.
		 */
		if (this.testForFailure) {
			return "Service succeeded while we expected it to fail.";
		}

		/*
		 * are we expecting a specific json?
		 */
		if (this.outputJson != null) {
			JSONObject expected = new JSONObject(this.outputJson);
			if (json.similar(expected)) {
				return null;
			}
			return "Response json did not match the expected json";
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

		return null;
	}

	/**
	 * @param json
	 * @return
	 */
	private int countErrors(JSONObject json) {
		Object obj = json.opt(ServiceProtocol.MESSAGES);
		JSONArray msgs = (JSONArray) obj;
		int nbrMsgs = msgs.length();
		int nbrErrors = 0;
		for (int i = 0; i < nbrMsgs; i++) {
			if (msgs.getJSONObject(i).optString("messageType").toUpperCase()
					.equals("error")) {
				nbrErrors++;
			}
		}
		return nbrErrors;
	}

	/**
	 * validate this components and report any errors
	 *
	 * @param vtx
	 * @return number of errors detected
	 */
	@Override
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
				vtx.addError("inputPayload is not a valid json\n"
						+ this.inputJson);
				nbr++;
			}
		}
		if (this.outputJson != null) {
			try {
				new JSONObject(this.outputJson);
			} catch (Exception e) {
				vtx.addError("inputPayload is not a valid json\n"
						+ this.outputJson);
				nbr++;
			}
			if (this.outputFields != null || this.outputItems != null
					|| this.outputLists != null || this.testForFailure) {
				vtx.addError("outputJson is specified, and hence other assertions on output are not relevant.");
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
		if (this.inputFields == null && this.inputItems == null) {
			return this.inputJson;
		}
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.comp.Component#getSimpleName()
	 */
	@Override
	public String getSimpleName() {
		return this.testCaseName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.comp.Component#getQualifiedName()
	 */
	@Override
	public String getQualifiedName() {
		if (this.moduleName == null) {
			return this.testCaseName;
		}
		return this.moduleName + '.' + this.testCaseName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.comp.Component#getReady()
	 */
	@Override
	public void getReady() {
		// This component is not saved and re-used in memory. Hence no
		// preparation on load.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.comp.Component#getComponentType()
	 */
	@Override
	public ComponentType getComponentType() {
		return ComponentType.TEST_CASE;
	}
}
