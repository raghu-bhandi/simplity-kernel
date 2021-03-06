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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
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
	/**
	 * for documentation. Not used as part of any logic.
	 */
	private String serviceName;
	/**
	 * user for whom this is created. Useful in security check, populating
	 * relevant fields in db, as well as logging.
	 */
	private Value userId;

	/**
	 * this has name-object pairs, and not restricted to name-Value pairs.
	 * designed to carry any data object across layers
	 */
	private final Map<String, Object> fields = new HashMap<String, Object>();

	/**
	 * could be request/response depending on direction. As of now this is JSON
	 * flowing between client and server. We should be able to use any other
	 * serialized data representations like XML
	 */
	private String payLoad;
	/**
	 * trace text from service to client, if flag is on
	 */
	private String trace;
	/**
	 * presence indicates error from server
	 */
	private List<FormattedMessage> messages = new ArrayList<FormattedMessage>();
	/**
	 * tracked based on messages added
	 */
	private int nbrErrors = 0;
	/**
	 * ms taken by server to execute service. does not include latency in
	 * getting the service etc..
	 */
	private int executionTime;

	/**
	 * output data is cachable for this comma separated list of input field
	 * values. Null implies that this can not be cached. empty string means it
	 * can be used for all users irrespective of input data. If this is userId
	 * specific, then _userId would be the first field.
	 */
	private String cacheForInput;

	/**
	 * default constructor, but you are better off using the one with userId and
	 * serviceName
	 */
	public ServiceData() {
		// default
	}

	/**
	 *
	 * @param userId
	 * @param serviceName
	 */
	public ServiceData(Value userId, String serviceName) {
		this.userId = userId;
		this.serviceName = serviceName;
	}

	/**
	 * @param cacheForInput
	 *            the cacheForInput to set. Null implies that this can not be
	 *            cached. empty string means it can be used for all users
	 *            irrespective of input data. If this is userId specific, then
	 *            _userId would be the first field.
	 */
	public void setCacheForInput(String cacheForInput) {
		this.cacheForInput = cacheForInput;
	}

	/**
	 * @return the cacheForInput. Null implies that this can not be cached.
	 *         empty string means it can be used for all users irrespective of
	 *         input data. If this is userId specific, then _userId would be the
	 *         first field.
	 */
	public String getCacheForInput() {
		return this.cacheForInput;
	}

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
			JsonUtil.extractAll(new JSONObject(this.payLoad), ctx);
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

	/**
	 * if there are errors, then create a valid JSON response as if messages are
	 * added to the response, rather than creating an array of messages
	 *
	 * @return json response with status and messages
	 */
	public String getResponseJson() {
		if (this.hasErrors() == false) {
			String resp = this.getPayLoad();
			if (resp != null) {
				return resp;
			}
			return "{}";
		}
		JSONWriter writer = new JSONWriter();
		writer.object();
		writer.key(ServiceProtocol.REQUEST_STATUS)
				.value(ServiceProtocol.STATUS_ERROR);
		writer.key(ServiceProtocol.MESSAGES);
		JsonUtil.addObject(writer, this.getMessages());
		return writer.toString();
	}
}
