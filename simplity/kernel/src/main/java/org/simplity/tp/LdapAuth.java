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

package org.simplity.tp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceInterface;
import org.simplity.service.ServiceProtocol;

/**
 * @author simplity.org
 *
 */
public class LdapAuth extends Action {

	Hashtable<String, String> env = new Hashtable<String, String>(11);
	String providerUrl;

	@Override
	protected Value doAct(ServiceContext ctx) {
		// Set up the environment for creating the initial context
		Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, providerUrl);

		// Authenticate with user and password
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, ctx.getTextValue(ServiceProtocol.USER_ID));
		env.put(Context.SECURITY_CREDENTIALS, ctx.getTextValue(ServiceProtocol.USER_TOKEN));

		try {
			// Create initial context
			DirContext ldapCtx = new InitialDirContext(env);
			ldapCtx.getEnvironment();

			// Close the context when we're done
			ldapCtx.close();
		} catch (NamingException e) {
			ctx.removeValue(ServiceProtocol.USER_ID);
			ctx.removeValue(ServiceProtocol.USER_TOKEN);
			e.printStackTrace();
		}
		return Value.VALUE_TRUE;
	}

}
