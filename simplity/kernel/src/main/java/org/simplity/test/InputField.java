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

package org.simplity.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.util.JsonUtil;

/**
 * represents a field to be provided as input to a service
 *
 * @author simplity.org
 */
public class InputField {
	private static final Logger logger = LoggerFactory.getLogger(InputField.class);

  /** field name. qualified name is relative to its parent. */
  String fieldSelector;
  /** field value. $variableName to get value from test context */
  String fieldValue;

  /**
   * @param vtx
   * @return number of errors added
   */
  int validate(ValidationContext vtx) {
    int nbr = 0;
    if (this.fieldSelector == null) {
      vtx.addError("fieldSelector is a required attribute for a test field");
      nbr++;
    }
    if (this.fieldValue == null) {
      vtx.addError("fieldValue is required for an input field");
      nbr++;
    }
    return nbr;
  }

  /**
   * @param json json array or object to which this field is to be assigned to
   * @param ctx
   */
  public void setInputValue(Object json, TestContext ctx) {
    if (this.fieldValue == null) {

      logger.info(this.fieldSelector + " has no value, and hence is not added to the input");

      return;
    }
    Object value = this.fieldValue;
    if (this.fieldValue.charAt(0) == '$') {
      value = ctx.getValue(this.fieldValue.substring(1));
    }
    JsonUtil.setValueWorker(this.fieldSelector, json, value);
  }
}
