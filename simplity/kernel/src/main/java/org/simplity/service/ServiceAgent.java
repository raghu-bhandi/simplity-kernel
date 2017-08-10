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
package org.simplity.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import org.simplity.gateway.ReqReader;
import org.simplity.gateway.RespWriter;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.Application;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Messages;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.tp.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * * Sole agent to be approached for any service from the app. App functionality
 * is delivered
 * strictly thru this one class. Agent prepares the right infrastructure for the
 * service before
 * calling it.
 *
 * <p>
 * This is to be used "internally" by another class, after taking care of
 * authentication, session
 * management etc.. ServiceAgent assumes that the caller is entitled for this
 * service, and it is up
 * to service to do any additional security/based on userId. It also assumes
 * that the caller has
 * authenticated the userId.
 */
public class ServiceAgent {
	private static final Logger logger = LoggerFactory.getLogger(ServiceAgent.class);
	// private static final String[] EMPTY_ARRAY = {};
	/** singleton instance that is instantiated with right parameters */
	private static ServiceAgent instance;

	/**
	 * Set plugins and parameters for agent
	 *
	 * @param autoLoginUserId
	 * @param userIdIsNumber
	 * @param login
	 * @param logout
	 * @param cacher
	 * @param guard
	 */
	public static void setUp(String autoLoginUserId, boolean userIdIsNumber, String login, String logout,
			ServiceCacheManager cacher, AccessController guard) {
		instance = new ServiceAgent(autoLoginUserId, userIdIsNumber, login, logout, cacher, guard);
	}

	/** @return an instance for use */
	public static ServiceAgent getAgent() {
		if (instance == null) {
			throw new ApplicationError("Service Agent is not set up, but there are requests for service!!");
		}
		return instance;
	}

	/** is user Id a numeric value? default is text */
	private final boolean numericUserId;
	/**
	 * login service to be called. If null, we use a dummy user id of 100 to
	 * auto-login
	 */
	private final String loginService;

	/** application may need to so something on logout */
	private final String logoutService;

	/**
	 * service response may be cached. This may also be used to have fake
	 * responses during development
	 */
	private final ServiceCacheManager cacheManager;
	/** registered access control class */
	private final AccessController securityManager;
	/** auto login ID */
	private final String autoLoginUserId;

	private String cacheKey;

	/**
	 * * We create an immutable instance fully equipped with all plug-ins
	 *
	 * @param autoLoginUserId
	 */
	private ServiceAgent(String autoLoginUserId, boolean userIdIsNumber, String login, String logout,
			ServiceCacheManager cacher, AccessController guard) {
		this.autoLoginUserId = autoLoginUserId;
		this.numericUserId = userIdIsNumber;
		this.loginService = login;
		this.logoutService = logout;
		this.cacheManager = cacher;
		this.securityManager = guard;
	}

	/**
	 * ask service to handle this special service
	 *
	 * @param inputData
	 * @return fields to be put into session. Note that we do not return any
	 *         response to client to
	 *         data returned by standard service call. This is typically used as
	 *         global fields for the
	 *         user session.
	 */
	public ServiceData login(ServiceData inputData) {

		/*
		 * login service may want to know whether this is an auto-login
		 *
		 */
		boolean isAutoLogin = this.autoLoginUserId != null
				&& this.autoLoginUserId.equals(inputData.get(ServiceProtocol.USER_ID).toString());
		inputData.put(ServiceProtocol.IS_AUTO_LOGIN, Value.newBooleanValue(isAutoLogin));

		ServiceData result = null;
		if (this.loginService == null) {
			result = this.dummyLogin(inputData);
		} else {
			result = ComponentManager.getService(this.loginService).respond(inputData, PayloadType.NONE);
		}
		if (result.hasErrors() == false) {
			Object uid = result.get(ServiceProtocol.USER_ID);
			if (uid == null) {

				logger.info("Login service did not set value for " + ServiceProtocol.USER_ID
						+ ". This implies that the login has failed.");

			} else {
				if (uid instanceof Value == false) {
					throw new ApplicationError("Login service returned userId as a field in " + ServiceProtocol.USER_ID
							+ " but instead of being an instance of Value we found it an instance of "
							+ uid.getClass().getName());
				}
				result.setUserId((Value) uid);
			}
		}
		return result;
	}

	/**
	 * application has not set any login service. Simulate a successful login
	 *
	 * @param inData
	 * @return
	 */
	private ServiceData dummyLogin(ServiceData inData) {
		ServiceData result = new ServiceData();

		logger.info("No login service is attached. we use a dummy login.");

		/*
		 * choosing a number, just in case the application uses a numeric field
		 */
		String userId = "100";
		Object obj = inData.get(ServiceProtocol.USER_ID);
		if (obj != null) {
			userId = obj.toString();
		}

		Value userIdValue = this.numericUserId ? Value.parseValue(userId, ValueType.INTEGER)
				: Value.newTextValue(userId);
		if (Value.isNull(userIdValue)) {

			logger.info("we would have cleared userId " + userId + " but for the fact that we insist on a number");

		} else {

			logger.info("we cleared userId=" + userId + " with no authentication whatsoever.");

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
			ComponentManager.getService(this.logoutService).respond(inputData, PayloadType.NONE);
		}
	}

	/**
	 * @param inputData
	 *            fields are assumed to be session data, and payLoad is the
	 *            request string from
	 *            client
	 * @return response to be returned to client payLoad is response text, while
	 *         fields collection is
	 *         data to be set to session. Null if the service not found or the
	 *         logged-in user is not
	 *         entitled for the service
	 */
	public ServiceData executeService(ServiceData inputData) {
		return this.executeService(inputData, PayloadType.NONE);
	}

	/**
	 * @param inputData
	 *            fields are assumed to be session data, and payLoad is the
	 *            request string from
	 *            client
	 * @param payloadType
	 * @return response to be returned to client payLoad is response text, while
	 *         fields collection is
	 *         data to be set to session. Null if the service not found or the
	 *         logged-in user is not
	 *         entitled for the service
	 */
	public ServiceData executeService(ServiceData inputData, PayloadType payloadType) {
		/*
		 * this is the entry point for the app-side. This method may be invoked
		 * either remotely, or with HttpAgent on the same JVM. This distinction
		 * need not be detected
		 */

		String serviceName = inputData.getServiceName();
		Value userId = inputData.getUserId();
		// ServiceInterface service =
		// ComponentManager.getServiceOrNull(serviceName);
		Service service = (Service) ComponentManager.getServiceOrNull(serviceName);
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

				logger.info("Service " + serviceName + " is missing in action !!");

				response = this.defaultResponse(inputData);
				response.addMessage(Messages.getMessage(Messages.NO_SERVICE, serviceName));
				break;
			}

			/*
			 * is it accessible to user?
			 */
			if (this.securityManager != null && this.securityManager.okToServe(service, inputData) == false) {
				response = this.defaultResponse(inputData);
				/*
				 * should we say you are not authorized?
				 */

				logger.info("Logged in user " + userId + " is not granted access to this service");

				response.addMessage(Messages.getMessage(Messages.NO_ACCESS));
				break;
			}

			/*
			 * is it cached?
			 */
			if (this.cacheManager != null) {
				if (service.okToCache()) {
					this.cacheKey = service.generateKeyToCache(serviceName, inputData);
					response = this.cacheManager.respond(this.cacheKey);
					if (response != null) {
						break;
					}
				}
			}

			/*
			 * is this to be run in the background always?
			 */
			if (service.toBeRunInBackground()) {
				response = this.runInBackground(inputData, service, payloadType);
				break;
			}
			/*
			 * OK. here we go and call the actual service
			 */
			try {

				logger.info("Invoking service " + serviceName);

				response = service.respond(inputData, payloadType);
				boolean hasErrors = response != null && response.hasErrors();
				if (hasErrors) {

					logger.info(serviceName + " returned with errors.");

				} else {

					logger.info(serviceName + " responded with all OK signal");

					String[] servicesToInvalidate = service.getServicesToInvalidate();

					if (servicesToInvalidate != null && servicesToInvalidate.length > 0) {
						for (String servName : servicesToInvalidate) {
							this.invalidateCache(servName, inputData);
						}
					}
				}
				if (this.cacheManager != null && hasErrors == false && service.okToCache()) {
					response.setCacheKey(this.cacheKey);
					this.cacheManager.cache(inputData, response);
				}
			} catch (Exception e) {
				Application.reportApplicationError(inputData, e);

				logger.error("Exception thrown by service " + serviceName, e);

				response = this.defaultResponse(inputData);
				response.addMessage(Messages.getMessage(Messages.INTERNAL_ERROR, e.getMessage()));
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
	 * @param inData
	 * @return response to be sent back to client
	 */
	@SuppressWarnings("resource")
	private ServiceData runInBackground(ServiceData inData, ServiceInterface service, PayloadType payloadType) {
		ServiceData outData = this.defaultResponse(inData);
		ObjectOutputStream stream = null;
		String token = null;
		File file = FileManager.createTempFile();
		try {
			stream = new ObjectOutputStream(new FileOutputStream(file));
			token = file.getName();
			inData.put(ServiceProtocol.HEADER_FILE_TOKEN, token);
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while creating file for output from  bckground job");
		}
		JSONWriter writer = new JSONWriter();
		writer.object().key(ServiceProtocol.HEADER_FILE_TOKEN).value(token).endObject();
		outData.setPayLoad(writer.toString());
		ServiceSubmitter submitter = new ServiceSubmitter(inData, service, stream, payloadType);
		Thread thread = Application.createThread(submitter);
		thread.start();

		return outData;
	}

	/**
	 * create a response with right headers..
	 *
	 * @param inData
	 * @return
	 */
	private ServiceData defaultResponse(ServiceData inData) {
		return new ServiceData(inData.getUserId(), inData.getServiceName());
	}

	/**
	 * invalidate any cached response for this service
	 *
	 * @param serviceName
	 * @param inData
	 */
	public void invalidateCache(String serviceName, ServiceData inData) {
		if (instance.cacheManager != null) {
			Service invalidateServ = (Service) ComponentManager.getServiceOrNull(serviceName);
			String keyToInvalidate = invalidateServ.generateKeyToCache(serviceName, inData);
			logger.info("Invalidating cache for the service " + serviceName);
			instance.cacheManager.invalidate(keyToInvalidate);
		}
	}

	/*
	 * agent for RESTful standards based requests
	 */

	/**
	 * executes a service. returns error messages in case of any trouble. This
	 * method uses the stream-piping based reader and writer to facilitate
	 * different formats/standards for input/output data. At the same time,
	 * over-head of intermediate objects can be avoided.
	 *
	 * @param serviceName
	 *            fully qualified service name
	 * @param userId
	 *            on whose behalf this service is being executed. This is the
	 *            user-id of logged-in user in case of a request from a client.
	 *            In case of request from another server-app, caller must ensure
	 *            that this id is relevant for the service to apply any business
	 *            logic that may use userId.
	 * @param reader
	 *            all input data for this service is read/demanded from this
	 *            reader
	 * @param writer
	 *            output data as specified by the service is written to this
	 *            writer
	 * @return null if all ok. list of errors if service is not successfully
	 *         executed
	 */
	public FormattedMessage[] executeService(String serviceName, Value userId, ReqReader reader, RespWriter writer) {
		ServiceInterface service = ComponentManager.getServiceOrNull(serviceName);

		/*
		 * do we have this service?
		 */
		if (service == null) {
			logger.info("Service " + serviceName + " is missing in action !!");
			FormattedMessage[] noService = { new FormattedMessage(Messages.NO_SERVICE, serviceName) };
			return noService;
		}

		try {
			InputData inData = service.getInputSpecification();
			ServiceContext ctx = new ServiceContext(serviceName, userId);
			if (reader.hasInputSpecs()) {
				if(inData != null){
					inData.prepareForInput(ctx);
				}
				reader.pushDataToContext(ctx);
			} else {
				if (inData != null) {
					inData.read(reader, ctx);
					inData = null;
				}
			}
			if (ctx.isInError()) {
				logger.info("Errors found in input for service " + serviceName + ". Service not invoked.");
				return ctx.getMessages().toArray(new FormattedMessage[0]);
			}

			logger.info("Invoking service " + serviceName);
			service.serve(ctx);
			if (ctx.isInError()) {
				logger.info("Service " + serviceName + " returned with errors.");
				return ctx.getMessages().toArray(new FormattedMessage[0]);
			}

			/*
			 * cachable?
			 */
			if (service.okToCache()) {
				String key = ctx.getCachingKey();
				writer.setCaching(key, ctx.getCacheValidity());
			} else {
				String[] invalidations = ctx.getInvalidations();
				if (invalidations != null) {
					writer.setInvalidations(invalidations);
				}
			}

			/*
			 * write output
			 */
			OutputData outData = service.getOutputSpecification();
			if (writer.hasOutputSpec()) {
				if (outData != null) {
					outData.prepareForOutput(ctx);
				}
				writer.pullDataFromContext(ctx);
			}else{
				if (outData != null) {
					outData.write(writer, ctx);
				}
			}

			logger.info("Service " + serviceName + " succeeded.");
			return null;
		} catch (Exception e) {
			Application.reportApplicationError(null, e);
			logger.info("{}Exception thrown by service {}", e.getMessage(), serviceName);
			FormattedMessage[] msgs = { Messages.getMessage(Messages.INTERNAL_ERROR, e.getMessage()) };
			return msgs;
		}
	}

	/**
	 * @param serviceName
	 *            non-null fully qualified service name
	 * @param reader
	 *            non-null request reader that can give values for possible key
	 *            names
	 * @param userId
	 *            non-null user id on whose behalf this service is to be
	 *            executed
	 * @return key with which the response for this request may have been
	 *         cached. null if this is not cached.
	 */
	public String getCachingKey(String serviceName, ReqReader reader, Value userId) {
		ServiceInterface service = ComponentManager.getService(serviceName);
		if (service.okToCache() == false) {
			return null;
		}
		String[] keys = service.getCacheKeyNames();
		if (keys == null) {
			return Service.createCachingKey(serviceName, null);
		}
		String[] vals = new String[keys.length];
		for (int i = 0; i < keys.length; i++) {
			String key = keys[i];
			Object val;
			if (key.equals(ServiceProtocol.USER_ID)) {
				val = userId;
			} else {
				val = reader.getValue(key);
			}
			if (val != null) {
				vals[i] = val.toString();
			}
		}
		return Service.createCachingKey(serviceName, vals);
	}
}
