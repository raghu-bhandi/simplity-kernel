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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
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
	MessageBodyType messageBodyType;
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
	 * consume a request queue, and optionally put a message on to the response
	 * queue. Keep doing this for messages in the request queue, till the queue
	 * is closed, or the processor signals a shut-down
	 *
	 * @param ctx
	 *            service context where all this is happening
	 *
	 * @param processor
	 *            object instance that is interested in processing the message
	 * @param session
	 * @param responseQ
	 *            optional response queue to be used to respond back to the
	 *            incoming message
	 */
	public void consume(ServiceContext ctx, MessageClient processor,
			Session session, JmsQueue responseQ) {
		MessageConsumer consumer = null;
		try {
			Queue request = session.createQueue(this.queueName);
			consumer = session.createConsumer(request, this.messageSelector);
			while (processor.toContinue()) {
				Message msg = consumer.receive();
				if (msg == null) {
					Tracer.trace("Queue " + this.queueName
							+ " has shutdown. Hence this consumer is also shutting down");
					/*
					 * queue is shut down
					 */
					break;
				}
				boolean allOk = false;
				try {
					this.extractMessage(msg, ctx);
					allOk = processor.process(ctx);
				} catch (Exception e) {
					Tracer.trace("Message processor threw an excpetion "
							+ e.getMessage());
				}
				if (allOk) {
					session.commit();
				} else {
					Tracer.trace(
							"Rolling back message as the processor crashed/returned a flase value");
					session.rollback();
				}
			}
		} catch (Exception e) {
			throw new ApplicationError(e,
					"Error while consuming and procesing JMS queue "
							+ this.queueName);

		} finally {
			if (consumer != null) {
				try {
					consumer.close();
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
	 * @param session
	 * @param responseQ
	 *            in case we are to get a response for this message-send
	 *            operation
	 * @return true if a message indeed was put on the queue. False otherwise
	 */
	public boolean produce(ServiceContext ctx, Session session,
			JmsQueue responseQ) {
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		Queue response = null;
		String corId = null;
		try {
			Queue request = session.createQueue(this.queueName);
			producer = session.createProducer(request);
			Message msg = this.createMessage(session, ctx);
			if (responseQ != null) {
				if (responseQ.queueName == null) {
					response = session.createTemporaryQueue();
					consumer = session.createConsumer(response);
				} else {
					response = session.createQueue(responseQ.getQueueName());
					corId = UUID.randomUUID().toString();
					consumer = session.createConsumer(response,
							"JMSCorrelationID='" + corId + '\'');
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
					Tracer.trace(
							"Response message is null. Probably som eissue with the queue provider");
					return false;
				}
				responseQ.extractMessage(message, ctx);
			}
			return true;
		} catch (Exception e) {
			Tracer.trace("Error while putting mesage on tp a queue. "
					+ e.getMessage());
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
	private Message createMessage(Session session, ServiceContext ctx)
			throws JMSException {
		Message message = null;
		if (this.dataFormatter != null) {
			String text = this.dataFormatter.format(ctx);
			message = session.createTextMessage(text);
			return message;
		}

		if (this.messageBodyType == null) {
			/*
			 * properties are used for transporting data
			 */
			message = session.createMessage();
			if (this.fieldNames != null && this.fieldNames.length > 0) {
				for (String nam : this.fieldNames) {
					Value val = ctx.getValue(nam);
					if (Value.isNull(val)) {
						Tracer.trace("No value found for " + nam + " in service context. Data not added to message");
					}else{
						message.setObjectProperty(nam, val.toObject());
					}
				}
				return message;
			}

			if (this.recordName != null) {
				Record record = ComponentManager.getRecord(this.recordName);
				for (String nam : record.getFieldNames()) {
					Value val = ctx.getValue(nam);
					if (Value.isNull(val)) {
						Tracer.trace("No value found for " + nam + " in service context. Data not added to message");
					}else{
						message.setObjectProperty(nam, val.toObject());
					}
				}
				return message;
			}

			Tracer.trace(
					"No fields specified to be added to teh message. No data added to message.");
			return message;
		}

		if (this.messageBodyType == MessageBodyType.OBJECT) {
			if (this.bodyFieldName == null) {
				throw new ApplicationError(
						"bodyFieldName is not specified for messaage body when messageBodyType is set to object");
			}
			Object object = ctx.getObject(this.bodyFieldName);
			if (object == null) {
				Tracer.trace("Service context has no object named "
						+ this.bodyFieldName
						+ ". No object assigned to message.");
				return session.createObjectMessage();
			}
			if (object instanceof Serializable) {
				return session.createObjectMessage((Serializable) object);
			}
			throw new ApplicationError("Service context has an instance of "
					+ object.getClass().getName()
					+ " as object for message. This class must be serializable.");
		}
		String text = null;
		switch (this.messageBodyType) {
		case COMMA_SEPARATED:
			break;
		case COMMA_SEPARATED_PAIRS:
			break;
		case FIXED_WIDTH:
			break;
		case FORM_DATA:
			break;
		case JSON:
			break;
		case MAP:
			break;
		case OBJECT:
			// this is not reachable by design
			break;
		case TEXT:
			if (this.bodyFieldName == null) {
				throw new ApplicationError(
						"field name not specified to receive body of the message");
			}
			text = ctx.getTextValue(this.bodyFieldName);
			if (text == null) {
				Tracer.trace("Service context has no value for field "
						+ this.bodyFieldName
						+ " and hence no data is assigned to message");
				return session.createTextMessage();
			}
			break;
		case XML_ATTRIBUTES:
			break;
		case XML_ELEMENTS:
			break;
		case YAML:
			break;
		default:
			break;
		}
		if (text == null) {
			throw new ApplicationError("Sorry. Body Messaage Type of "
					+ this.messageBodyType
					+ " is not yet implemented to create data for a JMS message. Use alternative method as of now");
		}
		return session.createTextMessage(text);
	}

	/**
	 *
	 * @param message
	 * @param ctx
	 * @throws JMSException
	 */
	public void extractMessage(Message message, ServiceContext ctx)
			throws JMSException {
		if (this.dataExtractor != null) {
			Tracer.trace("Using a custom class to extract data "
					+ this.messageExtractor);
			if (message instanceof TextMessage == false) {
				throw new ApplicationError("Expecting a TextMessage on queue "
						+ this.bodyFieldName + " but we got a "
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

			if (this.fieldNames != null && this.fieldNames.length > 0) {
				for (String nam : this.fieldNames) {
					Object val = message.getObjectProperty(nam);
					if (val != null) {
						ctx.setValue(nam, Value.parseObject(val));
					}
				}
				return;
			}

			if (this.recordName != null) {
				Record record = ComponentManager.getRecord(this.recordName);
				for (String nam : record.getFieldNames()) {
					Object val = message.getObjectProperty(nam);
					if (val != null) {
						ctx.setValue(nam, Value.parseObject(val));
					}
				}
				return;
			}

			Tracer.trace(
					"No fields specified to be extracted from the message. Nothing extracted. ");
			return;
		}
		switch (this.messageBodyType) {
		case COMMA_SEPARATED:
			break;
		case COMMA_SEPARATED_PAIRS:
			break;
		case FIXED_WIDTH:
			break;
		case FORM_DATA:
			break;
		case JSON:
			break;
		case MAP:
			break;
		case OBJECT:
			if (this.bodyFieldName == null) {
				throw new ApplicationError(
						"field name not specified to receive body of the message as an object");
			}
			if (message instanceof ObjectMessage == false) {
				throw new ApplicationError(
						"Expected a ObjectMessage on the queue " + this.queueName + " but received a message of type " + message.getClass().getSimpleName());
			}
			Object obj = ((ObjectMessage) message).getObject();
			if(obj == null){
				Tracer.trace("ObjectMessage had a null value. Object not extracted");
			}else{
				ctx.setObject(this.bodyFieldName, obj);
			}
			return;
		case TEXT:
			if (this.bodyFieldName == null) {
				throw new ApplicationError(
						"field name not specified to receive body of the message");
			}
			if (message instanceof TextMessage == false) {
				throw new ApplicationError(
						"Expected a TextMessage on the queue " + this.queueName + " but received a message of type " + message.getClass().getSimpleName());
			}
			String txt = ((TextMessage) message).getText();
			if(txt == null){
				Tracer.trace("TextMessage had a null value. Text not extracted");
			}else{
				ctx.setValue(this.bodyFieldName, Value.parseValue(txt));
			}
			return;

		case XML_ATTRIBUTES:
			break;
		case XML_ELEMENTS:
			break;
		case YAML:
			break;
		default:
			break;

		}
		throw new ApplicationError("Sorry. Body Messaage Type of "
				+ this.messageBodyType
				+ " is not yet implemented to exract data from a message.. Use alternative method as of now");

	}

	/**
	 * open shop and be ready for a repeated use
	 */
	public void getReady() {
		if (this.messageExtractor != null) {
			try {
				this.dataExtractor = (DataExtractor) Class
						.forName(this.messageExtractor).newInstance();
			} catch (Exception e) {
				throw new ApplicationError(e,
						"Error while creating an instance of DataExtractor for "
								+ this.messageExtractor);
			}
		}

		if (this.messageFormatter != null) {
			try {
				this.dataFormatter = (DataFormatter) Class
						.forName(this.messageFormatter).newInstance();
			} catch (Exception e) {
				throw new ApplicationError(e,
						"Error while creating an instance of DataFormatter for "
								+ this.messageFormatter);
			}
		}
	}

	/**
	 *
	 * @return name of this queue
	 */
	public String getQueueName() {
		return this.queueName;
	}

	/**
	 * validate attributes
	 *
	 * @param vtx
	 *            validation context
	 * @return number of errors detected
	 */
	public int validate(ValidationContext vtx) {
		// TODO : add all validation rules here
		return 0;
	}
}
