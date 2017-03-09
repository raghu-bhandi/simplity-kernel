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
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.AlreadyIteratingException;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.DataSheetIterator;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * Loop through a set of actions for each row in a data sheet
 *
 *
 * @author simplity.org
 *
 */
public class Loop extends Block {

	/**
	 * data sheet on which to loop
	 */
	String dataSheetName;

	/**
	 * for the loop, do you want to treat some columns as if they are fields in
	 * the collection? This feature helps in re-using services that assume
	 * fields as sub-service inside loops. * for all columns
	 */
	String[] columnsToCopyAsFields;

	/**
	 * in case the code inside the loop is updating some of the fields that are
	 * to be copied back to data sheet
	 */
	String[] fieldsToCopyBackAsColumns;

	/**
	 * special case where we are to copy all columns as fields
	 */
	private boolean copyAllColumnsToFields;

	/**
	 * special case where we are to copy back all fields into columns
	 */
	private boolean copyBackAllColumns;

	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
		BlockWorker actionBlock = new BlockWorker(this.actions,
				this.indexedActions, ctx);
		if (this.dataSheetName != null) {
			return this.loopOnSheet(actionBlock, driver, ctx);
		}
		if (this.executeOnCondition != null) {
			return this.loopOnCondition(actionBlock, driver, ctx);
		}
		throw new ApplicationError("Loop action " + this.actionName
				+ " has niether data sheet, nor condition.");
	}

	/**
	 * loop over this block for supplied expression/condition
	 *
	 * @param expr
	 * @param driver
	 * @return true if normal completion. False if we encountered a STOP signal
	 */
	private Value loopOnCondition(BlockWorker actionBlock, DbDriver driver,
			ServiceContext ctx) {
		/*
		 * loop with a condition
		 */
		try {
			Value value = this.executeOnCondition.evaluate(ctx);
			while (value.toBoolean()) {
				JumpSignal signal = actionBlock.execute(driver);
				if (signal == JumpSignal.STOP) {
					return Value.VALUE_FALSE;
				}
				if (signal == JumpSignal.BREAK) {
					return Value.VALUE_TRUE;
				}
				value = this.executeOnCondition.evaluate(ctx);
			}
			return Value.VALUE_TRUE;
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while evaluating "
					+ this.executeOnCondition + " into a boolean value.");
		}
	}

	/**
	 * loop over this block for supplied data sheet
	 *
	 * @param expr
	 * @param driver
	 * @return true if normal completion. False if we encountered a STOP signal
	 */
	private Value loopOnSheet(BlockWorker actionBlock, DbDriver driver,
			ServiceContext ctx) {
		DataSheet ds = ctx.getDataSheet(this.dataSheetName);
		if (ds == null) {
			Tracer.trace("Data Sheet " + this.dataSheetName
					+ " not found in the context. Loop action has no work.");
			return Value.VALUE_TRUE;
		}
		if (ds.length() == 0) {
			Tracer.trace("Data Sheet " + this.dataSheetName
					+ " has no data. Loop action has no work.");
			return Value.VALUE_TRUE;
		}
		DataSheetIterator iterator = null;
		try {
			iterator = ctx.startIteration(this.dataSheetName);
		} catch (AlreadyIteratingException e) {
			throw new ApplicationError(
					"Loop action is designed to iterate on data sheet "
							+ this.dataSheetName
							+ " but that data sheet is already iterating as part of an enclosing loop action.");
		}
		/*
		 * are we to copy columns as fields?
		 */
		Value[] savedValues = null;
		if (this.columnsToCopyAsFields != null) {
			savedValues = this.saveFields(ctx, ds);
		}
		Value result = Value.VALUE_TRUE;
		int idx = 0;
		while (iterator.moveToNextRow()) {
			if (this.columnsToCopyAsFields != null) {
				this.copyToFields(ctx, ds, idx);
			}

			JumpSignal signal = actionBlock.execute(driver);
			if (this.fieldsToCopyBackAsColumns != null) {
				this.copyToColumns(ctx, ds, idx);
			}
			if (signal == JumpSignal.STOP) {
				iterator.cancelIteration();
				result = Value.VALUE_FALSE;
				break;
			}
			if (signal == JumpSignal.BREAK) {
				iterator.cancelIteration();
				result = Value.VALUE_FALSE;
				break;
			}
			idx++;
		}
		if (savedValues != null) {
			this.restoreFields(ctx, ds, savedValues);
		}
		return result;
	}

	/**
	 * @param ctx
	 */
	private void copyToColumns(ServiceContext ctx, DataSheet ds, int idx) {
		if (this.copyBackAllColumns) {
			/*
			 * slightly optimized over getting individual columns..
			 */
			Value[] values = ds.getRow(idx);
			int i = 0;
			for (String fieldName : ds.getColumnNames()) {
				values[i++] = ctx.getValue(fieldName);
			}
			return;
		}
		for (String fieldName : this.fieldsToCopyBackAsColumns) {
			ds.setColumnValue(fieldName, idx, ctx.getValue(fieldName));
		}
	}

	/**
	 * @param ctx
	 */
	private void restoreFields(ServiceContext ctx, DataSheet ds,
			Value[] values) {
		int i = 0;
		if (this.copyAllColumnsToFields) {
			for (String fieldName : ds.getColumnNames()) {
				Value value = values[i++];
				if (value != null) {
					ctx.setValue(fieldName, value);
				}
			}
		} else {
			for (String fieldName : this.columnsToCopyAsFields) {
				Value value = values[i++];
				if (value != null) {
					ctx.setValue(fieldName, value);
				}
			}
		}
	}

	/**
	 * @param ctx
	 */
	private void copyToFields(ServiceContext ctx, DataSheet ds, int idx) {
		if (this.copyAllColumnsToFields) {
			/*
			 * slightly optimized over getting individual columns..
			 */
			Value[] values = ds.getRow(idx);
			int i = 0;
			for (String fieldName : ds.getColumnNames()) {
				ctx.setValue(fieldName, values[i++]);
			}
			return;
		}
		for (String fieldName : this.columnsToCopyAsFields) {
			ctx.setValue(fieldName, ds.getColumnValue(fieldName, idx));
		}
	}

	/**
	 * @param ctx
	 * @return
	 */
	private Value[] saveFields(ServiceContext ctx, DataSheet ds) {
		if (this.copyAllColumnsToFields) {
			Value[] values = new Value[ds.width()];
			int i = 0;
			for (String fieldName : ds.getColumnNames()) {
				values[i++] = ctx.getValue(fieldName);
			}
			return values;
		}
		Value[] values = new Value[this.columnsToCopyAsFields.length];
		for (int i = 0; i < values.length; i++) {
			values[i] = ctx.getValue(this.columnsToCopyAsFields[i]);
		}
		return values;
	}

	@Override
	public DbAccessType getDataAccessType() {
		return this.dbAccess;
	}

	@Override
	public void getReady(int idx) {
		super.getReady(idx);
		/*
		 * loop action may want the caller to stop. We use this facility
		 */
		this.actionNameOnFailure = "_stop";

		if (this.columnsToCopyAsFields != null) {
			if (this.columnsToCopyAsFields.length == 1
					&& this.columnsToCopyAsFields[0].equals("*")) {
				this.copyAllColumnsToFields = true;
			}
		}

		if (this.fieldsToCopyBackAsColumns != null) {
			if (this.fieldsToCopyBackAsColumns.length == 1
					&& this.fieldsToCopyBackAsColumns[0].equals("*")) {
				this.copyBackAllColumns = true;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#validate(org.simplity.kernel.comp.
	 * ValidationContext, org.simplity.tp.Service)
	 */
	@Override
	public int validate(ValidationContext ctx, Service service) {
		int count = super.validate(ctx, service);
		if (this.dataSheetName == null) {
			if (this.executeOnCondition == null) {
				ctx.addError(
						"Loop action should either have executeOnCondition or datasheet name");
				count++;
			}
		} else if (this.executeOnCondition != null) {
			ctx.addError(
					"Loop action should not specify both executeOnCondition and datasheet name. Please redesign your actions.");
			count++;
		}
		if (this.actions == null || this.actions.length == 0) {
			ctx.addError(
					"No actions specified for loop. We are not going to cycle with no chains!!.");
			count++;
		}
		if (this.executeIfNoRowsInSheet != null) {
			ctx.addError("executeIfNoRowsInSheet is invalid for loopAction.");
			count++;
		}
		if (this.executeIfRowsInSheet != null) {
			ctx.addError("executeIfRowsInSheet is invalid for loopAction.");
			count++;
		}
		return count;
	}
}
