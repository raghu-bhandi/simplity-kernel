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
import java.io.IOException;
import java.util.List;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;
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
 *
 */
public class FlatFile {
	/**
	 *
	 * If this is an associate file, then it is expressed using {name} and and
	 * {ext} that stand
	 * for file name and extension of primary input file. example if input file
	 * name is a.txt
	 * {name}.{ext}.out will translate to a.txt.out and {fileName}.out will
	 * translate to a.out.
	 *
	 * If this is the primary (driver) file, then this is a pattern as per java
	 * file name pattern. Refer to java file filter specification
	 */
	String fileName;
	/**
	 * record that describes fields/columns in this file
	 */
	String recordName;
	/**
	 * how columns/fields are organized in each row
	 */
	FlatFileRowType dataFormat;
	/**
	 * in case this is a child input file, specify the link columns (keys) in
	 * this row
	 */

	String[] linkFieldsInThisRow;

	/**
	 * in case this is a child input file, specify the link columns (keys)
	 */
	String[] linkFieldsInParentRow;
	/**
	 * in case the row may be written conditionally. Defaults to write always.
	 */
	Expression conditionForOutput;
	/**
	 * name relative to input file. Valid only if this is used as in input file
	 */
	String renameInfileTo;

	/**
	 * valid only if this is input, and renameInfileTo is not specified
	 */
	boolean deleteFile;

	/**
	 *
	 * @return worker to read from this file
	 */
	public InputWorker getInputWorker() {
		return new InputWorker();
	}

	/**
	 *
	 * @return worker to write to this file
	 */
	public OutputWorker getOutputWorker() {
		return new OutputWorker();
	}

	class Worker {
		protected Record record;
		protected String realName;
		protected ServiceContext ctx;

		String getFileName() {
			return this.realName;
		}

		protected void openShop(String refFileName, ServiceContext ctxt, boolean setRealNameAsWell) {
			if (setRealNameAsWell) {
				this.realName = TextUtil.getFileName(FlatFile.this.fileName, refFileName, ctxt);
			}
			this.record = ComponentManager.getRecord(FlatFile.this.recordName);
			this.ctx = ctxt;
		}
	}

	class OutputWorker extends Worker {
		private BufferedWriter writer;

		void openShop(String rootFolder, String refFileName, ServiceContext ctxt) throws IOException {
			super.openShop(refFileName, ctxt, true);
			this.writer = new BufferedWriter(new FileWriter(rootFolder + this.realName));
		}

		void writeOut() throws IOException {
			Expression expr = FlatFile.this.conditionForOutput;
			try {
				if (expr == null || Value.intepretAsBoolean(expr.evaluate(this.ctx))) {
					this.writer.write((this.record.formatFlatRow(FlatFile.this.dataFormat, this.ctx)));
					this.writer.newLine();
				}
			} catch (InvalidOperationException e) {
				throw new ApplicationError(e, "Error while evaluating expresssion " + expr);
			}
		}

		void closeShop() {
			try {
				this.writer.close();
			} catch (Exception ignore) {
				//
			}
		}
	}

	class InputWorker extends Worker {
		private File realFile;
		private BufferedReader reader;
		private File newFile;
		/*
		 * in case we are a child file, and we have to read matching rows
		 */
		private Value[] dataRow;
		private int[] keyIndexes;
		private String[] fieldNames;
		private String keyValue;
		private boolean endOfFile;

		void openShop(String rootFolder, String refFileName, ServiceContext ctxt) throws IOException {
			super.openShop(refFileName, ctxt, true);
			File file = this.realFile;
			if(file == null){
				file = new File(rootFolder + this.realName);
			}
			if(file.exists() == false){
				throw new ApplicationError("File " + file.getAbsolutePath() + " is not found for reading");
			}
			this.reader = new BufferedReader(new FileReader(file));
			this.gearUpToRead(rootFolder);
		}

		/**
		 * called by primary file processor. File is already created based on
		 * matching criterion, We should not use fileNAme attribute
		 *
		 * @param rootFolder
		 * @param file
		 * @param ctxt
		 * @throws IOException
		 */
		void openShop(String rootFolder, File file, ServiceContext ctxt) throws IOException {
			super.openShop(null, ctxt, false);
			this.realFile = file;
			this.realName = file.getName();
			this.reader = new BufferedReader(new FileReader(file));
			this.gearUpToRead(rootFolder);
		}

		void gearUpToRead(String rootFolder) {
			String newName = FlatFile.this.renameInfileTo;
			if (newName != null) {
				this.newFile = new File(rootFolder + TextUtil.getFileName(newName, this.realName, this.ctx));
			}
			String[] names = FlatFile.this.linkFieldsInThisRow;
			if (names == null || names.length == 0) {
				return;
			}
			this.keyIndexes = new int[names.length];
			for (int i = 0; i < names.length; i++) {
				int idx = this.record.getFieldIndex(names[i]);
				if (idx == -1) {
					throw new ApplicationError(names[i] + " is not a field in record " + FlatFile.this.recordName
							+ " but it is being referred as a key field to match");
				}
				this.keyIndexes[i] = idx;
			}
			this.fieldNames = this.record.getFieldNames();
		}

		/**
		 * read a row, and extract it into context.
		 */
		boolean read(List<FormattedMessage> errors) throws IOException {
			if (this.endOfFile) {
				return false;
			}

			String rowText = this.reader.readLine();
			if (rowText == null) {
				return false;
			}
			/*
			 * parse this text into fields and push them to service context
			 */
			this.record.parseFlatFileRow(rowText, FlatFile.this.dataFormat, this.ctx, errors);
			return true;
		}

		/**
		 * should the rows from this file be read for matching keys?
		 *
		 * @return true mens the caller should first get parent key and then
		 *         read for matching.
		 *         false means a row is read always.
		 */
		boolean toBeMatched() {
			return this.fieldNames != null;
		}

		/**
		 * get parent key to be matched for reading input file. To be called
		 * after checking with toBeMatched()
		 *
		 * @param errors
		 * @return key
		 */
		String getParentKey(List<FormattedMessage> errors) {
			String parentKey = "";
			for (int i = 0; i < this.keyIndexes.length; i++) {
				Value value = this.ctx.getValue(FlatFile.this.linkFieldsInParentRow[i]);
				if (Value.isNull(value)) {
					errors.add(new FormattedMessage("missingKeyColumn", MessageType.ERROR, "value for link field "
							+ FlatFile.this.linkFieldsInThisRow[i] + " is missing in a row in file " + this.realName));
					return null;
				}
				parentKey += value.toString();
			}
			return parentKey;
		}

		private String readLine() throws IOException {
			String rowText = this.reader.readLine();
			if (rowText == null) {
				this.endOfFile = true;
			}
			return rowText;
		}

		/**
		 * read a child row that matches prent key
		 */
		boolean readChildRow(List<FormattedMessage> errors, String keyToMatch) throws IOException, InvalidRowException {
			boolean allOk = true;
			/*
			 * ensure that we have a row in our cache before we enter the
			 * while-loop, unless of course it is end-of-file
			 */
			if (this.keyValue == null) {
				allOk = this.readChildRow(errors);
			}
			while (allOk) {
				int cmp = this.keyValue.compareToIgnoreCase(keyToMatch);
				if (cmp > 0) {
					/*
					 * we have moved ahead of the parent
					 */
					return false;
				}
				if (cmp == 0) {
					/*
					 * got it. push fields from cache to ctx
					 */
					for (int i = 0; i < this.fieldNames.length; i++) {
						this.ctx.setValue(this.fieldNames[i], this.dataRow[i]);
					}
					/*
					 * clear keyValue so that we force a read next time
					 */
					this.keyValue = null;
					return true;
				}
				Tracer.trace("Ignoring a row in child file " + this.realName + " with key " + this.keyValue
						+ " as there is no parent row for this.");
				allOk = this.readChildRow(errors);
			}
			/*
			 * we reach here only if there are no more rows. But there is no way
			 * we can tell the caller about it
			 */
			return false;
		}

		private boolean readChildRow(List<FormattedMessage> errors) throws IOException, InvalidRowException {
			String rowText = this.readLine();
			if (rowText == null) {
				return false;
			}
			this.dataRow = this.record.extractFromFlatRow(rowText, FlatFile.this.dataFormat, errors);
			if (this.dataRow == null) {
				throw new InvalidRowException();
			}
			/*
			 * set key value
			 */
			this.keyValue = "";
			for (int i = 0; i < this.keyIndexes.length; i++) {
				Value value = this.dataRow[this.keyIndexes[i]];
				if (Value.isNull(value)) {
					errors.add(new FormattedMessage("missingKeyColumn", MessageType.ERROR, "value for link field "
							+ FlatFile.this.linkFieldsInThisRow[i] + " is missing in a row in file " + this.realName));
					throw new InvalidRowException();
				}
				this.keyValue += value.toString();
			}
			return true;
		}

		void closeShop() {
			try {
				this.reader.close();
			} catch (Exception ignore) {
				//
			}
			if (this.newFile != null) {
				this.realFile.renameTo(this.newFile);
			} else if (FlatFile.this.deleteFile) {
				this.realFile.delete();
			}
		}
	}

	/**
	 * @param service
	 */
	public void getReady(Service service) {
		// TODO Auto-generated method stub

	}

}
