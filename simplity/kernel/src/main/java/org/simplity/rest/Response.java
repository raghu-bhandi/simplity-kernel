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

package org.simplity.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.simplity.gateway.ProtoRespWriter;
import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.rest.param.ArrayParameter;
import org.simplity.rest.param.ObjectParameter;
import org.simplity.rest.param.Parameter;
import org.simplity.service.ServiceProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Message.Builder;

/**
 * represents a response from a swagger (open api) document
 *
 * @author simplity.org
 *
 */
public class Response {

	private  static final Logger logger = LoggerFactory.getLogger(Response.class);
	/*
	 * default success and failure responses in case of emergency!!
	 */
	private static final int DEFAULT_ERROR_CODE = 422;
	private static final String EMPTY_RESPONSE = "{}";
	private static Response defaultFailueResponse = createDefFail();
	private static Response defaultSuccessResponse = createDefSuccess();

	private static Response createDefFail() {
		Response resp = new Response();
		resp.sendAllData = true;
		resp.responseCode = RestContext.getContext().getDefaultFailureResponseCode();
		return resp;
	}

	private static Response createDefSuccess() {
		Response resp = new Response();
		resp.responseCode = RestContext.getContext().getDefaultSuccessResponseCode();
		resp.sendAllData = true;
		return resp;
	}

	/**
	 * @return a response object instance that is suitable to respond back when
	 *         there is some error
	 */
	public static Response getDefaultForFailure() {
		return defaultFailueResponse;
	}

	/**
	 * @return a response object instance that is suitable to respond back when
	 *         all-ok
	 */
	public static Response getDefaultForSuccess() {
		return defaultSuccessResponse;
	}

	private boolean sendAllData;
	private int responseCode;
	private Parameter[] headers;
	private Parameter bodyParameter;
	private String bodyFieldName;

	private Response() {
		// for private use only
	}

	/**
	 * construct based on spec json
	 *
	 * @param code
	 * @param responseJson
	 */
	public Response(String code, JSONObject responseJson) {
		try {
			this.responseCode = Integer.parseInt(code);
		} catch (Exception e) {
			throw new ApplicationError("Invalid Http response code " + code);
		}
		JSONObject obj = responseJson.optJSONObject(Tags.HEADERS_ATTR);
		if (obj != null) {
			String[] keys = JSONObject.getNames(obj);
			this.headers = new Parameter[keys.length];
			for (int i = 0; i < keys.length; i++) {
				String key = keys[i];
				this.headers[i] = Parameter.parse(keys[i], null, obj.getJSONObject(key));
			}
		}

		this.bodyFieldName = responseJson.optString(Tags.FIELD_NAME_ATTR, null);
		obj = responseJson.optJSONObject(Tags.SCHEMA_ATTR);
		if (obj != null) {
			this.bodyParameter = Parameter.parse(obj);
		}
	}

	/**
	 * @return http response code to be used for this response
	 */
	public int getCode() {
		return this.responseCode;
	}

	/**
	 * writes response based on service output and service spec
	 *
	 * @param resp
	 * @param data
	 *            name of the field that has the data for body. null if data
	 *            itself has field values for body
	 * @param sendAll
	 *            send everything from input data.DO not go by swagger spec.
	 * @throws IOException
	 */
	public void writeResponse(HttpServletResponse resp, JSONObject data, boolean sendAll)
			throws IOException {
		if (this.responseCode != 0) {
			resp.setStatus(this.responseCode);
		} else {
			resp.setStatus(HttpServletResponse.SC_OK);
		}

		if (this.headers != null) {
			for (Parameter hdr : this.headers) {
				hdr.setHeader(resp, data.opt(hdr.getFieldName()));
			}
		}

		if (sendAll || this.sendAllData) {
			if (data == null) {
				resp.getWriter().write(EMPTY_RESPONSE);
			} else {
				resp.getWriter().write(data.toString());
			}
			return;
		}

		if (this.bodyParameter == null) {
			return;
		}

		if (data == null) {
			logger.info("No response object supplied for response. no body response.");
			return;
		}

		Object bodyData = data;
		if (this.bodyFieldName != null) {
			bodyData = data.opt(this.bodyFieldName);
			if (bodyData == null) {
				logger.info("Response field " + this.bodyFieldName + " has no value. No response sent.");
				return;
			}
		}

		logger.info(
				"Preparing response body based on a parameter type {}", this.bodyParameter.getClass().getSimpleName());
		/*
		 * most of the time, it is an object
		 */
		if (this.bodyParameter instanceof ObjectParameter) {
			if (bodyData instanceof JSONObject) {
				logger.info("Going to write body paremeters from " + bodyData.toString());
				((ObjectParameter) this.bodyParameter).toWriter(new JSONWriter(resp.getWriter()), bodyData, false);
			} else {
				logger.info("We expected a JSON Object as response value with bodyFieldName=" + this.bodyFieldName
						+ " but we got " + bodyData.getClass().getName()
						+ " as value. Null value assumed for response.");
			}
			return;
		}

		/*
		 * if it is primitive, we just write the value
		 */
		if (this.bodyParameter instanceof ArrayParameter == false) {
			resp.getWriter().write(data.toString());
			return;
		}

		ArrayParameter ap = (ArrayParameter) this.bodyParameter;
		JSONArray arrData = null;
		if (bodyData instanceof JSONArray) {
			arrData = (JSONArray) bodyData;
		} else if (bodyData instanceof JSONObject) {
			arrData = pickAnArray((JSONObject) bodyData);
		}

		if (arrData == null) {
			logger.error("We are to send an array as response but we god data as " + bodyData.getClass().getSimpleName()
					+ ". no body response");
			resp.getWriter().write(EMPTY_RESPONSE);
			return;
		}

		if (ap.expectsTextValue()) {
			resp.getWriter().write(ap.serialize((JSONArray) bodyData));
		} else {
			ap.toWriter(new JSONWriter(resp.getWriter()), arrData, false);
		}

	}

	/**
	 * return the first JSONArray attribute of this object. Attribute starting
	 * with '_' are assumed to be reserved, and hence skipped
	 *
	 * @param json
	 * @return array or null if we could not find one
	 */
	private static JSONArray pickAnArray(JSONObject json) {
		for (String name : JSONObject.getNames(json)) {
			if (name.charAt(0) == '_') {
				/*
				 * that is a reserved/special attribute
				 */
				continue;
			}
			JSONArray result = json.optJSONArray(name);
			if (result != null) {
				return result;
			}
		}
		return null;
	}

	/**
	 * write a response when we have only messages, and no data
	 *
	 * @param resp
	 * @param messages
	 * @throws IOException
	 */
	public void writeResponse(HttpServletResponse resp, FormattedMessage[] messages) throws IOException {
		if (this.responseCode == 0) {
			resp.setStatus(this.responseCode);
		} else {
			resp.setStatus(DEFAULT_ERROR_CODE);
		}
		if (messages != null) {
			JSONWriter writer = new JSONWriter(resp.getWriter());
			writer.object();
			writer.key(ServiceProtocol.REQUEST_STATUS);
			writer.value(ServiceProtocol.STATUS_ERROR);
			writer.key(ServiceProtocol.MESSAGES);
			JsonUtil.addObject(writer, messages);
			writer.endObject();
		}
	}

	/*
	 * methods added for proto
	 */

	/**
	 * @param resp
	 * @return response writer for this response
	 */
	public ProtoRespWriter getProtoWriter(HttpServletResponse resp) {
		String schemaName = this.bodyParameter.getName();
		if(schemaName == null){
			throw new ApplicationError("Response has a non-object schema, or the object schema is defined in-line. proto-buf requires the root to be an object, and you should define the schema in definitions for a protobuf class to be generated for that.");
		}

		Builder builder = Operations.getMessageBuilder(schemaName);
		return new ProtoRespWriter(builder);
	}
}
