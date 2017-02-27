/*
 * Copyright (c) 2017 simplity.org
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

import org.simplity.kernel.Tracer;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;

/**
 * add a row to a data sheet using fields from the context.
 *
 * @author org.simplity
 *
 */
public class AddRow extends Action {

	/**
	 * sheet to which row is to be added
	 */
	String sheetName;

	@Override
	protected Value doAct(ServiceContext ctx) {
		DataSheet sheet = ctx.getDataSheet(this.sheetName);
		if (sheet == null) {
			return Value.VALUE_FALSE;
		}
		String[] names = sheet.getColumnNames();
		Value[] row = new Value[names.length];
		ValueType[] types = sheet.getValueTypes();
		int i = 0;
		for (String name : names) {
			Value value = ctx.getValue(name);
			ValueType vt = types[i];
			if (value == null) {
				/*
				 * We prefer to avoid java null for obvious reasons
				 */
				value = Value.newUnknownValue(vt);
			} else if (value.getValueType() != vt) {
				/*
				 * should we reject this value? possible that it is text but has valid number in it!!
				 */
				String txt = value.toString();
				Tracer.trace("Found a value of type " + value.getValueType()
						+ " for column " + name + " while we were expecting "
						+ vt + ". We will try to convert it.");
				value = Value.parseValue(txt, vt);
				if(value == null){
					Tracer.trace("Unable to convert " + txt + " to type " + vt + " . setting column to  NullValue" );
				}
			}
			row[i] = value;
			i++;
		}
		sheet.addRow(row);
		return Value.VALUE_TRUE;
	}
}
