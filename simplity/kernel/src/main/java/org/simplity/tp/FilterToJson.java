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

import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.service.ResponseWriter;
import org.simplity.service.ServiceContext;

/**
 * Read a row from a record, and possibly read relevant rows from related records
 *
 * @author simplity.org
 */
public class FilterToJson extends DbAction {

  /** record that is used for inputting and creating filter criteria */
  String filterRecordName;
  /** optional. defaults to filterRecordName */
  String outputRecordName;
  /**
   * it is common practice to have each row as an object. However, if we use each row as an array of
   * values, with first row as array of names, the size of json would come down.
   */
  boolean useCompactFormat;

  String outputSheetName;

  /**
   * get a default filterAction for a record, possibly with child rows
   *
   * @param record
   */
  public FilterToJson(Record record) {
    this.actionName = "filter_" + record.getSimpleName();
    String recordName = record.getQualifiedName();
    this.filterRecordName = recordName;
    this.outputRecordName = recordName;
    this.outputSheetName = record.getDefaultSheetName();
  }

  @Override
  protected int doDbAct(ServiceContext ctx, DbDriver driver) {
    Record record = ComponentManager.getRecord(this.filterRecordName);
    Record outRecord = record;
    if (this.outputRecordName != null) {
      outRecord = ComponentManager.getRecord(this.outputRecordName);
    }
    ResponseWriter writer = ctx.getWriter();
    writer.key(this.outputSheetName).array();
    outRecord.filterToJson(record, ctx, driver, this.useCompactFormat, writer);
    writer.endArray();
    return 1;
  }

  @Override
  public DbAccessType getDataAccessType() {
    return DbAccessType.READ_ONLY;
  }

  @Override
  public int validate(ValidationContext ctx, Service service) {
    int count = super.validate(ctx, service);
    count += ctx.checkRecordExistence(this.filterRecordName, "filterRecordName", true);
    count += ctx.checkRecordExistence(this.outputRecordName, "outputRecordName", false);
    return count;
  }
}
