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

/**
 * RDBMS vendors that we can use. This Enum helps is documenting as to how to
 * add a new vendor. as of now we use only two features that are supported by
 * all in a standard way.
 *
 * @author simplity.org
 *
 */
public enum DbVendor {
	/**
	 * oracle
	 */
	ORACLE {
		private static final String G = "select sys_context('userenv','current_schema') x from dual";
		private static final String S = "ALTER SESSION SET CURRENT_SCHEMA = ";

		@Override
		public String getGetSchemaSql() {
			return G;
		}

		@Override
		public String getSetSchemaSql(String schema) {
			return S + schema;
		}

	}

	/**
	 * Microsoft Sql Server
	 */
	,
	MSSQL('%', '_', '[', ']') {
		private static final String G = "select schem_name()";
		private static final String S = "use ";

		@Override
		public String getGetSchemaSql() {
			return G;
		}

		@Override
		public String getSetSchemaSql(String schema) {
			return S + schema;
		}
	}

	/**
	 * postgre sql
	 */
	,
	POSTGRE {
		private static final String G = "select current_schema()";
		private static final String S = "SET schema ";

		@Override
		public String getGetSchemaSql() {
			return G;
		}

		@Override
		public String getSetSchemaSql(String schema) {
			return S + schema;
		}
	}

	/**
	 * my sql
	 */
	,
	MYSQL {
		private static final String G = "SELECT DATABASE()";
		private static final String S = "USE ";

		@Override
		public String getGetSchemaSql() {
			return G;
		}

		@Override
		public String getSetSchemaSql(String schema) {
			return S + schema;
		}

	}
	/**
	 * h2
	 */
	,
	H_2 {
		private static final String G = "SELECT DATABASE()";
		private static final String S = "USE ";

		@Override
		public String getGetSchemaSql() {
			return G;
		}

		@Override
		public String getSetSchemaSql(String schema) {
			return S + schema;
		}

	}
	;

	/*
	 * fields default to standard
	 */
	private String timeStampFunctionName;
	private char[] escapeCharsForLike;

	DbVendor() {
		this.timeStampFunctionName = "CURRENT_TIMESTAMP";
		char[] ec = { '%', '_' };
		this.escapeCharsForLike = ec;
	}

	/**
	 *
	 */
	private DbVendor(String timeStampFunctionName, char... escapeCharsForLike) {
		this.timeStampFunctionName = timeStampFunctionName;
		this.escapeCharsForLike = escapeCharsForLike;
	}

	/**
	 *
	 */
	private DbVendor(char... escapeCharsForLike) {
		this.timeStampFunctionName = "CURRENT_TIMESTAMP";
		this.escapeCharsForLike = escapeCharsForLike;
	}

	/**
	 *
	 * @return sql function name to get current time stamp
	 */
	public String getTimeStamp() {
		return this.timeStampFunctionName;
	}

	/**
	 *
	 * @return escape characters recognised inside a like parameter.
	 */
	public char[] getEscapesForLike() {
		return this.escapeCharsForLike;
	}

	/**
	 *
	 * @return get the sql that returns the default schema for the logged-in
	 *         user
	 */
	public abstract String getGetSchemaSql();

	/**
	 * @param schema
	 *            to bw set as default
	 * @return ddl sql get the sql that sets the schema
	 */
	public abstract String getSetSchemaSql(String schema);
}
