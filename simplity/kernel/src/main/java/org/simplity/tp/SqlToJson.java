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

import org.simplity.json.JSONWriter;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.db.Sql;
import org.simplity.service.ServiceContext;

/**
 * Read a row/s from as output of a prepared statement/sql
 *
 *
 * @author simplity.org
 *
 */
public class SqlToJson extends DbAction {

	/**
	 * fully qualified sql name
	 */
	String sqlName;

	String jsonName;

	boolean useCompactFormat;

	@Override
	protected int doDbAct(ServiceContext ctx, DbDriver driver) {
		Sql sql = ComponentManager.getSql(this.sqlName);
		JSONWriter writer = new JSONWriter();
		writer.object().key(this.jsonName).array();
		sql.sqlToJson(ctx, driver, this.useCompactFormat, writer);
		writer.endArray().endObject();
		ctx.setObject(this.jsonName, writer.toString());
		return 1;
	}

	@Override
	public DbAccessType getDataAccessType() {
		return DbAccessType.READ_ONLY;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.tp.DbAction#validate(org.simplity.kernel.comp.ValidationContext
	 * , org.simplity.tp.Service)
	 */
	@Override
	public int validate(ValidationContext ctx, Service service) {
		int count = super.validate(ctx, service);

		if (this.sqlName == null) {
			ctx.addError("ReadWithSql requires sql name.");
			count++;
		} else {
			ctx.addReference(ComponentType.SQL, this.sqlName);
		}

		return count;
	}
}
