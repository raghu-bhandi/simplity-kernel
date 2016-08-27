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
package org.simplity.kernel.comp;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Message;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.db.Sql;
import org.simplity.kernel.db.StoredProcedure;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.fn.Function;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceInterface;

/**
 * Utility class with specific named methods rather than dipping into
 * corresponding ComponentType methods
 *
 * @author simplity.org
 *
 */
public class ComponentManager {
	/**
	 *
	 * @param messageName
	 *            name
	 * @return message if it is defined, null otherwise
	 */
	public static Message getMessageOrNull(String messageName) {
		return (Message) ComponentType.MSG.getComponentOrNull(messageName);
	}

	/**
	 * note that this throws an error if message is not found
	 *
	 * @param messageName
	 *            name
	 * @return message if it is defined, error otherwise
	 */
	public static Message getMessage(String messageName) {
		return (Message) ComponentType.MSG.getComponentOrNull(messageName);
	}

	/**
	 *
	 * @param dataTypeName
	 *            name
	 * @return dataType if it is defined, null otherwise
	 */
	public static DataType getDataTypeOrNull(String dataTypeName) {
		return (DataType) ComponentType.DT.getComponentOrNull(dataTypeName);
	}

	/**
	 * note that this throws an error if data type is not found
	 *
	 * @param dataTypeName
	 *            name
	 * @return DataType if it is defined, error otherwise
	 */
	public static DataType getDataType(String dataTypeName) {
		return (DataType) ComponentType.DT.getComponentOrNull(dataTypeName);
	}

	/**
	 *
	 * @param recordName
	 *            name
	 * @return Record if it is defined, null otherwise
	 */
	public static Record getRecordOrNull(String recordName) {
		return (Record) ComponentType.REC.getComponentOrNull(recordName);
	}

	/**
	 * note that this throws an error if record is not found
	 *
	 * @param recordName
	 *            name
	 * @return Record if it is defined, error otherwise
	 */
	public static Record getRecord(String recordName) {
		return (Record) ComponentType.REC.getComponentOrNull(recordName);
	}

	/**
	 *
	 * @param serviceName
	 *            name
	 * @return Service if it is defined, null otherwise
	 */
	public static ServiceInterface getServiceOrNull(String serviceName) {
		return (ServiceInterface) ComponentType.SERVICE
				.getComponentOrNull(serviceName);
	}

	/**
	 * note that this throws an error if service is not found
	 *
	 * @param serviceName
	 *            name
	 * @return Service if it is defined, error otherwise
	 */
	public static ServiceInterface getService(String serviceName) {
		return (ServiceInterface) ComponentType.SERVICE
				.getComponentOrNull(serviceName);
	}

	/**
	 *
	 * @param sqlName
	 *            name
	 * @return Sql if it is defined, null otherwise
	 */
	public static Sql getSqlOrNull(String sqlName) {
		return (Sql) ComponentType.SQL.getComponentOrNull(sqlName);
	}

	/**
	 * note that this throws an error if sql is not found
	 *
	 * @param sqlName
	 *            name
	 * @return message if it is defined, error otherwise
	 */
	public static Sql getSql(String sqlName) {
		return (Sql) ComponentType.SQL.getComponentOrNull(sqlName);
	}

	/**
	 *
	 * @param procedureName
	 *            name
	 * @return StoredProcedure if it is defined, null otherwise
	 */
	public static StoredProcedure getStoredProcedureOrNull(String procedureName) {
		return (StoredProcedure) ComponentType.SP
				.getComponentOrNull(procedureName);
	}

	/**
	 * note that this throws an error if stored procedure is not found
	 *
	 * @param procedureName
	 *            name
	 * @return StoredProcedure if it is defined, error otherwise
	 */
	public static StoredProcedure getStoredProcedure(String procedureName) {
		return (StoredProcedure) ComponentType.SP
				.getComponentOrNull(procedureName);
	}

	/**
	 *
	 * @param functionName
	 *            name
	 * @return a function if it is defined, null otherwise
	 */
	public static Function getFunctionOrNull(String functionName) {
		return (Function) ComponentType.FUNCTION
				.getComponentOrNull(functionName);
	}

	/**
	 * note that this throws an error if function is not found
	 *
	 * @param functionName
	 *            name
	 * @return StoredProcedure if it is defined, error otherwise
	 */
	public static Function getFunction(String functionName) {
		return (Function) ComponentType.FUNCTION
				.getComponentOrNull(functionName);
	}

	/**
	 * evaluate a function and return its value
	 *
	 * @param functionName
	 *            name of function
	 * @param valueList
	 *            array of arguments. Must match arguments of function in the
	 *            right order. null or empty array if this function does not
	 *            require any arguments
	 * @param data
	 *            fields context that may contain other fields that the function
	 *            may refer at run time. This is typically the fieds from
	 *            serviceCOntext
	 * @return value is never null. However value.isNull() could be true.
	 * @throws ApplicationError
	 *             in case the function is not defined, or you are passing wrong
	 *             type of arguments for the function
	 */
	public static Value evaluate(String functionName, Value[] valueList,
			FieldsInterface data) {
		return getFunction(functionName).execute(valueList, data);
	}
}
