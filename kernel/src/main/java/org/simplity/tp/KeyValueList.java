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

import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * typical list of values for a drop-down. Action is designed in case you need
 * this in addition to other things. In case you have a service with only one
 * keyValueList action, consider using a record based on-the-fly service
 *
 * @author simplity.org
 *
 */
public class KeyValueList extends DbAction {

	/**
	 * record that is to be used
	 */
	String recordName;
	/**
	 * defaults to setting in the record
	 */
	String outputSheetName;

	/**
	 * defaults
	 */
	public KeyValueList() {
		//
	}

	/**
	 * list action for the record
	 *
	 * @param record
	 */
	public KeyValueList(Record record) {
		this.actionName = "list_" + record.getSimpleName();
		this.recordName = record.getQualifiedName();
		this.outputSheetName = record.getDefaultSheetName();
	}

	@Override
	protected int doDbAct(ServiceContext ctx, DbDriver driver) {
		Record record = ComponentManager.getRecord(this.recordName);
		Value value = ctx.getValue(record.getValueListKeyName());
		DataSheet sheet = record
				.list(value.toString(), driver, ctx.getUserId());
		if (sheet == null) {
			return 0;
		}
		String sheetName = this.outputSheetName == null ? record
				.getDefaultSheetName() : this.outputSheetName;
				ctx.putDataSheet(sheetName, sheet);
				return sheet.length();
	}

	@Override
	public DbAccessType getDataAccessType() {
		return DbAccessType.READ_ONLY;
	}
}
