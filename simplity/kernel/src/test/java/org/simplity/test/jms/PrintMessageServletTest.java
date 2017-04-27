package org.simplity.test.jms;

import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.InitialContext;

import org.junit.Before;
import org.junit.Test;

import com.mockrunner.jms.BasicJMSTestCaseAdapter;
import com.mockrunner.mock.jms.MockObjectMessage;
import com.mockrunner.mock.jms.MockQueue;
import com.mockrunner.mock.jms.MockQueueConnection;
import com.mockrunner.mock.jms.MockQueueConnectionFactory;
import com.mockrunner.mock.jms.MockQueueSession;
import com.mockrunner.mock.jms.MockTextMessage;

public class PrintMessageServletTest extends BasicJMSTestCaseAdapter {
	private MockQueue queue;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		InitialContext initialContext = new InitialContext();
		MockQueueConnectionFactory queueFactory = new MockQueueConnectionFactory(getDestinationManager(),
				getConfigurationManager());
		// (QueueConnectionFactory)initialContext.lookup("java:ConnectionFactory");
		QueueConnection queueConnection = queueFactory.createQueueConnection();
		QueueSession queueSession = queueConnection.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		Queue queue = queueSession.createQueue("queue/testQueue");
		QueueReceiver receiver = queueSession.createReceiver(queue);
		initialContext.bind("queue/testQueue", queue);
		// receiver.setMessageListener(new PrintMessageListener());
		queue = getDestinationManager().createQueue("testQueue");
	}

	@Test
	public void testInitPrintMessageReceiver() throws Exception {
		verifyQueueConnectionStarted();
		verifyNumberQueueSessions(1);
		verifyNumberQueueReceivers(0, "testQueue", 1);
		QueueReceiver receiver = getQueueTransmissionManager(0).getQueueReceiver("testQueue");
		QueueSender sender = getQueueTransmissionManager(0).createQueueSender(queue);
		verifyNumberOfReceivedQueueMessages("testQueue", 3);
		verifyReceivedQueueMessageEquals("testQueue", 0, new MockTextMessage("1"));
		verifyReceivedQueueMessageEquals("testQueue", 1, new MockTextMessage("2"));
		verifyReceivedQueueMessageEquals("testQueue", 2, new MockTextMessage("3"));

		sender.send(new MockObjectMessage(new Integer(3)));

		verifyAllReceivedQueueMessagesAcknowledged("testQueue");
		verifyNumberOfReceivedQueueMessages("testQueue", 4);
		verifyReceivedQueueMessageAcknowledged("testQueue", 3);
		verifyNumberOfCurrentQueueMessages("testQueue", 0);
	}

}