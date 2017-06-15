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

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
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
	 * non-jta queue connection
	 */
	private static QueueConnectionFactory queueConnectionFactory;

	/**
	 * for jta-managed queue connection
	 */
	private static QueueConnectionFactory xaQueueConnectionFactory;

	/**
	 * non-jta connection
	 */
	private static TopicConnectionFactory topicConnectionFactory;

	/**
	 * for jta-managed connection
	 */
	private static TopicConnectionFactory xaTopicConnectionFactory;
	
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
	public static String setup(String queueFactory, String xaQueueFactory, String topicFactory, String xaTopicFactory, Property[] properties) {
		Context ctx = null;

		try {
			if (properties != null && properties.length > 0) {
				Properties env = new Properties();
				for (Property property : properties) {
					env.put(property.getName(), property.getValue());
				}
				ctx = new InitialContext(env);
			} else {
				ctx = new InitialContext();
			}
			
			if (queueFactory != null) {
				queueConnectionFactory = (QueueConnectionFactory) ctx.lookup(queueFactory);
				Tracer.trace("queueConnectionFactory successfully set to " + queueConnectionFactory.getClass().getName());
			}
			
			if (xaQueueFactory != null) {
				xaQueueConnectionFactory = (QueueConnectionFactory) ctx.lookup(xaQueueFactory);
				Tracer.trace("xaQueueConnectionFactory successfully set to " + xaQueueConnectionFactory.getClass().getName());
			}
			
			if (topicFactory != null) {
				topicConnectionFactory = (TopicConnectionFactory) ctx.lookup(topicFactory);
				Tracer.trace("topicConnectionFactory successfully set to " + topicConnectionFactory.getClass().getName());
			}
			
			if (xaTopicFactory != null) {
				xaTopicConnectionFactory = (TopicConnectionFactory) ctx.lookup(xaTopicFactory);
				Tracer.trace("xaTopicConnectionFactory successfully set to " + xaTopicConnectionFactory.getClass().getName());
			}
		} catch (Exception e) {
			return e.getMessage();
		}
		return null;
	}

	/**
	 * get a JMS connection for repeated use across multiple transactions.
	 * caller can issue start(), commit() rollBack() etc..
	 * @param jmsUsage
	 *
	 * @return connection
	 */
	public static JmsConnector borrowMultiTransConnector(JmsUsage jmsUsage) {
		return borrow(jmsUsage, true);
	}

	/**
	 *
	 * get a JMS connection. And, please, please do not close() it or abandon
	 * it. Do return it once you are done. I am dependent on
	 * your discipline at this time to avoid memory leakage
	 *
	 * @param jmsUsage
	 *
	 * @return connection
	 */
	public static JmsConnector borrowConnector(JmsUsage jmsUsage) {
		return borrow(jmsUsage, false);
	}

	private static JmsConnector borrow(JmsUsage jmsUsage, boolean multi) {

		try {
			QueueConnection queueConnection = null;
			TopicConnection topicConnection = null;
			boolean transacted = false;
			QueueSession queueSession = null;
			TopicSession topicSession = null;
			if (jmsUsage == JmsUsage.EXTERNALLY_MANAGED) {
				if (xaQueueConnectionFactory == null && xaTopicConnectionFactory == null) {
					throw new ApplicationError("Application is not set up for JMS with JTA/JCA/XA");
				} else if(xaQueueConnectionFactory != null) {
					queueConnection = xaQueueConnectionFactory.createQueueConnection();
				} else if(xaTopicConnectionFactory != null) {
					topicConnection = xaTopicConnectionFactory.createTopicConnection();
				}
			} else {
				if (queueConnectionFactory == null && topicConnectionFactory == null) {
					throw new ApplicationError("Application is not set up for JMS local session managed operations");
				} else if(queueConnectionFactory != null) {
					queueConnection = queueConnectionFactory.createQueueConnection();
				} else if(topicConnectionFactory != null) {
					topicConnection = topicConnectionFactory.createTopicConnection();
				}
				
				if (jmsUsage == JmsUsage.SERVICE_MANAGED) {
					transacted = true;
				}
			}
			
			if(queueConnection != null) {
				queueSession = queueConnection.createQueueSession(transacted, Session.AUTO_ACKNOWLEDGE);
				/*
				 * not very well advertised.. but this method is a MUST for
				 * consuming queues, though production works without that
				 */
				queueConnection.start();
			}
			if(topicConnection != null) {
				topicSession = topicConnection.createTopicSession(transacted, Session.AUTO_ACKNOWLEDGE);
				/*
				 * not very well advertised.. but this method is a MUST for
				 * consuming queues, though production works without that
				 */
				topicConnection.start();
			}
			return new JmsConnector(queueConnection, topicConnection, queueSession, topicSession, jmsUsage, multi);
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
	private final QueueConnection queueConnection;

	/**
	 * jndi name of queueConnection factory non-JTA connection
	 */
	private final TopicConnection topicConnection;
	
	/**
	 * jndi name of queueConnection factory non-JTA connection
	 */
	private final QueueSession queueSession;

	/**
	 * jndi name of queueConnection factory non-JTA connection
	 */
	private final TopicSession topicSession;
	
	/**
	 * usage for which this instance is created
	 */
	private JmsUsage jmsUsage;

	private boolean forMultiTrans;

	/**
	 * @param queueConnection
	 * @param topicConnection
	 * @param queueSession
	 * @param topicSession
	 * @param jmsUsage
	 */
	private JmsConnector(QueueConnection queueConnection, TopicConnection topicConnection, 
			QueueSession queueSession, TopicSession topicSession, JmsUsage jmsUsage, boolean multi) {
		this.queueConnection = queueConnection;
		this.queueSession = queueSession;
		this.topicConnection = topicConnection;
		this.topicSession = topicSession;
		this.jmsUsage = jmsUsage;
		this.forMultiTrans = multi;
	}

	private void close(boolean allOk) {
		if (this.forMultiTrans == false) {
			try {
				if (this.jmsUsage == JmsUsage.SERVICE_MANAGED) {
					if (allOk) {
						Tracer.trace("Jms session committed.");
						if(this.queueSession != null) {
							this.queueSession.commit();
						} else if(this.topicSession != null) {
							this.topicSession.commit();
						}
					} else {
						Tracer.trace("Jms session rolled-back.");
						if(this.queueSession != null) {
							this.queueSession.rollback();
						} else if(this.topicSession != null) {
							this.topicSession.rollback();
						}
					}
				} else {
					Tracer.trace("non-transactional JMS session closed.");
				}
			} catch (Exception e) {
				throw new ApplicationError(e, "error while closing jms conenction");
			}
		}
		try {
			if(this.queueSession != null) {
				this.queueSession.close();
			} else if(this.topicSession != null) {
				this.topicSession.close();
			}
		} catch (Exception ignore) {
			//
		}
		try {
			if(this.queueConnection != null) {
				this.queueConnection.close();
			} else if(this.topicConnection != null) {
				this.topicConnection.close();
			}
		} catch (Exception ignore) {
			//
		}
	}

	private void checkMulti(){
		if(this.forMultiTrans == false){
			throw new ApplicationError("Jms connection is borrowed for a single transaciton, but is used to manage transactions.");
		}
	}

	/**
	 * commit current transaction. Valid only if the connection is borrowed for multi-trnsactions
	 * @throws JMSException
	 */
	public void commit() throws JMSException{
		this.checkMulti();
		if(this.queueSession != null) {
			this.queueSession.commit();
		} else if(this.topicSession != null) {
			this.topicSession.commit();
		}
	}
	/**
	 * roll-back current transaction. Valid only if the connection is borrowed for multi-trnsactions
	 * @throws JMSException
	 */
	public void rollback() throws JMSException{
		this.checkMulti();
		if(this.queueSession != null) {
			this.queueSession.rollback();
		} else if(this.topicSession != null) {
			this.topicSession.rollback();
		}
	}
	/**
	 * @return session associated with this connector
	 */
	public Session getSession() {
		if(this.queueSession != null) {
			return this.queueSession;
		} else if(this.topicSession != null) {
			return this.topicSession;
		}
		return null;
	}
}
