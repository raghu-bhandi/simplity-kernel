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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.tp;

import java.util.HashMap;
import java.util.Map;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * abstract action that manages a block of actions
 * @author simplity.org
 *
 */
public class Block extends Action{
	/**
	 * default constructor
	 */
	public Block(){
		//
	}
	/**
	 * actions of this block
	 */
	Action[] actions;

	/**
	 * determined based on sub-actions of this block
	 */
	protected DbAccessType dbAccess;

	protected Map<String, Integer> indexedActions = new HashMap<String, Integer>();

	@Override
	public DbAccessType getDataAccessType() {
		return this.dbAccess;
	}
	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
		BlockWorker actionBlock = new BlockWorker(this.actions,
				this.indexedActions, ctx);
		JumpSignal signal = actionBlock.execute(driver);
		if (signal == JumpSignal.STOP) {
			return Value.VALUE_FALSE;
		}
		if (signal == JumpSignal.BREAK) {
			return Value.VALUE_TRUE;
		}
		return Value.VALUE_TRUE;
	}


	@Override
	public void getReady(int idx, Service service) {
		super.getReady(idx, service);
		this.actionNameOnFailure = "_stop";

		if (this.actions == null) {
			throw new ApplicationError("Loop Action " + this.actionName
					+ " is empty. No point in looping just like that :-) ");
		}
		int i = 0;
		/*
		 * this.dbAccess is set to propagate it upwards. Start with none.
		 * upgrade it to READ_ONLY or READ_WRITE based on the demand by
		 * sub-actions
		 */
		this.dbAccess = DbAccessType.NONE;
		for (Action action : this.actions) {
			action.getReady(i, service);
			this.indexedActions.put(action.getName(), new Integer(i));
			i++;
			/*
			 * see if we have to upgrade our access type
			 */
			if (this.dbAccess == DbAccessType.READ_WRITE) {
				continue;
			}
			DbAccessType access = action.getDataAccessType();
			if (access == null || access == DbAccessType.NONE) {
				continue;
			}
			if (access == DbAccessType.READ_ONLY) {
				this.dbAccess = access;
			} else {
				/*
				 * anything other than none/read_only would mean read-write for
				 * us
				 */
				this.dbAccess = DbAccessType.READ_WRITE;
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
		if (this.actions == null || this.actions.length == 0) {
			ctx.addError(
					"No actions specified for loop. We are not going to cycle with no chains!!.");
			count++;
		}
		for(Action action : this.actions){
			count += action.validate(ctx, service);
		}
		return count;
	}

}
