package com.infosys.submission.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Base64;

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

	public void destroy() {
		// TODO Auto-generated method stub

	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		if(request.getServletPath().startsWith("/public")){
			chain.doFilter(req, res);// sends request to next resource
			return;
		}
		
		HttpSession session = request.getSession(false);
		String loginURI = request.getContextPath() + "/login.html";

		boolean loggedIn = session != null && session.getAttribute("user") != null;
		boolean loginRequest = request.getRequestURI().equals(loginURI);
		boolean loginURL = request.getRequestURI().endsWith("._i");

		if (loggedIn || loginRequest || loginURL) {
			chain.doFilter(request, response);
		} else {
			response.sendRedirect(loginURI);
		}
	}


	public void init(FilterConfig arg0) throws ServletException {
		System.out.println("Initialized");
	}

}
