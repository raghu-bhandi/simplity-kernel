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

import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * copy rows from a compatible sheet.
 *
 * @author simplity.org
 *
 */
public class CopyRows extends Action {

	/**
	 * sheet to which we want to add rows
	 */
	String toSheetName;

	/**
	 * sheet from which to copy rows
	 */
	String fromSheetName;

	@Override
	protected Value doAct(ServiceContext ctx) {
		DataSheet fromSheet = ctx.getDataSheet(this.fromSheetName);
		if (fromSheet == null) {
			return Value.VALUE_ZERO;
		}
		int nbrRows = fromSheet.length();
		if (nbrRows == 0) {
			return Value.VALUE_ZERO;
		}
		DataSheet toSheet = ctx.getDataSheet(this.toSheetName);
		if (toSheet == null) {
			ctx.putDataSheet(this.toSheetName, fromSheet);
		} else {
			toSheet.appendRows(fromSheet);
		}

		return Value.newIntegerValue(nbrRows);
	}
}
