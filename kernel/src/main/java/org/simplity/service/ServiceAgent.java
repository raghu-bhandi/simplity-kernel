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
package org.simplity.service;

import java.util.Date;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

/***
 * Sole agent to be approached for any service from the app. App functionality
 * is delivered strictly thru this one class. Agent prepares the right
 * infrastructure for the service before calling it.
 *
 * This is to be used "internally" by another class, after taking care of
 * authentication, session management etc.. ServiceAgent assumes that the caller
 * is entitled for this service, and it is upto service to do any additional
 * security/based on userId. It also assumes that the caller has authenticated
 * the userId.
 */
public class ServiceAgent {

	/**
	 * singleton instance that is instantiated with right parameters
	 */
	private static ServiceAgent instance;

	/**
	 * Set plugins and parameters for agent
	 *
	 * @param userIdIsNumber
	 * @param login
	 * @param logout
	 * @param cacher
	 * @param guard
	 * @param listener
	 */
	public static void setUp(boolean userIdIsNumber, String login,
			String logout, ServiceCacheManager cacher, AccessController guard,
			ExceptionListener listener) {
		instance = new ServiceAgent(userIdIsNumber, login, logout, cacher,
				guard, listener);
	}

	/**
	 * @return an instance for use
	 */
	public static ServiceAgent getAgent() {
		if (instance == null) {
			throw new ApplicationError(
					"Service Agent is not set up, but there are requests for service!!");
		}
		return instance;
	}

	/**
	 * is user Id a numeric value? default is text
	 */
	private final boolean numericUserId;
	/**
	 * login service to be called. If null, we use a dummy user id of 100 to
	 * auto-login
	 */
	private final String loginService;

	/**
	 * application may need to so something on logout
	 */
	private final String logoutService;

	/**
	 * service response may be cached. This may also be used to have fake
	 * responses during development
	 */
	private final ServiceCacheManager cacheManager;
	/**
	 * any exception thrown by service may need to be reported to a central
	 * system.
	 */
	private final ExceptionListener exceptionListener;
	/**
	 * registered access control class
	 */
	private final AccessController securityManager;

	/***
	 * We create an immutable instance fully equipped with all plug-ins
	 */
	private ServiceAgent(boolean userIdIsNumber, String login, String logout,
			ServiceCacheManager cacher, AccessController guard,
			ExceptionListener listener) {
		this.numericUserId = userIdIsNumber;
		this.loginService = login;
		this.logoutService = logout;
		this.cacheManager = cacher;
		this.exceptionListener = listener;
		this.securityManager = guard;
	}

	/**
	 * ask service to handle this special service
	 *
	 * @param inputData
	 *
	 * @return fields to be put into session. Note that we do not return any
	 *         response to client to data returned by standard service call.
	 *         This is typically used as global fields for the user session. In
	 *         case of
	 */
	public ServiceData login(ServiceData inputData) {
		ServiceData result = null;
		if (this.loginService == null) {
			result = this.dummyLogin(inputData);
		} else {
			result = ComponentManager.getService(this.loginService).respond(
					inputData);
		}
		Object uid = result.get(ServiceProtocol.USER_ID);
		if (uid == null) {
			Tracer.trace("Login service did not set value for "
					+ ServiceProtocol.USER_ID
					+ ". This implies that the login has failed.");
		} else {
			if (uid instanceof Value == false) {
				throw new ApplicationError(
						"Login service returned userId as a field in "
								+ ServiceProtocol.USER_ID
								+ " but intead of being an instance of Value we found it an instance of "
								+ uid.getClass().getName());
			}
			result.setUserId((Value) uid);
		}
		return result;
	}

	private ServiceData dummyLogin(ServiceData inData) {
		ServiceData result = new ServiceData();
		Tracer.trace("No login service is attched. we use a dummy login.");
		String userId = "100";
		Object obj = inData.get(ServiceProtocol.USER_ID);
		if (obj != null) {
			userId = obj.toString();
		}
		Value userIdValue = this.numericUserId ? Value.parseValue(userId,
				ValueType.INTEGER) : Value.newTextValue(userId);
		if (Value.isNull(userIdValue)) {
			Tracer.trace("I would have cleared userId " + userId
					+ " but for the fact that we insist on a number");
		} else {
			Tracer.trace("we cleared userId=" + userId
					+ " with no authentication whatsoever.");
			result.put(ServiceProtocol.USER_ID, userIdValue);
		}
		return result;
	}

	/**
	 * user has logged-out. Take care of any update on the server
	 *
	 * @param inputData
	 */
	public void logout(ServiceData inputData) {
		if (this.logoutService != null) {
			ComponentManager.getService(this.logoutService).respond(inputData);
		}
	}

	/**
	 * @param inputData
	 *            fields are assumed to be session data, and payLoad is the
	 *            request string from client
	 * @return response to be returned to client payLoad is response text, while
	 *         fields collection is data to be set to session. Null if the
	 *         service not found or the logged-in user is not entitled for the
	 *         service
	 *
	 */
	public ServiceData executeService(ServiceData inputData) {
		// String existingTrace = Tracer.startAccumulation();
		String serviceName = inputData.getServiceName();
		Value userId = inputData.getUserId();
		ServiceInterface service = ComponentManager
				.getServiceOrNull(serviceName);
		ServiceData response = null;
		Date startTime = new Date();

		/*
		 * do block is convenient to put breaks and avoid over-dose of else-if
		 */
		do {
			/*
			 * do we have this service?
			 */
			if (service == null) {
				Tracer.trace("Service " + serviceName
						+ " is missing in action !!");
				response = new ServiceData();
				response.addMessage(Messages.getMessage(Messages.NO_SERVICE,
						serviceName));
				break;
			}

			/*
			 * is it accessible to user?
			 */
			if (this.securityManager != null
					&& this.securityManager.okToServe(userId, serviceName,
							service) == false) {
				response = new ServiceData();
				/*
				 * should we say you are not authorized?
				 */
				Tracer.trace("Logged in user " + userId
						+ " is not granted access to this service");
				response.addMessage(Messages.getMessage(Messages.NO_ACCESS));
				break;
			}
			/*
			 * is it cached?
			 */
			if (this.cacheManager != null) {
				response = this.cacheManager.respond(inputData);
				if (response != null) {
					break;
				}
			}
			/*
			 * OK. here we go and call the actual service
			 */
			try {
				Tracer.trace("Invoking service " + serviceName);
				response = service.respond(inputData);
				boolean hasErrors = response != null && response.hasErrors();
				Tracer.trace(serviceName + " returned with "
						+ (hasErrors ? "" : "NO") + " errors");
				if (this.cacheManager != null && hasErrors == false) {
					this.cacheManager.cache(inputData, response);
				}
			} catch (Exception e) {
				if (this.exceptionListener != null) {
					this.exceptionListener.listen(inputData, e);
				}
				Tracer.trace(e, "Exception thrown by service " + serviceName);
				response = new ServiceData();
				response.addMessage(Messages.getMessage(
						Messages.INTERNAL_ERROR, e.getMessage()));
			}
		} while (false);

		Date endTime = new Date();
		long diffTime = endTime.getTime() - startTime.getTime();
		if (response != null) {
			response.setExecutionTime((int) diffTime);
		}
		return response;
	}

	/**
	 * invalidate any cached response for this service
	 *
	 * @param serviceName
	 */
	public static void invalidateCache(String serviceName) {
		if (instance.cacheManager != null) {
			instance.cacheManager.invalidate(serviceName);
		}
	}
}