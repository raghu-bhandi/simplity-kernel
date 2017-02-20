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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.jms.JmsConnector;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * @author simplity.org
 *
 */
public class JmsProcessor extends Block {

	/**
	 * name of the queue (destination) we are to consume and process
	 */
	String queueName;

	/**
	 * optional message selector to receive a subset of messages from the above queue
	 */
	String messageSelector;
	/**
	 * what does the message contain?
	 */
	MessageContentType messageType;

	/**
	 * if message content is fixedWidth, then the structure is to be specified
	 * in a record.
	 * Record may also be specified for json and XML if the input message
	 * content is to be validated
	 */
	String recordName;

	/**
	 * if message type is object, and if you want to extracted data from that
	 * into the service context, you may provide the class that implements
	 * OjectToData interface
	 */
	String messageExtractor;

	/* (non-Javadoc)
	 * @see org.simplity.tp.Action#delegate(org.simplity.service.ServiceContext, org.simplity.kernel.db.DbDriver)
	 */
	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
		Session session = null;
		MessageConsumer  consumer = null;
		try {
			session = JmsConnector.getConnection().createSession(true, 0);
			Queue q = session.createQueue(this.queueName);
			consumer = session.createConsumer(q, this.messageSelector);
			while (true){
				Message msg = consumer.receive();
				if(msg == null){
					break;
				}
				boolean allOk = false;
				try{
					allOk = this.handleAMessage(msg, ctx, ctx, driver);
				}catch(Exception e){
					Tracer.trace("Message processor threw an excpetion " + e.getMessage());
				}
				if(allOk){
					session.commit();
				}else{
					Tracer.trace("Rolling back message as the processor crashed/returned a flase value");
					session.rollback();
				}
			}
		} catch (JMSException e) {
			throw new ApplicationError(e, "Error while creating JMS session for action " + this.getName());

		}finally{
			if(consumer != null){
				try{
					consumer.close();
				}catch(Exception ignore){
					//
				}
			}
			if(session != null){
				try{
					session.close();
				}catch(Exception ignore){
					//
				}
			}
		}
		return Value.VALUE_TRUE;
	}

	/**
	 * @param ctx
	 * @param ctx2
	 * @param driver
	 */
	private boolean handleAMessage(Message message, ServiceContext ctx, ServiceContext ctx2,
			DbDriver driver) {
		if(this.recordName != null){
			/*
			 * extract using record
			 */
			ctx.setTextValue("message", message.toString());
		}
		BlockWorker worker = new BlockWorker(this.actions,
				this.indexedActions, ctx);
		worker.execute(driver);
		/*
		 * extract data from message
		 */
		return true;
	}
}
