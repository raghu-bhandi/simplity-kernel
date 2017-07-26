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

import org.simplity.kernel.data.AlreadyIteratingException;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.DataSheetIterator;
import org.simplity.kernel.expr.Expression;
import org.simplity.kernel.expr.InvalidOperationException;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;

/**
 * add a column to a data sheet.
 *
 * @author admin
 */
public class AddColumn extends Action {
  private static final Logger actionLogger = LoggerFactory.getLogger(AddColumn.class); 

  /** sheet to which we want to add a column */
  String sheetName;

  /** name of column to be added */
  String columnName;

  /** value type of the column */
  ValueType columnValueType = ValueType.TEXT;

  /**
   * if the value of the column is known at design time, provide the value. In case it is the value
   * of a field, use $fieldName as the value
   */
  String columnValue;

  /**
   * if the column value is to be calculated as an expression that involves other columns, then
   * provide the expression. NOTE : watch out for performance if you end up adding column after
   * column to a sheet with large number of rows
   */
  Expression columnValueExpression;

  @Override
  protected Value doAct(ServiceContext ctx) {
    DataSheet sheet = ctx.getDataSheet(this.sheetName);
    if (sheet == null) {
      return Value.VALUE_FALSE;
    }
    if (this.columnValue != null) {
      Value value = null;
      String fieldName = TextUtil.getFieldName(this.columnValue);
      if (fieldName != null) {
        /*
         * it is a field name
         */
        value = ctx.getValue(fieldName);
      } else {
        /*
         * it is a constant
         */
        value = Value.parseValue(this.columnValue, this.columnValueType);
      }
      if (value == null) {

    	  actionLogger.info(
            "Value is null for column "
                + this.columnName
                + " for addColumn action "
                + this.actionName
                + ". Colum nnot added.");

        return Value.VALUE_FALSE;
      }
      sheet.addColumn(this.columnName, value);
      return Value.VALUE_TRUE;
    }
    try {
      int nbrRows = sheet.length();
      if (nbrRows == 0) {
        sheet.addColumn(this.columnName, this.columnValueType, null);
        return Value.VALUE_TRUE;
      }

      Value[] values = new Value[nbrRows];
      DataSheetIterator iterator = ctx.startIteration(this.sheetName);
      int i = 0;
      while (iterator.moveToNextRow()) {
        values[i++] = this.columnValueExpression.evaluate(ctx);
      }
      sheet.addColumn(this.columnName, values[0].getValueType(), values);
      return Value.VALUE_TRUE;
    } catch (InvalidOperationException e) {
      throw new ApplicationError(e, "Error while adding a column to a grid." + e.getMessage());
    } catch (AlreadyIteratingException e) {
      throw new ApplicationError(e, "Error while adding a column to a grid." + e.getMessage());
    }
  }
}
