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

package org.simplity.job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.simplity.kernel.Application;
import org.simplity.kernel.Messages;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.AbstractService;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

/**
 * Example of a class that implements a full service
 *
 * @author simplity.org
 *
 */
public class JobsManager extends AbstractService implements LogicInterface {
	private static final String MY_NAME = "JobsManager";

	/*
	 * field names
	 */
	private static final String ACTION = "JobsAction";
	private static final String JOB_NAME = "jobName";
	private static final String JOBS_NAME = "JobsName";
	private static final String INTERVAL = "interval";
	private static final String NBR_THREADS = "nbrThreads";
	private static final String SERVICE_NAME = "serviceName";
	private static final String RUN_AT_THESE_TIMES = "runAtTheseTimes";
	/*
	 * valid actions/commands
	 */
	private static final String START = "start";
	private static final String STOP = "stop";
	private static final String INCR = "incr";
	private static final String DECR = "decr";
	private static final String STATUS = "status";
	private static final String CANCEL = "cancel";
	private static final String SCHEDULE = "schedule";
	private static final String NEW = "new";
	private static final String USAGE = "USAGE\nstart JobsName  : start a pre-defined Jobs\n"
			+ "start : start a blank Jobs to which you can add jobs with action=new\n"
			+ "stop : stop and unload current Jobs\n" + "status : get status of all jobs\n"
			+ "status jobName : get status of this job\n"
			+ "cancel jobName : cancel/stop/unschedule/interrupt this job\n "
			+ "new jobName <interval> <nbrThreads> <runAtTheseTimes> : (provide one of the three params) add a new job and schedule it."
			+ "schedule jobName : reschedule a job that was previousy cancelled";

	/*
	 * others
	 */
	private static final String SHEET_NAME = "jobsInfo";
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
	public Value executeAsAction(ServiceContext ctx, DbDriver driver, boolean transactionIsdelegated) {

		String action = ctx.getTextValue(ACTION);
		if (action == null || action.isEmpty()) {
			ctx.addMessage(Messages.ERROR, USAGE);
			return Value.VALUE_FALSE;
		}
		String JobsName = ctx.getTextValue(JOBS_NAME);
		/*
		 * we make use of the default input-output in AbstractService and put
		 * our logic here, so that we can also be called by other services
		 */
		Jobs jobs = Jobs.getCurrentInstance();

		/*
		 * when no Jobs is running
		 */
		if (jobs == null) {
			if (action.equals(START)) {
				if (JobsName == null || JobsName.isEmpty()) {
					jobs = Jobs.startEmptyJobs();
				} else {
					jobs = Jobs.startJobs(JobsName);
				}
				if (jobs == null) {
					ctx.addMessage(Messages.ERROR, "Jobs could not be started. Look at logs for more details..");
				} else {
					ctx.addMessage(Messages.SUCCESS, "Jobs started");
				}
			} else {
				if (action.equals(STATUS) == false) {
					ctx.addMessage(Messages.ERROR, "No Jobs is running. Use start action to start  a Jobs.");
				}
			}

		} else if (action.equals(STATUS) == false) {
			this.takeAction(jobs, action, ctx);
			if (action.equals(STOP)) {
				jobs = null;
			}
		}

		if (jobs == null) {
			ctx.removeValue(JOBS_NAME);
		} else {
			ctx.setTextValue(JOBS_NAME, jobs.getQualifiedName());
			RunningJobInfo[] infoList = jobs.getStatus();
			DataSheet ds = RunningJobInfo.toDataSheet(infoList);
			ctx.putDataSheet(SHEET_NAME, ds);
		}
		/*
		 * we always provide status
		 */
		return null;

	}

	private void takeAction(Jobs Jobs, String action, ServiceContext ctx) {
		String jobName = ctx.getTextValue(JOB_NAME);
		if (action.equals(START)) {
			ctx.addMessage(Messages.ERROR, "A scheduler is running. Can not start another one");
			return;
		}

		if (action.equals(STOP)) {
			ctx.addMessage(Messages.INFO, "Initiated shutdown for job " + jobName);
			Jobs.stopJobs();
			ctx.removeValue(JOBS_NAME);
			return;
		}

		if (jobName == null) {
			ctx.addMessage(Messages.VALUE_REQUIRED, JOB_NAME);
			return;
		}

		if (action.equals(CANCEL)) {
			Jobs.cancelJob(jobName);
			ctx.addMessage(Messages.INFO, "Initiated shutdown for job " + jobName);
			return;
		}
		if (action.equals(INCR)) {
			Jobs.incrmentThread(jobName);
			ctx.addMessage(Messages.INFO, "Initiated addition of a thread for job " + jobName);
			return;
		}
		if (action.equals(DECR)) {
			Jobs.decrmentThread(jobName);
			ctx.addMessage(Messages.INFO, "Initiated stopping of a thread for job " + jobName);
			return;
		}
		if (action.equals(SCHEDULE)) {
			Jobs.reschedule(jobName);
			ctx.addMessage(Messages.INFO, "Initiated stestart of job " + jobName);
			return;
		}

		if (action.equals(NEW)) {
			String serviceName = ctx.getTextValue(SERVICE_NAME);
			if (serviceName == null) {
				ctx.addMessage(Messages.VALUE_REQUIRED, SERVICE_NAME);
				return;
			}
			String times = ctx.getTextValue(RUN_AT_THESE_TIMES);
			int nbrThreads = (int) ctx.getLongValue(NBR_THREADS);
			int interval = (int) ctx.getLongValue(INTERVAL);
			Job job = new Job(jobName, serviceName, interval, nbrThreads, times);
			job.getReady();
			ctx.addMessage(Messages.INFO, "added job " + jobName + " to scheduler..");
			Jobs.scheduleJob(job);
			return;
		}
		ctx.addMessage(Messages.ERROR, action + " is an invalid action " + USAGE);
		return;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.LogicInterface#execute(org.simplity.service.
	 * ServiceContext)
	 */
	@Override
	public Value execute(ServiceContext ctx) {
		return this.executeAsAction(ctx, null, false);
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
				getInput(reader, JOBS_NAME, ctx, false);
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
					getInput(reader, RUN_AT_THESE_TIMES, ctx, false);
				}
			}
			manager.executeAsAction(ctx, null, false);

			MultiRowsSheet ds = (MultiRowsSheet) ctx.getMessagesAsDs();
			if (ds.length() > 0) {
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
