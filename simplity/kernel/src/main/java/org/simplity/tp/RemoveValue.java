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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * simply set value for a field
 *
 * @author simplity.org
 */
public class RemoveValue extends Action {
  static final Logger logger = LoggerFactory.getLogger(RemoveValue.class);

  /**
   * field name. Can be $fieldName, in which case we get the value from service context, and use
   * that value as the name of the field to be removed from the context
   */
  String fieldName;

  /*
   * if fieldName is of the form $fieldName then we keep that run-time field
   * name
   */
  private String runTimeFieldName;

  @Override
  protected Value doAct(ServiceContext ctx) {
    if (this.runTimeFieldName == null) {
      ctx.removeValue(this.fieldName);
    } else {
      Value field = ctx.getValue(this.runTimeFieldName);
      if (field == null) {

        logger.info(
            
            "No value for found in service context for field name "
                + this.runTimeFieldName
                + ". RemoveValue action could not continue.");
        Tracer.trace(
            "No value for found in service context for field name "
                + this.runTimeFieldName
                + ". RemoveValue action could not continue.");
      } else {
        ctx.removeValue(field.toString());
      }
    }
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
  public void getReady(int idx, Service service) {
    super.getReady(idx, service);
    if (this.fieldName == null) {
      throw new ApplicationError(
          "RemoveValue action '" + this.actionName + "' requires either fieldName");
    }
    this.runTimeFieldName = TextUtil.getFieldName(this.fieldName);
  }
}
