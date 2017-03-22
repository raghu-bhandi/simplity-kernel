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

package org.simplity.ide;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.simplity.job.Job;
import org.simplity.job.Jobs;
import org.simplity.job.RunningJobInfo;
import org.simplity.kernel.Application;
import org.simplity.kernel.Messages;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.AbstractService;
import org.simplity.service.ServiceContext;

/**
 * Example of a class that implements a full service
 *
 * @author simplity.org
 *
 */
public class JobsManager extends AbstractService {
	private static final String MY_NAME = "JobsManager";

	/*
	 * field names
	 */
	private static final String ACTION = "jobAction";
	private static final String JOB_NAME = "jobName";
	private static final String BATCH_NAME = "batchName";
	private static final String INTERVAL = "interval";
	private static final String NBR_THREADS = "nbrThreads";
	private static final String SERVICE_NAME = "serviceName";
	/*
	 * valid actions/commands
	 */
	private static final String START = "start";
	private static final String STOP = "stop";
	private static final String INCR = "incr";
	private static final String DECR = "decr";
	private static final String STATUS = "status";
	private static final String CANCEL = "cancel";
	private static final String RESTART = "restart";
	private static final String NEW = "new";

	/*
	 * others
	 */
	private static final String SHEET_NAME = "info";
	private static final String FIELD_SEP = ", ";
	private static final String ROW_SEP_SEP = "\n";

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getSimpleName()
	 */
	@Override
	public String getSimpleName() {
		return MY_NAME;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getQualifiedName()
	 */
	@Override
	public String getQualifiedName() {
		return MY_NAME;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.service.ServiceInterface#executeAsAction(org.simplity.
	 * service
	 * .ServiceContext, org.simplity.kernel.db.DbDriver)
	 */
	@Override
	public Value executeAsAction(ServiceContext ctx, DbDriver driver, boolean transactionIsdelegated ) {

		String action = ctx.getTextValue(ACTION);
		String batchName = ctx.getTextValue(BATCH_NAME);
		String jobName = ctx.getTextValue(JOB_NAME);
		/*
		 * we make use of the default input-output in AbstractService and put
		 * our logic here, so that we can also be called by other services
		 */
		Jobs jobs = Jobs.getCurrentInstance();

		/*
		 * when no batch is running
		 */
		if (jobs == null) {
			if (action == null || action.equals(START) == false) {
				ctx.addMessage(Messages.ERROR, "No batch is running. Use start action to start  a batch.");
				return Value.VALUE_FALSE;
			}
			if (batchName == null) {
				jobs = Jobs.startEmptyScheduler();
			} else {
				jobs = Jobs.ScheduleJobs(batchName);
			}
			if (jobs == null) {
				ctx.addMessage(Messages.ERROR, "Batch could not be started. Look at logs for mre details..");
				return Value.VALUE_FALSE;
			}
			ctx.addMessage(Messages.SUCCESS, "Batch started");
			return Value.VALUE_TRUE;
		}
		/*
		 * default action is get status
		 */
		if (action == null || action.equals(STATUS)) {
			RunningJobInfo[] infoList = jobs.getStatus(jobName);
			DataSheet ds = RunningJobInfo.toDataSheet(infoList);
			ctx.putDataSheet(SHEET_NAME, ds);
			return Value.VALUE_TRUE;
		}

		/*
		 * start
		 */
		if (action.equals(START)) {
			ctx.addMessage(Messages.ERROR, "A scheduler is running. Can not start another one");
			return Value.VALUE_FALSE;
		}
		/*
		 * stop
		 */
		if (action.equals(STOP)) {
			ctx.addMessage(Messages.INFO, "Initiated shutdown for job " + jobName);
			Jobs.stopScheduler();
			return Value.VALUE_TRUE;
		}
		/*
		 * cancel
		 */
		if (action.equals(CANCEL)) {
			if (jobName == null) {
				ctx.addMessage(Messages.INFO,
						"Initiated shutdown of all jobs. Note that the batch would still be active. use stop before using next start.");
				jobs.cancelAll();
			} else {
				ctx.addMessage(Messages.INFO, "Initiated shutdown for job " + jobName);
				jobs.cancelJob(jobName);
			}
			return Value.VALUE_TRUE;
		}
		/*
		 * we need jobName for all command from here..
		 */
		if (jobName == null) {
			ctx.addMessage(Messages.VALUE_REQUIRED, JOB_NAME);
			return Value.VALUE_FALSE;
		}

		if (action.equals(INCR)) {
			ctx.addMessage(Messages.INFO, "Initiated addition of a thread for job " + jobName);
			jobs.incrmentThread(jobName);
			return Value.VALUE_TRUE;
		}

		/*
		 * decrement
		 */
		if (action.equals(DECR)) {
			ctx.addMessage(Messages.INFO, "Initiated stopping of a thread for job " + jobName);
			jobs.decrmentThread(jobName);
			return Value.VALUE_TRUE;
		}

		/*
		 * decrement
		 */
		if (action.equals(RESTART)) {
			ctx.addMessage(Messages.INFO, "Initiated stestart of job " + jobName);
			jobs.reschedule(jobName);
			return Value.VALUE_TRUE;
		}

		/*
		 * decrement
		 */
		if (action.equals(NEW)) {
			String serviceName = ctx.getTextValue(SERVICE_NAME);
			if (serviceName == null) {
				ctx.addMessage(Messages.VALUE_REQUIRED, SERVICE_NAME);
				return Value.VALUE_FALSE;
			}
			int nbrThreads = (int) ctx.getLongValue(NBR_THREADS);
			int interval = (int) ctx.getLongValue(INTERVAL);
			Job job = new Job(jobName, serviceName, interval, nbrThreads);
			job.getReady();
			ctx.addMessage(Messages.INFO, "added job " + jobName + " to scheduler..");
			jobs.scheduleJob(job);
			return Value.VALUE_TRUE;
		}

		ctx.addMessage(Messages.INVALID_VALUE, ACTION, action);
		return Value.VALUE_FALSE;
	}

	/**
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String path = "c:/repos/simplity/test/WebContent/WEB-INF/comp/";
		if (args.length > 0) {
			path = args[0];
		}
		Application.bootStrap(path);
		System.out.println("Probably start is the best way to start :-)");
		String action = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		JobsManager manager = new JobsManager();
		ServiceContext ctx;
		String sn = "unknown";
		Value userId = Value.newTextValue("420");
		while (true) {
			System.out.print(ACTION + "[enter to exit] :");
			action = reader.readLine();
			if (action == null) {
				System.out.print("Bye..");
				break;
			}
			action.trim();
			if (action.isEmpty()) {
				System.out.print("Bye..");
				break;
			}
			System.out.println("Going to process action " + action);
			ctx = new ServiceContext(sn, userId);
			ctx.setTextValue(ACTION, action);
			action = action.toLowerCase();
			if (action.equals(START)) {
				getInput(reader, BATCH_NAME, ctx, false);
			} else if (action.equals(STATUS)) {
				getInput(reader, JOB_NAME, ctx, false);
			} else if (action.equals(STOP)) {
				//
			} else {
				getInput(reader, JOB_NAME, ctx, false);

				if (action.equals(NEW)) {
					getInput(reader, SERVICE_NAME, ctx, false);
					getInput(reader, INTERVAL, ctx, true);
					getInput(reader, NBR_THREADS, ctx, true);
				}
			}
			manager.executeAsAction(ctx, null, false);

			MultiRowsSheet ds = (MultiRowsSheet) ctx.getMessagesAsDS();
			if(ds.length() > 0){
				System.out.println("Messages");
				System.out.println(ds.toString(FIELD_SEP, ROW_SEP_SEP));
			}
			ds = (MultiRowsSheet) ctx.getDataSheet(SHEET_NAME);
			if (ds != null && ds.length() > 0) {
				System.out.println("Status");
				System.out.println(ds.toString(FIELD_SEP, ROW_SEP_SEP));
			}
		}
	}

	private static void getInput(BufferedReader reader, String name, ServiceContext ctx, boolean isNumber)
			throws IOException {
		System.out.print(name + "[enter to skip] :");
		String value = reader.readLine();
		if (value == null) {
			return;
		}
		value = value.trim();
		if (value.isEmpty()) {
			return;
		}

		if (isNumber == false) {
			ctx.setTextValue(name, value);
			return;
		}
		long n = 0;
		try {
			n = Long.parseLong(value);
		} catch (Exception e) {
			System.out.println(value + " is an invalid number. 0 assumed.");
		}
		ctx.setLongValue(name, n);
	}

}
