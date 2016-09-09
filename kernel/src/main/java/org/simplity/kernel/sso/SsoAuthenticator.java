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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Interfaces with org-wide SSO set up for the domain and authenticates an HTTP
 * request. Must be set up on start-up of server. Can be used from filter, as
 * well as from service managers
 *
 * Also has methods using which a dummy sso server can be set up for testing or
 * for development phase.
 *
 * It is important to have very clear understanding about the various parts of
 * url. FOllowing link has a good example.
 *
 * <pre>
 * http://stackoverflow.com/questions/4931323/whats-the-difference-between-getrequesturi-and-getpathinfo-methods-in-httpservl
 *
 *
 * important info is copied below
 *
 * http://30thh.loc:8480/app/test%3F/a%3F+b;jsessionid=S%3F+ID?p+1=c+d&p+2=e+f#a
 *
 *
 * Method              URL-Decoded Result
 * -------------------------------------------------
 * getContextPath()                /app
 * getLocalAddr()                  127.0.0.1
 * getLocalName()                  30thh.loc
 * getLocalPort()                  8480
 * getMethod()                     GET
 * getPathInfo()           yes     /a?+b
 * getProtocol()                   HTTP/1.1
 * getQueryString()        no      p+1=c+d&p+2=e+f
 * getRequestedSessionId() no      S%3F+ID
 * getRequestURI()         no      /app/test%3F/a%3F+b;jsessionid=S+ID
 * getRequestURL()         no      http://30thh.loc:8480/app/test%3F/a%3F+b;jsessionid=S+ID
 * getScheme()                     http
 * getServerName()                 30thh.loc
 * getServerPort()                 8480
 * getServletPath()        yes     /test?
 * getParameterNames()     yes     [p 2, p 1]
 * getParameter("p 1")     yes     c d
 * </pre>
 *
 * @author simplity.org
 *
 */
class SsoAuthenticator {

	/**
	 * we have put all the context parameter in an array for convenience of
	 * extraction and assigning to attributes
	 */
	private static final String[] ALL_CONTEXT_FIELDS = {
	/**
	 * cookie name that the client uses for sso token
	 */
	"sso-cookie-name",

	/**
	 * url to which a socket is to be opened to validate sso token from client.
	 * This should have all the query fields that the sso site would expect,
	 * like app-id, pwd etc.. token and ip are the two additional fields that
	 * the validator would append at run time. like
	 *
	 * HTTPS://com.myComany/sso/auth?appId=myApp&password=Mypassword etc..
	 */
	"sso-validation-url",

		/**
	 * qry field name with which to append the sso token to the url
	 */
	"sso-qry-name-for-token",

	/**
	 * qry field name with which to append the ip address to the url
	 */
	"sso-qry-name-for-ip",

		/**
	 * field name with which sso returns the status of authentication.
	 *
	 */
	"sso-status-field-name",

		/**
	 * value of sso field name that implies successful authentication
	 */
	"sso-status-success",

	/**
	 * value of sso field name that has the error message in case of sso failure
	 */
	"sso-message-field-name",
		/**
	 * page to redirect-to in case of error during sso process. example
	 * "error.jsp"
	 */
	"sso-error-page",
	/**
	 * example "module1/report.jsp;mdule2/report3.jsp" resource names to be used
	 * after the application path, without query fields etc... This is the list
	 * of requests that are internal, and use different authentication than sso.
	 * We recommend such apps to have sso authentication, rather than this
	 * white-listing. Also, in case you choose to use this, we strongly
	 * recommend that you restrict access to specific ip addresses using the
	 * next parameter
	 */
	"sso-whitelisted-resources",
	/**
	 * semicolon separated list of ip addresses that can request he white-listed
	 * resources.
	 */
	"sso-whitelisted-ips",
	/**
	 * complete url including authentication. We will append callback url and
		 * redirect for login. Like
		 *
	 * HTTPS://com.myComany/sso/login.htm
	 */
	"sso-login-url",

		/**
	 * qry field name with which callback page is indicated while invoking
	 * sso-login
	 */
	"sso-qry-name-for-callback",

		/**
	 * domain for which sso-token cookie is to be set. e.g. ".example.com"
	 */
	"sso-domain-name",

		/**
	 * life in number of seconds after which sso-cookie will have to expire
	 */
	"sso-cookie-life",
	/**
	 * comma separated field names that sso is to respond back with. status and
	 * messages are added in addition to these fields as art of response
	 */
	"sso-field-names",
	/**
	 * all valid users along with field values. be careful with separators.
	 * String is of the form:
	 *
	 * "user1:val11,val12,val13...;user2:val21,val22,val23....."
	 */
	"sso-user-data" };
	private static final String SESSION_FIELD_NAME_FOR_INFO = "_user_info";
	private static final String SESSION_FIELD_NAME_FOR_URL = "_user_requested_url";
	private static final char AND = '&';
	private static final String EQL = "=";
	private static final String SEMI_COLON = ";";
	private static final String COLON = ":";
	private static final String COMMA = ",";
	/**
	 * we send a simple call-back page to sso, rather than the actual URL. This
	 * is to protect the privacy. We keep the actual requested url in session,
	 * and use that after we get control back into our call-back
	 */
	private static final String CALL_BACK_PAGE_NAME = "/callbackfromsso";
	private static String HTML1 = "<!DOCTYPE html>\n<html xmlns=\"http://www.w3.org/1999/xhtml\">"
			+ "\n<head>\n<title>Dummy SSO Login</title>\n</head>\n<body>"
			+ "\n<h1>Choose User</h1>"
			+ "\n<form method=\"post\">"
			+ "\n<div style=\"width:200px;height:200px;border:solid 1px grey; border-radius:6px; background-color:#444444; text-align:center;\" >"
			+ "\n<div style=\"vertical-align:middle\" >User : <select name=\"user\">\n";
	private static final String HTML2 = "\n</select>\n<input type=\"submit\" value=\"GO\" ></div>\n</div>\n</form>";
	private static final String HTML_END = "\n</body>\n</html>";
	private static final String STATUS_FAILURE = "-1";
	private static final char NEW_LINE = '\n';
	private static String HTML_BEGIN = null;

	private static SsoAuthenticator instance = null;

	/*
	 * context values cached by the instance
	 */
	private final String cookieName;
	private final String validationUrl;
	private final String qryNameForToken;
	private final String qryNameForIp;
	private final String statusFieldName;
	private final String statusSuccess;
	private final String messageFieldName;
	private final String errorPage;
	private final Set<String> whiteListedResources;
	private final Set<String> whiteListedIps;
	private final String loginUrl;
	private final String qryNameForCallback;
	private final String domainName;
	private final int cookieLife;
	private final String[] fieldNames;
	private final Map<String, String[]> userData;

	private String[] validUsers;
	private Map<String, String> authenticatedUsers = new HashMap<String, String>();

	/**
	 * url to be sent to SSO as call-back. Formed first time and then cached.
	 */
	private String cachedLoginUrl = null;

	/**
	 * set up based on parameters in web.xm
	 *
	 * @param ctx
	 */
	public static void setupSso(ServletContext ctx) {

		SsoAuthenticator.instance = new SsoAuthenticator(ctx);
	}

	/**
	 * authenticate the request and return userInfo.
	 *
	 * @param req
	 *            http request object
	 * @return user info if the request is authenticated, null otherwise
	 */
	public static Map<String, String> getUserInfo(HttpServletRequest req) {

		if (SsoAuthenticator.instance == null) {
			SsoAuthenticator.reportSetupError(req);
			return null;
		}
		return SsoAuthenticator.instance.retrieveUserInfo(req);
	}

	/**
	 * has the user authenticated using sso for this request? used by filter
	 *
	 * @param req
	 *            http request object
	 * @param resp
	 * @return true if the request has a valid sso token, false otherwise
	 */
	public static boolean doFilter(HttpServletRequest req,
			HttpServletResponse resp) {

		if (SsoAuthenticator.instance == null) {
			SsoAuthenticator.reportSetupError(req);
			return false;
		}
		try {
			return SsoAuthenticator.instance.okToContinue(req, resp);
		} catch (IOException e) {
			System.err.println("SSO authenticator generated exception for "
					+ req.getRequestURL() + "\n error : " + e.getMessage());
			return false;
		}

	}

	/**
	 * to be called from doGet() of dummyLogin servlet of sso server
	 *
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	public static void doLoginGet(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		if (SsoAuthenticator.instance == null) {
			SsoAuthenticator.reportSetupError(req);
		} else {
			SsoAuthenticator.instance.loginGet(req, resp);
		}
	}

	/**
	 * to be called from doPost() of dummyLogin servlet of sso server
	 *
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	public static void doLoginPost(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		if (SsoAuthenticator.instance == null) {
			SsoAuthenticator.reportSetupError(req);
		} else {
			SsoAuthenticator.instance.loginPost(req, resp);
		}
	}

	/**
	 * to be called from doGet() of dummySsoAuthentication servlet
	 *
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	public static void doAuthenticateGet(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		if (SsoAuthenticator.instance == null) {
			SsoAuthenticator.reportSetupError(req);
		} else {
			SsoAuthenticator.instance.authGet(req, resp);
		}
	}

	private static void reportSetupError(HttpServletRequest req) {
		System.err
		.println("SSO authenticator not set up properly. Filter request turned down for "
				+ req.getRequestURL());

	}

	/**
	 * we have defined all state attributes as final, better set them in the
	 * constructor
	 *
	 * @param ctx
	 */
	private SsoAuthenticator(ServletContext ctx) {
		/*
		 * fragile design. array elements must match the order in which we look
		 * for values
		 */
		int nbr = SsoAuthenticator.ALL_CONTEXT_FIELDS.length;
		String[] values = new String[nbr];
		int i = 0;
		for (String param : SsoAuthenticator.ALL_CONTEXT_FIELDS) {
			values[i] = ctx.getInitParameter(param);
			i++;
		}
		this.cookieName = values[0];
		this.validationUrl = values[1];
		this.qryNameForToken = values[2];
		this.qryNameForIp = values[3];
		this.statusFieldName = values[4];
		this.statusSuccess = values[5];
		this.messageFieldName = values[6];
		this.errorPage = values[7];

		if (values[8] != null) {
			this.whiteListedResources = new HashSet<String>();
			for (String value : values[8].split(SsoAuthenticator.SEMI_COLON)) {
				this.whiteListedResources.add(value);
			}
		} else {
			this.whiteListedResources = null;
		}
		if (values[9] != null) {
			this.whiteListedIps = new HashSet<String>();
			for (String value : values[9].split(SsoAuthenticator.SEMI_COLON)) {
				this.whiteListedIps.add(value);
			}
		} else {
			this.whiteListedIps = null;
		}
		this.loginUrl = values[10];
		this.qryNameForCallback = values[11];
		this.domainName = values[12];
		int life = 10 * 60 * 60;
		try {
			life = Integer.parseInt(values[13]);
		} catch (Exception e) {
			//
		}
		this.cookieLife = life;
		this.fieldNames = values[14] == null ? null : values[14]
				.split(SsoAuthenticator.COMMA);

		if (values[15] != null) {
			this.userData = new HashMap<String, String[]>();
			for (String aUserData : values[15]
					.split(SsoAuthenticator.SEMI_COLON)) {
				String[] parts = aUserData.split(SsoAuthenticator.COLON);
				this.userData.put(parts[0].trim(),
						parts[1].split(SsoAuthenticator.COMMA));
			}

			String[] users = this.userData.keySet().toArray(new String[0]);
			Arrays.sort(users);
			this.validUsers = users;
		} else {
			this.userData = null;
		}

		/*
		 * prepare HTMl based on users
		 */
		StringBuilder sbf = new StringBuilder(SsoAuthenticator.HTML1);
		boolean firstOne = true;
		for (String user : this.validUsers) {
			sbf.append("\n<option value=\"").append(user).append("\"");
			if (firstOne) {
				firstOne = false;
				sbf.append("selected=\"selected\"");
			}
			sbf.append(">").append(user).append("</option>");
		}
		sbf.append(SsoAuthenticator.HTML2);
		SsoAuthenticator.HTML_BEGIN = sbf.toString();
		sbf.setLength(0);
		sbf.append("Initial Values are \n");
		for (String val : values) {
			sbf.append(val).append('\n');
		}
		System.out.println(sbf.toString());

	}

	/**
	 * an app has redirected to login page for sso login.
	 *
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	private void loginGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		/*
		 * keep the callback url in session for later use
		 */
		String callback = req.getParameter(this.qryNameForCallback);
		HttpSession session = req.getSession(true);
		session.setAttribute(SsoAuthenticator.SESSION_FIELD_NAME_FOR_URL,
				callback);

		System.out.println("Received a req with qry " + req.getQueryString()
				+ " and we detected call back as \n" + callback);
		/*
		 * render page for user to choose user
		 */
		Writer writer = resp.getWriter();
		writer.write(SsoAuthenticator.HTML_BEGIN);
		writer.write(SsoAuthenticator.HTML_END);
		writer.close();
	}

	/**
	 * user has submitted login page after choosing user
	 *
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	private void loginPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String user = req.getParameter("user");
		String token = this.issueToken(user, req.getRemoteAddr());
		if (token == null) {
			Writer writer = resp.getWriter();
			writer.write(SsoAuthenticator.HTML_BEGIN);
			writer.write("<div>INTERNAL ERROR : unable to authenticate you. Sorry. Try again :-( ");
			writer.write(SsoAuthenticator.HTML_END);
			writer.close();
			return;
		}
		Cookie cookie = new Cookie(this.cookieName, token);
		/*
		 * we are to set this for domain
		 */
		cookie.setDomain(this.domainName);
		cookie.setPath("/");
		cookie.setMaxAge(this.cookieLife);
		resp.addCookie(cookie);
		String callback = req.getSession(true)
				.getAttribute(SsoAuthenticator.SESSION_FIELD_NAME_FOR_URL)
				.toString();

		resp.sendRedirect(callback);
	}

	/**
	 * user has submitted login page after choosing user
	 *
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	private void authGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		System.out.println("Request : " + req.getRequestURL() + '?'
				+ req.getQueryString() + "  being authenticated ");
		String msg = null;
		String token = req.getParameter(this.qryNameForToken);
		String ip = req.getParameter(this.qryNameForIp);
		String user = null;
		if (token == null || token.length() == 0 || ip == null
				|| ip.length() == 0) {
			msg = this.qryNameForToken + " and " + this.qryNameForIp
					+ " are required for authentication ";
		} else {
			user = this.authenticatedUsers.get(token + '_' + ip);
			if (user == null) {
				msg = "Invalid credentials.";
			} else if (this.userData.containsKey(user) == false) {
				msg = "Internal error. Valid token, but user data is missing.";
				System.err.println(msg);
			}
		}
		Writer writer = resp.getWriter();
		if (msg != null) {
			this.writeAField(writer, this.statusFieldName,
					SsoAuthenticator.STATUS_FAILURE);
			this.writeAField(writer, this.messageFieldName, msg);
		} else {
			this.writeAField(writer, this.statusFieldName, this.statusSuccess);
			int i = 0;
			String[] data = this.userData.get(user);
			for (String fieldName : this.fieldNames) {
				this.writeAField(writer, fieldName, data[i]);
				i++;
			}
		}
		writer.close();
	}

	/**
	 * has the user authenticated using sso for this request? used by filter
	 *
	 * @param req
	 *            http request object
	 * @param resp
	 * @return true if the request has a valid sso token, false otherwise
	 * @throws IOException
	 */
	private boolean okToContinue(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {

		/*
		 * we have to skip contextPath as well as first '/' to get resource name
		 */
		int nbr = req.getContextPath().length();
		String requestedResource = req.getRequestURI().substring(nbr);
		System.out.println("Client-side SSO security is filtering for "
				+ requestedResource);
		/*
		 * is this a white-listed VIP request?
		 */
		if (this.whiteListedResources != null) {
			System.out.println("going to check " + requestedResource
					+ " against " + this.whiteListedResources.size()
					+ " whitelisted resources");
			if (this.whiteListedResources.contains(requestedResource)) {
				if (this.whiteListedIps == null
						|| this.whiteListedIps.contains(req.getRemoteAddr())) {
					System.out
					.println(requestedResource
							+ " cleared without authentication because it is white-listed");
					return true;
				}
				System.out.println("White-listed resource " + requestedResource
						+ " came from a non-white-listed ip "
						+ req.getRemoteAddr());
			}
		}
		if (this.retrieveUserInfo(req) != null) {
			/*
			 * valid token. is this the call-back from sso?
			 */
			if (requestedResource.equals(SsoAuthenticator.CALL_BACK_PAGE_NAME) == false) {
				System.out.println(requestedResource + " cleared security.");
				return true;
			}

			System.out.println(requestedResource
					+ " detected as call-back from sso.");
			HttpSession session = req.getSession(false);
			if (session == null) {
				System.out
				.println("DESIGN PROBLEM: our session in which we stored call back url is LOST!!!!");
				resp.sendRedirect(this.errorPage);
				return false;
			}

			Object originalUrl = session
					.getAttribute(SsoAuthenticator.SESSION_FIELD_NAME_FOR_URL);
			if (originalUrl != null) {
				System.out.println(" call-back redirected to " + originalUrl);
				session.removeAttribute(SsoAuthenticator.SESSION_FIELD_NAME_FOR_URL);
				resp.sendRedirect(originalUrl.toString());
				return false;
			}
			System.err
			.println("Unable to get original url from session after a call-back from sso");
			resp.sendRedirect(this.errorPage);
			return false;
		}
		/*
		 * user doesn't have the right credentials redirect to sso-login page.
		 * But what if this is not a get?
		 */
		if (req.getMethod().equals("GET") == false) {
			System.out
			.println("Received a "
					+ req.getMethod()
					+ " method that we do not expect without authentication. Responding with an error page");
			resp.sendRedirect(this.errorPage);
			return false;
		}
		HttpSession session = req.getSession(true);
		String cachedUrl = req.getRequestURL().toString();
		String qry = req.getQueryString();
		if (qry != null && qry.length() > 0) {
			cachedUrl = cachedUrl + '?' + qry;
		}
		session.setAttribute(SsoAuthenticator.SESSION_FIELD_NAME_FOR_URL,
				cachedUrl);
		if (this.cachedLoginUrl == null) {
			this.setCallbackUrl(req);
		}
		resp.sendRedirect(this.cachedLoginUrl);
		System.out.println(requestedResource + " is cached as " + cachedUrl
				+ " and is redirected to " + this.cachedLoginUrl);
		return false;
	}

	/**
	 * return user-data either from session (cached) or from sso server. Return
	 * null if client is not ss-authenticated
	 *
	 * @param req
	 * @return user data if authenticated, null otherwise
	 */
	private Map<String, String> retrieveUserInfo(HttpServletRequest req) {
		/*
		 * do we have the cookie?
		 */
		String cookieValue = null;
		Cookie[] cookies = req.getCookies();
		if (cookies == null) {
			return null;
		}

		for (Cookie cookie : req.getCookies()) {
			if (cookie.getName().equals(this.cookieName)) {
				cookieValue = cookie.getValue();
				break;
			}
		}

		if (cookieValue == null) {
			return null;
		}

		/*
		 * have we already validated this cookie?
		 */

		String ip = req.getRemoteAddr();
		HttpSession session = req.getSession(true);
		Object att = session
				.getAttribute(SsoAuthenticator.SESSION_FIELD_NAME_FOR_INFO);
		if (att != null) {
			System.out
			.println("We detected that this request is aready authentcated.");
			if (att instanceof Map == false) {
				System.err
						.println("Internal error during sso validation. Session object is expected to be a Map but it turned out to be "
								+ att.getClass().getName());
				return null;
			}
			@SuppressWarnings("unchecked")
			Map<String, String> fields = (Map<String, String>) att;
			String token = fields.get('_' + this.cookieName);
			if (token == null) {
				System.err
						.println("Internal error during sso validation. Field "
								+ this.cookieName
								+ " not found in session objct");
				return null;
			}
			if (token.equals(cookieValue)) {
				System.out
				.println("This is arequest that has a valid sso token. All OK. ");
				/*
				 * great. valid token.
				 */
				return fields;
			}
			/*
			 * we found the session object, but it was for a different token.
			 * Obsolete or manipulation;
			 */
			System.out.println("SSO Cookie " + cookieValue
					+ " does not match with last validated sso cookie : "
					+ token + " for ip address. " + ip
					+ "\n Removing User Info");
			session.removeAttribute(SsoAuthenticator.SESSION_FIELD_NAME_FOR_INFO);
		}

		/*
		 * let us check with SSO
		 */
		Map<String, String> fields = new HashMap<String, String>();
		String urlToUse = null;
		try {
			if (this.cachedLoginUrl == null) {
				this.setCallbackUrl(req);
			}
			urlToUse = this.validationUrl + SsoAuthenticator.AND
					+ this.qryNameForToken + SsoAuthenticator.EQL + cookieValue
					+ SsoAuthenticator.AND + this.qryNameForIp
					+ SsoAuthenticator.EQL + ip;

			URL url = new URL(urlToUse);
			System.out.println("Going to make a call to " + urlToUse);
			URLConnection con = url.openConnection();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(SsoAuthenticator.EQL);
				if (parts.length == 2) {
					fields.put(parts[0], parts[1]);
				} else {
					System.err
							.println("Invalid line received from sso. Line ignored \n"
									+ line);
				}
			}
		} catch (Exception e) {
			System.err
					.println("Connection to sso generated an exception. url = "
							+ urlToUse + "\n Error : " + e.getMessage());
			return null;
		}

		String status = fields.get(this.statusFieldName);
		System.out.println(" we heard back from sso server with status = "
				+ status);
		if (status == null) {
			System.err
			.println("SSO validator did not return any value for status field "
					+ this.statusFieldName);
			return null;
		}
		if (status.equals(this.statusSuccess) == false) {
			System.out.println("SSO failed for token : " + cookieValue
					+ " and ip : " + ip + ". Status : " + status);
			return null;
		}
		/*
		 * save the account info in session, but after putting the ssoToken in
		 * it
		 */
		fields.put('_' + this.cookieName, cookieValue);
		session.setAttribute(SsoAuthenticator.SESSION_FIELD_NAME_FOR_INFO,
				fields);
		System.out.println("Successfully cached token " + cookieValue);
		return fields;
	}

	/**
	 * one-time activity to fully qualify call-back url based on the site where
	 * this is hosted.
	 *
	 * @param req
	 * @throws MalformedURLException
	 */
	private synchronized void setCallbackUrl(HttpServletRequest req) {
		if (this.cachedLoginUrl != null) {
			// concurrent wait..
			return;
		}
		/*
		 * login url is like
		 * https://example.com/sso/login.do?appid=aaa?allbackurl= we have to
		 * append our url at the end.
		 */
		try {
			URL url = new URL(req.getRequestURL().toString());
			this.cachedLoginUrl = this.loginUrl;
			char appendingChar = this.loginUrl.indexOf('?') == -1 ? '?' : '&';
			this.cachedLoginUrl = this.loginUrl
					+ appendingChar
					+ this.qryNameForCallback
					+ SsoAuthenticator.EQL
					+ URLEncoder.encode(
							url.getProtocol() + "://" + url.getAuthority()
									+ req.getContextPath()
									+ SsoAuthenticator.CALL_BACK_PAGE_NAME,
							"UTF-8");
			System.out.println("login url set to " + this.cachedLoginUrl);
		} catch (Exception e) {
			System.err.println("Unable to set login url. URL from request \n"
					+ req.getRequestURL() + "\n resulted in an error \n"
					+ e.getMessage());
		}
	}

	/**
	 * write a field-value pair to the response stream
	 *
	 * @param writer
	 * @param fieldName
	 * @param fieldVaue
	 * @throws IOException
	 */
	private void writeAField(Writer writer, String fieldName, String fieldVaue)
			throws IOException {
		writer.write(fieldName);
		writer.write(SsoAuthenticator.EQL);
		writer.write(fieldVaue);
		writer.write(SsoAuthenticator.NEW_LINE);
	}

	/**
	 * keep token-ip of an authenticated user, and return the token associated
	 * with that
	 *
	 * @param user
	 * @param ip
	 * @return token to be used by the client. null if user is not valid
	 */
	private String issueToken(String user, String ip) {
		if (this.userData.containsKey(user) == false || ip == null) {
			return null;
		}
		String token = UUID.randomUUID().toString();
		System.out.println(" We issued a token = " + token + " for ip=" + ip);
		this.authenticatedUsers.put(token + '_' + ip, user);
		return token;
	}

}
