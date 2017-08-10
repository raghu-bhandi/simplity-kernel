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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

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
	 * key by which this response can be cached. It is serviceName, possibly
	 * appended with values of some key-fields. null means can not be cached
	 */
	private String cachingKey;

	/**
	 * valid if cachingKey is non-null. number of minutes after which this cache
	 * is to be invalidated. 0 means no such
	 */
	private int cacheValidityMinutes;

	/**
	 * service caches to be invalidated after execution this service. Null means
	 * nothing is to be invalidated
	 */
	private String[] invalidations;

	/**
	 * crate a string writer.
	 */
	public JsonRespWriter() {
		this.writer = new JSONWriter();
		this.writer.object();
		this.ioWriter = null;
	}

	/**
	 * crate a string writer.
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
		if (this.writer == null) {
			/*
			 * it was already closed..
			 */
			return this.responseText;
		}
		/*
		 * close writer
		 */
		this.writer.endObject();

		/*
		 * get final text into responseText
		 */
		if (this.ioWriter == null) {
			this.responseText = this.writer.toString();
		} else {
			// we were just a pipe, we do not have the accumulated string. That
			// is by design, and hence caller should be aware. Prefer empty
			// string to null
			this.responseText = "";
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

	/**
	 * @return
	 * 		get the key based on which the response can be cached.
	 *         emptyString means no key is used for caching. null means it can
	 *         not be cached.
	 */
	@Override
	public String getCachingKey() {
		return this.cachingKey;
	}

	/**
	 * @return
	 * 		number of minutes the cache is valid for. 0 means it has no
	 *         expiry. This method is relevant only if getCachingKey returns
	 *         non-null (indication that the service can be cached)
	 */
	@Override
	public int getCacheValidity() {
		return this.cacheValidityMinutes;
	}

	/**
	 * @param key
	 *            key based on which the response can be cached.
	 *            emptyString means no key is used for caching. null means it
	 *            can not be cached
	 * @param minutes
	 *            if non-null cache is to be invalidated after these many
	 *            minutes
	 */
	@Override
	public void setCaching(String key, int minutes) {
		this.cachingKey = key;
		this.cacheValidityMinutes = minutes;
	}

	/**
	 * @return
	 * 		cached keys that need to be invalidated
	 */
	@Override
	public String[] getInvalidations() {
		return this.invalidations;
	}

	/**
	 * @param invalidations
	 *            cached keys that need to be invalidated
	 */
	@Override
	public void setInvalidations(String[] invalidations) {
		this.invalidations = invalidations;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.RespWriter#writeout(java.io.Writer)
	 */
	@Override
	public void writeout(OutputStream stream) throws IOException {
		if (this.writer == null) {
			/*
			 * stream was already closed
			 */
			return;
		}
		if (this.ioWriter == null) {
			Writer riter = new OutputStreamWriter(stream);
			riter.write(this.writer.toString());
		} else {
			// it is already flushed
		}
		this.writer = null;
		this.responseText = "";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.RespWriter#writeAsPerSpec(org.simplity.service.
	 * ServiceContext)
	 */
	@Override
	public void pullDataFromContext(ServiceContext ctx) {
		this.checkNull();
		/*
		 * we are to write based on our spec
		 */
		for (Map.Entry<String, Value> entry : ctx.getAllFields()) {
			this.field(entry.getKey(), entry.getValue());
		}

		for (Map.Entry<String, DataSheet> entry : ctx.getAllSheets()) {
			DataSheet sheet = entry.getValue();
			if (sheet == null || sheet.length() == 0) {
				continue;
			}

			this.beginArray(entry.getKey());

			int nbrRows = sheet.length();
			String[] names = sheet.getColumnNames();
			for (int i = 0; i < nbrRows; i++) {
				Value[] row = sheet.getRow(i);
				this.beginObject();
				for (int j = 0; j < names.length; j++) {
					this.field(names[j], row[i]);
				}
				this.endObject();
			}
			this.endArray();
		}
	}

	/* (non-Javadoc)
	 * @see org.simplity.gateway.RespWriter#hasOutputSpec()
	 */
	@Override
	public boolean hasOutputSpec() {
		return true;
	}
}
