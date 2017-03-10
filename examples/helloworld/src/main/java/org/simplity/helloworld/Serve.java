package org.simplity.helloworld;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.http.HttpAgent;
import org.simplity.http.Startup;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;

/**
 * this is an servlet to provides the restful parsing of the URL
 *
 * @author simplity.org
 *
 */
public class Serve extends HttpServlet {

	/*
	 * we may have to co-exist with other application. It is possible that our
	 * start-up never started. One check may not be too expensive. Our start-up
	 * calls keep this as marker..
	 */
	private static boolean startedUp = false;
	private static boolean startUpFailed = false;
	/*
	 * of course we will have several other issues like logging....
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * notify that the start-up is successful, and we can go ahead and serve
	 *
	 * @param succeeded
	 *            Success flag
	 */
	public static void updateStartupStatus(boolean succeeded) {
		if (succeeded) {
			startedUp = true;
			Tracer.trace("Web Agent is given a green signal by Startup to start serving.");
		} else {
			startUpFailed = true;
			Tracer.trace(
					"Web agent Serve will not be available on this server as Startup reported a failure on boot-strap.");
		}
	}

	/**
	 * post is to be used by client in AJAX call.
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (startedUp == false) {
			if (startUpFailed == false) {
				Startup.bootStrap(this.getServletContext());
			}
			if (startUpFailed) {
				/*
				 * application had error during bootstrap.
				 */
				this.reportError(resp,
						"Application start-up had an error. Please refer to the logs. No service is possible.");
				return;
			}
		}
		try {			
			String serviceName = null;
			String[] tokens;
			if (req.getPathInfo() != null) {
				tokens = req.getPathInfo().split("/");
				for(int i=tokens.length-1;i>=0;i--){
					if(i==tokens.length-2){
						serviceName = req.getMethod().toLowerCase().concat(".").concat(serviceName);
					}
					if(!tokens[i].isEmpty()){
						if(serviceName!=null)
							serviceName = tokens[i].concat(".").concat(serviceName);
						else{
							serviceName = tokens[i];
						}
					}
				}				
			}
			HttpAgent.serve(req, resp, serviceName);
		} catch (Exception e) {
			String msg = "We have an internal error. ";
			String trace = Tracer.stopAccumulation();
			Tracer.trace(trace);
			Tracer.trace(e, msg);
			this.reportError(resp, msg + e.getMessage());
		}
	}

	/**
	 * Get is to be used ONLY IF POST is not possible for some reason. From
	 * security angle POST is preferred
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	private void reportError(HttpServletResponse resp, String msg) throws IOException {
		Tracer.trace(msg);
		FormattedMessage message = new FormattedMessage("internalerror", MessageType.ERROR, msg);
		FormattedMessage[] messages = { message };
		String response = HttpAgent.getResponseForError(messages);
		resp.getWriter().write(response);
	}
}
