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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceProtocol;

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
		data.setPayLoad(payload.toString());
	}

	/**
	 * writes response based on service output and service spec
	 * @param resp
	 * @param data
	 * @throws IOException
	 */
	public void writeResponse(HttpServletResponse resp,  ServiceData data) throws IOException {
		/*
		 * TODO: implement spec based extraction from out data. We are now assuming payload is all ready
		 */
		String text;
		if(data.hasErrors()){
			text = this.getResponseForError(data.getMessages());
		}else{
			 Object obj = data.getPayLoad();
			 if(obj == null || obj instanceof JSONObject == false){
				 text = "{}";
			 }else{
				 text = ((JSONObject)obj).toString();
			 }
		}
		resp.getWriter().write(text);
	}

	/**
	 * get the JSON to be sent back to client in case of errors
	 *
	 * @param messages Messages
	 * @return JSON string for the supplied errors
	 */
	private String getResponseForError(FormattedMessage[] messages) {
		JSONWriter writer = new JSONWriter();
		writer.object();
		writer.key(ServiceProtocol.REQUEST_STATUS);
		writer.value(ServiceProtocol.STATUS_ERROR);
		writer.key(ServiceProtocol.MESSAGES);
		JsonUtil.addObject(writer, messages);
		writer.endObject();
		return writer.toString();
	}
	/**
	 *
	 * @return schema of response for success
	 */
	public JSONObject getResponseSchema() {
		return (JSONObject) JsonUtil.getValue("responses.200.schema", this.operationSpec);
	}

	/**
	 * @param req
	 * @param params
	 * @return in data into which data from client is extracted as per service spec
	 */
	public ServiceData getInData(HttpServletRequest req, JSONObject params) {
		// TODO really parse data based on spec
		ServiceData data = new ServiceData(Value.newTextValue("100"), this.getServiceName(params) );
		data.setPayLoad(params.toString());
		return data;
	}

}
