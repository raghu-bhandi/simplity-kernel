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

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.util.JsonUtil;

/**
 * Represents a list/array of items in a json
 *
 * @author simplity.org
 *
 */
public class OutputList {
	/**
	 * qualified name that would fetch this list (array) for example a.2.b would
	 * get json[a][2][b]
	 */
	String listSelector;
	/**
	 * min rows expected
	 */
	int minRows;
	/**
	 * max rows expected
	 */
	int maxRows;

	/**
	 * check for any semantic errors
	 *
	 * @param vtx
	 * @return
	 */
	int validate(ValidationContext vtx) {
		int nbr = 0;
		if (this.listSelector == null) {
			vtx.addError("name is mandatory for list in a test case");
			nbr++;
		}
		if (this.maxRows < this.minRows) {
			vtx.addError("max rows is set to " + this.maxRows
					+ " while minRows is set to " + this.minRows);
			nbr++;
		}
		if (this.minRows == 0 && this.maxRows == 0) {
			vtx.addError("max rows and min rows are not set. This list has no assertion.");
			nbr++;
		}
		return nbr;
	}

	/**
	 * @param ctx
	 * @param arr
	 * @return
	 */
	String match(JSONObject parentJson, TestContext ctx) {
		Object json = JsonUtil.getObjectValue(this.listSelector, parentJson);
		if (json == null) {
			if (this.minRows != 0) {
				return "List " + this.listSelector + " should have at least "
						+ this.minRows + " rows.";
			}
		}
		if (json instanceof JSONArray == false) {
			return this.listSelector + " should be a list/array";
		}
		int nbrRows = ((JSONArray) json).length();
		if (this.minRows > 0 && nbrRows < this.minRows) {
			return "List " + this.listSelector + " should have at least "
					+ this.minRows + " rows.";
		}
		if (this.maxRows > 0 && nbrRows > this.maxRows) {
			return "List " + this.listSelector + " should have at most "
					+ this.maxRows + " rows.";
		}
		return null;
	}
}
