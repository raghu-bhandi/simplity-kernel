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
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.json.JSONObject;
import org.simplity.kernel.Application;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.util.HttpUtil;
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
public class Agent {
	private static final String UTF = "UTF-8";

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

	public static void serve(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/json");
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		resp.setDateHeader("Expires", 0);

		/*
		 * parse body and query string into json object
		 */
		JSONObject params = getPayload(req);
		/*
		 * assuming http://www.simplity.org:8020/app1/subapp/a/b/c?a=b&c=d
		 *
		 * we need to get a/b/c as RESTful path
		 */
		String path = URLDecoder.decode(req.getRequestURI(), UTF);
		System.out.println("getRequestURI=" + path + " and contextPath is " + req.getContextPath());
		/*
		 * path now is set to /app1/subapp/a/b/c
		 */

		int idx = req.getContextPath().length();
		/*
		 * contextPath is set to /app1/subapp
		 */
		if(idx >= path.length()){
			/*
			 * this should never happen though..
			 */
			System.out.println("oooooops. URI is shorter than contextpath ???");
			path = "";
		}else{
			path = path.substring(idx);
		}
		System.out.println("GOing to look for service spec for path=" + path);
		ServiceSpec spec = ServiceSpecs.getServiceSpec(path, req.getMethod().toLowerCase(), params);
		if (spec == null) {
			respondWithError(resp, "We do not serve that request path");
			return;
		}

		/*
		 * get the service name
		 */
		ServiceData inData = spec.getInData(req, params);

		if(inData.hasErrors()){
			spec.writeResponse(resp, inData);
		}

		Tracer.trace("Request received for service " + inData.getServiceName());
		FormattedMessage message = null;
		ServiceData outData = null;
		try {
			outData = ServiceAgent.getAgent().executeService(inData, PayloadType.JSON);
		} catch (ApplicationError e) {
			Application.reportApplicationError(inData, e);
			message = INTERNAL_ERROR;
		} catch (Exception e) {
			Application.reportApplicationError(inData, new ApplicationError(e, "Error while processing request"));
			message = INTERNAL_ERROR;
		}

		if (outData == null) {
			outData = inData;
			if (message == null) {
				message = INTERNAL_ERROR;
			}
			outData.addMessage(message);
		}
		spec.writeResponse(resp, outData);

	}

	/**
	 * @param req
	 * @return
	 * @throws IOException
	 */
	private static JSONObject getPayload(HttpServletRequest req) throws IOException {
		String text = HttpUtil.readInput(req);
		JSONObject params;
		if (text == null || text.isEmpty()) {
			params = new JSONObject();
		} else {
			params = new JSONObject(text);
		}
		HttpUtil.parseQueryString(req, params);
		return params;
	}

	/**
	 * @param resp
	 * @param message
	 * @throws IOException
	 */
	public static void respondWithError(HttpServletResponse resp, String message) throws IOException {
		resp.setStatus(500);
		resp.getWriter().write(message);
	}
}
