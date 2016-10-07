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
import org.simplity.service.CacheManager;
import org.simplity.service.ExceptionListener;
import org.simplity.service.ServiceAgent;
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
		ComponentType.setComponentFolder(componentFolder);
		Application app = new Application();
		XmlUtil.xmlToObject(componentFolder + CONFIG_FILE_NAME, app);
		if (app.applicationId == null) {
			throw new Exception("Unable to load the configuration component "
					+ CONFIG_FILE_NAME
					+ ". This file is expected to be inside folder "
					+ componentFolder);
		}
		app.configure();
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
	 * Response for certain services can be cached. Also, during development, we
	 * may want to fake some services with a pre-determined response. Use a
	 * class that implements org.simplity.service.CacheManager interface
	 */
	String cacheManagerClassName;

	/**
	 * class that decides whether a userId be served a given service
	 */
	String accessControllerClassName;
	/**
	 * way to wire exception to corporate utility
	 */
	String exceptionListenerClassName;

	/**
	 * should the sqls that are executed be added to trace?? Required during
	 * development. Some corporates have security policy that requires you not
	 * log sqls
	 */
	boolean logSqls;

	/**
	 * session parameters
	 */
	Parameter[] sessionParams;

	/**
	 * name of the session parameter that has the value for userId
	 */
	String userIdNameInSession;

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
	String attachmentAssistantClass;

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
		CacheManager casher = null;
		AccessController gard = null;
		ExceptionListener listener = null;
		if (this.cacheManagerClassName != null) {
			try {
				casher = (CacheManager) Class.forName(
						this.cacheManagerClassName).newInstance();

			} catch (Exception e) {
				Tracer.trace(this.cacheManagerClassName
						+ " could not be used to instantiate a cahce manager. "
						+ e.getMessage() + " We ill work with no cache manager");
			}
		}
		if (this.accessControllerClassName != null) {
			try {
				gard = (AccessController) Class.forName(
						this.accessControllerClassName).newInstance();

			} catch (Exception e) {
				Tracer.trace(this.accessControllerClassName
						+ " could not be used to instantiate access controller. "
						+ e.getMessage()
						+ " We will work with no cache manager");
			}
		}
		if (this.exceptionListenerClassName != null) {
			try {
				listener = (ExceptionListener) Class.forName(
						this.exceptionListenerClassName).newInstance();

			} catch (Exception e) {
				Tracer.trace(this.exceptionListenerClassName
						+ " could not be used to instantiate an exception listener. "
						+ e.getMessage() + " We will work with no listener");
			}
		}
		ServiceAgent.setUp(this.userIdIsNumber, this.loginServiceName,
				this.logoutServiceName, this.sendTraceToClient, casher, gard,
				listener);

		/*
		 * Some components like data-type are to be pre-loaded for the app to
		 * work.
		 */
		ComponentType.preLoad();
		if (this.cacheComponents) {
			ComponentType.startCaching();
		}
		if (this.autoLoginUserId != null) {
			Value uid = null;
			if (this.userIdIsNumber) {
				try {
					uid = Value.newIntegerValue(Integer
							.parseInt(this.autoLoginUserId));
				} catch (Exception e) {
					Tracer.trace("autoLoginUserId is set to "
							+ this.autoLoginUserId
							+ " but it has to be a number because userIdIsNumber is set to true. Autologin is not enabled.");
				}
			} else {
				uid = Value.newTextValue(this.autoLoginUserId);
			}
			if (uid != null) {
				HttpAgent.setUp(uid);
			}
		}
		/*
		 * what about file/media/attachment storage assistant?
		 */
		AttachmentAssistant ast = null;
		if (this.attachmentsFolderPath != null) {
			ast = new FileBasedAssistant(this.attachmentsFolderPath);
		} else if (this.attachmentAssistantClass != null) {
			try {
				ast = (AttachmentAssistant) Class.forName(
						this.attachmentAssistantClass).newInstance();
			} catch (Exception e) {
				Tracer.trace(e,
						"Error while setting storage asstistant based on class "
								+ this.attachmentAssistantClass);
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
		if (this.applicationId == null) {
			ctx.addError("applicationId must be specified as a unique id for your applicaiton on your corporate network. This id can be used for inter-application communication.");
			count++;
		}

		if (this.classInError(AccessController.class,
				this.accessControllerClassName, "accessControllerClassName",
				ctx)) {
			count++;
		}
		if (this.classInError(CacheManager.class, this.cacheManagerClassName,
				"cacheManagerClassName", ctx)) {
			count++;
		}
		if (this.classInError(ExceptionListener.class,
				this.exceptionListenerClassName, "exceptionListenerClassName",
				ctx)) {
			count++;
		}
		if (this.autoLoginUserId != null && this.userIdIsNumber) {
			try {
				Integer.parseInt(this.autoLoginUserId);
			} catch (Exception e) {
				ctx.addError("autoLoginUserId is set to "
						+ this.autoLoginUserId
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
			if (this.attachmentAssistantClass != null) {
				ctx.addError("Choose either built-in attachment manager with attachmntsFolderPath or your own calss with mediStorageAssistantClass, but you can not use both.");
			}
		}
		if (this.classInError(AttachmentAssistant.class,
				this.attachmentAssistantClass, "attachmentAssistantClass", ctx)) {
			count++;
		}
		if (this.serviceInError(this.loginServiceName, ctx)) {
			count++;
		}
		if (this.serviceInError(this.logoutServiceName, ctx)) {
			count++;
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
			ctx.addError(attName
					+ " is set to "
					+ className
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
		String[] parms = { "e:/repos/simplity/WebContent/WEB-INF/comp/",
				"tutorial.createSheet" };
		myTest(parms);
		// myTest(args);
	}

	private static void myTest(String[] args) {
		int nbr = args.length;
		if (nbr < 2) {
			printUsage();
			System.exit(-1);
		}

		String compPath = args[0];
		File file = new File(compPath);
		if (file.exists() == false) {
			System.out
					.println(compPath
							+ " is not a valid path. Esnure that you give the valid path of to the component root folder as first argument");
			System.exit(-1);
		}

		try {
			bootStrap(compPath);
		} catch (Exception e) {
			System.err.println("error while bootstrapping with compFolder="
					+ compPath);
			e.printStackTrace(System.err);
			System.exit(-2);
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

		ServiceData inData = new ServiceData();
		inData.setServiceName(serviceName);
		inData.setUserId(Value.newTextValue(user));
		inData.setPayLoad(json);
		ServiceData outData = ServiceAgent.getAgent().executeService(inData);
		System.out.println("response :" + outData.getPayLoad());
		System.out
				.println("message :" + JsonUtil.toJson(outData.getMessages()));
		System.out.println("trace :" + outData.getTrace());

	}

	private static void printUsage() {
		System.out
				.println("Usage : java  org.simplity.kernel.Applicaiton componentFolderPath serviceName inputParam1=vaue1 ...");
		System.out
				.println("example : java  org.simplity.kernel.Applicaiton /usr/data/ serviceName inputParam1=vaue1 ...");
	}
}