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

package  org.simplity.examples;

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
import java.util.Date;
import java.util.List;

import org.simplity.kernel.Application;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;

/**
 * A scheduled job picks-up files from a folder and processes them
 *
 * @author simplity.org
 *
 */
public class FlatFileAgent implements Runnable {
	/**
	 * hard coded parameters for the sake of demo.
	 */
	private static final String IN_FOLDER_NAME = "d:/temp/test/in/";

	/**
	 * service is executed as if this user id is authenticated. In case any of
	 * rdbms table uses createdBy/modifiedBy field, this user id is used as the
	 * value for that column
	 */
	private static final Value USER_ID = Value.newTextValue("420");
	/**
	 * qualified record name that describes the field structure in this flat
	 * file
	 */
	private static final String RECORD_NAME = "fixed";
	/**
	 * qualified name of the service to be invoked for data in each file
	 */
	private static final String SERVICE_NAME = "createOrders";
	/**
	 * sheet name in which the service is expecting contents of the flat file
	 */
	private static final String SHEET_NAME = "orders";
	/**
	 * we allow copiers to use a different extension during copy operation, and
	 * rename to the desired file name once copy is complete. This approach
	 * avoid problems associated with concurrent access to the file while it is
	 * still being written into
	 */
	private static final FilenameFilter filter = new FilenameFilter() {

		@Override
		public boolean accept(File dir, String name) {
			return name.matches(".*\\.txt");

		}
	};

	/*
	 * by default we take loooong nap for few seconds between our polling
	 */
	private static final long NAP_TIME = 10000;
	private static final String BACK = ".bak";

	/**
	 * current design is to have a single worker instance.
	 */
	private static FlatFileAgent myInstance;

	private final File inbox;
	private final long napTime;
	private boolean timeToWindup;

	/**
	 * @param inf
	 * @param outf
	 * @param inter
	 * @throws Exception
	 */
	private FlatFileAgent(String inf, long inter) throws Exception {
		this.inbox = new File(inf);
		this.napTime = inter;
		if (this.inbox.exists() == false) {
			throw new Exception(inf + " is not a valid path.");
		}
		if (this.inbox.isDirectory() == false) {
			throw new Exception(
					inf + " is a valid path but it is not a folder.");
		}
		this.getOnTheJob();
	}

	/**
	 * start the job. Keep working till stop() is invoked
	 *
	 * @param inFolderName
	 *            folder where we look for files to be processed. Null to use
	 *            default folder.
	 * @param interval
	 *            between checking for arrival of files. 0 to use our default
	 *            value
	 * @throws Exception
	 *             Obviously, in case of any exception :-)
	 */
	public static synchronized void start(String inFolderName, long interval)
			throws Exception {
		if (myInstance != null) {
			myInstance.shutDownPlease();
		}
		String inf = inFolderName == null ? IN_FOLDER_NAME : inFolderName;
		long inter = interval == 0 ? NAP_TIME : interval;

		myInstance = new FlatFileAgent(inf, inter);
	}

	/**
	 * stop the work
	 */
	public static void stop() {
		if (myInstance == null) {
			Tracer.trace("Stop request received but there is nothing to stop");
		} else {
			myInstance.shutDownPlease();
		}
	}

	/**
	 * shut down the operation
	 */
	private void shutDownPlease() {
		Tracer.trace("Received a request to stop the work.");
		/*
		 * we prefer requesting than interrupting. Hence designed a flag rather
		 * than keeping a handle to the thread
		 */
		this.timeToWindup = true;
	}

	private void getOnTheJob() {
		Thread thread = new Thread(this);
		thread.start();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		/*
		 * we re making this an infinite loop, but listen to request for a
		 * wind-up
		 */
		Tracer.trace("New thread " + Thread.currentThread().getName()
				+ " started..");
		while (true) {
			Tracer.trace("Started a job at " + new Date());
			this.processFiles();
			if (this.timeToWindup) {
				Tracer.trace(
						"Alright. End-of-day siren is heard. Let me rush to the gate..");
				return;
			}
			try {
				Tracer.trace("Going to take a nap for " + this.napTime + "ms");
				Thread.sleep(this.napTime);
			} catch (InterruptedException e) {
				Tracer.trace(
						"I hate being woken up, but I can't help responding to that..");
				break;
			}
		}
	}

	/**
	 * If you use Java 7, you should manage the resources better with
	 * try-using-finally
	 */
	private void processFiles() {
		try {
			for (File file : this.inbox.listFiles(filter)) {
				FileChannel channel = new RandomAccessFile(file, "rw")
						.getChannel();
				try {
					FileLock lock = channel.tryLock();
					lock.release();
					try {
						this.processOneFile(file);
					} catch (Exception e) {
						System.err.println("Error while processing "
								+ file.getName() + "\n" + e.getMessage()
								+ "\n moving on to next file");
					}
					File newFile = new File(file.getPath() + BACK);
					if (file.renameTo(newFile) == false) {
						Tracer.trace("Sorry, unable to rename the file to "
								+ newFile.getAbsolutePath()
								+ " going to delete it.");
						if (file.delete() == false) {
							Tracer.trace(
									"Unable to delete the file "
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
			}
		} catch (Exception e) {
			Tracer.trace(e, "Error while working on the folder");
		}
	}

	/**
	 * @param file
	 * @throws IOException
	 */
	private boolean processOneFile(File file) throws IOException {
		Tracer.trace("Processing " + file.getAbsolutePath() + "....");
		Record record = ComponentManager.getRecord(RECORD_NAME);
		FileReader fr = new FileReader(file);
		BufferedReader reader = new BufferedReader(fr);
		List<FormattedMessage> errors = new ArrayList<FormattedMessage>();
		DataSheet ds = record.fromFlatFile(reader, errors, true);
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

		ServiceData inData = new ServiceData(USER_ID, SERVICE_NAME);
		inData.put(SHEET_NAME, ds);
		ServiceData outData = ServiceAgent.getAgent().executeService(inData);
		if (outData.hasErrors()) {
			for (FormattedMessage msg : outData.getMessages()) {
				System.err.println(msg.text);
			}
			return false;
		}
		String serviceTrace = outData.getTrace();
		if (serviceTrace != null) {
			Tracer.trace("************* begin service log *****************");
			Tracer.trace(serviceTrace);
			Tracer.trace("************* end service log *****************");
		}
		Tracer.trace("Done");
		return true;
	}

	/**
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String compFolder = "D:/Workspace/mm_simplity/examples/wbBatch/src/main/resources/comp/";
		String inFolder = null;
		long interval = 0;
		if (args.length == 4) {
			compFolder = args[0];
			inFolder = args[1];
			try {
				interval = Long.parseLong(args[2]);
			} catch (Exception e) {
				System.out.println(args[2] + " is not a valid whole number");
				return;
			}
		} else if (args.length != 0) {
			printUsage();
		}
		Application.bootStrap(compFolder);
		start(inFolder, interval);
		System.out.println(
				"Process started. Press any key to stop the process >");
		System.in.read();
		stop();
		System.out.println("Bye");
		return;
	}

	private static void printUsage() {
		System.out.println(
				"usage : org.simplity.examples.FlatFileAgent [compPath] [inPath] [outpath] [interval]"
						+ "\nEither all four parameters are given in that or none, in which case we use default"
						+ "\ncompPath - root folder of your components where we find application.xml"
						+ "\ninPath - folder where we find *.txt files to be processed"
						+ "\noutPath - folder where processed files are moved as *.txt.back"
						+ "\ninterval - milliseconds to wait after we finish one round of processing"
						+ "\nyou may keep copying files into the folder to simulte ftp, "
						+ "\nPress any key to interrupt and come out");

	}
}
