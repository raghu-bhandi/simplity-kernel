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

import org.simplity.kernel.Tracer;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * service has actions that are executed in a sequence. JumpTo allows you to
 * change this sequence.
 *
 *
 * @author simplity.org
 *
 */
public class JumpTo extends org.simplity.tp.Action {

	/**
	 * returns either a name of action to go to, or "_stop", "_error",
	 * "_continue", "_break"
	 */
	String toAction;

	/**
	 * cached for performance
	 */
	private Value returnValue;

	@Override
	protected Value doAct(ServiceContext ctx) {
		Tracer.trace("Trying to jump with value = " + this.returnValue);
		return this.returnValue;
	}

	/**
	 *
	 * @return if this is for a signal, then signal, else null
	 */
	public boolean canJumpOut() {
		if (JumpSignal._BREAK.equals(this.toAction) || JumpSignal._STOP.equals(this.toAction)) {
			return true;
		}
		return false;
	}

	@Override
	public void getReady(int idx, Service service) {
		super.getReady(idx, service);
		this.returnValue = Value.newTextValue(this.toAction);
	}
}
