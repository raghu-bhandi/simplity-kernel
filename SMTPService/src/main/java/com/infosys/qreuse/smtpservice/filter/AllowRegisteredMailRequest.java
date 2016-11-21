package com.infosys.qreuse.smtpservice.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.simplity.json.JSONObject;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;

public class AllowRegisteredMailRequest implements Filter {
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		ServiceData inData = new ServiceData();
		inData.setServiceName("filter_smtp.registration");
		inData.setUserId(Value.newTextValue((String) request.getAttribute("_userId")));
		String domain = request.getRemoteHost();
		String application = ((HttpServletRequest)request).getHeader("application");
		String apikey = ((HttpServletRequest)request).getHeader("apikey");
    	inData.setPayLoad("{"
			 + "'domain':'abc',"
			 + "'application':'"+ application +"',"
			 + "'apikey':'"+ apikey +"'"
			 + "}");

		ServiceData outData = ServiceAgent.getAgent().executeService(inData);
		System.out.println(outData.getPayLoad());
		JSONObject obj = new JSONObject(outData.getPayLoad());
		if(obj.get("registration")!=null){
			System.out.println("records available");
		}
		System.out.println(outData.getPayLoad());
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

}
