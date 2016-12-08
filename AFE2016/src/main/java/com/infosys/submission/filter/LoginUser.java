package com.infosys.submission.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.h2.engine.Session;
import org.simplity.http.HttpAgent;

import com.infosys.submission.util.AES;

public class LoginUser implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpreq = (HttpServletRequest) request;
		HttpServletResponse httpres = (HttpServletResponse) response;
		
		String token = httpreq.getParameter("token");
		if (httpreq.getSession().getAttribute("_userIdInSession")==null && token!=null && !token.isEmpty()) {
			try {
				String loggedinUser = AES.decryptAES(token, "mcAX65PTadrrsKQ3");
				HttpAgent.login(loggedinUser, null, httpreq.getSession(false));
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
