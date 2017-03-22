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

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.simplity.kernel.Tracer;
import org.simplity.kernel.value.Value;

/**
 * A job that is added to a scheduler. manages running jobs for the job that is
 * scheduled
 *
 * @author simplity.org
 *
 */
public class BatchJob implements ScheduledJob {

	private final Job scheduledJob;
	private Value userId;
	private RunningJob runningJob;
	private boolean isScheduled;
	private ScheduledFuture<?> future;

	BatchJob(Job job, Value userId) {
		this.scheduledJob = job;
		this.userId = userId;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.job.ScheduledJob#schedule(java.util.concurrent.
	 * ScheduledThreadPoolExecutor)
	 */
	@Override
	public void schedule(ScheduledThreadPoolExecutor executor) {
		if (this.isScheduled) {
			Tracer.trace(this.scheduledJob.name + " is already scheduled");
		}
		this.runningJob = this.scheduledJob.createRunningJob(this.userId);
		this.future = executor.scheduleAtFixedRate(this.runningJob, 0, this.scheduledJob.runInterval, TimeUnit.SECONDS);
		this.isScheduled = true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.job.ScheduledJob#shutDownGracefully(java.util.concurrent.
	 * ScheduledThreadPoolExecutor)
	 */
	@Override
	public void shutDown(ScheduledThreadPoolExecutor executor) {
		this.future.cancel(true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.job.ScheduledJob#incrmentThread(java.util.concurrent.
	 * ScheduledThreadPoolExecutor)
	 */
	@Override
	public void incrmentThread(ScheduledThreadPoolExecutor executor) {
		this.noChange();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.job.ScheduledJob#decrmentThread(java.util.concurrent.
	 * ScheduledThreadPoolExecutor)
	 */
	@Override
	public void decrmentThread(ScheduledThreadPoolExecutor executor) {
		this.noChange();
	}

	private void noChange() {
		Tracer.trace("Job " + this.scheduledJob.name + " is a batch, and hence we can not add/remove thread");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.job.ScheduledJob#putStatus(java.util.List)
	 */
	@Override
	public void putStatus(List<RunningJobInfo> infoList) {
		JobStatus sts;
		if(this.future.isCancelled()){
			sts = JobStatus.CANCELLED;
		}else{
			sts = this.runningJob.jobStatus;
		}
		RunningJobInfo info = new RunningJobInfo(this.scheduledJob.name, this.scheduledJob.serviceName, sts, 0, "");
		infoList.add(info);
	}
}
