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

import org.simplity.aggr.Aggregator;
import org.simplity.aggr.AggregatorWorker;
import org.simplity.kernel.Application;
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
public class FileProcessor {
	/**
	 * if the rows are from a SQL
	 */
	String inputSql;

	/**
	 * if driver is an input file
	 */
	FlatFile inputFile;

	/**
	 * if a row is to be written out to a file
	 */
	FlatFile outputFile;

	/**
	 * actions to be taken for each row before processing child rows, Note that
	 * the action to be taken for the child rows is part of childProcssor
	 */
	Action actionBeforeChildren;

	/**
	 * are we accumulating/aggregating?. Note that aggregators accumulate for
	 * all rows for a given parent row in case of a child rows, but accumulate
	 * for all rows for the primary file
	 */
	Aggregator[] aggregators;

	/**
	 * any associated files that need to be read along with the primary input
	 * file
	 */
	FileProcessor[] childProcessors;

	/**
	 * action to be taken after processing child rows
	 */
	Action actionAfterChildren;

	/**
	 * @param service
	 */
	public void getReady(Service service) {
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
			for (FileProcessor child : this.childProcessors) {
				child.getReady(service);
			}
		}
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
	 * this is the control-entry into the maze of processing. But it all starts
	 * from here.
	 *
	 * @param file
	 * @param boss
	 *            if this is the primary-driver processor, then the batch
	 *            Processor, else null
	 * @param dbDriver
	 * @param ctx
	 * @return number of rows processed
	 * @throws InvalidRowException
	 *             in case the input row fails data-type validation
	 * @throws Exception
	 *             any other error
	 */
	public int process(File file, BatchProcessor.Worker boss, DbDriver dbDriver, ServiceContext ctx)
			throws InvalidRowException, Exception {
		Worker worker = this.getWorker(boss, dbDriver, ctx);
		try {
			worker.openShop(file);
			return worker.processAll();
		} finally {
			worker.closeShop();
		}
	}

	Worker getWorker(BatchProcessor.Worker boss, DbDriver dbDriver, ServiceContext ctx) {
		return new Worker(boss, dbDriver, ctx);
	}

	Worker getWorker(DbDriver dbDriver, ServiceContext ctx) {
		return new Worker(null, dbDriver, ctx);
	}

	class Worker implements DbRowProcessor {
		/*
		 * set by constructor
		 */
		BatchProcessor.Worker boss;
		private final DbDriver dbDriver;
		private final ServiceContext ctx;

		/*
		 * set at openShop()
		 */
		private Sql sql;
		private Worker[] workers;
		private AggregatorWorker[] aggWorkers;
		private FlatFile.InputWorker inFileWorker;
		private FlatFile.OutputWorker outFileWorker;

		/**
		 * instantiate with core(non-state) attributes
		 *
		 * @param jmsConnector
		 * @param userTransaction
		 * @param dbDriver
		 * @param ctx
		 */
		protected Worker(BatchProcessor.Worker boss, DbDriver dbDriver, ServiceContext ctx) {
			this.boss = boss;
			this.dbDriver = dbDriver;
			this.ctx = ctx;
		}

		/**
		 * get all required resources, and workers. This is called for the
		 * primary file processor (not a child processor) boss is non-null here.
		 *
		 * @throws IOException
		 */
		void openShop(File file) throws IOException {
			if (file != null) {
				this.inFileWorker = FileProcessor.this.inputFile.getInputWorker();
				this.inFileWorker.openShop(this.boss.rootFolder, file, this.ctx);
			}
			this.openShopCommon(this.boss.rootFolder);
		}

		/**
		 * initialize and get ready for repeated invocation. Any resource can be
		 * created and kept, because a closeShop() is guaranteed by the caller
		 * to this method
		 *
		 * @throws IOException
		 */
		private void openShop(String rootFolder, String parentFileName) throws IOException {
			FlatFile ff = FileProcessor.this.inputFile;
			if (ff != null) {
				this.inFileWorker = ff.getInputWorker();
				this.inFileWorker.openShop(rootFolder, parentFileName, this.ctx);
			}
			this.openShopCommon(rootFolder);
		}

		private void openShopCommon(String rootFolder) throws IOException {
			/*
			 * ensure input file or sql
			 */
			if (this.inFileWorker == null) {
				this.sql = ComponentManager.getSql(FileProcessor.this.inputSql);
			}
			/*
			 * what about output file?
			 */
			FlatFile ff = FileProcessor.this.outputFile;
			String refFileName = null;
			if (this.inFileWorker != null) {
				refFileName = this.inFileWorker.getFileName();
			}
			if (ff != null) {
				this.outFileWorker = ff.getOutputWorker();
				this.outFileWorker.openShop(rootFolder, refFileName, this.ctx);
			}
			/*
			 * aggregators?
			 */
			Aggregator[] ags = FileProcessor.this.aggregators;
			if (ags != null && ags.length > 0) {
				this.aggWorkers = new AggregatorWorker[ags.length];
				for (int i = 0; i < ags.length; i++) {
					this.aggWorkers[i] = ags[i].getWorker();
				}
			}

			/*
			 * child processors?
			 */
			FileProcessor[] prs = FileProcessor.this.childProcessors;
			if (prs != null && prs.length > 0) {
				this.workers = new Worker[prs.length];
				for (int i = 0; i < prs.length; i++) {
					Worker worker = prs[i].getWorker(this.dbDriver, this.ctx);
					worker.openShop(rootFolder, refFileName);
					this.workers[i] = worker;
				}
			}
		}

		void closeShop() {
			if (this.inFileWorker != null) {
				this.inFileWorker.closeShop();
			}
			if (this.outFileWorker != null) {
				this.outFileWorker.closeShop();
			}
			if (this.workers != null) {
				for (Worker worker : this.workers) {
					if(worker != null){
					worker.closeShop();
					}
				}
			}
		}

		/**
		 * this is the method for the primary file processor, and not child-ones
		 *
		 * @return
		 * @throws Exception
		 */
		int processAll() throws Exception {
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
			List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
			while (true) {
				errors.clear();
				try {
					/*
					 * read a row into serviceContext
					 */
					if (this.inFileWorker.read(errors) == false) {
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
					throw new InvalidRowException();
				}

				this.commonProcess();
				this.writeAggregators();
			}
			return 0;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.simplity.kernel.db.DbRowProcessor#processRow(java.lang.String[],
		 * org.simplity.kernel.value.Value[])
		 */
		@Override
		public boolean processRow(String[] outputNames, Value[] values) {
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
				this.commonProcess();
				this.writeAggregators();
			} catch (Exception e) {
				Application.reportApplicationError(null, e);
				return false;
			}
			return true;
		}

		/**
		 * process one row of this file/sql. Common for main as well as child
		 * files
		 *
		 * @throws Exception
		 */
		private void commonProcess() throws Exception {
			if (this.boss != null) {
				this.boss.beginTrans();
				this.ctx.resetMessages();
			}
			Exception exception = null;
			try {
				Action action = FileProcessor.this.actionBeforeChildren;
				if (action != null) {
					action.act(this.ctx, this.dbDriver);
				}

				this.accumulateAggregators();

				if (this.workers != null) {
					for (Worker worker : this.workers) {
						worker.processAParentRow();
					}
				}
				action = FileProcessor.this.actionAfterChildren;
				if (action != null) {
					action.act(this.ctx, this.dbDriver);
				}
				if (this.outFileWorker != null) {
					this.outFileWorker.writeOut();
				}

			} catch (ApplicationError e) {
				exception = e;
			} catch (Exception e) {
				exception = new ApplicationError(e, "Exception during execution of service. ");
			}
			if (this.boss == null) {
				if (exception != null) {
					throw exception;
				}
			} else {
				this.boss.endTrans(exception, this.dbDriver);
			}
		}

		/**
		 * read rows from child-file for matching key value from parent row.
		 * We assume that the file is sorted.
		 * @throws Exception
		 */
		private void processAParentRow() throws Exception {
			if (this.sql != null) {
				this.sql.processRows(this.ctx, this.dbDriver, this);
				return;
			}

			List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
			if(this.inFileWorker.toBeMatched() == false){
				/*
				 * single row only
				 */
				boolean ok = this.inFileWorker.read(errors);
				if(!ok){
					Tracer.trace("No rows in file child file");
					return;
				}
				if (errors.size() > 0) {
					this.ctx.addMessages(errors);
					throw new InvalidRowException();
				}
				this.commonProcess();
				this.writeAggregators();
				return;
			}
			/*
			 * possibly more than one rows in this file for  parent row
			 */
			String parentKey = this.inFileWorker.getParentKey(errors);
			if (errors.size() > 0) {
				this.ctx.addMessages(errors);
				throw new InvalidRowException();
			}
			while (true) {
				errors.clear();
				try {
					if (this.inFileWorker.readChildRow(errors, parentKey) == false) {
						break;
					}
				} catch (IOException e) {
					throw new ApplicationError(e, "Error while processing batch files");
				}
				if (errors.size() > 0) {
					this.ctx.addMessages(errors);
					throw new InvalidRowException();
				}
				this.commonProcess();
			}
			this.writeAggregators();
		}

		private void accumulateAggregators() {
			if (this.aggWorkers == null) {
				return;
			}
			for (AggregatorWorker agw : this.aggWorkers) {
				agw.accumulate(this.ctx, this.ctx);
			}
		}

		private void writeAggregators() {
			if (this.aggWorkers == null) {
				return;
			}
			for (AggregatorWorker agw : this.aggWorkers) {
				agw.writeOut(this.ctx, this.ctx);
			}
		}
	}
}
