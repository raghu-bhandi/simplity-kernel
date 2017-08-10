/*
 * Copyright (c) 2017 simplity.org
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

package org.simplity.proto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.DataSheetLink;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.DateValue;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

/**
 * utility functions while dealing with proto
 *
 * @author simplity.org
 *
 */
public class ProtoUtil {
	private static Logger logger = LoggerFactory.getLogger(ProtoUtil.class);
	private static final int MESSAGE = -2;
	private static final int PRIMITIVE_ARRAY = -3;
	private static final int MESSAGE_ARRAY = -4;

	/**
	 * create a message populated with sample data
	 *
	 * @param builder
	 * @param suffix
	 *            a number to be used as suffix to text fields. typically row
	 *            number
	 * @return message with sample data in every one of its field
	 */
	public static Message createSample(Builder builder, int suffix) {
		Collection<FieldDescriptor> fields = builder.getDescriptorForType().getFields();
		for (FieldDescriptor fd : fields) {
			String fieldName = fd.getName();
			/*
			 * array
			 */
			if (fd.isRepeated()) {
				int n = (int) (Math.round(Math.random()) * 10 + 1);
				if (fd.getType() == Type.MESSAGE) {
					Builder childBuilder = builder.newBuilderForField(fd);
					for (int i = 0; i < n; i++) {
						Message msg = createSample(childBuilder, i);
						builder.addRepeatedField(fd, msg);
						childBuilder.clear();
					}
				} else {
					for (int i = 0; i < n; i++) {
						Object val = randomValue(fd, fieldName + " - " + i);
						builder.addRepeatedField(fd, val);
					}
				}
				continue;
			}
			/*
			 * embedded object
			 */
			if (fd.getType() == Type.MESSAGE) {
				Builder childBuilder = builder.newBuilderForField(fd);
				Message msg = createSample(childBuilder, suffix);
				builder.setField(fd, msg);
				continue;
			}
			builder.setField(fd, randomValue(fd, fieldName + ' ' + suffix));
		}
		return builder.build();
	}

	@SuppressWarnings("boxing")
	private static Object randomValue(FieldDescriptor fd, String suggestion) {
		Type ft = fd.getType();
		if (ft == Type.STRING) {
			return suggestion;
		}
		int val = (int) Math.round(Math.random() * 1000);

		if (ft == Type.BOOL) {
			return 0 == val % 2;
		}
		if (ft == Type.ENUM) {
			EnumDescriptor ed = fd.getEnumType();
			int n = ed.getValues().size() - 1;
			return ed.findValueByNumber(val % n + 1);
		}
		return val;
	}

	/**
	 * create an empty sheet for all primitive fields in the collection
	 *
	 * @param fields
	 *
	 * @return data sheet that can be used to extract data from this message
	 */
	public static DataSheet createEmptySheet(Collection<FieldDescriptor> fields) {
		int nbr = fields.size();
		String[] names = new String[nbr];
		ValueType[] types = new ValueType[nbr];

		int col = 0;
		for (FieldDescriptor field : fields) {
			if (field.isRepeated() || field.getType() == Type.MESSAGE) {
				logger.info("found an embedded array/message. Will be extracyed separately ");
				continue;
			}
			names[col] = field.getName();
			types[col] = valueTypeForFied(field);
			col++;
		}
		if (col == nbr) {
			return new MultiRowsSheet(names, types);
		}
		/*
		 * there were some non-primitive fields. reduce the arrays..
		 */
		String[] newNames = new String[col];
		ValueType[] newTypes = new ValueType[col];
		for (int i = 0; i < newNames.length; i++) {
			newNames[i] = names[i];
			newTypes[i] = types[i];
		}
		return new MultiRowsSheet(newNames, newTypes);
	}

	/**
	 * what is an appropriate value type for this field? Unfortunately, date is
	 * not supported, and we can not detect which long/int is meant to be date.
	 * That will be handled later.
	 *
	 * @param field
	 * @return value type most suited for this primitive field
	 */
	public static ValueType valueTypeForFied(FieldDescriptor field) {
		if (field.isRepeated()) {
			throw new ApplicationError(
					"Field " + field.getName() + " is not a primitive, but is being used as one from a protobuf.");
		}
		Type type = field.getType();
		switch (type) {
		case BOOL:
			return ValueType.BOOLEAN;

		case ENUM:
		case STRING:
		case BYTES:
			return ValueType.TEXT;

		case DOUBLE:
		case FIXED32:
		case FIXED64:
		case SFIXED32:
		case FLOAT:
		case SFIXED64:
			return ValueType.DECIMAL;
		case GROUP:
			throw new ApplicationError("We do not handle group fields in protobuf yet..");
		default:
			return ValueType.INTEGER;
		}
	}

	/**
	 * build a child-message from a row of a data sheet
	 *
	 * @param builder
	 *            for this child message
	 * @param indexes
	 *            or each field, column index from where to get the value. Note
	 *            special cases for child-object nd child-array
	 * @param fields
	 *            of this message
	 * @param row
	 *            of data
	 * @param ctx
	 */
	public static void buildMessage(Builder builder, int[] indexes, List<FieldDescriptor> fields, Value[] row,
			ServiceContext ctx) {
		for (int i = 0; i < indexes.length; i++) {
			int idx = indexes[i];
			FieldDescriptor childField = fields.get(i);
			if (idx >= 0) {
				/*
				 * primitive value
				 */
				Value value = row[idx];
				if (!Value.isNull(value)) {
					builder.setField(childField, convertFieldValue(childField, value));
				}
				continue;
			}
			if (idx == -1) {
				/*
				 * no source of data :-(
				 */
				continue;
			}
			/*
			 * child message or array. data comes from a data sheet
			 */
			DataSheet childSheet = ctx.getChildSheet(childField.getName(), row);
			if (childSheet == null) {
				logger.info("Child data sheet found, and hence no data for field {} for this row");
				continue;
			}
			if (idx == MESSAGE_ARRAY) {
				setMessagesArray(builder, childField, childSheet, ctx);
			} else if (idx == MESSAGE) {
				setMessage(builder, childField, childSheet, ctx);
			} else {
				setPrimitiveArray(builder, childField, childSheet);
			}
		}
	}

	/**
	 * set a child-message from the first row of the data sheet
	 *
	 * @param parentBuilder
	 * @param fd
	 *            field
	 * @param sheet
	 *            data sheet
	 * @param ctx
	 *            service context
	 *
	 */
	public static void setMessage(Builder parentBuilder, FieldDescriptor fd, DataSheet sheet, ServiceContext ctx) {
		List<FieldDescriptor> fields = fd.getMessageType().getFields();
		int[] colIndexes = mapFieldsToColumns(fields, sheet);

		Builder childBuilder = parentBuilder.newBuilderForField(fd);
		/*
		 * following method builds, but does not create the message..
		 */
		buildMessage(childBuilder, colIndexes, fields, sheet.getRow(0), ctx);
		parentBuilder.setField(fd, childBuilder.build());
	}

	/**
	 *
	 * @param fd
	 * @param value
	 * @return object representation of value that is suitable for this field
	 */
	public static Object convertFieldValue(FieldDescriptor fd, Value value) {
		Type type = fd.getType();
		if (type == Type.STRING) {
			return value.toString();
		}
		if (value.getValueType() == ValueType.DATE) {
			return new Long(((DateValue) value).getDate());
		}
		if (type == Type.ENUM) {
			return fd.getEnumType().findValueByName(value.toString());
		}
		return value.toObject();
	}

	/**
	 * map field names to column names
	 *
	 * @param fields
	 * @param sheet
	 * @return array of indexes that has the index of the column in the data
	 *         sheet for each field. -1 means that the data sheet has no column
	 *         for this field, -2 means it is an embedded array and -3 means it
	 *         is an embedded Message
	 */
	public static int[] mapFieldsToColumns(List<FieldDescriptor> fields, DataSheet sheet) {
		int[] colIndexes = new int[fields.size()];
		for (int i = 0; i < colIndexes.length; i++) {
			FieldDescriptor field = fields.get(i);
			if (field.isRepeated()) {
				if (field.getType() == Type.MESSAGE) {
					colIndexes[i] = MESSAGE_ARRAY;
				} else {
					colIndexes[i] = PRIMITIVE_ARRAY;
				}
			} else if (field.getType() == Type.MESSAGE) {
				colIndexes[i] = MESSAGE;
			} else {
				colIndexes[i] = sheet.getColIdx(field.getName());
			}
		}
		return colIndexes;
	}

	/**
	 *
	 * @param parentBuilder
	 * @param fd
	 * @param sheet
	 * @param ctx
	 *            name of the field that has this object
	 */
	public static void setMessagesArray(Builder parentBuilder, FieldDescriptor fd, DataSheet sheet,
			ServiceContext ctx) {
		Builder builder = parentBuilder.newBuilderForField(fd);
		List<FieldDescriptor> fields = fd.getMessageType().getFields();
		/*
		 * we optimize extraction of data from sheet by taking a row at a time,
		 * and cache the col indexes
		 */
		int[] colIndexes = mapFieldsToColumns(fields, sheet);
		/*
		 * now create a message for each row, and add it to the field
		 */
		int nbrRows = sheet.length();
		for (int rowIdx = 0; rowIdx < nbrRows; rowIdx++) {
			builder.clear();
			buildMessage(builder, colIndexes, fields, sheet.getRow(rowIdx), ctx);
			parentBuilder.addRepeatedField(fd, builder.build());
		}
	}

	/**
	 * @param builder
	 * @param fd
	 * @param sheet
	 */
	public static void setPrimitiveArray(Builder builder, FieldDescriptor fd, DataSheet sheet) {
		int nbr = sheet.length();
		for (int i = 0; i < nbr; i++) {
			Value value = sheet.getRow(i)[0];
			if (!Value.isNull(value)) {
				builder.addRepeatedField(fd, convertFieldValue(fd, value));
			}
		}
	}

	/*
	 * methods to extract data from protobuf message into service context
	 */

	/**
	 * create a data sheet for an array of primitive values.
	 *
	 * @param fieldName
	 * @param arr
	 * @return data sheet with one column
	 */
	public static DataSheet arrayToSheet(String fieldName, Object[] arr) {
		String[] names = { fieldName };
		Value[][] values = new Value[arr.length][1];
		for (int i = 0; i < arr.length; i++) {
			values[i][0] = Value.parseObject(arr[i]);
		}
		return new MultiRowsSheet(names, values);
	}

	/**
	 * add array elements to rows of a data sheet.
	 *
	 * @param arr
	 * @param sheet
	 */
	public static void appendArrayToSheet(Object[] arr, DataSheet sheet) {
		if (sheet.width() != 1) {
			throw new ApplicationError("Data sheet for an array should have just one column We found " + sheet.width());
		}
		for (int i = 0; i < arr.length; i++) {
			Value[] row = { Value.parseObject(arr[i]) };
			sheet.addRow(row);
		}
	}


	/**
	 * if any of the child field is message/array, we have to extract that
	 *
	 * @param fieldName
	 * @param fieldValue
	 *
	 * @param childFields
	 * @param parentRow
	 * @param ctx
	 */
	public static void extractEmbeddedData(String fieldName, Object fieldValue, List<FieldDescriptor> childFields,
			Value[] parentRow, ServiceContext ctx) {
		/*
		 * this method would be quite confusing if you just read it. You MUST
		 * have a specific example on your note-book. Keep referring back to
		 * this example and write down values of fields/variable for you to
		 * follow the logic.
		 *
		 * long one, but not too many nesting. Hence not re-factored.
		 * But tricky to break into smaller methods because we have to recurse
		 */
		Message[] messages; // one row to be added for each message
		if (fieldValue instanceof Message) {
			messages = new Message[1];
			messages[0] = (Message) fieldValue;
		} else {
			messages = ((Collection<?>) fieldValue).toArray(new Message[0]);
		}
		/*
		 * data sheet to which we have to add rows. This may be already there in
		 * ctx
		 */
		DataSheet sheet = ctx.getDataSheet(fieldName);
		if (sheet == null) {
			if (parentRow != null) {
				logger.error(
						"Service context does not have a data sheet named {}, and hence this child sheet can not be extracted from protobuf input");
				return;
			}
			sheet = createEmptySheet(childFields);
			ctx.putDataSheet(fieldName, sheet);
		}
		/*
		 * if this is a child sheet, then we have to get ready to copy keys from
		 * the parent row to each of the child rows.
		 */
		DataSheetLink link = ctx.getDataSheetLink(fieldName);
		Value[] parentValues = null;
		int[] childIndexes = null;
		if (link == null) {
			if (parentRow != null) {
				logger.error(
						"Service context does not have a data-sheet-link for child sheet {}, and hence this child sheet can not be extracted from protobuf input");
				return;
			}
		} else {
			/*
			 * keep an array of parent keys.
			 */
			int[] indexes = link.parentIndexes;
			parentValues = new Value[indexes.length];
			for (int i = 0; i < parentValues.length; i++) {
				parentValues[i] = parentRow[indexes[i]];
			}

			/*
			 * this is the column index into child row to which the above values
			 * are to be copied
			 */
			childIndexes = link.childIndexes;
		}

		/*
		 * get ready to copy primitive values from fields of the message to row
		 * of a sheet.
		 * coumnIndexes[j] is the columnIndex in sheet for the jth field in
		 * fields.
		 */
		int[] columnIndexes = mapFieldsToColumns(childFields, sheet);

		int nbrCols = sheet.width();
		int nbrFields = childFields.size();
		ValueType[] types = sheet.getValueTypes();
		/*
		 * we may have grand-children as well. If so, we will have to handle
		 * that for each row.
		 */
		List<FieldDescriptor> nonPrimitiveFields = getNonPrimitivesFields(childFields);

		for (Message message : messages) {
			if (message == null) {
				logger.info("A null message is not added to the sheet");
				continue;
			}
			Value[] row = new Value[nbrCols];
			/*
			 * first set non-primitive field values to columns.
			 */
			for (int i = 0; i < nbrFields; i++) {
				int idx = columnIndexes[i];
				if (idx >= 0) { // - means non-primitive
					row[idx] = convertToValue(message.getField(childFields.get(i)), types[idx]);
				}
			}
			/*
			 * any parent field to be copied?
			 */
			if (parentValues != null && childIndexes != null) {
				for (int i = 0; i < parentValues.length; i++) {
					row[childIndexes[i]] = parentValues[i];
				}
			}

			sheet.addRow(row);

			if (nonPrimitiveFields == null) {
				continue;
			}
			/*
			 * dig deeper for data
			 */
			for (FieldDescriptor childField : nonPrimitiveFields) {
				if (childField.getType() != Type.MESSAGE) {
					logger.error(
							"We are yet to design extracting primitive arrays from embedded objects. Field {} not extracted.",
							childField.getName());
					continue;
				}
				Object childFieldValue = message.getField(childField);
				List<FieldDescriptor> grandChidren = childField.getMessageType().getFields();
				extractEmbeddedData(childField.getName(), childFieldValue, grandChidren, row, ctx);
			}
		}
	}

	/**
	 * get a subset of the field that are non-primitive
	 *
	 * @param fields
	 * @return null if all are primitive. if non-null, it is non-empty
	 */
	public static List<FieldDescriptor> getNonPrimitivesFields(List<FieldDescriptor> fields) {
		/*
		 * instantiate only if required, so that we return null in case we do
		 * not find anything
		 */
		List<FieldDescriptor> result = null;
		for (FieldDescriptor field : fields) {
			if (field.isRepeated() || field.getType() == Type.MESSAGE) {
				if (result == null) {
					result = new ArrayList<FieldDescriptor>();
				}
				result.add(field);
			}
		}
		return result;
	}

	/**
	 * @param fieldValue
	 * @param valueType
	 * @return parsed value of field value
	 */
	public static Value convertToValue(Object fieldValue, ValueType valueType) {
		Number number;
		switch (valueType) {
		case BOOLEAN:
			if (((Boolean) fieldValue).booleanValue()) {
				return Value.VALUE_TRUE;
			}
			return Value.VALUE_FALSE;
		case DATE:
			number = (Number) fieldValue;
			return Value.newDateValue(new Date(number.longValue()));
		case DECIMAL:
			number = (Number) fieldValue;
			return Value.newDecimalValue(number.doubleValue());
		case INTEGER:
			number = (Number) fieldValue;
			return Value.newIntegerValue(number.longValue());
		default:
			return Value.newTextValue(fieldValue.toString());
		}
	}
}
