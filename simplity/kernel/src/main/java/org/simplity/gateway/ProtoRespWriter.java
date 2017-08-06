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
import java.util.List;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;
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
	 * null till the writer is closed. will be null if the writer was not a
	 * string writer. Keeps the final string, if the writer was not piped to any
	 * other output
	 */
	private final String responseText = "";

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
		return this.messageBuilder.build().toString();
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
		return this.messageBuilder.build();
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
		stream.write(this.messageBuilder.build().toByteArray());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.RespWriter#writeAsPerSpec(org.simplity.service.
	 * ServiceContext)
	 */
	@Override
	public void writeAsPerSpec(ServiceContext ctx) {
		Collection<FieldDescriptor>fields = this.messageBuilder.getDescriptorForType().getFields();
		for (FieldDescriptor fd : fields) {
			String fieldName = fd.getName();
			/*
			 * array
			 */
			if (fd.isRepeated()) {
				DataSheet sheet = ctx.getDataSheet(fieldName);
				if (sheet == null || sheet.length() == 0) {
					logger.info("No data for array field " + fieldName);
				} else if (fd.getJavaType() == JavaType.MESSAGE) {
					addMessages(this.messageBuilder, fd, sheet);
				} else {
					this.addArray(this.messageBuilder, fd, sheet);
				}
				continue;
			}
			/*
			 * embedded object
			 */
			if (fd.getJavaType() == JavaType.MESSAGE) {
				DataSheet sheet = ctx.getDataSheet(fieldName);
				if (sheet == null || sheet.length() == 0) {
					logger.info("No data found for embedded maessage " + fieldName);
				} else {
					Message child = createMessage(this.messageBuilder.newBuilderForField(fd), fd.getMessageType().getFields(), sheet);
					if (child != null) {
						this.messageBuilder.setField(fd, child);
					}
				}
				continue;
			}
			/*
			 * primitive value
			 */
			Value value = ctx.getValue(fieldName);
			if (value != null) {
				setFieldValue(this.messageBuilder, fd, value);
			}
		}
	}

	/**
	 * @param builder
	 * @param fd
	 * @param sheet
	 */
	private void addArray(Builder builder, FieldDescriptor fd, DataSheet sheet) {
		int nbr = sheet.length();
		for(int i = 0; i < nbr; i++){
			Value value = sheet.getRow(i)[0];
			if(Value.isNull(value) == false){
				builder.addRepeatedField(fd, value.toObject());
			}
		}
	}

	/**
	 *
	 * @param parentBuilder
	 * @param fd
	 * @param sheet
	 */
	private static void addMessages(Builder parentBuilder, FieldDescriptor fd, DataSheet sheet) {
		Builder builder = parentBuilder.newBuilderForField(fd);
		List<FieldDescriptor> fields = fd.getMessageType().getFields();
		/*
		 * we optimize extraction of data from sheet by taking a row at a time,
		 * and cache the col indexes
		 */
		int[] colIndexes = new int[fields.size()];
		for (int i = 0; i < colIndexes.length; i++) {
			FieldDescriptor field = fields.get(i);
			if (field.isRepeated() || field.getJavaType() == JavaType.MESSAGE) {
				throw new ApplicationError("We are yet to implement arbitrary object structures. Only one level of chil-array/message is handled.");
			}
			colIndexes[i] = sheet.getColIdx(field.getName());
		}
		/*
		 * now create a message for each row, and add it to the field
		 */
		int nbrRows = sheet.length();
		for (int rowIdx = 0; rowIdx < nbrRows; rowIdx++) {
			builder.clear();
			Value[] row = sheet.getRow(rowIdx);
			for (int i = 0; i < colIndexes.length; i++) {
				int idx = colIndexes[i];
				if (idx >= 0) {
					Value value = row[idx];
					if(Value.isNull(value) == false){
						setFieldValue(builder, fields.get(i), value);
					}
				}
			}
			/*
			 * child-message attributes are all set. create and add it.
			 */
			parentBuilder.addRepeatedField(fd, builder.build());
		}
	}

	private static void setFieldValue(Builder builder, FieldDescriptor fd, Value value){
		Type type = fd.getType();
		Object fieldValue = null;
		if(type == Type.ENUM){
			fieldValue = fd.getEnumType().findValueByName(value.toString());
		}else if(type == Type.STRING){
			fieldValue = value.toString();
		}else{
			fieldValue= value.toObject();
		}
		builder.setField(fd, fieldValue);
	}
	/**
	 * @param newBuilderForField
	 * @param fields
	 * @return
	 */
	private static Message createMessage(Builder builder, List<FieldDescriptor> fields, DataSheet sheet) {
		for (FieldDescriptor field : fields) {
			if (field.isRepeated() || field.getJavaType() == JavaType.MESSAGE) {
				throw new ApplicationError("We are yet to implement arbitrary object structures. Only one level of chil-array/message is handled.");
			}
			Value value = sheet.getColumnValue(field.getName(), 0);
			if (Value.isNull(value) == false) {
				builder.setField(field, value.toObject());
			}
		}
		return builder.build();
	}
}
