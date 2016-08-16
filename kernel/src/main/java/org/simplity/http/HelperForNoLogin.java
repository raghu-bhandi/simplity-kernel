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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceProtocol;

/**
 * helper to be used when login is not required, typically during development.
 *
 * @author simplity.org
 *
 */
public class HelperForNoLogin implements SessionHelper {
	private static final String DUMMY_TOKEN = "_0_";
	private static final Value DUMMY_USER_ID = Value.newTextValue("100");

	@Override
	public boolean getSessionData(HttpSession session, String token,
			ServiceData inData) {
		@SuppressWarnings("unchecked")
		Map<String, Object> sessionData = (Map<String, Object>) session
				.getAttribute(DUMMY_TOKEN);
		if (sessionData == null) {
			sessionData = new HashMap<String, Object>();
			sessionData.put(ServiceProtocol.USER_ID, DUMMY_USER_ID);
			session.setAttribute(DUMMY_TOKEN, sessionData);
		} else {
			for (Map.Entry<String, Object> entry : sessionData.entrySet()) {
				inData.put(entry.getKey(), entry.getValue());
			}
		}
		return true;
	}

	@Override
	public void setSessionData(HttpSession session, String token,
			ServiceData data) {
		@SuppressWarnings("unchecked")
		Map<String, Object> sessionData = (Map<String, Object>) session
				.getAttribute(DUMMY_TOKEN);
		if (sessionData == null) {
			sessionData = new HashMap<String, Object>();
			session.setAttribute(DUMMY_TOKEN, sessionData);
		}
		for (String key : data.getFieldNames()) {
			sessionData.put(key, data.get(key));
		}
	}

	@Override
	public String newSession(HttpSession session, ServiceData data, String token) {
		Map<String, Object> sessionData = new HashMap<String, Object>();
		sessionData.put(ServiceProtocol.USER_ID, DUMMY_USER_ID);
		for (String key : data.getFieldNames()) {
			sessionData.put(key, data.get(key));
		}
		session.setAttribute(DUMMY_TOKEN, sessionData);
		return DUMMY_TOKEN;
	}

	@Override
	public void removeSession(HttpSession session, String token) {
		session.removeAttribute(DUMMY_TOKEN);
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
		return DUMMY_TOKEN;
	}
}
