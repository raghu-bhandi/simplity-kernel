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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel.db;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.DynamicSheet;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.IntegerValue;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;

import oracle.jdbc.driver.OracleConnection;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;

/**
 * We use DbDriver as a wrapper on JDBC to restrict the features to a smaller
 * subset that is easy to maintain.
 *
 * @author simplity.org
 *
 */
public class DbDriver {
	// static final int[] TEXT_TYPES = {Types.CHAR, Types.LONGNVARCHAR,
	// Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.VARCHAR};

	/*
	 * for sql escaping
	 */
	private static final String OUR_ESCAPE_CHAR = "!";
	private static final String OUR_ESCAPE_STR = "!!";
	private static final String CONTEXT_PREFIX = "java:/comp/env/";

	/*
	 * we store sql types with corresponding value types
	 */
	private static final int[] LONG_TYPES = { Types.BIGINT, Types.INTEGER,
			Types.SMALLINT };
	private static final int[] DATE_TYPES = { Types.DATE, Types.TIME,
			Types.TIMESTAMP };
	private static final int[] DOUBLE_TYPES = { Types.DECIMAL, Types.DOUBLE,
			Types.FLOAT, Types.REAL };
	private static final int[] BOOLEAN_TYPES = { Types.BIT, Types.BOOLEAN };
	private static final Map<Integer, ValueType> SQL_TYPES = new HashMap<Integer, ValueType>();

	/**
	 * character used in like operator to match any characters
	 */
	public static final char LIKE_ANY = '%';
	/*
	 * meta 0-table columns, 1-primary keys, 2-procedure parameters. refer to
	 * meta.getColumnNames(), getPrimarykeys() and getProcedureColumns() of JDBC
	 */

	/*
	 * we are going to use value types s many time, it is ugly to use full name.
	 * Let us have some short and sweet names
	 */
	private static final ValueType INT = ValueType.INTEGER;
	private static final ValueType TXT = ValueType.TEXT;
	private static final ValueType BOOL = ValueType.BOOLEAN;

	/*
	 * names, types and positions as per result set for meta.getTables()
	 */
	private static final int TABLE_IDX = 0;
	private static final String[] TABLE_NAMES = { "schema", "tableName",
			"tableType", "remarks" };
	private static final ValueType[] TABLE_TYPES = { TXT, TXT, TXT, TXT };
	private static final int[] TABLE_POSNS = { 2, 3, 4, 5 };
	private static final String[] TABLE_TYPES_TO_EXTRACT = { "TABLE", "VIEW" };
	/*
	 * names, types and positions as per result set for meta.getTables()
	 */
	private static final int COL_IDX = 1;
	private static final String[] COL_NAMES = { "schema", "tableName",
			"columnName", "sqlType", "sqlTypeName", "size", "nbrDecimals",
			"remarks", "nullable" };
	private static final ValueType[] COL_TYPES = { TXT, TXT, TXT, INT, TXT, INT,
			INT, TXT, BOOL };
	private static final int[] COL_POSNS = { 2, 3, 4, 5, 6, 7, 9, 12, 18 };

	/*
	 * names, types and positions as per result set for meta.getTables()
	 */
	private static final int KEY_IDX = 2;
	private static final String[] KEY_NAMES = { "columnName", "sequence" };
	private static final ValueType[] KEY_TYPES = { TXT, INT };
	private static final int[] KEY_POSNS = { 4, 5 };

	/*
	 * names, types and positions as per result set for meta.getTables()
	 */
	private static final int PROC_IDX = 3;
	private static final String[] PROC_NAMES = { "schema", "procedureName",
			"procedureType", "remarks" };
	private static final ValueType[] PROC_TYPES = { TXT, TXT, INT, TXT };
	private static final int[] PROC_POSNS = { 2, 3, 8, 7 };

	/*
	 * names, types and positions as per result set for meta.getTables()
	 */
	private static final int PARAM_IDX = 4;
	private static final String[] PARAM_NAMES = { "schema", "procedureName",
			"paramName", "columnType", "sqlType", "sqlTypeName", "size",
			"precision", "scale", "remarks", "nullable", "position" };
	private static final ValueType[] PARAM_TYPES = { TXT, TXT, TXT, INT, INT,
			TXT, INT, INT, INT, TXT, BOOL, INT };
	private static final int[] PARAM_POSNS = { 2, 3, 4, 5, 6, 7, 9, 8, 10, 13,
			19, 18 };

	/*
	 * names, types and positions as per result set for meta.getUDTs()
	 */
	private static final int STRUCT_IDX = 5;
	private static final String[] STRUCT_NAMES = { "schema", "structName",
			"structType", "remarks" };
	private static final ValueType[] STRUCT_TYPES = { TXT, TXT, TXT, TXT };
	private static final int[] STRUCT_POSNS = { 2, 3, 5, 6 };
	private static final int[] STRUCT_TYPES_TO_EXTRACT = { Types.STRUCT };
	/*
	 * names, types and positions as per result set for meta.getTables()
	 */
	private static final int ATTR_IDX = 6;
	private static final String[] ATTR_NAMES = { "schema", "structName",
			"attributeName", "sqlType", "sqlTypeName", "size", "nbrDecimals",
			"remarks", "nullable", "position" };
	private static final ValueType[] ATTR_TYPES = { TXT, TXT, TXT, INT, TXT,
			INT, INT, TXT, BOOL, INT };
	private static final int[] ATTR_POSNS = { 2, 3, 4, 5, 6, 7, 8, 11, 17, 16 };

	/*
	 * put them into array for modularity
	 */
	private static final String[][] META_COLUMNS = { TABLE_NAMES, COL_NAMES,
			KEY_NAMES, PROC_NAMES, PARAM_NAMES, STRUCT_NAMES, ATTR_NAMES };
	private static final ValueType[][] META_TYPES = { TABLE_TYPES, COL_TYPES,
			KEY_TYPES, PROC_TYPES, PARAM_TYPES, STRUCT_TYPES, ATTR_TYPES };
	private static final int[][] META_POSNS = { TABLE_POSNS, COL_POSNS,
			KEY_POSNS, PROC_POSNS, PARAM_POSNS, STRUCT_POSNS, ATTR_POSNS };

	static {
		for (int i : LONG_TYPES) {
			SQL_TYPES.put(new Integer(i), ValueType.INTEGER);
		}
		for (int i : DATE_TYPES) {
			SQL_TYPES.put(new Integer(i), ValueType.DATE);
		}
		for (int i : DOUBLE_TYPES) {
			SQL_TYPES.put(new Integer(i), ValueType.DECIMAL);
		}
		for (int i : BOOLEAN_TYPES) {
			SQL_TYPES.put(new Integer(i), ValueType.BOOLEAN);
		}
	}
	// private static int numberOfKeysToGenerateAtATime = 100;

	/**
	 * are we to trace all sqls? Used during development/debugging
	 */
	private static boolean traceSqls;
	/**
	 * We use either DataSource, or connection string to connect to the data
	 * base. DataSource is preferred
	 */
	private static DbVendor dbVendor;
	private static DataSource dataSource;
	private static String connectionString;

	/*
	 * this is set ONLY if the app is set for multi-schema. Stored at the time
	 * of setting the db driver
	 */
	private static String defaultSchema = null;

	private static Map<String, DataSource> otherDataSources = null;
	private static Map<String, String> otherConStrings = null;

	/*
	 * RDBMS brand dependent settings. set based on db vendor
	 */
	private static String timeStampFn;
	private static String[] charsToEscapeForLike;

	/**
	 * an open connection is maintained during execution of call back
	 */
	private Connection connection;

	/**
	 * stated access type that is checked for consistency during subsequent
	 * calls
	 */
	private DbAccessType accessType;

	/**
	 * set up to be called before any db operation can be done
	 *
	 * @param vendor
	 * @param dataSourceName
	 * @param driverClassName
	 * @param conString
	 * @param logSqls
	 * @param schemaDetails
	 */
	public static synchronized void initialSetup(DbVendor vendor,
			String dataSourceName, String driverClassName, String conString,
			boolean logSqls, SchemaDetail[] schemaDetails) {
		if (vendor == null) {
			Tracer.trace(
					"This Application has not set dbVendor. We assume that the application does not require any db connection.");
			if (dataSourceName != null || driverClassName != null
					|| conString != null || schemaDetails != null) {
				Tracer.trace(
						"WARNING: Since dbVendor is not set, we ignore other db related settings.");
			}
			return;
		}
		setVendorParams(vendor);
		traceSqls = logSqls;
		/*
		 * use data source if specified
		 */
		if (dataSourceName != null) {
			if (driverClassName != null || conString != null) {
				Tracer.trace(
						"WARNING: Since dataSourceName is specified, we ignore driverClassName and connectionString attributes");
			}

			setDataSource(null, dataSourceName);

			if (schemaDetails != null) {
				otherDataSources = new HashMap<String, DataSource>();
				for (SchemaDetail sd : schemaDetails) {
					if (sd.schemaName == null || sd.dataSourceName == null) {
						throw new ApplicationError(
								"schemaName and dataSourceName are required for mutli-schema operation");
					}
					if (sd.connectionString != null) {
						Tracer.trace(
								"Warning : This application uses data source, and hence connection string for schema "
										+ sd.schemaName + " ignored");
					}
					setDataSource(sd.schemaName.toUpperCase(),
							sd.dataSourceName);
				}
			}
			return;
		}

		/*
		 * connection string
		 */
		if (driverClassName == null) {
			throw new ApplicationError("dbVendor is set to " + vendor
					+ " but no dataSource or driverClassName specified. If you do not need db connection, do not set dbVendor attribute.");
		}

		if (conString == null) {
			throw new ApplicationError(
					"driveClassName is specified but connection string is missing in your application set up.");
		}
		try {
			Class.forName(driverClassName);
		} catch (Exception e) {
			throw new ApplicationError(e, "Could not use class "
					+ driverClassName + " as driver class name.");
		}
		Tracer.trace("Driver class name " + driverClassName
				+ " invoked successfully");

		setConnection(null, conString);
		if (schemaDetails != null) {
			Tracer.trace("Checking connection string for additional schemas");
			otherConStrings = new HashMap<String, String>();
			for (SchemaDetail sd : schemaDetails) {
				if (sd.schemaName == null || sd.connectionString == null) {
					throw new ApplicationError(
							"schemaName and connectionString are required for mutli-schema operation");
				}
				if (sd.dataSourceName != null) {
					Tracer.trace(
							"Warning: This application uses connection string, and hence dataSource for schema "
									+ sd.schemaName + " ignored");
				}
				setConnection(sd.schemaName.toUpperCase(), sd.connectionString);
			}
		}
	}

	/**
	 */
	private static void setDataSource(String schema, String dataSourceName) {
		Object obj = null;
		String msg = null;
		try {
			obj = new InitialContext().lookup(dataSourceName);
		} catch (Exception e) {
			if (dataSourceName.startsWith(CONTEXT_PREFIX)) {
				msg = e.getMessage();
			} else {
				try {
					obj = new InitialContext()
							.lookup(CONTEXT_PREFIX + dataSourceName);
				} catch (Exception e1) {
					msg = e1.getMessage();
				}
			}
		}
		if (obj == null) {
			throw new ApplicationError("Error while using data source name "
					+ dataSourceName + "\n" + msg);
		}
		if (obj instanceof DataSource == false) {
			throw new ApplicationError("We got an object instance of "
					+ obj.getClass().getName() + " as data source for name "
					+ dataSourceName + " while we were expecting a "
					+ DataSource.class.getName());
		}
		DataSource ds = (DataSource) obj;
		Connection con = null;
		ApplicationError err = null;
		try {
			con = ds.getConnection();
			if (schema == null) {
				defaultSchema = extractDefaultSchema(con);
			}
			if (schema == null) {
				dataSource = ds;
				Tracer.trace("Database connection for " + dbVendor
						+ " established successfully using dataSource. Default schema is "
						+ defaultSchema);
			} else {
				otherDataSources.put(schema.toUpperCase(), ds);
				Tracer.trace("DataSource added for schema " + schema);
			}
		} catch (SQLException e) {
			err = new ApplicationError(e,
					"Data source is initialized but error while opening connection.");
		} finally {
			try {
				if (con != null) {
					con.close();
				}
			} catch (Exception ignore) {
				//
			}
		}
		if (err != null) {
			throw err;
		}
	}

	/**
	 * set connection string for a schema after testing it
	 *
	 * @param schema
	 * @param conString
	 */
	private static void setConnection(String schema, String conString) {
		Connection con = null;
		Exception err = null;
		try {
			con = DriverManager.getConnection(conString);
			if (schema == null) {
				defaultSchema = extractDefaultSchema(con);
				connectionString = conString;
				Tracer.trace("Database connection for " + dbVendor
						+ " established successfully using a valid connection string. Default schema is "
						+ defaultSchema);
			} else {
				otherConStrings.put(schema.toUpperCase(), conString);
				Tracer.trace(
						"Additional connection string validated for schema "
								+ schema);
			}
		} catch (Exception e) {
			err = e;
		} finally {
			try {
				if (con != null) {
					con.close();
				}
			} catch (Exception ignore) {
				//
			}
		}
		if (err != null) {
			throw new ApplicationError(err,
					" Database set up using connection string failed after successfully setting the driver for "
							+ (schema == null ? " default schema"
									: (" schema " + schema)));
		}
	}

	/**
	 * set parameters that depend on the selected vendor
	 *
	 * @param vendor
	 */
	private static void setVendorParams(DbVendor vendor) {
		Tracer.trace("dbVendor is set to " + vendor);
		dbVendor = vendor;
		char[] chars = vendor.getEscapesForLike();
		charsToEscapeForLike = new String[chars.length];
		for (int i = 0; i < chars.length; i++) {
			charsToEscapeForLike[i] = chars[i] + "";
		}

		timeStampFn = vendor.getTimeStamp();
	}

	/**
	 * only way to get an instance of RdbDriver to do rdbms operations.
	 * Necessary resources are allotted, and the passed object is called back
	 * with workWithDriver(instance)
	 *
	 * @param callBackObject
	 * @param accessType
	 */
	public static void workWithDriver(DbClientInterface callBackObject,
			DbAccessType accessType) {
		workWithDriver(callBackObject, accessType, null);
	}

	/**
	 * only way to get an instance of RdbDriver to do rdbms operations.
	 * Necessary resources are allotted, and the passed object is called back
	 * with workWithDriver(instance)
	 *
	 * @param callBackObject
	 * @param accType
	 * @param schema
	 *            optional. Use Only if your application is designed to work with
	 *            multiple schemas, AND this session transaction need to use a
	 *            schema different from the default one
	 */
	public static void workWithDriver(DbClientInterface callBackObject,
			DbAccessType accType, String schema) {
		Connection con = null;
		if (accType != DbAccessType.NONE) {
			con = getConnection(accType, schema);
		}
		DbDriver driver = new DbDriver(con, accType);
		boolean allOk = false;
		Exception exception = null;
		try {
			allOk = callBackObject.workWithDriver(driver);
		} catch (Exception e) {
			Tracer.trace(e,
					"Callback object threw an exception while working with the driver");
			exception = e;
		}
		if (con != null) {
			closeConnection(con, accType, allOk);
		}
		if (exception != null) {
			String msg = "Error while executing a service. "
					+ exception.getMessage();
			throw new ApplicationError(exception, msg);
		}
	}

	/**
	 * get a connection to the db
	 *
	 * @param acType
	 * @param schema
	 * @return connection
	 */
	static Connection getConnection(DbAccessType acType, String schema) {
		/*
		 * set sch to an upper-cased schema, but only if it is non-null and
		 * different from default schema
		 */
		String sch = null;
		if (schema != null) {
			sch = schema.toUpperCase();
			if (sch.equals(defaultSchema)) {
				Tracer.trace("service is asking for schema " + schema
						+ " but that is the default. default connection used");
				sch = null;
			} else {
				Tracer.trace("Going to open a non-default connection for schema "
						+ schema);
			}
		}
		Connection con = null;
		Exception err = null;
		try {
			con = createConnection(schema);
			if (acType != null) {
				if (acType == DbAccessType.READ_ONLY) {
					con.setReadOnly(true);
				} else {
					con.setTransactionIsolation(
							Connection.TRANSACTION_READ_COMMITTED);
					con.setAutoCommit(acType == DbAccessType.AUTO_COMMIT);
				}
			}
		} catch (Exception e) {
			if (con != null) {
				try {
					con.close();
				} catch (Exception e1) {
					//
				}
				con = null;
			}
			err = e;
		}
		if (con == null) {
			if (err == null) {
				throw new ApplicationError("Unable to connect to DataBase");
			}
			throw new ApplicationError(err, "Unable to connect to DataBase");
		}
		return con;
	}

	/**
	 * get a connection to the db
	 *
	 * @param acType
	 * @param schema
	 * @return connection
	 * @throws SQLException
	 */
	private static Connection createConnection(String schema)
			throws SQLException {
		/*
		 * set sch to an upper-cased schema, but only if it is non-null and
		 * different from default schema
		 */
		String sch = null;
		if (schema != null) {
			sch = schema.toUpperCase();
			if (sch.equals(defaultSchema)) {
				Tracer.trace("service is asking for schema " + schema
						+ " but that is the default. default connection used");
				sch = null;
			} else {
				Tracer.trace("Going to open a non-default connection for schema "
						+ schema);
			}
		}
		if (dataSource != null) {
			/*
			 * this application uses dataSource
			 */
			if (sch == null) {
				return dataSource.getConnection();
			}
			/*
			 * this service is using a different schema
			 */
			DataSource ds = otherDataSources.get(sch);
			if (ds == null) {
				throw new ApplicationError(
						"No dataSource configured for schema " + sch);
			}
			return ds.getConnection();

		}
		if (connectionString == null) {
			throw new ApplicationError(
					"Database should be initialized properly before any operation can be done.");
		}
		/*
		 * old-fashioned application :-(
		 */
		if (sch == null) {
			return DriverManager.getConnection(connectionString);
		}
		/*
		 * service uses a non-default schema
		 */
		String conString = otherConStrings.get(sch);
		if (conString == null) {
			throw new ApplicationError(
					"No connection string configured for schema " + sch);
		}
		return DriverManager.getConnection(conString);
	}

	/**
	 * private constructor to ensure that we control its instantiation
	 *
	 * @param con
	 * @param dbAccessType
	 */
	private DbDriver(Connection con, DbAccessType dbAccessType) {
		this.connection = con;
		this.accessType = dbAccessType;
	}

	/**
	 * extract output from sql into data sheet
	 *
	 * @param sql
	 *            must be a single prepared sql, with no semicolon at the end.
	 * @param values
	 *            to be put into the prepared sql
	 * @param outSheet
	 *            data sheet that has the expected columns defined in it.
	 * @param oneRowOnly
	 *            true if (at most) one row is to be extracted. false to extract
	 *            all rows
	 * @return number of rows extracted
	 */
	public int extractFromSql(String sql, Value[] values, DataSheet outSheet,
			boolean oneRowOnly) {
		if (traceSqls) {
			this.traceSql(sql, values);
			if (this.connection == null) {
				return 0;
			}
		}
		PreparedStatement stmt = null;
		int result = 0;
		try {
			stmt = this.connection.prepareStatement(sql);
			this.setParams(stmt, values);
			if (oneRowOnly) {
				return this.extractOne(stmt, outSheet);
			}
			result = this.extractAll(stmt, outSheet);
		} catch (SQLException e) {
			throw new ApplicationError(e, "Sql Error while extracting data ");
		} finally {
			this.closeStatment(stmt);
		}
		return result;
	}

	/**
	 * check if this sql result sin at least one row
	 *
	 * @param sql
	 * @param values
	 * @return true if there is at least one row
	 */
	public boolean hasResult(String sql, Value[] values) {
		if (traceSqls) {
			this.traceSql(sql, values);
			if (this.connection == null) {
				return false;
			}
		}
		PreparedStatement stmt = null;
		boolean result = false;
		try {
			stmt = this.connection.prepareStatement(sql);
			this.setParams(stmt, values);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				result = true;
			}
			rs.close();
		} catch (SQLException e) {
			throw new ApplicationError(e, "Sql Error while extracting data ");
		} finally {
			this.closeStatment(stmt);
		}
		return result;
	}

	/**
	 * extract output from sql with different sets of input values into data
	 * sheet
	 *
	 * @param sql
	 *            must be a single prepared sql, with no semicolon at the end.
	 * @param values
	 *            to be put into the prepared sql, one row per batch
	 * @param outSheet
	 *            data sheet that has the expected columns defined in it.
	 * @return number of rows extracted
	 */
	public int extractFromSql(String sql, Value[][] values,
			DataSheet outSheet) {
		if (traceSqls) {
			this.traceBatchSql(sql, values);
			if (this.connection == null) {
				return 0;
			}
		}
		PreparedStatement stmt = null;
		int total = 0;
		try {
			stmt = this.connection.prepareStatement(sql);
			for (Value[] vals : values) {
				this.setParams(stmt, vals);
				total += this.extractAll(stmt, outSheet);
			}
		} catch (SQLException e) {
			throw new ApplicationError(e, "Sql Error while extracting data ");
		} finally {
			this.closeStatment(stmt);
		}
		return total;
	}

	/**
	 * extract output from sql that the caller has o idea about the output
	 * columns
	 *
	 * @param sql
	 *            must be a single prepared sql, with no semicolon at the end.
	 * @param values
	 *            to be put into the prepared sql
	 * @param oneRowOnly
	 *            true if (at most) one row is to be extracted. false to extract
	 *            all rows
	 * @return a data sheet with has 0 or more rows of extracted data
	 */
	public DataSheet extractFromDynamicSql(String sql, Value[] values,
			boolean oneRowOnly) {
		if (traceSqls) {
			this.traceSql(sql, values);
			if (this.connection == null) {
				return null;
			}
		}
		PreparedStatement stmt = null;
		DataSheet result = null;
		try {
			stmt = this.connection.prepareStatement(sql);
			this.setParams(stmt, values);
			if (oneRowOnly) {
				return this.extractMetaOne(stmt);
			}
			result = this.extractMetaAll(stmt);
		} catch (SQLException e) {
			throw new ApplicationError(e, "Sql Error while extracting data ");
		} finally {
			this.closeStatment(stmt);
		}
		return result;
	}

	/**
	 * execute a sql as a prepared statement
	 *
	 * @param sql
	 *            to be executed
	 * @param values
	 *            in the right order for the prepared statement
	 * @param treatSqlErrorAsNoAction
	 *            if true, sql error is treated as if rows affected is zero.
	 *            This is helpful when constraints are added in the db, and we
	 *            would treat failure as validation issue.
	 * @return number of affected rows
	 */
	public int executeSql(String sql, Value[] values,
			boolean treatSqlErrorAsNoAction) {
		PreparedStatement stmt = null;
		if (traceSqls) {
			this.traceSql(sql, values);
			if (this.connection == null) {
				return 0;
			}
		}
		this.checkWritable();
		int result = 0;
		try {
			stmt = this.connection.prepareStatement(sql);
			this.setParams(stmt, values);
			result = stmt.executeUpdate();
		} catch (SQLException e) {
			if (treatSqlErrorAsNoAction) {
				Tracer.trace("SQLException code:" + e.getErrorCode()
				+ " message :" + e.getMessage()
				+ " is treated as zero rows affected.");
			} else {
				throw new ApplicationError(e, "Sql Error while executing sql ");
			}
		} finally {
			this.closeStatment(stmt);
		}
		if (result < 0) {
			Tracer.trace(
					"Number of affected rows is not reliable as we got it as "
							+ result);
		} else {
			Tracer.trace(result + " rows affected.");
		}
		return result;
	}

	/**
	 * execute an insert statement as a prepared statement
	 *
	 * @param sql
	 *            to be executed
	 * @param values
	 *            in the right order for the prepared statement
	 * @param generatedKeys
	 *            array in which generated keys are returned
	 * @param keyNames
	 *            array of names of columns that have generated keys. This is
	 *            typically just one, primary key
	 * @param treatSqlErrorAsNoAction
	 *            if true, sql error is treated as if rows affected is zero.
	 *            This is helpful when constraints are added in the db, and we
	 *            would treat failure as validation issue.
	 * @return number of affected rows
	 */
	public int insertAndGetKeys(String sql, Value[] values,
			long[] generatedKeys, String[] keyNames,
			boolean treatSqlErrorAsNoAction) {
		PreparedStatement stmt = null;
		if (traceSqls) {
			this.traceSql(sql, values);
			if (this.connection == null) {
				return 0;
			}
		}
		this.checkWritable();
		int result = 0;
		try {
			stmt = this.connection.prepareStatement(sql, keyNames);
			this.setParams(stmt, values);
			result = stmt.executeUpdate();
			if (result > 0) {
				this.getGeneratedKeys(stmt, generatedKeys);
			}
		} catch (SQLException e) {
			if (treatSqlErrorAsNoAction) {
				Tracer.trace("SQLException code:" + e.getErrorCode()
				+ " message :" + e.getMessage()
				+ " is treated as zero rows affected.");
			} else {
				throw new ApplicationError(e, "Sql Error while executing sql ");
			}
		} finally {
			this.closeStatment(stmt);
		}
		if (result < 0) {
			Tracer.trace(
					"Number of affected rows is not reliable as we got it as "
							+ result);
		} else {
			Tracer.trace(result + " rows affected.");
		}
		return result;
	}

	/**
	 * extract generated keys into the array
	 *
	 * @param stmt
	 * @param generatedKeys
	 * @throws SQLException
	 */
	private void getGeneratedKeys(Statement stmt, long[] generatedKeys)
			throws SQLException {

		ResultSet rs = stmt.getGeneratedKeys();
		for (int i = 0; i < generatedKeys.length && rs.next(); i++) {
			generatedKeys[i] = rs.getLong(1);
		}
		rs.close();
	}

	/**
	 * for each row in the result set of the sql, call back the iterator.
	 *
	 * @param sql
	 * @param values
	 *            for the sql
	 * @param outputTypes
	 *            value types of output parameters. null if you want us to
	 *            discover it, but it will cost you a few grands :-)
	 * @param iterator
	 *            to be called back with workWithARow(row) method. iteration
	 *            stops if this method returns false;
	 * @return number of rows iterated
	 */
	public int workWithRows(String sql, Value[] values, ValueType[] outputTypes,
			RowIterator iterator) {
		PreparedStatement stmt = null;
		try {
			stmt = this.connection.prepareStatement(sql);
			this.setParams(stmt, values);
			return this.iterate(stmt, outputTypes, iterator);
		} catch (SQLException e) {
			throw new ApplicationError(e, "Sql Error executing service ");
		} finally {
			this.closeStatment(stmt);
		}
	}

	/**
	 * execute a prepared statement, with different sets of values
	 *
	 * @param sql
	 * @param values
	 *            each row should have the same number of values, in the right
	 *            order for the sql
	 * @param treatSqlErrorAsNoAction
	 *            if true, sql error is treated as if rows affected is zero.
	 *            This is helpful when constraints are added in the db, and we
	 *            would treat failure as validation issue.
	 * @return affected rows for each set of values
	 */
	public int[] executeBatch(String sql, Value[][] values,
			boolean treatSqlErrorAsNoAction) {
		if (traceSqls) {
			this.traceBatchSql(sql, values);
			if (this.connection == null) {
				return new int[0];
			}
		}
		this.checkWritable();
		PreparedStatement stmt = null;
		int[] result = new int[0];
		try {
			stmt = this.connection.prepareStatement(sql);
			for (Value[] row : values) {
				this.setParams(stmt, row);
				stmt.addBatch();
			}
			result = stmt.executeBatch();
		} catch (SQLException e) {
			if (treatSqlErrorAsNoAction) {
				Tracer.trace("SQLException code:" + e.getErrorCode()
				+ " message :" + e.getMessage()
				+ " is treated as zero rows affected.");
			} else {
				throw new ApplicationError(e,
						"Sql Error while executing batch ");
			}
		} finally {
			this.closeStatment(stmt);
		}
		int rows = 0;
		for (int j : result) {
			if (j < 0) {
				rows = j;
			} else if (rows >= 0) {
				rows += j;
			}
		}
		if (rows < 0) {
			Tracer.trace(
					"Number of affected rows is not reliable as we got it as "
							+ rows);
		} else {
			Tracer.trace(rows + " rows affected.");
		}
		return result;
	}

	/**
	 * extract output from stored procedure into data sheet
	 *
	 * @param sql
	 *            must be in the standard jdbc format {call
	 *            procedureName(?,?,...)}
	 * @param inputFields
	 * @param outputFields
	 * @param params
	 * @param outputSheets
	 * @param ctx
	 * @return number of rows extracted
	 */
	public int executeSp(String sql, FieldsInterface inputFields,
			FieldsInterface outputFields, ProcedureParameter[] params,
			DataSheet[] outputSheets, ServiceContext ctx) {
		if (traceSqls) {
			this.traceSql(sql, null);
			if (this.connection == null) {
				return 0;
			}
		}
		CallableStatement stmt = null;
		int result = 0;
		SQLException err = null;
		try {
			stmt = this.connection.prepareCall(sql);
			if (params != null) {
				for (ProcedureParameter param : params) {
					/*
					 * programmers often make mistakes while defining
					 * parameters. Better to pin-point such errors
					 */
					try {
						if (param.setParameter(stmt, inputFields,
								ctx) == false) {
							Tracer.trace("Error while setting " + param.name
									+ " You will get an error.");
							// issue in setting parameter. May be a mandatory
							// field is not set
							return 0;
						}
					} catch (Exception e) {
						Tracer.trace("Unable to set param " + param.name
								+ " error : " + e.getMessage());
						param.reportError(e);
					}
				}
			}
			boolean hasResult = stmt.execute();
			int i = 0;
			if (outputSheets != null && hasResult) {
				int nbrSheets = outputSheets.length;
				while (hasResult) {
					if (i >= nbrSheets) {
						Tracer.trace(
								"Stored procedure is ready to give more results, but the requester has supplied only "
										+ nbrSheets
										+ " data sheets to read data into. Other data ignored.");
						break;
					}
					DataSheet outputSheet = outputSheets[i];
					ValueType[] outputTypes = outputSheet.getValueTypes();
					ResultSet rs = stmt.getResultSet();
					while (rs.next()) {
						outputSheet.addRow(getParams(rs, outputTypes));
						result++;
					}
					rs.close();
					i++;
					hasResult = stmt.getMoreResults();
				}
			}
			if (params != null) {
				for (ProcedureParameter param : params) {
					/*
					 * programmers often make mistakes while defining
					 * parameters. Better to pin-point such errors
					 */
					try {
						param.extractOutput(stmt, outputFields, ctx);
					} catch (Exception e) {
						param.reportError(e);
					}
				}
			}
		} catch (SQLException e) {
			err = e;
		} finally {
			this.closeStatment(stmt);
		}
		if (err != null) {
			throw new ApplicationError(err,
					"Sql Error while extracting data using stored procedure");
		}

		Tracer.trace(result + " rows extracted.");
		if (result > 0) {
			return result;
		}
		if (outputFields != null) {
			return 1;
		}
		return 0;
	}

	/**
	 * close connection after commit/roll-back
	 *
	 * @param allOk
	 *            if we are managing transaction, true implies commit, and false
	 *            implies roll-back
	 */
	static void closeConnection(Connection con, DbAccessType accType,
			boolean allOk) {
		try {
			Tracer.trace("Going to close a connection of type " + accType
					+ " with allOK = " + allOk);
			if (accType == DbAccessType.READ_WRITE) {
				if (allOk) {
					con.commit();
				} else {
					con.rollback();
				}
			}
		} catch (SQLException e) {
			// throw new ApplicationError(e,
			Tracer.trace(e, "Sql Error while closing database connection. ");
		} finally {
			try {
				con.close();
			} catch (Exception e) {
				//
			}
		}
	}

	/**
	 * sets parameters to prepared statement
	 *
	 * @param stmt
	 * @param values
	 *            First occurrence of null implies logical end of array. This
	 *            feature is added to avoid use of List
	 * @throws SQLException
	 */
	private void setParams(PreparedStatement stmt, Value[] values)
			throws SQLException {
		if (values == null) {
			return;
		}
		int i = 1;
		for (Value value : values) {
			value.setToStatement(stmt, i);
			i++;
		}
	}

	/**
	 * extract rows from a statement into a sheet
	 *
	 * @param stmt
	 * @param outSheet
	 * @return number rows extracted
	 * @throws SQLException
	 */
	private int extractAll(PreparedStatement stmt, DataSheet outSheet)
			throws SQLException {
		ValueType[] outputTypes = outSheet.getValueTypes();
		ResultSet rs = stmt.executeQuery();
		int result = 0;
		while (rs.next()) {
			outSheet.addRow(getParams(rs, outputTypes));
			result++;
		}
		rs.close();
		Tracer.trace(result + " rows extracted.");
		return result;
	}

	/**
	 * extract at most one row from a statement into a sheet
	 *
	 * @param stmt
	 * @param outSheet
	 * @return number rows extracted
	 * @throws SQLException
	 */
	private int extractOne(PreparedStatement stmt, DataSheet outSheet)
			throws SQLException {
		ResultSet rs = stmt.executeQuery();
		int result = 0;
		if (rs.next()) {
			outSheet.addRow(getParams(rs, outSheet.getValueTypes()));

			result = 1;
		}
		rs.close();
		Tracer.trace(result + " rows extracted.");
		return result;
	}

	/**
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	private DataSheet extractMetaAll(PreparedStatement stmt)
			throws SQLException {
		ResultSet rs = stmt.executeQuery();
		DataSheet outSheet = this.createOutSheet(rs);
		this.extractAll(stmt, outSheet);
		rs.close();
		return outSheet;
	}

	/**
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	private DataSheet extractMetaOne(PreparedStatement stmt)
			throws SQLException {
		ResultSet rs = stmt.executeQuery();
		DataSheet outSheet = new DynamicSheet();
		if (rs.next()) {
			ResultSetMetaData md = rs.getMetaData();
			int n = md.getColumnCount();
			for (int i = 1; i <= n; i++) {
				String colName = md.getColumnName(i);
				ValueType type = this.getValueType(md.getColumnType(i));
				Value value = type.extractFromRs(rs, i);
				outSheet.setValue(colName, value);
			}
		}
		rs.close();
		return outSheet;
	}

	/**
	 * extract rows from a statement
	 *
	 * @param stmt
	 * @param outputTypes
	 * @return number of rows iterated
	 * @throws SQLException
	 */
	private int iterate(PreparedStatement stmt, ValueType[] outputTypes,
			RowIterator iterator) throws SQLException {
		ResultSet rs = stmt.executeQuery();
		ValueType[] types = outputTypes == null ? this.getOutputTypes(rs)
				: outputTypes;
		int nbr = 0;
		while (rs.next()) {
			iterator.workWithARow(getParams(rs, types));
			nbr++;
		}
		rs.close();
		return nbr;

	}

	/**
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private ValueType[] getOutputTypes(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int n = md.getColumnCount();
		ValueType[] types = new ValueType[n];
		for (int i = 0; i < types.length; i++) {
			int j = i + 1;
			ValueType type = SQL_TYPES.get(new Integer(md.getColumnType(j)));
			if (type == null) {
				type = ValueType.TEXT;
			}
			types[i] = type;
		}
		return types;
	}

	/**
	 * @param rs
	 * @return data sheet
	 * @throws SQLException
	 */
	private DataSheet createOutSheet(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int n = md.getColumnCount();
		ValueType[] types = new ValueType[n];
		String[] columnNames = new String[n];
		for (int i = 0; i < types.length; i++) {
			int j = i + 1;
			columnNames[i] = md.getColumnName(j);
			types[i] = this.getValueType(md.getColumnType(j));
		}
		return new MultiRowsSheet(columnNames, types);
	}

	private ValueType getValueType(int sqlType) {
		ValueType type = SQL_TYPES.get(new Integer(sqlType));
		if (type == null) {
			return ValueType.TEXT;
		}
		return type;
	}

	/**
	 * gets output parameters from a stored procedure
	 *
	 * @param stmt
	 * @param values
	 * @throws SQLException
	 */
	private static Value[] getParams(ResultSet rs, ValueType[] types)
			throws SQLException {
		Value[] values = new Value[types.length];
		for (int i = 0; i < types.length; i++) {
			values[i] = types[i].extractFromRs(rs, i + 1);
		}
		return values;
	}

	/**
	 * method to be used when the parameters to be extracted are not in the
	 * right order, or we are not extracting each of them, and the caller would
	 * like to specify the position.
	 *
	 * @param stmt
	 * @param values
	 * @throws SQLException
	 */
	private static Value[] getParams(ResultSet rs, ValueType[] types,
			int[] positions) throws SQLException {
		Value[] values = new Value[types.length];
		for (int i = 0; i < types.length; i++) {
			values[i] = types[i].extractFromRs(rs, positions[i]);
		}
		return values;
	}

	/**
	 * @param statement
	 */
	private void closeStatment(Statement statement) {
		if (statement == null) {
			return;
		}
		try {
			statement.close();
		} catch (Exception e) {
			//
		}
	}

	/**
	 * @return sql function to get time stamp
	 */
	public static String getTimeStamp() {
		return timeStampFn;
	}

	/**
	 * put % and escape the text suitable for a LIKE operation as per brand of
	 * RDBMS. we have standardized on ! as escape character
	 *
	 * @param text
	 *            to be escaped
	 * @return go ahead and send this as value of prepared statement for LIKE
	 */
	public static String escapeForLike(String text) {
		String result = text.replaceAll(OUR_ESCAPE_CHAR, OUR_ESCAPE_STR);
		for (String s : charsToEscapeForLike) {
			result = result.replace(s, OUR_ESCAPE_CHAR + s);
		}
		return result;
	}

	private void traceSql(String sql, Value[] values) {
		if (values == null || values.length == 0) {
			Tracer.trace(sql);
			return;
		}
		StringBuilder sbf = new StringBuilder(sql);
		sbf.append("\n Parameters");
		int i = 0;
		for (Value value : values) {
			if (value == null) {
				break;
			}
			i++;
			sbf.append('\n').append(i).append(" : ").append(value.toString());
			if (i > 12) {
				sbf.append("..like wise up to ").append(values.length)
				.append(" : ").append(values[values.length - 1]);
				break;
			}
		}
		Tracer.trace(sbf.toString());
	}

	private void traceBatchSql(String sql, Value[][] values) {
		StringBuilder sbf = new StringBuilder(sql);
		int i = 0;
		for (Value[] row : values) {
			if (row == null) {
				break;
			}
			i++;
			sbf.append("\n SET ").append(i);
			int j = 0;
			for (Value value : row) {
				if (value == null) {
					break;
				}
				j++;
				sbf.append('\n').append(j).append(" : ").append(value);
			}
		}
		// Tracer.trace(sbf.toString());
	}

	/**
	 * get tables/views defined in the database
	 *
	 * @param schemaName
	 *            null, pattern or name
	 * @param tableName
	 *            null, pattern or name
	 * @return data sheet that has attributes for tables/views. Null if no
	 *         output
	 */
	public static DataSheet getTables(String schemaName, String tableName) {
		Connection con = getConnection(DbAccessType.READ_ONLY, schemaName);
		try {
			return getMetaSheet(con, schemaName, tableName, TABLE_IDX);
		} finally {
			closeConnection(con, DbAccessType.READ_ONLY, true);
		}
	}

	/**
	 * get column names of a table
	 *
	 * @param schemaName
	 *            schema to which this table belongs to. leave it null to get
	 *            the table from default schema
	 * @param tableName
	 *            can be null to get all tables or pattern, or actual name
	 * @return sheet with one row per column. Null if no columns.
	 */
	public static DataSheet getTableColumns(String schemaName,
			String tableName) {
		Connection con = getConnection(DbAccessType.READ_ONLY, schemaName);
		try {
			return getMetaSheet(con, schemaName, tableName, COL_IDX);
		} finally {
			closeConnection(con, DbAccessType.READ_ONLY, true);
		}
	}

	/**
	 * get key columns for all tables in the schema
	 *
	 * @param schemaName
	 * @return sheet with one row per column. Null if this table does not exist,
	 *         or something went wrong!!
	 */
	public static DataSheet getPrimaryKeys(String schemaName) {
		Connection con = getConnection(DbAccessType.READ_ONLY, schemaName);
		try {
			return getMetaSheet(con, schemaName, null, KEY_IDX);
		} finally {
			closeConnection(con, DbAccessType.READ_ONLY, true);
		}
	}

	/**
	 * get key columns names of a table
	 *
	 * @param schemaName
	 *            possibly null
	 * @param tableName
	 *            non-null
	 * @return key column names
	 */
	public static String[] getPrimaryKeysForTable(String schemaName,
			String tableName) {
		if (tableName == null) {
			Tracer.trace(
					"getPrimaryKeysForTable() is for a specific table. If you want for all tables, use the getPrimaryKeys()");
			return null;
		}
		Connection con = getConnection(DbAccessType.READ_ONLY, schemaName);
		try {
			DataSheet sheet = getMetaSheet(con, schemaName, tableName, KEY_IDX);
			if (sheet == null) {
				return null;
			}
			int n = sheet.length();
			String[] result = new String[n];
			for (int i = 0; i < n; i++) {
				Value[] row = sheet.getRow(i);
				int idx = (int) ((IntegerValue) row[1]).getLong() - 1;
				result[idx] = row[0].toString();
			}
			return result;
		} finally {
			closeConnection(con, DbAccessType.READ_ONLY, true);
		}
	}

	/**
	 * get stored procedures
	 *
	 * @param schemaName
	 *            null, pattern or name
	 * @param procedureName
	 *            null, pattern or name
	 * @return data sheet that has attributes of procedures. Null if no output
	 */
	public static DataSheet getProcedures(String schemaName,
			String procedureName) {
		Connection con = getConnection(DbAccessType.READ_ONLY, schemaName);
		try {
			return getMetaSheet(con, schemaName, procedureName, PROC_IDX);
		} finally {
			closeConnection(con, DbAccessType.READ_ONLY, true);
		}
	}

	/**
	 * get parameters of procedure
	 *
	 * @param schemaName
	 *            null, pattern or name
	 * @param procedureName
	 *            null, pattern or name
	 * @return sheet with one row per column. Null if this table does not exist,
	 *         or something went wrong!!
	 */
	public DataSheet getProcedureParams(String schemaName,
			String procedureName) {
		Connection con = getConnection(DbAccessType.READ_ONLY, schemaName);
		try {
			return getMetaSheet(con, schemaName, procedureName, PARAM_IDX);
		} finally {
			closeConnection(con, DbAccessType.READ_ONLY, true);
		}
	}

	/**
	 * get structures/user defined types
	 *
	 * @param schemaName
	 *            null, pattern, or name
	 * @param structName
	 *            null or pattern.
	 * @return data sheet containing attributes of structures. Null of no output
	 */
	public DataSheet getStructs(String schemaName, String structName) {
		Connection con = getConnection(DbAccessType.READ_ONLY, schemaName);
		try {
			return getMetaSheet(con, schemaName, structName, STRUCT_IDX);
		} finally {
			closeConnection(con, DbAccessType.READ_ONLY, true);
		}
	}

	/**
	 * get attributes of structure (user defined data type)
	 *
	 * @param schemaName
	 *            null for all or pattern/name
	 * @param structName
	 *            null for all or pattern/name
	 * @return sheet with one row per column. Null if no output
	 */
	public DataSheet getStructAttributes(String schemaName, String structName) {
		Connection con = getConnection(DbAccessType.READ_ONLY, schemaName);
		try {
			return getMetaSheet(con, schemaName, structName, ATTR_IDX);
		} finally {
			closeConnection(con, DbAccessType.READ_ONLY, true);
		}
	}

	private static DataSheet getMetaSheet(Connection con, String schema,
			String metaName, int metaIdx) {
		ResultSet rs = null;
		String schemaName = schema;
		if (schema == null) {
			schemaName = defaultSchema;
		}
		try {
			DatabaseMetaData meta = con.getMetaData();
			switch (metaIdx) {
			case TABLE_IDX:
				rs = meta.getTables(null, schemaName, metaName,
						TABLE_TYPES_TO_EXTRACT);
				break;
			case COL_IDX:
				rs = meta.getColumns(null, schemaName, metaName, null);
				break;
			case KEY_IDX:
				rs = meta.getPrimaryKeys(null, schemaName, metaName);
				break;
			case PROC_IDX:
				rs = meta.getProcedures(null, schemaName, metaName);
				break;
			case PARAM_IDX:
				rs = meta.getProcedureColumns(null, schemaName, metaName, null);
				break;
			case STRUCT_IDX:
				rs = meta.getUDTs(null, schemaName, metaName,
						STRUCT_TYPES_TO_EXTRACT);
				break;
			case ATTR_IDX:
				rs = meta.getAttributes(null, schemaName, metaName, null);
				break;
			default:
				throw new ApplicationError(
						"Meta data " + metaIdx + " is not defined yet.");
			}
			if (rs.next()) {
				DataSheet sheet = new MultiRowsSheet(META_COLUMNS[metaIdx],
						META_TYPES[metaIdx]);
				do {
					sheet.addRow(getParams(rs, META_TYPES[metaIdx],
							META_POSNS[metaIdx]));
				} while (rs.next());
				return sheet;
			}
		} catch (Exception e) {
			Tracer.trace(e, "Unable to get meta data for " + metaName);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) {
					//
				}
			}
		}
		return null;
	}

	/**
	 * @return db vendor from whom the driver is used
	 */
	public static DbVendor getDbVendor() {
		return dbVendor;
	}

	/**
	 *
	 * @param con
	 * @param schema
	 */
	private static String extractDefaultSchema(Connection con) {
		String schema = null;
		try {
			Statement stmt = con.createStatement();
			stmt.executeQuery(dbVendor.getGetSchemaSql());
			ResultSet rs = stmt.getResultSet();
			if (rs.next()) {
				schema = rs.getString(1);
				if (rs.wasNull()) {
					throw new ApplicationError(
							"data base returned null as default schema.");
				}
			} else {
				throw new ApplicationError(
						"data base returned no result for sql "
								+ dbVendor.getGetSchemaSql());
			}
		} catch (SQLException e) {
			throw new ApplicationError(e,
					"Error while getting default schema for this db connection.");
		}
		return schema.toUpperCase();
	}

	/**
	 * @return default schema or null
	 */
	public static Object getDefaultSchema() {
		return defaultSchema;
	}

	/**
	 * @return true if the db vendor needs a key generator , like oracle
	 */
	public static boolean generatorNameRequired() {
		return dbVendor == DbVendor.ORACLE;
	}

	private void checkWritable() {
		if (this.accessType == DbAccessType.READ_ONLY) {
			throw new ApplicationError(
					"Service is set for read-only access to database, but an attempt is made to manipulate data");
		}
	}

	/**
	 * @param schema
	 *            name of schema to check for
	 * @return true is this schema is defined as additional schema. False
	 *         otherwise
	 */
	public static boolean isSchmeaDefined(String schema) {
		if (schema == null) {
			return false;
		}
		String sn = schema.toUpperCase();
		if (sn.equals(defaultSchema)) {
			return true;
		}
		if (dataSource != null) {
			if (otherDataSources != null && otherDataSources.containsKey(sn)) {
				return true;
			}
			return false;
		}
		if (otherConStrings != null && otherConStrings.containsKey(sn)) {
			return true;
		}
		return false;
	}

	/*
	 * methods related to data structure/object in db,
	 */

	/**
	 * delegated back to DBDriver to take care of driver related issues between
	 * Oracle and standard SQL
	 *
	 * @param con
	 * @param values
	 * @param dbArrayType
	 *            as defined in the RDBMS
	 * @return object that is suitable to be assigned to an array parameter
	 * @throws SQLException
	 */
	public static Array createArray(Connection con, Value[] values,
			String dbArrayType) throws SQLException {
		Object[] data = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			Value val = values[i];
			if (val != null) {
				data[i] = val.toObject();
			}
		}
		if (dbVendor == DbVendor.ORACLE) {
			OracleConnection ocon = toOracleConnection(con);
			ArrayDescriptor ad = ArrayDescriptor.createDescriptor(dbArrayType,
					ocon);
			return new ARRAY(ad, ocon, data);
		}
		return con.createArrayOf(dbArrayType, data);
	}

	/**
	 * This is delegated back to DbDriver because oracle driver does not support
	 * standard SQL way of doing this. Let DbDriver class be the repository of
	 * all Driver related issues
	 *
	 * @param con
	 * @param data
	 * @param dbObjectType
	 *            as defined in RDBMS
	 * @return object that can be assigned to a struct parameter
	 * @throws SQLException
	 */
	public static Struct createStruct(Connection con, Object[] data,
			String dbObjectType) throws SQLException {
		if (dbVendor == DbVendor.ORACLE) {
			OracleConnection ocon = toOracleConnection(con);
			StructDescriptor sd = StructDescriptor
					.createDescriptor(dbObjectType, ocon);
			return new STRUCT(sd, ocon, data);
		}
		return con.createStruct(dbObjectType, data);
	}

	/**
	 * This is delegated back to DbDriver because oracle driver does not support
	 * standard SQL way of doing this. Let DbDriver class be the repository of
	 * all Driver related issues
	 *
	 * @param con
	 * @param values
	 * @param dbObjectType
	 *            as defined in RDBMS
	 * @return object that can be assigned to a struct parameter
	 * @throws SQLException
	 */
	public static Struct createStruct(Connection con, Value[] values,
			String dbObjectType) throws SQLException {
		Object[] data = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			Value value = values[i];
			if (value != null) {
				data[i] = value.toObject();
			}
		}
		return createStruct(con, data, dbObjectType);
	}

	/**
	 * Create a struct array that can be assigned to procedure parameter. This
	 * is delegated to DBDriver because of issues with Oracle driver
	 *
	 * @param con
	 * @param structs
	 * @param dbArrayType
	 *            as defined in the rdbms
	 * @return object that is suitable to be assigned to stored procedure
	 *         parameter
	 * @throws SQLException
	 */
	public static Array createStructArray(Connection con, Struct[] structs,
			String dbArrayType) throws SQLException {
		if (dbVendor == DbVendor.ORACLE) {
			OracleConnection ocon = toOracleConnection(con);
			ArrayDescriptor ad = ArrayDescriptor.createDescriptor(dbArrayType,
					ocon);
			return new ARRAY(ad, ocon, structs);
		}
		return con.createArrayOf(dbArrayType, structs);
	}

	private static OracleConnection toOracleConnection(Connection con) {
		if (con instanceof OracleConnection) {
			return (OracleConnection) con;
		}
		try {
			return con.unwrap(OracleConnection.class);
		} catch (Exception e) {
			throw new ApplicationError(
					"Error while unwrapping to Oracle connection. This is a set-up issue with your server. It is probably using a pooled-connection with a flag not to allow access to underlying connection object "
							+ e.getMessage());
		}
	}

	/**
	 * not recommended for use. Use only under strict parental supervision.
	 * ensure that you close it properly
	 *
	 * @return connection object that MUST be closed by you at any cost!!
	 */
	public static Connection getConnection() {
		return getConnection(null, null);
	}
}