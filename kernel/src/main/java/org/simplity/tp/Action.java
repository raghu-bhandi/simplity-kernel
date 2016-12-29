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

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.expr.Expression;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * An action inside a service
 *
 * @author simplity.org
 *
 */
public abstract class Action {
	private static final String ACTION_NAME_PREFIX = "_a";

	/**
	 * unique name within a service
	 */
	protected String actionName = null;

	/**
	 * precondition to be met for this step to be executed.
	 */
	protected Expression executeOnCondition = null;

	/**
	 * if you want to execute this step if a sheet exists and has at least one
	 * row
	 */
	protected String executeIfRowsInSheet;

	/**
	 * execute if there is no sheet, or sheet has no rows
	 */
	protected String executeIfNoRowsInSheet;

	private int serviceIdx;

	/**
	 * main method called by service.
	 *
	 * @param ctx
	 * @param driver
	 * @return an indicator of what the action did. This value is saved as a
	 *         field named actionNameResult that can be used by subsequent
	 *         actions. null implies no such feature
	 */
	public Value act(ServiceContext ctx, DbDriver driver) {
		/*
		 * is this a conditional step? i.e. to be executed only if the condition
		 * is met
		 */

		if (this.executeOnCondition != null) {
			try {
				if (!this.executeOnCondition.evaluate(ctx).toBoolean()) {
					return null;
				}
			} catch (Exception e) {
				throw new ApplicationError("Action " + this.actionName
						+ " has an executOnCondition="
						+ this.executeOnCondition.toString()
						+ " that is invalid. \nError : " + e.getMessage());
			}
		}
		if (this.executeIfNoRowsInSheet != null
				&& ctx.nbrRowsInSheet(this.executeIfNoRowsInSheet) > 0) {
			return null;
		}
		if (this.executeIfRowsInSheet != null
				&& ctx.nbrRowsInSheet(this.executeIfRowsInSheet) == 0) {
			return null;
		}
		return this.doAct(ctx, driver);
	}

	/**
	 * main method of the concrete type.
	 *
	 * @param ctx
	 * @param driver
	 * @return an indicator of what the action did. null means it has detected
	 *         an error, and we have to stop and roll-back this service
	 */
	protected abstract Value doAct(ServiceContext ctx, DbDriver driver);

	/***
	 * what type of data access does this action require?
	 *
	 * @return data base access type required by this step.
	 */
	public DbAccessType getDataAccessType() {
		return DbAccessType.NONE;
	}

	/**
	 * @return name of this action
	 */
	public String getName() {
		return this.actionName;
	}

	/**
	 * if there is anything this class wants to do after loading its attributes,
	 * but before being used, here is the method to do that.
	 *
	 * @param idx
	 *            0 based index of actions in service
	 */
	public void getReady(int idx) {
		this.serviceIdx = idx;
		if (this.actionName == null) {
			this.actionName = ACTION_NAME_PREFIX + this.serviceIdx;
		}

	}

	/**
	 * validate this action
	 *
	 * @param ctx
	 * @param service
	 *            parent service
	 * @return number of errors added to the list
	 */
	public int validate(ValidationContext ctx, Service service) {
		return 0;
	}
}