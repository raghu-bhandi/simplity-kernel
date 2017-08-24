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
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

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
	 * in case a ready message is designated for response instead of a builder.
	 * Also, this attribute has the actual message that is sent to the client
	 * after sending
	 */
	private Message rootMessage;

	/**
	 * we keep objects that are being built into stack, in case of recursive
	 * calls
	 */
	private Stack<Target> stack = new Stack<Target>();

	/**
	 * current object that is receiving data. null if an array is receiving
	 * data.
	 */
	private Target currentTarget = new Target(this.messageBuilder, null);

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
	public Object getFinalResponseObject() {
		if (this.errorText != null) {
			return null;
		}
		if (this.rootMessage == null) {
			this.rootMessage = this.messageBuilder.build();
		}
		return this.rootMessage;
	}

	private void checkNullObject() {
		if (this.currentTarget.arrayField != null) {
			throw new ApplicationError("A field is being set when an array is open. Error in th call sequence.");
		}
	}

	private void checkNullArray() {
		if (this.currentTarget.arrayField == null) {
			throw new ApplicationError(
					"An array element is being set when an object is open (but not an array). Error in th call sequence.");
		}
	}

	private FieldDescriptor getField(String fieldName) {
		if (this.currentTarget.arrayField != null) {
			throw new ApplicationError("A field is being set when an array is open. Error in th call sequence.");
		}
		FieldDescriptor field = this.currentTarget.fields.get(fieldName);
		if (field != null) {
			return field;
		}
		logger.error("No field named {}. Value not set", fieldName);
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#field(java.lang.String,
	 * org.simplity.kernel.value.Value)
	 */
	@Override
	public void setField(String fieldName, Object value) {
		FieldDescriptor field = this.getField(fieldName);
		if (field != null) {
			this.currentTarget.builder.setField(field, value);
		}
	}

	@Override
	public void setField(String fieldName, Value value) {
		FieldDescriptor field = this.getField(fieldName);
		if (field != null) {
			Object obj = ProtoUtil.convertFieldValue(field, value);
			this.currentTarget.builder.setField(field, obj);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#field(java.lang.Object)
	 */
	@Override
	public void addToArray(Object value) {
		this.checkNullArray();
		Object obj = value;
		if (value instanceof Value) {
			obj = ProtoUtil.convertFieldValue(this.currentTarget.arrayField, (Value) value);
		}
		this.currentTarget.builder.addRepeatedField(this.currentTarget.arrayField, obj);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#object(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void setObject(String fieldName, Object value) {
		FieldDescriptor field = this.getField(fieldName);
		if (field == null) {
			return;
		}
		if (value instanceof Message == false) {
			throw new ApplicationError(
					"Only a Message instance can be set as a child-Object. An instance of " + value.getClass().getName()
							+ " is received. If this is a primitive value, setField() is to be used");
		}
		this.currentTarget.builder.setField(field, value);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#array(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void setArray(String fieldName, Object[] arr) {
		FieldDescriptor field = this.getField(fieldName);
		if (field == null) {
			return;
		}
		if (field.isRepeated() == false) {
			throw new ApplicationError(fieldName + " is not an array, but an array value is supplied");
		}
		for (Object obj : arr) {
			if (obj instanceof Value) {
				obj = ProtoUtil.convertFieldValue(field, (Value) obj);
			}
			this.currentTarget.builder.addRepeatedField(field, obj);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#array(java.lang.String,
	 * org.simplity.kernel.data.DataSheet)
	 */
	@Override
	public void setArray(String fieldName, DataSheet sheet) {
		throw new ApplicationError("This writer can not add a data sheet as an array");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginObject(java.lang.String)
	 */
	@Override
	public Object beginObject(String fieldName) {
		FieldDescriptor field = this.getField(fieldName);
		if (field == null) {
			return null;
		}
		Builder builder = this.currentTarget.builder.getFieldBuilder(field);
		Target target = new Target(builder, null);
		this.push();
		this.currentTarget = target;
		return builder;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginObject(java.lang.String)
	 */
	@Override
	public Object beginObjectAsArrayElement() {
		this.checkNullArray();
		int nbr = this.currentTarget.builder.getRepeatedFieldCount(this.currentTarget.arrayField);
		Builder builder = this.currentTarget.builder.getRepeatedFieldBuilder(this.currentTarget.arrayField, nbr);
		Target target = new Target(builder, null);
		this.push();
		this.currentTarget = target;
		return builder;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#endObject()
	 */
	@Override
	public Object endObject() {
		this.checkNullObject();
		Builder builder = this.currentTarget.builder;
		this.pop();
		return builder;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginArray(java.lang.String)
	 */
	@Override
	public Object beginArray(String fieldName) {
		FieldDescriptor field = this.getField(fieldName);
		if (field == null) {
			return null;
		}

		Builder builder = this.currentTarget.builder.getRepeatedFieldBuilder(field, 0);
		Target target = new Target(builder, field);
		this.push();
		this.currentTarget = target;
		return builder;
	}

	private void pop() {
		if (this.stack.isEmpty()) {
			throw new ApplicationError("endObject() when there is no open object");
		}
		this.currentTarget = this.stack.pop();
	}

	private void push() {
		this.stack.push(this.currentTarget);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginArray()
	 */
	@Override
	public Object beginArrayAsArrayElement() {
		throw new ApplicationError("This writer can not add array of arrays");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#endArray()
	 */
	@Override
	public Object endArray() {
		this.checkNullArray();
		Builder builder = this.currentTarget.builder;
		this.pop();
		return builder;
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
		if (this.rootMessage == null) {
			this.rootMessage = this.messageBuilder.build();
		}
		this.rootMessage.writeTo(stream);
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

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.RespWriter#useAsResponse(java.lang.Object)
	 */
	@Override
	public void setAsResponse(Object responseObject) {
		if (responseObject instanceof Message) {
			this.rootMessage = (Message) responseObject;
			return;
		}
		throw new ApplicationError(
				this.getClass().getSimpleName() + " uses a Message as a reponse object, but an object of type "
						+ responseObject.getClass().getName() + " is asked to be used.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.RespWriter#moveToObject(java.lang.String)
	 */
	@Override
	public Object moveToObject(String qualifiedFieldName) {
		return ProtoUtil.getBuilderForQualifiedField(this.messageBuilder, qualifiedFieldName, false);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.RespWriter#moveToArray(java.lang.String)
	 */
	@Override
	public Object moveToArray(String qualifiedFieldName) {
		return ProtoUtil.getBuilderForQualifiedField(this.messageBuilder, qualifiedFieldName, true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.RespWriter#setAsCurrentObject(java.lang.Object)
	 */
	@Override
	public void setAsCurrentObject(Object object) {
		if (object instanceof Builder == false) {
			logger.error("Writer expects a Builder as current object but an instance of class {} supplied.",
					object.getClass().getName());
		}
	}

}

class Target {
	/**
	 * @param builder
	 * @param arrayField
	 */
	public Target(Builder builder, FieldDescriptor arrayField) {
		this.builder = builder;
		this.arrayField = arrayField;
		for (FieldDescriptor field : builder.getAllFields().keySet()) {
			this.fields.put(field.getName(), field);
		}
	}

	/**
	 * builder to be used to build this message
	 */
	final Builder builder;
	/**
	 * field name of the array being built. null if the object is build, and not
	 * an array of that message
	 */
	final FieldDescriptor arrayField;
	/**
	 * unfortunately, builder does not have a method to get a field by name. We
	 * have to iterate thru a set of keys to get it. Since we expect the caller
	 * to come back with repeated request for field-name-based methods, we get
	 * ready with a map.
	 *
	 */
	final Map<String, FieldDescriptor> fields = new HashMap<String, FieldDescriptor>();
}
