package com.infosys.submission.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.StringTokenizer;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.simplity.service.ServiceProtocol;

public class LoginFilter implements Filter {
	private ArrayList urlList;

	public void destroy() {
		// TODO Auto-generated method stub

	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		String url = request.getServletPath();
		boolean allowedRequest = false;
		String strURL = "";

		// To check if the url can be excluded or not
		for (int i = 0; i < urlList.size(); i++) {
			strURL = urlList.get(i).toString();
			if (url.startsWith(strURL)) {
				allowedRequest = true;
			}
		}

		if (!allowedRequest) {
			HttpSession session = request.getSession(false);
			if (session == null || session.getAttribute("_userIdInSession") == null) {
				request.getRequestDispatcher("/login.jsp").forward(request, response);
			}
		}
		chain.doFilter(req, res);
	}

	public void init(FilterConfig config) throws ServletException {
		// Read the URLs to be avoided for authentication check (From web.xml)
		String urls = config.getInitParameter("avoid-urls");
		StringTokenizer token = new StringTokenizer(urls, ",");
		urlList = new ArrayList();
		while (token.hasMoreTokens()) {
			urlList.add(token.nextToken());
		}
	}

}
