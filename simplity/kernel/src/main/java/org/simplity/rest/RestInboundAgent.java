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
package org.simplity.rest;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.auth.AuthRequirement;
import org.simplity.auth.OAuth2Agent;
import org.simplity.gateway.Cacher;
import org.simplity.gateway.JsonReqReader;
import org.simplity.gateway.JsonRespWriter;
import org.simplity.gateway.ReqReader;
import org.simplity.gateway.RespWriter;
import org.simplity.json.JSONObject;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * internal servlet that handles all requests coming in as open-api. To be
 * called by the designated servlet that receives request from the web server
 *
 * @author simplity.org
 *
 */
public class RestInboundAgent {
	private  static final Logger logger = LoggerFactory.getLogger(RestInboundAgent.class);
	private static final String UTF = "UTF-8";
	private static final int MILLIS = 60000;

	/**
	 * cache assistant
	 */
	private static Cacher cacher;

	/**
	 * message to be sent to client if there is any internal error
	 */
	private static final String INTERNAL_ERROR = "We are sorry. There was an internal error on server. Support team has been notified.";

	/**
	 * serve an in-bound request.
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

		try {

			/*
			 * assuming http://www.simplity.org:8020/app1/subapp/a/b/c?a=b&c=d
			 *
			 * we need to get a/b/c as RESTful path
			 */
			String path = URLDecoder.decode(req.getRequestURI(), UTF);
			logger.info("Started serving URI=" + path);
			/*
			 * path now is set to /app1/subapp/a/b/c
			 */

			int idx = req.getContextPath().length();
			/*
			 * contextPath is set to /app1/subapp
			 */
			if (idx >= path.length()) {
				/*
				 * this should never happen though..
				 */
				logger.info("oooooops. URI is shorter than contextpath ???");
				path = "";
			} else {
				path = path.substring(idx);
				logger.info("Going to use path=" + path + " for mapping this request to an operation/service");
			}
			/*
			 * parse body and query string into json object
			 */
			JSONObject pathJson = new JSONObject();
			Operation operation = Operations.getOperation(path, req.getMethod().toLowerCase(), pathJson);
			if (operation == null) {
				respondWithError(resp, "We do not serve that request path");
				return;
			}

			/*
			 * do we have to authenticate?
			 *
			 * as of now, we can deal with just one scheme associated with
			 * Oauth2 only. Following code hard codes these assumptions
			 */
			AuthRequirement[] auths = operation.getAuthSchemes();
			if (auths != null && auths.length > 0) {
				/*
				 * we work with Oauth agent that requires request and response
				 * as parameters
				 */
				OAuth2Agent oAgent = (OAuth2Agent) Operations.getSecurityAgent(auths[0].getAuthName());
				if (oAgent.securityCleared(req, resp) == false) {
					logger.info("Authentication failed.");
					return;
				}
			}
			/*
			 * using operation specification, get service name and input data
			 */
			JSONObject json = new JSONObject();
			List<FormattedMessage> messages = new ArrayList<FormattedMessage>();
			String serviceName = operation.prepareRequest(req, json, pathJson, messages);

			if (messages.size() > 0) {
				logger.info("Input data has validation errors. Responding back without calling the service");
				operation.writeResponse(resp, messages.toArray(new FormattedMessage[0]));
				return;
			}

			logger.info("Request received for service " + serviceName);

			Value userId = Value.newTextValue("100");
			ReqReader reader = new JsonReqReader(json);
			ServiceAgent agent = ServiceAgent.getAgent();
			if (cacher != null) {
				String cacheKey = agent.getCachingKey(serviceName, reader, userId);
				if(cacheKey != null){
					Object cache = cacher.get(cacheKey);
					if (cache != null) {
						logger.info("Response retrieved from cache.");
						operation.writeResponse(resp, (JSONObject) cache, serviceName);
						return;
					}
				}
			}

			RespWriter writer = new JsonRespWriter();
			/*
			 * error is part of response and writeResponse is handling that.
			 * We do not do anything differently based on errors, and hence the
			 * returned value of the following method is not captured
			 */
			agent.executeService(serviceName, userId, reader, writer);
			JSONObject response = (JSONObject) writer.getFinalResponseObject();
			logger.info("Response recd = " + response.toString());

			if (cacher != null) {
				handleCaching(writer, json);
			}

			operation.writeResponse(resp, response, serviceName);
		} catch (Exception e) {
			e.printStackTrace();
			respondWithError(resp, INTERNAL_ERROR);
		}
	}

	private static void handleCaching(RespWriter writer, Object response){
		String key  = writer.getCachingKey();
		if(key != null){
			Date exp = null;
			int minutes = writer.getCacheValidity();
			if(minutes != 0){
				exp = new Date(new Date().getTime() + minutes * MILLIS);
			}
			cacher.put(key, response, exp);
		}else{
			String[] invalidations = writer.getInvalidations();
			if(invalidations != null){
				for(String k : invalidations){
					cacher.remove(k);
				}
			}
		}
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
