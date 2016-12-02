package com.infosys.qreuse.smtpservice.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.simplity.http.HttpAgent;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONException;
import org.simplity.json.JSONObject;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;

public class AllowRegisteredMailRequest implements Filter {
	private static final String SESSION_NAME_FOR_MAP = "userSessionMap";

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)	{
		try {
			HttpSession session = ((HttpServletRequest) request).getSession(true);
			if (session.getAttribute("_userIdInSession") == null) {
				HttpAgent.login("100", null, session);
			};
			String servicename = ((HttpServletRequest) request).getHeader("_serviceName");
			if (servicename.equals("smtp.mailer")) {
				ServiceData inData = new ServiceData();
				inData.setServiceName("filter_smtp.registration");
				inData.setUserId(Value.newTextValue((String) request.getAttribute("_userId")));
				String domain = request.getRemoteHost();
				String application = ((HttpServletRequest) request).getHeader("application");
				String apikey = ((HttpServletRequest) request).getHeader("apikey");
				inData.setPayLoad("{" + "'domain':'" + domain + "'," + "'application':'" + application + "'," + "'apikey':'"
						+ apikey + "'" + "}");
				ServiceData outData = ServiceAgent.getAgent().executeService(inData);
				JSONObject obj = new JSONObject(outData.getPayLoad());

				if (!obj.get("registration").toString().equals("null")) {
					@SuppressWarnings("unchecked")
					Map<String, Object> sessionData = (Map<String, Object>) session.getAttribute(SESSION_NAME_FOR_MAP);
					sessionData.put("fromId",
							new JSONArray(obj.get("registration").toString()).getJSONObject(0).get("mailid"));
					chain.doFilter(request, response);
					return;
				}
				((HttpServletResponse) response).sendError(404, "Not Authorized");
				return;
			}

			chain.doFilter(request, response);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ServletException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
