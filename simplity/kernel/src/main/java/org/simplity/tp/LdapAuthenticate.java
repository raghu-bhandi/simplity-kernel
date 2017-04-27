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

import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.ldap.LdapProperties;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceProtocol;

/**
 * @author simplity.org
 *
 */
public class LdapAuthenticate extends Action {
	private DirContext ldapCtx;

	String principal;
	String credentials;

	private String parsedPrincipal;
	private String parsedCredentials;

	private Value parsedPrincipalValue;
	private Value parsedCredentialsValue;

	@Override
	protected Value doAct(ServiceContext ctx) {
		if (this.parsedPrincipal != null) {
			this.parsedPrincipalValue = ctx.getValue(this.parsedPrincipal);
		} else {
			this.parsedPrincipalValue = Value.newTextValue(this.principal);
		}

		if (this.parsedCredentials != null) {
			this.parsedCredentialsValue = ctx.getValue(this.parsedCredentials);
		} else {
			this.parsedCredentialsValue = Value.newTextValue(this.credentials);
		}

		Hashtable<String, String> env = new Hashtable<String, String>(11);
		DirContext ldapCtx = null;
		try {
			// Create initial context
			ldapCtx = LdapProperties.getInitialDirContext(parsedPrincipalValue.toText(), parsedCredentialsValue.toText());
			if (ldapCtx == null) {
				ctx.addMessage("kernel.invalidLogin", "");
				return Value.VALUE_FALSE;
			}
		} finally {
			if (ldapCtx != null)
				try {
					ldapCtx.close();
				} catch (NamingException e) {
					e.printStackTrace();
				}
		}
		ctx.setValue(ServiceProtocol.USER_ID, Value.newTextValue(parsedPrincipalValue.toText()));
		return Value.VALUE_TRUE;
	}

	@Override
	public void getReady(int idx, Service service) {
		super.getReady(idx, service);
		this.parsedPrincipal = TextUtil.getFieldName(this.principal);
		this.parsedCredentials = TextUtil.getFieldName(this.credentials);
	}

	@Override
	public int validate(ValidationContext ctx, Service service) {
		int count = super.validate(ctx, service);
		if (this.principal == null) {
			ctx.addError("principal is a required field");
			count++;
		}
		if (this.credentials == null) {
			ctx.addError("credentials is a required field");
			count++;
		}
		return count;
	}
}
