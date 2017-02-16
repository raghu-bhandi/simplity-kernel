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
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.List;

import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceInterface;

/**
 * @author simplity.org
 *
 */
public class FileProcessor extends Action {
	/**
	 * folder in which we look for files to process
	 */
	String inFolderName;
	/**
	 * example *.txt
	 */
	String fileNamePattern;
	/**
	 * example .bak. Note that we do not remove existing extension, but append
	 * it
	 */
	String archivalExtension;
	/**
	 * record that describes the structure of this file
	 */
	String recordName;

	/**
	 * if we expect multiple rows of this record, or if you want the single row
	 * to be extracted to a data sheet. If this is not specified, we extract the
	 * first/only row as fields into service context
	 */
	String sheetName;

	/**
	 * service to be execute for data in each file
	 */
	String serviceName;

	/**
	 * Are there end-of-line markers in this file (could be LF or CR/LF )
	 */
	boolean fileHasEndOfLineChars;
	/**
	 * filter corresponding to the input file
	 */
	private FilenameFilter filter;

	private File inbox;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#getReady(int)
	 */
	@Override
	public void getReady(int idx) {
		super.getReady(idx);
		this.filter = TextUtil.getFIleNameFilter(this.fileNamePattern);
		this.inbox = new File (this.inFolderName);
	}

	/* (non-Javadoc)
	 * @see org.simplity.tp.Action#delegate(org.simplity.service.ServiceContext, org.simplity.kernel.db.DbDriver)
	 */
	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
		int nbrFiles= 0;
		try {
			for (File file : this.inbox.listFiles(this.filter)) {
				FileChannel channel = new RandomAccessFile(file, "rw")
						.getChannel();
				try {
					FileLock lock = channel.tryLock();
					lock.release();
					try {
						this.processOneFile(file, ctx, driver);
					} catch (Exception e) {
						System.err.println("Error while processing "
								+ file.getName() + "\n" + e.getMessage()
								+ "\n moving on to next file");
					}
					File newFile = new File(file.getPath() + this.archivalExtension);
					if (file.renameTo(newFile) == false) {
						Tracer.trace("Sorry, unable to rename the file to "
								+ newFile.getAbsolutePath()
								+ " going to delete it.");
						if (file.delete() == false) {
							Tracer.trace(
									"Hey Bhagavaan, Ye tune kya kar raha hai? I am unable to provide muktu to file "
											+ file.getName());
						}

					}
				} catch (OverlappingFileLockException e) {
					Tracer.trace("Unble to lock " + file.getName() + "\n "
							+ e.getMessage());
					/*
					 * may be another thread is processing this
					 */
					continue;
				}
				nbrFiles++;
			}
		} catch (Exception e) {
			Tracer.trace(e, "Error while working on the folder");
		}
		return Value.newIntegerValue(nbrFiles);
	}
	/**
	 * @param file
	 * @throws IOException
	 */
	private boolean processOneFile(File file, ServiceContext ctx, DbDriver driver) throws IOException {
		Tracer.trace("Processing " + file.getAbsolutePath() + "....");
		Record record = ComponentManager.getRecord(this.recordName);
		FileReader fr = new FileReader(file);
		BufferedReader reader = new BufferedReader(fr);
		List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
		DataSheet ds = record.fromFlatFile(reader, errors, this.fileHasEndOfLineChars);
		if (errors.size() > 0) {
			/*
			 * process error. msg is a data structure with details for I18N as
			 * well as client side help on fields and rows. FOr demo we just
			 * spit out the text
			 */
			for (FormattedMessage msg : errors) {
				System.err.println(msg.text);
			}
			return false;
		}

		ctx.putDataSheet(this.sheetName, ds);
		ServiceInterface service = ComponentManager.getService(this.serviceName);
		service.executeAsAction(ctx, driver);
		return true;
	}


}
