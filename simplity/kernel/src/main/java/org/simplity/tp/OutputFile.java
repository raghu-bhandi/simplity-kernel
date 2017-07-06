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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.tp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.data.FlatFileRowType;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.expr.Expression;
import org.simplity.kernel.expr.InvalidOperationException;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * Data structure that keeps meta data about a flat-file
 *
 * @author simplity.org
 */
public class OutputFile {
  /**
   * This is expressed using {name} and and {ext} that stand for file name and extension of primary
   * input file. example if input file name is a.txt, then {name}.{ext}.out will translate to
   * a.txt.out and {fileName}.out will translate to a.out.
   *
   * <p>Of course if it is not dependent on the input file, then it can be plain file name, or
   * $fieldName
   */
  String fileName;
  /** record that describes fields/columns in this file */
  String recordName;
  /** how columns/fields are organized in each row */
  FlatFileRowType dataFormat;
  /** in case the row may be written conditionally. Defaults to write always. */
  Expression conditionToOutput;

  /** @param service */
  void getReady(Service service) {
    if (this.fileName == null) {
      throw new ApplicationError("file name is required for outputFile");
    }
  }

  /** @return worker to read from this file */
  public Worker getWorker() {
    return new Worker();
  }

  /**
   * mutable class to take care of run-time requirements of this component
   *
   * @author simplity.org
   */
  class Worker implements BatchOutput {
    protected Record record;
    protected File realFile;
    private BufferedWriter writer;

    String getFileName() {
      return this.realFile.getName();
    }

    void setFileName(String rootFolder, String refFileName, ServiceContext ctx) {
      this.realFile =
          new File(rootFolder + TextUtil.getFileName(OutputFile.this.fileName, refFileName, ctx));
    }

    @Override
    public void openShop(ServiceContext ctx) throws IOException {
      this.record = ComponentManager.getRecord(OutputFile.this.recordName);
      this.writer = new BufferedWriter(new FileWriter(this.realFile));
    }

    @Override
    public boolean outputARow(ServiceContext ctx) throws IOException {
      Expression expr = OutputFile.this.conditionToOutput;
      try {
        if (expr == null || Value.intepretAsBoolean(expr.evaluate(ctx))) {
          this.writer.write((this.record.formatFlatRow(OutputFile.this.dataFormat, ctx)));
          this.writer.newLine();
          return true;
        }
        return false;
      } catch (InvalidOperationException e) {
        throw new ApplicationError(e, "Error while evaluating expresssion " + expr);
      }
    }

    @Override
    public void closeShop(ServiceContext ctx) {
      try {
        this.writer.close();
      } catch (Exception ignore) {
        //
      }
    }
  }
}
