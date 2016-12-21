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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.tp;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.expr.Expression;
import org.simplity.kernel.expr.InvalidOperationException;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * simply set value for a field
 *
 *
 * @author simplity.org
 *
 */
public class RemoveValue extends Action {

	/**
	 * field name
	 */
	String fieldName;

	/*
	 * if fieldValue is a field, then we keep that parsed name
	 */
	private String parsedField;

	@Override
	protected Value doAct(ServiceContext ctx, DbDriver driver) {
		if (this.parsedField != null) {
			this.fieldName = ctx.getValue(this.parsedField).toText();
		}
		ctx.removeValue(this.fieldName);
		return Value.newBooleanValue(true);
	}

	@Override
	public DbAccessType getDataAccessType() {
		return DbAccessType.NONE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.tp.Action#getReady()
	 */
	@Override
	public void getReady(int idx) {
		super.getReady(idx);
		if (this.fieldName == null && this.parsedField == null) {
			throw new ApplicationError("RemoveValue action '" + this.actionName
					+ "' requires either fieldName or parsedField to be specified");

		}
		if (this.parsedField != null && Value.parseValue(this.parsedField) == null) {
			throw new ApplicationError(
					"RemovetValue action " + this.actionName + " has an invalid parsedField=" + this.parsedField);
		}
	}
}
