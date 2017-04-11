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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
public class InputFile {
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
	 * name relative to input file. Valid only if this is used as in input file
	 */
	String renameInfileTo;

	/**
	 * valid only if this is input, and renameInfileTo is not specified
	 */
	boolean deleteFile;

	Expression conditionToProcess;

	/**
	 * @param service
	 */
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
		throw new ApplicationError("Input file should linkFieldsInParentRow to match linkFieldsInThisRow ");
	}

	/**
	 *
	 * @return worker to read from this file
	 */
	public Worker getWorker() {
		return new Worker();
	}

	/**
	 * state-full class that is is invoked repeatedly by FileProcessor. We have
	 * to have an instance of this for each run. This is our approach to ensure
	 * that the main component, INputFile, InputFile, remains immutable
	 *
	 * @author simplity.org
	 *
	 */
	class Worker {
		protected Record record;
		private File realFile;
		protected ServiceContext ctx;

		private BufferedReader reader;
		private File newFile;
		/*
		 * in case we are a child file, and we have to read matching rows
		 */
		/**
		 * index of key fields to dataRow array
		 */
		private int[] keyIndexes;
		/**
		 * data read from input file that is yet to be used (read ahead)
		 */
		private Value[] dataRow;
		/**
		 * all the field names in this rec
		 */
		private String[] fieldNames;
		/**
		 * computed/concatenated string of key fields
		 */
		private String keyValue;

		/**
		 * did we hit the wall while trying to read?
		 */
		private boolean endOfFile;

		/**
		 *
		 * @return actual file name being matched
		 */
		String getFileName() {
			return this.realFile.getName();
		}

		/**
		 * called when this is a child file
		 *
		 * @param rootFolder
		 * @param refFileName
		 * @param ctxt
		 * @throws IOException
		 */
		void openShop(String rootFolder, String refFileName, ServiceContext ctxt) throws IOException {
			this.ctx = ctxt;
			String fn = TextUtil.getFileName(InputFile.this.fileName, refFileName, ctxt);
			this.realFile = new File(rootFolder + fn);
			this.openShopCommon(rootFolder);
		}

		/**
		 * called by primary file processor when this is the driver-input file.
		 * A real file is passed in this case.
		 *
		 *
		 * @param rootFolder
		 * @param file
		 * @param ctxt
		 * @throws IOException
		 */
		void openShop(String rootFolder, File file, ServiceContext ctxt) throws IOException {
			this.ctx = ctxt;
			this.realFile = file;
			this.openShopCommon(rootFolder);
		}

		/**
		 * Initialize all state-variables
		 */
		void openShopCommon(String rootFolder) throws FileNotFoundException {
			this.record = ComponentManager.getRecord(InputFile.this.recordName);

			if (this.realFile == null) {
				this.realFile = new File(rootFolder + this.realFile);
			}
			if (this.realFile.exists() == false) {
				throw new ApplicationError("File " + this.realFile.getAbsolutePath() + " does not exist");
			}
			this.reader = new BufferedReader(new FileReader(this.realFile));

			String newName = InputFile.this.renameInfileTo;
			if (newName != null) {
				this.newFile = new File(rootFolder + TextUtil.getFileName(newName, this.getFileName(), this.ctx));
			}

			String[] names = InputFile.this.linkFieldsInThisRow;
			if (names == null || names.length == 0) {
				return;
			}
			this.keyIndexes = new int[names.length];
			for (int i = 0; i < names.length; i++) {
				int idx = this.record.getFieldIndex(names[i]);
				if (idx == -1) {
					throw new ApplicationError(names[i] + " is not a field in record " + InputFile.this.recordName
							+ " but it is being referred as a key field to match");
				}
				this.keyIndexes[i] = idx;
			}
			this.fieldNames = this.record.getFieldNames();
		}

		/**
		 * read a row, and extract it into context. This method is invoked for
		 * driver-file. It is also invoked in case the child-file is one-to-one
		 * and not based on key match. That is, it is actually not a child, but
		 * an associate
		 */
		boolean extractToCtx(List<FormattedMessage> errors) throws IOException {

			/*
			 * we loop in case there is some condition for row row to be processed
			 */
			while (true) {
				String rowText = this.reader.readLine();
				if (rowText == null) {
					return false;
				}
				/*
				 * parse this text into fields and push them to service context
				 */
				this.record.parseFlatFileRow(rowText, InputFile.this.dataFormat, this.ctx, errors);
				/*
				 * is there a condition to process this?
				 */
				if (this.okToProceed()) {
					return true;
				}
			}
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
			if (this.keyIndexes == null) {
				return null;
			}
			String parentKey = "";
			for (int i = 0; i < this.keyIndexes.length; i++) {
				Value value = this.ctx.getValue(InputFile.this.linkFieldsInParentRow[i]);
				if (Value.isNull(value)) {
					errors.add(new FormattedMessage("missingKeyColumn", MessageType.ERROR,
							"value for link field " + InputFile.this.linkFieldsInThisRow[i]
									+ " is missing in a row in file " + this.getFileName()));
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

		/**
		 * read a child row that matches parent key. INvoked when this file is a
		 * child-file with possible multiple rows for a given parent row
		 */
		boolean extractForMatchingKey(List<FormattedMessage> errors, String keyToMatch)
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
						Tracer.trace("Ignoring a row in child file " + this.getFileName() + " with key " + this.keyValue
								+ " as there is no parent row for this.");
						allOk = this.readChildRow(errors);
						continue;
					}
				}
				/*
				 * Ok, this child for the right parent.
				 */
				for (int i = 0; i < this.fieldNames.length; i++) {
					this.ctx.setValue(this.fieldNames[i], this.dataRow[i]);
				}
				/*
				 * important to empty the cache once it is used
				 */
				this.dataRow = null;
				/*
				 * Are there further conditions to process this?
				 */
				if (this.okToProceed()) {
					return true;
				}
				Tracer.trace("Ignoring a child row that failed qualifying condition.");
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
		private boolean okToProceed() {
			Expression condition = InputFile.this.conditionToProcess;
			if (condition == null) {
				return true;
			}
			Value val;
			try {
				val = condition.evaluate(this.ctx);
			} catch (InvalidOperationException e) {
				throw new ApplicationError(e, "Input file is using conditionToProcess=" + condition
						+ " but this expression probably has improper data types/operators.");
			}
			return Value.intepretAsBoolean(val);
		}

		private boolean readChildRow(List<FormattedMessage> errors) throws IOException, InvalidRowException {
			String rowText = this.readLine();
			if (rowText == null) {
				return false;
			}
			this.dataRow = this.record.extractFromFlatRow(rowText, InputFile.this.dataFormat, errors);
			if (this.dataRow == null) {
				Tracer.trace("Validation errors found during extracting a row from flat file using record "
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
					errors.add(new FormattedMessage("missingKeyColumn", MessageType.ERROR,
							"value for link field " + InputFile.this.linkFieldsInThisRow[i]
									+ " is missing in a row in file " + this.getFileName()));
					throw new InvalidRowException("Validation errors found in input file");
				}
				this.keyValue += value.toString();
			}
			return true;
		}

		/**
		 * release any resources
		 */
		void closeShop() {
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
	}
}
