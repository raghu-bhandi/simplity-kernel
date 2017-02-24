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

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.data.AlreadyIteratingException;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.DataSheetIterator;
import org.simplity.kernel.data.Fields;
import org.simplity.kernel.dm.Field;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.expr.Expression;
import org.simplity.kernel.expr.InvalidOperationException;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;

/**
 * add a column to a data sheet.
 *
 * @author admin
 *
 */
public class AddRow extends Action {

	/**
	 * sheet to which we want to add a column
	 */
	String sheetName;

	/**
	 * name of column to be added
	 */
	String recordName;

	@Override
	protected Value doAct(ServiceContext ctx) {
		DataSheet sheet = ctx.getDataSheet(this.sheetName);
		if (sheet == null) {
			return Value.VALUE_FALSE;
		}
		Record record = ComponentManager.getRecord(this.recordName);

		if (this.recordName != null) {
			Field[] fields = record.getFields();
			Value[] row = new Value[fields.length];
			for (int i = 0; i < fields.length; i++) {
				Value value = null;
				String fieldName = fields[i].getName();
				if (fieldName != null) {
					value = ctx.getValue(fieldName);
				} else {
					value = Value.parseValue(fieldName, fields[i].getValueType());
				}
				row[i] = value;
			}
			sheet.addRow(row);
		}
		return Value.VALUE_TRUE;
	}
}
