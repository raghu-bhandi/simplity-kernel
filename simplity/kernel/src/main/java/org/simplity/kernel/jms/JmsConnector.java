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

package org.simplity.kernel.jms;

import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
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
public class JmsConnector {

	/**
	 * we use just one connection for our entire application
	 */
	private static Connection connection;

	/*
	 * set-up parameters loaded at Applicaiton level
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

	/**
	 * initial setup. Called by Application on startup
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
		 * initial context and provider url may be set explicitly (typically for testing) or may be already set by the App Server
		 */
		if(this.initialContext == null || this.providerUrl == null){
			ctx = new InitialContext();
		}else{
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
		connection  = ((ConnectionFactory)ctx.lookup(this.connectionFactory)).createConnection();
	}


	/**
	 * get a JMS connection
	 * @return connection
	 */
	public static Connection getConnection(){
		if (connection == null) {
			throw new ApplicationError(
					"JMS is not set up for this application, or an effort to do so has failed.");
		}
		return connection;
	}
}
