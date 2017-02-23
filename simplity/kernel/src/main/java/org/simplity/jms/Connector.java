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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.jms;

import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.simplity.kernel.ApplicationError;

/**
 * Class that helps other classes that use JMS to get a connection to the JMS
 * provider (implementation). This is similar to DbDriver in its functionality
 *
 * @author simplity.org
 *
 */
public class Connector {

	/**
	 * we use just one connection for our entire application
	 */
	private static Connection connection;

	/*
	 * set-up parameters loaded at Application level
	 */
	/**
	 * initial context jndi name, or null to get default set by admin
	 */
	String initialContext;

	/**
	 * null to use default set by admin.
	 */
	String providerUrl;

	/**
	 * jndi name of connection factory
	 */
	String connectionFactory;

	String sessionPool;

	/**
	 * initial setup. Called by Application on startup
	 *
	 * @throws NamingException
	 * @throws JMSException
	 *
	 * @throws ApplicationError
	 *             : in case of any issue with the set-up
	 */
	public void getReady() throws JMSException, NamingException {

		/*
		 * get JNDI naming context
		 */
		Context ctx;
		/*
		 * initial context and provider url may be set explicitly (typically for
		 * testing) or may be already set by the App Server
		 */
		if (this.initialContext == null || this.providerUrl == null) {
			ctx = new InitialContext();
		} else {
			/*
			 * push these parameters to the context
			 */
			Hashtable<String, String> env = new Hashtable<String, String>();
			env.put(Context.INITIAL_CONTEXT_FACTORY, this.initialContext);
			env.put(Context.PROVIDER_URL, this.providerUrl);
			ctx = new InitialContext(env);
		}

		/*
		 * get a connection from the factory that is set by admin
		 */
		connection = ((ConnectionFactory) ctx.lookup(this.connectionFactory))
				.createConnection();
	}

	/**
	 * get a JMS connection. And, please, please do not close() it or abandon it. Do return it once you are done. I am dependent on
	 * your discipline at this time to avoid memory leakage
	 *
	 * @return connection
	 */
	public static Session getSession() {
		if (connection == null) {
			throw new ApplicationError(
					"JMS is not set up for this application, or an effort to do so has failed.");
		}
		try {
			return connection.createSession(true, 0);
		} catch (JMSException e) {
			throw new ApplicationError(e,
					"Error while creating a JMS session.");
		}
	}

	/**
	 * return the session once you are done.
	 * @param session
	 */
	public void returnSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.close();
		} catch (Exception ignore) {
			// playing it safe
		}
	}
}
