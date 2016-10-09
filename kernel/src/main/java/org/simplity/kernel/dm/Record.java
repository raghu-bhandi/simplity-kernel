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
package org.simplity.kernel.dm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FilterCondition;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.Component;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataPurpose;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.data.SingleRowSheet;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.value.IntegerValue;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceProtocol;
import org.simplity.tp.InputRecord;
import org.simplity.tp.OutputRecord;

/**
 *
 * This is the main part of our data model. Every piece of data that the
 * application has to keep as part of "system of records" must be modeled into
 * this. Data structures that are used as input to, or output from a service are
 * modeled as records as well.
 *
 * It is common industry practice to have "physical data model" and
 * "logical data model" for proper understanding. We encourage such designs
 * before recording them using the Record concept.
 *
 * Record is a good candidate to represent any data structure that is used
 * across components, even if it is not persisted.
 *
 */
public class Record implements Component {
	/**
	 * header row of returned sheet when there is only one column
	 */
	private static String[] SINGLE_HEADER = { "value" };
	/**
	 * header row of returned sheet when there are two columns
	 */
	private static String[] DOUBLE_HEADER = { "id", "value" };
	private static final char COMMA = ',';
	private static final char PARAM = '?';
	private static final char EQUAL = '=';
	private static final char PERCENT = '%';
	private static final String TABLE_ACTION_FIELD_NAME = ServiceProtocol.TABLE_ACTION_FIELD_NAME;
	private static final Field TABLE_ACTION_FIELD = Field
			.getDefaultField(ServiceProtocol.TABLE_ACTION_FIELD_NAME);
	private static final ComponentType MY_TYPE = ComponentType.REC;

	/***
	 * Name of this record/entity, as used in application
	 */
	String name;

	/**
	 * module name + name would be unique for a component type within an
	 * application
	 */
	String moduleName;
	/**
	 * type of this record
	 */
	RecordUsageType recordType = RecordUsageType.STORAGE;
	/**
	 * name of the rdbms table, if this is either a storage table, or a view
	 * that is to be defined in the rdbms
	 */
	String tableName;

	/**
	 * has this table got an internal key, and do you want it to be managed
	 * automatically?
	 */
	boolean keyToBeGenerated;
	/**
	 * if this table is expected to have large number of rows, we would like to
	 * protect against a select with no where conditions. Of course one can
	 * always argue that this is no protection, as some one can easily put a
	 * condition like 1 = 1
	 */
	boolean okToSelectAll;

	/**
	 * child records that are to be read whenever a row from this record is
	 * read.
	 */
	String[] childrenToBeRead = null;
	/**
	 * child records to be saved along with this record. operations for this
	 * record
	 */
	String[] childrenToBeSaved = null;
	/**
	 * fields that this record is made-up of
	 */
	Field[] fields = new Field[0];

	/**
	 * In case this is a table that supplies key-value list for drop-downs, then
	 * we use primary key as internal key. Specify the field to be used as
	 * display value. key of this table is the internal value
	 */
	String valueListFieldName = null;

	/**
	 * relevant only if valueListFieldName is used. If the list of values need
	 * be further filtered with a key, like country code for list of state,
	 * specify the that field name.
	 */
	String valueListKeyName = null;

	/**
	 * what is the sheet name to be used as input/output sheet. (specifically
	 * used in creating services on the fly)
	 */
	String defaultSheetName = null;

	/**
	 * if keys are to be generated, we may have a sequence, as in oracle. Null
	 * implies that RDBMS auto-increments and we are not to set any value to
	 * this column
	 */
	String keyGeneratorName;

	/**
	 * if this record is used for a suggestion service, field that is used for
	 * search
	 */
	String suggestionKeyName;
	/**
	 * what fields do we respond back with for a suggestion service
	 */
	String[] suggestionOutputNames;
	/**
	 * is this record only for reading?
	 */
	boolean readOnly;

	/**
	 * in case this is a view, then the record from which fields are referred by
	 * default
	 */
	String defaultRefRecord;

	/**
	 * primary key is to be unique. Is there candidate key, other than the
	 * primary key, that may have more than one columns. We assume that the db
	 * has a constraint for these columns to be unique
	 */
	String[] logicalPrimaryKeyFields;

	/**
	 * if this application uses multiple schemas, and the underlying table of
	 * this record belongs to a schema other than the default, then specify it
	 * here, so that the on-the-fly services based on this record can use teh
	 * right schema.
	 */
	String schemaName;
	/*
	 * following fields are assigned for caching/performance
	 */
	/**
	 * Is there at least one field that has validations linked to another field.
	 * Initialized during init operation
	 */
	private boolean hasInterFieldValidations;

	/*
	 * standard fields are cached
	 */
	/**
	 * primary key of this record
	 */
	private Field primaryKeyField;
	/**
	 * field in this record that links to parent record
	 */
	private Field parentKeyField;
	private Field modifiedStampField;
	private Field modifiedUserField;
	private Field createdUserField;

	/**
	 * we need the set to validate field names at time
	 */
	private final Map<String, Field> indexedFields = new HashMap<String, Field>();

	/**
	 * and field names of course. cached after loading
	 */
	private String[] fieldNames;
	/**
	 * sql for reading a row for given primary key value
	 */
	private String readSql;

	/**
	 * select f1,f2,..... WHERE used in filtering
	 */
	private String filterSql;

	/**
	 * sql ready to insert a row into the table
	 */
	private String insertSql;

	/**
	 * sql to update every field. (Not selective update)
	 */
	private String updateSql;

	/**
	 * we skip few standard fields while updating s row. Keeping this count
	 * simplifies code
	 */
	private String deleteSql;

	/**
	 * we skip few standard fields while updating s row. Keeping this count
	 * simplifies code
	 */
	private String listSql;
	private ValueType[] valueListTypes;
	private ValueType valueListKeyType;
	private String suggestSql;

	/*
	 * methods for ComponentInterface
	 */

	@Override
	public String getSimpleName() {
		return this.name;
	}

	/**
	 * @param fieldName
	 * @return field or null
	 */
	public Field getField(String fieldName) {
		return this.indexedFields.get(fieldName);
	}

	/**
	 * to avoid confusion, we do not have a method called getName.
	 *
	 * @return qualified name of this record
	 */
	@Override
	public String getQualifiedName() {
		return this.moduleName + '.' + this.name;
	}

	/**
	 * @return name of the primary key, null if there is no primary key
	 */
	public String getPrimaryKeyName() {
		if (this.primaryKeyField == null) {
			return null;
		}
		return this.primaryKeyField.name;
	}

	/**
	 *
	 * @return dependent children to be input for saving
	 */
	public String[] getChildrenToInput() {
		return this.childrenToBeSaved;
	}

	/**
	 *
	 * @return dependent child records that are read along
	 */
	public String[] getChildrenToOutput() {
		return this.childrenToBeRead;
	}

	/**
	 * get the default name of data sheet to be used for input/output
	 *
	 * @return sheet name
	 */
	public String getDefaultSheetName() {
		return this.defaultSheetName;
	}

	/**
	 * get value types of fields in the same order that getFieldName() return
	 * field names
	 *
	 * @return value types of fields
	 */
	public ValueType[] getValueTypes() {
		ValueType[] types = new ValueType[this.fieldNames.length];
		int i = 0;
		for (Field field : this.fields) {
			types[i] = field.getValueType();
			i++;
		}
		return types;
	}

	/**
	 * get value types of fields in the same order that getFieldName() and an
	 * additional field for save action
	 *
	 * @return value types of fields and the last field a save action, which is
	 *         TEXT
	 */
	private Field[] getFieldsWithSave() {
		int n = this.fields.length + 1;
		Field[] allFields = new Field[n];
		n = 0;
		for (Field field : this.fields) {
			allFields[n++] = field;
		}
		allFields[n] = TABLE_ACTION_FIELD;
		return allFields;
	}

	/**
	 * create an empty data sheet based on this record
	 *
	 * @param forSingleRow
	 *            true if you intend to store only one row. False otherwise
	 * @param addActionColumn
	 *            should we add a column to have action to be performed on each
	 *            row
	 *
	 * @return an empty sheet ready to receive data
	 */
	public DataSheet createSheet(boolean forSingleRow, boolean addActionColumn) {
		Field[] sheetFeilds = this.fields;
		if (addActionColumn) {
			sheetFeilds = this.getFieldsWithSave();
		}
		if (forSingleRow) {
			return new SingleRowSheet(sheetFeilds);
		}
		return new MultiRowsSheet(sheetFeilds);
	}

	/**
	 * create an empty data sheet based on this record for a subset of fields
	 *
	 * @param colNames
	 *            subset of column names to be included
	 * @param forSingleRow
	 *            true if you intend to use this to have just one row
	 * @param addActionColumn
	 *            should we add a column to have action to be performed on each
	 *            row
	 * @return an empty sheet ready to receive rows of data
	 */
	public DataSheet createSheet(String[] colNames, boolean forSingleRow,
			boolean addActionColumn) {
		ValueType[] types = new ValueType[colNames.length];
		int i = 0;
		for (String colName : colNames) {
			Field field = this.indexedFields.get(colName);
			if (field == null) {
				throw new ApplicationError("Record " + this.getQualifiedName()
						+ " has no field named " + colName);
			}
			types[i] = field.getValueType();
			i++;
		}
		if (forSingleRow) {
			return new SingleRowSheet(colNames, types);
		}
		return new MultiRowsSheet(colNames, types);
	}

	/**
	 * read, in our vocabulary, is ALWAYS primary key based read. Hence we
	 * expect (at most) one row of output per read. If values has more than one
	 * rows, we read for primary key in each row.
	 *
	 * @param inSheet
	 *            one or more rows that has value for the primary key.
	 * @param outSheet
	 *            sheet to which rows are to be extracted
	 *
	 * @param driver
	 * @param userId
	 *            we may have a common security strategy to restrict output rows
	 *            based on user. Not used as of now
	 * @return data sheet for sure. With data that is returned by the driver
	 */
	public int readMany(DataSheet inSheet, DataSheet outSheet, DbDriver driver,
			Value userId) {
		if (this.primaryKeyField == null) {
			throw new ApplicationError(
					"Record "
							+ this.name
							+ " is not defined with a primary key, and hence we can not do a read operation on this.");
		}
		int nbrRows = inSheet.length();
		if (nbrRows == 0) {
			return 0;
		}

		boolean singleRow = nbrRows == 1;
		if (singleRow) {
			Value[] values = { inSheet.getColumnValue(
					this.primaryKeyField.getName(), 0) };
			return driver.extractFromSql(this.readSql, values, outSheet,
					singleRow);
		}
		Value[][] values = new Value[nbrRows][];
		for (int i = 0; i < nbrRows; i++) {
			Value value = inSheet.getColumnValue(
					this.primaryKeyField.getName(), i);
			Value[] vals = { value };
			values[i] = vals;
		}
		return driver.extractFromSql(this.readSql, values, outSheet);
	}

	/**
	 * read, in our vocabulary, is ALWAYS primary key based read. Hence we
	 * expect (at most) one row of output per read. If values has more than one
	 * rows, we read for primary key in each row.
	 *
	 * @param inData
	 *            one or more rows that has value for the primary key.
	 * @param outData
	 *            sheet to which data is to be extracted into
	 * @param driver
	 * @param userId
	 *            we may have a common security strategy to restrict output rows
	 *            based on user. Not used as of now
	 * @return number of rows extracted
	 */
	public int readOne(FieldsInterface inData, DataSheet outData,
			DbDriver driver, Value userId) {
		if (this.primaryKeyField == null) {
			Tracer.trace("Record "
					+ this.name
					+ " is not defined with a primary key, and hence we can not do a read operation on this.");
			return 0;
		}
		Value value = inData.getValue(this.primaryKeyField.getName());
		if (value == null || value.isUnknown()) {
			Tracer.trace("Value for primary key not prsent, and hence no read operation.");
			return 0;
		}
		Value[] values = { value };
		return driver.extractFromSql(this.readSql, values, outData, true);
	}

	/**
	 * checks if there is a row for this key. Row is not read.
	 *
	 * @param inData
	 * @param keyFieldName
	 * @param driver
	 * @param userId
	 * @return true if there is a row for this key, false otherwise. Row is not
	 *         read.
	 */
	public boolean rowExistsForKey(FieldsInterface inData, String keyFieldName,
			DbDriver driver, Value userId) {
		if (this.primaryKeyField == null) {
			Tracer.trace("Record "
					+ this.name
					+ " is not defined with a primary key, and hence we can not do a read operation on this.");
			this.noPrimaryKey();
			return false;
		}
		String keyName;
		if (keyFieldName == null) {
			keyName = this.primaryKeyField.name;
		} else {
			keyName = keyFieldName;
		}
		Value value = inData.getValue(keyName);
		if (Value.isNull(value)) {
			Tracer.trace("Primary key field " + keyName
					+ " has no value, and hence no read operation.");
			return false;
		}
		Value[] values = { value };
		return driver.hasResult(this.readSql, values);
	}

	/**
	 * filter rows from underlying view/table as per filtering criterion
	 *
	 * @param inputRecord
	 *            record that has fields for filter criterion
	 * @param userId
	 *            we may have a common security strategy to restrict output rows
	 *            based on user. Not used as of now
	 *
	 * @param inData
	 *            as per filtering conventions
	 * @param driver
	 * @return data sheet, possible with retrieved rows
	 */
	public DataSheet filter(Record inputRecord, FieldsInterface inData,
			DbDriver driver, Value userId) {
		DataSheet result = this.createSheet(false, false);
		/*
		 * we have to create where clause with ? and corresponding values[]
		 */
		StringBuilder sql = new StringBuilder(this.filterSql);
		Tracer.trace("Filter started with " + this.filterSql);
		List<Value> filterValues = new ArrayList<Value>();
		boolean firstTime = true;
		for (Field field : inputRecord.fields) {
			String fieldName = field.name;
			Value value = inData.getValue(fieldName);
			if (value == null || value.isUnknown()) {
				continue;
			}
			if (value.toString().length() == 0) {
				Tracer.trace("I found " + field.name
						+ " with an empty value in filtering.");
				continue;
			}
			FilterCondition condition = FilterCondition.Equal;
			Value otherValue = inData.getValue(fieldName
					+ ServiceProtocol.COMPARATOR_SUFFIX);
			if (otherValue != null && otherValue.isUnknown() == false) {
				condition = FilterCondition.parse(otherValue.toText());
			}
			if (condition == FilterCondition.Like) {
				value = Value.newTextValue(Record.PERCENT
						+ DbDriver.escapeForLike(value.toString())
						+ Record.PERCENT);
			} else if (condition == FilterCondition.StartsWith) {
				value = Value.newTextValue(DbDriver.escapeForLike(value
						.toString()) + Record.PERCENT);
			}
			if (firstTime) {
				firstTime = false;
			} else {
				sql.append(" AND ");
			}
			sql.append(field.columnName).append(condition.getSql()).append("?");
			filterValues.add(value);
			if (condition == FilterCondition.Between) {
				otherValue = inData.getValue(fieldName
						+ ServiceProtocol.TO_FIELD_SUFFIX);
				if (otherValue == null || otherValue.isUnknown()) {
					throw new ApplicationError(
							"To value not supplied for fied " + this.name
									+ " for filtering");
				}
				sql.append(" AND ?");
				filterValues.add(otherValue);
			}
		}
		Value[] values;
		if (firstTime) {
			/*
			 * no conditions..
			 */
			if (this.okToSelectAll == false) {
				throw new ApplicationError(
						"Record "
								+ this.name
								+ " is likely to contain large number of records, and hence we do not allow select-all operation");
			}
			sql.append(" 1 = 1 ");
			values = new Value[0];
		} else {
			values = filterValues.toArray(new Value[0]);
		}
		/*
		 * is there sort order?
		 */
		Value sorts = inData.getValue(ServiceProtocol.SORT_COLUMN_NAME);
		if (sorts != null) {
			sql.append(" ORDER BY ").append(sorts.toString());
		}
		driver.extractFromSql(sql.toString(), values, result, false);
		return result;
	}

	/**
	 * add, modify and delete are the three operations we can do for a record.
	 * "save" is a special convenient command. If key is specified, it is
	 * assumed to be modify, else add. Save
	 *
	 * @param row
	 *            data to be saved.
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 *            if true, we assume that some constraints are set at db level,
	 *            and sql error is treated as if affected rows is zero
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public SaveActionType saveOne(FieldsInterface row, DbDriver driver,
			Value userId, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}

		if (this.primaryKeyField == null) {
			this.noPrimaryKey();
		}
		Value[] values = new Value[this.fields.length];
		/*
		 * modified user field, even if sent by client, must be over-ridden
		 */
		if (this.modifiedUserField != null) {
			row.setValue(this.modifiedUserField.name, userId);
		}
		/*
		 * is the action explicitly specified
		 */
		SaveActionType saveAction = SaveActionType.SAVE;
		Value action = row.getValue(ServiceProtocol.TABLE_ACTION_FIELD_NAME);
		if (action != null) {
			/*
			 * since this field is extracted by us earlier, we DO KNOW that it
			 * is valid
			 */
			saveAction = SaveActionType.parse(action.toString());
		}
		if (saveAction == SaveActionType.SAVE) {
			/*
			 * is the key supplied?
			 */
			Value keyValue = row.getValue(this.primaryKeyField.name);
			if (this.keyToBeGenerated) {
				if (Value.isNull(keyValue)) {
					saveAction = SaveActionType.ADD;
				} else {
					saveAction = SaveActionType.MODIFY;
				}
			} else {
				if (this.rowExistsForKey(row, null, driver, userId)) {
					saveAction = SaveActionType.MODIFY;
				} else {
					saveAction = SaveActionType.ADD;
				}
			}
		}
		if (saveAction == SaveActionType.ADD) {
			if (this.createdUserField != null) {
				row.setValue(this.createdUserField.name, userId);
			}
			values = this.getInsertValues(row, userId);
			long[] generatedKeys = this.keyToBeGenerated ? new long[1] : null;
			driver.executeSql(this.insertSql, values, generatedKeys,
					treatSqlErrorAsNoResult);
			if (generatedKeys != null) {
				row.setValue(this.primaryKeyField.name,
						Value.newIntegerValue(generatedKeys[0]));
			}
		} else if (saveAction == SaveActionType.DELETE) {
			values = this.getDeleteValues(row);
			driver.executeSql(this.deleteSql, values, null,
					treatSqlErrorAsNoResult);
		} else {
			values = this.getUpdateValues(row, userId);
			if (driver.executeSql(this.updateSql, values, null,
					treatSqlErrorAsNoResult) == 0) {
				throw new ApplicationError(
						"Data was changed by some one else while you were editing it. Please cancel this oepration and redo it with latest data.");
			}
		}
		return saveAction;
	}

	/**
	 * add, modify and delete are the three operations we can do for a record.
	 * "save" is a special convenient command. If key is specified, it is
	 * assumed to be modify, else add. Save
	 *
	 * @param inSheet
	 *            data to be saved.
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public SaveActionType[] saveMany(DataSheet inSheet, DbDriver driver,
			Value userId, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		SaveActionType[] result = new SaveActionType[inSheet.length()];
		int rowIdx = 0;
		for (FieldsInterface row : inSheet) {
			result[rowIdx++] = this.saveOne(row, driver, userId,
					treatSqlErrorAsNoResult);
			rowIdx++;
		}
		return result;
	}

	/**
	 * parent record got saved. we are to save rows for this record
	 *
	 * @param inSheet
	 *            data for this record
	 * @param parentRow
	 *            data for parent record that is already saved
	 * @param actions
	 *            that are already done using parent sheet
	 * @param driver
	 * @param userId
	 */
	public void saveWithParent(DataSheet inSheet, FieldsInterface parentRow,
			SaveActionType[] actions, DbDriver driver, Value userId) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.parentKeyField == null) {
			this.noParent();
		}
		Value parentKey = parentRow.getValue(this.parentKeyField.referredField);
		if (parentKey == null) {
			Tracer.trace("Parent key value is null, and hence no save with parent operation");
		}
		/*
		 * for security/safety, we copy parent key into data
		 */
		inSheet.addColumn(this.parentKeyField.name, parentKey);
		for (FieldsInterface row : inSheet) {
			this.saveOne(row, driver, userId, false);
		}
	}

	/**
	 * @param inSheet
	 * @param userId
	 * @param rowIdx
	 * @return
	 */
	private Value[] getInsertValues(FieldsInterface row, Value userId) {
		/*
		 * we may not fill all fields, but let us handle that exception later
		 */
		int nbrFields = this.fields.length;
		Value[] values = new Value[nbrFields];
		int valueIdx = 0;
		for (Field field : this.fields) {
			if (field.doNotInsert()) {
				continue;
			}
			if (field.fieldType == FieldType.CREATED_BY_USER
					|| field.fieldType == FieldType.MODIFIED_BY_USER) {
				values[valueIdx] = userId;
			} else {
				Value value = row.getValue(field.name);
				if (value == null) {
					if (field.isNullable) {
						value = Value.newUnknownValue(field.getValueType());
					} else {
						throw new ApplicationError(
								"Column "
										+ field.columnName
										+ " in table "
										+ this.tableName
										+ " is designed to be non-null, but a row is being inserted with a null value in it.");
					}
				}
				values[valueIdx] = value;
			}
			valueIdx++;
		}
		/*
		 * did we skip some values?
		 */
		if (nbrFields != valueIdx) {
			values = this.chopValues(values, valueIdx);
		}
		return values;
	}

	/**
	 * @param row
	 * @param userId
	 * @param rowIdx
	 * @return
	 */
	private Value[] getUpdateValues(FieldsInterface row, Value userId) {
		int nbrFields = this.fields.length;
		Value[] values = new Value[nbrFields];
		int valueIdx = 0;
		for (Field field : this.fields) {
			if (field.doNotUpdate()) {
				continue;
			}
			if (field.fieldType == FieldType.MODIFIED_BY_USER) {
				values[valueIdx] = userId;
			} else {
				Value value = row.getValue(field.name);
				if (value == null) {
					if (field.isNullable) {
						value = Value.newUnknownValue(field.getValueType());
					} else {
						throw new ApplicationError(
								"Column "
										+ field.columnName
										+ " in table "
										+ this.tableName
										+ " is designed to be non-null, but a row is being updated witha null value in it.");
					}
				}
				values[valueIdx] = value;
			}
			valueIdx++;
		}
		values[valueIdx++] = row.getValue(this.primaryKeyField.name);
		// if (this.modifiedStampField != null) {
		// values[valueIdx++] = row.getValue(this.modifiedStampField.name);
		// }
		/*
		 * did we skip some values?
		 */
		if (nbrFields != valueIdx) {
			values = this.chopValues(values, valueIdx);
		}
		return values;
	}

	/**
	 * create a values row for a delete sql
	 *
	 * @param inSheet
	 * @param rowIdx
	 * @return
	 */
	private Value[] getDeleteValues(FieldsInterface row) {
		Value keyValue = row.getValue(this.primaryKeyField.name);

		if (this.modifiedStampField == null) {
			Value[] values = { keyValue };
			return values;
		}
		/**
		 * todo: time stamp to be introduced after some re-design
		 */
		// Value stampValue = row.getValue(this.modifiedStampField.name);
		// Value[] values = { keyValue, stampValue };
		Value[] values = { keyValue };
		return values;
	}

	/**
	 * insert row/s
	 *
	 * @param inSheet
	 *
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int insert(DataSheet inSheet, DbDriver driver, Value userId,
			boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		int nbrRows = inSheet.length();
		if (nbrRows == 1) {
			return this.insert((FieldsInterface) inSheet, driver, userId,
					treatSqlErrorAsNoResult);
		}
		Value[][] allValues = new Value[nbrRows][];
		/*
		 * we mostly expect one row, but we do not want to write separate
		 * code...
		 */
		int rowIdx = 0;
		for (FieldsInterface row : inSheet) {
			allValues[rowIdx] = this.getInsertValues(row, userId);
			rowIdx++;
		}
		if (this.keyToBeGenerated == false) {
			return this.executeWorker(driver, this.insertSql, allValues, null,
					treatSqlErrorAsNoResult);
		}
		long[] generatedKeys = new long[nbrRows];
		int result = this.executeWorker(driver, this.insertSql, allValues,
				generatedKeys, treatSqlErrorAsNoResult);
		if (result > 0) {
			this.addKeyColumn(inSheet, generatedKeys);
		}
		return result;
	}

	/**
	 * insert row/s
	 *
	 * @param inData
	 *
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int insert(FieldsInterface inData, DbDriver driver, Value userId,
			boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		Value[][] allValues = new Value[1][];
		allValues[0] = this.getInsertValues(inData, userId);
		if (this.keyToBeGenerated == false) {
			return this.executeWorker(driver, this.insertSql, allValues, null,
					treatSqlErrorAsNoResult);
		}
		long[] generatedKeys = new long[1];
		int result = this.executeWorker(driver, this.insertSql, allValues,
				generatedKeys, treatSqlErrorAsNoResult);
		if (result > 0) {
			inData.setValue(this.primaryKeyField.name,
					Value.newIntegerValue(generatedKeys[0]));
		}
		return result;
	}

	/**
	 * insert row/s
	 *
	 * @param inSheet
	 *            to be inserted along with this parent
	 * @param parentRow
	 *            fields/row that has the parent key
	 *
	 * @param driver
	 * @param userId
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int insertWithParent(DataSheet inSheet, FieldsInterface parentRow,
			DbDriver driver, Value userId) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.parentKeyField == null) {
			this.noParent();
		}
		Value parentKey = parentRow.getValue(this.parentKeyField.referredField);
		if (parentKey == null) {
			Tracer.trace("Parent key value is null, and hence no insert with parent operation");
		}
		/*
		 * for security/safety, we copy parent key into data
		 */
		inSheet.addColumn(this.parentKeyField.name, parentKey);
		int nbrRows = inSheet.length();
		Value[][] allValues = new Value[nbrRows][];
		int rowIdx = 0;
		for (FieldsInterface row : inSheet) {
			allValues[rowIdx] = this.getInsertValues(row, userId);
			rowIdx++;
		}
		if (this.keyToBeGenerated == false) {
			return this.executeWorker(driver, this.insertSql, allValues, null,
					false);
		}
		long[] keys = new long[nbrRows];
		int result = this.executeWorker(driver, this.insertSql, allValues,
				keys, false);
		this.addKeyColumn(inSheet, keys);
		return result;
	}

	/**
	 * add a column to the data sheet and copy primary key values into that
	 *
	 * @param inSheet
	 * @param keys
	 */
	private void addKeyColumn(DataSheet inSheet, long[] keys) {
		int nbrKeys = keys.length;
		Value[] values = new Value[nbrKeys];
		int i = 0;
		for (long key : keys) {
			values[i++] = Value.newIntegerValue(key);
		}
		inSheet.addColumn(this.primaryKeyField.name, ValueType.INTEGER, values);
	}

	/**
	 * copy parent key to child sheet
	 *
	 * @param inData
	 *            row/fields that has the parent key
	 * @param sheet
	 *            to which we have to copy the key values
	 */
	public void copyParentKey(FieldsInterface inData, DataSheet sheet) {
		String fieldName = this.parentKeyField.name;
		String parentKeyName = this.parentKeyField.referredField;
		Value parentKey = inData.getValue(parentKeyName);
		if (parentKey == null) {
			Tracer.trace("No value found for parent key field "
					+ this.parentKeyField.referredField
					+ " and hence no column is going to be added to child table");
			return;
		}
		sheet.addColumn(fieldName, parentKey);
	}

	/**
	 * update all fields, except of course the ones that are not to be.
	 *
	 * @param inSheet
	 *            data to be saved.
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int update(DataSheet inSheet, DbDriver driver, Value userId,
			boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.primaryKeyField == null) {
			this.noPrimaryKey();
		}
		int nbrRows = inSheet.length();
		/*
		 * we mostly expect one row, but we do not want to write separate
		 * code...
		 */
		Value[][] allValues = new Value[nbrRows][];
		if (nbrRows == 1) {
			allValues[0] = this.getUpdateValues(inSheet, userId);
		} else {
			int i = 0;
			for (FieldsInterface row : inSheet) {
				allValues[i++] = this.getUpdateValues(row, userId);
			}
		}
		return this.executeWorker(driver, this.updateSql, allValues, null,
				treatSqlErrorAsNoResult);
	}

	/**
	 * update all fields, except of course the ones that are not to be.
	 *
	 * @param inputData
	 *            data to be saved.
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int update(FieldsInterface inputData, DbDriver driver, Value userId,
			boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.primaryKeyField == null) {
			this.noPrimaryKey();
		}
		Value[][] allValues = new Value[1][];
		allValues[0] = this.getUpdateValues(inputData, userId);
		return this.executeWorker(driver, this.updateSql, allValues, null,
				treatSqlErrorAsNoResult);
	}

	/**
	 * update all fields, except of course the ones that are not to be.
	 *
	 * @param inSheet
	 *            data to be saved.
	 * @param driver
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int delete(DataSheet inSheet, DbDriver driver,
			boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.primaryKeyField == null) {
			this.noPrimaryKey();
		}
		int nbrRows = inSheet.length();
		if (nbrRows == 1) {
			return this.delete(inSheet, driver, treatSqlErrorAsNoResult);
		}
		/*
		 * we mostly expect one row, but we do not want to write separate
		 * code...
		 */
		Value[][] allValues = new Value[nbrRows][];
		nbrRows = 0;
		for (FieldsInterface row : inSheet) {
			allValues[nbrRows++] = this.getDeleteValues(row);
		}
		return this.executeWorker(driver, this.deleteSql, allValues, null,
				treatSqlErrorAsNoResult);
	}

	/**
	 * update all fields, except of course the ones that are not to be.
	 *
	 * @param inData
	 *            data to be saved.
	 * @param driver
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int delete(FieldsInterface inData, DbDriver driver,
			boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.primaryKeyField == null) {
			this.noPrimaryKey();
		}
		Value[][] allValues = new Value[1][];
		allValues[0] = this.getDeleteValues(inData);
		return this.executeWorker(driver, this.deleteSql, allValues, null,
				treatSqlErrorAsNoResult);
	}

	private void notWritable() {
		throw new ApplicationError(
				"Record "
						+ this.name
						+ " is not designed to be writable. Add/Update/Delete operations are not possible.");
	}

	private void noParent() {
		throw new ApplicationError(
				"Record "
						+ this.name
						+ " does not have a parent key field. Operation with parent is not possible.");
	}

	private void noPrimaryKey() {
		throw new ApplicationError(
				"Update/Delete operations are not possible for Record "
						+ this.name + " as it does not define a primary key.");
	}

	/**
	 * update all fields, except of course the ones that are not to be.
	 *
	 * @param parentRow
	 *            from where we pick up parent key.
	 * @param driver
	 * @param userId
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int deleteWithParent(FieldsInterface parentRow, DbDriver driver,
			Value userId) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.parentKeyField == null) {
			this.noParent();
		}
		String parentName = this.parentKeyField.referredField;
		Value value = parentRow.getValue(parentName);
		if (value == null) {
			Tracer.trace("Delete with parent has nothing to delete as parent key is null");
			return 0;
		}
		Value[] values = { value };
		String sql = "DELETE FROM " + this.tableName + " WHERE "
				+ this.parentKeyField.columnName + "=?";
		return driver.executeSql(sql, values, null, false);
	}

	private int executeWorker(DbDriver driver, String sql, Value[][] values,
			long[] generatedKeys, boolean treatSqlErrorAsNoResult) {
		if (values.length == 1) {
			return driver.executeSql(sql, values[0], generatedKeys,
					treatSqlErrorAsNoResult);
		}
		int[] counts = driver.executeBatch(sql, values, generatedKeys,
				treatSqlErrorAsNoResult);
		int result = 0;
		for (int n : counts) {
			if (n < 0) {
				return -1;
			}
			result += n;
		}
		return result;
	}

	/**
	 * update subset of fields.
	 *
	 * @param inData
	 *            data to be saved.
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int selectiveUpdate(FieldsInterface inData, DbDriver driver,
			Value userId, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		if (this.primaryKeyField == null) {
			this.noPrimaryKey();
		}
		/*
		 * we have primary key in where clause.
		 */
		int nbrFields = this.fields.length;
		Value[] values = new Value[nbrFields];
		int valueIdx = 0;
		StringBuilder update = new StringBuilder("UPDATE ");
		update.append(this.tableName).append(" SET ");
		for (Field field : this.fields) {
			Value value = null;
			if (field.doNotUpdate()) {
				continue;
			}
			if (field.fieldType == FieldType.MODIFIED_BY_USER) {
				value = userId;
			} else {
				value = inData.getValue(field.name);
			}
			if (value != null) {
				if (valueIdx != 0) {
					update.append(Record.COMMA);
				}
				update.append(field.columnName).append(Record.EQUAL)
						.append(Record.PARAM);
				values[valueIdx++] = value;
			}
		}
		update.append(" WHERE ").append(this.primaryKeyField.columnName)
				.append(Record.EQUAL).append(Record.PARAM);
		values[valueIdx++] = inData.getValue(this.primaryKeyField.name);

		if (this.modifiedStampField != null) {
			update.append(" AND ").append(this.modifiedStampField.columnName)
					.append(Record.EQUAL).append(Record.PARAM);
			values[valueIdx++] = inData.getValue(this.modifiedStampField.name);
		}
		/*
		 * did we skip some values?
		 */
		if (nbrFields != valueIdx) {
			values = this.chopValues(values, valueIdx);
		}
		return driver.executeSql(this.updateSql, values, null,
				treatSqlErrorAsNoResult);
	}

	private Value[] chopValues(Value[] values, int nbrsToRetain) {
		Value[] newValues = new Value[nbrsToRetain];
		for (int i = 0; i < nbrsToRetain; i++) {
			newValues[i] = values[i];
		}
		return newValues;
	}

	/**
	 * update subset of fields.
	 *
	 * @param inSheet
	 *            data to be saved.
	 * @param driver
	 * @param userId
	 * @param treatSqlErrorAsNoResult
	 * @return number of rows saved. -1 in case of batch, and the driver is
	 *         unable to count the saved rows
	 */
	public int selectiveUpdate(DataSheet inSheet, DbDriver driver,
			Value userId, boolean treatSqlErrorAsNoResult) {
		if (this.readOnly) {
			this.notWritable();
		}
		int nbrRows = inSheet.length();
		if (nbrRows == 1) {
			return this.selectiveUpdate(inSheet, driver, userId,
					treatSqlErrorAsNoResult);
		}
		nbrRows = 0;
		for (FieldsInterface row : inSheet) {
			nbrRows += this.selectiveUpdate(row, driver, userId,
					treatSqlErrorAsNoResult);
		}
		return nbrRows;
	}

	/**
	 * read rows from this record for a given parent record
	 *
	 * @param parentData
	 *            rows for parent
	 * @param driver
	 * @return sheet that contains rows from this record for the parent rows
	 */
	public DataSheet filterForParent(DataSheet parentData, DbDriver driver) {
		DataSheet result = this.createSheet(false, false);
		int n = parentData.length();
		if (n == 0) {
			return result;
		}
		String keyName = this.parentKeyField.referredField;
		StringBuilder sbf = new StringBuilder(this.filterSql);
		sbf.append(this.parentKeyField.columnName);
		Value[] values = parentData.getColumnValues(keyName);
		Tracer.trace("There are " + n + " rows in parent sheet. column "
				+ keyName + " has " + values.length
				+ " values with first value=" + values[0]
				+ ". We are going to read child rows for them using record "
				+ this.name);
		/*
		 * for single key we use where key = ?
		 * 
		 * for multiple, we use where key in (?,?,....)
		 */
		if (n == 1) {
			sbf.append("=?");
		} else {
			sbf.append(" IN (?");
			for (int i = 1; i < n; i++) {
				sbf.append(",?");
			}
			sbf.append(')');
		}
		driver.extractFromSql(sbf.toString(), values, result, false);
		return result;
	}

	/**
	 * read rows from this record for a given parent record
	 *
	 * @param parentKey
	 *
	 * @param driver
	 * @return sheet that contains rows from this record for the parent rows
	 */
	public DataSheet filterForParent(Value parentKey, DbDriver driver) {
		DataSheet result = this.createSheet(false, false);
		Value[] values = { parentKey };
		StringBuilder sbf = new StringBuilder(this.filterSql);
		sbf.append(this.parentKeyField.columnName).append("=?");
		driver.extractFromSql(sbf.toString(), values, result, false);
		return result;
	}

	/*
	 * code after this point relates to getReady(). we have violated our norm,
	 * and defined a field also at this point, for grouping them into getready()
	 * block
	 */

	/*
	 * we have a possible issue with referred records. If A refers to B and B
	 * refers to A, we have an error on hand. How do we track this? as of now,
	 * we will track this during getReady() invocation. getReady() will ask for
	 * a referred record. That record will execute getReady() before returning.
	 * It may ask for another record, so an and so forth.
	 * 
	 * There are two ways to solve this problem.
	 * 
	 * One way is to differentiate between normal-request and reference-request
	 * for a record. Pass history during reference request so that we can detect
	 * circular reference. Issue with this is that getRequest() is a generic
	 * method and hence we can not customize it.
	 * 
	 * Other approach is to use thread-local that is initiated by getReady().
	 * 
	 * our algorithm is :
	 * 
	 * 1. we initiate refHistory before getReady() and current record to
	 * pendingOnes.
	 * 
	 * 2. A referred field may invoke parent.getRefrecord() Referred record is
	 * requested from ComponentManager.getRecord();
	 * 
	 * 3. that call will trigger getReady() on the referred record. This chain
	 * will go-on..
	 * 
	 * 4. before adding to pending list we check if it already exists. That
	 * would be a circular reference.
	 * 
	 * 5. Once we complete getReady(), we remove this record from pendingOnes.
	 * And if there no more pending ones, we remove it. and that completes the
	 * check.
	 */
	/**
	 * tracks recursive reference calls between records and referred records for
	 * referred fields
	 */
	static ThreadLocal<RefHistory> referenceHistory = new ThreadLocal<Record.RefHistory>();

	class RefHistory {
		/**
		 * recursive reference history of record names
		 */
		List<String> pendingOnes = new ArrayList<String>();
		/**
		 * records that have completed loading as part of this process
		 */
		Map<String, Record> finishedOnes = new HashMap<String, Record>();
	}

	/**
	 * called from field when it refers to a field in another record
	 *
	 * @param recordName
	 * @return
	 */
	Record getRefRecord(String recordName) {
		RefHistory history = referenceHistory.get();
		if (history == null) {
			throw new ApplicationError(
					"Record.java has an issue with getReady() logic. history is null");
		}
		/*
		 * do we have it in our cache?
		 */
		Record record = history.finishedOnes.get(recordName);
		if (record != null) {
			return record;
		}
		/*
		 * is this record already in the pending list?
		 */
		if (history.pendingOnes.contains(recordName)) {
			/*
			 * we have a circular reference issue.
			 */
			StringBuilder sbf = new StringBuilder();
			if (recordName.equals(this.getQualifiedName())) {
				sbf.append("Record ")
						.append(recordName)
						.append(" has at least one field that refers to another field in this record itself. Sorry, you can't do that.");
			} else {
				sbf.append("There is a circular reference of records amongst the following records. Please review and fix.\n{\n");
				int nbr = history.pendingOnes.size();
				for (int i = 0; i < nbr; i++) {

					sbf.append(i).append(": ")
							.append(history.pendingOnes.get(i)).append('\n');
				}
				sbf.append(nbr).append(": ").append(recordName).append('\n');
				sbf.append('}');
			}
			throw new ApplicationError(sbf.toString());
		}
		return ComponentManager.getRecord(recordName);
	}

	/**
	 * called before starting getReady()
	 *
	 * @return true if we initiated the trail..
	 */
	private boolean recordGettingReady() {
		String recName = this.getQualifiedName();
		RefHistory history = referenceHistory.get();
		if (history == null) {
			history = new RefHistory();
			history.pendingOnes.add(recName);
			referenceHistory.set(history);
			return true;
		}
		if (history.pendingOnes.contains(recName) == false) {
			history.pendingOnes.add(recName);
			return false;
		}
		/*
		 * we have a circular reference issue.
		 */

		StringBuilder sbf = new StringBuilder();
		if (history.pendingOnes.size() == 1) {
			sbf.append("Record ")
					.append(recName)
					.append(" has at least one field that refers to another field in this record itself. Sorry, you can't do that.");
		} else {
			sbf.append("There is a circular reference of records amongst the following records. Please review and fix.\n{\n");
			int nbr = history.pendingOnes.size();
			for (int i = 0; i < nbr; i++) {

				sbf.append(i).append(". ").append(history.pendingOnes.get(i))
						.append('\n');
			}
			sbf.append(nbr).append(". ").append(recName).append('\n');
			sbf.append('}');
		}
		Tracer.trace(sbf.toString());
		return false;
		// throw new ApplicationError(sbf.toString());
	}

	/**
	 * called at the end of getReady();
	 */
	private void recordGotReady(boolean originator) {
		String recName = this.getQualifiedName();
		RefHistory history = referenceHistory.get();
		if (history == null) {
			Tracer.trace("There is an issue with the way Record "
					+ recName
					+ "  is trying to detect circular reference. History has disappeared.");
			return;
		}
		if (originator == false) {
			history.pendingOnes.remove(recName);
			history.finishedOnes.put(recName, this);
			return;
		}
		if (history.pendingOnes.size() > 1) {
			StringBuilder sbf = new StringBuilder();
			for (String s : history.pendingOnes) {
				sbf.append(s).append(' ');
			}
			Tracer.trace("There is an issue with the way Record "
					+ recName
					+ "  is trying to detect circular reference. pending list remained as "
					+ sbf.toString());
		}
		referenceHistory.remove();
	}

	@Override
	public void getReady() {
		if (this.tableName == null) {
			this.tableName = this.name;
		}
		if (this.defaultSheetName == null) {
			this.defaultSheetName = this.name;
		}
		if (this.recordType != RecordUsageType.STORAGE) {
			this.readOnly = true;
		}
		/*
		 * we track referred records. push to stack
		 */
		boolean originator = this.recordGettingReady();
		Record refRecord = null;
		if (this.defaultRefRecord != null) {
			refRecord = this.getRefRecord(this.defaultRefRecord);
		}
		this.fieldNames = new String[this.fields.length];
		for (int i = 0; i < this.fields.length; i++) {
			Field field = this.fields[i];
			field.getReady(this, refRecord,
					this.recordType == RecordUsageType.VIEW);
			String fName = field.name;
			this.fieldNames[i] = fName;
			this.indexedFields.put(fName, field);
			if (!this.hasInterFieldValidations
					&& field.hasInterFieldValidations()) {
				this.hasInterFieldValidations = true;
			}
			FieldType ft = field.getFieldType();
			if (ft == FieldType.PRIMARY_KEY) {
				this.checkDuplicateError(this.primaryKeyField);
				this.primaryKeyField = field;
			} else if (ft == FieldType.PARENT_KEY) {
				this.checkDuplicateError(this.parentKeyField);
				this.parentKeyField = field;
			} else if (ft == FieldType.MODIFIED_TIME_STAMP) {
				this.checkDuplicateError(this.modifiedStampField);
				this.modifiedStampField = field;
			} else if (ft == FieldType.CREATED_BY_USER) {
				this.checkDuplicateError(this.createdUserField);
				this.createdUserField = field;
			}

		}

		if (this.defaultSheetName == null) {
			this.defaultSheetName = this.name;
		}
		this.createReadSqls();
		if (this.readOnly == false) {
			this.createWriteSqls();
		}
		if (this.valueListFieldName != null) {
			this.setListSql();
		}
		if (this.suggestionKeyName != null) {
			this.setSuggestSql();
		}
		/*
		 * we have successfully loaded. remove this record from stack.
		 */
		this.recordGotReady(originator);
	}

	private void checkDuplicateError(Field savedField) {
		if (savedField == null) {
			return;
		}

		throw new ApplicationError("Record " + this.getQualifiedName()
				+ " defines more than one field with field type "
				+ savedField.fieldType.name()
				+ ". This feature is not supported");

	}

	/**
	 * set sql strings. We are setting four fields at the end. For clarity, you
	 * should trace one string at a time and understand what we are trying to
	 * do. Otherwise it looks confusing
	 */
	private void createWriteSqls() {
		String timeStamp = DbDriver.getTimeStamp();
		/*
		 * we have two buffers for insert as fields are to be inserted at two
		 * parts
		 */
		StringBuilder insert = new StringBuilder("INSERT INTO ");
		insert.append(this.tableName).append('(');
		StringBuilder vals = new StringBuilder(") Values(");

		StringBuilder update = new StringBuilder("UPDATE ");
		update.append(this.tableName).append(" SET ");
		// Field stampField = null;
		boolean firstInsertField = true;
		boolean firstUpdatableField = true;
		for (Field field : this.fields) {
			/*
			 * some fields are not updatable
			 */
			if (field.doNotUpdate() == false
					|| field.fieldType == FieldType.MODIFIED_TIME_STAMP) {
				if (firstUpdatableField) {
					firstUpdatableField = false;
				} else {
					update.append(COMMA);
				}
				update.append(field.columnName).append(Record.EQUAL);
				if (field.fieldType == FieldType.MODIFIED_TIME_STAMP) {
					// stampField = field;
					update.append(timeStamp);
				} else {
					update.append(Record.PARAM);
				}
			}
			FieldType fieldType = field.fieldType;
			/*
			 * if primary key is managed by rdms, we do not bother about it?
			 */
			if (fieldType == FieldType.PRIMARY_KEY
					&& this.keyToBeGenerated == true) {
				continue;
			}
			if (firstInsertField) {
				firstInsertField = false;
			} else {
				insert.append(Record.COMMA);
				vals.append(Record.COMMA);
			}
			insert.append(field.columnName);
			/*
			 * value is hard coded for time stamps
			 */
			if (field.fieldType == FieldType.MODIFIED_TIME_STAMP
					|| field.fieldType == FieldType.CREATED_TIME_STAMP) {
				vals.append(timeStamp);
			} else if (fieldType == FieldType.PRIMARY_KEY
					&& this.keyToBeGenerated) {
				vals.append(this.keyGeneratorName);
			} else {
				vals.append(Record.PARAM);
			}
		}
		/*
		 * set insert sql
		 */
		insert.append(vals.append(')'));
		this.insertSql = insert.toString();

		/*
		 * where clause of delete and update are same, but they are valid only
		 * if we have a primary key
		 */
		if (this.primaryKeyField != null) {
			StringBuilder where = new StringBuilder(" WHERE ");
			where.append(this.primaryKeyField.columnName).append(Record.EQUAL)
					.append(Record.PARAM);

			// if (stampField != null) {
			// where.append(" AND ").append(stampField.columnName).append("=?");
			// }
			this.deleteSql = "DELETE FROM " + this.tableName + where;
			this.updateSql = update.append(where).toString();
		}

	}

	/**
	 * Create read and filter sqls
	 */
	private void createReadSqls() {
		StringBuilder select = new StringBuilder("SELECT ");

		boolean isFirstField = true;
		for (Field field : this.fields) {
			if (isFirstField) {
				isFirstField = false;
			} else {
				select.append(Record.COMMA);
			}
			select.append(field.columnName).append(" \"").append(field.name)
					.append('"');
		}
		select.append(" FROM ").append(this.tableName).append(" WHERE ");
		this.filterSql = select.toString();
		if (this.primaryKeyField != null) {
			this.readSql = this.filterSql + this.primaryKeyField.columnName
					+ EQUAL + PARAM;
		}
	}

	private void setListSql() {
		Field field = this.getField(this.valueListFieldName);
		if (field == null) {
			this.invalidFieldName(this.valueListFieldName);
			return;
		}
		StringBuilder sbf = new StringBuilder();
		sbf.append("SELECT ");
		if (this.primaryKeyField != null && field != this.primaryKeyField) {
			sbf.append(this.primaryKeyField.columnName).append(" id,");
			this.valueListTypes = new ValueType[2];
			this.valueListTypes[0] = this.primaryKeyField.getValueType();
			this.valueListTypes[1] = field.getValueType();
		} else {
			this.valueListTypes = new ValueType[1];
			this.valueListTypes[0] = field.getValueType();
		}
		sbf.append(field.columnName).append(" value from ")
				.append(this.tableName);
		if (this.valueListKeyName != null) {
			field = this.getField(this.valueListKeyName);
			if (field == null) {
				this.invalidFieldName(this.valueListKeyName);
				return;
			}
			sbf.append(" WHERE ").append(field.columnName).append("=?");
			this.valueListKeyType = field.getValueType();
		}
		this.listSql = sbf.toString();
	}

	private void setSuggestSql() {
		Field field = this.getField(this.suggestionKeyName);
		if (field == null) {
			this.invalidFieldName(this.suggestionKeyName);
			return;
		}
		if (this.suggestionOutputNames == null
				|| this.suggestionOutputNames.length == 0) {
			throw new ApplicationError(
					"Record "
							+ this.getQualifiedName()
							+ " specifies suggestion key but no suggestion output fields");
		}
		StringBuilder sbf = new StringBuilder();
		sbf.append("SELECT ");
		for (String fieldName : this.suggestionOutputNames) {
			Field f = this.getField(fieldName);
			if (f == null) {
				this.invalidFieldName(this.suggestionKeyName);
				return;
			}
			sbf.append(f.columnName).append(' ').append(f.name).append(COMMA);
		}
		sbf.setLength(sbf.length() - 1);
		sbf.append(" from ").append(this.tableName).append(" WHERE ")
				.append(field.columnName).append(" LIKE ?");
		this.suggestSql = sbf.toString();
	}

	/**
	 * get list of values, typically for drop-down control
	 *
	 * @param keyValue
	 * @param driver
	 * @param userId
	 * @return sheet that has the data
	 */
	public DataSheet list(String keyValue, DbDriver driver, Value userId) {
		Value[] values = null;
		if (this.valueListKeyName != null) {
			if (keyValue == null || keyValue.length() == 0) {
				return null;
			}
			values = new Value[1];
			values[0] = Value.parseValue(keyValue, this.valueListKeyType);
		}
		DataSheet sheet = null;
		if (this.valueListTypes.length == 1) {
			sheet = new MultiRowsSheet(SINGLE_HEADER, this.valueListTypes);
		} else {
			sheet = new MultiRowsSheet(DOUBLE_HEADER, this.valueListTypes);
		}
		driver.extractFromSql(this.listSql, values, sheet, false);
		return sheet;
	}

	/**
	 * extract rows matching/starting with supplied chars. Typically for a
	 * suggestion list
	 *
	 * @param keyValue
	 * @param matchStarting
	 * @param driver
	 * @param userId
	 * @return sheet that has the data
	 */
	public DataSheet suggest(String keyValue, boolean matchStarting,
			DbDriver driver, Value userId) {
		String text = keyValue + DbDriver.LIKE_ANY;
		if (!matchStarting) {
			text = DbDriver.LIKE_ANY + text;
		}
		Value[] values = new Value[1];
		values[0] = Value.newTextValue(text);
		DataSheet sheet = this.createSheet(this.suggestionOutputNames, false,
				false);
		driver.extractFromSql(this.suggestSql, values, sheet, false);
		return sheet;
	}

	/**
	 *
	 * @return all fields of this record.
	 */
	public Field[] getFields() {
		return this.fields;
	}

	/**
	 * @return all fields mapped by their names
	 */
	public Map<String, Field> getFieldsMap() {
		Map<String, Field> map = new HashMap<String, Field>();
		for (Field field : this.fields) {
			map.put(field.name, field);
		}
		return map;
	}

	/**
	 * field name specified at record level is not defined as a field
	 *
	 * @param fieldName
	 */
	private void invalidFieldName(String fieldName) {
		throw new ApplicationError(fieldName
				+ " is specified as a field in record " + this.name
				+ " but that field is not defined.");
	}

	/*
	 * **********************************************
	 */

	/**
	 * extract and validate a data sheet
	 *
	 * @param inData
	 * @param names
	 *            leave it null if you want all fields
	 * @param errors
	 * @param purpose
	 * @param saveActionExpected
	 * @return data sheet, or null in case of validation errors
	 */
	public DataSheet extractSheet(String[][] inData, String[] names,
			List<FormattedMessage> errors, DataPurpose purpose,
			boolean saveActionExpected) {
		Field[] fieldsToExtract = this.getFieldsToBeExtracted(names, purpose,
				saveActionExpected);
		int nbrFields = fieldsToExtract.length;
		/*
		 * array index in the input rows with matching name for this field. -1
		 * if input has no column for this field
		 */
		int[] indexes = new int[nbrFields];
		String[] header = inData[0];
		ValueType[] types = new ValueType[nbrFields];
		String[] allNames = new String[nbrFields];
		/*
		 * set values for inputFields, types and indexes
		 */
		int idx = 0;
		for (Field field : fieldsToExtract) {
			String fn = field.name;
			int map = -1;
			for (int j = 0; j < header.length; j++) {
				if (fn.equals(header[j])) {
					map = j;
					break;
				}
			}
			indexes[idx] = map;
			types[idx] = field.getValueType();
			allNames[idx] = fn;
			idx++;
		}

		if (inData.length == 1) {
			/*
			 * only header, no data. Let us also create a sheet with no data.
			 */
			return new MultiRowsSheet(fieldsToExtract);
		}

		boolean fieldsAreOptional = purpose == DataPurpose.SUBSET;
		/*
		 * we are all set to extract data from each row now
		 */
		List<Value[]> values = new ArrayList<Value[]>();
		for (int i = 1; i < inData.length; i++) {
			String[] inputRow = inData[i];
			Value[] outputRow = new Value[nbrFields];
			for (int j = 0; j < fieldsToExtract.length; j++) {
				Field field = fieldsToExtract[j];
				int map = indexes[j];
				String textValue = map == -1 ? "" : inputRow[map];
				Value value = field.parseField(textValue, errors,
						fieldsAreOptional, this.name);
				if (value == null) {
					value = Value.newUnknownValue(field.getValueType());
				}
				outputRow[j] = value;
			}
			values.add(outputRow);
		}
		DataSheet ds = new MultiRowsSheet(allNames, values);
		if (this.hasInterFieldValidations) {
			for (FieldsInterface row : ds) {
				for (Field field : fieldsToExtract) {
					field.validateInterfield(row, errors, this.name);
				}
			}
		}
		return ds;

	}

	/**
	 * extract all fields or named fields for the given purpose
	 *
	 * @param inData
	 * @param namesToExtract
	 *            null if all fields are to be extract
	 * @param extractedValues
	 * @param errors
	 * @param purpose
	 *            what fields are extracted depends on the purpose
	 * @param saveActionExpected
	 * @return number of fields extracted
	 */
	public int extractFields(Map<String, String> inData,
			String[] namesToExtract, FieldsInterface extractedValues,
			List<FormattedMessage> errors, DataPurpose purpose,
			boolean saveActionExpected) {
		/*
		 * is the caller providing a list of fields? else we use all
		 */
		Field[] fieldsToExtract = this.getFieldsToBeExtracted(namesToExtract,
				purpose, saveActionExpected);
		if (purpose == DataPurpose.FILTER) {
			Tracer.trace("Extracting filter fields");
			return this.extractFilterFields(inData, extractedValues,
					fieldsToExtract, errors);
		}
		int result = 0;
		boolean fieldsAreOptional = purpose == DataPurpose.SUBSET;
		for (Field field : fieldsToExtract) {
			String text = inData.get(field.name);
			Value value = field.parseField(text, errors, fieldsAreOptional,
					this.name);
			if (value != null) {
				extractedValues.setValue(field.name, value);
				result++;
			}
		}
		if (this.hasInterFieldValidations && fieldsAreOptional == false
				&& result > 1) {
			for (Field field : fieldsToExtract) {
				field.validateInterfield(extractedValues, errors, this.name);
			}
		}
		return result;
	}

	/**
	 * get fields based on names, or all fields
	 *
	 * @param names
	 *            null if all fields are to be extracted
	 * @param purpose
	 * @param extractSaveAction
	 * @return fields
	 */
	public Field[] getFieldsToBeExtracted(String[] names, DataPurpose purpose,
			boolean extractSaveAction) {
		/*
		 * are we being dictated as to what fields to be used?
		 */
		if (names != null) {
			Field[] result = new Field[names.length];
			int i = 0;
			for (String s : names) {
				Field field = this.indexedFields.get(s);
				if (field == null) {
					if (s.equals(TABLE_ACTION_FIELD_NAME)) {
						field = TABLE_ACTION_FIELD;
					} else {
						throw new ApplicationError(s
								+ " is not a valid fiels in Record "
								+ this.name + ". Field can not be extracted.");
					}
				}
				result[i] = field;
				i++;
			}
			return result;
		}
		/*
		 * now that the option is with us, we pick fields based on options
		 */
		if (purpose == DataPurpose.READ) {
			Field[] result = { this.primaryKeyField };
			return result;
		}
		if (extractSaveAction == false) {
			return this.fields;
		}
		Field[] result = new Field[this.fields.length + 1];
		int i = 0;
		for (Field field : this.fields) {
			result[i] = field;
			i++;
		}
		result[i] = TABLE_ACTION_FIELD;
		return result;
	}

	/**
	 * filter fields are special fields that have comparators etc..
	 *
	 * @param inData
	 * @param extractedValues
	 * @param fieldsToExtract
	 * @param errors
	 * @return
	 */
	private int extractFilterFields(Map<String, String> inData,
			FieldsInterface extractedValues, Field[] fieldsToExtract,
			List<FormattedMessage> errors) {
		int result = 0;
		for (Field field : fieldsToExtract) {
			result += field.parseFilter(inData, extractedValues, errors,
					this.name);
		}
		/*
		 * some additional fields for filter, like sort
		 */
		/*
		 * what about sort ?
		 */
		String fieldName = ServiceProtocol.SORT_COLUMN_NAME;
		String textValue = inData.get(fieldName);
		if (textValue != null) {
			Value value = ComponentManager.getDataType(DataType.ENTITY_LIST)
					.parseValue(textValue);
			if (value == null) {
				errors.add(new FormattedMessage(Messages.INVALID_ENTITY_LIST,
						this.defaultSheetName, fieldName, null, 0));
			} else {
				extractedValues.setValue(fieldName, value);
			}
		}

		fieldName = ServiceProtocol.SORT_ORDER;
		textValue = inData.get(fieldName);
		if (textValue != null) {
			textValue = textValue.toLowerCase();
			if (textValue.equals(ServiceProtocol.SORT_ORDER_ASC)
					|| textValue.equals(ServiceProtocol.SORT_ORDER_DESC)) {
				extractedValues.setValue(fieldName,
						Value.newTextValue(textValue));
			} else {
				errors.add(new FormattedMessage(Messages.INVALID_SORT_ORDER,
						this.defaultSheetName, fieldName, null, 0));
			}
		}
		return result;
	}

	/**
	 * @return field names in this record
	 */
	public String[] getFieldNames() {
		return this.fieldNames;
	}

	/**
	 * get a subset of fields.
	 *
	 * @param namesToGet
	 * @return array of fields. ApplicationError is thrown in case any field is
	 *         not found.
	 */
	public Field[] getFields(String[] namesToGet) {
		if (namesToGet == null) {
			return this.fields;
		}
		Field[] result = new Field[namesToGet.length];
		int i = 0;
		for (String s : namesToGet) {
			Field f = this.indexedFields.get(s);
			if (f == null) {
				throw new ApplicationError("Record " + this.getQualifiedName()
						+ " is asked to get a field named " + s
						+ ". Such a field is not defined for this record.");
			}
			result[i] = f;
		}
		return result;
	}

	/**
	 * @return are there fields with inter-field validations?
	 */
	public boolean hasInterFieldValidations() {
		return this.hasInterFieldValidations;
	}

	/**
	 * @param parentSheetName
	 *            if this sheet is to be output as a child. null if a normal
	 *            sheet
	 * @return output record that will copy data sheet to output
	 */
	public OutputRecord getOutputRecord(String parentSheetName) {
		if (parentSheetName == null) {
			return new OutputRecord(this.defaultSheetName);
		}
		return new OutputRecord(this.defaultSheetName, parentSheetName,
				this.parentKeyField.name, this.parentKeyField.referredField);
	}

	/**
	 * @param parentSheetName
	 *            if this sheet is to be output as a child. null if a normal
	 *            sheet
	 * @return output record that will copy data sheet to output
	 */
	public InputRecord getInputRecord(String parentSheetName) {
		if (parentSheetName == null) {
			return new InputRecord(this.defaultSheetName);
		}
		return new InputRecord(this.defaultSheetName, parentSheetName,
				this.parentKeyField.name, this.parentKeyField.referredField);
	}

	/**
	 * @return the suggestionKeyName
	 */
	public String getSuggestionKeyName() {
		return this.suggestionKeyName;
	}

	/**
	 * @return the suggestionOutputNames
	 */
	public String[] getSuggestionOutputNames() {
		return this.suggestionOutputNames;
	}

	/**
	 * @return the valueListKeyName
	 */
	public String getValueListKeyName() {
		return this.valueListKeyName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.simplity.kernel.comp.Component#validate(org.simplity.kernel.comp.
	 * ValidationContext)
	 */
	@Override
	public int validate(ValidationContext ctx) {
		/*
		 * this is probably one of our longest methods. Since processing is
		 * sequential, we have not bothered to cut hem to pieces :-)
		 */
		ctx.beginValidation(MY_TYPE, this.getQualifiedName());
		int count = 0;
		/*
		 * of course we have to have fields
		 */
		if (this.fields.length == 0) {
			ctx.addError("Record has no fields");
			count++;
		}
		/*
		 * reference to other records
		 */
		count += ctx.checkRecordExistence(this.defaultRefRecord,
				"defaultRefRecord", false);
		if (this.childrenToBeRead != null) {
			for (String child : this.childrenToBeRead) {
				count += ctx.checkRecordExistence(child, "childRecord", true);
			}
		}
		if (this.childrenToBeSaved != null) {
			for (String child : this.childrenToBeSaved) {
				count += ctx.checkRecordExistence(child, "childRecord", true);
			}
		}
		/*
		 * validate fields, and accumulate them in a map for other validations
		 */
		Map<String, Field> fieldMap = new HashMap<String, Field>();
		Map<String, Field> columnMap = new HashMap<String, Field>();
		Set<String> referredFields = new HashSet<String>();
		int nbrKeys = 0;
		Field pkey = null;
		for (Field field : this.fields) {
			/*
			 * validate this field
			 */
			count += field.validate(ctx, this, referredFields);
			if (field.fieldType == FieldType.PRIMARY_KEY) {
				pkey = field;
				nbrKeys++;
			}

			/*
			 * look for duplicate field name
			 */
			if (fieldMap.containsKey(field.name)) {
				ctx.addError(field.name + " is a duplicate field name");
				count++;
			} else {
				fieldMap.put(field.name, field);
			}

			/*
			 * duplicate column name?
			 */
			if (field.fieldType != FieldType.TEMP) {
				String colName = field.getColumnName();
				if (columnMap.containsKey(colName)) {
					ctx.addError(colName + " is a duplicate column name");
					count++;
				} else {
					columnMap.put(colName, field);
				}
			}
		}
		/*
		 * we can generate key, but only if it is of integral type
		 */
		if (this.keyToBeGenerated) {
			if (pkey == null) {
				ctx.addError("Primary key is not defined but keyToBeGenerated is set to true.");
				count++;
			} else if (nbrKeys > 0) {
				ctx.addError("keyToBeGenerated is set to true, but primary key is a composite of more than one fields.");
				count++;
			} else {
				DataType dt = ComponentManager.getDataTypeOrNull(pkey.dataType);
				if (dt != null && dt.getValueType() != ValueType.INTEGER) {
					ctx.addError("keyToBeGenerated is set to true, but primary key field is of value type "
							+ dt.getValueType()
							+ ". We generate only integers.");
					count++;
				}
			}
			if (DbDriver.generatorNameRequired()
					&& this.keyGeneratorName == null) {
				ctx.addError("Db Vendor is set to " + DbDriver.getDbVendor()
						+ " for which keyGeneratorName is required");
				count++;
			}
		}
		/*
		 * add referred fields
		 */
		if (this.valueListFieldName != null) {
			referredFields.add(this.valueListFieldName);
		}
		if (this.valueListKeyName != null) {
			referredFields.add(this.valueListKeyName);
		}
		if (this.suggestionKeyName != null) {
			referredFields.add(this.suggestionKeyName);
		}
		if (this.suggestionOutputNames != null) {
			for (String sug : this.suggestionOutputNames) {
				referredFields.add(sug);
			}
		}
		if (referredFields.size() > 0) {
			for (String fn : referredFields) {
				if (fieldMap.containsKey(fn) == false) {
					ctx.addError("Referred field name " + fn
							+ " is not defined for this record");
					count++;
				}
			}
		}
		/*
		 * check from db if fields are Ok with the db
		 */
		if (this.recordType != RecordUsageType.STRUCTURE) {
			String nam = this.tableName;
			if (nam == null) {
				nam = this.name;
			}
			DataSheet columns = DbDriver.getTableColumns(null, nam);
			int nbrCols = columns.length();
			/*
			 * as of now, we check only if names match. we will do more. refer
			 * to DbDrive.COL_NAMES for sequence of columns in each row of
			 * columns data sheet
			 */
			for (int i = 0; i < nbrCols; i++) {
				Value[] row = columns.getRow(i);
				String colName = row[2].toText();
				/*
				 * we should cross-check value type and size. As of now let us
				 * check for length issues with text fields
				 */
				Field field = columnMap.remove(colName);
				if (field == null) {
					/*
					 * column not in this record. No problems.
					 */
					continue;
				}
				if (field.dataType != null) {
					DataType dt = ComponentManager
							.getDataTypeOrNull(field.dataType);
					if (dt != null && dt.getValueType() == ValueType.TEXT) {
						int len = (int) ((IntegerValue) row[5]).getLong();
						int dtLen = dt.getMaxLength();
						if (dtLen > len) {
							ctx.addError("Field " + field.name
									+ " allows a length of " + dtLen
									+ " but db allows a max of " + len
									+ " chars");
						}
					}
				}
			}
			if (columnMap.size() > 0) {
				for (String key : columnMap.keySet()) {
					ctx.addError(key
							+ " is not a valid column name in the data base table/view");
					count++;
				}
			}
		}
		if (this.schemaName != null
				&& DbDriver.isSchmeaDefined(this.schemaName) == false) {
			ctx.addError("schemaName is set to "
					+ this.schemaName
					+ " but it is not defined as one of additional schema names in application.xml");
		}
		ctx.endValidation();
		return count;
	}

	@Override
	public ComponentType getComponentType() {
		return MY_TYPE;
	}

	/**
	 * get schema name that the table/view associated with this record is part
	 * of. null if it is part of default schema
	 *
	 * @return null if it is default. Otherwise schema name.
	 */
	public String getSchemaName() {
		return this.schemaName;
	}
}
