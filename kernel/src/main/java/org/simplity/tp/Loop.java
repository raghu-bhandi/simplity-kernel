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

import java.util.HashMap;
import java.util.Map;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.AlreadyIteratingException;
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
public class Loop extends Action {

	/**
	 * returns either a name of action to go to, or "_stop", "_error"
	 */
	String dataSheetName;

	/**
	 * actions that are to be performed for each row of the data sheet
	 */
	Action[] actions;

	/**
	 * calculated based on sub-actions of this block
	 */
	private DbAccessType dbAccess;

	private Map<String, Integer> indexedActions = new HashMap<String, Integer>();

	@Override
	protected Value doAct(ServiceContext ctx, DbDriver driver) {
		ActionBlock actionBlock = new ActionBlock(this.actions, this.indexedActions,
				ctx);
		boolean toContinue = true;
		if (this.dataSheetName != null) {
			toContinue = this.loopOnSheet(actionBlock, driver, ctx);
		} else if (this.executeOnCondition != null) {
			toContinue = this.loopOnCondition(actionBlock, driver, ctx);
		} else {
			throw new ApplicationError("Loop action " + this.actionName
					+ " has niether data sheet, nor condition.");
		}
		if(toContinue){
			return null;
		}
		return Service.STOP_VALUE;
	}

	/**
	 * loop over this block for supplied expression/condition
	 *
	 * @param expr
	 * @param driver
	 * @return true if normal completion. False if we encountered a STOP signal
	 */
	private boolean loopOnCondition(ActionBlock actionBlock, DbDriver driver, ServiceContext ctx) {
		/*
		 * loop with a condition
		 */
		try {
			Value value = this.executeOnCondition.evaluate(ctx);
			while (value.toBoolean()) {
				JumpSignal signal = actionBlock.execute(driver);
				if (signal == JumpSignal.STOP) {
					return false;
				}
				if (signal == JumpSignal.BREAK) {
					return true;
				}
				value = this.executeOnCondition.evaluate(ctx);
			}
			return true;
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
	private boolean loopOnSheet(ActionBlock actionBlock, DbDriver driver, ServiceContext ctx) {
		DataSheetIterator iterator = null;
		try {
			iterator = ctx.startIteration(this.dataSheetName);
		} catch (AlreadyIteratingException e) {
			throw new ApplicationError(
					"Loop action is designed to iterate on data sheet "
							+ this.dataSheetName
							+ " but that data sheet is already iterating as part of an enclosing loop action.");
		}
		while (iterator.moveToNextRow()) {
			JumpSignal signal = actionBlock.execute(driver);
			if (signal == JumpSignal.STOP) {
				iterator.cancelIteration();
				return false;
			}
			if (signal == JumpSignal.BREAK) {
				iterator.cancelIteration();
				return true;
			}
		}
		return true;
	}

	@Override
	public DbAccessType getDataAccessType() {
		return this.dbAccess;
	}

	@Override
	public void getReady(int idx) {
		super.getReady(idx);
		if (this.actions == null) {
			throw new ApplicationError("Loop Action " + this.actionName
					+ " is empty. No point in looping just like that :-) ");
		}
		int i = 0;
		for (Action action : this.actions) {
			action.getReady(i);
			this.indexedActions.put(action.getName(), new Integer(i));
			i++;
			DbAccessType access = action.getDataAccessType();
			switch (access) {
			case READ_WRITE:
				this.dbAccess = access;
				return;
			case READ_ONLY:
				this.dbAccess = access;
				break;
			case NONE:
				break;
			default:
				throw new ApplicationError(
						"sub-actions of a loop action can not have dbAccess types other than none, readOnly and readWrite");
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
