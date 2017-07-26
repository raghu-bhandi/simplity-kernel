/*
 * Copyright (c) 2017 simplity.org
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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;

import org.simplity.gateway.Gateway;
import org.simplity.gateway.JsonReqReader;
import org.simplity.gateway.JsonRespWriter;
import org.simplity.gateway.OutboundAgent;
import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.service.InputData;
import org.simplity.service.OutputData;
import org.simplity.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is designed more like a simplity component. That is, immutable,
 * thread-safe instances are created based on xml files. A worker instance is
 * (sub-class) is created for each execution
 *
 * @author simplity.org
 *
 */
public class HttpGateway extends Gateway {
	private static final Logger logger = LoggerFactory.getLogger(HttpGateway.class);
	static final String DEFULT_METHOD = "POST";

	/**
	 * base url of the server. for example https://www.simplity.org/thisApp/
	 * value of path received from client requests to be appended to this base
	 * url to make a connection
	 */
	String baseUrl;

	/**
	 * application/json, application/xml, and text/html are the common ones.
	 */
	String contentType;

	/**
	 * Proxy url
	 */
	String proxyHostName;

	/**
	 * proxy port. Required if proxy is specified.
	 */
	int proxyPort;

	/**
	 * Proxy username
	 */
	String proxyUserName;

	/**
	 * Proxy pwd
	 */
	String proxyPassword;

	protected Authenticator authenticator;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.Gateway#getAgentInstance()
	 */
	@Override
	protected OutboundAgent getAgentInstance() {
		return new Agent();
	}

	protected Authenticator getAuth() {
		if (this.proxyHostName == null) {
			return null;
		}
		if (this.authenticator == null) {
			/*
			 * using anonymous class as it is used here and nowhere else
			 */
			this.authenticator = new Authenticator() {
				private PasswordAuthentication auth = new PasswordAuthentication(HttpGateway.this.proxyUserName,
						HttpGateway.this.proxyPassword.toCharArray());

				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return this.auth;
				}

			};
		}
		return this.authenticator;
	}

	/**
	 * a worker inner-class that re-uses set-up time parameters from its parent
	 * instance and manages state across method invocations with its own
	 * attributes
	 *
	 * @author simplity.org
	 *
	 */
	public class Agent implements OutboundAgent {

		/**
		 * path that is to be appended to the base path for making the http
		 * request. This may include query string as well. It does not have
		 * protocol, domain or port.
		 */
		private String path;

		private String method = HttpGateway.DEFULT_METHOD;

		private String[] headerNames;

		private String[] headerValues;

		private HttpURLConnection conn;

		/**
		 * set connection parameter before calling serve method
		 *
		 * @param path
		 *            only the specific path, possibly with query string, but
		 *            with no protocol, domain or port. this is appended with
		 *            the baseURL for the connection
		 * @param httpMethod
		 *            valid http method. null to use the default method.
		 * @param headerNamesToSend
		 *            list of header fields to be set. if non-null, values must
		 *            also be provided in the next parameter.
		 * @param values
		 *            associate array for headerNamesToSend with values for each
		 */
		public void setConnectionParams(String path, String httpMethod, String[] headerNamesToSend, String[] values) {
			this.path = path;
			if (httpMethod != null) {
				this.method = httpMethod;
			}
			if (headerNamesToSend != null) {
				this.headerNames = headerNamesToSend;
				this.headerValues = values;
			}
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.simplity.gateway.OutboundAgent#server(java.lang.String,
		 * org.simplity.service.ServiceContext, org.simplity.service.OutputData,
		 * org.simplity.service.InputData)
		 */
		@Override
		public boolean serve(ServiceContext ctx, OutputData dataTobeSent, InputData dataToBeReceived) {
			if (this.path == null) {
				throw new ApplicationError("HttpGateway.Agent.setCOnnectionParameters() to be invoked before serve() ");
			}

			String payload = null;
			if (dataTobeSent != null) {
				JsonRespWriter writer = new JsonRespWriter();
				dataTobeSent.write(writer, ctx);
				payload = writer.getFinalResponseText();
			}

			/*
			 * avoid the long HttpGateWay.this in each statement to improve
			 * readability
			 */
			HttpGateway gateway = HttpGateway.this;
			String fullPath = gateway.baseUrl + this.path;
			try {

				URL url = new URL(fullPath);

				/*
				 * get connection
				 */
				Authenticator auth = gateway.getAuth();
				if (auth != null) {

					Proxy proxyCon = new Proxy(Proxy.Type.HTTP,
							new InetSocketAddress(gateway.proxyHostName, gateway.proxyPort));
					Authenticator.setDefault(auth);
					this.conn = (HttpURLConnection) url.openConnection(proxyCon);
				} else {
					this.conn = (HttpURLConnection) url.openConnection();
				}

				/*
				 * despatch request
				 */
				this.setHeader();
				this.conn.setDoOutput(true);
				if (payload != null) {
					this.conn.getOutputStream().write(payload.getBytes("UTF-8"));
				}
				/*
				 * receive response
				 */
				int status = this.conn.getResponseCode();
				/*
				 * how do you know this successful? 2xx series is safe
				 */
				if (status < 200 || status > 299) {
					logger.error("Http call failed for gateway " + gateway.getName() + " with url " + fullPath
							+ " with status code " + status);
					return false;
				}
				if(dataToBeReceived  != null){
					String resp = this.readResponse(ctx, dataToBeReceived);
				    JsonReqReader reqReader = new JsonReqReader(new JSONObject(resp));
				    dataToBeReceived.read(reqReader, ctx);
				}
				return true;
			} catch (Exception e) {
				logger.error(" Http call failed for gateway " + gateway.getName() + " with url " + fullPath, e);
				return false;
			}
		}

		/**
		 * @param ctx
		 * @param dataToBeReceived
		 * @throws IOException
		 */
		private String readResponse(ServiceContext ctx, InputData dataToBeReceived) throws IOException {
		    BufferedReader reader = null;
		    StringBuilder sbf = new StringBuilder();
		    try {
		      reader = new BufferedReader(new InputStreamReader((this.conn.getInputStream())));
		      int ch;
		      while ((ch = reader.read()) > -1) {
		        sbf.append((char) ch);
		      }
		      reader.close();
		      return sbf.toString();
		    } finally {
		      if (reader != null) {
		        try {
		          reader.close();
		        } catch (Exception e) {
		          //
		        }
		      }
		    }
		}

		/**
		 * @return http response status code
		 */
		public long getStatus() {
			if (this.conn == null) {
				logger.error("Invalid call to getStatus() before calling serve() method");
				return 0;
			}
			try {
				return this.conn.getResponseCode();
			} catch (IOException e) {
				return 0;
			}
		}

		/**
		 * @param names
		 * @return array of values received for the header field names
		 */
		public String[] getHeaders(String[] names) {
			String[] values = new String[names.length];
			for (int i = 0; i < values.length; i++) {
				values[i] = this.conn.getHeaderField(names[i]);
			}
			return values;
		}

		/**
		 * @param conn
		 * @throws ProtocolException
		 */
		private void setHeader() throws ProtocolException {
			this.conn.setRequestMethod(this.method);
			String ct = HttpGateway.this.contentType;
			this.conn.setRequestProperty("Accept", ct);
			this.conn.setRequestProperty("Content-Type", ct);
			if (this.headerNames != null) {
				for (int i = 0; i < this.headerNames.length; i++) {
					this.conn.setRequestProperty(this.headerNames[i], this.headerValues[i]);
				}
			}
		}

	}

}
