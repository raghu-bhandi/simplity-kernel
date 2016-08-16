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

import org.simplity.http.HttpAgent;
import org.simplity.http.PassiveHelper;
import org.simplity.http.SessionHelper;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.db.DbVendor;
import org.simplity.kernel.db.SchemaDetail;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.AccessController;
import org.simplity.service.CacheManager;
import org.simplity.service.ExceptionListener;
import org.simplity.service.ServiceAgent;

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
		ComponentManager.setComponentFolder(componentFolder);
		Application app = new Application();
		ComponentManager.loadObject(app, CONFIG_FILE_NAME);
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
	 * session helper is a class that implements
	 * org.simplity.http.WebSessionHelper interface. Simplity uses an instance
	 * of this class to manage session variables. Session management is also
	 * associated with login requirements.Simplity provides three default
	 * classes. HelperForNoLogin during development if session variables are not
	 * used, DefaultHelper that uses a simple session management, and
	 * PassiveHelper when this application uses Simplity only as a module, and
	 * session is not managed by Simplity. You may write your session manager
	 * based on the application design
	 */
	String sessionHelperClassName;

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

		ComponentManager.initialLoad();
		if (this.cacheComponents) {
			ComponentManager.startCaching();
		}
		if (this.sessionHelperClassName != null) {
			try {
				SessionHelper helper = (SessionHelper) Class.forName(
						this.sessionHelperClassName).newInstance();
				if (helper instanceof PassiveHelper) {
					ValueType vt = this.userIdIsNumber ? ValueType.INTEGER
							: ValueType.TEXT;
					((PassiveHelper) helper).setUp(this.sessionParams,
							this.userIdNameInSession, vt);
				}
				HttpAgent.setUp(helper);
			} catch (Exception e) {
				Tracer.trace(this.sessionHelperClassName
						+ " could not be used to instantiate a sesion helper. "
						+ e.getMessage() + " we will use a default helper.");
			}
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
		return 0;
	}
}