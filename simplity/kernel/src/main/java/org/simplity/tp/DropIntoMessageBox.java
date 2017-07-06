/*
 * Copyright (c) 2017 simplity.org
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
import org.simplity.kernel.expr.Expression;
import org.simplity.kernel.expr.InvalidOperationException;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * set message in the message box
 *
 * @author simplity.org
 */
public class DropIntoMessageBox extends Action {
  /** field/table names to be logged */
  String fieldName;

  Expression expression;

  @Override
  protected Value doAct(ServiceContext ctx) {
    String val = null;
    if (this.fieldName != null) {
      val = ctx.getTextValue(this.fieldName);
    } else if (this.expression != null) {
      try {
        val = this.expression.evaluate(ctx).toString();
      } catch (InvalidOperationException e) {
        throw new ApplicationError(
            "Error while evaluating expression " + this.expression.toString());
      }
    }
    ctx.putMessageInBox(val);
    return null;
  }

  /* (non-Javadoc)
   * @see org.simplity.tp.Action#validate(org.simplity.kernel.comp.ValidationContext, org.simplity.tp.Service)
   */
  @Override
  public int validate(ValidationContext vtx, Service service) {
    int count = super.validate(vtx, service);
    if (this.fieldName == null) {
      if (this.expression == null) {
        vtx.addError("fieldName is required for dropIntoMessageBox action");
        count++;
      }
    } else if (this.expression != null) {
      vtx.addError(
          "Specify either fieldName, or expression, but not both for a dropIntoMessageBox action");
      count++;
    }
    return count;
  }
}
