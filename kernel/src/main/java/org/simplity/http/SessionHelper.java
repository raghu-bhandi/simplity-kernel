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

import org.simplity.service.ServiceData;

/**
 * We would like to have the flexibility to maintain session on web tier or an
 * app tier based on deployment scenario. This interface is for the web-tier
 *
 * @author simplity.org
 */
public interface SessionHelper {
	/**
	 * extracted session data into inData
	 *
	 * @param session
	 *            http session
	 * @param token
	 *            with which this session data was created (returned by
	 *            createSession() method)
	 * @param inData
	 *            to which session
	 * @return true if session data is available, and extracted into inData.
	 *         False if there is no session data associated with the token for
	 *         this token
	 */
	public boolean getSessionData(HttpSession session, String token,
			ServiceData inData);

	/**
	 * Set/reset session data from inData
	 *
	 * @param session
	 *            http session
	 * @param token
	 *            that was returned when this session was created
	 * @param data
	 *            that has fields to be put to session
	 */
	public void setSessionData(HttpSession session, String token,
			ServiceData data);

	/**
	 * remove session
	 *
	 * @param session
	 *            http session
	 * @param token
	 *            that was returned when this session was created
	 */
	public void removeSession(HttpSession session, String token);

	/**
	 * create session data for this user in this session.
	 *
	 * @param session
	 *            http session
	 * @param data
	 *            to be pushed to session
	 * @param existingToken
	 *            that need to be removed. Null if no session existed for this
	 *            user
	 * @return key/token to this session, to be used for subsequent requests for
	 *         get. null implies that there is no userId, or some issue, and
	 *         hence caller should assume that the session creation has failed
	 */
	public String newSession(HttpSession session, ServiceData data,
			String existingToken);

	/**
	 * @param session
	 *            http session
	 * @return user token, or null if this session has no user token
	 */
	public String getUserToken(HttpSession session);
}
