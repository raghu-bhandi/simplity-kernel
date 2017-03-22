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

import org.simplity.service.ServiceData;

/**
 * scheduler manages jobs
 *
 * @author simplity.org
 *
 */
public interface Scheduler {
	/**
	 * job has completed normally. call-back from the running job after the
	 * service returns normally.
	 *
	 * @param job
	 *            that is done
	 * @param outData
	 *            output data from the job
	 */
	public void jobDone(RunningJob job, ServiceData outData);

	/**
	 * job has failed. call-back from a running job when the service fails.
	 *
	 * @param job
	 * @param message
	 */
	public void jobFailed(RunningJob job, String message);

	/**
	 * schedule a job. Once a job is scheduled, scheduler takes care of running
	 * the job as per specifications of the job
	 *
	 * @param jobName
	 *            unique job name
	 */
	public void schedule(String jobName);

	/**
	 * If this job is currently running, running jobs are requested to shutdown
	 * gracefully. That is, job is asked to shutdown, and it is up to the
	 * running job to respond that that request.
	 *
	 * @param jobName
	 *            unique job name
	 */
	public void shutDown(String jobName);

	/**
	 * Emergency shut-down. Running jobs for this job are terminated with no
	 * notice.
	 *
	 * @param jobName
	 *            unique job name
	 */
	public void forceDown(String jobName);

	/**
	 * schedule a job. Once a job is scheduled, scheduler takes care of running
	 * the job as per specifications of the job
	 *
	 * @param jobName
	 *            unique job name
	 * @return status of job. If it is Failed, it may be run again if this job
	 *         is to be run periodically. It will not be run if it is marked to
	 *         be run n the background
	 */
	public JobStatus getStatus(String jobName);
}
