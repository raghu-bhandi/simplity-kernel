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
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * A job that is added to a scheduler. manages running jobs for the job that is
 * scheduled
 *
 * @author simplity.org
 *
 */
public interface ScheduledJob {
	/**
	 * schedule running jobs using the executor
	 * @param executor
	 */
	public void schedule(ScheduledThreadPoolExecutor executor);

	/**
	 * @param executor
	 */
	public void shutDown(ScheduledThreadPoolExecutor executor);
	/**
	 * add another thread to this job. ignored if this is a batch job, or if there is only one thread at this time
	 * @param executor
	 */
	public void incrmentThread(ScheduledThreadPoolExecutor executor);

	/**
	 * reduce a thread from this job. ignored if this is a batch job, or if there is only one thread at this time
	 *
	 * @param executor
	 */
	public void decrmentThread(ScheduledThreadPoolExecutor executor);

	/**
	 * add status of running jobs into the list
	 * @param infoList
	 */
	public void putStatus(List<RunningJobInfo> infoList);
}
