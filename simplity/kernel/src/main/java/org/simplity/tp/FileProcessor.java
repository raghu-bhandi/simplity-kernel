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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.simplity.kernel.Application;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.FlatFileRowType;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.expr.Expression;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * @author simplity.org
 *
 */
public class FileProcessor extends Block {
	/**
	 * folder in which we look for files to process
	 */
	String inFolderName;
	/**
	 * folder in which we look for files to process
	 */
	String outFolderName;
	/**
	 * example *.txt
	 */
	String inFileNamePattern;
	/**
	 * parsed inFileNamePattern
	 */
	private String parsedInFileNamePattern;
	/**
	 * if we are to create an output file. file name can contain parts of input
	 * file name as part of it. example if input file name is a.txt
	 * {name}{ext}.out will translate to a.txt.out and {fileName}.out will
	 * translate to a.out
	 */
	String outFileName;

	/**
	 * Similar to outFileName that can be based on input file name
	 */
	String renameInFileTo;
	/**
	 * should the input file be deleted after processing it? Not relevant if
	 * rename attribute is specified.
	 */
	boolean deleteInFileAfterProcessing;
	/**
	 * record that describes the structure of this file
	 */
	String inRecordName;
	/**
	 * record that describes the structure of this file
	 */
	String outRecordName;

	/**
	 * format of the data in input file
	 */
	FlatFileRowType inDataFormat;
	/**
	 * format of the data in output file
	 */
	FlatFileRowType outDataFormat;

	/**
	 * What action do we take in case the input row fails data-type validation?
	 */
	Action actionOnInvalidInputRow;

	/**
	 * action to be executed of the row processing generates error
	 */
	Action actionOnErrorWhileProcessing;

	/**
	 * is there a conditional based on which we have to decide whether to write
	 * output or not? Null means we write always. If specified, output row is
	 * written for this row only if it evaluates to true.
	 */
	Expression conditionForOutput;
	/**
	 * filter corresponding to the input file
	 */

	private File inbox;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#delegate(org.simplity.service.ServiceContext,
	 * org.simplity.kernel.db.DbDriver)
	 */
	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
		Tracer.trace("Going to process files in folder " + this.inFolderName + " that exists = " + this.inbox.exists());
		
		if(this.inFileNamePattern.startsWith("$")){
				this.parsedInFileNamePattern = ctx.getValue(this.parsedInFileNamePattern).toText();	
		}
		
		FilenameFilter filter = TextUtil.getFileNameFilter(this.parsedInFileNamePattern);
		int nbrFiles = 0;
		Record record = ComponentManager.getRecord(this.inRecordName);
		Record outRecord = null;
		if (this.outRecordName != null) {
			outRecord = ComponentManager.getRecord(this.outRecordName);
		}
		BlockWorker worker = new BlockWorker(this.actions, this.indexedActions, ctx);
		for (File file : this.inbox.listFiles(filter)) {
			Tracer.trace("File " + file.getAbsolutePath());
			if (this.processOneFile(file, ctx, driver, record, outRecord, worker)) {
				nbrFiles++;
			}
		}
		return Value.newIntegerValue(nbrFiles);
	}

	/**
	 * @param file
	 * @throws IOException
	 */
	private boolean processOneFile(File file, ServiceContext ctx, DbDriver driver, Record record, Record outRecord,
			BlockWorker worker) {

		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			/*
			 * create input/output streams
			 */
			String inName = file.getName();
			Tracer.trace("Processing " + inName + "....");
			reader = new BufferedReader(new FileReader(file));
			if (this.outFileName != null) {
				String outName = TextUtil.getFileName(this.outFileName, inName);
				File outFile = new File(this.outFolderName + outName);
				writer = new BufferedWriter(new FileWriter(outFile));
			}
			String inText;
			/*
			 * loop on each row in input file. Note that the ctx is not reset
			 * per row. However, error status is reset because we manage
			 * transactions at row level
			 */
			List<FormattedMessage> earlierMessages = new ArrayList<FormattedMessage>(ctx.getMessages());
			List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
			while ((inText = reader.readLine()) != null) {
				/*
				 * Absolved of all past sins. Start life afresh :-)
				 **/				
				ctx.resetMessages();
				errors.clear();
				record.extractFromFlatRow(inText, this.inDataFormat, ctx, errors);
				/*
				 * above method validates input as per data type specification.
				 * Was there any trouble?
				 */
				if (errors.size() > 0) {
					for(FormattedMessage error:errors){
						error.addData(inText);
					}
					List<FormattedMessage> errorsCopy = new ArrayList<FormattedMessage>(errors);
					earlierMessages.addAll(errorsCopy);
					ctx.setTextValue("invalidInputRow", inText);

					if (this.actionOnInvalidInputRow == null) {
						Tracer.trace("Invalid row received as input. Row is not processed.");
					} else {
						this.actionOnInvalidInputRow.act(ctx, driver);
					}
					continue;
				}
				/*
				 * now that the input is fine, let us process the row
				 */
				worker.execute(driver);
				/*
				 * any trouble while processing?
				 */
				if (ctx.isInError()) {
					ctx.setTextValue("errorWhileProcessingRow", inText);
					if (this.actionOnErrorWhileProcessing == null) {
						Tracer.trace("Invalid row received as input. Row is not processed.");
					} else {
						this.actionOnErrorWhileProcessing.act(ctx, driver);
					}
					continue;
				}
				/*
				 * re-populate the messages
				 */
				{
					List<FormattedMessage> errorsCopy = new ArrayList<FormattedMessage>(ctx.getMessages());
					earlierMessages.addAll(errorsCopy);
				}

				/*
				 * are we to write output row?
				 */
				if (writer != null) {
					if (this.conditionForOutput != null) {
						if (this.conditionForOutput.evaluate(ctx).toBoolean() == false) {
							Tracer.trace("Output not written because the condition for the same is not satisfied.");
							continue;
						}
					}
					String outText = outRecord.formatFlatRow(this.outDataFormat, ctx);
					writer.write(outText);
					writer.newLine();
				}
			}
			ctx.resetMessages();
			ctx.addMessages(earlierMessages);
			/*
			 * important to close the stream before trying to delete/rename
			 */
			reader.close();
			if (this.renameInFileTo != null) {
				String arcName = this.inFolderName + TextUtil.getFileName(this.renameInFileTo, inName);
				Tracer.trace("Renaming to " + arcName);
				file.renameTo(new File(arcName));
			} else if (this.deleteInFileAfterProcessing) {
				if (file.delete() == false) {
					throw new ApplicationError("Unable to delete file " + file.getPath());
				}
			}
			reader = null;
			return true;
		} catch (Exception e) {
			Application.getExceptionListener().listen(null, e);
			return false;
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception ignore) {
					//
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ignore) {
					//
				}
			}
		}
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#getReady(int)
	 */
	@Override
	public void getReady(int idx) {
		super.getReady(idx);
		
		this.parsedInFileNamePattern = TextUtil.getFieldName(this.inFileNamePattern);
		if(this.parsedInFileNamePattern==null){
			this.parsedInFileNamePattern = this.inFileNamePattern;
		}
		
		if (this.actionOnErrorWhileProcessing != null) {
			this.actionOnErrorWhileProcessing.getReady(0);
		}
		if (this.actionOnInvalidInputRow != null) {
			this.actionOnInvalidInputRow.getReady(0);
		}
				
		this.inbox = new File(this.inFolderName);
		if (this.inFolderName != null && this.inFolderName.endsWith("/") == false) {
			this.inFolderName += '/';
		}
		if (this.outFolderName != null && this.outFolderName.endsWith("/") == false) {
			this.outFolderName += '/';
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
		int count = 0;
		count = super.validate(vtx, service);
		if (this.inFolderName == null) {
			vtx.addError("inFolderName is required for file processor");
			count++;
		}
		if (this.inFileNamePattern == null) {
			vtx.addError("inFileNamePattern is required for file processor");
			count++;
		}
		if (this.inDataFormat == null) {
			vtx.addError("inDataFormat is required for file processor");
			count++;
		}
		if (this.inRecordName == null) {
			vtx.addError("inRecordName is required for file processor");
			count++;
		}
		if (this.outFolderName != null) {
			if (this.outDataFormat == null) {
				vtx.addError("outDataFormat is required when outFolderName is specified");
				count++;
			}
			if (this.outRecordName == null) {
				vtx.addError("outRecordName is required when outFolderName is specified");
				count++;
			}
			if (this.outFileName == null) {
				vtx.addError("outFileName is required when outFolderName is specified");
				count++;
			}
		} else if (this.outDataFormat != null || this.outRecordName != null || this.outFileName != null) {
			vtx.reportUnusualSetting(
					"Since outFolderName is not specified, no output is going to be written, and other output related attributes are ignored.");
		}
		if (this.renameInFileTo == null) {
			if (this.deleteInFileAfterProcessing == false) {
				vtx.reportUnusualSetting(
						"Input file is neither deleted, nor renamed. This may result in the file being processed again.");
			}
		} else if (this.deleteInFileAfterProcessing) {
			vtx.reportUnusualSetting("since rename is specified, we ignore delete directive.");
		}
		/*
		 * If db updates re involved, file-processor requires that the service
		 * delegates commits to sub-service
		 */
		if (this.dbAccess == DbAccessType.READ_WRITE) {
			count += this.validateDbAccess(service, vtx);
		}
		return count;
	}

	private int validateDbAccess(Service service, ValidationContext vtx) {
		int count = 0;
		/*
		 * There must be one sub-service action that manages its own
		 * transaction, and rest of the actions should not do any updates
		 */
		if (service.dbAccessType != DbAccessType.SUB_SERVICE) {
			vtx.addError(
					"File-processor is designed to update data base. Service should delegate transaction processing to sub-service.");
			count++;
		}
		if (this.actionOnErrorWhileProcessing.getDataAccessType().updatesDb()) {
			if (this.actionOnErrorWhileProcessing instanceof SubService == false) {
				vtx.addError(
						"actionOnErrorWhileProcessing is designed for db update. For such a requirement, you should convert this to a subService action.");
				count++;
			}
		}
		if (this.actionOnInvalidInputRow.getDataAccessType().updatesDb()) {
			if (this.actionOnInvalidInputRow instanceof SubService == false) {
				vtx.addError(
						"actionOnInvalidInputRow is designed for db update. For such a requirement, you should convert this to a subService action.");
				count++;
			}
		}
		for (Action action : this.actions) {
			if (action instanceof SubService == false && action.getDataAccessType().updatesDb()) {
				vtx.addError(
						"actions of a file-processor is designed for a bb update. You should re-factor this into sub-service so that the db updates can be managed within  separate transaction.");
				count++;
			}
		}
		return count;
	}

}
