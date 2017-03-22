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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSerializationType;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.value.Value;
import org.simplity.service.DataExtractor;
import org.simplity.service.DataFormatter;
import org.simplity.service.ServiceContext;

/**
 * A data structure that capture all details of a JmsQueue
 *
 * @author simplity.org
 *
 */
public class JmsQueue {

	/**
	 * name of the queue (destination) used for requesting a service
	 */
	String queueName;
	/**
	 * null if message body is not used, but header parameters are used to
	 * transport data.
	 * How the body of the message is used to transport data.
	 */
	DataSerializationType messageBodyType;
	/**
	 * field that is associated with body. Entire body is
	 * assigned-to/retrieved-from this field
	 */
	String bodyFieldName;
	/**
	 * comma separated list of fields that supply/receive data.
	 */
	String fieldNames[];
	/**
	 * data structure on which this message data is based on
	 */
	String recordName;
	/**
	 * just extract all fields with no validation, if this queue is being
	 * consumed
	 */
	boolean extractAll;
	/**
	 * java class that implements org.simplity.service.DataFormatter interface.
	 * message body text is formatted using this class.
	 */
	String messageFormatter;
	/**
	 * java class that implements org.simplity.service.DataExtractor interface
	 * to
	 * extract data from message body text
	 */
	String messageExtractor;

	/**
	 * sheet name with two columns, first one name, second one value, both text.
	 * bodyMessageType must e set to COMMA_SEPARATED_PAIRS. Consumer extracts
	 * into this sheet, while producer formats the text using data in this
	 * sheet
	 */
	String nameValueSheetName;
	/**
	 * subset of messages that we are interested. As per JMS selector syntax.
	 */
	String messageSelector;
	/**
	 * message type to the message header. Use this ONLY if the provider insists
	 * on this, or your application uses this.
	 */
	String messageType;

	/**
	 * object instance for re-use
	 */
	private DataFormatter dataFormatter;
	/**
	 * object instance for re-use
	 */
	private DataExtractor dataExtractor;
	/**
	 * jms queue instance for this queue
	 */
	private Queue queue;

	/**
	 * consume a request queue, and optionally put a message on to the response
	 * queue. Keep doing this for messages in the request queue, till the queue
	 * is closed, or the processor signals a shut-down
	 *
	 * @param ctx
	 *            service context where all this is happening
	 *
	 * @param processor
	 *            object instance that is interested in processing the message
	 * @param responseQ
	 *            optional response queue to be used to respond back to the
	 *            incoming message
	 * @param consumeAll
	 *            false means we will process (at most) one message. true means
	 *            no such restrictions.
	 * @param waitForMessage
	 *            true means we will wait for at least the first message. If
	 *            consumeAll is true, then we do not come-out till interrupted,
	 *            or the queue closes
	 */
	public void consume(ServiceContext ctx, MessageClient processor, JmsQueue responseQ, boolean consumeAll,
			boolean waitForMessage) {

		Session session = ctx.getJmsSession();
		MessageConsumer consumer = null;
		MessageProducer producer = null;

		try {
			consumer = session.createConsumer(this.queue, this.messageSelector);
			String nam = this.queue.getQueueName();
			Tracer.trace("Started consumer for queue " + nam);
			/*
			 * We may not use producer at all, but an empty producer does not
			 * hurt as much as creating it repeatedly..
			 */
			producer = session.createProducer(null);
			/*
			 * wait 0 means blocking-wait, 1 means try and come out.
			 */
			long wait = waitForMessage ? 0 : 1;
			/*
			 * loop for each message.
			 */
			do {
				if(waitForMessage){
					Tracer.trace("Looking/waiting for next message on " + nam);
				}
				Message msg = consumer.receive(wait);
				if (msg == null) {
					Tracer.trace("No message in " + this.queueName + ". Queue consumer will not continue;");
					/*
					 * queue is shut down
					 */
					break;
				}
				/*
				 * let exception in one message not affect the over-all process
				 */
				try {
					/*
					 * data content of message is extracted into ctx
					 */
					this.extractMessage(msg, ctx);
					/*
					 * is the requester asking us to respond on a specific
					 * queue?
					 */
					Destination replyQ = msg.getJMSReplyTo();
					/*
					 * and the all important correlation id for the requester to
					 * select the message back
					 */
					String corId = msg.getJMSCorrelationID();
					if (replyQ == null && responseQ != null) {
						replyQ = responseQ.getQueue();
					}

					processor.process(ctx);
					if (replyQ != null) {
						Message respMsg = null;
						if (responseQ == null) {
							Tracer.trace(
									"No response is specified for this consumer, but producer is asking for a reply. Sending a blank message");
							respMsg = session.createMessage();
						} else {
							/*
							 * prepare a reply based on specification
							 */
							respMsg = responseQ.createMessage(ctx);
						}
						if (corId != null) {
							respMsg.setJMSCorrelationID(corId);
						}
						producer.send(replyQ, respMsg);
					}
				} catch (Exception e) {
					ctx.addMessage(Messages.INTERNAL_ERROR, "Message processor threw an excpetion. " + e.getMessage());
				}
				if(consumeAll == false){
					break;
				}
			} while (processor.toContinue());
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while consuming and procesing JMS queue " + this.queueName);

		} finally {
			if (consumer != null) {
				try {
					consumer.close();
				} catch (Exception ignore) {
					//
				}
			}
			if (producer != null) {
				try {
					producer.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/**
	 * produce message on this queue
	 *
	 * @param ctx
	 *            service context where this is all happening
	 *
	 * @param responseQ
	 *            in case we are to get a response for this message-send
	 *            operation
	 * @return true if a message indeed was put on the queue. False otherwise
	 */
	public boolean produce(ServiceContext ctx, JmsQueue responseQ) {
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		Queue response = null;
		String corId = null;
		Session session = ctx.getJmsSession();
		try {
			producer = session.createProducer(this.queue);
			/*
			 * create a message with data from ctx
			 */
			Message msg = this.createMessage(ctx);

			/*
			 * should we ask for a return message?
			 */
			if (responseQ != null) {
				if (responseQ.queueName == null) {
					response = session.createTemporaryQueue();
					consumer = session.createConsumer(response);
				} else {
					response = responseQ.getQueue();
					corId = UUID.randomUUID().toString();
					consumer = session.createConsumer(response, "JMSCorrelationID='" + corId + '\'');
					msg.setJMSCorrelationID(corId);
				}
				msg.setJMSReplyTo(response);
			}
			producer.send(msg);
			/*
			 * checking for one of them is good enough, but compiler would crib.
			 * We would rather live with whatever is the over-head of checking
			 * for another null than loosing the null-check facility for this
			 * method
			 */
			if (consumer != null && responseQ != null) {
				Message message = consumer.receive();
				if (message == null) {
					/*
					 * some issue in the queue
					 */
					Tracer.trace("Response message is null. Probably some issue with the queue provider");
					return false;
				}
				responseQ.extractMessage(message, ctx);
			}
			return true;
		} catch (Exception e) {
			Tracer.trace("Error while putting mesage on tp a queue. " + e.getMessage());
			return false;
		} finally {
			if (consumer != null) {
				try {
					consumer.close();
				} catch (Exception ignore) {
					//
				}
			}
			if (producer != null) {
				try {
					producer.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}

	/**
	 * @param session
	 * @param ctx
	 * @return
	 * @throws JMSException
	 */
	private Message createMessage(ServiceContext ctx) throws JMSException {
		Session session = ctx.getJmsSession();
		if (this.dataFormatter != null) {
			String text = this.dataFormatter.format(ctx);
			return session.createTextMessage(text);
		}

		if (this.messageBodyType == null) {
			/*
			 * properties are used for transporting data
			 */
			Message message = session.createMessage();
			if (this.fieldNames != null) {
				this.setHeaderFields(message, ctx, this.fieldNames);
			} else if (this.recordName != null) {
				Record record = ComponentManager.getRecord(this.recordName);
				this.setHeaderFields(message, ctx, record.getFieldNames());
			} else {
				Tracer.trace("No fields specified to be added to the message.");
			}
			return message;
		}

		if (this.messageBodyType == DataSerializationType.MAP) {
			MapMessage message = session.createMapMessage();
			if (this.fieldNames != null) {
				this.setMapFields(message, ctx, this.fieldNames);
			} else if (this.recordName != null) {
				Record record = ComponentManager.getRecord(this.recordName);
				this.setMapFields(message, ctx, record.getFieldNames());
			} else {
				Tracer.trace("No fields specified to be added Map.");
			}
			return message;
		}

		if (this.messageBodyType == DataSerializationType.OBJECT) {
			if (this.bodyFieldName == null) {
				throw new ApplicationError(
						"bodyFieldName is not specified for messaage body when messageBodyType is set to object");
			}
			Object object = ctx.getObject(this.bodyFieldName);
			if (object == null) {
				Tracer.trace("Service context has no object named " + this.bodyFieldName
						+ ". No object assigned to message.");
				return session.createObjectMessage();
			}
			if (object instanceof Serializable) {
				return session.createObjectMessage((Serializable) object);
			}
			throw new ApplicationError("Service context has an instance of " + object.getClass().getName()
					+ " as object for message. This class must be serializable.");
		}

		/*
		 * so, it is a TextMessage. Our task is to create the text to be set to
		 * the message body
		 */
		TextMessage message = session.createTextMessage();
		String text = null;

		if (this.bodyFieldName != null) {
			/*
			 * simplest of our task. text is readily available in this field.
			 */
			text = ctx.getTextValue(this.bodyFieldName);
			if (text == null) {
				Tracer.trace("No value found for body text with field name " + this.bodyFieldName + ". Data not set.");
			} else {
				message.setText(text);
			}
			return message;
		}

		if (this.recordName != null) {
			Record record = ComponentManager.getRecord(this.recordName);
			message.setText(this.messageBodyType.serializeFields(ctx, record.getFields()));
			return message;
		}
		if (this.fieldNames != null) {
			message.setText(this.messageBodyType.serializeFields(ctx, this.fieldNames));
			return message;
		}
		throw new ApplicationError("Record or field details are required for creating message");
	}

	/**
	 *
	 * @param message
	 * @param ctx
	 * @throws JMSException
	 */
	public void extractMessage(Message message, ServiceContext ctx) throws JMSException {
		if (this.dataExtractor != null) {
			Tracer.trace("Using a custom class to extract data " + this.messageExtractor);
			if (message instanceof TextMessage == false) {
				throw new ApplicationError("Expecting a TextMessage on queue " + this.bodyFieldName + " but we got a "
						+ message.getClass().getSimpleName());
			}
			String text = ((TextMessage) message).getText();
			this.dataExtractor.extract(text, ctx);
			return;
		}

		if (this.messageBodyType == null) {
			/*
			 * properties are used for transporting data
			 */
			if (this.extractAll) {
				this.extractAllFromHeader(message, ctx);
			} else if (this.fieldNames != null && this.fieldNames.length > 0) {
				this.extractHeaderFields(message, ctx, this.fieldNames);
			} else if (this.recordName != null) {
				Record record = ComponentManager.getRecord(this.recordName);
				this.extractHeaderFields(message, ctx, record.getFieldNames());
			} else {

				Tracer.trace("No fields specified to be extracted from the message. Nothing extracted. ");
			}
			return;
		}
		/*
		 * we use three types of message body. TEXT, MAP and OBJECT
		 */
		if (this.messageBodyType == DataSerializationType.OBJECT) {
			if (message instanceof ObjectMessage == false) {
				Tracer.trace("We expected a ObjectMessage but got " + message.getClass().getSimpleName()
						+ ". No object extracted.");
				return;
			}
			Object object = ((ObjectMessage) message).getObject();
			if (object == null) {
				Tracer.trace("Messaage object is null. No object extracted.");
			} else if (this.bodyFieldName == null) {
				Tracer.trace("bodyFieldName not set, and hence the object of instance " + object.getClass().getName()
						+ "  " + object + " not added to context.");
			} else {
				ctx.setObject(this.bodyFieldName, object);
			}
			return;
		}

		if (this.messageBodyType == DataSerializationType.MAP) {
			if (message instanceof MapMessage == false) {
				Tracer.trace("We expected a MapMessage but got " + message.getClass().getSimpleName()
						+ ". No data extracted.");
				return;
			}
			MapMessage msg = (MapMessage) message;
			if (this.extractAll) {
				this.extractAllFromMap(ctx, msg);
			} else if (this.fieldNames != null) {
				this.extractMapFields(ctx, msg, this.fieldNames);
			} else if (this.recordName != null) {
				Record record = ComponentManager.getRecord(this.recordName);
				this.extractMapFields(ctx, msg, record.getFieldNames());
			} else {
				Tracer.trace("No directive to extract any fields from this MapMessage.");
			}
			return;

		}

		if (message instanceof TextMessage == false) {
			Tracer.trace(
					"We expected a TextMessage but got " + message.getClass().getSimpleName() + ". No data extracted.");
			return;
		}

		String text = ((TextMessage) message).getText();
		if (text == null) {
			Tracer.trace("Messaage text is null. No data extracted.");
			return;
		}
		if (this.bodyFieldName != null) {
			ctx.setTextValue(this.bodyFieldName, text);
			return;
		}
		if (this.recordName != null) {
			Record record = ComponentManager.getRecord(this.recordName);
			this.messageBodyType.parseFields(text, ctx, record.getFields());
			return;
		}
		this.messageBodyType.parseFields(text, ctx, this.fieldNames, null);
		return;
	}

	/**
	 * @param ctx
	 * @param message
	 * @param names
	 * @throws JMSException
	 */
	private void extractMapFields(ServiceContext ctx, MapMessage message, String[] names) throws JMSException {
		for (String nam : names) {
			Object val = message.getObject(nam);
			if (val != null) {
				ctx.setValue(nam, Value.parseObject(val));
			}
		}
	}

	/**
	 * @param ctx
	 * @param message
	 * @throws JMSException
	 */
	private void extractAllFromMap(ServiceContext ctx, MapMessage message) throws JMSException {
		@SuppressWarnings("unchecked")
		Enumeration<String> names = message.getMapNames();
		while (true) {
			try {
				String nam = names.nextElement();
				Object val = message.getObject(nam);
				if (val != null) {
					ctx.setValue(nam, Value.parseObject(val));
				}
			} catch (NoSuchElementException e) {
				/*
				 * unfortunately we have to live with this old-styled
				 * exception for a normal event!!!
				 */
				return;
			}
		}
	}

	/**
	 * @param message
	 * @param ctx
	 * @throws JMSException
	 */
	private void extractAllFromHeader(Message message, ServiceContext ctx) throws JMSException {
		@SuppressWarnings("unchecked")
		Enumeration<String> names = message.getPropertyNames();
		while (true) {
			try {
				String nam = names.nextElement();
				Object val = message.getObjectProperty(nam);
				if (val != null) {
					ctx.setValue(nam, Value.parseObject(val));
				}
			} catch (NoSuchElementException e) {
				/*
				 * unfortunately we have to live with this old-styled
				 * exception for a normal event!!!
				 */
				return;
			}
		}
	}

	/**
	 *
	 * @param message
	 * @param ctx
	 * @param names
	 * @throws JMSException
	 */
	private void extractHeaderFields(Message message, ServiceContext ctx, String[] names) throws JMSException {
		for (String nam : names) {
			Object val = message.getObjectProperty(nam);
			if (val != null) {
				ctx.setValue(nam, Value.parseObject(val));
			}
		}

	}

	/**
	 *
	 * @param message
	 * @param ctx
	 * @param names
	 * @throws JMSException
	 */
	private void setHeaderFields(Message message, ServiceContext ctx, String[] names) throws JMSException {
		for (String nam : names) {
			Value val = ctx.getValue(nam);
			if (val == null) {
				Tracer.trace("No value for " + nam + ". Value not set to message header.");
			} else {
				message.setObjectProperty(nam, val.toObject());
			}
		}

	}

	/**
	 *
	 * @param message
	 * @param ctx
	 * @param names
	 * @throws JMSException
	 */
	private void setMapFields(MapMessage message, ServiceContext ctx, String[] names) throws JMSException {
		for (String nam : names) {
			Value val = ctx.getValue(nam);
			if (val == null) {
				Tracer.trace("No value for " + nam + ". Value not set to Map.");
			} else {
				message.setObject(nam, val.toObject());
			}
		}

	}

	/**
	 * open shop and be ready for a repeated use
	 */
	public void getReady() {
		/*
		 * avoid repeated check for empty array
		 */
		if (this.fieldNames != null && this.fieldNames.length == 0) {
			this.fieldNames = null;
		}
		/*
		 * cache object instance
		 */
		if (this.messageExtractor != null) {
			try {
				this.dataExtractor = (DataExtractor) Class.forName(this.messageExtractor).newInstance();
			} catch (Exception e) {
				throw new ApplicationError(e,
						"Error while creating an instance of DataExtractor for " + this.messageExtractor);
			}
		}

		/*
		 * cache object instance
		 */
		if (this.messageFormatter != null) {
			try {
				this.dataFormatter = (DataFormatter) Class.forName(this.messageFormatter).newInstance();
			} catch (Exception e) {
				throw new ApplicationError(e,
						"Error while creating an instance of DataFormatter for " + this.messageFormatter);
			}
		}
		try {
			this.queue = (Queue) new InitialContext().lookup(this.queueName);
		} catch (Exception e) {
			throw new ApplicationError("Jms queue name " + this.queueName
					+ " could not be used as a JNDI name to locate a queue name. " + e.getMessage());
		}
	}

	/**
	 *
	 * @return name of this queue
	 */
	public Queue getQueue() {
		return this.queue;
	}

	/**
	 * validate attributes
	 *
	 * @param vtx
	 *            validation context
	 * @param forProducer
	 * @return number of errors detected
	 */
	public int validate(ValidationContext vtx, boolean forProducer) {
		int count = 0;
		/*
		 * fieldNames and recordName are two ways to specify list if fields
		 */
		boolean fieldListSpecified = this.recordName != null || (this.fieldNames != null && this.fieldNames.length > 0);
		/*
		 * fieldName is required if we are set/get message body directly from
		 * one field rather than constructing it from other fields, or
		 * extracting it to other fields
		 */
		boolean fieldNameRequired = this.messageBodyType == DataSerializationType.TEXT
				|| this.messageBodyType == DataSerializationType.OBJECT;

		/*
		 * now let is start our role as an auditor - find anything unusual :-)
		 */
		if (this.bodyFieldName == null) {
			if (fieldNameRequired) {
				vtx.addError(
						"messageBodyType=text/object requires bodyFieldName to which this text/object is to be assigned from/to");
				count++;
			}
		} else {
			if (!fieldNameRequired) {
				vtx.reportUnusualSetting(
						"bodyFieldName is used for messageBodyType of object and text only. It is ignored otherwise.");
			}
		}
		/*
		 * custom extractor
		 */
		if (this.messageExtractor != null) {
			if (forProducer) {
				vtx.reportUnusualSetting(
						"messageExtractor is used when the queue is used for consuming/reading message.");
			} else {
				if (fieldListSpecified) {
					vtx.reportUnusualSetting(
							"messageExtractor is used for extrating data. fieldNames, and reordName settings are ignored.");
				}
			}
			if (this.messageBodyType != DataSerializationType.TEXT) {
				vtx.reportUnusualSetting(
						"messageExtractor is used when the message body usage is text. this.messageBodyType is set to "
								+ this.messageBodyType + ". we will ignore this setting and assume text body.");
			}
		}

		/*
		 * custom formatter
		 */
		if (this.messageFormatter != null) {
			if (!forProducer) {
				vtx.reportUnusualSetting(
						"messageFormatter is used when message is to be created/produced. Setting ignored");
			} else {
				if (fieldListSpecified) {
					vtx.reportUnusualSetting(
							"messageFormatter is used for formatting message body. fieldNames, and reordName settings are ignored.");
				}
			}
			if (this.messageBodyType != DataSerializationType.TEXT) {
				vtx.reportUnusualSetting(
						"messageFormatter is used when the message body usage is text. this.messageBodyType is set to "
								+ this.messageBodyType + ". we will ignore this setting and assume text body.");
			}
		}

		/*
		 * record name is required for fixed-width
		 */
		if (this.recordName == null) {
			if (this.messageBodyType == DataSerializationType.FIXED_WIDTH) {
				vtx.addError("messageBodyType=fixedWidth requires recordName");
				count++;
			}
		} else {
			if (fieldNameRequired) {
				vtx.reportUnusualSetting("recordName is ignored for message body type of text/object.");
			}
		}

		/*
		 * no data specification?
		 */
		if (fieldNameRequired == false && fieldListSpecified == false) {
			if (forProducer) {
				vtx.reportUnusualSetting("No fields/records specified. Message is designed to carry no data.");
			} else if (!this.extractAll) {
				vtx.reportUnusualSetting(
						"No fields/records specified, and extractAll is set to false. Message consumer is not looking for any data in this message.");
			}
		}

		if (this.bodyFieldName == null) {
			if (this.messageBodyType == DataSerializationType.OBJECT
					|| this.messageBodyType == DataSerializationType.TEXT) {
				vtx.addError(
						"messageBodyType=object or text requires bodyFieldName to which the body object/text is to be assigned to.");
				count++;
			}
		}

		if (this.extractAll && forProducer) {
			vtx.reportUnusualSetting(
					"extractAll is not relevant when the queue is used to produce/send message. Attribute ignored.");
		}

		return count;
	}
}
