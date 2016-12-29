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

import java.util.HashSet;
import java.util.Set;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceProtocol;

/**
 * Component that specifies what inputs are expected
 *
 * @author simplity.org
 *
 */

public class OutputData {

	/**
	 * comma separated list of fields to be output.
	 */
	String[] fieldNames;

	/**
	 * sheets/fields to be output based on record definitions
	 */
	OutputRecord[] outputRecords;

	/**
	 * comma separated data sheets to be output
	 */
	String[] dataSheets;

	/**
	 * if this service wants to set/reset some session fields. Note that this
	 * directive is independent of fieldNames or outputRecords. That is if a is
	 * set as sessionFields, it is not sent to client, unless "a" is also
	 * specified as fieldNames
	 */
	String[] sessionFields;

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
	 * set response and session parameters
	 *
	 * @param ctx
	 * @param outData
	 */
	public void setResponse(ServiceContext ctx, ServiceData outData) {

		/*
		 * extract attachments if required
		 */
		if (this.attachmentFields != null) {
			InputData.storeFieldAttaches(this.attachmentFields, ctx, false);
		}

		if (this.attachmentColumns != null) {
			InputData.storeColumnAttaches(this.attachmentColumns, ctx, false);
		}
		/**
		 * session data if any
		 */
		if (this.sessionFields != null) {
			for (String f : this.sessionFields) {
				Object val = ctx.getValue(f);
				if (val == null) {
					val = ctx.getObject(f);
				}
				/*
				 * we may set null to remove existing values
				 */
				outData.put(f, val);
			}
		}

		/*
		 * response
		 */
		JSONWriter writer = new JSONWriter();
		writer.object();
		if (this.fieldNames != null) {
			JsonUtil.addAttributes(writer, this.fieldNames, ctx);
		}

		if (this.dataSheets != null) {
			for (String sheetName : this.dataSheets) {
				DataSheet sheet = ctx.getDataSheet(sheetName);
				if (sheet == null) {
					Tracer.trace("Service context has no sheet with name "
							+ sheetName + " for output.");
				} else {
					writer.key(sheetName);
					JsonUtil.sheetToJson(writer, sheet, null);
				}
			}
		}
		if (this.outputRecords != null) {
			for (OutputRecord rec : this.outputRecords) {
				rec.toJson(writer, ctx);
			}
		}
		/*
		 * we also push non-error messages
		 */
		writer.key(ServiceProtocol.MESSAGES).array();
		for (FormattedMessage msg : ctx.getMessages()) {
			if (msg.messageType != MessageType.ERROR) {
				msg.writeJsonValue(writer);
			}
		}
		writer.endArray().endObject();
		outData.setPayLoad(writer.toString());
	}

	/**
	 * get ready for a long-haul service :-)
	 */
	public void getReady() {
		if (this.outputRecords == null) {
			return;
		}

		int nbrChildren = 0;
		if (this.outputRecords != null) {
			for (OutputRecord rec : this.outputRecords) {
				rec.getReady();
				if (rec.parentSheetName != null) {
					nbrChildren++;
				}
			}
		}

		if (nbrChildren == 0) {
			return;
		}
		/*
		 * OK. we have to deal with hierarchical data output
		 */
		/*
		 * store child records for a parent. we know the upper limit on the
		 * array. Hence we use an array instead of list. We take care of null
		 * entries while handling it
		 */
		OutputRecord[] children = new OutputRecord[nbrChildren];

		/*
		 * we go by parent, and accumulate children
		 */
		for (OutputRecord parent : this.outputRecords) {
			String sheetName = parent.sheetName;
			if (sheetName == null) {
				continue;
			}
			/*
			 * look for possible child claiming this sheet be parent :-)
			 */
			int idx = 0;
			for (OutputRecord child : this.outputRecords) {
				if (sheetName.equals(child.parentSheetName)) {
					children[idx++] = child;
					nbrChildren--; // for tracking
				}
			}
			if (idx != 0) {
				parent.setChildren(children, idx);
				/*
				 * are we done?
				 */
				if (nbrChildren == 0) {
					break;
				}
				/*
				 * we utilized current array. Create new one for next parent.
				 */
				children = new OutputRecord[nbrChildren];
			}
		}
		/*
		 * are there children still looking for parents?
		 */
		if (nbrChildren != 0) {
			throw new ApplicationError(
					"Please check parentSheetName attribute for inputRecords. We found "
							+ nbrChildren
							+ " sheets with missing parent sheets!!");
		}
	}

	/**
	 * validate this specification
	 *
	 * @param ctx
	 * @return number of errors added
	 */
	int validate(ValidationContext ctx) {
		if (this.outputRecords == null) {
			return 0;
		}

		int count = 0;
		Set<String> allSheets = new HashSet<String>();
		Set<String> parentSheets = new HashSet<String>();
		for (OutputRecord rec : this.outputRecords) {
			count += rec.validate(ctx);
			allSheets.add(rec.sheetName);
			if (rec.parentSheetName != null) {
				parentSheets.add(rec.parentSheetName);
			}
		}
		if (parentSheets.size() == 0) {
			return count;
		}
		for (String sheetName : parentSheets) {
			if (allSheets.contains(sheetName) == false) {
				ctx.addError(sheetName
						+ " is designated as a parent, but it is not as sheet name in any output record.");
				count++;
			}
		}
		return count;
	}
}
