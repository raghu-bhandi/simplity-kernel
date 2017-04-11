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

import javax.transaction.UserTransaction;

import org.simplity.jms.JmsConnector;
import org.simplity.jms.JmsUsage;
import org.simplity.kernel.Application;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbClientInterface;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.db.MultiTransClientInterface;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.JavaAgent;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceData;

/**
 * @author simplity.org
 *
 */
public class BatchProcessor extends Action {
	private static final String FIELD_PREFIX = "$";
	private static final String FOLDER_SEP = "/";

	/**
	 * folder where input files are expected.This folder is used for all file
	 * processors. Any input file that is expected in a different folder may
	 * have the relative folder path in its name
	 *
	 */
	String inputFolder;

	/**
	 * folder where output files are to be written out.This folder is used for all file
	 * processors. Any output file that is expected in a different folder may
	 * have the relative folder path in its name
	 *
	 */
	String outputFolder;

	/**
	 * main file processor
	 */
	BatchRowProcessor batchRowProcessor;

	/**
	 * What action do we take in case the input row fails data-type validation?
	 */
	String serviceOnInvalidInput;

	/**
	 * action to be executed if the row processing generates error
	 */
	String serviceOnErrorAtRowLevel;

	/**
	 * in case this thread is interrupted, should we exit after completing the
	 * current file?
	 */
	boolean exitOnInterrupt;
	/**
	 * do we use JMS?
	 */
	JmsUsage jmsUsage;

	/**
	 * what kind of rdbms access is required?
	 */
	DbAccessType dbAccessType;

	/**
	 * Does this use a schema different from the default schema for the project?
	 */
	String schemaName;
	/**
	 * if some custom code need to make use of the actual file name, let us set a field name in context
	 */
	String setActualFileNameTo;

	/**
	 * sub-service created for the desired service
	 */
	private SubService actionOnInvalidInput;
	/**
	 * sub-service created for the desired service
	 */
	private SubService actionOnErrorAtRowLevel;

	Action getInvalidAction() {
		return this.actionOnInvalidInput;
	}

	Action getErrorAction() {
		return this.actionOnErrorAtRowLevel;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#getDataAccessType()
	 * we manage our own db access, and not depend on what the service has set
	 */
	@Override
	public DbAccessType getDataAccessType() {
		return DbAccessType.NONE;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#getReady(int)
	 */
	@Override
	public void getReady(int idx, Service service) {
		super.getReady(idx, service);
		if (this.batchRowProcessor == null) {
			throw new ApplicationError("fileProcessor is required for a BatchProcessor");
		}

		this.batchRowProcessor.getReady(service);

		if (this.inputFolder == null) {
			if (this.batchRowProcessor.inputFile != null) {
				throw new ApplicationError("Batch processor uses input files, but inputFolder is not specified");
			}
		}

		if (this.outputFolder == null) {
			if (this.batchRowProcessor.outputFile != null) {
				throw new ApplicationError("Batch processor uses output files, but outputFolder is not specified");
			}
		}

		/*
		 * create and getReady() event actions if required
		 */
		if (this.serviceOnInvalidInput != null) {
			this.actionOnInvalidInput = new SubService();
			this.actionOnInvalidInput.serviceName = this.serviceOnInvalidInput;
			this.actionOnInvalidInput.getReady(0, service);
		}
		if (this.serviceOnErrorAtRowLevel != null) {
			this.actionOnErrorAtRowLevel = new SubService();
			this.actionOnErrorAtRowLevel.serviceName = this.serviceOnErrorAtRowLevel;
			this.actionOnErrorAtRowLevel.getReady(0, service);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Block#validate(org.simplity.kernel.comp.
	 * ValidationContext, org.simplity.tp.Service)
	 */
	@Override
	public int validate(ValidationContext vtx, Service service) {
		int count = super.validate(vtx, service);
		if (this.actionOnErrorAtRowLevel != null) {
			count += this.actionOnErrorAtRowLevel.validate(vtx, service);
		}
		if (this.actionOnInvalidInput != null) {
			count += this.actionOnInvalidInput.validate(vtx, service);
		}
		count += this.batchRowProcessor.validate(vtx, service);
		return count;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#delegate(org.simplity.service.ServiceContext,
	 * org.simplity.kernel.db.DbDriver)
	 */
	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
		String inFolderName = this.translateFolderName(this.inputFolder, ctx);
		String outFolderName = this.translateFolderName(this.outputFolder, ctx);
		File[] files = null;
		if (this.batchRowProcessor.inputFile == null) {
			/*
			 * sql driven. eqvt of one null file to be processed
			 */
			files = new File[1];
			// files[0] remain as null
		} else {
			files = this.getFiles(inFolderName, ctx);
		}

		if (files == null) {
			Tracer.trace("BatchProcesser " + this.actionName + " had nothing to process.");
			return Value.VALUE_ZERO;
		}
		/*
		 * resources that need to be released without fail..
		 */
		JmsConnector jmsConnector = null;
		UserTransaction userTransaction = null;
		/*
		 * Simplity has made big issue out of db connection, and insists on not
		 * using the connection explicitly. Best practice is to use a call back
		 * object that is provided with an instance of dbDriver for a method. We
		 * use a worker for that sake
		 */

		ApplicationError exception = null;
		int nbrRows = 0;
		try {
			/*
			 * acquire resources that are needed for this service
			 */
			if (this.jmsUsage != null) {
				jmsConnector = JmsConnector.borrowMultiTransConnector(this.jmsUsage);
				ctx.setJmsSession(jmsConnector.getSession());
			}

			/*
			 * jms connector is sent to worker only if needs to be used for
			 * transaction management at each row
			 */
			JmsConnector con = null;
			if (this.jmsUsage == JmsUsage.SERVICE_MANAGED) {
				con = jmsConnector;
			}
			Worker worker = new Worker(inFolderName, outFolderName, con, userTransaction, files, ctx);
			worker.work();
			nbrRows = worker.getNbrRowsProcessed();
		} catch (ApplicationError e) {
			exception = e;
		} catch (Exception e) {
			exception = new ApplicationError(e, "Exception during execution of service. ");
		}
		/*
		 * close/return resources
		 */
		if (jmsConnector != null) {
			JmsConnector.returnConnector(jmsConnector, true);
		}
		if (exception == null) {
			return Value.newIntegerValue(nbrRows);
		}
		throw exception;
	}

	private String translateFolderName(String folder, ServiceContext ctx) {

		String folderName = folder;
		if (folderName == null) {
			return null;
		}

		if (folderName.startsWith(FIELD_PREFIX)) {
			/*
			 * get it as a field from context
			 */
			folderName = ctx.getTextValue(folderName.substring(1));
			if (folderName == null) {
				throw new ApplicationError(
						"No value for folder name field " + folder.substring(1) + " in service context");
			}
		}

		if (folderName.endsWith(FOLDER_SEP) == false) {
			folderName += FOLDER_SEP;
		}
		return folderName;
	}

	/**
	 * @param ctx
	 * @return
	 */
	private File[] getFiles(String folderName, ServiceContext ctx) {

		File folder = new File(folderName);
		if (folder.exists() == false) {
			throw new ApplicationError("No folder named " + folderName + "and hence batch processor cannot proceed.");
		}

		Tracer.trace("Going to process files in folder " + folderName);

		String fileName = this.batchRowProcessor.inputFile.fileName;
		if (fileName.startsWith(FIELD_PREFIX) == false) {
			/*
			 * it is a pattern, and not a field name.
			 * we are possibly reading multiple files with this pattern
			 */
			File[] files = folder.listFiles(TextUtil.getFileNameFilter(fileName));
			if (files == null || files.length == 0) {
				Tracer.trace("No file found in folder " + this.inputFolder + " matching name " + fileName
						+ ". Batch processor has no work.");
				return null;
			}
			return files;
		}
		/*
		 * get it as a field from context. We are processing just that file
		 */
		fileName = ctx.getTextValue(fileName.substring(1));
		if (fileName == null) {
			throw new ApplicationError(
					"input file name is to be from field " + this.batchRowProcessor.inputFile.fileName.substring(1)
							+ " but no value is foundin service context for thsi field");
		}
		File[] files = { new File(folderName + fileName) };
		return files;
	}

	/**
	 * we create a worker for this component because we need to keep state
	 * across method calls. Our main class remains state-less (extremely
	 * important to ensure thread-safety)
	 *
	 * @author simplity.org
	 *
	 */
	class Worker implements DbClientInterface, MultiTransClientInterface {
		/*
		 * state-less instance attributes
		 */
		final JmsConnector jmsConnector;
		final UserTransaction userTransaction;
		final File[] files;
		final ServiceContext ctx;
		final String inFolderName;
		final String outFolderName;

		/*
		 * state-attribute
		 */
		int nbrRowsProcessed;

		/**
		 * @param inName
		 * @param outName
		 * @param jmsConnector
		 * @param userTransaction
		 * @param files
		 * @param ctx
		 */
		public Worker(String inName, String outName, JmsConnector jmsConnector, UserTransaction userTransaction, File[] files,
				ServiceContext ctx) {
			this.inFolderName = inName;
			this.outFolderName = outName;
			this.jmsConnector = jmsConnector;
			this.userTransaction = userTransaction;
			this.files = files;
			this.ctx = ctx;
		}

		/**
		 * @return number of rows processed by this worker
		 */
		public int getNbrRowsProcessed() {
			return this.nbrRowsProcessed;
		}

		/**
		 * this is an intermediate method to get the dbDriver before executing
		 * our real worker method named processAllFiles()
		 */
		void work() {
			DbAccessType access = BatchProcessor.this.dbAccessType;
			if (access == null || access == DbAccessType.NONE) {
				this.processAllFiles(null);
				return;
			}

			String schema = BatchProcessor.this.schemaName;
			if (access == DbAccessType.READ_WRITE) {
				/*
				 * dbDriver will call us back doMultiplTrans();
				 */
				DbDriver.workForMultiTrans(this, schema);
				return;
			}

			if (access == DbAccessType.SUB_SERVICE) {
				access = DbAccessType.READ_ONLY;
			}
			/*
			 * db driver will call us back with workWithDriver()
			 */
			DbDriver.workWithDriver(this, access, schema);
		}

		@Override
		public int doMultiplTrans(DbDriver dbDriver) {
			/*
			 * we got the dbDriver. go to main processing
			 */
			this.processAllFiles(dbDriver);
			return this.nbrRowsProcessed;
		}

		@Override
		public boolean workWithDriver(DbDriver dbDriver) {
			/*
			 * we got the dbDriver. go to main processing
			 */
			this.processAllFiles(dbDriver);
			return false;
		}

		/**
		 * real worker method that does the core job. Process all the files
		 * using the resources that are made available
		 */
		private void processAllFiles(DbDriver dbDriver) {
			for (File file : this.files) {
				if(BatchProcessor.this.setActualFileNameTo != null){
					this.ctx.setTextValue(BatchProcessor.this.setActualFileNameTo, file.getName());
				}
				try {
					this.nbrRowsProcessed += BatchProcessor.this.batchRowProcessor.process(file, this, dbDriver, this.ctx);
				} catch (Exception e) {
					Action action = BatchProcessor.this.getErrorAction();
					if (action == null) {
						Application.reportApplicationError(null,
								new ApplicationError(e, "Error while processing a file in batchProcessor"));
					} else {
						try {
							action.act(this.ctx, null);
						} catch (Exception ex) {
							Application.reportApplicationError(null, ex);
						}
					}
				}
			}
		}

		/**
		 * called-back from the primary file processor before processing each
		 * row
		 */
		void beginTrans() {
			if (this.userTransaction != null) {
				try {
					this.userTransaction.begin();
				} catch (Exception e) {
					throw new ApplicationError(e, "Unable to use begin() on  user transaction  on on an instance of "
							+ this.userTransaction.getClass().getName());
				}
			}
		}

		/**
		 * called-back from primary file-processor at the end of each row.
		 * Essentially handle commit/rollBack and exception reporting
		 *
		 * @param exception
		 * @param driver
		 */
		void endTrans(Exception exception, DbDriver driver) {
			boolean allOk = exception == null && this.ctx.isInError() == false;
			if (this.userTransaction != null) {
				try {
					if (allOk) {
						this.userTransaction.commit();
					} else {
						this.userTransaction.rollback();
					}
				} catch (Exception ignore) {
					//
				}
			} else {
				/*
				 * service managed transactions
				 */
				if (this.jmsConnector != null) {
					try {
						if (allOk) {
							this.jmsConnector.commit();
						} else {
							this.jmsConnector.rollback();
						}
					} catch (Exception ignore) {
						//
					}
				}

				if (BatchProcessor.this.dbAccessType == DbAccessType.READ_WRITE) {
					try {
						if (allOk) {
							driver.commit();
						} else {
							driver.rollback();
						}
					} catch (Exception ignore) {
						//
					}
				}
			}
			if (exception == null) {
				return;
			}

			/*
			 * invalid data in a row
			 */
			if (exception instanceof InvalidRowException) {
				this.errorOnInputValidation(exception);
				return;
			}
			/*
			 * general error
			 */
			Action action = BatchProcessor.this.getErrorAction();
			if (action == null) {
				Application.reportApplicationError(null, exception);
			} else {
				try {
					/*
					 * this is a sub-service, and we want it to use its own
					 * driver
					 */
					action.act(this.ctx, null);
				} catch (Exception ex) {
					Application.reportApplicationError(null, ex);
				}
			}
		}

		/**
		 * input row was in error
		 * @param exception
		 */
		void errorOnInputValidation(Exception exception){
			Action action = BatchProcessor.this.getInvalidAction();
			if (action == null) {
				Application.reportApplicationError(null,
						new ApplicationError(exception, "Input file contains invalid data for batch processor"));
			} else {
				try {
					/*
					 * this is a sub-service, and we want it to use its own
					 * driver
					 */
					action.act(this.ctx, null);
				} catch (Exception ex) {
					Application.reportApplicationError(null, ex);
				}
			}
		}
	}
/**
 *
 * @param args
 * @throws Exception
 */
	public static void main(String[] args) throws Exception {
		String comp = "c:/repos/simplity/examples/WebContent/WEB-INF/comp/";
		Application.bootStrap(comp);
		String serviceName = "file.po";
		JavaAgent agent = JavaAgent.getAgent("420", "pwd");
		ServiceData outData = agent.serve(serviceName, "");
		System.out.println(outData.getPayLoad());
	}
}
