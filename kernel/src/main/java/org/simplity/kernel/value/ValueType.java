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
package org.simplity.kernel.value;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.simplity.kernel.Tracer;
import org.simplity.kernel.util.DateUtil;

/**
 * basic type of values used to represent data values in application.
 *
 * @author simplity.org
 *
 */
public enum ValueType {
	/**
	 * textual
	 */
	TEXT(Types.VARCHAR, '0', "VARCHAR", "_text") {

		@Override
		public void toJson(String value, StringBuilder json) {
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

		@Override
		public Value extractFromRs(ResultSet resultSet, int posn)
				throws SQLException {
			/*
			 * written for text. others override this
			 */
			String val = resultSet.getString(posn);
			if (resultSet.wasNull()) {
				return Value.newUnknownValue(TEXT);
			}
			return Value.newTextValue(val);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int posn)
				throws SQLException {
			/*
			 * written for text. others override this
			 */
			String val = stmt.getString(posn);
			if (stmt.wasNull()) {
				return Value.newUnknownValue(TEXT);
			}
			return Value.newTextValue(val);
		}

		@Override
		public Value fromObject(Object dbObject) {
			return Value.newTextValue(dbObject.toString());
		}

		@Override
		public Value[] toValues(Object[] arr) {
			Tracer.trace("Going to convert " + arr.length + " objects into "
					+ this.name());
			int n = arr.length;
			Value[] result = new Value[n];
			for (int i = 0; i < n; i++) {
				Object obj = arr[i];
				String val = obj == null ? "null" : obj.toString();
				result[i] = Value.newTextValue(val);
			}
			return result;
		}
	},
	/**
	 * whole numbers with no fraction
	 */
	INTEGER(Types.BIGINT, '1', "BIGINT", "_number") {
		@Override
		public Value extractFromRs(ResultSet resultSet, int idx)
				throws SQLException {
			long val = resultSet.getLong(idx);
			if (resultSet.wasNull()) {
				return Value.newUnknownValue(INTEGER);
			}
			return Value.newIntegerValue(val);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx)
				throws SQLException {
			long val = stmt.getLong(idx);
			if (stmt.wasNull()) {
				return Value.newUnknownValue(INTEGER);
			}
			return Value.newIntegerValue(val);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.simplity.kernel.value.ValueType#fromObject(java.lang.Object)
		 */
		@Override
		public Value fromObject(Object dbObject) {
			long val = 0;
			if (dbObject instanceof Number) {
				val = ((Number) dbObject).longValue();
			} else {
				try {
					val = Long.parseLong(dbObject.toString());
				} catch (Exception e) {
					//
				}
			}
			return Value.newIntegerValue(val);
		}

		@Override
		public Value[] toValues(Object[] arr) {
			int n = arr.length;
			Value[] result = new Value[n];
			int i = 0;
			for (Object obj : arr) {
				result[i] = this.fromObject(obj);
				i++;
			}
			return result;
		}
	},
	/**
	 * number with possible fraction
	 */
	DECIMAL(Types.DECIMAL, '2', "DECIMAL", "_decimal") {
		@Override
		public Value extractFromRs(ResultSet resultSet, int idx)
				throws SQLException {
			double val = resultSet.getDouble(idx);
			if (resultSet.wasNull()) {
				return Value.newUnknownValue(DECIMAL);
			}
			return Value.newDecimalValue(val);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx)
				throws SQLException {
			double val = stmt.getDouble(idx);
			if (stmt.wasNull()) {
				return Value.newUnknownValue(DECIMAL);
			}
			return Value.newDecimalValue(val);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.simplity.kernel.value.ValueType#fromObject(java.lang.Object)
		 */
		@Override
		public Value fromObject(Object dbObject) {
			double val = 0;
			if (dbObject instanceof Number) {
				val = ((Number) dbObject).doubleValue();
			} else {
				try {
					val = Double.parseDouble(dbObject.toString());
				} catch (Exception e) {
					//
				}
			}
			Tracer.trace("I converted " + dbObject + " to " + val);
			return Value.newDecimalValue(val);
		}

		@Override
		public Value[] toValues(Object[] arr) {
			Number[] vals = (Number[]) arr;
			int n = vals.length;
			Value[] result = new Value[n];
			for (int i = 0; i < n; i++) {
				result[i] = Value.newDecimalValue(vals[i].doubleValue());
				Tracer.trace(arr[i] + " got extracted into " + result[i]);
			}
			return result;
		}
	},
	/**
	 * true-false we would have loved to call it binary, but unfortunately that
	 * has different connotation :-)
	 */
	BOOLEAN(Types.BOOLEAN, '3', "BOOLEAN", "_boolean") {
		@Override
		public Value extractFromRs(ResultSet resultSet, int idx)
				throws SQLException {
			Object obj = resultSet.getObject(idx);
			if (resultSet.wasNull()) {
				return Value.newUnknownValue(BOOLEAN);
			}
			boolean val = false;
			if (obj instanceof Boolean) {
				val = ((Boolean) obj).booleanValue();
			} else {
				val = this.parse(obj);
			}
			return Value.newBooleanValue(val);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx)
				throws SQLException {
			Object obj = stmt.getObject(idx);
			if (stmt.wasNull()) {
				return Value.newUnknownValue(BOOLEAN);
			}
			boolean val = false;
			if (obj instanceof Boolean) {
				val = ((Boolean) obj).booleanValue();
			} else {
				val = this.parse(obj);
			}
			return Value.newBooleanValue(val);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.simplity.kernel.value.ValueType#fromObject(java.lang.Object)
		 */
		@Override
		public Value fromObject(Object dbObject) {
			boolean val = true;
			if (dbObject instanceof Boolean) {
				val = ((Boolean) dbObject).booleanValue();
			} else {
				val = this.parse(dbObject);
			}
			if (val) {
				return Value.VALUE_TRUE;
			}
			return Value.VALUE_FALSE;
		}

		@Override
		public Value[] toValues(Object[] arr) {
			Boolean[] vals = (Boolean[]) arr;
			int n = vals.length;
			Value[] result = new Value[n];
			for (int i = 0; i < n; i++) {
				result[i] = Value.newBooleanValue(vals[i].booleanValue());
			}
			return result;
		}

		private boolean parse(Object obj) {
			String str = obj.toString();
			if (str.length() == 0) {
				return false;
			}
			char c = str.charAt(0);
			if (c == ZERO || c == N || c == N1) {
				return false;
			}
			return true;
		}

	},
	/**
	 * date, possibly with specific time of day
	 */
	DATE(Types.DATE, '4', "DATE", "_dateTime") {
		@Override
		public Value extractFromRs(ResultSet resultSet, int idx)
				throws SQLException {
			Date val = resultSet.getDate(idx);
			if (resultSet.wasNull()) {
				return Value.newUnknownValue(DATE);
			}
			return Value.newDateValue(val);
		}

		@Override
		public Value extractFromSp(CallableStatement stmt, int idx)
				throws SQLException {
			Date val = stmt.getDate(idx);
			if (stmt.wasNull()) {
				return Value.newUnknownValue(DATE);
			}
			return Value.newDateValue(val);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.simplity.kernel.value.ValueType#fromObject(java.lang.Object)
		 */
		@Override
		public Value fromObject(Object dbObject) {
			if (dbObject instanceof java.util.Date) {
				return Value.newDateValue((java.util.Date) dbObject);
			}
			String val = dbObject.toString();
			java.util.Date date = DateUtil.parseUtc(val);
			if (date == null) {
				date = DateUtil.parseYmd(val);
			}
			if (date != null) {
				return new DateValue(date.getTime());
			}
			return Value.newUnknownValue(this);
		}

		@Override
		public Value[] toValues(Object[] arr) {
			java.util.Date[] vals = (java.util.Date[]) arr;
			int n = vals.length;
			Value[] result = new Value[n];
			n = 0;
			for (java.util.Date date : vals) {
				result[n] = Value.newDateValue(date);
				n++;
			}
			return result;
		}

	};

	protected static final String QUOTE_STR = "\"";
	protected static final char QUOTE = '"';
	protected static final String EMPTY = "\"\"";
	protected static final String ESCAPED_QUOTE = "\\\"\\\"";
	protected static final String TRUE = "true";
	protected static final String FALSE = "false";
	protected static final String NULL = "null";
	protected static final char ZERO = '0';
	protected static final char N = 'N';
	protected static char N1 = 'n';

	protected final int sqlType;
	protected final char typePrefix;
	protected final String sqlText;
	protected final String defaultDataType;

	ValueType(int sqlType, char typePrefix, String sqlText, String dt) {
		this.sqlType = sqlType;
		this.typePrefix = typePrefix;
		this.sqlText = sqlText;
		this.defaultDataType = dt;
	}

	/**
	 *
	 * @return sql type that can be used to register a parameter o fthis type
	 */
	public int getSqlType() {
		return this.sqlType;
	}

	/**
	 * extracts the value from result set at the current index
	 *
	 * @param resultSet
	 * @param posn
	 * @return value
	 * @throws SQLException
	 */
	public abstract Value extractFromRs(ResultSet resultSet, int posn)
			throws SQLException;

	/**
	 * extracts the value from result set at the current index
	 *
	 * @param stmt
	 * @param posn
	 * @return value
	 * @throws SQLException
	 */
	public abstract Value extractFromSp(CallableStatement stmt, int posn)
			throws SQLException;

	/**
	 * registers return type of a stored procedure
	 *
	 * @param statement
	 * @param posn
	 * @throws SQLException
	 */
	public void registerForSp(CallableStatement statement, int posn)
			throws SQLException {
		statement.registerOutParameter(posn, this.sqlType);
	}

	/**
	 * assuming that the supplied value is a valid value, format it for a json
	 *
	 * @param value
	 *            text input
	 * @param json
	 *            to which value is to be appended
	 */
	public void toJson(String value, StringBuilder json) {
		if (value == null || value.length() == 0) {
			json.append(NULL);
		} else {
			json.append(value);
		}
	}

	/**
	 *
	 * @return char prefix that indicates the value type
	 */
	public char getTypePrefix() {
		return this.typePrefix;
	}

	/**
	 * @param arr
	 * @return value list for the array object
	 */
	public abstract Value[] toValues(Object[] arr);

	/**
	 * @param dbObject
	 *            as returned by a resultSet.getObject()
	 * @return Value of this value type based on the dbObject
	 */
	public Value parseObject(Object dbObject) {
		if (dbObject == null) {
			Tracer.trace("Parse Object received null for type " + this.name());
			return Value.newUnknownValue(this);
		}
		return this.fromObject(dbObject);
	}

	protected abstract Value fromObject(Object dbObject);

	/**
	 * @return get the sql type text
	 */
	public String getSqlTypeText() {
		return this.sqlText;
	}

	/**
	 * @return a default data type for this value type
	 */
	public String getDefaultDataType() {
		return this.defaultDataType;
	}
}
