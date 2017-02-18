package org.simplity.kernel.jms;

import javax.jms.Queue;

public class JMSResponse {
	private Object message = null;
	private boolean redelivered = false;
	private int deliveryCount = 0;
	private Queue replyToQ = null;
	private String messageID = null;
	private String correlationID = null;
	/*
	 * @return Returns the deliveryCount.
	 * 
	 */
	public Object getMessage() {
		return message;
	}
	public void setMessage(Object message) {
		this.message = message;
	}
	public boolean isRedelivered() {
		return redelivered;
	}
	public void setRedelivered(boolean redelivered) {
		this.redelivered = redelivered;
	}
	public int getDeliveryCount() {
		return deliveryCount;
	}
	public void setDeliveryCount(int deliveryCount) {
		this.deliveryCount = deliveryCount;
	}
	public Queue getReplyToQ() {
		return replyToQ;
	}
	public void setReplyToQ(Queue replyToQ) {
		this.replyToQ = replyToQ;
	}
	public String getMessageID() {
		return messageID;
	}
	public void setMessageID(String messageID) {
		this.messageID = messageID;
	}
	public String getCorrelationID() {
		return correlationID;
	}
	public void setCorrelationID(String correlationID) {
		this.correlationID = correlationID;
	}
}