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

import org.simplity.jms.JmsQueue;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * @author simplity.org
 *
 */
public class JmsConsumer extends Block {
	/**
	 * queue from which to consume requests to be processed as requests
	 */
	JmsQueue requestQueue;
	/**
	 * optional queue on which responses to be sent on
	 */
	JmsQueue responseQueue;

	/**
	 * should we consume just one or all of them on the queue?
	 */
	boolean consumeAll;

	/**
	 * true means wait for the message. consume=false means wait for one, but then come out. consumeAll=true means keep listening till cows come home :-).
	 * false means do not wait, even for one.
	 */
	boolean waitForMessage;
	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#doAct(org.simplity.service.ServiceContext)
	 */
	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
		BlockWorker worker = new BlockWorker(this.actions, this.indexedActions, ctx, driver);
		this.requestQueue.consume(ctx, worker, this.responseQueue, this.consumeAll, this.waitForMessage);
		return Value.VALUE_TRUE;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Block#getReady(int)
	 */
	@Override
	public void getReady(int idx, Service service) {
		super.getReady(idx, service);
		this.requestQueue.getReady();
		if (this.responseQueue != null) {
			this.responseQueue.getReady();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Block#validate(org.simplity.kernel.comp.
	 * ValidationContext, org.simplity.tp.Service)
	 */
	@Override
	public int validate(ValidationContext vtx, Service service) {
		int count = super.validate(vtx, service);
		if (this.requestQueue == null) {
			vtx.addError("requestQueue is required");
			count++;
		} else {
			count += this.requestQueue.validate(vtx, true);
		}
		if (this.responseQueue != null) {
			count += this.responseQueue.validate(vtx, false);
		}
		if(service.jmsUsage == null){
			vtx.addError("Service uses JMS but has not specified jmsUsage attribute.");
			count ++;
		}
		return count;
	}
}
