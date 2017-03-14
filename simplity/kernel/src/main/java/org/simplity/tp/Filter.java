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
package org.simplity.tp;

import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.service.ServiceContext;

/**
 * Read a row from a record, and possibly read relevant rows from related
 * records
 *
 *
 * @author simplity.org
 *
 */
public class Filter extends DbAction {

	/**
	 * record that is used for inputting and creating filter criteria
	 */
	String filterRecordName;
	/**
	 * optional. defaults to filterRecordName
	 */
	String outputRecordName;
	/**
	 * name of the sheet in which data is received. if null, we take data from
	 * fields. Sheet can not contain more than one rows
	 */
	String inputSheetName;
	/**
	 * name of the sheet in which output is sent. Defaults to simple name of
	 * outputRecordName
	 */
	String outputSheetName;
	/**
	 * child records from which to read rows, for the row read in this record
	 */
	RelatedRecord[] childRecords;

	/**
	 * should child records for this filter/record be filtered automatically?
	 */
	boolean cascadeFilterForChildren;

	/**
	 * default constructor
	 */
	public Filter() {

	}

	/**
	 * get a default filterAction for a record, possibly with child rows
	 *
	 * @param record
	 */
	public Filter(Record record) {
		this.actionName = "filter_" + record.getSimpleName();
		String recordName = record.getQualifiedName();
		this.filterRecordName = recordName;
		this.outputRecordName = recordName;
		this.outputSheetName = record.getDefaultSheetName();
		this.cascadeFilterForChildren = true;
	}

	@Override
	protected int doDbAct(ServiceContext ctx, DbDriver driver) {
		Record record = ComponentManager.getRecord(this.filterRecordName);
		Record outRecord = record;
		if (this.outputRecordName != null) {
			outRecord = ComponentManager.getRecord(this.outputRecordName);
		}

		DataSheet outSheet = null;

		if (this.inputSheetName == null) {
			outSheet = outRecord.filter(record, ctx, driver, ctx.getUserId());
		} else {
			DataSheet inSheet = ctx.getDataSheet(this.inputSheetName);
			if (inSheet == null) {
				Tracer.trace("Filter Action " + this.actionName
						+ " did not execute because input sheet "
						+ this.inputSheetName + " is not found.");
				return 0;
			}

			outSheet = outRecord.filter(record, inSheet, driver,
					ctx.getUserId());
		}
		int result = outSheet.length();
		if (this.outputSheetName == null) {
			if (result == 0) {
				ctx.addMessage(Messages.WARNING, "No matching records");
				return 0;
			}
			ctx.copyFrom(outSheet);
			result = 1;
		}
		ctx.putDataSheet(this.outputSheetName, outSheet);
		if (result == 0) {
			return 0;
		}
		if (this.childRecords != null) {
			for (RelatedRecord rr : this.childRecords) {
				Record cr = ComponentManager.getRecord(rr.recordName);
				cr.filterForParents(outSheet, driver, rr.sheetName,
						this.cascadeFilterForChildren, ctx);
			}
			return result;
		}
		if (this.cascadeFilterForChildren) {
			record.filterChildRecords(outSheet, driver, ctx);
		}
		return result;
	}

	@Override
	public DbAccessType getDataAccessType() {
		return DbAccessType.READ_ONLY;
	}

	@Override
	public int validate(ValidationContext ctx, Service service) {
		int count = super.validate(ctx, service);
		count += ctx.checkRecordExistence(this.filterRecordName,
				"filterRecordName", true);
		count += ctx.checkRecordExistence(this.outputRecordName,
				"outputRecordName", false);
		if (this.childRecords != null) {
			for (RelatedRecord rec : this.childRecords) {
				count += rec.validate(ctx);
			}
		}
		return count;
	}
}
