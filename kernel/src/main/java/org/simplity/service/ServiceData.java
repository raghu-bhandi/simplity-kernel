/*
 * Copyright (c) 2016 simplity.org
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
package org.simplity.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;

/**
 * data carrier between client tier and service tier. we have few known fields
 * that are defined as attributes. rest are carried in a generic map
 * 
 * @author simplity.org
 *
 */
public class ServiceData {
	private String serviceName;
	private Value userId;
	private int nbrErrors = 0;
	/**
	 * could be request/response depending on direction
	 */
	private String payLoad;
	/**
	 * trace text from service, if flag is on
	 */
	private String trace;
	/**
	 * presence indicates error from server
	 */
	private List<FormattedMessage> messages = new ArrayList<FormattedMessage>();
	/**
	 * ms taken by server to execute service. does not include latency in
	 * getting the service etc..
	 */
	private int executionTime;

	/**
	 * @return the trace
	 */
	public String getTrace() {
		return this.trace;
	}

	/**
	 * @param trace
	 *            the trace to set
	 */
	public void setTrace(String trace) {
		this.trace = trace;
	}

	private final Map<String, Object> fields = new HashMap<String, Object>();

	/**
	 * @param serviceName
	 *            the serviceName to set
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * @param userId
	 *            the userId to set
	 */
	public void setUserId(Value userId) {
		this.userId = userId;
	}

	/**
	 * @param payLoad
	 *            the payLoad to set
	 */
	public void setPayLoad(String payLoad) {
		this.payLoad = payLoad;
	}

	/**
	 * @return the serviceName
	 */
	public String getServiceName() {
		return this.serviceName;
	}

	/**
	 * @return the userId
	 */
	public Value getUserId() {
		return this.userId;
	}

	/**
	 * @return the payLoad
	 */
	public String getPayLoad() {
		return this.payLoad;
	}

	/**
	 * @return the executionTime
	 */
	public int getExecutionTime() {
		return this.executionTime;
	}

	/**
	 * @param executionTime
	 *            the executionTime to set
	 */
	public void setExecutionTime(int executionTime) {
		this.executionTime = executionTime;
	}

	/**
	 * 
	 * @param key
	 * @return value associated with this key, or null if such field
	 */
	public Object get(String key) {
		return this.fields.get(key);
	}

	/**
	 * put this key-value pair into the map
	 * 
	 * @param key
	 * @param value
	 */
	public void put(String key, Object value) {
		if (key != null && value != null) {
			this.fields.put(key, value);
		}
	}

	/**
	 * 
	 * @return all field names in the fields map
	 */
	public Collection<String> getFieldNames() {
		return this.fields.keySet();
	}

	/**
	 * add message.
	 * 
	 * @param msg
	 */
	public void addMessage(FormattedMessage msg) {
		this.messages.add(msg);
		if (msg.messageType == MessageType.ERROR) {
			this.nbrErrors++;
		}
	}

	/**
	 * do we have errors?
	 * 
	 * @return true if there is at least one error message
	 */
	public boolean hasErrors() {
		return this.nbrErrors > 0;
	}

	/**
	 * @return A default service context based on input data
	 */
	public ServiceContext createContext() {
		ServiceContext ctx = new ServiceContext(this.serviceName, this.userId);
		if (this.payLoad != null) {
			JsonUtil.extractAll(this.payLoad, ctx);
		}
		/*
		 * session variables
		 */
		for (Map.Entry<String, Object> entry : this.fields.entrySet()) {
			Object val = entry.getValue();
			if (val instanceof Value) {
				ctx.setValue(entry.getKey(), (Value) val);
			} else {
				ctx.setObject(entry.getKey(), val);
			}
		}
		return ctx;
	}

	/**
	 * @return array of messages. empty array if there are no messages
	 */
	public FormattedMessage[] getMessages() {
		return this.messages.toArray(new FormattedMessage[0]);
	}

}
