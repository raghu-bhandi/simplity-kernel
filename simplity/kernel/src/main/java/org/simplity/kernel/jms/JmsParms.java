package org.simplity.kernel.jms;

import javax.jms.Queue;
import javax.jms.QueueSession;

public class JmsParms {
	protected QueueSession session = null;
	protected Queue queue = null;
	protected String persistent = null;
	protected String priority = null;
	protected String timetolive = null;
	public QueueSession getSession() {
		return session;
	}
	public void setSession(QueueSession session) {
		this.session = session;
	}
	public Queue getQueue() {
		return queue;
	}
	public void setQueue(Queue queue) {
		this.queue = queue;
	}
	public String getPersistent() {
		return persistent;
	}
	public void setPersistent(String persistent) {
		this.persistent = persistent;
	}
	public String getPriority() {
		return priority;
	}
	public void setPriority(String priority) {
		this.priority = priority;
	}
	public String getTimetolive() {
		return timetolive;
	}
	public void setTimetolive(String timetolive) {
		this.timetolive = timetolive;
	}

}