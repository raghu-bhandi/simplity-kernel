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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.tp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;

import org.simplity.json.JSONObject;
import org.simplity.json.XML;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * Open an url connection and make a rest call with the configurations
 *
 * @author infosys.com
 *
 */
public class RestClient extends Action {
	final static String JSONFormat = "application/json";
	final static String XMLFormat = "application/xml";

	/**
	 * The HTTP method call
	 */
	String restMethod;
	/**
	 * The HTTP URL string
	 */
	String urlString;
	String parsedUrlString;
	/**
	 * The HTTP expected content type
	 */
	String contentType;
	/**
	 * name of the output data
	 */
	String outputFieldName;
	/**
	 * value of the output data 
	 */
	String 	outputFieldValue;
	/**
	 * Proxy details
	 */
	String proxy;
	int proxyport;
	/**
	 * Proxy username
	 */
	String proxyUserName;
	/**
	 * Proxy pwd
	 */
	String proxyPassword;

	/**
	 * default
	 */
	public RestClient() {

	}

	@Override
	protected Value doAct(ServiceContext ctx, DbDriver driver) {
		try {
			if(parsedUrlString!=null){
				urlString = ctx.getTextValue(parsedUrlString);
			}
			
			URL url = new URL(urlString);
			HttpURLConnection conn;
			if (proxy != null) {
				Proxy proxycon = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy, proxyport));
				Authenticator authenticator = new Authenticator() {
					public PasswordAuthentication getPasswordAuthentication() {
						return (new PasswordAuthentication(proxyUserName, proxyPassword.toCharArray()));
					}
				};
				Authenticator.setDefault(authenticator);
				conn = (HttpURLConnection) url.openConnection(proxycon);
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}

			conn.setRequestMethod(restMethod);
			conn.setRequestProperty("Accept", contentType);
			if (conn.getResponseCode() != 200) {
				return null;
			}
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			StringBuffer output = new StringBuffer();
			String line;
			while ((line = br.readLine()) != null) {
				output.append(line);
			}
			if (contentType.equals(JSONFormat)) {
				ctx.setValue(outputFieldName,
						Value.newTextValue(new JSONObject(output.toString()).getJSONArray(outputFieldValue).toString()));
				return Value.newBooleanValue(true);
			}
			if (contentType.equals(XMLFormat)) {
				ctx.setValue(outputFieldName, Value
						.newTextValue(XML.toJSONObject(output.toString()).getJSONObject(outputFieldValue).toString()));
				return Value.newBooleanValue(true);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Value.newBooleanValue(false);
	}

	@Override
	public DbAccessType getDataAccessType() {
		return DbAccessType.NONE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.tp.Action#getReady()
	 */
	@Override
	public void getReady(int idx) {
		super.getReady(idx);
		if (this.restMethod == null) {
			throw new ApplicationError("Rest Client action requires rest method");
		}
		if (this.urlString == null) {
			throw new ApplicationError("Rest Client action requires URL string");
		}
		if (this.contentType == null) {
			throw new ApplicationError("Rest Client action requires content-type");
		}
		if (this.outputFieldName == null) {
			throw new ApplicationError("Rest Client action requires output sheet for putting the fetched values");
		}
		if (this.outputFieldValue == null) {
			throw new ApplicationError("Rest Client action requires output sheet for putting the fetched values");
		}		
		this.parsedUrlString = TextUtil.getFieldName(this.urlString);
	}
}
