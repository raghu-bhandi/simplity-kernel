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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;

import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.data.FlatFileRowType;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.expr.Expression;
import org.simplity.kernel.expr.InvalidOperationException;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceProtocol;

/**
 * Data structure that keeps meta data about a flat-file
 *
 * @author simplity.org
 */
public class InputFile {
protected static final Logger actionLogger = LoggerFactory.getLogger(InputFile.class);

  /**
   * If this is an associate file, then it is expressed using {name} and and {ext} that stand for
   * file name and extension of primary input file. example if input file name is a.txt
   * {name}.{ext}.out will translate to a.txt.out and {fileName}.out will translate to a.out.
   *
   * <p>If this is the primary (driver) file, then this is a pattern as per java file name pattern.
   * Refer to java file filter specification
   */
  String fileName;
  /** record that describes fields/columns in this file */
  String recordName;
  /** how columns/fields are organized in each row */
  FlatFileRowType dataFormat;
  /** in case this is a child input file, specify the link columns (keys) in this row */
  String[] linkFieldsInThisRow;

  /** in case this is a child input file, specify the link columns (keys) */
  String[] linkFieldsInParentRow;
  /** name relative to input file. Valid only if this is used as in input file */
  String renameInfileTo;

  /** valid only if this is input, and renameInfileTo is not specified */
  boolean deleteFile;

  Expression conditionToProcess;

  /** @param service */
  void getReady(Service service) {
    if (this.fileName == null) {
      throw new ApplicationError("file name is required for inputFile");
    }
    if (this.linkFieldsInParentRow == null) {
      if (this.linkFieldsInThisRow != null) {
        this.throwError();
      }
    } else {
      if (this.linkFieldsInThisRow == null
          || this.linkFieldsInThisRow.length != this.linkFieldsInParentRow.length) {
        this.throwError();
      }
    }
  }

  private void throwError() {
    throw new ApplicationError(
        "Input file should linkFieldsInParentRow to match linkFieldsInThisRow ");
  }

  /** @return worker to read from this file */
  public Worker getWorker() {
    return new Worker();
  }

  /**
   * state-full class that is is invoked repeatedly by FileProcessor. We have to have an instance of
   * this for each run. This is our approach to ensure that the main component, INputFile,
   * InputFile, remains immutable
   *
   * @author simplity.org
   */
  class Worker implements BatchInput {
    protected String rootFolder;
    protected Record record;
    private File realFile;

    private LineNumberReader reader;
    private File newFile;
    /*
     * in case we are a child file, and we have to read matching rows
     */
    /** index of key fields to dataRow array */
    private int[] keyIndexes;
    /** data read from input file that is yet to be used (read ahead) */
    private Value[] dataRow;
    /** all the field names in this rec */
    private String[] fieldNames;
    /** computed/concatenated string of key fields */
    private String keyValue;

    /** did we hit the wall while trying to read? */
    private boolean endOfFile;

    @Override
    public String getFileName() {
      return this.realFile.getName();
    }

    /**
     * called before openShop() when input file name is to be decided based on parent file name,
     * typically for child process
     *
     * @param rootFolder
     * @param refFileName
     * @param ctxt
     */
    void setInputFileName(String rootFolder, String refFileName, ServiceContext ctxt) {
      this.rootFolder = rootFolder;
      String fn = TextUtil.getFileName(InputFile.this.fileName, refFileName, ctxt);
      this.realFile = new File(rootFolder + fn);
      if (this.realFile.exists() == false) {
        throw new ApplicationError("File " + this.realFile.getAbsolutePath() + " does not exist");
      }
    }

    /**
     * called before openShop when caller has already set the file to be read. Typically for driver
     * process
     *
     * @param rootFolder
     * @param file
     */
    void setInputFile(String rootFolder, File file) {
      this.rootFolder = rootFolder;
      this.realFile = file;
    }

    @Override
    public void openShop(ServiceContext ctxt) throws IOException {
      this.record = ComponentManager.getRecord(InputFile.this.recordName);

      this.reader = new LineNumberReader(new FileReader(this.realFile));

      String newName = InputFile.this.renameInfileTo;
      if (newName != null) {
        this.newFile =
            new File(this.rootFolder + TextUtil.getFileName(newName, this.getFileName(), ctxt));
      }

      String[] names = InputFile.this.linkFieldsInThisRow;
      if (names == null || names.length == 0) {
        return;
      }
      this.keyIndexes = new int[names.length];
      for (int i = 0; i < names.length; i++) {
        int idx = this.record.getFieldIndex(names[i]);
        if (idx == -1) {
          throw new ApplicationError(
              names[i]
                  + " is not a field in record "
                  + InputFile.this.recordName
                  + " but it is being referred as a key field to match");
        }
        this.keyIndexes[i] = idx;
      }
      this.fieldNames = this.record.getFieldNames();
    }

    @Override
    public boolean inputARow(List<FormattedMessage> errors, ServiceContext ctxt)
        throws IOException {

      /*
       * we loop in case there is some condition for row to be
       * processed
       */
      while (true) {
        String rowText = this.reader.readLine();
        if (rowText == null) {
          return false;
        }
        int lineNumber = this.reader.getLineNumber();

        ctxt.setTextValue(ServiceProtocol.ROW_TEXT, rowText);
        ctxt.setLongValue(ServiceProtocol.LINE_NUM, lineNumber);
        ctxt.setTextValue(ServiceProtocol.FIlE_BATCH, this.realFile.getName());
        /*
         * parse this text into fields and push them to service context
         */
        this.record.parseFlatFileRow(rowText, InputFile.this.dataFormat, ctxt, errors);
        /*
         * is there a condition to process this?
         */
        if (this.okToProceed(ctxt)) {
          return true;
        }
      }
    }

    /**
     * should the rows from this file be read for matching keys?
     *
     * @return true mens the caller should first get parent key and then read for matching. false
     *     means a row is read always.
     */
    boolean toBeMatched() {
      return this.fieldNames != null;
    }

    @Override
    public String getParentKeyValue(List<FormattedMessage> errors, ServiceContext ctxt) {
      if (this.keyIndexes == null) {
        return null;
      }
      String parentKey = "";
      for (int i = 0; i < this.keyIndexes.length; i++) {
        Value value = ctxt.getValue(InputFile.this.linkFieldsInParentRow[i]);
        if (Value.isNull(value)) {
          errors.add(
              new FormattedMessage(
                  "missingKeyColumn",
                  MessageType.ERROR,
                  "value for link field "
                      + InputFile.this.linkFieldsInThisRow[i]
                      + " is missing in a row in file "
                      + this.getFileName()));
          return null;
        }
        parentKey += value.toString();
      }
      return parentKey;
    }

    /**
     * read a line from reader, provided there is something to be read
     *
     * @return a line, or null if there is nothing more to read
     * @throws IOException
     */
    private String readLine() throws IOException {
      if (this.endOfFile) {
        return null;
      }

      String rowText = this.reader.readLine();
      if (rowText == null || rowText.length() == 0) {
        this.endOfFile = true;
      }
      return rowText;
    }

    @Override
    public boolean inputARow(List<FormattedMessage> errors, String keyToMatch, ServiceContext ctxt)
        throws IOException, InvalidRowException {
      boolean allOk = true;
      /*
       * ensure that we have a row in our cache before we enter the
       * while-loop, unless of course it is end-of-file
       */
      if (this.dataRow == null) {
        allOk = this.readChildRow(errors);
      }
      while (allOk) {
        if (this.keyIndexes != null) {
          /*
           * check whether this row is for this parent
           */
          int cmp = this.keyValue.compareToIgnoreCase(keyToMatch);
          if (cmp > 0) {
            /*
             * we have moved ahead of the parent
             */
            return false;
          }
          if (cmp < 0) {

        	  actionLogger.info(
                "Ignoring a row in child file "
                    + this.getFileName()
                    + " with key "
                    + this.keyValue
                    + " as there is no parent row for this.");

            allOk = this.readChildRow(errors);
            continue;
          }
        }
        /*
         * Ok, this child for the right parent.
         */
        for (int i = 0; i < this.fieldNames.length; i++) {
          ctxt.setValue(this.fieldNames[i], this.dataRow[i]);
        }
        /*
         * important to empty the cache once it is used
         */
        this.dataRow = null;
        /*
         * Are there further conditions to process this?
         */
        if (this.okToProceed(ctxt)) {
          return true;
        }

        actionLogger.info("Ignoring a child row that failed qualifying condition.");

        allOk = this.readChildRow(errors);
      }
      /*
       * we reach here only if there are no more rows. But there is no way
       * we can tell the caller about it
       */
      return false;
    }

    /*
     * check we can process this row based on conditionToProcess
     */
    private boolean okToProceed(ServiceContext ctxt) {
      Expression condition = InputFile.this.conditionToProcess;
      if (condition == null) {
        return true;
      }
      Value val;
      try {
        val = condition.evaluate(ctxt);
      } catch (InvalidOperationException e) {
        throw new ApplicationError(
            e,
            "Input file is using conditionToProcess="
                + condition
                + " but this expression probably has improper data types/operators.");
      }
      return Value.intepretAsBoolean(val);
    }

    private boolean readChildRow(List<FormattedMessage> errors)
        throws IOException, InvalidRowException {
      String rowText = this.readLine();
      if (rowText == null) {
        return false;
      }
      this.dataRow = this.record.extractFromFlatRow(rowText, InputFile.this.dataFormat, errors);
      if (this.dataRow == null) {

    	  actionLogger.info(
            "Validation errors found during extracting a row from flat file using record "
                + this.record.getQualifiedName());

        throw new InvalidRowException(rowText + "   is an invalid input row.");
      }
      /*
       * set key value
       */
      this.keyValue = "";
      for (int i = 0; i < this.keyIndexes.length; i++) {
        Value value = this.dataRow[this.keyIndexes[i]];
        if (Value.isNull(value)) {
          errors.add(
              new FormattedMessage(
                  "missingKeyColumn",
                  MessageType.ERROR,
                  "value for link field "
                      + InputFile.this.linkFieldsInThisRow[i]
                      + " is missing in a row in file "
                      + this.getFileName()));
          throw new InvalidRowException("Validation errors found in input file");
        }
        this.keyValue += value.toString();
      }
      return true;
    }

    @Override
    public void closeShop(ServiceContext ctx) {
      try {
        this.reader.close();
      } catch (Exception ignore) {
        //
      }
      if (this.newFile != null) {
        this.realFile.renameTo(this.newFile);
      } else if (InputFile.this.deleteFile) {
        this.realFile.delete();
      }
    }

    @Override
    public boolean possiblyMultipleRowsPerParent() {
      return InputFile.this.linkFieldsInParentRow != null;
    }
  }
}
