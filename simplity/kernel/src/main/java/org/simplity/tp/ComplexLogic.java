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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.tp;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * complex logic that is implemented in a java code. Complexity in this is the
 * driver. We believe that a logic that requires db operations in between is
 * leads to maintenance issues, and hence must be developed and reviewed by
 * experienced folks. For enabling such a process, we have separated simple and
 * complex logic interfaces.
 *
 * @author simplity.org
 *
 */
public class ComplexLogic extends Action {
	/**
	 * fully qualified class name
	 */
	String className;

	/**
	 * cached for performance
	 */
	private ComplexLogicInterface logic;

	@Override
	public void getReady(int idx, Service service) {
		super.getReady(idx, service);
		try {
			this.logic = (ComplexLogicInterface) (Class.forName(this.className))
					.newInstance();
		} catch (Exception e) {
			throw new ApplicationError(
					e,
					this.className
							+ " is not a valid class that implements ComplexLogicInterface.");
		}
	}

	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
			return this.logic.execute(ctx, driver);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.tp.Action#validate(org.simplity.kernel.comp.ValidationContext
	 * , org.simplity.tp.Service)
	 */
	@Override
	public int validate(ValidationContext ctx, Service service) {
		int count = super.validate(ctx, service);
		if (this.className == null) {
			ctx.addError("COmplexLogic action requires className");
			count++;
		} else {
			count += ctx.checkClassName(this.className,
					ComplexLogicInterface.class);
		}
		return count;
	}

}
