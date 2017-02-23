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

import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.simplity.kernel.ldap.LdapAgent;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
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
		try {
			String prinicpal = ctx.getTextValue(ServiceProtocol.USER_ID);
			String credentials = ctx.getTextValue(ServiceProtocol.USER_TOKEN);
			// Create initial context
			DirContext ldapCtx = LdapAgent.getInitialDirContext(prinicpal,credentials);
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
