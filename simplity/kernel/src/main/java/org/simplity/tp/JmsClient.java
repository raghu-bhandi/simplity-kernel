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

import java.util.UUID;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.simplity.kernel.jms.JmsConnector;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * @author simplity.org
 *
 */
public class JmsClient extends Action {
	/**
	 * name of the queue (destination) we are to consume and process
	 */
	String requestQueueName;
	/**
	 * what does the message contain?
	 */
	MessageContentType requestMessageType;

	/**
	 * if message content is fixedWidth, then the structure is to be specified
	 * in a record.
	 * Record may also be specified for json and XML if the input message
	 * content is to be validated
	 */
	String requestRecordName;

	String requestFields[];

	String requestMessageFormatter;

	boolean useTempQueue;

	String responseQueueName;

	MessageContentType reMessageType;

	String responseRecordName;

	String responseFields[];

	String responseMessaageExtractor;

	String correlationId;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#doAct(org.simplity.service.ServiceContext)
	 */
	@Override
	protected Value doAct(ServiceContext ctx) {
		/*
		 *
		 */
		Session session = null;
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		try {
			session = JmsConnector.getConnection().createSession(true, 0);
			Queue req = session.createQueue(this.requestQueueName);
			producer = session.createProducer(req);
			Queue resp = null;
			if (this.useTempQueue) {
				resp = session.createTemporaryQueue();
			} else if (this.responseQueueName != null) {
				resp = session.createQueue(this.responseQueueName);
			}
			if (resp != null) {
				consumer = session.createConsumer(resp);
			}

			Message msg = this.createMessage(session, ctx);
			if (consumer != null) {
				msg.setJMSReplyTo(resp);
				String corId = this.correlationId;
				if (corId == null) {
					corId = UUID.randomUUID().toString();
				}
				if (this.correlationId != null) {
					msg.setJMSCorrelationID(corId);
					consumer.setMessageListener(new MyReceiver(ctx));
				}
				producer.send(msg);
			}
			session.commit();
		} catch (Exception e) {
			//
		}finally{
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
	 * @return
	 */
	private Message createMessage(Session session, ServiceContext ctx) {
		// TODO Auto-generated method stub
		return null;
	}

	class MyReceiver implements MessageListener {
		private final ServiceContext ctx;

		MyReceiver(ServiceContext ctx) {
			this.ctx = ctx;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
		 */
		@Override
		public void onMessage(Message arg0) {
			/*
			 * extract response to ctx
			 */
		}
	}
}
