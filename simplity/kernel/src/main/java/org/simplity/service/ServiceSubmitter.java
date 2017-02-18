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

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

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
	private final ExceptionListener listener;

	private boolean timeToWindup = false;

	/***
	 * instantiate with required attributes
	 *
	 * @param inData
	 * @param service
	 * @param outStream
	 * @param listener
	 *
	 */
	public ServiceSubmitter(ServiceData inData, ServiceInterface service,
			ObjectOutputStream outStream, ExceptionListener listener) {
		this.inData = inData;
		this.service = service;
		this.outStream = outStream;
		this.listener = listener;

	}

	@Override
	public void run() {
		int interval = this.service.getBackgroundRunInterval();
		if (interval == 0) {
			this.runOnce(null);
			return;
		}
		/*
		 * interval in milli seconds
		 */
		interval = interval * 1000;
		String serviceName = this.service.getQualifiedName();
		while (true) {
			Date started = new Date();
			Tracer.trace("Started service " + serviceName + " at " + started);
			Object obj = this.inData.get(ServiceProtocol.HEADER_FILE_TOKEN);
			String token = obj == null ? null : obj.toString();
			this.runOnce(token);
			if (this.timeToWindup) {
				Tracer.trace("Shutting down service " + serviceName);
				return;
			}
			Date finished = new Date();
			Tracer.trace("Finished service " + serviceName + " at " + started);
			long napTime = interval - (finished.getTime() - started.getTime());
			if (napTime < 0) {
				Tracer.trace("Re-starting the service immediately");
			} else {
				try {
					Tracer.trace("Going to wait for " + napTime + "ms");
					Thread.sleep(napTime);
				} catch (InterruptedException e) {
					Tracer.trace(serviceName + " got interrupted..");
					break;
				}
			}
		}
	}

	private void runOnce(String token) {
		Tracer.startAccumulation();
		Date startTime = new Date();
		ServiceData outData = null;
		String serviceName = this.service.getQualifiedName();
		try {
			outData = this.service.respond(this.inData);
		} catch (Exception e) {
			if (this.listener != null) {
				this.listener.listen(this.inData, e);
			}
			outData = new ServiceData(this.inData.getUserId(), serviceName);
			Tracer.trace(e,
					"Service " + serviceName + " resulted in fatal error");
			outData.addMessage(Messages.getMessage(Messages.INTERNAL_ERROR,
					e.getMessage()));
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
		if (token == null) {
			return;
		}

		outData.put(ServiceProtocol.HEADER_FILE_TOKEN, token);

		if (this.outStream == null) {
			return;
		}
		try {
			this.outStream.writeObject(outData);
		} catch (IOException e) {
			Tracer.trace(e,
					"Error while writing response from background service onto stream");
		} finally {
			try {
				this.outStream.close();
			} catch (Exception ignore) {
				//
			}
		}

	}

	public static void main(String[] args) {
		FileChannel channel = null;
		FileLock lock = null;
		try {
			File folder = new File("c:/temp/test/in");
			for (File file : folder.listFiles()) {
				channel = new RandomAccessFile(file, "rw").getChannel();
				lock = channel.tryLock();
				lock.release();
				channel.close();

				Path path = Paths.get(file.getAbsolutePath());
				Files.move(path, path.resolveSibling(file.getName() + ".bak"));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(lock != null){
				try {
					lock.release();
				} catch (Exception ignore) {
					// TODO Auto-generated catch block
				}
			}
			if(channel != null){
				try {
					channel.close();
				} catch (Exception ignore) {
					// TODO Auto-generated catch block
				}
			}
		}
	}
}
