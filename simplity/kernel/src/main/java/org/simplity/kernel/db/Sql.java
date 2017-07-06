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

import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.Component;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.data.SingleRowSheet;
import org.simplity.kernel.dm.Field;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

/**
 * A prepared statement with which to interacts with the data base.
 *
 * @author simplity.org
 */
public class Sql implements Component {
  private static final ComponentType MY_TYPE = ComponentType.SQL;

  /** unique within a module */
  String name;

  /** module + name is unique */
  String moduleName;
  /** prepared statement. */
  String preparedStatement;

  /**
   * purpose of this sql/procedure. Important to specify whether you are expecting output, and if so
   * whether we may get more than one rows
   */
  SqlType sqlType;
  /** input parameters. In the same order as in prepared statement. */
  SqlParameter[] inputParameters;

  /**
   * output parameters if this is a select sql. Alternately, you may specify an output record. You
   * should not specify both.
   */
  SqlParameter[] outputParameters;

  /**
   * if you already have a record that has the right fields as input for this sql, this is easier
   * than specifying the fields in inputParameters
   */
  String inputRecordName;
  /** If you already have a record that has the right fields for this sql.. */
  String outputRecordName;
  /** we need names and types repeatedly. Better cache them */
  private String[] outputNames;

  private ValueType[] outputTypes;

  /** @return unqualified name */
  @Override
  public String getSimpleName() {
    return this.name;
  }

  /** @return fully qualified name typically module.name */
  @Override
  public String getQualifiedName() {
    if (this.moduleName == null) {
      return this.name;
    }
    return this.moduleName + '.' + this.name;
  }

  /**
   * @param inSheet
   * @param driver
   * @return extracted data
   */
  public DataSheet extractBatch(DataSheet inSheet, DbDriver driver) {
    if (this.sqlType == SqlType.UPDATE) {
      throw new ApplicationError(
          "Sql "
              + this.getQualifiedName()
              + " is meant for update, but it is called for data extraction");
    }
    /*
     * are we running this sql once, or multiple times?
     */
    int nbrRows = inSheet.length();
    if (nbrRows == 1) {
      return this.extract(inSheet, driver);
    }
    DataSheet outSheet = this.createOutputSheet();
    driver.extractFromSql(this.preparedStatement, this.getInputRows(inSheet), outSheet);
    return outSheet;
  }

  /**
   * @param dataRow
   * @param driver
   * @return extracted data
   */
  public DataSheet extract(FieldsInterface dataRow, DbDriver driver) {
    if (this.sqlType == SqlType.UPDATE) {
      throw new ApplicationError(
          "Sql "
              + this.getQualifiedName()
              + " is meant for update, but it is called for data extraction");
    }
    DataSheet outSheet = this.createOutputSheet();
    Value[] values = this.getInputValues(dataRow);
    boolean singles = this.sqlType == SqlType.SINGLE_SELECT;
    driver.extractFromSql(this.preparedStatement, values, outSheet, singles);
    return outSheet;
  }

  /**
   * @param dataRow
   * @param driver
   * @param callbackObject
   * @return extracted data
   */
  public int processRows(FieldsInterface dataRow, DbDriver driver, DbRowProcessor callbackObject) {
    if (this.sqlType == SqlType.UPDATE) {
      throw new ApplicationError(
          "Sql "
              + this.getQualifiedName()
              + " is meant for update, but it is called for data extraction");
    }
    Value[] values = this.getInputValues(dataRow);
    return driver.processRows(
        this.preparedStatement, values, this.outputNames, this.outputTypes, callbackObject);
  }

  /**
   * @param dataRow
   * @param driver
   * @param treatErrorAsNoAction if true, sql exception is assumed to be because of some
   *     constraints, and hence rows affected is set to 0
   * @return number of affected rows
   */
  public int execute(FieldsInterface dataRow, DbDriver driver, boolean treatErrorAsNoAction) {
    if (this.sqlType != SqlType.UPDATE) {
      throw new ApplicationError(
          "Sql "
              + this.getQualifiedName()
              + " is meant for data extraction, but it is called for update");
    }
    return driver.executeSql(
        this.preparedStatement, this.getInputValues(dataRow), treatErrorAsNoAction);
  }

  /**
   * @param inSheet
   * @param driver
   * @param treatErrorAsNoAction
   * @return number of affected rows
   */
  public int executeBatch(DataSheet inSheet, DbDriver driver, boolean treatErrorAsNoAction) {
    if (this.sqlType != SqlType.UPDATE) {
      throw new ApplicationError(
          "Sql "
              + this.getQualifiedName()
              + " is meant for data extraction, but it is called for update");
    }
    int nbrRows = inSheet.length();
    if (nbrRows == 0) {
      return driver.executeSql(
          this.preparedStatement, this.getInputValues(inSheet), treatErrorAsNoAction);
    }
    int[] result =
        driver.executeBatch(
            this.preparedStatement, this.getInputRows(inSheet), treatErrorAsNoAction);
    nbrRows = 0;
    for (int i : result) {
      if (i == -1) {
        return -1;
      }
      nbrRows += i;
    }
    return nbrRows;
  }

  /** @return a suitable output sheet */
  private DataSheet createOutputSheet() {
    if (this.outputRecordName != null) {
      Record record = ComponentManager.getRecord(this.outputRecordName);
      return record.createSheet(this.sqlType == SqlType.MULTI_SELECT, false);
    }
    if (this.sqlType == SqlType.MULTI_SELECT) {
      return new MultiRowsSheet(this.outputNames, this.outputTypes);
    }
    return new SingleRowSheet(this.outputNames, this.outputTypes);
  }

  /**
   * get input values based on the supplied name-value pair
   *
   * @param inValues
   * @return
   */
  private Value[] getInputValues(FieldsInterface inValues) {
    /*
     * user record
     */
    if (this.inputRecordName != null) {
      Record record = ComponentManager.getRecord(this.inputRecordName);
      Field[] fields = record.getFields();
      Value[] values = new Value[fields.length];
      int i = 0;
      for (Field field : fields) {
        values[i++] = field.getValue(inValues);
      }
      return values;
    }
    /*
     * use parameters
     */
    if (this.inputParameters != null) {
      Value[] values = new Value[this.inputParameters.length];
      int i = 0;
      for (SqlParameter param : this.inputParameters) {
        values[i++] = param.getValue(inValues);
      }
      return values;
    }

    Value[] values = {};
    return values;
  }

  /**
   * get input values based on the supplied name-value pair
   *
   * @param inValues
   * @return
   */
  private Value[][] getInputRows(DataSheet inSheet) {
    int nbrRows = inSheet.length();
    Value[][] values = new Value[nbrRows][];
    for (FieldsInterface row : inSheet) {
      values[nbrRows++] = this.getInputValues(row);
    }
    return values;
  }

  /** called by loader after loading this class. */
  @Override
  public void getReady() {
    if (this.inputParameters != null) {
      for (SqlParameter parm : this.inputParameters) {
        parm.getReady();
      }
    }
    if (this.outputParameters != null) {
      int nbr = this.outputParameters.length;
      this.outputNames = new String[nbr];
      this.outputTypes = new ValueType[nbr];
      for (int i = 0; i < this.outputParameters.length; i++) {
        SqlParameter parm = this.outputParameters[i];
        parm.getReady();
        this.outputNames[i] = parm.name;
        this.outputTypes[i] = parm.getValueType();
      }
    }
  }

  @Override
  public ComponentType getComponentType() {
    return MY_TYPE;
  }

  @Override
  public int validate(ValidationContext ctx) {
    int count = 0;
    ctx.beginValidation(MY_TYPE, this.getQualifiedName());
    try {
      if (this.preparedStatement == null) {
        ctx.addError("preparedStatement is required.");
        count++;
        return count;
      }

      if (!this.preparedStatement.contains("?")) {
        ctx.addError("preparedStatement does not have any parameters");
        count++;
        return count;
      }

      int nbrParams =
          this.preparedStatement.length() - this.preparedStatement.replace("?", "").length();

      if (this.inputParameters != null) {
        if (nbrParams != this.inputParameters.length) {
          ctx.addError(
              "There are "
                  + nbrParams
                  + " parameters in prepared statement, but "
                  + this.inputParameters
                  + " number input parameters.");
          count++;
        }
        for (SqlParameter p : this.inputParameters) {
          count += p.validate(ctx);
        }
        if (this.inputRecordName != null) {
          ctx.addError("Specify either input parameters or inputRecordName but not both.");
          count++;
        }
      }

      if (this.inputRecordName != null) {
        Record record = ComponentManager.getRecordOrNull(this.inputRecordName);
        if (record == null) {
          ctx.addError(
              "inputRecordName is set to " + this.inputRecordName + " but it is not defined.");
          count++;
        } else {
          int n = record.getFields().length;
          if (n != nbrParams) {
            ctx.addError(
                nbrParams
                    + " parameters in prepared statement, but the input record "
                    + this.inputRecordName
                    + " has "
                    + n
                    + " fields.");
            count++;
          }
        }
      }

      if (this.outputParameters != null) {
        if (this.sqlType == SqlType.UPDATE) {
          ctx.addError("This is for update but outputParameters specified.");
          count++;
        }
        for (SqlParameter p : this.inputParameters) {
          count += p.validate(ctx);
        }
        if (this.outputRecordName != null) {
          ctx.addError("Both output parameters and outputRecordName are specified.");
          count++;
        }
      }

      if (this.outputRecordName != null) {
        ctx.checkRecordExistence(this.outputRecordName, "outputRecordName", false);
        if (this.sqlType == SqlType.UPDATE) {
          ctx.addError("This is for update but outputRecordName is specified.");
          count++;
        }
      }

      return count;
    } finally {
      ctx.endValidation();
    }
  }

  /** @return the sqlType */
  public SqlType getSqlType() {
    return this.sqlType;
  }
  /**
   * Create a elements of a josn array directly from the output of this sql
   *
   * @param inData source of values for inut fields
   * @param driver
   * @param useCompactFormat if true, a header array is written first with column names, followed by
   *     an array of values for each row. If false, an object is written for each row.
   * @param writer
   */
  public void sqlToJson(
      FieldsInterface inData, DbDriver driver, boolean useCompactFormat, JSONWriter writer) {
    Value[] values = this.getInputValues(inData);
    String[] names = this.outputNames;
    ValueType[] types = this.outputTypes;
    if (names == null) {
      Record record = ComponentManager.getRecord(this.outputRecordName);
      names = record.getFieldNames();
      types = record.getValueTypes();
    }
    /*
     * in compact form, we write a header row values
     */
    if (useCompactFormat == false) {
      writer.array();
      for (String nam : names) {
        writer.value(nam);
      }
      writer.endArray();
      names = null;
    }
    driver.sqlToJson(this.preparedStatement, values, types, names, writer);
  }
}
