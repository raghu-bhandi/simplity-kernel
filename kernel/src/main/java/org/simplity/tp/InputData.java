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
package org.simplity.tp;

import org.simplity.json.JSONObject;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.service.ServiceContext;

/**
 * Component that specifies what inputs are expected
 *
 * @author simplity.org
 *
 */

public class InputData {

	InputField[] inputFields;

	InputRecord[] inputRecords;

	/**
	 * extract and validate data from input service data into service context
	 *
	 * @param json
	 *            input
	 * @param ctx
	 *            into which data is to be extracted to
	 */
	public void extractFromJson(JSONObject json, ServiceContext ctx) {
		int n = 0;
		if (this.inputFields != null) {
			for (InputField field : this.inputFields) {
				if (field.extractInput(json.opt(field.name), ctx)) {
					n++;
				}
			}
			Tracer.trace(n + " fields extracted for input");
		}
		if (this.inputRecords != null) {
			for (InputRecord inRec : this.inputRecords) {
				inRec.extractInput(json, ctx);
			}
		}
		Tracer.trace("We extracted " + ctx.getAllFields().size()
				+ " fields in all");

	}

	/**
	 * get ready for a long-haul service :-)
	 */
	public void getReady() {
		if (this.inputFields != null) {
			for (InputField field : this.inputFields) {
				field.getReady();
			}
		}
		if (this.inputRecords != null) {
			for (InputRecord inRec : this.inputRecords) {
				inRec.getReady();
			}
		}
	}

	/**
	 * validate this specification
	 *
	 * @param ctx
	 * @return number of errors added
	 */
	public int validate(ValidationContext ctx) {
		if (this.inputRecords == null) {
			if (this.inputFields == null) {
				ctx.addError("input data has no input records and no input fields. If no data is expected, just skip InputData.");
				return 1;
			}
			return 0;
		}
		int count = 0;
		for (InputRecord rec : this.inputRecords) {
			count += rec.validate(ctx);
		}
		if (this.inputFields != null) {
			for (InputField fields : this.inputFields) {
				count += fields.validate(ctx);
			}
		}
		return count;
	}
}
