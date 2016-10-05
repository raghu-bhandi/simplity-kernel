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
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceProtocol;

/**
 * helper to be used when this application manages the session on its own. This
 * helper is designed to allow only one session per userId. Hence, it keeps the
 * token itself as a session object, and avoids creation of multiple sessions
 * per userId
 *
 * @author simplity.org
 *
 */
public class DefaultHelper implements SessionHelper {
	/*
	 * token is stored in a session variable. if we want to provide multiple
	 * sessions, we make this a set of tokens.
	 */
	private static final String USER_TOKEN = "_userToken";

	@Override
	public void removeSession(HttpSession session, String token) {
		Object val = session.getAttribute(USER_TOKEN);
		if (val != null) {
			session.removeAttribute(USER_TOKEN);
			session.removeAttribute(val.toString());
		}
	}

	@Override
	public boolean getSessionData(HttpSession session, String token,
			ServiceData inData) {
		@SuppressWarnings("unchecked")
		Map<String, Object> sessionData = (Map<String, Object>) session
				.getAttribute(token);
		if (sessionData == null) {
			return false;
		}

		for (Map.Entry<String, Object> entry : sessionData.entrySet()) {
			inData.put(entry.getKey(), entry.getValue());
		}

		return true;
	}

	@Override
	public void setSessionData(HttpSession session, String token,
			ServiceData data) {
		@SuppressWarnings("unchecked")
		Map<String, Object> sessionData = (Map<String, Object>) session
				.getAttribute(token);
		if (sessionData == null) {
			Tracer.trace("No sesison data exists.");
		} else {
			for (String key : data.getFieldNames()) {
				sessionData.put(key, data.get(key));
			}
		}
	}

	@Override
	public String newSession(HttpSession session, ServiceData data,
			String existingToken) {
		/*
		 * remove existing session data, if any
		 */
		Object val = session.getAttribute(USER_TOKEN);
		if (val != null) {
			session.removeAttribute(val.toString());
		}

		val = data.get(ServiceProtocol.USER_ID);
		if (val == null) {
			Tracer.trace("Session data does not have value for field "
					+ ServiceProtocol.USER_ID
					+ " and hence we assume that the login has failed.");
			return null;
		}
		if (val instanceof Value == false) {
			throw new ApplicationError("User id field "
					+ ServiceProtocol.USER_ID
					+ " should have a value of type Value");
		}

		String newToken = UUID.randomUUID().toString();
		session.setAttribute(USER_TOKEN, newToken);
		Map<String, Object> sessionData = new HashMap<String, Object>();
		for (String key : data.getFieldNames()) {
			sessionData.put(key, data.get(key));
		}
		session.setAttribute(newToken, sessionData);
		return newToken;
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
		return (String) session.getAttribute(USER_TOKEN);
	}
}
