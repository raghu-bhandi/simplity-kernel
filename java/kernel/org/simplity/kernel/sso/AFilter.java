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
package org.simplity.kernel.sso;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Our security policy is simple. Access to anything requires sso
 * authentication.
 *
 * Entire functionality is implemented in SsoAuthenticator. This is just a
 * wrapper to make it a Filter
 *
 * @author simplity.org
 *
 */
public class AFilter implements Filter {

	@Override
	public void init(FilterConfig config) throws ServletException {
		//
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws ServletException, IOException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		String url = request.getQueryString();
		if (url == null || url.length() == 0) {
			url = request.getRequestURL().toString();
		} else {
			url = request.getRequestURL().toString() + '?' + url;
		}

		System.out.println("Filtering : " + url);
		if (SsoAuthenticator.doFilter(request, response)) {
			System.out.println("Cleared " + url);
			chain.doFilter(request, response);
		} else {
			System.out.println("Blocked " + url);
		}
		/*
		 * if not logged-in, response is already redirected..
		 */
	}

	@Override
	public void destroy() {
		// we are not destroyers :-(
	}
}
