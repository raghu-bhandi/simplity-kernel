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

/**
 * @author simplity.org
 *
 */

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

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

public class JmsSend extends Action {
	String destination;
	String policy;
	String message;
	String requestService;
	String messageID;

	private QueueSender queueSender = null;
	private QueueSession session = null;
	private JmsParms jmsp = null;

	public JmsSend() {

	}

	@Override
	protected Value doAct(ServiceContext ctx) {
		Tracer.trace("entered JMSSender.createSender");
		try {
			jmsp = JmsAgent.setUp(destination, policy, null, null);
			// setUp will get the right queue and session for us
			Queue theQueue = jmsp.getQueue();
			session = jmsp.getSession();
			// create the sender
			queueSender = session.createSender(theQueue);
		} catch (ApplicationError | JMSException jmsle) {
			throw new ApplicationError("Could not create a connection, session or sender");
		}
		JMSResponse jsmr = null;
		if (requestService != null) {
			jsmr = sendRequest(message, requestService);
		}
		if (messageID != null) {
			sendReply(message, messageID);
		}
		if (policy != null)
			send(message, policy);
		else
			send(message);
		return super.doAct(ctx);
	}

	public void send(Object object) throws ApplicationError {

		Tracer.trace("entered JMSSender.send");

		Message message = null;
		String persistent = jmsp.getPersistent();
		String priority = jmsp.getPriority();
		String timetolive = jmsp.getTimetolive();

		try {

			// create the message
			if (object instanceof String)
				message = session.createTextMessage((String) object);
			else
				// object must implement the java.io.Serializable interface
				message = session.createObjectMessage((java.io.Serializable) object);
			// send the message, defaults for this version:
			// persistent, priority (0-4 is normal with 4 high normal, 5-9 is
			// expedited) and
			// timeToLive are policies set in the properties file.
			if (persistent.equals("yes"))
				queueSender.send(message, DeliveryMode.PERSISTENT, Integer.parseInt(priority),
						Integer.parseInt(timetolive));
			else
				queueSender.send(message, DeliveryMode.NON_PERSISTENT, Integer.parseInt(priority),
						Integer.parseInt(timetolive));
		} catch (JMSException jmse) {
			throw new ApplicationError("Could not create a JMS TextMessage or send the message on this session");
		}

		Tracer.trace("exited JMSSender.send");

	}

	public void send(Object object, String policy) throws ApplicationError {

		Tracer.trace("entered JMSSender.send with policy");

		Message message = null;
		JmsParms jmsptemp = JmsAgent.setUp(policy);
		String persistent = jmsptemp.getPersistent();
		String priority = jmsptemp.getPriority();
		String timetolive = jmsptemp.getTimetolive();

		try {
			// create the message
			if (object instanceof String)
				message = session.createTextMessage((String) object);
			else
				// object must implement the java.io.Serializable interface
				message = session.createObjectMessage((java.io.Serializable) object);
			if (persistent.equals("yes"))
				queueSender.send(message, DeliveryMode.PERSISTENT, Integer.parseInt(priority),
						Integer.parseInt(timetolive));
			else
				queueSender.send(message, DeliveryMode.NON_PERSISTENT, Integer.parseInt(priority),
						Integer.parseInt(timetolive));
		} catch (JMSException jmse) {
			throw new ApplicationError("Could not create a JMS TextMessage or send the message on this session");
		}

		Tracer.trace("exited JMSSender.send with policy");

	}

	public JMSResponse sendRequest(Object object, String service) throws ApplicationError {
		Tracer.trace("entered JMSSender.sendRequest");

		Message message = null;
		String persistent = jmsp.getPersistent();
		String priority = jmsp.getPriority();
		String timetolive = jmsp.getTimetolive();
		Queue replyTo = JmsAgent.setUpReplyTo(service);

		JMSResponse jmsr = new JMSResponse();

		try {

			// create the message
			if (object instanceof String) {
				message = session.createTextMessage((String) object);
			} else { // object must implement the java.io.Serializable interface
				message = session.createObjectMessage((java.io.Serializable) object);
			}

			message.setJMSReplyTo(replyTo);
			jmsr.setMessageID(message.getJMSMessageID());
			if (persistent.equals("yes"))
				queueSender.send(message, DeliveryMode.PERSISTENT, Integer.parseInt(priority),
						Integer.parseInt(timetolive));
			else
				queueSender.send(message, DeliveryMode.NON_PERSISTENT, Integer.parseInt(priority),
						Integer.parseInt(timetolive));
		} catch (JMSException jmse) {
			throw new ApplicationError("Could not create a JMS TextMessage or send the message on this session");
		}

		Tracer.trace("exited JMSSender.sendRequest");

		return jmsr;

	}

	public void sendReply(Object object, String messageID) throws ApplicationError {

		Tracer.trace("entered JMSSender.sendReply");
		Message message = null;
		String persistent = jmsp.getPersistent();
		String priority = jmsp.getPriority();
		String timetolive = jmsp.getTimetolive();
		try {

			// create the message
			if (object instanceof String)
				message = session.createTextMessage((String) object);
			else
				// object must implement the java.io.Serializable interface
				message = session.createObjectMessage((java.io.Serializable) object);
			message.setJMSCorrelationID(messageID);
			if (persistent.equals("yes"))
				queueSender.send(message, DeliveryMode.PERSISTENT, Integer.parseInt(priority),
						Integer.parseInt(timetolive));
			else
				queueSender.send(message, DeliveryMode.NON_PERSISTENT, Integer.parseInt(priority),
						Integer.parseInt(timetolive));
		} catch (JMSException jmse) {
			throw new ApplicationError(" Could not create a JMS TextMessage or send the message on this session");
		}
		Tracer.trace("exited JMSSender.sendReply");
	}

	public void commit() throws ApplicationError {
		try {
			session.commit();
		} catch (JMSException jmse) {
			throw new ApplicationError("Could not do a session commit");

		}

	}

	public void rollback() throws ApplicationError {
		try {
			session.rollback();
		} catch (JMSException jmse) {
			throw new ApplicationError("Could not do a session rollback");
		}
	}

	public void closeSession() throws ApplicationError {
		try {
			session.close();
		} catch (JMSException jmse) {
			throw new ApplicationError("Could not do a session close");
		}
	}

}