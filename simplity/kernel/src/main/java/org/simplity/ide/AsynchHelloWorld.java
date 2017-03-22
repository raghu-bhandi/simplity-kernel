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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.ide;

import org.simplity.kernel.Tracer;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

/**
 * @author simplity.org
 *
 */
public class AsynchHelloWorld implements LogicInterface {

	/* (non-Javadoc)
	 * @see org.simplity.tp.LogicInterface#execute(org.simplity.service.ServiceContext)
	 */
	@Override
	public Value execute(ServiceContext arg0) {
		// We want to put some arbitrary delay to simulate an external task
		long l = Math.round(10000 * Math.random());
		Tracer.trace("James Bond " + "started, but will take a nap for " + l + "ns");
		try {
			Thread.sleep(l);
		} catch (InterruptedException e) {
			Tracer.trace("James Bond is interrupted. Can you beleive that!!!");
			return Value.VALUE_FALSE;
		}
		Tracer.trace("My Name is Bond.. James Bond-" + l);
		return Value.VALUE_TRUE;
	}
}
