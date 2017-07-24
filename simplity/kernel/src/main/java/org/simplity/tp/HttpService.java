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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.tp;

import org.simplity.gateway.OutboundAgent;
import org.simplity.http.HttpGateway;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * an external service request using http protocol
 *
 * @author simplity.org
 *
 */
public class HttpService extends ExternalService {

	/**
	 * Complete HTTP URL string starting. Example
	 * http://app.acnecorp.com/a/b/c.html?b=s&j=3 this
	 * string may contain ${variableName} in which case the string is created at
	 * run time after
	 * substituting values for these at run time. If no value is found, we treat
	 * it as empty string
	 * and still go ahead.
	 */
	String urlString;
	/**
	 * GET, POST etc..
	 */
	String httpMethod;

	/**
	 * headers to be set.
	 */
	String[] headerNamesToSend;
	/**
	 * in case the header names are different from the field names in the
	 * service context. Default to headerNames;
	 */
	String[] headerFieldSources;

	/**
	 * header names to be received after the call.
	 */
	String[] headerNamesToReceive;

	/**
	 * in case the received headers to be extracted to service context with
	 * different field names. Defaults to headerNamesToReceive
	 */
	String[] headerFieldDestinations;

	/**
	 * if the http status code returned by the external server needs to be set a
	 * field in service context
	 */
	String setStatusCodeTo;

	/**
	 * in case url has variables in it, cache its parts for efficiency at run
	 * time. into an array which has its odd-index (0-based) has names and other
	 */
	private String[] urlParts;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.ExternalService#preServe(org.simplity.gateway.
	 * OutboundAgent)
	 */
	@Override
	boolean preServe(OutboundAgent agent, ServiceContext ctx) {
		HttpGateway.Agent hagent = (HttpGateway.Agent) agent;
		String path;
		if (this.urlParts == null) {
			path = this.urlString;
		} else {
			path = TextUtil.substituteFields(this.urlParts, ctx);
		}

		String[] values = null;
		if (this.headerFieldSources != null) {
			values = new String[this.headerFieldSources.length];
			for (int i = 0; i < values.length; i++) {
				Value val = ctx.getValue(this.headerFieldSources[i]);
				if (val != null) {
					values[i] = val.toString();
				}
			}
		}
		hagent.setConnectionParams(path, this.httpMethod, this.headerNamesToSend, values);
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.ExternalService#postServe(org.simplity.gateway.
	 * OutboundAgent)
	 */
	@Override
	Value postServe(OutboundAgent agent, ServiceContext ctx) {
		HttpGateway.Agent hagent = (HttpGateway.Agent) agent;
		if (this.setStatusCodeTo != null) {
			ctx.setValue(this.setStatusCodeTo, Value.newIntegerValue(hagent.getStatus()));
		}

		if (this.headerNamesToReceive != null) {
			String[] vals = hagent.getHeaders(this.headerNamesToReceive);
			for (int i = 0; i < vals.length; i++) {
				ctx.setTextValue(this.headerFieldDestinations[i], vals[i]);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#getReady()
	 */
	@Override
	public void getReady(int idx, Service service) {
		super.getReady(idx, service);
		if (this.urlString.indexOf('$') != -1) {
			this.urlParts = TextUtil.parseToParts(this.urlString);
		}

		if (this.headerNamesToSend != null) {
			if (this.headerFieldSources == null) {
				this.headerFieldSources = this.headerNamesToSend;
			}
		}

		if (this.headerNamesToReceive != null) {
			if (this.headerFieldDestinations == null) {
				this.headerFieldDestinations = this.headerNamesToReceive;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#validate(org.simplity.kernel.comp.
	 * ValidationContext, org.simplity.tp.Service)
	 */
	@Override
	public int validate(ValidationContext vtx, Service service) {
		int count = super.validate(vtx, service);
		/*
		 * mandatory fields
		 */
		count += vtx.checkMandatoryField("urlString", this.urlString);
		count += vtx.checkMandatoryField("restMethod", this.httpMethod);

		if (this.headerNamesToSend != null) {
			if (this.headerFieldSources != null && this.headerFieldSources.length != this.headerNamesToSend.length) {
				count++;
				vtx.addError(
						"headerFieldSources defaults to headerNamesToSend, but if you specify it, number of field names should correspond to header names ");
			}
		} else if (this.headerFieldSources != null) {
			count++;
			vtx.addError("headerFieldSources is not relevant when headerNamesToSend is not specified");
		}

		if (this.headerNamesToReceive != null) {
			if (this.headerFieldDestinations != null && this.headerFieldDestinations.length != this.headerNamesToReceive.length) {
				count++;
				vtx.addError(
						"headerFieldDestinations defaults to headerNamesToReceive, but if you specify it, number of field names should correspond to header names ");
			}
		} else if (this.headerFieldDestinations != null) {
			count++;
			vtx.addError("headerFieldDestinations is not relevant when headerNamesToReceive is not specified");
		}

		return count;
	}
}
