/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
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
package org.simplity.tp;

import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.util.TextUtil;

/**
 * convenient class to work with related records as part of record based action.
 *
 * @author simplity.org
 *
 */
public class RelatedRecord {

	String recordName;
	String sheetName;
	/**
	 * child rows can either be saved individually, or we can insert new rows
	 * after deleting existing rows
	 */
	boolean replaceRows;

	/**
	 * default constructor
	 */
	public RelatedRecord() {

	}

	/**
	 * constructor with record name and sheet name
	 * 
	 * @param recordName
	 * @param sheetName
	 */
	public RelatedRecord(String recordName, String sheetName) {
		this.recordName = recordName;
		this.sheetName = sheetName;
	}

	void getReady() {
		if (this.sheetName == null) {
			this.sheetName = TextUtil.getSimpleName(this.recordName);
		}
	}

	/**
	 * @param ctx
	 * @return number of errors added
	 */
	public int validate(ValidationContext ctx) {
		if (this.recordName == null) {
			ctx.addError("Record name is required for save action");
			return 1;
		}
		ctx.addReference(ComponentType.REC, this.recordName);
		return 0;
	}
}
