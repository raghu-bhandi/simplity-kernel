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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import org.simplity.kernel.Application;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.util.XmlParseException;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.Value;

/**
 * list of all jobs to be managed as batch/background services.
 *
 * @author simplity.org
 *
 */
public class Jobs {

	private static Jobs myInstance;

	/**
	 *
	 * @return current instance that can be used for managing running jobs
	 */
	public static Jobs getCurrentInstance() {
		return myInstance;
	}

	/**
	 * start an empty one. Jobs may be added later..
	 * @return instance of jobs that can be used for managing running jobs
	 */
	public static Jobs startEmptyScheduler() {
		return load(null);
	}

	/**
	 * schedule jobs from a non-default resource
	 *
	 * @param jobsName
	 *            name of the jobs resource under Jobs folder to be used.
	 *
	 * @return instance of jobs that can be used for managing running jobs
	 */

	public static Jobs ScheduleJobs(String jobsName) {
		return load(jobsName);
	}

	/**
	 * stop the scheduler after bringing down all running jobs
	 */
	public static void stopScheduler(){
		if(myInstance != null){
			myInstance.stop();
			myInstance = null;
		}
	}
	private static Jobs load(String jobsName) {
		if (myInstance != null) {
			throw new ApplicationError(
					"Jobs are already running. Bring them down before re-running, or incrmentally add ad-hoc jobs");
		}
		myInstance = new Jobs();
		if(jobsName == null){
			myInstance.jobs = new Job[0];
			myInstance.maxThreads = 100;
			myInstance.name = "dummy";
		}else{
			String fileName = ComponentManager.getComponentFolder() + "jobs/" + jobsName + ".xml";
			try {
				XmlUtil.xmlToObject(fileName, myInstance);
			} catch (XmlParseException e) {
				throw new ApplicationError("Resource " + fileName + " has syntax errors.");
			}
			if (myInstance.name == null || myInstance.name.equals(jobsName) == false) {
				throw new ApplicationError(
						"You must follow a naming convention where name matches the file in which it is saved.");
			}
		}
		myInstance.execute();
		return myInstance;
	}

	/**
	 * name of this batch jobs. Should match the file name.
	 */
	String name;

	/**
	 * maximum number of threads to be utilized for running all the jobs.
	 */
	int maxThreads;

	/**
	 * in case you want to control the parameters with which threads are to be
	 * created
	 */
	String threadFactory;

	/**
	 * default user id
	 */
	String defaultUserId;

	/**
	 * jobs to be executed
	 */
	Job[] jobs;

	private ScheduledThreadPoolExecutor executor;

	private Map<String, ScheduledJob> scheduledJobs = new HashMap<String, ScheduledJob>();

	/**
	 *
	 */
	public void execute() {
		this.getReady();
		if (this.executor != null) {
			throw new ApplicationError(
					"Jobs are already getting executed while another attempt is being made to execute them.");
		}
		if (this.threadFactory != null) {
			ThreadFactory tf = null;
			try {
				tf = (ThreadFactory) Class.forName(this.threadFactory).newInstance();
			} catch (Exception e) {
				throw new ApplicationError(e,
						" Error while instantiating ThreadFactory from class " + this.threadFactory);
			}
			this.executor = new ScheduledThreadPoolExecutor(this.maxThreads, tf);
		} else {
			this.executor = new ScheduledThreadPoolExecutor(this.maxThreads);
		}
		/*
		 * we want jobs to run only when the executor is active. That is, executor is not just a submitter, but manager
		 */
		this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		this.executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
		this.executor.setRemoveOnCancelPolicy(true);
		Value userId = this.getUserId();
		for (Job job : this.jobs) {
			ScheduledJob sj = job.getScheduledJob(userId);
			this.scheduledJobs.put(job.name, sj);
			sj.schedule(this.executor);
		}
	}

	/**
	 * bring down all running jobs and shutdown the scheduler
	 *
	 */
	public void stop() {
		this.cancelAll();
		this.executor.shutdownNow();
	}

	/**
	 * gracefully stop a job
	 *
	 * @param jobName
	 */
	public void cancelJob(String jobName) {
		if(jobName == null){
			this.cancelAll();
			return;
		}
		ScheduledJob job = this.scheduledJobs.get(jobName);
		if (job == null) {
			Tracer.trace("No job named " + jobName);
			return;
		}
		job.shutDown(this.executor);
	}

	/**
	 * @param jobName
	 */
	public void reschedule(String jobName) {
		if(jobName == null){
			Tracer.trace("No job name specified for rescheduling");
			return;
		}
		ScheduledJob job = this.scheduledJobs.get(jobName);
		if (job == null) {
			Tracer.trace("No job named " + jobName);
			return;
		}
		job.schedule(this.executor);
	}
	/**
	 * gracefully stop all jobs
	 */
	public void cancelAll() {
		for (ScheduledJob job : this.scheduledJobs.values()) {
			job.shutDown(this.executor);
		}
		this.scheduledJobs.clear();
	}

	/**
	 * force job to shut down. Job is not given an option to quit on its own
	 *
	 * @param jobName
	 */
	public void forceShutDown(String jobName) {
		if(jobName == null){
			this.forceShutDownAll();
			return;
		}
		ScheduledJob job = this.scheduledJobs.get(jobName);
		if (job == null) {
			Tracer.trace("No job named " + jobName);
			return;
		}
		job.shutDown(this.executor);
	}

	/**
	 * force jobs to shut down. Jobs are not given an option to quit on their
	 * own
	 */
	public void forceShutDownAll() {
		for (ScheduledJob job : this.scheduledJobs.values()) {
			job.shutDown(this.executor);
		}
		this.scheduledJobs.clear();
	}

	/**
	 * add another thread to this job. ignored if this is a batch job
	 *
	 * @param jobName
	 */
	public void incrmentThread(String jobName) {
		ScheduledJob job = this.scheduledJobs.get(jobName);
		if (job == null) {
			Tracer.trace("No job named " + jobName);
			return;
		}
		job.incrmentThread(this.executor);
	}

	/**
	 * reduce a thread from this job. ignored if this is a batch job, or if
	 * there is only one thread at this time
	 *
	 * @param jobName
	 */
	public void decrmentThread(String jobName) {
		ScheduledJob job = this.scheduledJobs.get(jobName);
		if (job == null) {
			Tracer.trace("No job named " + jobName);
			return;
		}
		job.decrmentThread(this.executor);
	}

	/**
	 * get status of all running jobs
	 *
	 * @return status for all running jobs
	 */
	public RunningJobInfo[] getStatus() {
		List<RunningJobInfo> infoList = new ArrayList<RunningJobInfo>();
		for (ScheduledJob job : this.scheduledJobs.values()) {
			job.putStatus(infoList);
		}
		return infoList.toArray(new RunningJobInfo[0]);
	}

	/**
	 *
	 * @param jobName
	 * @return status info for this job
	 */
	public RunningJobInfo[] getStatus(String jobName) {
		RunningJobInfo[] inf = new RunningJobInfo[0];
		ScheduledJob job = this.scheduledJobs.get(jobName);
		if (job == null) {
			Tracer.trace("No job named " + jobName);
			return inf;
		}
		List<RunningJobInfo> infoList = new ArrayList<RunningJobInfo>();
		job.putStatus(infoList);
		return infoList.toArray(inf);
	}

	/**
	 * ad-hoc running of a job.
	 *
	 * @param job
	 *            to be also run. Ensure that its name should not clash with
	 *            existing jobs
	 * @return error message if add failed. null if all OK.
	 */
	public String scheduleJob(Job job) {
		if (this.scheduledJobs.containsKey(job.name)) {
			return ("Job named " + job.name
					+ " is already running. Choose a different name for your job if you insist on running it");
		}
		ScheduledJob sjob = job.getScheduledJob(this.getUserId());
		sjob.schedule(this.executor);
		this.scheduledJobs.put(job.name, sjob);
		return null;

	}

	/*
	 * get default user id
	 */
	private Value getUserId() {
		if (this.defaultUserId != null) {
			if (Application.userIdIsNumeric()) {
				try {
					return Value.newIntegerValue(Long.parseLong(this.defaultUserId));
				} catch (Exception e) {
					throw new ApplicationError(e,
							" Jobs has specified a non-numeric defaultUserId while application.xml states that userId is to be numeric");
				}
			}
			return Value.newTextValue(this.defaultUserId);
		}
		Value userId = Application.getDefaultUserId();
		if (userId != null) {
			return userId;
		}
		Tracer.trace(
				"Default User Id is not specified either at app level or at Jobs level. If they are indeed required in a service, and the job has not speicified, we will end up using a dummy value of 100");
		if (Application.userIdIsNumeric()) {
			return Value.newIntegerValue(100);
		}
		return Value.newTextValue("100");
	}

	/**
	 *
	 */
	public void getReady() {
		for (Job job : this.jobs) {
			job.getReady();
		}

	}
}
