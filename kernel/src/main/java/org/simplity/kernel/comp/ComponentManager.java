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

import org.simplity.kernel.Message;
import org.simplity.kernel.db.Sql;
import org.simplity.kernel.db.StoredProcedure;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.dt.DataType;
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
	 * @return message if it is defined, null otherwise
	 */
	public static Message getMessageOrNull(String messageName) {
		return (Message) ComponentType.MSG.getComponentOrNull(messageName);
	}

	/**
	 * note that this throws an error if message is not found
	 *
	 * @param messageName
	 * @return message if it is defined, error otherwise
	 */
	public static Message getMessage(String messageName) {
		return (Message) ComponentType.MSG.getComponentOrNull(messageName);
	}

	/**
	 *
	 * @param dataTypeName
	 * @return dataType if it is defined, null otherwise
	 */
	public static DataType getDataTypeOrNull(String dataTypeName) {
		return (DataType) ComponentType.DT.getComponentOrNull(dataTypeName);
	}

	/**
	 * note that this throws an error if message is not found
	 *
	 * @param dataTypeName
	 * @return DataType if it is defined, error otherwise
	 */
	public static DataType getDataType(String dataTypeName) {
		return (DataType) ComponentType.DT.getComponentOrNull(dataTypeName);
	}

	/**
	 *
	 * @param recordName
	 * @return Record if it is defined, null otherwise
	 */
	public static Record getRecordOrNull(String recordName) {
		return (Record) ComponentType.REC.getComponentOrNull(recordName);
	}

	/**
	 * note that this throws an error if message is not found
	 *
	 * @param recordName
	 * @return Record if it is defined, error otherwise
	 */
	public static Record getRecord(String recordName) {
		return (Record) ComponentType.REC.getComponentOrNull(recordName);
	}

	/**
	 *
	 * @param serviceName
	 * @return Service if it is defined, null otherwise
	 */
	public static ServiceInterface getServiceOrNull(String serviceName) {
		return (ServiceInterface) ComponentType.SERVICE
				.getComponentOrNull(serviceName);
	}

	/**
	 * note that this throws an error if message is not found
	 *
	 * @param serviceName
	 * @return Service if it is defined, error otherwise
	 */
	public static ServiceInterface getService(String serviceName) {
		return (ServiceInterface) ComponentType.SERVICE
				.getComponentOrNull(serviceName);
	}

	/**
	 *
	 * @param sqlName
	 * @return Sql if it is defined, null otherwise
	 */
	public static Sql getSqlOrNull(String sqlName) {
		return (Sql) ComponentType.SQL.getComponentOrNull(sqlName);
	}

	/**
	 * note that this throws an error if message is not found
	 *
	 * @param sqlName
	 * @return message if it is defined, error otherwise
	 */
	public static Sql getSql(String sqlName) {
		return (Sql) ComponentType.SQL.getComponentOrNull(sqlName);
	}

	/**
	 *
	 * @param procedureName
	 * @return StoredProcedure if it is defined, null otherwise
	 */
	public static StoredProcedure getStoredProcedureOrNull(String procedureName) {
		return (StoredProcedure) ComponentType.SP
				.getComponentOrNull(procedureName);
	}

	/**
	 * note that this throws an error if message is not found
	 *
	 * @param procedureName
	 * @return StoredProcedure if it is defined, error otherwise
	 */
	public static StoredProcedure getStoredProcedure(String procedureName) {
		return (StoredProcedure) ComponentType.SP
				.getComponentOrNull(procedureName);
	}

}
