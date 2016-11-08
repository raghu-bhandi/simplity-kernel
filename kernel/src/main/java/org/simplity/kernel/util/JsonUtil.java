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

package org.simplity.kernel.util;

import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.json.Jsonable;
import org.simplity.kernel.FilterCondition;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.data.HierarchicalSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.dm.Field;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceProtocol;

/**
 * utilities that help Simplity deal with JSON
 *
 * @author simplity.org
 *
 */
public class JsonUtil {
	/**
	 * create a data sheet out of a well-formed json array of simple jsonObject.
	 *
	 * @param arr
	 *            that has the json array
	 * @param inputFields
	 *            Fields to be input. null if we are to take whatever is offered
	 * @param errors
	 *            to which any validation errors are added
	 * @param allFieldsAreOptional
	 *            true of we are to consider all fields as optional, even if the
	 *            field specifies it as mandatory
	 * @param parentFieldName
	 *            if this is a child sheet, specify the column name in this
	 *            sheet that should be populated with the parent key value
	 * @param parentValue
	 *            if this is a child sheet, and you have specified
	 *            parentFieldName, value to be populated in each row for that
	 *            column
	 * @return data sheet. Null if no data found or the json is not well
	 *         formated. was null. case the array is not well-formed
	 */
	public static DataSheet getSheet(JSONArray arr, Field[] inputFields,
			List<FormattedMessage> errors, boolean allFieldsAreOptional,
			String parentFieldName, Value parentValue) {
		if (arr == null || arr.length() == 0) {
			return null;
		}
		Field[] fields = inputFields;
		int parentIdx = -1;
		if (fields == null) {
			/*
			 * we guess the fields based on the attributes of first element in
			 * the array
			 */
			JSONObject exampleObject = arr.optJSONObject(0);
			if (exampleObject == null) {
				Tracer.trace("Json array has its first object as null, and hence we abandoned parsing it.");
				return null;
			}
			fields = getFields(exampleObject);
			if (parentFieldName != null) {
				Field[] newFields = new Field[fields.length + 1];
				newFields[0] = Field.getDefaultField(parentFieldName,
						parentValue.getValueType());
				int j = 1;
				for (Field field : fields) {
					newFields[j] = field;
					j++;
				}
				parentIdx = 0;
			}
		} else if (parentFieldName != null) {
			int j = 0;
			for (Field field : fields) {
				if (field.getName().equals(parentFieldName)) {
					parentIdx = j;
					break;
				}
				j++;
			}
			if (parentIdx == -1) {
				Tracer.trace("Parent field name "
						+ parentFieldName
						+ " not found in the fields list for child. Filed will not be populated from parent sheet.");
			}
		}
		DataSheet ds = new MultiRowsSheet(fields);
		int nbrRows = arr.length();
		/*
		 * let us now extract each row into data sheet
		 */
		for (int i = 0; i < nbrRows; i++) {
			JSONObject obj = arr.optJSONObject(i);
			if (obj == null) {
				Tracer.trace("Row " + (i + 1) + " is null. Not extracted");
				continue;
			}
			int j = 0;
			Value[] row = new Value[fields.length];
			for (Field field : fields) {
				Object val = obj.opt(field.getName());
				if (j == parentIdx) {
					row[j] = parentValue;
				} else {
					row[j] = field.parseObject(val, errors,
							allFieldsAreOptional, null);
				}
				j++;
			}
			ds.addRow(row);
		}
		return ds;
	}

	/**
	 * supplied jsonArray has the parent rows. Extract child rows from these
	 * array elements
	 *
	 * @param arr
	 * @param attName
	 *            attribute name that holds the child JSONArray
	 * @param fields
	 *            expected fields. INput data is validated as per these field
	 *            specifications.
	 * @param errors
	 * @param allFieldsAreOptional
	 *
	 * @return data sheet. Null if no data found. Throws ApplicationError on
	 *         case the array is not well-formed
	 */
	@SuppressWarnings("null")
	public static DataSheet getChildSheet(JSONArray arr, String attName,
			Field[] fields, List<FormattedMessage> errors,
			boolean allFieldsAreOptional) {
		/*
		 * arr corresponds to following json. We are to accumulate child rows
		 * across all main rows
		 *
		 * [...,"attName"=[{},{}....],..],[....,"attName"=[{},{}.... ],..]....
		 */
		Field[] inputFields = fields;
		DataSheet ds = null;
		if (inputFields != null) {
			ds = new MultiRowsSheet(inputFields);
		}
		/*
		 * we are not sure of getting a valid child row in first element. So,
		 * let us have a flexible strategy
		 */
		int nbrParentRows = arr.length();
		/*
		 * for each parent row
		 */
		for (int i = 0; i < nbrParentRows; i++) {
			JSONObject pr = arr.optJSONObject(i);
			if (pr == null) {
				continue;
			}
			JSONArray rows = pr.optJSONArray(attName);
			if (rows == null) {
				continue;
			}
			int n = rows.length();
			/*
			 * extract this child row into ds
			 */
			for (int idx = 0; idx < n; idx++) {
				JSONObject obj = rows.optJSONObject(idx);
				if (obj == null) {
					continue;
				}
				if (inputFields == null) {
					inputFields = getFields(obj);
					ds = new MultiRowsSheet(inputFields);
				}
				int j = 0;
				Value[] row = new Value[fields.length];
				for (Field field : inputFields) {
					Object val = obj.opt(field.getName());
					row[j] = field.parseObject(val, errors,
							allFieldsAreOptional, attName);
					j++;
				}
				ds.addRow(row);
			}
		}
		return ds;
	}

	/**
	 * write the data sheet to json
	 *
	 * @param writer
	 * @param ds
	 * @param childSheets
	 */
	public static void sheetToJson(JSONWriter writer, DataSheet ds,
			HierarchicalSheet[] childSheets) {
		int nbrRows = 0;
		int nbrCols = 0;
		if (ds != null) {
			nbrRows = ds.length();
			nbrCols = ds.width();
		}
		if (ds == null || nbrRows == 0 || nbrCols == 0) {
			writer.value(null);
			Tracer.trace("Sheet  has no data. json is not added");
			return;
		}
		Tracer.trace("Going to generate Json with "
				+ (childSheets == null ? "no " : (childSheets.length + " "))
				+ "child sheets");
		writer.array();
		String[] names = ds.getColumnNames();
		for (int i = 0; i < nbrRows; i++) {
			writer.object();
			/*
			 * note that getRow() returns values in the same order as in
			 * getColumnNames()
			 */
			Value[] row = ds.getRow(i);
			int j = 0;
			for (String colName : names) {
				Value value = row[j];
				/*
				 * no need to write null attributes
				 */
				if (value != null) {
					writer.key(colName).value(value.toObject());
				}
				j++;
			}

			if (childSheets != null) {
				for (HierarchicalSheet childSheet : childSheets) {
					childSheet.toJson(writer, row);
				}
			}
			writer.endObject();
		}
		writer.endArray();
	}

	/**
	 * create a data sheet for attributes in this object
	 *
	 * @param obj
	 * @param parentName
	 * @return
	 */
	private static Field[] getFields(JSONObject obj) {
		String[] names = JSONObject.getNames(obj);
		int nbrCols = names.length;
		int j = 0;
		Field[] fields = new Field[nbrCols];
		int nonAtts = 0;
		for (String colName : names) {
			Object val = obj.opt(colName);
			if (val instanceof JSONArray || val instanceof JSONObject) {
				/*
				 * this is not a att-value.
				 */
				nonAtts++;
			} else {
				Value value = Value.parseObject(val);
				fields[j] = Field
						.getDefaultField(colName, value.getValueType());
				j++;
			}
		}
		if (nonAtts == 0) {
			return fields;
		}

		/*
		 * this is rare case, and hence we have not optimized the algorithm for
		 * this case. non-primitive attributes would have their valueType set to
		 * null. Copy primitive-ones to a new array.
		 */
		nbrCols = nbrCols - nonAtts;
		Field[] newFields = new Field[nbrCols];
		int newIdx = 0;
		for (Field field : fields) {
			if (field != null) {
				newFields[newIdx++] = field;
			}
		}
		return newFields;
	}

	/**
	 * @param json
	 * @param fields
	 * @param ctx
	 * @param errors
	 * @param allFieldsAreOptional
	 * @return number of fields extracted
	 */
	public static int extractFields(JSONObject json, Field[] fields,
			FieldsInterface ctx, List<FormattedMessage> errors,
			boolean allFieldsAreOptional) {
		int result = 0;
		for (Field field : fields) {
			Object val = json.opt(field.getName());
			Value value = null;
			if (val != null) {
				value = field.getValueType().parseObject(val);
				if (value == null) {
					errors.add(new FormattedMessage(Messages.INVALID_VALUE,
							null, field.getName(), null, 0, '\''
							+ val.toString() + "' is not a valid "
							+ field.getValueType()));
					continue;
				}
			}
			/*
			 * this is validation, and not exactly parse.
			 */
			value = field.parse(value, errors, allFieldsAreOptional, null);
			if (value != null) {
				ctx.setValue(field.getName(), value);
				result++;
			}
		}
		return result;
	}

	/**
	 * @param json
	 * @param fields
	 * @param ctx
	 * @param errors
	 * @return number of fields extracted
	 */
	public static int extractFilterFields(JSONObject json, Field[] fields,
			FieldsInterface ctx, List<FormattedMessage> errors) {
		int result = 0;
		for (Field field : fields) {
			result += parseFilter(json, ctx, errors, field.getName(),
					field.getValueType());
		}
		/*
		 * some additional fields for filter, like sort
		 */
		/*
		 * what about sort ?
		 */
		String fieldName = ServiceProtocol.SORT_COLUMN_NAME;
		String textValue = json.optString(fieldName, null);
		if (textValue != null) {
			Value value = ComponentManager.getDataType(DataType.ENTITY_LIST)
					.parseValue(textValue);
			if (value == null) {
				errors.add(new FormattedMessage(Messages.INVALID_ENTITY_LIST,
						null, fieldName, null, 0));
			} else {
				ctx.setValue(fieldName, value);
			}
		}

		fieldName = ServiceProtocol.SORT_ORDER;
		textValue = json.optString(fieldName, null);
		if (textValue != null) {
			textValue = textValue.toLowerCase();
			if (textValue.equals(ServiceProtocol.SORT_ORDER_ASC)
					|| textValue.equals(ServiceProtocol.SORT_ORDER_DESC)) {
				ctx.setValue(fieldName, Value.newTextValue(textValue));
			} else {
				errors.add(new FormattedMessage(Messages.INVALID_SORT_ORDER,
						null, fieldName, null, 0));
			}
		}
		return result;
	}

	/**
	 * parse input object as a filter field
	 *
	 * @param json
	 * @param extratedFields
	 *            to which extracted fields are to be added
	 * @param validationErrors
	 * @param recordName
	 * @return number of fields extracted
	 */
	private static int parseFilter(JSONObject json,
			FieldsInterface extratedFields,
			List<FormattedMessage> validationErrors, String fieldName,
			ValueType valueType) {

		Object obj = json.opt(fieldName);
		if (obj == null) {
			return 0;
		}
		/*
		 * what is the comparator
		 */
		String otherName = fieldName + ServiceProtocol.COMPARATOR_SUFFIX;
		String otherValue = json.optString(otherName, null);
		FilterCondition f = FilterCondition.parse(otherValue);
		/*
		 * filter field need not conform to data-type but it should be of the
		 * same value type, except that IN_LIST is alwasy text
		 */
		Value value = FilterCondition.In == f ? ValueType.TEXT.parseObject(obj)
				: valueType.parseObject(obj);
		if (value == null) {
			if (validationErrors != null) {
				validationErrors.add(new FormattedMessage(
						Messages.INVALID_VALUE, null, fieldName, null, 0));
			}
		} else {
			extratedFields.setValue(fieldName, value);
		}
		if (f == null) {
			extratedFields.setValue(otherName,
					Value.newTextValue(ServiceProtocol.EQUAL));
			return 1;
		}
		extratedFields.setValue(otherName, Value.newTextValue(otherValue));
		if (f != FilterCondition.Between) {
			return 1;
		}
		otherName = fieldName + ServiceProtocol.TO_FIELD_SUFFIX;
		Object val = json.opt(otherName);
		value = null;
		if (val != null) {
			value = valueType.parseObject(val);
		}
		if (value == null) {
			if (validationErrors != null) {
				validationErrors.add(new org.simplity.kernel.FormattedMessage(
						Messages.INVALID_VALUE, null, otherName, null, 0));
			}
		} else {
			extratedFields.setValue(otherName, value);
		}
		return 1;
	}

	/**
	 * extract a simple json object (with fields and tables) into service
	 * context
	 *
	 * @param json
	 * @param ctx
	 */
	public static void extractAll(JSONObject json, ServiceContext ctx) {
		for (String key : json.keySet()) {
			JSONArray arr = json.optJSONArray(key);
			if (arr != null) {
				DataSheet sheet = JsonUtil.getSheet(arr, null, null, true,
						null, null);
				if (sheet == null) {
					Tracer.trace("Table " + key + " could not be etxracted");
				} else {
					ctx.putDataSheet(key, sheet);
					Tracer.trace("Table " + key + " etxracted with "
							+ sheet.length() + " rows");
				}
				continue;
			}
			JSONObject obj = json.optJSONObject(key);
			if (obj != null) {
				/*
				 * we do not have a standard for converting data structure. As
				 * of now, we just copy this json
				 */
				ctx.setObject(key, obj);
				Tracer.trace(key + " retained as a JSON into ctx");
				continue;
			}
			Object val = json.opt(key);
			Value value = Value.parseObject(val);
			if (value != null) {
				ctx.setValue(key, value);
				Tracer.trace(key + " = " + value + " extracted");
			} else {
				Tracer.trace(key + " = " + val + " is not extracted");
			}
		}
	}

	/**
	 *
	 * @param json
	 * @param ctx
	 * @param sheetName
	 * @param parentSheetName
	 */
	public static void extractWithNoValidation(JSONObject json,
			ServiceContext ctx, String sheetName, String parentSheetName) {
		DataSheet ds = null;
		String arrName = null;
		JSONArray arr = null;
		if (parentSheetName != null) {
			arr = json.optJSONArray(parentSheetName);
			if (arr == null) {
				arrName = parentSheetName;
			} else {
				ds = getChildSheet(arr, sheetName, null, null, true);
			}
		} else {
			arr = json.optJSONArray(parentSheetName);
			if (arr == null) {
				arrName = sheetName;
			} else {
				ds = JsonUtil.getSheet(arr, null, null, true, null, null);
			}
		}
		if (arr == null) {
			Tracer.trace("No data found for sheet " + arrName);
		} else if (ds == null) {
			Tracer.trace("Sheet " + arrName
					+ " has only null data. Data not extracted");
		} else {
			ctx.putDataSheet(sheetName, ds);
		}
	}

	/**
	 * @param writer
	 * @param fieldNames
	 * @param ctx
	 */
	public static void addAttributes(JSONWriter writer, String[] fieldNames,
			ServiceContext ctx) {
		for (String fieldName : fieldNames) {
			Value value = ctx.getValue(fieldName);
			if (value != null) {
				if (value.isUnknown() == false) {
					writer.key(fieldName).value(value.toObject());
				}
				continue;
			}
			Object obj = ctx.getObject(fieldName);
			if (obj != null) {
				writer.key(fieldName);
				addObject(writer, obj);
			}
		}
	}

	/**
	 * write an arbitrary object to json
	 *
	 * @param writer
	 * @param obj
	 */
	public static void addObject(JSONWriter writer, Object obj) {
		if (obj instanceof Jsonable) {
			((Jsonable) obj).writeJsonValue(writer);
			return;
		}
		if (obj instanceof String || obj instanceof Number
				|| obj instanceof Boolean || obj instanceof Date
				|| obj instanceof Enum) {
			writer.value(obj);
			return;
		}
		if (obj.getClass().isArray()) {
			writer.array();
			int n = Array.getLength(obj);
			for (int i = 0; i < n; i++) {
				addObject(writer, Array.get(obj, i));
			}
			writer.endArray();
			return;
		}
		if (obj instanceof Map) {
			writer.object();
			@SuppressWarnings("unchecked")
			Map<String, Object> childMap = (Map<String, Object>) obj;
			for (Map.Entry<String, Object> childEntry : childMap.entrySet()) {
				writer.key(childEntry.getKey());
				addObject(writer, childEntry.getValue());
			}
			writer.endObject();
			return;
		}
		if (obj instanceof Collection) {
			writer.array();
			@SuppressWarnings("unchecked")
			Collection<Object> children = (Collection<Object>) obj;
			for (Object child : children) {
				addObject(writer, child);
			}
			writer.endArray();
			return;
		}
		/*
		 * it is another object
		 */
		writer.object();
		for (Map.Entry<String, java.lang.reflect.Field> entry : ReflectUtil
				.getAllFields(obj).entrySet()) {
			writer.key(entry.getKey());
			try {
				addObject(writer, entry.getValue().get(obj));
			} catch (Exception e) {
				Tracer.trace("Unable to get value for object attribute "
						+ entry.getKey() + ". null assumed");
				writer.value(null);
			}
		}
		writer.endObject();
	}

	/**
	 * @param object
	 *            to be jsoned
	 * @return json string for the object
	 */
	public static String toJson(Object object) {
		Writer w = new StringWriter();
		JSONWriter writer = new JSONWriter(w);
		addObject(writer, object);
		return w.toString();
	}

	/**
	 * append the text to string builder duly quoted and escaped as per JSON
	 * standard.
	 *
	 * @param value
	 *            to be appended
	 * @param json
	 *            to be appended to
	 */
	public static void appendQoutedText(String value, StringBuilder json) {
		if (value == null || value.length() == 0) {
			json.append("\"\"");
			return;
		}

		char lastChar = 0;
		String hhhh;

		json.append('"');
		for (char c : value.toCharArray()) {
			switch (c) {
			case '\\':
			case '"':
				json.append('\\');
				json.append(c);
				break;
			case '/':
				if (lastChar == '<') {
					json.append('\\');
				}
				json.append(c);
				break;
			case '\b':
				json.append("\\b");
				break;
			case '\t':
				json.append("\\t");
				break;
			case '\n':
				json.append("\\n");
				break;
			case '\f':
				json.append("\\f");
				break;
			case '\r':
				json.append("\\r");
				break;
			default:
				if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
						|| (c >= '\u2000' && c < '\u2100')) {
					json.append("\\u");
					hhhh = Integer.toHexString(c);
					json.append("0000", 0, 4 - hhhh.length());
					json.append(hhhh);
				} else {
					json.append(c);
				}
			}
			lastChar = c;
		}
		json.append('"');
	}

	/**
	 * convert a JSON array to array of primitive objects.
	 *
	 * @param array
	 *            json Array
	 * @return array of primitives, or null in case any of the array element is
	 *         not primitive
	 */
	public static Object[] toObjectArray(JSONArray array) {
		Object[] result = new Object[array.length()];
		for (int i = 0; i < result.length; i++) {
			Object obj = array.get(i);
			if (obj == null) {
				continue;
			}
			if (obj instanceof JSONObject || obj instanceof JSONArray) {
				Tracer.trace("Element no (zero based) "
						+ i
						+ " is not a primitive, and hence unable to convert the JSONArray into an array of primtitives");
				return null;
			}
			result[i] = obj;
		}
		return result;
	}
}
