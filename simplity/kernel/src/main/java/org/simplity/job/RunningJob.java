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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.simplity.kernel.Application;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.MessageBox;

import org.simplity.service.PayloadType;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceInterface;

/**
 * a thread that runs a service with given input data
 *
 * @author simplity.org
 */
public class RunningJob implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(RunningJob.class);

  private final ServiceInterface service;
  private final ServiceData inData;
  private final MessageBox messageBox;
  private JobStatus jobStatus = JobStatus.SCHEDULED;

  /**
   * create a job thread to run a service with the input data
   *
   * @param service to be run as part of this job
   * @param inData input data to the service
   */
  public RunningJob(ServiceInterface service, ServiceData inData) {
    this.service = service;
    this.inData = inData;
    this.messageBox = new MessageBox();
    this.inData.setMessageBox(this.messageBox);
  }

  @Override
  public void run() {
    /*
     * remember : this is the thread that would run as long as the service
     * wants it.The service may be designed to run for ever, or it may be
     * a batch job and return once known set of work is finished. It is
     * possible that getJobStatus() be invoked from another thread, and
     * hence we keep that field updated
     */
    this.jobStatus = JobStatus.RUNNING;

    logger.info("Job status set to  " + this.jobStatus);

    try {
      this.service.respond(this.inData, PayloadType.JSON);

      logger.info("Service " + this.service.getQualifiedName() + " is done..");

      this.jobStatus = JobStatus.DONE;

      logger.info("Reset to  " + this.jobStatus);

    } catch (Exception e) {
      this.jobStatus = JobStatus.FAILED;
      String msg =
          "Error while running service " + this.service.getQualifiedName() + " as a batch job.";
      Application.reportApplicationError(this.inData, new ApplicationError(e, msg));
    }
  }

  /** @return current job status */
  public JobStatus getJobStatus() {
    return this.jobStatus;
  }
  /** @return current job status */
  public String getServiceStatus() {
    Object msg = this.messageBox.getMessage();
    if (msg == null) {
      return "unknown";
    }
    return msg.toString();
  }
}
