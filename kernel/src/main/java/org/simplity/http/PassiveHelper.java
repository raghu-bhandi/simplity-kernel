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
package org.simplity.http;

import javax.servlet.http.HttpSession;

import org.simplity.kernel.Parameter;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceProtocol;

/**
 * helper to be used when this application is deployed as part of another
 * application. That is this application co-exists with another application, and
 * that application actually manages session etc.. We assume that the active
 * application has name-value pairs in session, and our job is to pick them into
 * a userSessionData object
 *
 * @author simplity.org
 *
 */
public class PassiveHelper implements SessionHelper {
	private Parameter[] sessionParameters;
	private String userIdName;
	private ValueType userIdType;

	/**
	 * set up desired types
	 * 
	 * @param paramNames
	 * @param userIdName
	 * @param userIdType
	 */
	public void setUp(Parameter[] paramNames, String userIdName,
			ValueType userIdType) {
		this.sessionParameters = paramNames;
		this.userIdName = userIdName;
		this.userIdType = userIdType;
	}

	@Override
	public boolean getSessionData(HttpSession session, String token,
			ServiceData inData) {
		Object uid = session.getAttribute(this.userIdName);
		/*
		 * we get token from client if login is enabled
		 */
		if (uid == null) {
			return false;
		}

		Value userId = Value.parseValue(uid.toString(), this.userIdType);
		inData.put(ServiceProtocol.USER_ID, userId);
		if (this.sessionParameters != null) {
			for (Parameter param : this.sessionParameters) {
				String nam = param.getName();
				Object val = session.getAttribute(nam);
				if (val != null) {
					inData.put(nam, param.parseValue(val.toString()));
				}
			}
		}
		return true;
	}

	@Override
	public void setSessionData(HttpSession session, String token,
			ServiceData data) {
		for (Parameter param : this.sessionParameters) {
			String nam = param.getName();
			Object val = data.get(nam);
			if (val != null) {
				session.setAttribute(nam, val.toString());
			}
		}
	}

	@Override
	public void removeSession(HttpSession session, String token) {
		/*
		 * let us remove fields that we know of
		 */
		if (this.sessionParameters == null) {
			return;
		}
		for (Parameter param : this.sessionParameters) {
			session.removeAttribute(param.getName());
		}
	}

	@Override
	public String newSession(HttpSession session, ServiceData data,
			String existingToken) {
		Tracer.trace("As passive helper, I can not create session...");
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.simplity.http.SessionHelper#getUserToken(javax.servlet.http.HttpSession
	 * )
	 */
	@Override
	public String getUserToken(HttpSession session) {
		return null;
	}
}
