/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.service.ServiceProtocol;

/**
 * Our recommendation is that the client application should be designed to
 * handle login-service...-logout paradigm. It should keep track of login
 * status. Hence we have a separate url for login, service, and logout.
 *
 * This is a dummy servlet that is useful during development. Just send userId
 * and userToken has header fields, and we log-you in nicely
 *
 * @author simplity.org
 *
 */
public class DefaultLogin extends HttpServlet {

	/*
	 * of course we will have several other issues like logging....
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		/*
		 * TODO : work with SSO or any specific login-strategy in your project
		 * and do establish credentials of logged-in user
		 */

		String userId = req.getHeader(ServiceProtocol.USER_ID);
		String userToken = req.getHeader(ServiceProtocol.USSER_TOKEN);
		String csrf = HttpAgent.login(userId, userToken, req.getSession(true));
		if (csrf != null) {
			resp.setHeader(ServiceProtocol.CSRF_HEADER, csrf);
		}
	}
}
