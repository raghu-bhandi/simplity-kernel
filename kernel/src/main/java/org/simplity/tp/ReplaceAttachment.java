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
package org.simplity.tp;

import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.SingleRowSheet;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceProtocol;

/**
 * Save (add/modify/delete) a row from a record, and possibly save relevant rows
 * from related records
 *
 *
 * @author simplity.org
 *
 */
public class ReplaceAttachment extends DbAction {

	/**
	 * rdbms table that has this column
	 */
	String tableName;

	/**
	 * field name of this attachment. This is the name that we refer in our
	 * service context. This is the name that is known to client
	 */
	String attachmentFieldName;

	/**
	 * column name in the db
	 */
	String attachmentColumnName;

	/**
	 * field name of the primary key for this table
	 */
	String keyFieldName;

	/**
	 * column name of the primary key for this table
	 */
	String keyColumnName;

	@Override
	protected int doDbAct(ServiceContext ctx, DbDriver driver) {
		Value tokenValue = ctx.getValue(this.attachmentFieldName);
		Value keyValue = ctx.getValue(this.keyFieldName);
		/*
		 * select existing token into a sheet
		 */
		String sql = "SELECT " + this.attachmentColumnName + " FROM " + this.tableName + " WHERE "
				+ this.keyColumnName + " =?";
		Value[] values = { keyValue };
		String[] columnNames = { this.attachmentFieldName };
		ValueType[] valueTypes = { ValueType.TEXT };
		DataSheet outData = new SingleRowSheet(columnNames, valueTypes);
		int res = driver.extractFromSql(sql, values, outData, true);
		if (res == 0) {
			Tracer.trace("No row in table " + this.tableName + " for key value " + keyValue
					+ " and hence no update.");
			return 0;
		}
		/*
		 * save this token into fieldNameOld. Service will take care of removing
		 * this on exit
		 */
		ctx.setValue(this.attachmentFieldName + ServiceProtocol.OLD_ATT_TOKEN_SUFFIX, outData.getRow(0)[0]);

		/*
		 * update row with new value
		 */
		sql = "UPDATE " + this.tableName + " SET " + this.attachmentColumnName + " = ? " + " WHERE " + this.keyColumnName + " =?";
		Value[] updateValues = { tokenValue, keyValue };
		return driver.executeSql(sql, updateValues, false);
	}

	@Override
	public DbAccessType getDataAccessType() {
		return DbAccessType.READ_WRITE;
	}

	@Override
	public int validate(ValidationContext ctx, Service service) {
		int count = super.validate(ctx, service);
		count += ctx.checkMandatoryField("attachmentFieldName", this.attachmentFieldName);
		count += ctx.checkMandatoryField("attachmentColumnName", this.attachmentColumnName);
		count += ctx.checkMandatoryField("tableName", this.tableName);
		count += ctx.checkMandatoryField("keyColumnName", this.keyColumnName);
		count += ctx.checkMandatoryField("keyFieldName", this.keyFieldName);
		return count;
	}
}
