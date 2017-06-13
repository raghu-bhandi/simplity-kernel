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

package org.simplity.openapi;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.json.JSONObject;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.service.ServiceData;

/**
 * service specification based on Open API for a given operation.
 * This is a dummy wrapper on the JSON object returned by api
 *
 * @author simplity.org
 *
 */
public class ServiceSpec {
	private final JSONObject operationSpec;
	private final String serviceName;

	ServiceSpec(JSONObject operationSpec, String serviceName) {
		this.operationSpec = operationSpec;
		this.serviceName = serviceName;
	}

	/**
	 * @param params
	 *            input received from client. contents may be altered by the
	 *            translator as per service API
	 * @return the serviceName
	 */

	public String getServiceName(JSONObject params) {
		return ServiceMapper.translate(this.serviceName, params);
	}

	public void extractInput(JSONObject payload, ServiceData data) {
		//
	}

	public void writeResponse(HttpServletResponse resp,  ServiceData data) {

	}

	public JSONObject getResponseSchema() {
		return (JSONObject) JsonUtil.getValue("responses.200.schema", this.operationSpec);
	}

	/**
	 * @param req
	 * @param params
	 * @return
	 */
	public ServiceData getInData(HttpServletRequest req, JSONObject params) {
		// TODO Auto-generated method stub
		return null;
	}

}
