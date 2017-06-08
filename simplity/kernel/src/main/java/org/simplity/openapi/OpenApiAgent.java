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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.openapi;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.kernel.Application;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.value.Value;
import org.simplity.service.PayloadType;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;

/**
 * internal servlet that handles all requests coming in as open-api. To be
 * called by the designated servlet that receives request from the web server
 *
 * @author simplity.org
 *
 */
public class OpenApiAgent {

	/**
	 * message to be sent to client if there is any internal error
	 */
	private static final FormattedMessage INTERNAL_ERROR = new FormattedMessage("internalError", MessageType.ERROR,
			"We are sorry. There was an internal error on server. Support team has been notified.");

	/**
	 * serve this service. Main entrance to the server from an http client.
	 *
	 * @param req
	 *            http request
	 * @param resp
	 *            http response
	 * @throws ServletException
	 *             Servlet exception
	 * @throws IOException
	 *             IO exception
	 *
	 */

	public void serve(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		/*
		 * get the service name
		 */
		ServiceData inData = this.createInData(req, resp);
		if (inData == null) {
			return;
		}

		Tracer.trace("Request received for service " + inData.getServiceName());
		FormattedMessage message = null;
		ServiceData outData = null;
		try {
			outData = ServiceAgent.getAgent().executeService(inData, PayloadType.NONE);
		} catch (ApplicationError e) {
			Application.reportApplicationError(inData, e);
			message = INTERNAL_ERROR;
		} catch (Exception e) {
			Application.reportApplicationError(inData, new ApplicationError(e, "Error while processing request"));
			message = INTERNAL_ERROR;
		}

		resp.setContentType("text/json");
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		resp.setDateHeader("Expires", 0);
		if (outData == null) {
			if (message == null) {
				message = INTERNAL_ERROR;
			}
			/*
			 * we got error
			 */
			Tracer.trace("Error on web tier : " + message.text);
			FormattedMessage[] messages = new FormattedMessage[1];
			messages[0] = message;
			this.writeErrors(resp, messages);
		} else {
			this.writeResponse(resp, outData);
		}
	}

	/**
	 * @param resp
	 * @param outData
	 * @throws IOException
	 */
	private void writeResponse(HttpServletResponse resp, ServiceData outData) throws IOException {
		//TODO: use swagger to create response json
		//TODO: refer to org.simplity.tp.OutputData for creating json from service data
		Writer writer = resp.getWriter();
		writer.write("{}");
		writer.close();
	}

	/**
	 * @param resp
	 * @param messages
	 * @throws IOException
	 */
	private void writeErrors(HttpServletResponse resp, FormattedMessage[] messages) throws IOException {
		//TODO: write a response indicating error status and error messages
		Writer writer = resp.getWriter();
		writer.write("{}");
		writer.close();
	}

	/**
	 * @param req
	 * @param resp
	 * @return
	 */
	private ServiceData createInData(HttpServletRequest req, HttpServletResponse resp) {
		//TODO: use swagger to validate service request
		//TODO: use mapping data/config to map url to serviceName
		//TODO: how do you get userId for the logged-in user?
		Value userId = null;
		String serviceName = null;
		ServiceData inData = new ServiceData(userId, serviceName);
		return inData;
	}

}
