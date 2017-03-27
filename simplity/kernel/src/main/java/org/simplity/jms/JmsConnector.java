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

package org.simplity.jms;

import java.util.Properties;

import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Property;
import org.simplity.kernel.Tracer;

/**
 * class that manages to provide desired jmsSession for jms clients. This is
 * similar to DbDriver in its functionality
 *
 * @author simplity.org
 *
 */
public class JmsConnector {

	/**
	 * non-jta connection
	 */
	private static QueueConnectionFactory factory;

	/**
	 * for jta-managed connection
	 */
	private static QueueConnectionFactory xaFactory;

	/**
	 * initial setup. Called by Application on startup
	 *
	 * @param queueConnectionFactory
	 * @param xaQueueConnectionFactory
	 * @param properties
	 *            additional properties like user name etc.. that are required
	 *            to be set to teh context for getting the connection
	 * @return error message in case of error. null if all OK
	 *
	 * @throws ApplicationError
	 *             : in case of any issue with the set-up
	 */
	public static String setup(String queueConnectionFactory, String xaQueueConnectionFactory, Property[] properties) {
		Context ctx = null;

		try {
			if(properties != null && properties.length > 0){
				Properties env = new Properties();
				for (Property property: properties){
					env.put(property.getName(), property.getValue());
				}
				ctx = new InitialContext(env);
			}else{
				ctx = new InitialContext();
			}
			if (queueConnectionFactory != null) {
				factory = (QueueConnectionFactory) ctx.lookup(queueConnectionFactory);
				Tracer.trace("queueConnectionFactory successfully set to " + factory.getClass().getName());
			}
			if (xaQueueConnectionFactory != null) {
				xaFactory = (QueueConnectionFactory) ctx.lookup(xaQueueConnectionFactory);
				Tracer.trace("xaQueueConnectionFactory successfully set to " + xaFactory.getClass().getName());
			}
		} catch (Exception e) {
			return e.getMessage();
		}
		return null;
	}

	/**
	 * get a JMS connection. And, please, please do not close() it or abandon
	 * it. Do return it once you are done. I am dependent on
	 * your discipline at this time to avoid memory leakage
	 *
	 * @param jmsUsage
	 *
	 * @return connection
	 */
	public static JmsConnector borrowConnector(JmsUsage jmsUsage) {
		try {
			QueueConnection con = null;
			boolean transacted = false;
			QueueSession session = null;
			if (jmsUsage == JmsUsage.EXTERNALLY_MANAGED) {
				if (xaFactory == null) {
					throw new ApplicationError("Application is not set up for JMS with JTA/JCA/XA");
				}
				con = xaFactory.createQueueConnection();
			} else {
				if (factory == null) {
					throw new ApplicationError("Application is not set up for JMS local session managed operations");
				}
				con = factory.createQueueConnection();
				if (jmsUsage == JmsUsage.SERVICE_MANAGED) {
					transacted = true;
				}
			}
			session = con.createQueueSession(transacted, Session.AUTO_ACKNOWLEDGE);
			/*
			 * not very well advertised.. but this method is a MUST for
			 * consuming queues, though production works without that
			 */
			con.start();
			return new JmsConnector(con, session, jmsUsage);
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while creating jms session");
		}
	}

	/**
	 *
	 * @param connector
	 * @param allOk
	 */
	public static void returnConnector(JmsConnector connector, boolean allOk) {
		connector.close(allOk);
	}

	/**
	 * jndi name of queueConnection factory non-JTA connection
	 */
	private final QueueConnection connection;

	/**
	 * jndi name of queueConnection factory non-JTA connection
	 */
	private final QueueSession session;

	/**
	 * usage for which this instance is created
	 */
	JmsUsage jmsUsage;

	/**
	 * @param con
	 * @param session
	 * @param jmsUsage
	 */
	private JmsConnector(QueueConnection con, QueueSession session, JmsUsage jmsUsage) {
		this.connection = con;
		this.session = session;
		this.jmsUsage = jmsUsage;

	}

	private void close(boolean allOk) {
		try {
			if (this.jmsUsage == JmsUsage.SERVICE_MANAGED) {
				if (allOk) {
					Tracer.trace("Jms session committed.");
					this.session.commit();
				} else {
					Tracer.trace("Jms session rolled-back.");
					this.session.rollback();
				}
			} else {
				Tracer.trace("non-transactional JMS session closed.");
			}
		} catch (Exception e) {
			throw new ApplicationError(e, "error while closing jms conenction");
		}
		try {
			this.session.close();
		} catch (Exception ignore) {
			//
		}
		try {
			this.connection.close();
		} catch (Exception ignore) {
			//
		}
	}

	/**
	 * @return session associated with this connector
	 */
	public Session getSession() {
		return this.session;
	}
}
