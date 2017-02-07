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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.ClientCacheManager;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.ServiceLogger;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.CircularLifo;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ExceptionListener;
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
	/*
	 * session parameter name with which user token is saved. This token is the
	 * name under which our global parameters are saved. This indirection is
	 * used to keep flexibility allow a client to have multiple active sessions.
	 * sessions, we make this a set of tokens.
	 */
	private static final String GET = "GET";

	/**
	 * message to be sent to client if there is any internal error
	 */
	public static final FormattedMessage INTERNAL_ERROR = new FormattedMessage(
			"internalError", MessageType.ERROR,
			"We are sorry. There was an internal error on server. Support team has been notified.");
	/**
	 * message to be sent to client if this request requires a login and the
	 * user has not logged in
	 */
	public static final FormattedMessage NO_LOGIN = new FormattedMessage(
			"notLoggedIn", MessageType.ERROR,
			"You are not logged into the server, or server may have logged-you out as a safety measure after a period of no activity.");
	/**
	 * message to be sent to client if data text was not in order.
	 */
	public static final FormattedMessage DATA_ERROR = new FormattedMessage(
			"invalidDataFormat", MessageType.ERROR,
			"Data text sent from client is not formatted properly. Unable to extract data from the text.");
	/**
	 * message to be used when client has not specified a service
	 */
	public static final FormattedMessage NO_SERVICE = new FormattedMessage(
			"noService", MessageType.ERROR,
			"No service name was specified for this request.");

	/**
	 * message to be used when client's request for login has failed
	 */
	public static final FormattedMessage LOGIN_FAILED = new FormattedMessage(
			"loginFailed", MessageType.ERROR,
			"Invalid Credentials. Login failed.");

	/**
	 * no token from client
	 */
	public static final FormattedMessage NO_TOKEN = new FormattedMessage(
			"noToken", MessageType.ERROR,
			"A valid token for the bckground job is required to get its response.");

	/**
	 * response is not yet available for this token
	 */
	public static final FormattedMessage NO_RESPONSE = new FormattedMessage(
			"noResponse", MessageType.INFO,
			"No response yet from the background job.");
	private static final String STILL_PENDING_PREFIX = "{\""
			+ ServiceProtocol.HEADER_FILE_TOKEN + "\":\"";
	private static final String STILL_PENDING_SUFFIX = "\"}";
	/**
	 * parameter name with which userId is to be saved in session. made this
	 * public filters to observe this convention. Value in session with this
	 * name implies that the user has logged-in. HttpAgent does not serve unless
	 * there is a userId associated with the session
	 */
	public static final String SESSION_NAME_FOR_USER_ID = "_userIdInSession";

	/**
	 * name with session fields for the logged-in users are saved in a Map
	 */
	public static final String SESSION_NAME_FOR_MAP = "_userSessionMap";

	static final String CACHED_TRACES = "CACHED_TRACES";

	/**
	 * set at set-up time in case we are in development mode, and we use a
	 * default dummyLogin id
	 */
	private static Value autoLoginUserId;

	/**
	 * cache service responses
	 */
	private static ClientCacheManager httpCacheManager;

	/**
	 * accumulated traces to be streamed to client when requested.
	 */
	private static boolean tracesToBeCached;
	/**
	 * any exception thrown by service may need to be reported to a central
	 * system.
	 */
	private static ExceptionListener exceptionListener;

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

		String fileToken = req.getHeader(ServiceProtocol.HEADER_FILE_TOKEN);
		if(fileToken != null){
			Tracer.trace("Checking for pending service with token " + fileToken);
			getPendingResponse(req, resp, fileToken);
			return;
		}
		String serviceName = req.getHeader(ServiceProtocol.SERVICE_NAME);
		HttpSession session = req.getSession(true);
		boolean isGet = GET.equals(req.getMethod());
		/*
		 * serviceName is a parameter in GET mode, and CSRF would be in session
		 */
		if (isGet) {
			if (serviceName == null) {
				serviceName = req.getParameter(ServiceProtocol.SERVICE_NAME);
			}
		}

		long startedAt = new Date().getTime();
		long elapsed = 0;
		Value userId = null;
		ServiceData outData = null;
		Tracer.startAccumulation();
		Tracer.trace("Request received for service " + serviceName);
		FormattedMessage message = null;
		/*
		 * let us earnestly try to serve now :-) this do{} is not a loop, but a
		 * block that helps in handling errors in an elegant way
		 */
		ServiceData inData = null;
		do {
			try {
				if (serviceName == null) {
					message = NO_SERVICE;
					break;
				}

				inData = createServiceData(session);
				if (inData == null) {
					message = NO_LOGIN;
					break;
				}
				userId = inData.getUserId();
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
				/*
				 * we are forced to check payload for the time being for some
				 * safety
				 */
				if (payLoad == null || payLoad.isEmpty()
						|| payLoad.equals("undefined")
						|| payLoad.equals("null")) {
					payLoad = "{}";
				}
				inData.setPayLoad(payLoad);
				inData.setServiceName(serviceName);
				if (httpCacheManager != null) {
					outData = httpCacheManager.respond(inData, session);
					if (outData != null) {
						break;
					}
				}
				outData = ServiceAgent.getAgent().executeService(inData);
				/*
				 * by our convention, server may send data in outData to be set
				 * to session
				 */
				if (outData.hasErrors() == false) {
					setSessionData(session, outData);
					if (httpCacheManager != null) {
						httpCacheManager.cache(inData, outData, session);
					}
				}
			} catch (Exception e) {
				Tracer.trace(e, "Internal error");
				if (exceptionListener != null) {
					exceptionListener.listen(inData, e);
					Tracer.trace("listener notified");
				} else {
					Tracer.trace("No listener to notify support team");
				}
				message = INTERNAL_ERROR;
			}
		} while (false);

		elapsed = new Date().getTime() - startedAt;
		resp.setHeader(ServiceProtocol.SERVICE_EXECUTION_TIME, elapsed + "");
		resp.setContentType("text/json");
		String response = null;
		FormattedMessage[] messages = null;
		if (outData == null) {
			if (message == null) {
				message = INTERNAL_ERROR;
			}
			/*
			 * we got error
			 */
			Tracer.trace("Error on web tier : " + message.text);
			messages = new FormattedMessage[1];
			messages[0] = message;
			response = getResponseForError(messages);
		} else if (outData.hasErrors()) {
			Tracer.trace("Service returned with errors");
			response = getResponseForError(outData.getMessages());
		} else {
			/*
			 * all OK
			 */
			response = outData.getPayLoad();
			Tracer.trace(
					"Service succeeded and has "
							+ (response == null ? "no "
									: (response.length()) + " chars ")
							+ " payload");
		}
		writeResponse(resp, response);
		String trace = Tracer.stopAccumulation();
		if (outData != null) {
			String serverTrace = outData.getTrace();
			if (serverTrace != null) {
				trace = "---- Web Tier Trace ---\n" + trace
						+ "\n------ App Tier Trace ----\n" + serverTrace;
			}
		}
		if (tracesToBeCached) {
			cacheTraces(session, trace);
		}
		String uid = userId == null ? "unknown" : userId.toString();
		ServiceLogger.pushTraceToLog(serviceName, uid, (int) elapsed, trace);

	}

	/**
	 * get the JSON to be sent back to client in case of errors
	 *
	 * @param messages
	 * @return JSON string for the supplied errors
	 */
	public static String getResponseForError(FormattedMessage[] messages) {
		JSONWriter writer = new JSONWriter();
		writer.object();
		writer.key(ServiceProtocol.REQUEST_STATUS);
		writer.value(ServiceProtocol.STATUS_ERROR);
		writer.key(ServiceProtocol.MESSAGES);
		JsonUtil.addObject(writer, messages);
		writer.endObject();
		return writer.toString();
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
	 *            Security Token, optional.
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
		if (securityToken != null) {
			inData.put(ServiceProtocol.USER_TOKEN,
					Value.newTextValue(securityToken));
		}
		inData.setPayLoad("{}");
		ServiceData outData = ServiceAgent.getAgent().login(inData);
		if (outData == null) {
			return null;
		}
		Value userId = outData.getUserId();
		if (userId == null) {
			/*
			 * possible that loginService is a custom one. Let us try to fish in
			 * the Map
			 */
			Object uid = outData.get(ServiceProtocol.USER_ID);
			if (uid == null) {
				Tracer.trace(
						"Server came back with no userId and hence HttpAgent assumes that the login did not succeed");
				return null;
			}
			if (uid instanceof Value) {
				userId = (Value) uid;
			} else {
				userId = Value.parseObject(uid);
			}
		}

		/*
		 * create and save new session data
		 */
		newSession(session, userId);
		setSessionData(session, outData);
		Tracer.trace("Login succeeded for loginId " + loginId);
		String result = outData.getPayLoad();
		if (result == null || result.length() == 0) {
			result = "{}";
		}
		return result;
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
		ServiceData inData = createServiceData(session);
		if (inData == null) {
			Tracer.trace(
					"No active session found, and hence logout not called");
			return;
		}
		if (timedOut) {
			inData.put(ServiceProtocol.TIMED_OUT, Value.VALUE_TRUE);
		}

		ServiceAgent.getAgent().logout(inData);

		removeSession(session);
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
		/*
		 * we have to suppress this warning as it is an external call that we
		 * have no control over
		 */
		@SuppressWarnings("unchecked")
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
	 * @param autoUserId
	 *            in case login is disabled and a default loginId is to be used
	 *            for all services
	 * @param cacher
	 *            http cache manager
	 * @param listener
	 *            exception listener
	 * @param cacheTraces
	 *            if true, traces are also saved into a circular buffer that can
	 *            be delivered to the client
	 */
	public static void setUp(Value autoUserId, ClientCacheManager cacher,
			ExceptionListener listener, boolean cacheTraces) {
		autoLoginUserId = autoUserId;
		httpCacheManager = cacher;
		tracesToBeCached = cacheTraces;
		exceptionListener = listener;

	}

	/**
	 *
	 * @param session
	 * @param token
	 * @param inData
	 * @return
	 */
	private static ServiceData createServiceData(HttpSession session) {
		Value userId = (Value) session.getAttribute(SESSION_NAME_FOR_USER_ID);
		if (userId == null) {
			Tracer.trace("Request by non-logged-in session detected.");
			if (autoLoginUserId == null) {
				Tracer.trace("Login is required.");
				return null;
			}
			login(autoLoginUserId.toString(), null, session);
			userId = (Value) session.getAttribute(SESSION_NAME_FOR_USER_ID);
			if (userId == null) {
				Tracer.trace("autoLoginUserId is set to " + autoLoginUserId
						+ " but loginService is probably not accepting this id without credentials. Check your lginServiceName=\"\" in applicaiton.xml and ensure that your service clears this dummy userId with no credentials");
				return null;
			}
			Tracer.trace("User " + userId + " auto logged-in");
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> sessionData = (Map<String, Object>) session
				.getAttribute(SESSION_NAME_FOR_MAP);
		if (sessionData == null) {
			throw new ApplicationError(
					"Unexpected situation. UserId is located in session, but not map");
		}
		ServiceData data = new ServiceData(userId, null);
		for (Map.Entry<String, Object> entry : sessionData.entrySet()) {
			data.put(entry.getKey(), entry.getValue());
		}

		return data;
	}

	/**
	 *
	 * @param session
	 * @param token
	 *            - future use
	 */
	private static void removeSession(HttpSession session) {
		Object obj = session.getAttribute(SESSION_NAME_FOR_USER_ID);
		if (obj == null) {
			Tracer.trace("Remove session : No session to remove");
			return;
		}
		Tracer.trace("Session removed for " + obj);
		session.removeAttribute(SESSION_NAME_FOR_USER_ID);
		session.removeAttribute(SESSION_NAME_FOR_MAP);
	}

	/**
	 *
	 * @param session
	 * @param data
	 */
	private static void setSessionData(HttpSession session, ServiceData data) {
		@SuppressWarnings("unchecked")
		Map<String, Object> sessionData = (Map<String, Object>) session
				.getAttribute(SESSION_NAME_FOR_MAP);

		if (sessionData == null) {
			Tracer.trace(
					"Unexpected situation. setSession invoked with no active session. Action ignored");
		} else {
			for (String key : data.getFieldNames()) {
				sessionData.put(key, data.get(key));
			}
		}
	}

	/**
	 * create a new session for this user. To be used by filter/SSO etc.. if
	 * there is no specific login process for Simplity application. Note; If a
	 * loginService is to be executed, then caller should use login() instead of
	 * this method
	 *
	 * @param session
	 * @param userId
	 * @return map of global fields that is maintained by Simplity. Any
	 *         parameter in this map is made available to every service request
	 */
	public static Map<String, Object> newSession(HttpSession session,
			Value userId) {

		Map<String, Object> sessionData = new HashMap<String, Object>();
		session.setAttribute(SESSION_NAME_FOR_USER_ID, userId);
		session.setAttribute(SESSION_NAME_FOR_MAP, sessionData);
		if (tracesToBeCached) {
			session.setAttribute(CACHED_TRACES, new CircularLifo<String>());
		}
		Tracer.trace("New session data created for " + userId);
		return sessionData;
	}

	/**
	 * get the userId that has logged into this session.
	 *
	 * @param session
	 *            can not be null
	 * @return userId, or null if no login so far in this session
	 */
	public static Value getLoggedInUser(HttpSession session) {
		return (Value) session.getAttribute(SESSION_NAME_FOR_USER_ID);
	}

	/**
	 * invalidate any cached response for this service
	 *
	 * @param serviceName
	 * @param session
	 */
	public static void invalidateCache(String serviceName,
			HttpSession session) {
		if (httpCacheManager != null) {
			httpCacheManager.invalidate(serviceName, session);
		}
	}

	/**
	 * push trace to the buffer in session
	 *
	 * @param session
	 * @param trace
	 */
	private static void cacheTraces(HttpSession session, String trace) {
		Object obj = session.getAttribute(HttpAgent.CACHED_TRACES);
		if (obj == null) {
			Tracer.trace(
					"Unexpected absence of trace buffer in session. Client will not get traces.");
			return;
		}
		@SuppressWarnings("unchecked")
		CircularLifo<String> lifo = ((CircularLifo<String>) obj);
		lifo.put(trace);

	}

	/**
	 * serve this service. Main entrance to the server from an http client.
	 *
	 * @param req
	 *            http request
	 * @param resp
	 *            http response
	 * @param fileToken this is the token that was returned by an earlier call to server. Whenever a service request is  for a service indicating that the service
	 * @throws ServletException
	 *             Servlet exception
	 * @throws IOException
	 *             IO exception
	 *
	 */
	public static void getPendingResponse(HttpServletRequest req,
			HttpServletResponse resp, String fileToken) throws ServletException, IOException {

		FormattedMessage message = null;
		ServiceData outData = null;
		do {
			File file = FileManager.getTempFile(fileToken);
			if (file == null || file.length() == 0) {
				/*
				 * trick-design. To be re-factored later. We break with no
				 * message and no Data to imply a pending status
				 */
				// message = NO_RESPONSE;
				break;
			}
			Object obj = null;
			try {
				ObjectInputStream stream = new ObjectInputStream(
						new FileInputStream(file));
				obj = stream.readObject();
			} catch (Exception e) {
				if (exceptionListener != null) {
					exceptionListener.listen(null, e);
				}
				message = INTERNAL_ERROR;
				break;
			}
			if (obj instanceof ServiceData == false) {
				String text = "Temp file is expected to contain an object instance of ServiceData but we found "
						+ obj.getClass().getName();
				if (exceptionListener != null) {
					exceptionListener.listen(null, new ApplicationError(text));
				} else {
					Tracer.trace(text);
				}
				message = INTERNAL_ERROR;
				break;
			}
			outData = (ServiceData) obj;
		} while (false);

		String response = null;
		if (message != null) {
			FormattedMessage[] messages = { message };
			response = getResponseForError(messages);
		} else if (outData != null) {
			if (outData.hasErrors()) {
				response = getResponseForError(outData.getMessages());
			} else {
				response = outData.getPayLoad();
			}
		} else {
			/*
			 * trick design. no data, no message implies no response available
			 * yet for this token. We have no way to check whether this is a
			 * valid token. Feature has to be added.
			 *
			 */
			response = STILL_PENDING_PREFIX + fileToken + STILL_PENDING_SUFFIX;
		}

		writeResponse(resp, response);
		if (tracesToBeCached && outData != null) {
			cacheTraces(req.getSession(true), outData.getTrace());
		}
	}

	/**
	 * respond back with the pay load
	 *
	 * @param resp
	 * @param payLoad
	 * @throws IOException
	 */
	private static void writeResponse(HttpServletResponse resp, String payLoad)
			throws IOException {
		resp.setContentType("text/json");
		resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		resp.setDateHeader("Expires", 0);
		Writer writer = resp.getWriter();
		writer.write(payLoad);
		writer.close();
	}
}
