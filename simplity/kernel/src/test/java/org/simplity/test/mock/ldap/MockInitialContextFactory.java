package org.simplity.test.mock.ldap;

import java.util.Hashtable;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.mockito.Mockito;

public class MockInitialContextFactory implements InitialContextFactory {
	private static Context context;

	public Context getInitialContext(Hashtable<?, ?> arg0) throws NamingException {
		if (context == null) {
			context = Mockito.mock(Context.class);
			final QueueConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
			Mockito.when(context.lookup("vm://localhost?broker.persistent=false")).thenReturn(connectionFactory);
			try {
				QueueConnection queueConnection = (QueueConnection) connectionFactory.createConnection();
				QueueSession queueSession = queueConnection.createQueueSession(false, javax.jms.Session.DUPS_OK_ACKNOWLEDGE);
				queueConnection.start();
				Destination destination = queueSession.createQueue("jms/Queue01");
				Mockito.when(context.lookup("jms/Queue01")).thenReturn(destination);
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
		return context;
	}

}