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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.util.HttpUtil;
import org.simplity.rest.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sun.corba.se.pept.transport.Connection;

/**
 * Handle Oauth2 authentication
 *
 * @author simplity.org
 *
 */
public class OAuth2Agent implements SecurityAgent {
	Logger logger = LoggerFactory.getLogger(OAuth2Agent.class);
			
	/**
	 * true if token is expected in the header. Else it is expected in query
	 * string
	 */
    private final String type = "oauth2";
    private final String authorizationUrl;
    private final String tokenUrl;
    private final String flow;    
	private final String description;
	private Map<String, String> scopes;

	/**
	 * initialize with specifications
	 *
	 * @param specs
	 */
	public OAuth2Agent(JSONObject specs) {
		this.flow = specs.getString(Tags.FLOW_ATTR);
		this.authorizationUrl = specs.getString(Tags.AUTH_URL_ATTR);
		this.tokenUrl = specs.optString(Tags.TOKEN_URL_ATTR);
		JSONObject scopes = specs.optJSONObject(Tags.SCOPES_ATTR);
//		if (scoops != null) {
//			this.scopes = JSONObject.getNames(scoops);
//		} else {
//			this.scopes = new String[0];
//		}
		this.description = specs.optString(Tags.DESC);
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
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is required");
			return false;
		}			
		return checkForValidToken(token);
	}

	private boolean checkForValidToken(String token) {
		String url = this.tokenUrl;
		url += "?access_token="
				+ token
				+ "&correlationId="
				+ MDC.get("correlationId");
		HttpURLConnection conn = null;
		logger.info("Checking token " + url);
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();			
			if(conn.getResponseCode()==HttpServletResponse.SC_OK){
				return true;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
		return false;
	}

	/**
	 * parse token form header/query string
	 *
	 * @param req
	 * @return
	 */
	private String parseToken(HttpServletRequest req) {
		String qry = req.getHeader(Tags.ACCESS_TOKEN);
		if(qry==null){
			qry = req.getParameter(Tags.ACCESS_TOKEN);
		}
		return qry;		
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
