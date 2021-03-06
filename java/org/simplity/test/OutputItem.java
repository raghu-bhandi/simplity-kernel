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

import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.util.JsonUtil;

/**
 * represents a JSON object. Has attributes and values. Allows setting
 * assertions on such an item
 *
 */
public class OutputItem {

	/**
	 * qualified name to get the row data. 0 for first row, a.2 to get third row
	 * of the array with attribute a etc..
	 */
	String itemSelector;
	/**
	 * fields specified for this row. This is a must. If you want to assert that
	 * this should not be there, you may as well define that as a Field
	 */
	OutputField[] outputFields;

	/**
	 * is this component designed ok?
	 *
	 * @param vtx
	 * @return number of errors
	 */
	int validate(ValidationContext vtx) {
		int nbr = 0;
		if (this.itemSelector == null) {
			vtx.addError("itemSelector is a must.");
			nbr = 1;
		}
		if (this.outputFields == null) {
			vtx.addError("Item must have fields. Use field instead if you want to assert that this item is not present.");
			nbr++;
			return nbr;
		}
		for (OutputField field : this.outputFields) {
			nbr += field.validate(vtx);
		}
		return nbr;
	}

	/**
	 * take care of assertions
	 *
	 * @param arr
	 * @return
	 */
	String match(Object data, TestContext ctx) {
		Object json = JsonUtil.getValue(this.itemSelector, data);
		if (json == null) {
			return "No data found for item " + this.itemSelector;
		}
		for (OutputField field : this.outputFields) {
			String resp = field.match(json, ctx);
			if (resp != null) {
				return resp;
			}
		}
		return null;
	}
}
