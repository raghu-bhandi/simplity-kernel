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

package org.simplity.service;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;

import org.simplity.kernel.Application;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;

/**
 * convenient class that can be used to run a service in the background
 *
 * @author simplity.org
 *
 */
public class ServiceSubmitter implements Runnable {
	private final ServiceData inData;
	private final ServiceInterface service;
	private final ObjectOutputStream outStream;

	/***
	 * instantiate with required attributes
	 *
	 * @param inData
	 * @param service
	 * @param outStream
	 *
	 */
	public ServiceSubmitter(ServiceData inData, ServiceInterface service, ObjectOutputStream outStream) {
		this.inData = inData;
		this.service = service;
		this.outStream = outStream;

	}

	@Override
	public void run() {
		Tracer.startAccumulation();
		Date startTime = new Date();
		ServiceData outData = null;
		String serviceName = this.service.getQualifiedName();
		try {
			outData = this.service.respond(this.inData);
		} catch (Exception e) {
			Application.reportApplicationError(this.inData, e);
			outData = new ServiceData(this.inData.getUserId(), serviceName);
			Tracer.trace(e, "Service " + serviceName + " resulted in fatal error");
			outData.addMessage(Messages.getMessage(Messages.INTERNAL_ERROR, e.getMessage()));
		}
		String trace = Tracer.stopAccumulation();
		/*
		 * no way to communicate back
		 */
		Tracer.trace("Background service completed with following trace");
		Tracer.trace(trace);
		if (this.outStream == null) {
			return;
		}

		Date endTime = new Date();
		long diffTime = endTime.getTime() - startTime.getTime();
		outData.setExecutionTime((int) diffTime);
		outData.setTrace(trace);
		Object obj = this.inData.get(ServiceProtocol.HEADER_FILE_TOKEN);
		String token = obj == null ? null : obj.toString();
		if (token == null) {
			return;
		}

		outData.put(ServiceProtocol.HEADER_FILE_TOKEN, token);

		try {
			this.outStream.writeObject(outData);
		} catch (IOException e) {
			Tracer.trace(e, "Error while writing response from background service onto stream");
		} finally {
			try {
				this.outStream.close();
			} catch (Exception ignore) {
				//
			}
		}
	}
}
