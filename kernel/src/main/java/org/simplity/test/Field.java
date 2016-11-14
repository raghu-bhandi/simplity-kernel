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

import org.simplity.json.JSONWriter;
import org.simplity.kernel.comp.ValidationContext;

/**
 * represents a field to be provided as input to a service
 *
 * @author simplity.org
 *
 */
public class Field {
	/**
	 * field name
	 */
	String fieldName;
	/**
	 * field value
	 */
	String fieldValue;

	/**
	 *
	 * @param vtx
	 * @param forInput
	 * @return number of errors added
	 */
	int validate(ValidationContext vtx, boolean forInput) {
		int nbr = 0;
		if (this.fieldName == null) {
			vtx.addError("fieldName is a requried attribute for an input field");
			nbr++;
		}
		if (forInput && this.fieldValue == null) {
			vtx.addError("Defining a field with null value has no meaning. You may as well drop the field.");
			nbr++;
		}
		return nbr;
	}

	/**
	 * write this as an attribute (key-value)
	 * 
	 * @param writer
	 */
	void toJson(JSONWriter writer) {
		writer.key(this.fieldName);
		writer.value(this.fieldValue);
	}
}
