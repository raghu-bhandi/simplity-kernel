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

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;

import org.simplity.aggr.AggregationWorker;
import org.simplity.aggr.AggregatorInterface;
import org.simplity.jms.JmsDestination;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.db.DbRowProcessor;
import org.simplity.kernel.db.Sql;
import org.simplity.kernel.expr.Expression;
import org.simplity.kernel.expr.InvalidOperationException;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * Data structure that keeps meta data about a flat-file
 *
 * @author simplity.org
 */
public class BatchRowProcessor {
  static final Logger logger = Logger.getLogger(BatchRowProcessor.class.getName());

  /** if the rows are from a SQL */
  String inputSql;

  /** if driver is an input file. Not valid if inputSql is specified */
  InputFile inputFile;

  /** if a row is to be written out to a file for each row at this level */
  OutputFile outputFile;

  /** actions to be taken for each row before processing child rows, if any. */
  Action actionBeforeChildren;

  /**
   * are we accumulating/aggregating?. We can accumulate, say sum of a number, for all rows in this
   * file/sql for each row of the parent. For the driver(primary) processor, there is no parent-row,
   * and hence we accumulate across all rows in the file/sql.
   */
  AggregatorInterface[] aggregators;

  /** If we have to process rows from one or more files/sqls for each row in this file/sql. */
  BatchRowProcessor[] childProcessors;

  /**
   * action to be taken after processing child rows. This action is executed even if there were no
   * child rows.
   */
  Action actionAfterChildren;
  /** if the aggregation process is conditional. */
  Expression conditionToAggregate;

  /** you may supply a custom class that writes the output */
  String customOutputClassName;

  /** you may provide a custom class that serves as a driver input */
  String customInputClassName;

  /** queue from which to consume requests to be processed as requests */
  JmsDestination inputDestination;
  /** optional queue on which responses to be sent on */
  JmsDestination outputDestination;

  /** @param service */
  public void getReady(Service service) {
    int nbrInputChannels = 0;
    if (this.inputFile != null) {
      this.inputFile.getReady(service);
      nbrInputChannels++;
    }

    if (this.inputDestination != null) {
      this.inputDestination.getReady();
      nbrInputChannels++;
    }
    if (this.inputSql != null) {
      nbrInputChannels++;
    }
    if (this.customInputClassName != null) {
      nbrInputChannels++;
    }
    if (nbrInputChannels != 1) {
      this.throwError();
    }

    if (this.outputFile != null) {
      this.outputFile.getReady(service);
    }

    if (this.outputDestination != null) {
      this.outputDestination.getReady();
    }

    if (this.actionBeforeChildren != null) {
      this.actionBeforeChildren.getReady(0, service);
    }

    if (this.actionAfterChildren != null) {
      this.actionAfterChildren.getReady(0, service);
    }
    if (this.childProcessors != null) {
      for (BatchRowProcessor child : this.childProcessors) {
        child.getReady(service);
      }
    }
  }

  private void throwError() {
    throw new ApplicationError(
        "A file processor should specify one and only one way to get input rows : sql, file, jms queue or custom input");
  }

  /**
   * @param vtx
   * @param service
   * @return number of errors
   */
  public int validate(ValidationContext vtx, Service service) {
    return 0;
  }

  /**
   * main method invoked by BatchProcessor.Worker to process a given file/sql. Since this processing
   * require state-full objects to call back and forth, we delegate the work to a worker instance
   *
   * @param file input file. Or null if the processor uses a sql as driver
   * @param batchWorker pointer to BatchProcessor.Worker to access the required resources
   * @param dbDriver to be used for all db work by transaction processing. This driver is NOT used
   *     by exception processors
   * @param ctx
   * @param interruptible
   * @return number of rows processed
   * @throws InvalidRowException in case the input row fails data-type validation
   * @throws Exception any other error
   */
  int process(
      File file,
      BatchProcessor.Worker batchWorker,
      DbDriver dbDriver,
      ServiceContext ctx,
      boolean interruptible)
      throws InvalidRowException, Exception {
    DriverProcess driver = this.getDriverProcess(dbDriver, ctx, interruptible);
    try {
      driver.openShop(
          batchWorker, batchWorker.inFolderName, batchWorker.outFolderName, null, file, ctx);
      return driver.callFromParent();
    } finally {
      driver.closeShop();
    }
  }

  /**
   * get an instance of worker to take care of one run. invoked when this is the driver-processor
   */
  protected DriverProcess getDriverProcess(
      DbDriver dbDriver, ServiceContext ctx, boolean inturrutible) {
    return new DriverProcess(dbDriver, ctx, inturrutible);
  }

  /** recursively invoked by a parent DriverProcess */
  protected ChildProcess getChildProcess(DbDriver dbDriver, ServiceContext ctx) {
    return new ChildProcess(dbDriver, ctx);
  }

  /**
   * common tasks between a DriverProcess and ChildProcess have been carved into this abstract
   * class.
   *
   * @author simplity.org
   */
  abstract class AbstractProcess implements DbRowProcessor {
    /*
     * set by constructor
     */
    protected final DbDriver dbDriver;
    protected final ServiceContext ctx;

    /*
     * set at openShop() time
     */
    protected Sql sql;
    protected ChildProcess[] children;
    protected AggregationWorker[] aggWorkers;
    protected BatchInput batchInput;
    protected BatchOutput fileOutput;
    protected BatchOutput customOutput;
    protected BatchOutput jmsOutput;

    /**
     * very tricky design because of call-back design. In case a child-level row processing throws
     * an exception, we can not propagate it back properly. Hence we keep track of that with this
     * state-variable
     */
    protected Exception excpetionOnCallBack;

    /** instantiate with core(non-state) attributes */
    protected AbstractProcess(DbDriver dbDriver, ServiceContext ctx) {
      this.dbDriver = dbDriver;
      this.ctx = ctx;
    }

    /**
     * get all required resources, and workers.
     *
     * @throws IOException
     * @throws JMSException
     */
    protected void openShop(
        BatchProcessor.Worker batchWorker,
        String folderIn,
        String folderOut,
        String parentFileName,
        File file,
        ServiceContext ctxt)
        throws IOException, JMSException {

      this.setInputFile(batchWorker, folderIn, parentFileName, file, ctxt);

      String inputFileName = null;
      /*
       * what is our input?. We haev to have only one way of input
       */
      if (this.batchInput != null) {
        inputFileName = this.batchInput.getFileName();
      } else if (BatchRowProcessor.this.inputSql != null) {
        this.sql = ComponentManager.getSql(BatchRowProcessor.this.inputSql);
      } else if (BatchRowProcessor.this.inputDestination != null) {
        this.batchInput = BatchRowProcessor.this.inputDestination.getBatchInput(ctxt);
        this.batchInput.openShop(ctxt);
      } else if (BatchRowProcessor.this.customInputClassName != null) {
        try {
          this.batchInput =
              (BatchInput) Class.forName(BatchRowProcessor.this.customInputClassName).newInstance();
          this.batchInput.openShop(ctxt);
        } catch (Exception e) {
          throw new ApplicationError(
              e,
              "Error while using "
                  + BatchRowProcessor.this.customInputClassName
                  + " to get an instance of BatchInput");
        }
      } else {
        throw new ApplicationError("Batch row processor has no input specified.");
      }
      /*
       * what about output ? we can have more than one output channels
       */
      OutputFile ff = BatchRowProcessor.this.outputFile;

      if (ff != null) {
        OutputFile.Worker worker = ff.getWorker();
        worker.setFileName(folderOut, inputFileName, this.ctx);
        worker.openShop(this.ctx);
        this.fileOutput = worker;
      }
      String clsName = BatchRowProcessor.this.customOutputClassName;
      if (clsName != null) {
        try {
          this.customOutput = (BatchOutput) Class.forName(clsName).newInstance();
          this.customOutput.openShop(ctxt);
        } catch (Exception e) {
          throw new ApplicationError(
              e, "Error while using " + clsName + " to get an instance of BatchOutput");
        }
      }
      JmsDestination outq = BatchRowProcessor.this.outputDestination;
      if (outq != null) {
        try {
          this.jmsOutput = outq.getBatchOutput(ctxt);
          this.jmsOutput.openShop(ctxt);
        } catch (Exception e) {
          throw new ApplicationError(
              e,
              "Error while using " + (String) outq.getName() + " to get an instance of JMSOutput");
        }
      }
      /*
       * aggregators?
       */
      AggregatorInterface[] ags = BatchRowProcessor.this.aggregators;
      if (ags != null && ags.length > 0) {
        this.aggWorkers = new AggregationWorker[ags.length];
        for (int i = 0; i < ags.length; i++) {
          AggregationWorker ag = ags[i].getWorker();
          ag.init(ctxt);
          this.aggWorkers[i] = ag;
        }
      }

      /*
       * child processors?
       */
      BatchRowProcessor[] prs = BatchRowProcessor.this.childProcessors;
      if (prs != null && prs.length > 0) {
        this.children = new ChildProcess[prs.length];
        for (int i = 0; i < prs.length; i++) {
          BatchRowProcessor fp = prs[i];
          ChildProcess process = fp.getChildProcess(this.dbDriver, this.ctx);
          process.openShop(null, folderIn, folderOut, inputFileName, null, this.ctx);
          this.children[i] = process;
        }
      }
    }

    /**
     * set input file based on the actual file already identified by the caller. Relevant when this
     * is a driver (primary) processor
     */
    protected abstract void setInputFile(
        BatchProcessor.Worker boss,
        String folderIn,
        String parentFileName,
        File file,
        ServiceContext ctxt)
        throws IOException;

    /** release all resource */
    void closeShop() {
      if (this.batchInput != null) {
        this.batchInput.closeShop(this.ctx);
      }
      if (this.customOutput != null) {
        this.customOutput.closeShop(this.ctx);
      }
      if (this.fileOutput != null) {
        this.fileOutput.closeShop(this.ctx);
      }
      if (this.jmsOutput != null) {
        this.jmsOutput.closeShop(this.ctx);
      }
      if (this.children != null) {
        for (AbstractProcess worker : this.children) {
          worker.closeShop();
        }
      }
    }

    /** Entry point for the process to do its job worker to do its job. */
    protected abstract int callFromParent() throws Exception;

    /** when sql is used, this is how we get called back on each row in the result */
    @Override
    public abstract boolean callBackOnDbRow(String[] outputNames, Value[] values);

    /**
     * process one row of this file/sql.
     *
     * @throws Exception
     */
    protected void processARow() throws Exception {
      Action action = BatchRowProcessor.this.actionBeforeChildren;
      if (action != null) {
        action.act(this.ctx, this.dbDriver);
      }

      this.accumulateAggregators();

      if (!ctx.getBooleanValue(BatchProcessor.EOF_FIELD_IN_CTX)) {
        //process if not eof
        if (this.children != null) {
          for (ChildProcess child : this.children) {
            child.callFromParent();
          }
        }
      }

      action = BatchRowProcessor.this.actionAfterChildren;
      if (action != null) {
        action.act(this.ctx, this.dbDriver);
      }
      if (this.fileOutput != null) {
        this.fileOutput.outputARow(this.ctx);
      }

      if (this.jmsOutput != null) {
        this.jmsOutput.outputARow(this.ctx);
      }
    }

    /** if aggregators are specified, call each of them to accumulate */
    protected void accumulateAggregators() {
      if (this.aggWorkers == null) {
        return;
      }
      Expression condition = BatchRowProcessor.this.conditionToAggregate;
      if (condition != null) {
        try {
          Value value = condition.evaluate(this.ctx);
          if (Value.intepretAsBoolean(value) == false) {
            return;
          }
        } catch (InvalidOperationException e) {
          throw new ApplicationError(e, "Error while evaluating expression " + condition);
        }
      }
      for (AggregationWorker agw : this.aggWorkers) {
        agw.accumulate(this.ctx, this.ctx);
      }
    }

    /**
     * if aggregators are specified, call each of them to writ-out the resultant to service context
     */
    protected void writeAggregators() {
      if (this.aggWorkers == null) {
        return;
      }
      for (AggregationWorker agw : this.aggWorkers) {
        agw.writeOut(this.ctx, this.ctx);
      }
    }

    /**
     * if aggregators are specified, call each of them to writ-out the resultant to service context
     */
    protected void initAggregators() {
      if (this.aggWorkers == null) {
        return;
      }
      for (AggregationWorker agw : this.aggWorkers) {
        agw.init(this.ctx);
      }
    }
  }

  protected class DriverProcess extends AbstractProcess {
    private static final String VALIDATION_ERROR = "Input row has validation errors.";
    private BatchProcessor.Worker batchWorker;
    private boolean isInterruptible;

    /**
     * @param dbDriver
     * @param ctx
     */
    protected DriverProcess(DbDriver dbDriver, ServiceContext ctx) {
      super(dbDriver, ctx);
    }

    /**
     * @param dbDriver
     * @param ctx
     */
    protected DriverProcess(DbDriver dbDriver, ServiceContext ctx, boolean interruptible) {
      super(dbDriver, ctx);
      this.isInterruptible = interruptible;
    }

    @Override
    protected void setInputFile(
        BatchProcessor.Worker boss,
        String folderIn,
        String parentFileName,
        File file,
        ServiceContext ctxt)
        throws IOException {
      this.batchWorker = boss;
      if (file != null) {
        InputFile.Worker inf = BatchRowProcessor.this.inputFile.getWorker();
        inf.setInputFile(folderIn, file);
        inf.openShop(ctxt);
        this.batchInput = inf;
      }
    }

    /**
     * recursive call from a parent-processor. We have to process rows in this file/sql for current
     * parent row.
     *
     * @return
     * @throws Exception
     */
    @Override
    protected int callFromParent() throws Exception {
      if (this.sql != null) {
        /*
         * tricky design. looks like a neat return, but it is not.
         * following method will call us back on processRow() for each
         * row.
         *
         * So, read this statement as "call prcoessRow() for each row in
         * result set of this sql
         */
        return this.sql.processRows(this.ctx, this.dbDriver, this);
      }

      /*
       * use batchInput to get input rows for processing
       */
      int nbrRows = 0;
      List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
      while (true) {
        errors.clear();
        try {
          /*
           * read a row into serviceContext
           */
          if (this.batchInput.inputARow(errors, this.ctx) == false) {
            if (this.batchWorker != null && this.batchWorker.doEof()) {
              this.ctx.setBooleanValue(BatchProcessor.EOF_FIELD_IN_CTX, true);
              this.processARow();
            }
            /*
             * no more rows
             */
            break;
          }
        } catch (IOException e) {
          throw new ApplicationError(e, "Error while processing batch files");
        }
        if (errors.size() > 0) {

          this.ctx.addMessages(errors);
          this.batchWorker.errorOnInputValidation(new InvalidRowException(VALIDATION_ERROR));
        } else {
          this.doOneTransaction();
        }
        if (this.isInterruptible && Thread.interrupted()) {

          logger.log(
              Level.INFO, "Detected an interrupt. Going to stop processing rows from sql output");
          Tracer.trace("Detected an interrupt. Going to stop processing rows from sql output");
          Thread.currentThread().interrupt();
          break;
        }
      }
      return nbrRows;
    }

    @Override
    public boolean callBackOnDbRow(String[] outputNames, Value[] values) {
      /*
       * this is the callback from sql.processRows for each row in the sql
       * result. We should do the same thing that we would do after
       * reading each row in the input file
       *
       */
      for (int i = 0; i < values.length; i++) {
        this.ctx.setValue(outputNames[i], values[i]);
      }
      this.doOneTransaction();

      if (this.isInterruptible && Thread.interrupted()) {

        logger.log(
            Level.INFO, "Detected an interrupt. Going to stop processing rows from sql output");
        Tracer.trace("Detected an interrupt. Going to stop processing rows from sql output");
        Thread.currentThread().interrupt();
        return false;
      }
      return true;
    }

    /** process one row under a transaction */
    private void doOneTransaction() {
      this.batchWorker.beginTrans();
      this.ctx.resetMessages();
      Exception exception = null;
      try {
        this.processARow();
        this.writeAggregators();
      } catch (Exception e) {
        exception = e;
        this.ctx.addMessage(
            Messages.ERROR,
            "Error while processing a row from batch driver input. " + e.getMessage());
      }
      this.batchWorker.endTrans(exception, this.dbDriver);
    }
  }

  /**
   * child processor processes rows for a given parent row on each call.
   *
   * @author simplity.org
   */
  protected class ChildProcess extends AbstractProcess {

    /**
     * @param dbDriver
     * @param ctx
     */
    protected ChildProcess(DbDriver dbDriver, ServiceContext ctx) {
      super(dbDriver, ctx);
    }

    @Override
    protected void setInputFile(
        org.simplity.tp.BatchProcessor.Worker boss,
        String folderIn,
        String parentFileName,
        File file,
        ServiceContext ctxt)
        throws IOException {
      InputFile inf = BatchRowProcessor.this.inputFile;
      if (inf != null) {
        InputFile.Worker worker = inf.getWorker();
        worker.setInputFileName(folderIn, parentFileName, ctxt);
        worker.openShop(ctxt);
        this.batchInput = worker;
      }
    }

    @Override
    public boolean callBackOnDbRow(String[] outputNames, Value[] values) {
      /*
       * this is the callback from sql.processRows for each row in the sql
       * result. We should do the same thing that we would do after
       * reading each row in the input file
       *
       */
      for (int i = 0; i < values.length; i++) {
        this.ctx.setValue(outputNames[i], values[i]);
      }

      try {
        this.processARow();
        this.writeAggregators();
      } catch (Exception e) {
        this.excpetionOnCallBack = e;
        return false;
      }
      return true;
    }

    /**
     * recursive call from a parent-processor. We have to process rows in this file/sql for current
     * parent row.
     *
     * @throws Exception
     */
    @Override
    protected int callFromParent() throws Exception {
      if (this.sql != null) {
        this.sql.processRows(this.ctx, this.dbDriver, this);
        /*
         * was there an exception because of which we would forced sql
         * to stop?
         */
        if (this.excpetionOnCallBack != null) {
          Exception e = this.excpetionOnCallBack;
          this.excpetionOnCallBack = null;
          throw e;
        }
        return 0;
      }

      List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
      if (this.batchInput.possiblyMultipleRowsPerParent() == false) {
        /*
         * single row only
         */
        boolean ok = this.batchInput.inputARow(errors, this.ctx);
        if (!ok) {

          logger.log(Level.INFO, "No rows in file child file");
          Tracer.trace("No rows in file child file");
          return 0;
        }
        if (errors.size() > 0) {
          this.ctx.addMessages(errors);
          throw new InvalidRowException();
        }
        this.processARow();
        this.writeAggregators();
        return 1;
      }
      /*
       * possibly more than one rows in this file for a parent row
       */
      String parentKey = this.batchInput.getParentKeyValue(errors, this.ctx);
      if (errors.size() > 0) {
        this.ctx.addMessages(errors);
        throw new InvalidRowException();
      }
      int nbr = 0;
      while (true) {
        errors.clear();
        try {
          if (this.batchInput.inputARow(errors, parentKey, this.ctx) == false) {
            break;
          }
        } catch (IOException e) {
          throw new ApplicationError(e, "Error while processing batch files");
        }
        if (errors.size() > 0) {
          this.ctx.addMessages(errors);
          throw new InvalidRowException();
        }
        this.processARow();
        nbr++;
      }
      this.writeAggregators();
      return nbr;
    }
  }
}
