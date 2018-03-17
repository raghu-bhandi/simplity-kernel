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

import org.simplity.gateway.ReqReader;
import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.AttachmentManager;
import org.simplity.kernel.Messages;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Component that specifies what inputs are expected
 *
 * @author simplity.org
 *
 */

public class InputData {
	private static final Logger logger = LoggerFactory.getLogger(InputData.class);
	/**
	 * do not parse the request text. Just set it to this field. Service will
	 * take care of that
	 */
	String setInputToFieldName;

	/**
	 * No specification. Trust the client and extract whatever is input
	 */
	boolean justInputEveryThing;

	/**
	 * fields to be extracted from input
	 */

	InputField[] inputFields;

	/**
	 * data sheets to be extracted from input
	 */
	InputRecord[] inputRecords;

	/**
	 * comma separated list of field names that carry key to attachments. these
	 * are processed as per attachmentManagement, and revised key is replaced as
	 * the field-value
	 */
	String[] attachmentFields;
	/**
	 * comma separated list of column names in the form
	 * sheetName.columnName,sheetName1.columnName2....
	 */
	String[] attachmentColumns;

	/**
	 * extract and validate data from input service data into service context
	 *
	 * @param inputText
	 *            text
	 *            non-null pay-load received from client
	 * @param ctx
	 *            into which data is to be extracted to
	 */
	public void extractFromJson(String inputText, ServiceContext ctx) {
		JSONObject json = null;
		if (inputText == null) {
			json = new JSONObject();
		} else {
			String jsonText = inputText.trim();
			if (jsonText.isEmpty()) {
				json = new JSONObject();
			} else {
				json = new JSONObject(jsonText);
			}
		}
		this.extractFromJson(json, ctx);
	}

	/**
	 * extract and validate data from input service data into service context
	 *
	 * @param json
	 *            non-null pay-load received from client
	 * @param ctx
	 *            into which data is to be extracted to
	 */
	public void extractFromJson(JSONObject json, ServiceContext ctx) {
		if (this.setInputToFieldName != null) {
			ctx.setTextValue(this.setInputToFieldName, json.toString());
			logger.info("Request text is not parsed but set as object value of " + this.setInputToFieldName);
			return;
		}
		if (this.justInputEveryThing) {
			JsonUtil.extractAll(json, ctx);
			return;
		}

		int n = 0;
		if (this.inputFields != null) {
			for (InputField field : this.inputFields) {
				if (field.extractInput(json.opt(field.name), ctx)) {
					n++;
				}
			}
			logger.info(n + " fields extracted for input");
		}
		if (this.inputRecords != null) {
			for (InputRecord inRec : this.inputRecords) {
				inRec.extractInput(json, ctx);
			}

		}

		if (this.attachmentFields != null) {
			storeFieldAttaches(this.attachmentFields, ctx, true);
		}

		if (this.attachmentColumns != null) {
			storeColumnAttaches(this.attachmentColumns, ctx, true);
		}
	}

	/**
	 * this is made static to allow re-use by OutputData as well
	 *
	 * @param columns
	 * @param ctx
	 * @param toStore
	 */
	static void storeColumnAttaches(String[] columns, ServiceContext ctx, boolean toStore) {
		for (String ac : columns) {
			int idx = ac.lastIndexOf('.');
			if (idx == -1) {
				throw new ApplicationError("Invalid attachmentColumns specification");
			}
			String sheetName = ac.substring(0, idx);
			String colName = ac.substring(idx + 1);
			DataSheet sheet = ctx.getDataSheet(sheetName);
			if (sheet == null) {
				logger.info("Data sheet " + sheetName + " not input. Hence no attachment management on its column "
						+ colName);
				continue;
			}
			idx = sheet.getColIdx(colName);
			if (idx == -1) {
				logger.info("Data sheet " + sheetName + " does not have a column named " + colName
						+ " No attachment management on this column");
				continue;
			}
			int nbr = sheet.length();
			if (nbr == 0) {
				logger.info("Data sheet " + sheetName + " has no rows. No attachment management on this column");
				continue;
			}
			for (int i = 0; i < nbr; i++) {
				Value key = sheet.getColumnValue(colName, i);

				if (Value.isNull(key) || key.toString().isEmpty()) {
					continue;
				}
				String newKey = null;
				if (toStore) {
					newKey = AttachmentManager.moveToStorage(key.toText());
				} else {
					newKey = AttachmentManager.moveFromStorage(key.toText());
				}
				if (newKey == null) {
					throw new ApplicationError(
							"Unable to move attachment content with key=" + key + " from/to temp area");
				}
				logger.info("Attachment key " + key + " replaced with " + newKey
						+ " after swapping content from/to temp area");
				sheet.setColumnValue(colName, i, Value.newTextValue(newKey));
			}
		}
	}

	/**
	 *
	 * @param fields
	 * @param ctx
	 * @param toStor
	 */
	static void storeFieldAttaches(String[] fields, ServiceContext ctx, boolean toStor) {
		for (String af : fields) {
			String key = ctx.getTextValue(af);
			if (key == null || key.isEmpty()) {
				logger.info("Attachment field " + af + " is not specified. Skipping it.");
				continue;
			}
			String newKey = null;
			if (toStor) {
				newKey = AttachmentManager.moveToStorage(key);
			} else {
				newKey = AttachmentManager.moveFromStorage(key);
			}
			if (newKey == null) {
				logger.info("Error while managing attachment key " + key);
				ctx.addValidationMessage(Messages.INVALID_ATTACHMENT_KEY, af, null, null, 0, newKey);
			} else {
				logger.info("Attachment key " + key + " replaced with " + newKey
						+ " after swapping the contents from/to temp area");
				ctx.setTextValue(af, newKey);
			}
		}
	}

	/**
	 * get ready for a long-haul service :-)
	 */
	public void getReady() {
		if (this.inputFields != null) {
			for (InputField field : this.inputFields) {
				field.getReady();
			}
		}
		if (this.inputRecords != null) {
			for (InputRecord inRec : this.inputRecords) {
				inRec.getReady();
			}
		}
	}

	/**
	 * validate this specification
	 *
	 * @param ctx
	 * @return number of errors added
	 */
	public int validate(ValidationContext ctx) {
		if (this.inputRecords == null) {
			if (this.inputFields == null) {
				ctx.addError(
						"input data has no input records and no input fields. If no data is expected, just skip InputData.");
				return 1;
			}
			return 0;
		}
		int count = 0;
		for (InputRecord rec : this.inputRecords) {
			count += rec.validate(ctx);
		}
		if (this.inputFields != null) {
			for (InputField fields : this.inputFields) {
				count += fields.validate(ctx);
			}
		}
		if (this.attachmentColumns != null) {
			for (String txt : this.attachmentColumns) {
				int idx = txt.lastIndexOf('.');
				if (idx == -1) {
					ctx.addError(
							"attachmentColumns is set to " + txt + ". This should be of the form sheetName.columnName");
					count++;
				}
			}
		}
		return count;
	}

	/**
	 * Now that the service has succeeded, is there anything that the input had
	 * done that need to be cleaned-up? As of now, we have attachments that may
	 * have been superseded..
	 *
	 * @param ctx
	 */
	public void cleanup(ServiceContext ctx) {
		if (this.attachmentFields == null) {
			return;
		}
		for (String attId : this.attachmentFields) {
			String fieldName = attId + ServiceProtocol.OLD_ATT_TOKEN_SUFFIX;
			String token = ctx.getTextValue(fieldName);
			if (token == null) {
				logger.info(attId + " is an attachment input field. No value found in " + fieldName
						+ " on exit of service, and hence this attachment is not removed from storage");
			} else {
				AttachmentManager.removeFromStorage(token);
				logger.info("Attachment field " + attId + " had an existing token " + token
						+ ". That is now removed from storage");
			}

		}
	}

	/**
	 * @param inRecs
	 */
	public void setRecords(InputRecord[] inRecs) {
		this.inputRecords = inRecs;

	}

	/**
	 *
	 * @param inFields
	 */
	public void setInputFields(InputField[] inFields) {
		this.inputFields = inFields;
	}

	/**
	 * input is managed by the service itself. Keep the reader in the context as
	 * it is.
	 */
	boolean managedByService;

	/**
	 * do not parse the request text. Just set it to this field. Service will
	 * take care of that
	 */
	String saveInputObjectAs;

	/**
	 * extract and validate data from input service data into service context
	 *
	 * @param reader
	 *            non-null pay-load received from client
	 * @param ctx
	 *            into which data is to be extracted to
	 */
	public void read(ReqReader reader, ServiceContext ctx) {
		if (this.managedByService) {
			logger.info("Input writer will be used during service execution. No data extracted upfront.");
			return;
		}

		if (this.saveInputObjectAs != null) {
			ctx.setObject(this.saveInputObjectAs, reader.getRawInput());
			logger.info("Input data is not parsed, but saved as it with key=" + this.saveInputObjectAs);
			return;
		}

		if (this.justInputEveryThing) {
			reader.pushDataToContext(ctx);
			return;
		}

		if (this.inputFields != null) {
			int n = 0;
			for (InputField field : this.inputFields) {
				n += field.read(reader, ctx);
			}
			logger.info(n + " fields extracted for input");
		}

		if (this.inputRecords != null) {
			for (InputRecord inRec : this.inputRecords) {
				inRec.read(reader, ctx);
			}
		}

		if (this.attachmentFields != null) {
			storeFieldAttaches(this.attachmentFields, ctx, true);
		}

		if (this.attachmentColumns != null) {
			storeColumnAttaches(this.attachmentColumns, ctx, true);
		}
	}

	/**
	 *
	 * @return input fields list
	 */
	public InputField[] getInputFields() {
		return this.inputFields;
	}

	/**
	 * input reader has the specs, and is going to push data to context. We may
	 * have to prepare hierarchical data-structure etc..
	 *
	 * @param ctx
	 */
	public void prepareForInput(ServiceContext ctx) {
		if(this.inputRecords == null){
			return;
		}
		/*
		 * create empty sheets to receive data
		 */
		for(InputRecord rec : this.inputRecords){
			rec.addEmptySheet(ctx);
		}
		/*
		 * add links for parent-child
		 */
		for(InputRecord rec : this.inputRecords){
			rec.addDataSheetLink(ctx);
		}

	}

	/**
	 * @param xmlString
	 * @param ctx
	 */
	public void extractFromXml(String xmlString, ServiceContext ctx) {
		throw new ApplicationError("InputData.extractFromXml() not yet implemented");
	}
	/**
	 * @param xml
	 * @param ctx
	 */
	public void extractFromXml(Document xml, ServiceContext ctx) {
		throw new ApplicationError("InputData.extractFromXml() not yet implemented");
	}
}
