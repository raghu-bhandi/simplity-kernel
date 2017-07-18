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

package org.simplity.auth;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.util.HttpUtil;
import org.simplity.rest.Tags;

/**
 * Handle Oauth2 authentication
 *
 * @author simplity.org
 *
 */
public class Oauth2Agent implements SecurityAgent {

	/**
	 * name of the field in which we expect the token
	 */
	private final String fieldName;
	/**
	 * true if token is expected in the header. Else it is expected in query
	 * string
	 */
	private final boolean inHeader;
	private final String flow;
	private final String authorizationUrl;
	private final String tokenUrl;
	private final String[] scopes;

	/**
	 * initialize with specifications
	 *
	 * @param specs
	 */
	public Oauth2Agent(JSONObject specs) {
		this.fieldName = specs.getString(Tags.PARAM_NAME_ATTR);
		this.inHeader = Tags.IN_HEADER.equals(specs.getString(Tags.IN_ATTR));
		this.flow = specs.getString(Tags.FLOW_ATTR);
		this.authorizationUrl = specs.getString(Tags.AUTH_URL_ATTR);
		this.tokenUrl = specs.getString(Tags.TOKEN_URL_ATTR);
		JSONObject scoops = specs.optJSONObject(Tags.SCOPES_ATTR);
		if (scoops != null) {
			this.scopes = JSONObject.getNames(scoops);
		} else {
			this.scopes = new String[0];
		}
	}

	/**
	 *
	 * @param req
	 * @param resp
	 * @return true if cleared. false if response is redirected to
	 *         authentication site. Caller should end this request.
	 * @throws IOException
	 */
	public boolean securityCleared(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String token = this.parseToken(req);
		if (token == null) {
			// TODO: Is this just this or do we add anything else to query
			// string???
			resp.sendRedirect(this.authorizationUrl);
			return false;
		}
		// TODO : contact token url and confirm, or check in our cache of valid
		// tokens
		return true;

	}

	/**
	 * parse token form header/query string
	 *
	 * @param req
	 * @return
	 */
	private String parseToken(HttpServletRequest req) {
		if (this.inHeader) {
			return req.getHeader(this.fieldName);
		}

		String qry = req.getQueryString();
		if (qry == null) {
			return null;
		}

		String[][] params = HttpUtil.parseQueryString(qry);
		if (params == null) {
			return null;
		}

		for (String[] pair : params) {
			if (this.fieldName.equals(pair[0])) {
				return pair[1];
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.simplity.auth.SecurityAgent#securityCleared(java.lang.Object[])
	 */
	@Override
	public boolean securityCleared(Object... params) {
		try{
			return this.securityCleared((HttpServletRequest)params[0], (HttpServletResponse)params[1]);
		}catch(Exception e){
			throw new ApplicationError(e, "Invalid parametrs passed to securityCleared()");
		}
	}

}
