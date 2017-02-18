package org.simplity.kernel.jms;

import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;

public class JmsAgent {
	static String jmss;
	static String destinationName;
	static String connectionFactory;
	static String acknowledgeMode;
	static boolean transacted;
	static String persistent;
	static String priority;
	static String timeToLive;
	static String initialContext;
	static String providerUrl;

	private QueueSession session = null;
	private Queue queue = null;

	public static void initialSetup(JmsSetup jmsSetup) {
		if (jmsSetup != null) {
			jmss = jmsSetup.jmss;
			destinationName = jmsSetup.destinationName;
			connectionFactory = jmsSetup.connectionFactory;
			acknowledgeMode = jmsSetup.acknowledgeMode;
			transacted = Boolean.getBoolean(jmsSetup.transacted);
			persistent = jmsSetup.persistent;
			priority = jmsSetup.priority;
			timeToLive = jmsSetup.timeToLive;
			initialContext = jmsSetup.initialContext;
			providerUrl = jmsSetup.providerUrl;
			Tracer.trace("Setting up the JMS Agent");
		}
	}

	/**
	 * @param destination
	 * @param policy
	 * @param queue
	 * @return a JMSParms with the values needed by the sender or receiver
	 *         instance. We don't want to keep those values in the setup
	 *         instance since other queues may be created off this setup
	 *         instance.
	 * @throws ApplicationError
	 */
	public static JmsParms setUp(String destination, String policy, Queue queue, QueueSession queuesession)
			throws ApplicationError {
		JmsParms jmsp = new JmsParms();

		QueueConnectionFactory theFactory = null;
		Context qctx = null;
		QueueSession session = null;
		QueueConnection theConn = null;

		Tracer.trace("entered JMSSetUp.setUp");

		// get the objects from jndi
		// For running from JUnit or an application client you
		// can put the appropriate values in your properties file.

		try { // this is for running in a WAS container
			if (initialContext == null || providerUrl == null) {
				qctx = new InitialContext();
			} else { // this is for JUnit testing
				Hashtable env = new Hashtable();
				env.put(Context.INITIAL_CONTEXT_FACTORY, initialContext);
				env.put(Context.PROVIDER_URL, providerUrl);
				qctx = new InitialContext(env);
			}
			// create a connection
			if (theConn == null)
				theFactory = (QueueConnectionFactory) qctx.lookup(connectionFactory);
			// create the queue
			Tracer.trace("Queue name: " + destinationName);
			if (queue == null)
				jmsp.setQueue((Queue) qctx.lookup(destinationName));
			else
				jmsp.setQueue(queue);
		} catch (NamingException nme) {
			throw new ApplicationError(" Could not create a connection factory or destination");
		}
		// make the connection
		try {

			Tracer.trace("transacted: " + transacted);
			if (theConn == null) {
				theConn = theFactory.createQueueConnection();
				// you must start the connection for the receive
				theConn.start();
			}

			if (queuesession == null) {
				if (acknowledgeMode.equals("client")) {
					session = theConn.createQueueSession(transacted, QueueSession.CLIENT_ACKNOWLEDGE);
				} else if (acknowledgeMode.equals("dups")) {
					session = theConn.createQueueSession(transacted, QueueSession.DUPS_OK_ACKNOWLEDGE);
				} else {
					session = theConn.createQueueSession(transacted, QueueSession.AUTO_ACKNOWLEDGE);
				}
			} else
				session = queuesession;
		} catch (JMSException jmse) {
			throw new ApplicationError("Could not create a connection, session or sender");

		}
		jmsp.setPersistent(persistent);
		jmsp.setTimetolive(timeToLive);
		jmsp.setPriority(priority);
		jmsp.setSession(session);
		Tracer.trace("exited JMSSetUp.setUp");
		return jmsp;
	}
	
    public static JmsParms setUp(String policy)
            throws ApplicationError
        {
            String persistent = null;
            String priority = null;
            String timetolive = null;
            JmsParms jmsp = new JmsParms();

            jmsp.setPersistent(persistent);
            jmsp.setTimetolive(timetolive);
            jmsp.setPriority(priority);
            return jmsp;
        }
    
    public static Queue setUpReplyTo(String destination)
            throws ApplicationError
        {
            Queue queue = null;
            String destinationName = null;
            String initialcontext = null;
            String providerurl = null;
            Context qctx = null;
            try
            {
                if(initialcontext == null || providerurl == null)
                {
                    qctx = new InitialContext();
                } else
                {
                    Hashtable env = new Hashtable();
                    env.put("java.naming.factory.initial", initialcontext);
                    env.put("java.naming.provider.url", providerurl);
                    qctx = new InitialContext(env);
                }
                queue = (Queue)qctx.lookup(destinationName);
            }
            catch(NamingException nme)
            {
                throw new ApplicationError("JMSS-040-E,Could not create a connection factory or destination");
            }
            return queue;
        }

}
