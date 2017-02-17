/*
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
package org.simplity.kernel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.simplity.http.HttpAgent;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.db.DbVendor;
import org.simplity.kernel.db.SchemaDetail;
import org.simplity.kernel.file.FileBasedAssistant;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.AccessController;
import org.simplity.service.ExceptionListener;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceCacheManager;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceInterface;

/**
 * Configure this application
 *
 * @author simplity.org
 *
 */
public class Application {

	/**
	 * name of configuration file, including extension
	 */
	public static final String CONFIG_FILE_NAME = "application.xml";

	/**
	 * get me the path for resourceFolder, and I will kick-start the engine for
	 * your application.
	 *
	 * @param componentFolder
	 *            folder relative to applicationRoot
	 * @return true if all OK. False in case of any set-up issue.
	 * @throws Exception
	 *             in case the root folder does not exist, or does not required
	 *             resources
	 */
	public static boolean bootStrap(String componentFolder) throws Exception {
		if(!(new File(componentFolder).exists())){
			componentFolder = Thread.currentThread().getContextClassLoader().getResource(componentFolder).getPath();
		}
		Tracer.trace("Bootstrapping with " + componentFolder);
		String msg = null;
		Application app = new Application();
		try {
			componentFolder = ComponentType.setComponentFolder(componentFolder);
			XmlUtil.xmlToObject(componentFolder + CONFIG_FILE_NAME, app);
			if (app.applicationId == null) {
				msg = "Unable to load the configuration component "
								+ CONFIG_FILE_NAME
								+ ". This file is expected to be inside folder "
								+ componentFolder;
			} else {
				msg = app.configure();
			}
		} catch (Exception e) {
			msg = e.getMessage();
		}

		if (msg == null) {
			return true;
		}

		ApplicationError e = new ApplicationError(msg);
		/**
		 * try and pipe exception to the listener..
		 */
		ExceptionListener listener = new DefaultExceptionListener();
		if (app.exceptionListener != null) {
			try {
				listener = (ExceptionListener) Class
						.forName(app.exceptionListener).newInstance();
			} catch (Exception ignore) {
				// we just tried
			}
		}
		if(listener != null){
			listener.listen(null, e);
		}
		throw e;
	}

	/**
	 * unique name of this application within a corporate. This may be used as
	 * identity while trying to communicate with other applications within the
	 * corporate cluster
	 */
	String applicationId;

	/**
	 * do we cache components as they are loaded
	 */
	boolean cacheComponents;
	/**
	 * The database vendor we are using
	 */
	DbVendor dbVendor;
	/**
	 * for connecting to data base, we either use connection string with driver
	 * class name, or use dataSource. Connection string overrides.
	 */
	String connectionString;

	/**
	 * The database driver to use
	 */
	String dbDriverClassName;
	/**
	 * Data source name to be used to look-up in JNDI for dataSource
	 */
	String dataSourceName;
	/**
	 * The service identifier used to perform login. If this is null, we
	 * simulate a dummy login where loginId is accepted as userId,
	 *
	 */
	String loginServiceName;

	/**
	 * The service identifier used to perform logout action. No action is taken
	 * if this is left as null
	 */
	String logoutServiceName;

	/**
	 * does this project use an integer as user id? (default is string
	 */
	boolean userIdIsNumber;

	/**
	 * Response to service request can be cached at two levels : at the Web tier
	 * or at the service level. Specify fully qualified class name you want use
	 * at the service layer. This class should implement
	 * org.simplity.service.ServiceCacheManager
	 */
	String serviceCacheManager;
	/**
	 * class that decides whether a userId be served a given service
	 */
	String accessController;
	/**
	 * way to wire exception to corporate utility
	 */
	String exceptionListener;

	/**
	 * should the sqls that are executed be added to trace?? Required during
	 * development. Some corporate have security policy that requires you not
	 * log sqls
	 */
	boolean logSqls;

	/**
	 * Some projects use multiple schema. In such a case, it is possible that a
	 * given service may use a schema other than the default. We have optimized
	 * the design for a frequently used default schema that is set for the user,
	 * and a few services use their own schema. Provide such schema with
	 * dataSource/connection
	 */
	SchemaDetail[] schemaDetails;

	/**
	 * Simplity provides a rudimentary, folder-based system that can be used for
	 * storing and retrieving attachments. If you want to use that, provide the
	 * folder that is available for the server instance
	 */
	String attachmentsFolderPath;
	/**
	 * If you have an infrastructure for storing and retrieving attachments that
	 * you want to use, provide the class name that implements
	 * AttachmentAssistant. A single instance of this class is re-used
	 */
	String attachmentAssistant;
	/**
	 * logging utility to which service log is to be emitted to. Default is to
	 * emit to console (System.out)
	 */
	LoggingFramework loggingFramework;
	/**
	 * fully qualified class name that implements org.simplity.core.TraceWrapper
	 * to either format service-log or to actually log it.
	 */
	String traceWrapper;

	/**
	 * should we send server trace to client?
	 */
	boolean sendTraceToClient;
	/**
	 * if you want to disable login, and use a dummy user id for all services,
	 * typically during development/demo. Ensure that this value is numeric in
	 * case you have set userIdIsNumber="true"
	 */
	String autoLoginUserId;
	/**
	 * Response to service request can be cached at two levels : at the Web tier
	 * or at the service level. specify fully qualified class name you want use
	 * as cache manager at we tier. This class must implement HttpCacheManager
	 * interface. You may start with the a simple one called
	 * org.siplity.http.SimpleCacheManager that caches services based on service
	 * definition inside http sessions.
	 */
	String clientCacheManager;
	/**
	 * configure application based on the settings. This MUST be triggered
	 * before using the app. Typically this would be triggered from start-up
	 * servlet in a web-app
	 * @return null if all OK. Else message that described why we could not succeed.
	 */
	public String configure() {
		List<String> msgs = new ArrayList<String>();
		Tracer.startAccumulation();
		if (this.traceWrapper != null) {
			try {
				TraceWrapper wrapper = (TraceWrapper) Class
						.forName(this.traceWrapper).newInstance();
				ServiceLogger.setWrapper(wrapper);

			} catch (Exception e) {
				msgs.add(this.traceWrapper
						+ " could not be used to instantiate a Trace Wrapper. "
						+ e.getMessage()
						+ " We will work with a default wrapper");
			}
		}

		ServiceCacheManager casher = null;
		if (this.serviceCacheManager != null) {
			try {
				casher = (ServiceCacheManager) Class
						.forName(this.serviceCacheManager).newInstance();

			} catch (Exception e) {
				msgs.add(this.serviceCacheManager
						+ " could not be used to instantiate a cache manager. "
						+ e.getMessage()
						+ " We will work with no cache manager");
			}
		}

		AccessController gard = null;
		if (this.accessController != null) {
			try {
				gard = (AccessController) Class.forName(this.accessController)
						.newInstance();

			} catch (Exception e) {
				msgs.add(this.accessController
						+ " could not be used to instantiate access controller. "
						+ e.getMessage()
						+ " We will work with no cache manager");
			}
		}

		ExceptionListener listener = null;
		if (this.exceptionListener != null) {
			try {
				listener = (ExceptionListener) Class
						.forName(this.exceptionListener).newInstance();

			} catch (Exception e) {
				msgs.add(this.exceptionListener
						+ " could not be used to instantiate an exception listener. "
						+ e.getMessage() + " We will work with default listener");
			}
		}
		if(listener == null){
			listener = new DefaultExceptionListener();
		}

		/*
		 * Set up db driver.
		 */
		try {
			DbDriver.initialSetup(this.dbVendor, this.dataSourceName,
					this.dbDriverClassName, this.connectionString, this.logSqls,
					this.schemaDetails);
		} catch (Exception e) {
			msgs.add("Error while setting up DbDriver. " + e.getMessage()
					+ " Application will not work properly.");
		}

		/*
		 * in production, we cache components as they are loaded, but in development we prefer to load the latest
		 */
		if (this.cacheComponents) {
			ComponentType.startCaching();
		}

		/*
		 * what about file/media/attachment storage assistant?
		 */
		AttachmentAssistant ast = null;
		if (this.attachmentsFolderPath != null) {
			ast = new FileBasedAssistant(this.attachmentsFolderPath);
		} else if (this.attachmentAssistant != null) {
			try {
				ast = (AttachmentAssistant) Class
						.forName(this.attachmentAssistant).newInstance();
			} catch (Exception e) {
				msgs.add("Error while setting storage assistant based on class "
						+ this.attachmentAssistant + ". " + e.getMessage());
			}
		}
		if (ast != null) {
			AttachmentManager.setAssistant(ast);
		}
		/*
		 * is there a logging framework
		 */
		if (this.loggingFramework == null) {
			Tracer.trace(
					"No logging framework set by application designer. All service logs will be emitted to console. (System.out)");
		} else {
			Tracer.trace("Logging framework is set to " + this.loggingFramework
					+ ". Will try to locate the right class and connect to it.");
			try {
				ServiceLogger.setLogger(this.loggingFramework);
				Tracer.trace(
						"Service logs successfully diverted to the logging framework "
								+ this.loggingFramework);
			} catch (Exception e) {
				String msg = "Logging framework " + this.loggingFramework
						+ " could not be initiated for logging. \n " + e.getMessage() +"\nAre you missing required jar file?. ";
				Tracer.trace(msg);
				msgs.add(msg);
			}
		}

		/*
		 * initialize service agent
		 */
		ServiceAgent.setUp(this.userIdIsNumber, this.loginServiceName, this.logoutServiceName, casher, gard, listener);

		/*
		 * initialize http-agent. In rare cases, a project may not use httpAgent, but it is not so much of an issue if the agent is all dressed-up but no work :-)
		 *
		 */
		ClientCacheManager cacher = null;
		if(this.clientCacheManager != null){
			try{
			cacher = (ClientCacheManager)Class.forName(this.clientCacheManager).newInstance();
			}catch(Exception e){
				msgs.add("Error while creating a ClientCacheManager instance using class name " + this.clientCacheManager + ". " + e.getMessage());
			}
		}

		Value uid = null;
		if (this.autoLoginUserId != null) {
			if (this.userIdIsNumber) {
				try {
					uid = Value.newIntegerValue(
							Integer.parseInt(this.autoLoginUserId));
				} catch (Exception e) {
					msgs.add("autoLoginUserId is set to " + this.autoLoginUserId
							+ " but it has to be a number because userIdIsNumber is set to true. Auto login is not enabled.");
				}
			} else {
				uid = Value.newTextValue(this.autoLoginUserId);
			}
		}
		HttpAgent.setUp(uid, cacher, listener, this.sendTraceToClient);
		String result = null;
		if (msgs.size() > 0) {
			/*
			 * we got errors.
			 */
			StringBuilder err = new StringBuilder("Error while bootstrapping\n");
			for (String msg : msgs) {
				err.append(msg).append('\n');
			}
			result = err.toString();
			Tracer.trace(result);
		}
		/*
		 * we will output all the messages to console as well, just in case the
		 * logging set-up is not ok, or it is sending to some place the poor
		 * programmer is unable to figure out
		 */
		System.out.println(Tracer.stopAccumulation());
		return result;
	}

	/**
	 * validate attributes
	 *
	 * @param ctx
	 *            to which any error found is to be added
	 * @return number of errors added
	 */
	public int validate(ValidationContext ctx) {
		int count = 0;
		/*
		 * name is a must. Else we will have identity crisis!!
		 */
		if (this.applicationId == null) {
			ctx.addError(
					"applicationId must be specified as a unique id for your application on your corporate network. This id can be used for inter-application communication.");
			count++;
		}

		/*
		 * check class references
		 */
		if (this.classInError(AccessController.class, this.accessController,
				"accessControllerClassName", ctx)) {
			count++;
		}
		if (this.classInError(ServiceCacheManager.class,
				this.serviceCacheManager, "serviceCacheManager", ctx)) {
			count++;
		}
		if (this.classInError(ExceptionListener.class, this.exceptionListener,
				"exceptionListenerClassName", ctx)) {
			count++;
		}
		if (this.classInError(AttachmentAssistant.class,
				this.attachmentAssistant, "attachmentAssistantClass", ctx)) {
			count++;
		}
		if (this.classInError(AttachmentAssistant.class, this.traceWrapper,
				"traceWrapper", ctx)) {
			count++;
		}

		/*
		 * check service references
		 */
		if (this.serviceInError(this.loginServiceName, ctx)) {
			count++;
		}
		if (this.serviceInError(this.logoutServiceName, ctx)) {
			count++;
		}

		if (this.attachmentsFolderPath != null) {
			File file = new File(this.attachmentsFolderPath);
			if (file.exists() == false) {
				ctx.addError("attachmentsFolderPath is set to "
						+ this.attachmentsFolderPath
						+ " but it is not a valid folder path.");
				count++;
			}
			if (this.attachmentAssistant != null) {
				ctx.addError(
						"Choose either built-in attachment manager with attachmntsFolderPath or your own class with mediStorageAssistantClass, but you can not use both.");
			}
		}

		if (this.classInError(ClientCacheManager.class, this.clientCacheManager,
				"clientCacheManager", ctx)) {
			count++;
		}

		if (this.autoLoginUserId != null && this.userIdIsNumber) {
			try {
				Integer.parseInt(this.autoLoginUserId);
			} catch (Exception e) {
				ctx.addError("autoLoginUserId is set to " + this.autoLoginUserId
						+ " but it is to be numeric because userIdIsNumber is set to true");
				count++;
			}
		}
		return count;
	}

	private boolean serviceInError(String serviceName, ValidationContext ctx) {
		if (serviceName == null) {
			return false;
		}
		ServiceInterface service = ComponentManager
				.getServiceOrNull(serviceName);
		if (service == null) {
			ctx.addError(serviceName + " is not a valid service name.");
			return true;
		}
		return false;
	}

	/**
	 * check if a class name provided is suitable for the purpose
	 *
	 * @param klass
	 * @param className
	 * @param attName
	 * @param ctx
	 * @return
	 */
	private boolean classInError(Class<?> klass, String className,
			String attName, ValidationContext ctx) {
		if (className == null) {
			return false;
		}
		try {
			Class<?> cls = Class.forName(className);
			Object obj = cls.newInstance();
			if (klass.isInstance(obj)) {
				return false;
			}
			ctx.addError(attName
					+ " should be set to a class that implements/extends "
					+ klass.getName() + ". " + className
					+ " is valid class but it is not a suitable sub-class");
			return true;

		} catch (Exception e) {
			ctx.addError(attName + " is set to " + className
					+ ". Error while using this class to instantiate an object. "
					+ e.getMessage());
			return true;
		}
	}

	/**
	 * command line utility to run any service. Since this is command line, we
	 * assume that security is already taken care of. (If user could do delete
	 * *.* what am I trying to stop him/her from doing??) We pick-up logged-in
	 * user name. Note that java command line has an option to specify the
	 * login-user. One can use this to run the application as that user
	 *
	 * @param args
	 *            comFolderName serviceName param1=value1 param2=value2 .....
	 */
	public static void main(String[] args){
		myTest(args);
	}

	private static void myTest(String[] args) {
		int nbr = args.length;
		if (nbr < 2) {
			printUsage();
			return;
		}

		String compPath = args[0];
		File file = new File(compPath);
		if (file.exists() == false) {
			System.out.println(compPath
					+ " is not a valid path. Ensure that you give the valid path of to the component root folder as first argument");
			return;
		}

		try {
			bootStrap(compPath);
		} catch (Exception e) {
			System.err.println(
					"error while bootstrapping with compFolder=" + compPath);
			e.printStackTrace(System.err);
			return;
		}

		String serviceName = args[1];
		String user = System.getProperty("user.name");

		String json = null;
		if (nbr > 2) {
			JSONWriter w = new JSONWriter();
			w.object();
			for (int i = 2; i < nbr; i++) {
				String[] parms = args[i].split("=");
				if (parms.length != 2) {
					printUsage();
					System.exit(-3);
				}
				w.key(parms[0]).value(parms[1]);
			}
			w.endObject();
			json = w.toString();
		} else {
			json = "{}";
		}

		System.out.println("path:" + compPath);
		System.out.println("userId:" + user);
		System.out.println("service:" + serviceName);
		System.out.println("request:" + json);

		ServiceData inData = new ServiceData(Value.newTextValue(user),
				serviceName);
		inData.setPayLoad(json);
		ServiceData outData = ServiceAgent.getAgent().executeService(inData);
		System.out.println("response :" + outData.getPayLoad());
		System.out
				.println("message :" + JsonUtil.toJson(outData.getMessages()));
		System.out.println("trace :" + outData.getTrace());

	}

	private static void printUsage() {
		System.out.println(
				"Usage : java  org.simplity.kernel.Applicaiton componentFolderPath serviceName inputParam1=vaue1 ...");
		System.out.println(
				"example : java  org.simplity.kernel.Applicaiton /user/data/ serviceName inputParam1=vaue1 ...");
	}
}