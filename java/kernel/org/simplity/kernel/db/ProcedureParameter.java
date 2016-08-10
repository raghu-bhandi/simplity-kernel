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
package org.simplity.kernel.db;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;

import oracle.jdbc.driver.OracleConnection;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;

/**
 * parameter used in a prepared statement or stored procedure
 *
 * @author simplity.org
 *
 */
public class ProcedureParameter {
	/**
	 * name in which field value is found in input
	 */
	String name;
	/**
	 * data type
	 */
	String dataType;
	/**
	 * Value to be used in case field value id omitted. null implies that the
	 * value is mandatory.
	 */
	String defaultValue;

	/**
	 * in, out or inOut
	 */
	InOutType inOutType = InOutType.INPUT;
	/**
	 * if this is a data structure/object, specify the type name as declared in
	 * the db or stored procedure. This MIUST match the declared data type
	 */
	String sqlObjectType;
	/**
	 * if sqlType is specified, you MUST define a record that has the
	 * corresponding structure.
	 */
	String recordName;
	/**
	 * is this an array parameter?
	 */
	boolean isArray;
	/**
	 * if this is an array, then we have to specify the arrayType name as
	 * defined in the stored procedure
	 */
	String sqlArrayType;

	/**
	 * is this parameter mandatory
	 */
	boolean isRequired;

	/**
	 * cached for performance
	 */
	private DataType dataTypeObject;
	/**
	 * cached for performance
	 */
	private Value defaultValueObjet;

	/**
	 * 1-based index of this parameter
	 */
	private int myPosn;

	/**
	 * get ready after loading. Called only once after lkoading
	 *
	 * @param posn
	 */
	void getReady(int posn) {
		this.myPosn = posn;
		if (this.recordName == null) {
			if (this.dataType == null) {
				throw new ApplicationError(
						"Procedure parameter should be either a struct, in which case recordName is specified, or a primitive of a dataType.");
			}
			this.dataTypeObject = ComponentManager.getDataType(this.dataType);
			if (this.defaultValue != null) {
				this.defaultValueObjet = this.dataTypeObject
						.parseValue(this.defaultValue);
				if (this.defaultValueObjet == null) {
					throw new ApplicationError("sql paramter " + this.name
							+ " has an invalid defaullt value.");
				}
			}
		} else {
			if (this.sqlObjectType == null) {
				throw new ApplicationError(
						"Stored procedure parameter "
								+ this.name
								+ " has a record associated with it, impliying that it is a struct/object. Please specify sqlObjectType as the type of this parameter in the stored procedure");
			}
			this.sqlObjectType = this.sqlObjectType.toUpperCase();
		}
		if (this.isArray) {
			if (this.sqlArrayType == null) {
				throw new ApplicationError(
						"Stored procedure parameter "
								+ this.name
								+ " is an arry, and hence sqlArrayType must be specified as the type with which this parametr is defined in the stored procedure");
			}
			this.sqlArrayType = this.sqlArrayType.toUpperCase();
		}
	}

	/**
	 * called before executing the statement. If this has input, we have to set
	 * its value. If it is for output, we have to register it
	 *
	 * @param stmt
	 * @param inputFields
	 *            from which value for this par
	 * @param ctx
	 *            in case this parameter is an array/object, we get data sheet
	 *            from service context
	 * @return true if all OK, false if we had issue and the process should not
	 *         be continues
	 * @throws SQLException
	 */
	public boolean setParameter(CallableStatement stmt,
			FieldsInterface inputFields, ServiceContext ctx)
			throws SQLException {
		/*
		 * register this param if it is out or in-out
		 */
		if (this.inOutType != InOutType.INPUT) {
			if (this.isArray) {
				stmt.registerOutParameter(this.myPosn, Types.ARRAY,
						this.sqlArrayType);
			} else {
				if (this.recordName != null) {
					stmt.registerOutParameter(this.myPosn, Types.STRUCT,
							this.sqlObjectType);
				} else {
					int scale = this.dataTypeObject.getScale();
					if (scale == 0) {
						stmt.registerOutParameter(this.myPosn, this
								.getValueType().getSqlType());
					} else {
						stmt.registerOutParameter(this.myPosn, this
								.getValueType().getSqlType(), scale);
					}

				}
			}
		}

		if (this.inOutType == InOutType.OUTPUT) {
			return true;
		}

		if (this.isArray == false && this.recordName == null) {
			/*
			 * it is a simple primitive value
			 */
			Value value = inputFields.getValue(this.name);
			if (value == null) {
				value = this.defaultValueObjet;
			}
			if (value == null) {
				if (this.isRequired) {
					ctx.addMessage(org.simplity.kernel.Messages.VALUE_REQUIRED,
							this.name);
					return false;
				}
				stmt.setNull(this.myPosn, this.getValueType().getSqlType());
				return true;
			}
			stmt.setObject(this.myPosn, value.getObject());
			return true;
		}
		/*
		 * non-primitive value is found in ctx as a data sheet
		 */
		DataSheet ds = ctx.getDataSheet(this.name);
		if (ds == null || ds.length() == 0) {
			if (this.isRequired) {
				ctx.addMessage(Messages.VALUE_REQUIRED, this.name);
				return false;
			}
			if (this.isArray) {
				stmt.setNull(this.myPosn, Types.ARRAY, this.sqlArrayType);
			} else {
				// stmt.setNull(this.myPosn, Types.STRUCT);
				stmt.setNull(this.myPosn, Types.STRUCT, this.sqlObjectType);
			}
			return true;
		}
		/*
		 * as of ojdbc6, oracle does not support standard way of creating array
		 * and struct. we have this oracle specific code for that.
		 */
		if (DbDriver.getDbVendor() == DbVendor.ORACLE) {
			return this.setOracleParam(ds, stmt);
		}

		return this.setGenericParam(ds, stmt);
	}

	/**
	 * set parameters as per standard jdbc interface
	 *
	 * @param ds
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	private boolean setGenericParam(DataSheet ds, CallableStatement stmt)
			throws SQLException {
		Connection con = stmt.getConnection();
		Object[] data;
		if (this.isArray) {
			if (this.recordName == null) {
				data = this.getSimpleArray(ds);
			} else {
				data = this.getStructArray(ds, con);
			}
			Array array = con.createArrayOf(this.sqlArrayType, data);
			stmt.setArray(this.myPosn, array);
			return true;
		}
		/*
		 * so, it is a struct
		 */
		data = this.getStruct(ds);
		Struct struct = con.createStruct(this.sqlObjectType, data);
		stmt.setObject(this.myPosn, struct, Types.STRUCT);
		return true;
	}

	/**
	 * get an array of values
	 *
	 * @param ds
	 */
	private Object[] getSimpleArray(DataSheet ds) {
		Value[] values = ds.getColumnValues(this.name);
		if (values == null) {
			/*
			 * possible that the designer has not bothered to name the column.
			 * We will go ahead with the first column. But caller has ensured
			 * that there is data
			 */
			values = ds.getColumnValues(ds.getColumnNames()[0]);
		}

		Object[] result = new Object[values.length];
		int i = 0;
		for (Value val : values) {
			if (val != null) {
				result[i] = val.getObject();
			}
			i++;
		}
		return result;
	}

	/**
	 * get data as array of arrays of objects
	 *
	 * @param ds
	 * @param stmt
	 * @throws SQLException
	 */
	private Object[] getStructArray(DataSheet ds, Connection con)
			throws SQLException {
		int nbrRows = ds.length();
		Object[] result = new Object[nbrRows];
		ValueType[] types = ds.getValueTypes();
		int nbrCols = types.length;
		for (int row = 0; row < nbrRows; row++) {
			Object[] rowData = new Object[nbrCols];
			Value[] rowValues = ds.getRow(row);
			int col = 0;
			for (Value val : rowValues) {
				if (val != null) {
					rowData[col] = val.getObject();
				}
				col++;
			}
			result[row] = con.createStruct(this.sqlObjectType, rowData);
		}
		return result;
	}

	/**
	 * get an array of objects for this structure
	 *
	 * @param ds
	 */
	private Object[] getStruct(DataSheet ds) {
		ValueType[] types = ds.getValueTypes();
		Object[] result = new Object[types.length];
		Value[] rowValues = ds.getRow(0);
		int col = 0;
		for (Value val : rowValues) {
			if (val != null) {
				result[col] = val.getObject();
			}
			col++;
		}
		return result;
	}

	/**
	 * set array and struct for oracle driver
	 *
	 * @param ds
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	private boolean setOracleParam(DataSheet ds, CallableStatement stmt)
			throws SQLException {
		Connection connection = stmt.getConnection();
		OracleConnection con;
		if (connection instanceof OracleConnection) {
			con = (OracleConnection) connection;
		} else {
			con = connection.unwrap(OracleConnection.class);
		}
		if (this.isArray) {
			ARRAY arr = null;
			if (this.recordName == null) {
				arr = this.getOracleArray(ds, con);
			} else {
				arr = this.getOracleStructArray(ds, con);
			}
			stmt.setArray(this.myPosn, arr);
			Tracer.trace(arr.length() + " rows added to procedure parameter "
					+ this.name);
			return true;
		}
		/*
		 * so, it is a struct
		 */
		STRUCT struct = this.getOracleStruct(ds, con);
		stmt.setObject(this.myPosn, struct, Types.STRUCT);
		Tracer.trace("parameter " + this.name + " set to a structure");
		return true;
	}

	/**
	 * get an array of values
	 *
	 * @param ds
	 * @throws SQLException
	 */
	private ARRAY getOracleArray(DataSheet ds, OracleConnection con)
			throws SQLException {
		Value[] values = ds.getColumnValues(this.name);
		if (values == null) {
			/*
			 * possible that the designer has not bothered to name the column.
			 * We will go ahead with the first column. But caller has ensured
			 * that there is data
			 */
			values = ds.getColumnValues(ds.getColumnNames()[0]);
		}

		Object[] result = new Object[values.length];
		int i = 0;
		for (Value val : values) {
			if (val != null) {
				result[i] = val.getObject();
			}
			i++;
		}
		ArrayDescriptor ad = ArrayDescriptor.createDescriptor(
				this.sqlArrayType, con);
		return new ARRAY(ad, con, result);
	}

	/**
	 * get data as array of arrays of objects
	 *
	 * @param ds
	 * @param stmt
	 * @throws SQLException
	 */
	private ARRAY getOracleStructArray(DataSheet ds, OracleConnection con)
			throws SQLException {
		int nbrRows = ds.length();
		STRUCT[] result = new STRUCT[nbrRows];
		ValueType[] types = ds.getValueTypes();
		int nbrCols = types.length;
		StructDescriptor sd = new StructDescriptor(this.sqlObjectType, con);
		for (int row = 0; row < nbrRows; row++) {
			Object[] rowData = new Object[nbrCols];
			Value[] rowValues = ds.getRow(row);
			int col = 0;
			for (Value val : rowValues) {
				if (val != null) {
					rowData[col] = val.getObject();
				}
				col++;
			}
			result[row] = new STRUCT(sd, con, rowData);
		}
		ArrayDescriptor ad = ArrayDescriptor.createDescriptor(
				this.sqlArrayType, con);
		return new ARRAY(ad, con, result);
	}

	/**
	 * get an array of objects for this structure
	 *
	 * @param ds
	 * @throws SQLException
	 */
	private STRUCT getOracleStruct(DataSheet ds, OracleConnection con)
			throws SQLException {
		ValueType[] types = ds.getValueTypes();
		Object[] result = new Object[types.length];
		Value[] rowValues = ds.getRow(0);
		int col = 0;
		for (Value val : rowValues) {
			if (val != null) {
				result[col] = val.getObject();
			}
			col++;
		}
		StructDescriptor sd = StructDescriptor.createDescriptor(
				this.sqlObjectType, con);
		return new STRUCT(sd, con, result);
	}

	/**
	 * extract output, if required, for this parameter
	 *
	 * @param stmt
	 * @param outputFields
	 *            to which out field is to be extracted into
	 * @param ctx
	 *            Service COntext
	 * @throws SQLException
	 */
	public void extractOutput(CallableStatement stmt,
			FieldsInterface outputFields, ServiceContext ctx)
					throws SQLException {
		if (this.inOutType == InOutType.INPUT) {
			return;
		}
		Object value = stmt.getObject(this.myPosn);
		if (value == null) {
			Tracer.trace("Value for parameter " + this.name
					+ " is returned as null from stored procedure");
			return;
		}
		if (this.recordName == null && this.isArray == false) {
			/*
			 * Handle simple case first.
			 */
			Value val = Value.parseObject(value, this.getValueType());
			outputFields.setValue(this.name, val);
			return;
		}
		DataSheet ds;
		if (this.isArray) {
			if (this.recordName == null) {
				ds = this.arrayToDs(value);
			} else {
				ds = this.structsToDs(value);
			}
		} else {
			ds = this.StructToDs(value);
		}
		ctx.putDataSheet(this.name, ds);
	}

	/**
	 * create a data sheet with the given struct as its only row
	 *
	 * @param struct
	 * @return
	 * @throws SQLException
	 */
	private DataSheet StructToDs(Object dbObject) throws SQLException {
		/*
		 * struct is extracted into a data sheet with just one row
		 */
		DataSheet ds = ComponentManager.getRecord(this.recordName).createSheet(
				true, false);
		ValueType[] types = ds.getValueTypes();
		Struct struct = (Struct) dbObject;
		Value[] row = this.getRowFromStruct(struct.getAttributes(), types);
		ds.addRow(row);
		return ds;
	}

	/**
	 * convert a struct into a row of a data sheet
	 *
	 * @param struct
	 * @return
	 */
	private Value[] getRowFromStruct(Object[] struct, ValueType[] types) {
		/*
		 * get values from struct as an array of objects
		 */
		Value[] row = new Value[struct.length];
		int col = 0;
		for (Object val : struct) {
			row[col] = Value.parseObject(val, types[col]);
			col++;
		}
		return row;
	}

	/**
	 * create a data sheet with the array of the only column in that
	 *
	 * @param dbObject
	 *            that is received from db
	 * @return data sheet into which value is extracted
	 * @throws SQLException
	 */
	private DataSheet arrayToDs(Object dbObject) throws SQLException {
		Array arr = (Array) dbObject;
		Value[] values = this.getValueType()
				.toValues((Object[]) arr.getArray());
		String[] columnNames = { this.name };
		Value[][] columnValues = { values };
		return new MultiRowsSheet(columnNames, columnValues);
	}

	/**
	 * create a data sheet out of an array of structs
	 *
	 * @param objectData
	 *            that is received from db converted into array of array of
	 *            objects
	 * @return data sheet into which data from the db object is extracted
	 * @throws SQLException
	 */
	private DataSheet structsToDs(Object dbObject) throws SQLException {
		DataSheet ds = ComponentManager.getRecord(this.recordName).createSheet(
				false, false);
		ValueType[] types = ds.getValueTypes();
		Object[] arr = (Object[]) ((Array) dbObject).getArray();
		for (Object struct : arr) {
			Object[] rowData = ((Struct) struct).getAttributes();
			Value[] row = this.getRowFromStruct(rowData, types);
			ds.addRow(row);
		}
		return ds;
	}

	/**
	 * @return
	 */
	ValueType getValueType() {
		return this.dataTypeObject.getValueType();
	}

	/**
	 * @param e
	 */
	public void reportError(Exception e) {
		StringBuilder msg = new StringBuilder("Procedure parameter ");
		msg.append(this.name).append(" at number ").append(this.myPosn)
		.append(" has caused an exception.");
		if (this.isArray) {
			msg.append(
					"Verify that this array paramater is defined as type "
							+ this.sqlArrayType)
							.append("which is an array of ");
			if (this.recordName != null) {
				msg.append(this.sqlObjectType);
			} else {
				msg.append(this.getValueType());
			}
		} else if (this.recordName == null) {
			msg.append("Ensure that the db type of this parameter is compatible with our type "
					+ this.getValueType());
		}
		if (this.recordName != null) {
			msg.append("Verify that the fields in record ")
			.append(this.recordName)
			.append(" are of the right type/sequence as compared to the type ")
			.append(this.sqlObjectType).append(" in db.");
		}
		throw new ApplicationError(e, msg.toString());
	}

	/**
	 * validate the loaded attributes of this sub-component
	 *
	 * @param ctx
	 * @return number of errors added
	 */
	int validate(ValidationContext ctx) {
		int count = 0;
		if (this.name == null) {
			ctx.addError("Parameter has to have a name. This need not be the same as the one in the db though.");
			count++;
		}
		count += ctx.checkDtExistence(this.dataType, "dataType", false);
		count += ctx.checkRecordExistence(this.recordName, "recordName", false);

		if (this.defaultValue != null) {
			if (this.dataType == null) {
				this.addError(ctx,
						" is non-promitive but a default value is specified.");
				count++;
			} else {
				DataType dt = ComponentManager.getDataTypeOrNull(this.dataType);
				if (dt != null && dt.parseValue(this.defaultValue) == null) {
					this.addError(ctx, " default value of " + this.defaultValue
							+ " is invalid as per dataType " + this.dataType);
				}
			}
		}

		if (this.recordName != null) {
			if (this.dataType != null) {
				this.addError(
						ctx,
						" Both dataType and recordName specified. Use dataType if this is primitive type, or record if it as array or a structure.");
				count++;
			}
			if (this.sqlObjectType == null) {
				this.addError(
						ctx,
						" recordName is specified which means that it is a data-structure. Please specify sqlObjectType as the type of this parameter in the stored procedure");
			}
		} else {
			if (this.dataType == null) {
				this.addError(
						ctx,
						" No data type or recordName specified. Use dataType if this is primitive type, or record if it as an array or a data-structure.");
				count++;
			} else if (this.sqlObjectType != null) {
				ctx.addError("sqlObjectType is relevant ony if this parameter is non-primitive.");
			}

		}

		if (this.isArray) {
			if (this.sqlArrayType == null) {
				this.addError(
						ctx,
						" is an array, and hence sqlArrayType must be specified as the type with which this parametr is defined in the stored procedure");
				count++;
			}
		}

		return count;
	}

	private void addError(ValidationContext ctx, String msg) {
		ctx.addError("Procedure parameter " + this.name + ": " + msg);
	}
}
