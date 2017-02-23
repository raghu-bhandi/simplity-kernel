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

import javax.jms.Session;

import org.simplity.jms.Connector;
import org.simplity.jms.JmsQueue;
import org.simplity.kernel.Tracer;
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
	 * queue to be used to send a message as request
	 */
	JmsQueue requestQueue;

	/**
	 * queue to be used to get back a response. optional.
	 */
	JmsQueue responseQueue;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#doAct(org.simplity.service.ServiceContext)
	 */
	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
		/*
		 *
		 */
		Session session = null;
		boolean allOk = false;
		try {
			session = Connector.getSession();
			allOk = this.requestQueue.produce(ctx, session, this.responseQueue);
			if(allOk){
				session.commit();
			}else{
				Tracer.trace("JMS session rolled back because the producer had an issue.");
				session.rollback();
			}
		} catch (Exception e) {
			Tracer.trace("Error while sending message.");
			if(session != null){
				try{
					session.rollback();
				}catch(Exception ignore){
					//
				}
			}
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
		return Value.VALUE_TRUE;
	}

	/* (non-Javadoc)
	 * @see org.simplity.tp.Block#getReady(int)
	 */
	@Override
	public void getReady(int idx) {
		super.getReady(idx);
		this.requestQueue.getReady();
		if(this.responseQueue != null){
			this.responseQueue.getReady();
		}
	}

	/* (non-Javadoc)
	 * @see org.simplity.tp.Block#validate(org.simplity.kernel.comp.ValidationContext, org.simplity.tp.Service)
	 */
	@Override
	public int validate(ValidationContext vtx, Service service) {
		int count = super.validate(vtx, service);
		count += this.requestQueue.validate(vtx);
		if(this.responseQueue != null){
			count += this.responseQueue.validate(vtx);
		}
		return count;
	}
}
