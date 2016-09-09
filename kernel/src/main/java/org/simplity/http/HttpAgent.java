/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
 * Copyright (c) 2016 simplity.org
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
package org.simplity.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceProtocol;

/**
 * servlet that is the agent to expose all services on http. receives requests
 * for service and responds back with response given by the service. As you can
 * see, this class can be easily converted into a servlet and configured to be
 * the target URL for client requests. However, there would be several
 * infrastructure related issues to be addressed in this layer. Hence we
 * recommend that you take care of all that for your project, and call this
 * class, rather than we making provisions for all of that
 *
 * @author simplity.org
 *
 */
public class HttpAgent {
	private static final String GET = "GET";
	private static final String TAG = "<htppTrace at=\"";
	private static final String ELAPSED = "\" elapsed=\"";
	private static final String SERVICE = "\" serviceName=\"";
	private static final String USER = "\" userId=\"";
	private static final String TAG_CLOSE = "\n]]>\n</httpTrace>";
	private static final String CLOSE = "\" >\n<![CDATA[\n";
	private static final String TRACE = "trace";

	private static final Logger myLogger = Logger.getLogger(TRACE);
	private static final FormattedMessage INTERNAL_ERROR = new FormattedMessage(
			"internalError",
			MessageType.ERROR,
			"We are sorry. There was an internal error on server. Support team has been notified.");
	private static final FormattedMessage NO_LOGIN = new FormattedMessage(
			"notLoggedIn",
			MessageType.ERROR,
			"You are not logged into the server, or server may have logged-you out as a safety measure after a period of no activity.");
	private static final FormattedMessage DATA_ERROR = new FormattedMessage(
			"invalidDataFormat",
			MessageType.ERROR,
			"Data text sent from client is not formatted properly. Unable to extract data from the text.");
	private static final FormattedMessage NO_SERVICE = new FormattedMessage(
			"noService", MessageType.ERROR,
			"No service name was specified for this request.");

	/**
	 * session helper is to be set at the start-up time based on setting for
	 * this project
	 */
	private static SessionHelper sessionHelper = new HelperForNoLogin();

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
	public static void serve(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		/*
		 * serviceName and CSRF headers are expected in header in AJAX mode with
		 * post
		 */
		String serviceName = req.getHeader(ServiceProtocol.SERVICE_NAME);
		String clientToken = req.getHeader(ServiceProtocol.CSRF_HEADER);
		HttpSession session = req.getSession(true);
		boolean isGet = GET.equals(req.getMethod());

		/*
		 * serviceName is a parameter in GET mode, and CSRF would be in session
		 */
		if (isGet) {
			if (serviceName == null) {
				serviceName = req.getParameter(ServiceProtocol.SERVICE_NAME);
			}
			if (clientToken == null) {
				clientToken = sessionHelper.getUserToken(session);
			}
		}

		long startedAt = new Date().getTime();
		long elapsed = 0;
		Value userId = null;
		ServiceData outData = null;
		Tracer.startAccumulation();
		FormattedMessage message = null;

		/*
		 * let us earnestly try to serve now :-) this do{} is not a loop, but a
		 * block that helps in handling errors in an elegant way
		 */
		do {
			try {
				if (serviceName == null) {
					message = NO_SERVICE;
					break;
				}
				/*
				 * we send token, possibly null to sessionHelper. It is up to
				 * him to tell us whether
				 */
				ServiceData inData = new ServiceData();
				/*
				 * session helper decides whether login is required etc..
				 */
				if (sessionHelper.getSessionData(session, clientToken, inData) == false) {
					message = NO_LOGIN;
					break;
				}
				Object obj = inData.get(ServiceProtocol.USER_ID);
				if (obj != null && obj instanceof Value) {
					userId = (Value) obj;
				} else {
					Tracer.trace("Session helper is saying OK with no userId. Sending the request to service layer with fingers crossed");
				}
				String payLoad = null;
				if (isGet) {
					payLoad = queryToJson(req);
				} else {
					/*
					 * try-catch specifically for any possible I/O errors
					 */
					try {
						payLoad = readInput(req);
					} catch (Exception e) {
						message = DATA_ERROR;
						break;
					}
				}
				inData.setPayLoad(payLoad);
				inData.setServiceName(serviceName);
				/*
				 * all right. Go ahead and ask server to deliver this service
				 */
				Stream.putUploads(inData, session);
				outData = ServiceAgent.getAgent().executeService(inData);
				/*
				 * by our convention, server may send data in inData to be set
				 * to session
				 */
				if (outData.hasErrors() == false) {
					sessionHelper.setSessionData(session, clientToken, outData);
				}
			} catch (Exception e) {
				Tracer.trace(e, "Internal error");
				message = INTERNAL_ERROR;
			}
		} while (false);

		elapsed = new Date().getTime() - startedAt;
		resp.setHeader(ServiceProtocol.SERVICE_EXECUTION_TIME, elapsed + "");
		String response = null;
		if (outData == null) {
			/*
			 * we got error
			 */
			Tracer.trace("Error on web tier");
			FormattedMessage[] messages = { message };
			response = JsonUtil.toJson(messages);
			resp.setHeader(ServiceProtocol.REQUEST_STATUS, "1");
		} else {
			if (outData.hasErrors()) {
				Tracer.trace("Service returned with errors");
				resp.setHeader(ServiceProtocol.REQUEST_STATUS, "1");
				response = JsonUtil.toJson(outData.getMessages());
			} else {
				/*
				 * all OK
				 */
				resp.setHeader(ServiceProtocol.REQUEST_STATUS, "0");
				response = outData.getPayLoad();
				Tracer.trace("Service has no errors and has "
						+ (response == null ? "no " : (response.length())
								+ " chars ") + " payload");
			}
		}
		if (response != null) {
			resp.getOutputStream().print(response);
		}
		if (outData != null) {
			String serverTrace = outData.getTrace();
			if (serverTrace != null) {
				Tracer.trace("-------Server Trace Begin ----------");
				Tracer.trace(serverTrace);
				Tracer.trace("-------Server Trace END ----------");
			}
			/*
			 * any downloaded media into temp area need to be communicated to
			 * Streamer..
			 */
			Stream.receiveDownLoads(outData, session);
		}
		String trace = Tracer.stopAccumulation();
		String log = TAG + startedAt + ELAPSED + elapsed + SERVICE
				+ serviceName + USER + userId + CLOSE + trace + TAG_CLOSE;
		myLogger.info(log);
	}

	/**
	 * This method can be used in two scenarios.
	 *
	 * 1. SSO or some other login mechanism is used that is outside of this
	 * application. In this case, the registered loginService() inside this
	 * application does not do any authentication, but it will extract user-data
	 * for this session. It may also keep track of the SSO token etc..
	 *
	 * 2. login is designed as service within this application. In this case,
	 * securityToekn is the password supplied by the user. registered
	 * loginService authenticates this password before extracting use data.
	 *
	 * In case you use SSO, or any other common utility outside this
	 * application, we assume that you have authenticated the user, and we will
	 * be calling the spec
	 *
	 * @param loginId
	 *            Login ID
	 * @param securityToken
	 *            Security Token
	 * @param session
	 *            for accessing session
	 * @return token for this session that needs to be supplied for any service
	 *         under this sessions. null implies that we could not login.
	 */
	public static String login(String loginId, String securityToken,
			HttpSession session) {
		/*
		 * ask serviceAgent to login.
		 */
		ServiceData inData = new ServiceData();
		inData.put(ServiceProtocol.USER_ID, Value.newTextValue(loginId));
		inData.put(ServiceProtocol.USSER_TOKEN,
				Value.newTextValue(securityToken));
		ServiceData outData = ServiceAgent.getAgent().login(inData);
		if (outData == null) {
			return null;
		}
		Value userId = outData.getUserId();
		if (userId == null) {
			Tracer.trace("Login service did not set userId. We treat this as login failure");
			return null;
		}
		/*
		 * create and save new session data
		 */
		Tracer.trace("Login succeeded for user id " + userId);
		return sessionHelper.newSession(session, outData, null);
	}

	/**
	 * logout after executing desired service
	 *
	 * @param session
	 *            http session
	 * @param timedOut
	 *            is this triggered on session time-out? false means a specific
	 *            logout request from user
	 *
	 */
	public static void logout(HttpSession session, boolean timedOut) {
		if (session == null) {
			return;
		}
		ServiceData inData = new ServiceData();
		if (timedOut) {
			inData.put(ServiceProtocol.TIMED_OUT, Value.VALUE_TRUE);
		}
		if (sessionHelper.getSessionData(session, null, inData)) {
			ServiceAgent.getAgent().logout(inData);
		}
		sessionHelper.removeSession(session, null);
	}

	/**
	 * read input stream into a string
	 *
	 * @param req
	 * @throws IOException
	 */
	private static String readInput(HttpServletRequest req) throws IOException {
		BufferedReader reader = null;
		StringBuilder sbf = new StringBuilder();
		try {
			reader = req.getReader();
			int ch;
			while ((ch = reader.read()) > -1) {
				sbf.append((char) ch);
			}
			reader.close();
			return sbf.toString();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
					//
				}
			}
		}
	}

	private static String queryToJson(HttpServletRequest req) {
		JSONWriter writer = new JSONWriter();
		writer.object();
		Map<String, String[]> fields = req.getParameterMap();
		if (fields != null) {
			for (Map.Entry<String, String[]> entry : fields.entrySet()) {
				writer.key(entry.getKey()).value(entry.getValue()[0]);
			}
		}
		writer.endObject();
		return writer.toString();
	}

	/**
	 * @param helper
	 *            Session helper to be used to manage session.
	 */
	public static void setUp(SessionHelper helper) {
		sessionHelper = helper;

	}
}
