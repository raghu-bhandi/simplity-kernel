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

import java.util.Arrays;

import org.simplity.kernel.Application;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceInterface;

/**
 * @author simplity.org
 *
 */
public class Job {
	/**
	 * name of the job, unique within a jobs collection
	 */
	String name;

	/**
	 * service to be run as a job
	 */
	String serviceName;
	/**
	 * this job is to be fired at these times on a 24 hour clock.
	 */
	String[] runAtTheseTimes;
	/**
	 * this job is to be run every so many seconds
	 */
	int runInterval;

	/**
	 * is this a job that runs for ever left to itself? In that case specify
	 * number of such instances. In this case, runInterval and runAtTheseTimes
	 * are ignored.
	 */
	int nbrDedicatedThreads;

	/**
	 * parameters that this service expects as input are supplied with this
	 * mechanism
	 */
	String inputJson;

	/**
	 * if this job is to be fired with a specific userId. Defaults to scheduler
	 * level setting
	 */
	String userId;

	/**
	 * input fields
	 */
	InputField[] inputFields;
	/**
	 * cached during getReady();
	 */
	private Value userIdValue;

	/**
	 * number of minutes elapsed for the day
	 */
	private int[] timesOfDay;


	/**
	 * @param jobName
	 * @param service
	 * @param interval
	 * @param nbrThreads
	 */
	public Job(String jobName, String service, int interval, int nbrThreads) {
		this.name = jobName;
		this.serviceName = service;
		this.runInterval = interval;
		this.nbrDedicatedThreads = nbrThreads;
	}

	/**
	 *
	 */
	public Job(){
		//default
	}
	/**
	 *
	 */
	public void getReady() {
		if(this.runInterval > 0 && this.nbrDedicatedThreads > 0){
			throw new ApplicationError("Job " + this.name + " has set both runInterval and nbrDedicatedThreads. You shoudl specify one of them : either to run as batch every so often, or as a background job");
		}
		if(this.runInterval == 0 && this.nbrDedicatedThreads == 0){
			Tracer.trace("Job " + this.name + " will be run once");
			this.nbrDedicatedThreads = 1;
		}
		if(this.userId != null){
			if(Application.userIdIsNumeric()){
				try{
					this.userIdValue = Value.newIntegerValue(Long.parseLong(this.userId));
				}catch(Exception e){
					throw new ApplicationError("Job " + this.name + " has a wrong numeric value of " + this.userId + " as user id");
				}
			}else{
				this.userIdValue = Value.newTextValue(this.userId);
			}
		}
		if(this.inputFields != null){
			for(InputField field : this.inputFields){
				field.getReady();
			}
		}
		if(this.runAtTheseTimes != null){
			this.setTimes();
		}
	}

	/**
	 * @param uid
	 * @return instance of a scheduled job
	 */
	public ScheduledJob getScheduledJob(Value uid) {
		Value val = this.userIdValue;
		if(val == null){
			val = uid;
		}
		if(this.timesOfDay != null){
			return new DayJob(this, val, this.timesOfDay);
		}
		if(this.runInterval > 0){
			return new BatchJob(this, val);
		}
		return new ListenerJob(this, val);
	}

	/**
	 *
	 * @param uid
	 * @return a running job
	 */
	public RunningJob createRunningJob(Value uid){
		Value val = this.userIdValue;
		if(val == null){
			val = uid;
		}
		ServiceInterface service = ComponentManager.getService(this.serviceName);
		ServiceData inData = new ServiceData(val, this.serviceName);
		if(this.inputJson != null){
			inData.setPayLoad(this.inputJson);
		}
		if(this.inputFields != null){
			for(InputField field : this.inputFields){
				field.setInputValue(inData);
			}
		}
		return new RunningJob(service, inData);
	}

	private void setTimes(){
		this.timesOfDay = new int[this.runAtTheseTimes.length];
		for(int i = 0; i < this.timesOfDay.length; i++){
			String[] pair = this.runAtTheseTimes[i].split(":");
			if(pair.length != 2){
				throw new ApplicationError("Job " + this.name + " has an invalied time-of-day " + this.runAtTheseTimes[i] + ". hh:mm, hh:mm,..  formatis expected.");
			}
			try{
				int hh = Integer.parseInt(pair[0].trim(), 10);
				int mm = Integer.parseInt(pair[1].trim(), 10);
				this.timesOfDay[i] = hh * 60 + mm;
			}catch(Exception e){
				throw new ApplicationError("Job " + this.name + " has an invalied time-of-day " + this.runAtTheseTimes[i] + ". hh:mm, hh:mm,..  formatis expected.");
			}
		}
		Arrays.sort(this.timesOfDay);
	}
}
