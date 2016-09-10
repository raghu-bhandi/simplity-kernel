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
	ORACLE("select sys_context('userenv','current_schema') x from dual",
			"ALTER SESSION SET CURRENT_SCHEMA = ")

	/**
	 * Microsoft Sql Server
	 */
	, MSSQL("CURRENT_TIMESTAMP", "select schem_name()", "use ", '%', '_', '[',
			']')

	/**
	 * postgre sql
	 */
	, POSTGRE("select current_schema()", "SET schema ")

	/**
	 * my sql
	 */
	, MYSQL

	/**
	 * S2 data base
	 */
	, S2;

	/*
	 * fields default to standard
	 */
	private String timeStampFunctionName = "CURRENT_TIMESTAMP";
	private char[] escapeCharsForLike = { '%', '_' };
	private String getSchema = "SELECT DATABASE()";
	private String setSchema = "USE ";

	/*
	 * standard sql compliant vendor
	 */
	DbVendor() {
	}

	/**
	 * with non-standard parameters
	 */
	private DbVendor(String timeStampFunctionName, String getSchema,
			String setSchema, char... escapeCharsForLike) {
		this.timeStampFunctionName = timeStampFunctionName;
		this.getSchema = getSchema;
		this.setSchema = setSchema;
		this.escapeCharsForLike = escapeCharsForLike;
	}

	/**
	 * standard except for schema...
	 */
	private DbVendor(String getSchema, String setSchema) {
		this.getSchema = getSchema;
		this.setSchema = setSchema;
	}

	/**
	 * standard except for escape chars..
	 */
	private DbVendor(char... escapeCharsForLike) {
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
	public String getGetSchemaSql() {
		return this.getSchema;
	}

	/**
	 * @param schema
	 *            to bw set as default
	 * @return ddl sql get the sql that sets the schema
	 */
	public String getSetSchemaSql(String schema) {
		return this.setSchema + schema;
	}
}
