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
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.TextMessage;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.jms.JMSResponse;
import org.simplity.kernel.jms.JmsAgent;
import org.simplity.kernel.jms.JmsParms;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * Uses the connection, queue and session created by JMSSetUp to create an
 * instance of a JMSSender. This sender uses the setup instance to know which
 * connection to use.
 */

public class JmsReceive extends Action {
	String destination;
	String policy;

	private QueueReceiver queueReceiver;
	private QueueSession session = null;
	private JmsParms jmsp = null;

	public JmsReceive() {

	}
	
	@Override
	protected Value doAct(ServiceContext ctx) {
		Tracer.trace("entered JMSReceiver.createReceiver");
		try {
			jmsp = JmsAgent.setUp(destination, policy, null, null);
			Queue theQueue = jmsp.getQueue();
			session = jmsp.getSession();
			queueReceiver = session.createReceiver(theQueue);
		} catch (ApplicationError | JMSException jmsle) {
			throw new ApplicationError("Could not create a connection, session or sender");
		}
		JMSResponse jsmr = null;
		
		jsmr = receiveResponse();
		Tracer.trace("exited JMSReceiver.createReceiver");
		return super.doAct(ctx);
	}

	public QueueSession thisSession() throws ApplicationError {
		if (jmsp == null)
			throw new ApplicationError("JMSS-045-E JMSParms is null. You have not initialized the sender or receiver");
		else
			return jmsp.getSession();
	}

	public Object receive() throws ApplicationError {
		Tracer.trace("entered JMSReceive.receive");
		Object receiveMsg = null;
		try {
			Tracer.trace("Waiting for a message!");
			Message request = queueReceiver.receive();
			if (request instanceof TextMessage) {
				TextMessage trequest = (TextMessage) request;
				receiveMsg = trequest.getText();
			} else if (request instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) request;
				receiveMsg = objectMessage.getObject();
			} else {
				throw new ApplicationError("JMSS-044-E Received object was not a TextMessage or ObjectMessage");
			}
		} catch (JMSException jmse) {
			throw new ApplicationError("JMSS-020-E Could not create a JMS TextMessage or send the message on this session");
		}
		Tracer.trace("exited JMSReceive.receive");
		return receiveMsg;
	}

	public Object receive(long wait) throws ApplicationError {
		Tracer.trace("entered JMSReceive.receive(wait)");
		Object receiveMsg = null;
		try {
			Tracer.trace("Waiting for a message!");
			Message request = queueReceiver.receive(wait);
			if (request instanceof TextMessage) {
				TextMessage trequest = (TextMessage) request;
				receiveMsg = trequest.getText();
			} else if (request instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) request;
				receiveMsg = objectMessage.getObject();
			} else {
				throw new ApplicationError("JMSS-044-E Received object was not a TextMessage or ObjectMessage");
			}
		} catch (JMSException jmse) {
			throw new ApplicationError("JMSS-020-E Could not create a JMS TextMessage or send the message on this session");
		}
		Tracer.trace("exited JMSReceive.receive(wait)");
		return receiveMsg;
	}

	public Object receiveNoWait() throws ApplicationError {
		Tracer.trace("entered JMSReceive.receiveNoWait");
		Object receiveMsg = null;
		try {
			Tracer.trace("Waiting for a message!");
			Message request = queueReceiver.receiveNoWait();
			if (request instanceof TextMessage) {
				TextMessage trequest = (TextMessage) request;
				receiveMsg = trequest.getText();
			} else if (request instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) request;
				receiveMsg = objectMessage.getObject();
			} else {
				throw new ApplicationError("JMSS-044-E Received object was not a TextMessage or ObjectMessage");
			}
		} catch (JMSException jmse) {
			throw new ApplicationError("JMSS-020-E Could not create a JMS TextMessage or send the message on this session");
		}
		Tracer.trace("exited JMSReceive.receiveNoWait");
		return receiveMsg;
	}

	public JMSResponse receiveResponse() throws ApplicationError {
		JMSResponse message = new JMSResponse();
		Tracer.trace("entered JMSReceive.receiveResponse");
		Object receiveMsg = null;
		try {
			Tracer.trace("Waiting for a message!");
			Message request = queueReceiver.receive();
			if (request instanceof TextMessage) {
				TextMessage trequest = (TextMessage) request;
				receiveMsg = trequest.getText();
			} else if (request instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) request;
				receiveMsg = objectMessage.getObject();
			} else {
				throw new ApplicationError("JMSS-044-E Received object was not a TextMessage or ObjectMessage");
			}
			message.setCorrelationID(request.getJMSCorrelationID());
			message.setMessageID(request.getJMSMessageID());
			message.setMessage(receiveMsg);
			message.setReplyToQ((Queue) request.getJMSReplyTo());
			message.setRedelivered(request.getJMSRedelivered());
			message.setDeliveryCount(request.getIntProperty("JMSXDeliveryCount"));
		} catch (JMSException jmse) {
			throw new ApplicationError("JMSS-020-E Could not create a JMS TextMessage or send the message on this session");
		}
		Tracer.trace("exited JMSReceive.receiveResponse");
		return message;
	}

	public JMSResponse receiveResponse(long wait) throws ApplicationError {
		JMSResponse message = new JMSResponse();
		Tracer.trace("entered JMSReceive.receiveResponse(wait)");
		Object receiveMsg = null;
		try {
			Tracer.trace("Waiting for a message!");
			Message request = queueReceiver.receive(wait);
			if (request instanceof TextMessage) {
				TextMessage trequest = (TextMessage) request;
				receiveMsg = trequest.getText();
			} else if (request instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) request;
				receiveMsg = objectMessage.getObject();
			} else {
				throw new ApplicationError("JMSS-044-E Received object was not a TextMessage or ObjectMessage");
			}
			message.setCorrelationID(request.getJMSCorrelationID());
			message.setMessageID(request.getJMSMessageID());
			message.setMessage(receiveMsg);
			message.setReplyToQ((Queue) request.getJMSReplyTo());
			message.setRedelivered(request.getJMSRedelivered());
			message.setDeliveryCount(request.getIntProperty("JMSXDeliveryCount"));
		} catch (JMSException jmse) {
			throw new ApplicationError("JMSS-020-E Could not create a JMS TextMessage or send the message on this session");
		}
		Tracer.trace("exited JMSReceive.receiveResponse(wait)");
		return message;
	}

	public JMSResponse receiveNoWaitResponse() throws ApplicationError {
		JMSResponse message = new JMSResponse();
		Tracer.trace("entered JMSReceive.receiveNoWaitResponse");
		Object receiveMsg = null;
		try {
			Tracer.trace("Waiting for a message!");
			Message request = queueReceiver.receiveNoWait();
			if (request instanceof TextMessage) {
				TextMessage trequest = (TextMessage) request;
				receiveMsg = trequest.getText();
			} else if (request instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) request;
				receiveMsg = objectMessage.getObject();
			} else {
				throw new ApplicationError("JMSS-044-E Received object was not a TextMessage or ObjectMessage");
			}
			message.setCorrelationID(request.getJMSCorrelationID());
			message.setMessageID(request.getJMSMessageID());
			message.setMessage(receiveMsg);
			message.setReplyToQ((Queue) request.getJMSReplyTo());
			message.setRedelivered(request.getJMSRedelivered());
			message.setDeliveryCount(request.getIntProperty("JMSXDeliveryCount"));
		} catch (JMSException jmse) {
			throw new ApplicationError("JMSS-020-E Could not create a JMS TextMessage or send the message on this session");
		}
		Tracer.trace("exited JMSReceive.receiveNoWaitResponse");
		return message;
	}

	public void commit() throws ApplicationError {
		try {
			session.commit();
		} catch (JMSException jmse) {
			throw new ApplicationError("JMSS-034-E Could not do a session commit");
		}
	}

	public void rollback() throws ApplicationError {
		try {
			session.rollback();
		} catch (JMSException jmse) {
			throw new ApplicationError("JMSS-033-E Could not do a session rollback");
		}
	}

	public void closeSession() throws ApplicationError {
		try {
			session.close();
		} catch (JMSException jmse) {
			throw new ApplicationError("JMSS-032-E Could not do a session close");
		}
	}

}