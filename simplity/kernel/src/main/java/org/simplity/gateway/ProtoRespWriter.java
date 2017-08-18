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
import java.util.Collection;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.value.Value;
import org.simplity.proto.ProtoUtil;
import org.simplity.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message.Builder;

/**
 * protobuf based writer. We have implemented specBasedWrite only. This writer
 * can not be sued by service to push data. It is used to pull data from service
 * context
 *
 * @author simplity.org
 *
 */
public class ProtoRespWriter implements RespWriter {
	private static final Logger logger = LoggerFactory.getLogger(ProtoRespWriter.class);
	/**
	 * initialized on instantiation. set to null once writer is closed.
	 */
	private Builder messageBuilder;
	/**
	 * in case the service has returned with errors, we hav e the text here, and
	 * the builder should not be used.
	 */
	private String errorText;
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
	 *
	 * @param builder
	 *            that will receive the output
	 */
	public ProtoRespWriter(Builder builder) {
		this.messageBuilder = builder;
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
		if (this.errorText == null) {
			return this.messageBuilder.build().toString();
		}
		return this.errorText;
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
		if (this.errorText == null) {
			return this.messageBuilder.build();
		}
		return null;
	}

	/**
	 * every call to write requires us to check if teh writer is still open
	 */
	private void throwException() {
		throw new ApplicationError(
				"ProtoResponseWriter can only pull data from service context. It can not be used to push data.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#writeResponse(java.lang.String)
	 */
	@Override
	public ProtoRespWriter writeCompleteResponse(String response) {
		this.throwException();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#field(java.lang.String,
	 * org.simplity.kernel.value.Value)
	 */
	@Override
	public ProtoRespWriter field(String fieldName, Object value) {
		this.throwException();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#field(java.lang.Object)
	 */
	@Override
	public RespWriter field(Object value) {
		this.throwException();
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#object(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public ProtoRespWriter object(String fieldName, Object value) {
		this.throwException();
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
		this.throwException();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#array(java.lang.String,
	 * org.simplity.kernel.data.DataSheet)
	 */
	@Override
	public ProtoRespWriter array(String arrayName, DataSheet sheet) {
		this.throwException();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginObject(java.lang.String)
	 */
	@Override
	public ProtoRespWriter beginObject(String objectName) {
		this.throwException();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginObject(java.lang.String)
	 */
	@Override
	public ProtoRespWriter beginObject() {
		this.throwException();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#endObject()
	 */
	@Override
	public ProtoRespWriter endObject() {
		this.throwException();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginArray(java.lang.String)
	 */
	@Override
	public ProtoRespWriter beginArray(String arrayName) {
		this.throwException();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginArray()
	 */
	@Override
	public RespWriter beginArray() {
		this.throwException();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#endArray()
	 */
	@Override
	public ProtoRespWriter endArray() {
		this.throwException();
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
		this.messageBuilder.build().writeTo(stream);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.RespWriter#writeAsPerSpec(org.simplity.service.
	 * ServiceContext)
	 */
	@Override
	public void pullDataFromContext(ServiceContext ctx) {
		/*
		 * are there errors?
		 */
		if (ctx.isInError()) {
			this.errorText = FormattedMessage.toString(ctx.getMessages());
			return;
		}

		/*
		 * build the root object. its primitive fields are available as fields,
		 * and embedded message, messageArray and primitiveArrays are available
		 * as data sheets. If an embedded message, or embedded message array in
		 * turn contains message/array, then the are available as child sheets
		 * in context
		 */
		Collection<FieldDescriptor> fields = this.messageBuilder.getDescriptorForType().getFields();
		for (FieldDescriptor fd : fields) {
			String fieldName = fd.getName();
			/*
			 * array
			 */
			if (fd.isRepeated()) {
				DataSheet sheet = ctx.getDataSheet(fieldName);
				logger.info("Data sheet {} is to be extracted.", fieldName);
				if (sheet == null || sheet.length() == 0) {
					logger.info("No data for array field {}", fieldName);
				} else if (fd.getType() == Type.MESSAGE) {
					ProtoUtil.setMessagesArray(this.messageBuilder, fd, sheet, ctx);
				} else {
					ProtoUtil.setPrimitiveArray(this.messageBuilder, fd, sheet);
				}
				continue;
			}
			/*
			 * embedded object
			 */
			if (fd.getType() == Type.MESSAGE) {
				DataSheet sheet = ctx.getDataSheet(fieldName);
				if (sheet == null || sheet.length() == 0) {
					logger.info("No data found for embedded maessage {}", fieldName);
				} else {
					ProtoUtil.setMessage(this.messageBuilder, fd, sheet, ctx);
				}
				continue;
			}
			/*
			 * primitive value
			 */
			Value value = ctx.getValue(fieldName);
			if (value != null) {
				Object objValue = ProtoUtil.convertFieldValue(fd, value);
				this.messageBuilder.setField(fd, objValue);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.RespWriter#hasOutputSpec()
	 */
	@Override
	public boolean hasOutputSpec() {
		return true;
	}
}
