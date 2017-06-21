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

import org.simplity.jms.JmsDestination;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * @author simplity.org
 *
 */
public class JmsProducer extends Action {

	/**
	 * queue/topic to be used to send a message as request
	 */
	JmsDestination requestJmsDestination;

	/**
	 * queue/topic to be used to get back a response. optional.
	 */
	JmsDestination responseJmsDestination;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#doAct(org.simplity.service.ServiceContext)
	 */
	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
		boolean allOk = this.requestJmsDestination.produce(ctx, this.responseJmsDestination);
		if (allOk) {
			return Value.VALUE_TRUE;
		}
		return Value.VALUE_FALSE;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Block#getReady(int)
	 */
	@Override
	public void getReady(int idx, Service service) {
		super.getReady(idx, service);

		this.requestJmsDestination.getReady();
		if (this.responseJmsDestination != null) {
			this.responseJmsDestination.getReady();
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
		if (this.requestJmsDestination == null) {
			vtx.addError("requestJmsObject is required");
			count++;
		} else {
			if (this.requestJmsDestination.getName() == null) {
				vtx.addError("name is required for requestJmsDestination");
				count++;
			}
			count += this.requestJmsDestination.validate(vtx, true);
		}
		if (this.responseJmsDestination != null) {
			count += this.responseJmsDestination.validate(vtx, false);
		}
		if(service.jmsUsage == null){
			vtx.addError("Service uses JMS but has not specified jmsUsage attribute.");
			count ++;
		}
		return count;
	}
}
