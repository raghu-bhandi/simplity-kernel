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

import org.simplity.kernel.MessageType;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * Base class for all action that deal with db
 *
 *
 * @author simplity.org
 *
 */
public abstract class DbAction extends Action {

	/**
	 * if the sql succeeds in extracting at least one row, or affecting one
	 * update, do we need to put a message?
	 */
	String successMessageName;
	/**
	 * comma separated list of parameters, to be used to populate success
	 * message
	 */
	String[] successMessageParameters;

	/**
	 * if the sql fails to extract/update even a single row, should we flash any
	 * message?
	 */
	String failureMessageName;
	/**
	 * parameters to be used to format failure message
	 */
	String[] failureMessageParameters;

	/**
	 * should we stop this service in case the message added is of type error.
	 */
	boolean stopIfMessageTypeIsError;

	@Override
	protected Value doAct(ServiceContext ctx, DbDriver driver) {
		int result = this.doDbAct(ctx, driver);
		MessageType msgType = null;
		if (result == 0) {
			if (this.failureMessageName != null) {
				msgType = ctx.addMessage(this.failureMessageName,
						this.failureMessageParameters);
			}
		} else if (this.successMessageName != null) {
			msgType = ctx.addMessage(this.successMessageName,
					this.successMessageParameters);
		}
		if (this.stopIfMessageTypeIsError && msgType != null
				&& msgType == MessageType.ERROR) {
			return ActionBlock.STOP_VALUE;
		}
		return Value.newIntegerValue(result);
	}

	/**
	 * let the concrete action do its job.
	 *
	 * @param ctx
	 * @param driver
	 * @return
	 */
	protected abstract int doDbAct(ServiceContext ctx, DbDriver driver);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.simplity.tp.Action#validate(org.simplity.kernel.comp.ValidationContext
	 * , org.simplity.tp.Service)
	 */
	@Override
	public int validate(ValidationContext ctx, Service service) {
		int count = super.validate(ctx, service);
		if (this.failureMessageName != null) {
			ctx.addReference(ComponentType.MSG, this.failureMessageName);
		}
		if (this.successMessageName != null) {
			ctx.addReference(ComponentType.MSG, this.successMessageName);
		}
		return count;
	}
}
