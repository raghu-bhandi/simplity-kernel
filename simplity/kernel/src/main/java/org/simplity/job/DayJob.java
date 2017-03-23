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

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.simplity.kernel.Tracer;
import org.simplity.kernel.value.Value;

/**
 * A job that is added to a scheduler. manages running jobs for the job that is
 * scheduled
 *
 * @author simplity.org
 *
 */
public class DayJob implements ScheduledJob {
	private static final int MINUTES = 24 * 60;

	private final Job scheduledJob;
	private final Value userId;
	/*
	 * number of elapsed minute for the day, sorted asc
	 */
	private final int[] timesOfDay;

	/*
	 * state attributes
	 */
	private RunningJob runningJob;
	private ScheduledThreadPoolExecutor scheduleExecutor;
	private Future<?> future;
	/*
	 * which is the next run-time?
	 */
	private int nextIdx;

	DayJob(Job job, Value userId, int[] times) {
		this.scheduledJob = job;
		this.userId = userId;
		this.timesOfDay = times;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.job.BatchJob#schedule(java.util.concurrent.
	 * ScheduledThreadPoolExecutor)
	 */
	@Override
	public boolean schedule(ScheduledThreadPoolExecutor executor) {
		if (this.scheduleExecutor != null) {
			Tracer.trace(this.scheduledJob.name + " is already scheduled");
			return false;
		}
		/*
		 * we schedule it into our own scheduler, but not to executor.
		 * submitted to executor based on polling
		 */
		this.runningJob = this.scheduledJob.createRunningJob(this.userId);
		this.scheduleExecutor = executor;
		this.nextIdx = this.getTimeIdx();
		return true;
	}

	private int getTimeIdx() {
		Calendar cal = Calendar.getInstance();
		int minutes = cal.get(Calendar.HOUR_OF_DAY) * cal.get(Calendar.MINUTE);
		for (int i = 0; i < this.timesOfDay.length; i++) {
			if (this.timesOfDay[i] > minutes) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * submit a job if it is time to do so.
	 *
	 * @param minutes
	 * @return number of minutes remaining for the next run
	 */
	@Override
	public int poll(int minutes) {
		if (this.scheduleExecutor == null) {
			/*
			 * probably cancelled..
			 */
			return NEVER;
		}
		if (this.nextIdx < 0) {
			/*
			 * last job for day is done, and waiting for morning..
			 *
			 * is it morning?
			 */
			int n = this.timesOfDay[0] - minutes;
			if (n >= 0) {
				this.nextIdx = 0;
				return n;
			}
			return MINUTES + n;
		}
		int n = minutes - this.timesOfDay[this.nextIdx];
		if (n > 0) {
			return n;
		}
		/*
		 * time has come to submit
		 */
		boolean submitted = this.submit();
		if (submitted == false) {
			/*
			 * most probably previous run is active. Let us retry after a minute
			 */
			return 1;
		}

		/*
		 * move to next run-time
		 */
		this.nextIdx++;
		if (this.nextIdx == this.timesOfDay.length) {
			/*
			 * done for the day.
			 */
			this.nextIdx = -1;
			return MINUTES - minutes + this.timesOfDay[0];
		}
		return this.timesOfDay[this.nextIdx] - minutes;
	}

	private boolean submit() {
		if (this.future != null && this.future.isCancelled() == false && this.future.isDone() == false) {
			Tracer.trace("Job " + this.scheduledJob.name
					+ " is still running when it is time to run it again.. Will wait for next poll");
			return false;
		}

		this.future = this.scheduleExecutor.submit(this.runningJob);
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.job.ScheduledJob#shutDownGracefully(java.util.concurrent.
	 * ScheduledThreadPoolExecutor)
	 */
	@Override
	public void cancel(ScheduledThreadPoolExecutor executor) {
		this.future.cancel(true);
		this.scheduleExecutor = null;
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
		if (this.scheduleExecutor == null) {
			sts = JobStatus.CANCELLED;
		} else if (this.future == null) {
			sts = JobStatus.SCHEDULED;
		} else if (this.future.isCancelled()) {
			sts = JobStatus.CANCELLED;
		} else {
			sts = this.runningJob.jobStatus;
		}
		RunningJobInfo info = new RunningJobInfo(this.scheduledJob.name, this.scheduledJob.serviceName, sts, 0, "");
		infoList.add(info);
	}
}
