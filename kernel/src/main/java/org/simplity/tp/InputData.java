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
package org.simplity.tp;

import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.file.AttachmentManager;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * Component that specifies what inputs are expected
 *
 * @author simplity.org
 *
 */

public class InputData {

	InputField[] inputFields;

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
	 * @param json
	 *            input
	 * @param ctx
	 *            into which data is to be extracted to
	 */
	public void extractFromJson(JSONObject json, ServiceContext ctx) {
		int n = 0;
		if (this.inputFields != null) {
			for (InputField field : this.inputFields) {
				if (field.extractInput(json.opt(field.name), ctx)) {
					n++;
				}
			}
			Tracer.trace(n + " fields extracted for input");
		}
		if (this.inputRecords != null) {
			for (InputRecord inRec : this.inputRecords) {
				inRec.extractInput(json, ctx);
			}
		}
		Tracer.trace("We extracted " + ctx.getAllFields().size()
				+ " fields in all");

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
	static void storeColumnAttaches(String[] columns, ServiceContext ctx,
			boolean toStore) {
		for (String ac : columns) {
			int idx = ac.lastIndexOf('.');
			if (idx == -1) {
				throw new ApplicationError(
						"Invalid attachmentColumns specification");
			}
			String sheetName = ac.substring(0, idx);
			String colName = ac.substring(idx + 1);
			DataSheet sheet = ctx.getDataSheet(sheetName);
			if (sheet == null) {
				Tracer.trace("Data sheet "
						+ sheetName
						+ " not input. Hence no attachment management on its column "
						+ colName);
				continue;
			}
			idx = sheet.getColIdx(colName);
			if (idx == -1) {
				Tracer.trace("Data sheet " + sheetName
						+ " does not have a column named " + colName
						+ " No attachment management on this column");
				continue;
			}
			int nbr = sheet.length();
			if (nbr == 0) {
				Tracer.trace("Data sheet "
						+ sheetName
						+ " has no rows. No attachment management on this column");
				continue;
			}
			for (int i = 0; i < nbr; i++) {
				Value key = sheet.getColumnValue(colName, i);
				if (Value.isNull(key)) {
					continue;
				}
				String newKey = null;
				if (toStore) {
					newKey = AttachmentManager.moveToStorage(key.toText());
				} else {
					newKey = AttachmentManager.moveFromStorage(key.toText());
				}
				if (newKey == null) {
					ctx.addInternalMessage(MessageType.ERROR,
							"Error whiel storing attachment with temp key "
									+ key);
				} else {
					sheet.setColumnValue(colName, i, Value.newTextValue(newKey));
				}
			}
		}
	}

	/**
	 *
	 * @param fields
	 * @param ctx
	 * @param toStor
	 */
	static void storeFieldAttaches(String[] fields, ServiceContext ctx,
			boolean toStor) {
		for (String af : fields) {
			String key = ctx.getTextValue(af);
			if (key == null) {
				Tracer.trace("Attachment field " + af
						+ " is not spcified. Skipping it.");
				continue;
			}
			if (toStor) {
				key = AttachmentManager.moveToStorage(key);
			} else {
				key = AttachmentManager.moveFromStorage(key);
			}
			if (key == null) {
				ctx.addValidationMessage(Messages.INVALID_ATTACHMENT_KEY, af,
						null, null, 0, key);
			} else {
				ctx.setTextValue(af, key);
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
				ctx.addError("input data has no input records and no input fields. If no data is expected, just skip InputData.");
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
					ctx.addError("attachmentColumns is set to "
							+ txt
							+ ". This should be of the form sheetName.columnName");
					count++;
				}
			}
		}
		return count;
	}
}
