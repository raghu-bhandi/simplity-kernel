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

package org.simplity.gateway;

import java.io.IOException;
import java.io.Writer;

import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;

/**
 * prepares a JSON response based on output specifications. Important to note
 * that this writer automatically starts an object so that the caller can start
 * writing right away. It also obviously men that this root object is
 * automatically closed when the response ends
 *
 * @author simplity.org
 *
 */
public class JsonRespWriter implements RespWriter {
	/**
	 * initialized on instantiation. set to null once writer is closed.
	 */
	private JSONWriter writer;
	/**
	 * null till the writer is closed. will be null if the writer was not a
	 * string writer. Keeps the final string, if the writer was not piped to any
	 * other output
	 */
	private String responseText;

	/**
	 * is there an underlying io.writer?
	 */
	private final Writer ioWriter;

	/**
	 * crate a string writer.
	 */
	public JsonRespWriter() {
		this.writer = new JSONWriter();
		this.writer.object();
		this.ioWriter = null;
	}

	/**
	 * create a string writer.
	 *
	 * @param writer
	 *            that will receive the output
	 */
	public JsonRespWriter(Writer writer) {
		this.writer = new JSONWriter(writer);
		this.writer.object();
		this.ioWriter = writer;
	}

	/**
	 * get the response text and close the writer. That is the reason this
	 * method is called getFinalResponse rather than getResponse. Side effect of
	 * closing the writer is hinted with this name
	 *
	 * @return response text.Null if nothing was written so far, or the writer
	 *         was piped to another output mechanism
	 */
	@Override
	public String getFinalResponseText() {
		if (this.writer != null) {
			this.writer.endObject();
			if (this.ioWriter != null) {
				// how do we give final response? let us assume empty.
				this.responseText = "";
			} else {
				this.responseText = this.writer.toString();
			}
			this.writer = null;
		}
		return this.responseText;
	}

	/**
	 * get the response text and close the writer. That is the reason this
	 * method is called getFinalResponse rather than getResponse. Side effect of
	 * closing the writer is hinted with this name
	 *
	 * @return response text.Null if nothing was written so far, or the writer
	 *         was piped to another output mechanism
	 */
	@Override
	public Object getFinalResponseObject() {
		return new JSONObject(this.getFinalResponseText());
	}

	/**
	 * every call to write requires us to check if teh writer is still open
	 */
	private void checkNull() {
		if (this.writer == null) {
			throw new ApplicationError("Response writer is invoked after the writer is closed.");
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#writeResponse(java.lang.String)
	 */
	@Override
	public JsonRespWriter writeCompleteResponse(String response) {
		this.checkNull();
		if (this.ioWriter != null) {
			/*
			 * we have no idea whether something was already written. But that
			 * is caller's problem
			 */
			try {
				this.ioWriter.write(response);
			} catch (IOException e) {
				throw new ApplicationError(e, "error whielwriting a pre-cdetermined text as response");
			}
		} else {
			this.responseText = response;
		}
		this.writer = null;
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#field(java.lang.String,
	 * org.simplity.kernel.value.Value)
	 */
	@Override
	public JsonRespWriter field(String fieldName, Object value) {
		this.checkNull();
		this.writer.key(fieldName).value(value);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#field(java.lang.Object)
	 */
	@Override
	public RespWriter field(Object value) {
		this.checkNull();
		this.writer.value(value);
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#object(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public JsonRespWriter object(String fieldName, Object value) {
		this.checkNull();

		this.writer.key(fieldName);
		JsonUtil.addObject(this.writer, value);
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#array(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public RespWriter array(String arrayName, Object[] arr) {
		this.checkNull();
		this.writer.key(arrayName).array();
		if (arr != null && arr.length != 0) {
			for (Object value : arr) {
				JsonUtil.addObject(this.writer, value);
			}
		}
		this.writer.endArray();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#array(java.lang.String,
	 * org.simplity.kernel.data.DataSheet)
	 */
	@Override
	public JsonRespWriter array(String arrayName, DataSheet sheet) {
		this.checkNull();
		this.writer.array();
		if (sheet != null && sheet.length() > 0 && sheet.width() > 0) {
			for (Value[] row : sheet.getAllRows()) {
				Value value = row[0];
				if (value != null) {
					this.writer.value(value);
				}
			}
		}
		this.writer.endArray();
		return this;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginObject(java.lang.String)
	 */
	@Override
	public JsonRespWriter beginObject(String objectName) {
		this.checkNull();
		this.writer.key(objectName).object();
		return this;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginObject(java.lang.String)
	 */
	@Override
	public JsonRespWriter beginObject() {
		this.checkNull();
		this.writer.object();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#endObject()
	 */
	@Override
	public JsonRespWriter endObject() {
		this.checkNull();
		this.writer.endObject();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginArray(java.lang.String)
	 */
	@Override
	public JsonRespWriter beginArray(String arrayName) {
		this.checkNull();
		this.writer.key(arrayName).array();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginArray()
	 */
	@Override
	public RespWriter beginArray() {
		this.checkNull();
		this.writer.array();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#endArray()
	 */
	@Override
	public JsonRespWriter endArray() {
		this.checkNull();
		this.writer.endArray();
		return this;
	}
}
