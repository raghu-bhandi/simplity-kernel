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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel;

import java.io.File;

import org.simplity.http.HttpAgent;
import org.simplity.http.HttpCacheManager;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.db.DbVendor;
import org.simplity.kernel.db.SchemaDetail;
import org.simplity.kernel.file.AttachmentAssistant;
import org.simplity.kernel.file.AttachmentManager;
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
	 * @throws Exception
	 *             in case the root folder does not exist, or does not required
	 *             resources
	 */
	public static void bootStrap(String componentFolder) throws Exception {
		Tracer.trace("Bootstrapping with " + componentFolder);
		Exception msg = null;
		Application app = new Application();
		try {
			ComponentType.setComponentFolder(componentFolder);
			XmlUtil.xmlToObject(componentFolder + CONFIG_FILE_NAME, app);
			if (app.applicationId == null) {
				msg = new ApplicationError(
						"Unable to load the configuration component "
								+ CONFIG_FILE_NAME
								+ ". This file is expected to be inside folder "
								+ componentFolder);
			} else {
				app.configure();
			}
		} catch (Exception e) {
			msg = e;
		}

		if (msg == null) {
			return;
		}

		/**
		 * try and pipe exception to the listener..
		 */
		if (app.exceptionListener != null) {
			try {
				ExceptionListener listener = (ExceptionListener) Class
						.forName(app.exceptionListener).newInstance();
				listener.listen(new ServiceData(), msg);
				return;
			} catch (Exception ignore) {
				// we just tried
			}
		}
		throw msg;
	}

	/**
	 * unique name of this application within a corporate. This may be used as
	 * identity while trying to communicate with other applications within the
	 * corporate cluster
	 */
	String applicationId;

	/**
	 * should we send server trace to client?
	 */
	boolean sendTraceToClient;
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
	String httpCacheManager;

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
	 * development. Some corporates have security policy that requires you not
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
	 * configure application based on the settings. This MUST be triggered
	 * before using the app. Typically this would be triggered from start-up
	 * servlet in a web-app
	 */
	public void configure() {
		/*
		 * Set up db driver.
		 */
		DbDriver.initialSetup(this.dbVendor, this.dataSourceName,
				this.dbDriverClassName, this.connectionString, this.logSqls,
				this.schemaDetails);

		/*
		 * set up ServiceAgenet
		 */
		ServiceCacheManager casher = null;
		if (this.serviceCacheManager != null) {
			try {
				casher = (ServiceCacheManager) Class
						.forName(this.serviceCacheManager).newInstance();

			} catch (Exception e) {
				Tracer.trace(this.serviceCacheManager
						+ " could not be used to instantiate a cahce manager. "
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
				Tracer.trace(this.accessController
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
				Tracer.trace(this.exceptionListener
						+ " could not be used to instantiate an exception listener. "
						+ e.getMessage() + " We will work with no listener");
			}
		}
		ServiceAgent.setUp(this.userIdIsNumber, this.loginServiceName,
				this.logoutServiceName, casher, gard, listener);

		/*
		 * Some components like data-type are to be pre-loaded for the app to
		 * work.
		 */
		ComponentType.preLoad();
		if (this.cacheComponents) {
			ComponentType.startCaching();
		}
		Value uid = null;
		HttpCacheManager cacheManager = null;
		if (this.httpCacheManager != null) {
			try {
				cacheManager = (HttpCacheManager) Class
						.forName(this.httpCacheManager).newInstance();

			} catch (Exception e) {
				Tracer.trace(this.httpCacheManager
						+ " could not be used to instantiate a cache manager. "
						+ e.getMessage()
						+ " We will work with no http cache manager");
			}
		}
		if (this.autoLoginUserId != null) {
			if (this.userIdIsNumber) {
				try {
					uid = Value.newIntegerValue(
							Integer.parseInt(this.autoLoginUserId));
				} catch (Exception e) {
					Tracer.trace("autoLoginUserId is set to "
							+ this.autoLoginUserId
							+ " but it has to be a number because userIdIsNumber is set to true. Autologin is not enabled.");
				}
			} else {
				uid = Value.newTextValue(this.autoLoginUserId);
			}
			if (uid != null || cacheManager != null) {
				HttpAgent.setUp(uid, cacheManager, this.sendTraceToClient);
			}
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
				Tracer.trace(e,
						"Error while setting storage asstistant based on class "
								+ this.attachmentAssistant);
			}
		}
		if (ast != null) {
			AttachmentManager.setAssistant(ast);
		}
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
					"applicationId must be specified as a unique id for your applicaiton on your corporate network. This id can be used for inter-application communication.");
			count++;
		}

		/*
		 * check class references
		 */
		if (this.classInError(AccessController.class, this.accessController,
				"accessControllerClassName", ctx)) {
			count++;
		}
		if (this.classInError(HttpCacheManager.class, this.httpCacheManager,
				"httpCacheManager", ctx)) {
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

		/*
		 * check service references
		 */
		if (this.serviceInError(this.loginServiceName, ctx)) {
			count++;
		}
		if (this.serviceInError(this.logoutServiceName, ctx)) {
			count++;
		}

		/*
		 *
		 */
		if (this.autoLoginUserId != null && this.userIdIsNumber) {
			try {
				Integer.parseInt(this.autoLoginUserId);
			} catch (Exception e) {
				ctx.addError("autoLoginUserId is set to " + this.autoLoginUserId
						+ " but it is to be numeric because userIdIsNumber is set to true");
				count++;
			}
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
						"Choose either built-in attachment manager with attachmntsFolderPath or your own calss with mediStorageAssistantClass, but you can not use both.");
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
			ctx.addError(serviceName + " is not a vaid service name.");
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
	public static void main(String[] args) {
		// String[] parms = { "c:/repos/simplity/WebContent/WEB-INF/comp/",
		// "tutorial.createSheet" };
		// myTest(parms);
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
					+ " is not a valid path. Esnure that you give the valid path of to the component root folder as first argument");
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
				"example : java  org.simplity.kernel.Applicaiton /usr/data/ serviceName inputParam1=vaue1 ...");
	}
}