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
import java.io.FilenameFilter;
import java.io.IOException;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataSerializationType;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.file.FileManager;
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
	 * example .bak. Note that we do not remove existing extension, but append
	 * it
	 */
	String archivalExtension;
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
	DataSerializationType inDataFormat;
	/**
	 * format of the data in output file
	 */
	DataSerializationType outDataFormat;
	/**
	 * if we expect multiple rows of this record, or if you want the single row
	 * to be extracted to a data sheet. If this is not specified, we extract the
	 * first/only row as fields into service context
	 */
	String inSheetName;

	/**
	 * if we expect multiple rows of this record, or if you want the single row
	 * to be extracted to a data sheet. If this is not specified, we extract the
	 * first/only row as fields into service context
	 */
	String outSheetName;

	/**
	 * filter corresponding to the input file
	 */
	private FilenameFilter filter;

	private File inbox;

	private File outbox;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#delegate(org.simplity.service.ServiceContext,
	 * org.simplity.kernel.db.DbDriver)
	 */
	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
		Tracer.trace("Going to process files in folder " + this.inFolderName
				+ " that exists = " + this.inbox.exists());
		int nbrFiles = 0;
		for (File file : this.inbox.listFiles(this.filter)) {
			Tracer.trace("File " + file.getAbsolutePath());
			if (this.processOneFile(file, ctx, driver)) {
				nbrFiles++;
			}
		}
		return Value.newIntegerValue(nbrFiles);
	}

	/**
	 * @param file
	 * @throws IOException
	 */
	private boolean processOneFile(File file, ServiceContext ctx,
			DbDriver driver) {
		try {
			Tracer.trace("Processing " + file.getAbsolutePath() + "....");
			Record record = ComponentManager.getRecord(this.inRecordName);
			String inText = FileManager.readFile(file);
			DataSheet ds = this.inDataFormat.parseRows(inText,
					record.getFields());

			ctx.putDataSheet(this.inSheetName, ds);
			if (this.outSheetName != null) {
				Record outRecord = ComponentManager.getRecord(this.outRecordName);
				DataSheet outDs = outRecord.createSheet(false, false);
				ctx.putDataSheet(this.outSheetName, outDs);	
			}
			BlockWorker worker = new BlockWorker(this.actions,
					this.indexedActions, ctx);
			worker.execute(driver);
			file.renameTo(new File(file.getName() + ".bak"));

			if (this.outbox == null) {
				return true;
			}

			
			String outText = null;

			Value[][] values = ds.getAllRows().toArray(new Value[0][]);
			if (this.outRecordName == null) {
				outText = this.outDataFormat.serializeRows(values,
						ds.getColumnNames());
			} else {
				outText = this.outDataFormat.serializeRows(values,
						record.getFields());
			}
			String outpath = this.outbox.getAbsolutePath()
					.concat(java.io.File.separator)
					.concat(file.getName());
			FileManager.writeFile(new File(outpath), outText);
			return true;
		}catch(Exception e){
			throw new ApplicationError(e, "Error while processing file " +file.getAbsolutePath());
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
		this.filter = TextUtil.getFileNameFilter(this.inFileNamePattern);
		this.inbox = new File(this.inFolderName);
		if (this.outFolderName != null) {
			this.outbox = new File(this.outFolderName);
		} else {
			this.outbox = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.simplity.tp.Block#validate(org.simplity.kernel.comp.ValidationContext, org.simplity.tp.Service)
	 */
	@Override
	public int validate(ValidationContext vtx, Service service) {
		int count = 0;
		count = super.validate(vtx, service);
		if(this.inFolderName == null){
			vtx.addError("inFolderName is required for file processor");
			count++;
		}
		if(this.inFileNamePattern == null){
			vtx.addError("inFileNamePattern is required for file processor");
			count++;
		}
		if(this.inDataFormat == null){
			vtx.addError("inDataFormat is required for file processor");
			count++;
		}
		if(this.inSheetName == null){
			vtx.addError("inSheetName is required for file processor");
			count++;
		}
		if(this.inRecordName == null){
			vtx.addError("inRecordName is required for file processor");
			count++;
		}
		if(this.outFolderName != null){
			if(this.outDataFormat == null){
				vtx.addError("outDataFormat is required when outFolderName is specified");
				count++;
			}
			if(this.outSheetName == null){
				vtx.addError("outSheetName is required when outFolderName is specified");
				count++;
			}
			if(this.outRecordName == null){
				vtx.addError("outRecordName is required when outFolderName is specified");
				count++;
			}
		}
		return count;
	}
}
