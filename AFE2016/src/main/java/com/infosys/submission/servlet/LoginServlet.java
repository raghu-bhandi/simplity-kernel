package com.infosys.submission.servlet;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.chrono.IsoEra;
import java.util.Base64;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.http.DefaultLogin;
import org.simplity.http.HttpAgent;
import org.simplity.service.ServiceProtocol;

public class LoginServlet extends DefaultLogin {
	private static final long serialVersionUID = 7262315326588566300L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		final String userPrincipal = ((HttpServletRequest) req).getHeader("userPrincipal");
		if(userPrincipal!=null){
			HttpAgent.login(userPrincipal, "", req.getSession(true));
			return;
		}
		
		final String authorization = ((HttpServletRequest) req).getHeader("Authorization");
		String[] values = null;
		if (authorization != null && authorization.startsWith("Basic")) {
			// Authorization: Basic base64credentials
			String base64Credentials = authorization.substring("Basic".length()).trim();
			String credentials = new String(Base64.getDecoder().decode(base64Credentials), Charset.forName("UTF-8"));
			values = credentials.split(":", 2);
		}

		if (values != null && (values[0].equals("encore")||values[0].equals("sponsor"))) {
			String userId = values[0];
			String userToken = values[0];
			HttpAgent.login(userId, userToken, req.getSession(true));
		} else {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not authorized");
		}
	}
}
