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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.simplity.aggr.AggregationWorker;
import org.simplity.aggr.AggregatorInterface;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.db.DbRowProcessor;
import org.simplity.kernel.db.Sql;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * Data structure that keeps meta data about a flat-file
 *
 * @author simplity.org
 *
 */
public class BatchRowProcessor {
	/**
	 * if the rows are from a SQL
	 */
	String inputSql;

	/**
	 * if driver is an input file. Not valid if inputSql is specified
	 */
	InputFile inputFile;

	/**
	 * if a row is to be written out to a file for each row at this level
	 */
	OutputFile outputFile;

	/**
	 * actions to be taken for each row before processing child rows, if any.
	 */
	Action actionBeforeChildren;

	/**
	 * are we accumulating/aggregating?. We can accumulate, say sum of a number,
	 * for all rows in this file/sql for each row of the parent.
	 * For the driver(primary) processor, there is no parent-row, and hence we
	 * accumulate across all rows in the file/sql.
	 */
	AggregatorInterface[] aggregators;

	/**
	 * If we have to process rows from one or more files/sqls for each row in
	 * this file/sql.
	 */
	BatchRowProcessor[] childProcessors;

	/**
	 * action to be taken after processing child rows. This action is executed
	 * even if there were no child rows.
	 */
	Action actionAfterChildren;

	/**
	 * @param service
	 */
	public void getReady(Service service) {
		if (this.inputSql == null) {
			if (this.inputFile == null) {
				this.throwError();
			}
		} else {
			if (this.inputFile != null) {
				this.throwError();
			}
		}
		if (this.inputFile != null) {
			this.inputFile.getReady(service);
		}

		if (this.outputFile != null) {
			this.outputFile.getReady(service);
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
		throw new ApplicationError("A file processor should specify either a sql, or a file for input, but not both");
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
	 * main method invoked by BatchProcessor.Worker to process a given file/sql.
	 * Since this processing require state-full objects to call back and forth,
	 * we delegate the work to a worker instance
	 *
	 * @param file
	 *            input file. Or null if the processor uses a sql as driver
	 * @param batchWorker
	 *            pointer to BatchProcessor.Worker to access the required
	 *            resources
	 * @param dbDriver
	 *            to be used for all db work by transaction processing. This
	 *            driver is NOT used by exception processors
	 * @param ctx
	 * @return number of rows processed
	 * @throws InvalidRowException
	 *             in case the input row fails data-type validation
	 * @throws Exception
	 *             any other error
	 */
	int process(File file, BatchProcessor.Worker batchWorker, DbDriver dbDriver, ServiceContext ctx)
			throws InvalidRowException, Exception {
		DriverProcess driver = this.getDriverProcess(dbDriver, ctx);
		try {
			driver.openShop(batchWorker, batchWorker.inFolderName, batchWorker.outFolderName, null, file, ctx);
			return driver.callFromParent();
		} finally {
			driver.closeShop();
		}
	}

	/**
	 * get an instance of worker to take care of one run. invoked when this is
	 * the driver-processor
	 *
	 */
	protected DriverProcess getDriverProcess(DbDriver dbDriver, ServiceContext ctx) {
		return new DriverProcess(dbDriver, ctx);
	}

	/**
	 * recursively invoked by a parent DriverProcess
	 *
	 */
	protected ChildProcess getChildProcess(DbDriver dbDriver, ServiceContext ctx) {
		return new ChildProcess(dbDriver, ctx);
	}

	/**
	 * common tasks between a DriverProcess and ChildProcess have been
	 * carved into this abstract class.
	 *
	 * @author simplity.org
	 *
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
		protected InputFile.Worker inFileWorker;
		protected OutputFile.Worker outFileWorker;

		/**
		 * very tricky design because of call-back design. In case a child-level
		 * row processing throws an exception, we can not propagate it back
		 * properly. Hence we keep track of that with this state-variable
		 */
		protected Exception excpetionOnCallBack;

		/**
		 * instantiate with core(non-state) attributes
		 *
		 */
		protected AbstractProcess(DbDriver dbDriver, ServiceContext ctx) {
			this.dbDriver = dbDriver;
			this.ctx = ctx;
		}

		/**
		 * get all required resources, and workers.
		 *
		 * @throws IOException
		 */
		protected void openShop(BatchProcessor.Worker batchWorker, String folderIn, String folderOut, String parentFileName,
				File file, ServiceContext ctxt) throws IOException {

			this.setInputFile(batchWorker, folderIn, parentFileName, file, ctxt);

			String inputFileName = null;
			/*
			 * if it is not input file, it has to be sql
			 */
			if (this.inFileWorker == null) {
				this.sql = ComponentManager.getSql(BatchRowProcessor.this.inputSql);
			} else {
				inputFileName = this.inFileWorker.getFileName();
			}
			/*
			 * what about output file?
			 */
			OutputFile ff = BatchRowProcessor.this.outputFile;

			if (ff != null) {
				this.outFileWorker = ff.getWorker();
				this.outFileWorker.openShop(folderOut, inputFileName, this.ctx);
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
		 * set input file based on the actual file already identified by the
		 * caller. Relevant when this is a driver (primary) processor
		 */
		protected abstract void setInputFile(BatchProcessor.Worker boss, String folderIn, String parentFileName,
				File file, ServiceContext ctxt) throws IOException;

		/**
		 * release all resource
		 */
		void closeShop() {
			if (this.inFileWorker != null) {
				this.inFileWorker.closeShop();
			}
			if (this.outFileWorker != null) {
				this.outFileWorker.closeShop();
			}
			if (this.children != null) {
				for (AbstractProcess worker : this.children) {
					worker.closeShop();
				}
			}
		}

		/**
		 * Entry point for the process to do its job worker to do its job.
		 */
		protected abstract int callFromParent() throws Exception;

		/**
		 * when sql is used, this is how we get called back on each row in the result
		 */
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

			if (this.children != null) {
				for (ChildProcess child : this.children) {
					child.callFromParent();
				}
			}
			action = BatchRowProcessor.this.actionAfterChildren;
			if (action != null) {
				action.act(this.ctx, this.dbDriver);
			}
			if (this.outFileWorker != null) {
				this.outFileWorker.writeOut();
			}

		}

		/**
		 * if aggregators are specified, call each of them to accumulate
		 */
		protected void accumulateAggregators() {
			if (this.aggWorkers == null) {
				return;
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
		private BatchProcessor.Worker batchWorker;

		/**
		 * @param dbDriver
		 * @param ctx
		 */
		protected DriverProcess(DbDriver dbDriver, ServiceContext ctx) {
			super(dbDriver, ctx);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.simplity.tp.FileProcessor.AbstractWorker#openSpecificShop(org.
		 * simplity.tp.BatchProcessor.Worker, java.lang.String,
		 * java.lang.String, java.io.File, org.simplity.service.ServiceContext)
		 */
		@Override
		protected void setInputFile(BatchProcessor.Worker boss, String folderIn, String parentFileName, File file,
				ServiceContext ctxt) throws IOException {
			this.batchWorker = boss;
			if (file != null) {
				this.inFileWorker = BatchRowProcessor.this.inputFile.getWorker();
				this.inFileWorker.openShop(folderIn, file, ctxt);
			}
		}

		/**
		 * recursive call from a parent-processor. We have to process rows in
		 * this file/sql for current parent row.
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
			 * OK. file processing that is more functional-like
			 */
			int nbrRows = 0;
			List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
			while (true) {
				errors.clear();
				try {
					/*
					 * read a row into serviceContext
					 */
					if (this.inFileWorker.extractToCtx(errors) == false) {
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
					this.batchWorker.errorOnInputValidation(new InvalidRowException());
				}else{
					this.doOneTransaction();
				}
			}
			return nbrRows;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.simplity.kernel.db.DbRowProcessor#processRow(java.lang.String[],
		 * org.simplity.kernel.value.Value[])
		 */
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
			return true;
		}

		/**
		 * process one row under a transaction
		 */
		private void doOneTransaction() {
			this.batchWorker.beginTrans();
			this.ctx.resetMessages();
			Exception exception = null;
			try {
				this.processARow();
				this.writeAggregators();
			} catch (Exception e) {
				exception = e;
			}
			this.batchWorker.endTrans(exception, this.dbDriver);
		}
	}

	/**
	 * child processor processes rows for a given parent row on each call.
	 *
	 * @author simplity.org
	 *
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
		protected void setInputFile(org.simplity.tp.BatchProcessor.Worker boss, String folderIn,
				String parentFileName, File file, ServiceContext ctxt) throws IOException {
			InputFile inf = BatchRowProcessor.this.inputFile;
			if (inf != null) {
				this.inFileWorker = inf.getWorker();
				this.inFileWorker.openShop(folderIn, parentFileName, ctxt);
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
		 * recursive call from a parent-processor. We have to process rows in
		 * this file/sql for current parent row.
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
			if (this.inFileWorker.toBeMatched() == false) {
				/*
				 * single row only
				 */
				boolean ok = this.inFileWorker.extractToCtx(errors);
				if (!ok) {
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
			String parentKey = this.inFileWorker.getParentKey(errors);
			if (errors.size() > 0) {
				this.ctx.addMessages(errors);
				throw new InvalidRowException();
			}
			int nbr = 0;
			while (true) {
				errors.clear();
				try {
					if (this.inFileWorker.extractForMatchingKey(errors, parentKey) == false) {
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
