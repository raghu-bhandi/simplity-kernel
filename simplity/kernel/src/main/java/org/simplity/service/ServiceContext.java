/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
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
import java.util.Map.Entry;
import java.util.Set;

import javax.jms.Session;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageBox;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Messages;
import org.simplity.kernel.data.ChildSheets;
import org.simplity.kernel.data.CommonData;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.DataSheetLink;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context is created for an execution of a service. A service execution
 * requires a number of
 * actions to be performed. However, each action is on its own, and does not
 * assume presence of any
 * other action. Of course, it expects that certain data is available, and it's
 * job may be to
 * extract some more data.
 *
 * <p>
 * We use a common (we can call this global for the service) data context for
 * all such action.
 * While a DataScape is good enough data structure, we would like to make use
 * this concept for other
 * design aspects, like accumulating messages and tracking error states etc..
 * Hence this class
 * extends DataScape.
 *
 * @author simplity.org
 */
public class ServiceContext extends CommonData {
	private static final Logger logger = LoggerFactory.getLogger(ServiceContext.class);
	private final String serviceName;
	private final Value userId;
	private List<FormattedMessage> messages = new ArrayList<FormattedMessage>();
	private int nbrErrors = 0;

	/** writer that can be used by actions to write directly to the response */
	private ResponseWriter responseWriter;
	/** jms session associated with this service */
	private Session jmsSession;

	/** message box */
	private MessageBox messageBox;

	/**
	 * key by which this response can be cached. It is serviceName, possibly
	 * appended with values of some key-fields. null means can not be cached
	 */
	private String cachingKey;

	/**
	 * valid if cachingKey is non-null. number of minutes after which this cache
	 * is to be invalidated. 0 means no such
	 */
	private int cacheValidityMinutes;
	/**
	 * service caches to be invalidated after execution this service. Null means
	 * nothing is to be invalidated
	 */
	private String[] invalidations;
	/**
	 * Hierarchical child sheets prepared for output
	 */
	private Map<String, ChildSheets> outputChildSheets;

	private Map<String, DataSheetLink> inputSheetLinks;
	/**
	 * @param serviceName
	 * @param userId
	 */
	public ServiceContext(String serviceName, Value userId) {
		this.userId = userId;
		this.serviceName = serviceName;
	}

	/**
	 * add a message that is associated with a data element in the input data
	 *
	 * @param messageName
	 * @param referredField
	 *            if this is about a specific field, so that client can
	 *            associate this
	 *            message with a field/column
	 * @param otherReferredField
	 *            if the message has more than one referred field, like from-to
	 *            validation failed
	 * @param referredTable
	 *            if the referred field is actually a column in a table
	 * @param rowNumber
	 *            if table is referred, 1 based.
	 * @param params
	 *            if the message is parameterized, provide values for those
	 *            parameters. $name means
	 *            name is a field, and the field value is to be taken from
	 *            context
	 * @return type of message that got added
	 */
	public MessageType addValidationMessage(String messageName, String referredField, String otherReferredField,
			String referredTable, int rowNumber, String... params) {
		String[] values = null;
		if (params != null && params.length > 0) {
			values = new String[params.length];
			for (int i = 0; i < values.length; i++) {
				String val = params[i];
				/*
				 * if it is a field name, we get its value
				 */
				String fieldName = TextUtil.getFieldName(val);
				if (fieldName != null) {
					Value v = this.getValue(fieldName);
					if (v != null) {
						val = v.toString();
					}
				}
				values[i] = val;
			}
		}

		FormattedMessage msg = Messages.getMessage(messageName, values);
		if (msg.messageType == MessageType.ERROR) {
			this.nbrErrors++;
		}
		msg.fieldName = referredField;
		msg.relatedFieldName = otherReferredField;
		msg.tableName = referredTable;
		msg.rowNumber = rowNumber;
		this.messages.add(msg);
		return msg.messageType;
	}

	/**
	 * add a message that has no reference to a data element
	 *
	 * @param messageName
	 * @param paramValues
	 *            if the message is parameterized, provide values for those
	 *            parameters
	 * @return type of message that got added
	 */
	public MessageType addMessage(String messageName, String... paramValues) {
		return this.addValidationMessage(messageName, null, null, null, 0, paramValues);
	}

	/**
	 * add row to the messages list, but after checking for error status
	 *
	 * @param messageName
	 * @param messageType
	 * @param messageText
	 * @param referredField
	 * @param otherReferredField
	 * @param referredTable
	 * @param rowNumber
	 */
	public void addMessageRow(String messageName, MessageType messageType, String messageText, String referredField,
			String otherReferredField, String referredTable, int rowNumber) {
		if (messageType == MessageType.ERROR) {
			this.nbrErrors++;
		}
		FormattedMessage msg = new FormattedMessage(messageName, messageType, messageText);
		msg.fieldName = referredField;
		msg.relatedFieldName = otherReferredField;
		msg.tableName = referredTable;
		msg.rowNumber = rowNumber;
		this.messages.add(msg);
	}

	/**
	 * has any one raised an error message?
	 *
	 * @return true if any error message is added. False otherwise
	 */
	public boolean isInError() {
		return this.nbrErrors > 0;
	}

	/**
	 * get all messages
	 *
	 * @return messages
	 */
	public List<FormattedMessage> getMessages() {
		return this.messages;
	}

	/**
	 * get all messages as a DataSheet
	 *
	 * @return messages
	 */
	public DataSheet getMessagesAsDs() {
		String[] columnNames = { "name", "text", "messageType", "fieldName" };
		Collection<FormattedMessage> msgs = this.getMessages();
		int nbr = msgs.size();
		if (nbr == 0) {
			ValueType[] types = { ValueType.TEXT, ValueType.TEXT, ValueType.TEXT, ValueType.TEXT };
			return new MultiRowsSheet(columnNames, types);
		}
		/*
		 * data is by column
		 */
		Value[][] data = new Value[4][nbr];

		int i = 0;
		for (FormattedMessage msg : msgs) {
			data[0][i] = Value.newTextValue(msg.name);
			data[1][i] = Value.newTextValue(msg.text);
			data[2][i] = Value.newTextValue(msg.messageType.name());
			data[3][i] = Value.newTextValue(msg.fieldName);
			i++;
		}

		return new MultiRowsSheet(columnNames, data);
	}

	/** sreset/remove all messages */
	public void resetMessages() {
		this.messages.clear();
		this.nbrErrors = 0;
	}

	/** @return userId for whom this context is created */
	public Value getUserId() {
		return this.userId;
	}

	/** @return service for which this context is created */
	public String getServiceName() {
		return this.serviceName;
	}

	/**
	 * @param dataRow
	 */
	public void copyFrom(FieldsInterface dataRow) {
		for (Entry<String, Value> entry : dataRow.getAllFields()) {
			this.allFields.put(entry.getKey(), entry.getValue());
		}
	}

	/** @return set of all sheets that you can iterate over */
	public Set<Map.Entry<String, DataSheet>> getAllSheets() {
		return this.allSheets.entrySet();
	}

	/** @return summary for tracing */
	public String getSummaryInfo() {
		StringBuilder sbf = new StringBuilder("Context has ");
		sbf.append(this.allFields.size()).append(" fields and ").append(this.allSheets.size()).append(" sheets and ")
				.append(this.messages.size()).append(" messages.");
		return sbf.toString();
	}

	/**
	 * add error messages from an accumulated list
	 *
	 * @param errors
	 */
	public void addMessages(List<FormattedMessage> errors) {
		if (errors == null) {
			return;
		}
		for (FormattedMessage msg : errors) {
			this.messages.add(msg);
			if (msg.messageType == MessageType.ERROR) {
				this.nbrErrors++;
			}
		}
	}

	/**
	 * @param sheetName
	 * @return 0 if sheet does not exist
	 */
	public int nbrRowsInSheet(String sheetName) {
		DataSheet ds = this.getDataSheet(sheetName);
		if (ds == null) {
			return 0;
		}
		return ds.length();
	}

	/**
	 * set a default response writer to the context
	 *
	 * @param writer
	 */
	public void setWriter(ResponseWriter writer) {
		this.responseWriter = writer;
	}

	/** @return writer, never null */
	public ResponseWriter getWriter() {
		if (this.responseWriter == null) {
			this.setWriter(new JSONWriter());
			this.responseWriter.init();
		}
		return this.responseWriter;
	}

	/**
	 * @param session
	 */
	public void setJmsSession(Session session) {
		this.jmsSession = session;
	}

	/**
	 * @return jms connector associated with this service. never null.
	 * @throws ApplicationError
	 *             in case this service is not associated with a connector
	 */
	public Session getJmsSession() {
		if (this.jmsSession == null) {
			throw new ApplicationError("This service is not set up for a JMS operation.");
		}
		return this.jmsSession;
	}

	/**
	 * @param message
	 */
	public void putMessageInBox(Object message) {
		if (this.messageBox == null) {
			this.messageBox = new MessageBox();
		}
		this.messageBox.setMessage(message);
	}

	/** @return message, or null if there is none */
	public Object getMessageFromBox() {
		if (this.messageBox == null) {
			return null;
		}
		return this.messageBox.getMessage();
	}

	/**
	 * @param box
	 */
	public void setMessageBox(MessageBox box) {
		this.messageBox = box;
	}

	/** @return message box, or null if there is none */
	public MessageBox getMessageBox() {
		return this.messageBox;
	}

	/**
	 * @return
	 * 		get the key based on which the response can be cached.
	 *         emptyString means no key is used for caching. null means it can
	 *         not be cached.
	 */
	public String getCachingKey() {
		return this.cachingKey;
	}

	/**
	 * @return
	 * 		number of minutes the cache is valid for. 0 means it has no
	 *         expiry. This method is relevant only if getCachingKey returns
	 *         non-null (indication that the service can be cached)
	 */
	public int getCacheValidity() {
		return this.cacheValidityMinutes;
	}

	/**
	 * @param key
	 *            key based on which the response can be cached.
	 *            emptyString means no key is used for caching. null means it
	 *            can not be cached
	 * @param minutes
	 *            if non-null cache is to be invalidated after these many
	 *            minutes
	 */
	public void setCaching(String key, int minutes) {
		this.cachingKey = key;
		this.cacheValidityMinutes = minutes;
	}

	/**
	 * @return
	 * 		cached keys that need to be invalidated
	 */
	public String[] getInvalidations() {
		return this.invalidations;
	}

	/**
	 * @param invalidations
	 *            cached keys that need to be invalidated
	 */
	public void setInvalidations(String[] invalidations) {
		this.invalidations = invalidations;
	}

	/**
	 * Break a child sheet into small sheets, one for each key-combination s per
	 * parent-child relationship
	 *
	 * @param parentSheetName
	 * @param childSheetName
	 * @param parentKeys
	 * @param childKeys
	 */
	public void prepareChildSheets(String parentSheetName, String childSheetName, String[] parentKeys,
			String[] childKeys) {
		DataSheet parentSheet = this.getDataSheet(parentSheetName);
		if (parentSheet == null) {
			logger.error("Data sheet named {} not found. child sheets not prepared for this parent.", parentSheetName);
		}
		DataSheet childSheet = this.getDataSheet(childSheetName);
		if (childSheet == null) {
			logger.error("Data sheet named {} not found. child sheets not prepared for this child.", childSheetName);
		}
		ChildSheets children = new ChildSheets(parentKeys, childKeys, parentSheet, childSheet);
		if (this.outputChildSheets == null) {
			this.outputChildSheets = new HashMap<String, ChildSheets>();
		}
		this.outputChildSheets.put(childSheetName, children);
	}

	/**
	 * @param parentSheetName
	 * @param childSheetName
	 * @param pkeys
	 * @param ckeys
	 */
	public void addSheetLink(String parentSheetName, String childSheetName, String[] pkeys, String[] ckeys) {
		if(this.inputSheetLinks == null){
			this.inputSheetLinks = new HashMap<String, DataSheetLink>();
		}
		DataSheet sheet =this.getDataSheet(parentSheetName);
		if(sheet == null){
			logger.error("parent sheet {} not found in ctx. link not created.", parentSheetName);
			return;
		}
		int[] pindexes = sheet.getColumnIndexes(pkeys);
		sheet =this.getDataSheet(childSheetName);
		if(sheet == null){
			logger.error("parent sheet {} not found in ctx. link not created.", childSheetName);
			return;
		}
		int[] chindexes = sheet.getColumnIndexes(ckeys);

		this.inputSheetLinks.put(childSheetName, new DataSheetLink(parentSheetName, childSheetName, pindexes, chindexes));
	}

	/**
	 * DataSheetLink
	 * @param childSheetName
	 * @return link, or null if there is no link for this child sheet
	 */
	public DataSheetLink getDataSheetLink(String childSheetName){
		if(this.inputSheetLinks == null){
			return null;
		}
		return this.inputSheetLinks.get(childSheetName);
	}
	/**
	 * get filtered child-sheet for the parent row.
	 *
	 * @param childSheetName
	 * @param parentRow
	 *            for which child sheet is to be returned
	 * @return filtered child sheet for this parent row
	 */
	public DataSheet getChildSheet(String childSheetName, Value[] parentRow) {
		ChildSheets children = null;
		if (this.outputChildSheets != null) {
			children = this.outputChildSheets.get(childSheetName);
		}

		if (children == null) {
			logger.error("Child sheet " + childSheetName
					+ " is not filtered for its parent. outputRecord may be missing for this");
			return null;
		}
		return children.getChildSheet(parentRow);
	}
}
