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
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * add a message to the context
 *
 * @author simplity.org
 *
 */
public class AddMessage extends Action {
	/**
	 * mandatory name of a predefined message
	 */
	String messageName;
	/**
	 * list of parameters
	 */
	String[] parameters;
	/**
	 * should we stop this service in case the message added is of type error.
	 */

	@Override
	protected Value doAct(ServiceContext ctx) {
		MessageType msgType = ctx.addMessage(this.messageName, this.parameters);
		if (this.stopIfMessageTypeIsError && msgType == MessageType.ERROR) {
			return Service.STOP_VALUE;
		}
		return Value.VALUE_TRUE;
	}

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
		if (this.messageName == null) {
			ctx.addError("Message name is required for addMessage action");
			count++;
		} else {
			ctx.addReference(ComponentType.MSG, this.messageName);
		}
		return count;
	}
}
