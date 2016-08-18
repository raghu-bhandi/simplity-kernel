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
package org.simplity.kernel;

import org.simplity.json.JSONWriter;
import org.simplity.json.Jsonable;
import org.simplity.kernel.comp.ComponentManager;

/**
 * formatted message data structure.
 *
 * @author simplity.org
 *
 */
public class FormattedMessage implements Jsonable {
	/**
	 * name of the message
	 */
	public final String name;
	/**
	 * message text
	 */
	public final String text;
	/**
	 * message type
	 */
	public final MessageType messageType;
	/**
	 * if this message is regarding a field/column that was sent from client
	 */
	public String fieldName;
	/**
	 * if this message is regarding two fields, this is the other field (like
	 * from-to)
	 */
	public String relatedFieldName;
	/**
	 * if this message is regarding a column in a sheet
	 */
	public String tableName;
	/**
	 * in case the error is for a specific row (1-based) of a sheet
	 */
	public int rowNumber;

	/**
	 * we require these three fields that can not be changed afterwards. Other
	 * attributes can be optionally set
	 *
	 * @param name
	 * 			Name
	 * @param type
	 * 			Type
	 * @param text
	 * 			Text
	 */
	public FormattedMessage(String name, MessageType type, String text) {
		this.name = name;
		this.messageType = type;
		this.text = text;
	}

	/**
	 *
	 * @param msg
	 *            message component that has no parameters in its text
	 */
	public FormattedMessage(Message msg) {
		this.name = msg.getQualifiedName();
		this.messageType = msg.getMessageType();
		this.text = msg.text;
	}

	/**
	 *
	 * @param messageName
	 * 			MessageName
	 * @param params
	 * 			params
	 */
	public FormattedMessage(String messageName, String... params) {
		Message msg = ComponentManager.getMessageOrNull(messageName);
		if (msg == null) {
			this.name = messageName;
			this.text = messageName
					+ " : description for this message is not found.";
			this.messageType = MessageType.WARNING;
			Tracer.trace("Missing mssage : " + messageName);
		} else {
			this.name = msg.getQualifiedName();
			this.messageType = msg.getMessageType();
			this.text = msg.toString(params);
		}
	}

	/**
	 *
	 * @param msgName
	 * 			msgname
	 * @param tableName
	 * 			tableName
	 * @param fieldName
	 * 			fieldName
	 * @param otherFieldName
	 * 			otherFieldName
	 * @param rowNumber
	 * 			rowNumber
	 * @param params
	 * 			params
	 */
	public FormattedMessage(String msgName, String tableName, String fieldName,
			String otherFieldName, int rowNumber, String... params) {
		this(msgName, params);
		this.tableName = tableName;
		this.fieldName = fieldName;
		this.relatedFieldName = otherFieldName;
		this.rowNumber = rowNumber;

	}

	@Override
	public void writeJsonValue(JSONWriter writer) {
		writer.object().key("name").value(this.name).key("text")
				.value(this.text).key("messageType").value(this.messageType);
		if (this.fieldName != null) {
			writer.key("fieldName").value(this.fieldName);
		}
		if (this.relatedFieldName != null) {
			writer.key("relatedFieldName").value(this.relatedFieldName);
		}
		if (this.tableName != null) {
			writer.key("tableName").value(this.tableName);
		}
		if (this.rowNumber != 0) {
			writer.key("rowNumber").value(this.rowNumber);
		}
		writer.endObject();
	}
}
