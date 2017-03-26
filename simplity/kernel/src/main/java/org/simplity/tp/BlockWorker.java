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

import org.simplity.jms.MessageClient;
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
public class BlockWorker implements DbClientInterface, MessageClient {

	/**
	 * field name with which result of an action is available in service context
	 */
	public static final String RESULT_SUFFIX = "Result";
	/**
	 * actions we have to take care of
	 */
	private final Action[] actions;

	/**
	 * in case there is a goto action, we need the map
	 */
	private final Map<String, Integer> indexedActions;

	/**
	 * service context
	 */
	private final ServiceContext ctx;

	/**
	 * db driver that this is initialized with. Used for JMS
	 */
	private final DbDriver initialDriver;

	private boolean keepGoing = true;
	/**
	 *
	 * @param actions
	 * @param indexedActions
	 * @param ctx
	 */
	public BlockWorker(Action[] actions, Map<String, Integer> indexedActions,
			ServiceContext ctx) {
		this.actions = actions;
		this.indexedActions = indexedActions;
		this.ctx = ctx;
		this.initialDriver = null;
	}

	/**
	 *
	 * @param actions
	 * @param indexedActions
	 * @param ctx
	 * @param driver
	 */
	public BlockWorker(Action[] actions, Map<String, Integer> indexedActions,
			ServiceContext ctx, DbDriver driver) {
		this.actions = actions;
		this.indexedActions = indexedActions;
		this.ctx = ctx;
		this.initialDriver = driver;
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
		return this.execute(driver) !=  JumpSignal.STOP;
	}

	/**
	 * execute this block once
	 * @param driver
	 * @return null if it is a normal exit, or a specific signal
	 */
	public JumpSignal execute(DbDriver driver) {
		int nbrActions = this.actions.length;
		int currentIdx = 0;
		Value result = null;
		while (currentIdx < nbrActions) {
			Action action = this.actions[currentIdx];
			long startedAt = new Date().getTime();
			result = action.act(this.ctx, driver);
			currentIdx++;
			Tracer.trace("Action " + action.actionName
					+ " finished with result=" + result + " in "
					+ (new Date().getTime() - startedAt) + " ms");

			if (result == null) {
				continue;
			}
			/*
			 * did the caller signal a stop ?
			 */
			if (result.equals(Service.STOP_VALUE)) {
				return JumpSignal.STOP;
			}

			if(action instanceof JumpTo == false){
				this.ctx.setValue(action.actionName + BlockWorker.RESULT_SUFFIX,
						result);
				continue;
			}
			String destn = result.toString();
			/*
			 * process special case of jumpAction.
			 * Re we to jump out?
			 */
			if (destn.equals(JumpSignal._CONTINUE)) {
				return JumpSignal.CONTINUE;
			}

			if (destn.equals(JumpSignal._BREAK)) {
				return JumpSignal.BREAK;
			}

			if (destn.equals(JumpSignal._STOP)) {
				return JumpSignal.STOP;
			}

			/*
			 * jump is within this block to another action
			 */
			Integer idx = this.indexedActions.get(destn);
			if (idx != null) {
				/*
				 * we are to go to this step.
				 */
				currentIdx = idx.intValue();
			}else{
				throw new ApplicationError(result
						+ " is not a valid action to jump to.");
			}

		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.simplity.jms.MessageClient#process(org.simplity.service.ServiceContext)
	 */
	@Override
	public boolean process(ServiceContext sameCtxComingBack) {
		JumpSignal signal = this.execute(this.initialDriver);
		if(signal == JumpSignal.STOP){
			this.keepGoing = false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.simplity.jms.MessageClient#toContinue()
	 */
	@Override
	public boolean toContinue() {
		return this.keepGoing;
	}
}