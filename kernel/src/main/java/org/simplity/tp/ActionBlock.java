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

import java.util.Date;
import java.util.Map;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.db.DbClientInterface;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * block of actions. we have created this as a class to improve re-use across
 * service and loopAction
 *
 * @author simplity.org
 *
 */
public class ActionBlock implements DbClientInterface {

	/**
	 * stop the execution of this service as success
	 */
	public static final String STOP = "_stop";

	/**
	 * stop the execution of this service as success
	 */
	public static final Value STOP_VALUE = Value.newTextValue(STOP);

	/**
	 * field name with which result of an action is available in service context
	 */
	public static final String RESULT_SUFFIX = "Result";
	private final Action[] actions;
	private final Map<String, Integer> indexedActions;
	private final ServiceContext ctx;

	/**
	 *
	 * @param actions
	 * @param indexedActions
	 * @param ctx
	 */
	public ActionBlock(Action[] actions, Map<String, Integer> indexedActions,
			ServiceContext ctx) {
		this.actions = actions;
		this.indexedActions = indexedActions;
		this.ctx = ctx;
	}

	@Override
	public boolean workWithDriver(DbDriver driver) {
		this.act(driver);
		if (this.ctx.isInError()) {
			return false;
		}
		return true;
	}

	/**
	 * act on all the actions
	 *
	 * @param driver
	 * @return true if we completed all actions. False if a stop was encountered
	 *         and we stopped in between.
	 */
	public boolean act(DbDriver driver) {

		int nbrActions = this.actions == null ? 0 : this.actions.length;
		if (nbrActions == 0) {
			return true;
		}
		int currentIdx = 0;
		Value result = null;

		while (currentIdx < nbrActions) {
			Action action = this.actions[currentIdx];
			long startedAt = new Date().getTime();
			result = action.act(this.ctx, driver);
			Tracer.trace("Action " + action.actionName
					+ " finished with result=" + result + " in "
					+ (new Date().getTime() - startedAt) + " ms");

			if (result == null) {
				currentIdx++;
				continue;
			}
			/*
			 * did the caller signal a stop ?
			 */
			if (result.equals(STOP_VALUE)) {
				return false;
			}
			/*
			 * are we to jump up or down?
			 */
			if (action instanceof JumpTo) {
				String resultText = result.toText();
				Integer idx = this.indexedActions.get(resultText);
				if (idx == null) {
					throw new ApplicationError("NavigationAction "
							+ action.actionName
							+ " returned an invalid action name " + resultText);
				}
				/*
				 * we are to go to this step.
				 */
				currentIdx = idx.intValue();
				continue;
			}
			/*
			 * normal action returned non-null. save the value and move on
			 */
			this.ctx.setValue(action.actionName + ActionBlock.RESULT_SUFFIX,
					result);
			currentIdx++;
		}
		return true;
	}

}