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
import org.simplity.json.JSONWriter;
import org.simplity.kernel.comp.ValidationContext;

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
	 * description for documentation
	 */
	String description;

	/**
	 * in case you do not want to specify individual field values but provide a
	 * ready string that is to be provided as request pay-load to the service.
	 * If this is specified, fields ad sheets are not relevant and are ignored
	 */
	String inputPayload;
	/**
	 * input fields with which the service is to be requested
	 */
	Field[] inputFields;
	/**
	 * input data sheets
	 */
	Sheet[] inputSheets;

	/*
	 * output meant for assertions
	 */

	/**
	 * if you want to specify exactly the fields/sheets expected as in the
	 * pay-load format(josn). We compare the josn elements, and not just a
	 * string comparison to take care of white-space issues
	 */
	String outputPayload;

	/**
	 * number of errors expected. non-zero implies that we expect this request
	 * to fail, and hence output data expectations are irrelevant. Also, in case
	 * expectedMessageNames are specified, ensure that this number does not
	 * contradict them.
	 */
	int nbrErrorsExpected;
	/**
	 * expected output fields that need assertions
	 */
	Field[] outputFields;
	/**
	 * expected output sheets that need assertions
	 */
	Sheet[] outputSheets;

	/**
	 * @param output
	 *            json that is returned from service. This is either an array of
	 *            messages or a response json
	 * @return null if it compares well. Error message in case of any trouble
	 */
	String compareOutput(String output) {
		int nbrErrors = 0;
		if (output.charAt(0) == '[') {
			JSONArray arr = new JSONArray(output);
			nbrErrors = arr.length();
		}
		if (nbrErrors > 0) {
			/*
			 * service failed
			 */
			if (this.nbrErrorsExpected == nbrErrors) {
				/*
				 * test succeeded
				 */
				return null;
			}
			if (this.nbrErrorsExpected == 0) {
				return nbrErrors
						+ " errors found while a successfull request is expected";
			}
			return nbrErrors + " errors found while expecting "
					+ this.nbrErrorsExpected + " errors.";
		}
		/*
		 * service succeeded.
		 */

		if (this.nbrErrorsExpected > 0) {
			return "No errors found while expecting " + this.nbrErrorsExpected
					+ " errors.";
		}

		/*
		 * are we expecting a specific json?
		 */
		JSONObject json = new JSONObject(output);
		if (this.outputPayload != null) {
			JSONObject expected = new JSONObject(this.outputPayload);
			if (json.similar(expected)) {
				return null;
			}
			return "Response json did not match the expected json";
		}

		/*
		 * what fields are we expecting?
		 */
		if (this.outputFields != null) {
			for (Field field : this.outputFields) {
				Object val = json.opt(field.fieldName);
				if (val == null) {
					return "Response did not contain an expected field named "
							+ field.fieldName;
				}
				if (val.toString().equals(field.fieldValue) == false) {
					return "Expected a value of " + field.fieldValue
							+ " for field " + field.fieldName + " but we got "
							+ val;
				}
			}
		}

		/*
		 * sheets
		 */
		if (this.outputSheets != null) {
			return this.matchSheets(json);
		}

		return null;
	}

	private String matchSheets(JSONObject json) {
		for (Sheet sheet : this.outputSheets) {
			Object val = json.opt(sheet.sheetName);
			if (val == null) {
				return "Response did not contain an expected data sheet named "
						+ sheet.sheetName;
			}
			JSONArray arr = null;
			if (val instanceof JSONArray) {
				arr = (JSONArray) val;
			} else {
				if (val instanceof JSONObject == false) {
					return "Expected a data sheet named " + sheet.sheetName
							+ " but we found a non-object non-array of " + val;
				}
				arr = new JSONArray();
				arr.put(val);
			}
			String msg = sheet.match(arr);
			if (msg != null) {
				return msg;
			}
		}
		return null;
	}

	/**
	 * @return json(payload)
	 */
	String getInput() {
		if (this.inputPayload != null) {
			return this.inputPayload;
		}
		JSONWriter writer = new JSONWriter();
		writer.object();
		if (this.inputFields != null) {
			for (Field field : this.inputFields) {
				field.toJson(writer);
			}
		}
		if (this.inputSheets != null) {
			for (Sheet sheet : this.inputSheets) {
				sheet.writeToJson(writer);
			}
		}
		writer.endObject();
		return writer.toString();
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
		if (this.inputPayload != null) {
			try {
				new JSONObject(this.inputPayload);
			} catch (Exception e) {
				vtx.addError("inputPayload is not a valid json\n"
						+ this.inputPayload);
				nbr++;
			}
		}
		if (this.inputFields != null) {
			if (this.inputPayload != null) {
				vtx.addError("inputPayload is specified, and hence inputFields is not relavant");
				nbr++;
			}
			for (Field field : this.inputFields) {
				nbr += field.validate(vtx, true);
			}
		}
		if (this.inputSheets != null) {
			if (this.inputPayload != null) {
				vtx.addError("inputPayload is specified, and hence inpuSheets is not relavant");
				nbr++;
			}
			for (Sheet sheet : this.inputSheets) {
				nbr += sheet.validate(vtx);
			}

		}
		if (this.outputPayload != null) {
			try {
				new JSONObject(this.outputPayload);
			} catch (Exception e) {
				vtx.addError("inputPayload is not a valid json\n"
						+ this.inputPayload);
				nbr++;
			}
		}
		if (this.nbrErrorsExpected > 0) {
			if (this.outputPayload != null) {
				vtx.addError("inputPayload is not relevant once nbrErrorExpected is specified");
				nbr++;
			}
		}
		if (this.outputFields != null) {
			if (this.outputPayload != null) {
				vtx.addError("outputFields is not relevant once nbrErrorExpected is specified");
				nbr++;
			}
			for (Field field : this.outputFields) {
				nbr += field.validate(vtx, false);
			}
		}
		if (this.outputSheets != null) {
			if (this.outputPayload != null) {
				vtx.addError("outputSheets is not relevant once nbrErrorExpected is specified");
				nbr++;
			}
			for (Sheet sheet : this.outputSheets) {
				nbr += sheet.validate(vtx);
			}
		}
		return nbr;
	}
}
