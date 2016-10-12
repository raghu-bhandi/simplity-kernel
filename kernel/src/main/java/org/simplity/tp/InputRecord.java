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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.tp;

import java.util.ArrayList;
import java.util.List;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataPurpose;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.dm.Field;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceMessages;

/**
 * represents a record/row/table that is used as input for a service
 */
public class InputRecord {

	/**
	 * fully qualified name of the record that we are expecting as input. In
	 * very special case, like some utility service that is internally used, we
	 * may skip record. If record is skipped, input is accepted as it is with no
	 * validation. Must be used with care.
	 */
	String recordName;

	/**
	 * For the purpose of input parsing and validation, assume this record has
	 * only these subset of fields. null if full row is in force.
	 */
	String[] fieldNames = null;

	/**
	 * min rows expected. Used for validating input.
	 */
	int minRows = 0;

	/**
	 * certainly a good idea to limit rows from a practical view.
	 */
	int maxRows = Integer.MAX_VALUE;
	/**
	 * why is this record being input? we extract and validate input based on
	 * this purpose
	 */
	DataPurpose purpose = DataPurpose.SUBSET;

	/**
	 * name of sheet in which we are expecting data. null if data is not
	 * expected in a sheet
	 */
	String sheetName = null;

	/**
	 * are we expecting the special field that indicates how to save data?
	 */
	boolean saveActionExpected;

	/**
	 * client may use object paradigm and send data as hierarchical object
	 * structure. We need to extract them into related sheets. Is this sheet
	 * expected as child rows of a parent sheet? Rows for this sheet are
	 * available with an attribute name same as this sheet name
	 */
	String parentSheetName;

	/**
	 * and we need to know how to identify child rows for a parent row. we use
	 * common columns in the two sheets to link them
	 */
	String linkColumnInThisSheet;

	/**
	 * field name in parent sheet that links a parent row to rows in this sheet
	 */
	String linkColumnInParentSheet;

	private Field[] fields = null;
	private boolean hasInterFieldValidations = false;

	/**
	 * default constructor
	 */
	public InputRecord() {
		// default
	}

	/**
	 * create an output record for a data
	 *
	 * @param sheetName
	 */
	public InputRecord(String sheetName) {
		this.sheetName = sheetName;
	}

	/**
	 * create output record for a child sheet
	 *
	 * @param sheetName
	 * @param parentSheetName
	 * @param childColName
	 * @param parentColName
	 */
	public InputRecord(String sheetName, String parentSheetName,
			String childColName, String parentColName) {
		this.sheetName = sheetName;
		this.parentSheetName = parentSheetName;
		this.linkColumnInThisSheet = childColName;
		this.linkColumnInParentSheet = parentColName;
	}

	/**
	 * parse fields of this record from inData into context. Error if any, will
	 * be added to context. We parse all fields irrespective of validation
	 * errors.
	 *
	 * @param json
	 *            from which data is to be extracted
	 * @param ctx
	 *            into which data is extracted
	 */
	public void extractInput(JSONObject json, ServiceContext ctx) {
		if (this.fields == null) {
			JsonUtil.extractWithNoValidation(json, ctx, this.sheetName,
					this.parentSheetName);
			return;
		}
		boolean allFieldsAreOptional = this.purpose == DataPurpose.SUBSET;
		List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
		if (this.sheetName == null) {
			int nbr = 0;
			if (this.purpose == DataPurpose.FILTER) {
				nbr = JsonUtil.extractFilterFields(json, this.fields, ctx,
						errors);

			} else {
				nbr = JsonUtil.extractFields(json, this.fields, ctx, errors,
						allFieldsAreOptional);
			}
			Tracer.trace("Input Record extracted " + nbr + " fields into ctx");

			if (errors.size() > 0) {
				Tracer.trace(" We got " + errors.size()
						+ " validaiton errors before inter-field validations");
			}
			if (this.hasInterFieldValidations && allFieldsAreOptional == false
					&& nbr > 1) {
				for (Field field : this.fields) {
					field.validateInterfield(ctx, errors, null);
				}
			}
			if (errors.size() > 0) {
				Tracer.trace(" We got " + errors.size()
						+ " validaiton errors during this record input");
				ctx.addMessages(errors);
			}
			return;
		}
		/*
		 * use record for validating input
		 */
		String arrName = this.sheetName;
		if (this.parentSheetName != null) {
			arrName = this.parentSheetName;
		}
		JSONArray data = null;
		if (this.sheetName != null) {
			data = json.optJSONArray(arrName);
		}
		/*
		 * we extract fields if sheet is not specified, or the specified sheet
		 * is not found
		 */
		if (data == null) {
			if (this.minRows > 1) {
				ctx.addMessage(ServiceMessages.MIN_INPUT_ROWS, ""
						+ this.minRows, "" + this.maxRows);
			} else {
				Tracer.trace("No data for sheet " + this.sheetName);
			}
			return;
		}

		/*
		 * we are now dealing with a data sheet
		 */
		DataSheet sheet = null;
		if (this.parentSheetName != null) {
			sheet = JsonUtil.getChildSheet(data, this.sheetName, this.fields,
					errors, allFieldsAreOptional);
		} else {
			sheet = JsonUtil.getSheet(data, this.fields, errors,
					allFieldsAreOptional);
		}
		if (errors.isEmpty() == false) {
			ctx.addMessages(errors);
			return;
		}
		// sheet is always non-null
		if (sheet == null) {
			if (this.minRows > 0) {
				ctx.addMessage(Messages.VALUE_REQUIRED, this.sheetName
						+ " requires a min of " + this.minRows);
			}
			return;
		}
		int nbrRows = sheet.length();
		if (nbrRows < this.minRows
				|| (this.maxRows != 0 && nbrRows > this.maxRows)) {
			if (this.minRows > 0) {
				ctx.addMessage(ServiceMessages.MIN_INPUT_ROWS, ""
						+ this.minRows, "" + this.maxRows);
			} else {
				ctx.addMessage(ServiceMessages.MAX_INPUT_ROWS, ""
						+ this.maxRows, "" + this.maxRows);
			}
			return;
		}
		if (this.hasInterFieldValidations) {
			for (FieldsInterface row : sheet) {
				for (Field field : this.fields) {
					field.validateInterfield(row, errors, this.sheetName);
				}
			}

		}
		if (nbrRows > 0) {
			ctx.putDataSheet(this.sheetName, sheet);
		}
		Tracer.trace(nbrRows + " rows extracted for record " + this.recordName);
	}

	/**
	 * called once on loading the component
	 */
	public void getReady() {
		if (this.recordName == null) {
			if (this.sheetName == null) {
				throw new ApplicationError(
						"Input record has no record or sheet specified");
			}
			if (this.fieldNames != null) {
				throw new ApplicationError(
						"Field names is meant to specify a subset of fields in record. Valid only if record is specified.");
			}
			Tracer.trace("WARNING : Input in sheet "
					+ this.sheetName
					+ " will be accepted as it is with no validation whatsoever.");
		} else {
			Record record = ComponentManager.getRecord(this.recordName);
			this.fields = record.getFieldsToBeExtracted(this.fieldNames,
					this.purpose, this.saveActionExpected);
			this.hasInterFieldValidations = record.hasInterFieldValidations();
		}
		if (this.parentSheetName != null && this.sheetName == null) {
			throw new ApplicationError(
					"Can not have a parent sheet name without a sheet name for this record");
		}

	}

	/**
	 * @param ctx
	 * @return number of errors added
	 */
	int validate(ValidationContext ctx) {
		int count = 0;
		if (this.recordName == null) {
			if (this.sheetName == null) {
				ctx.addError("Input record has no record or sheet specified");
				count++;
			}
			if (this.fieldNames != null) {
				ctx.addError("Field names is meant to specify a subset of fields in record. Valid only if record is specified.");
				count++;
			}
		}
		count += ctx.checkRecordExistence(this.recordName, "recordName", false);
		/*
		 * can not have parent sheet if sheet itself is not there
		 */
		if (this.parentSheetName != null) {
			if (this.sheetName == null) {
				ctx.addError("Can not have a parent sheet name without a sheet name for this record");
				count++;
			}
			if (this.linkColumnInParentSheet == null
					|| this.linkColumnInThisSheet == null) {
				ctx.addError("linkColumnInParentSheet and linkColumnInThisSheet are required to link this sheet with parent sheet.");
				count++;
			}
		}

		if (this.minRows < 0 || this.maxRows < 0 || this.minRows > this.maxRows) {
			ctx.addError("minRows and maxRows are to be positive and min should not be greater than max.");
			count++;
		}
		return count;
	}
}
