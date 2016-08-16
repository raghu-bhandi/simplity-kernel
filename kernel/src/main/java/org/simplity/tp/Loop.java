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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.tp;

import java.util.HashMap;
import java.util.Map;

import org.simplity.kernel.ApplicationError;
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
		DataSheet sheet = ctx.getDataSheet(this.dataSheetName);
		if (sheet == null) {
			return null;
		}
		int nbrRows = sheet.length();
		if (nbrRows == 0) {
			return null;
		}
		DataSheetIterator iterator = null;
		try {
			iterator = ctx.startIteration(this.dataSheetName);
		} catch (AlreadyIteratingException e) {
			throw new ApplicationError(
					"Loop action "
							+ this.actionName
							+ " is desigend to iterate on data sheet "
							+ this.dataSheetName
							+ " but that data sheet is already iterating as part of an enclosing loop action.");
		}
		ActionBlock actionBlock = new ActionBlock(this.actions,
				this.indexedActions, ctx);
		while (iterator.moveToNextRow()) {
			actionBlock.workWithDriver(driver);
		}
		return null;
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
}
