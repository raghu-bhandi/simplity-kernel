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
import java.util.Stack;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * prepares a dynamic JSON Object based on the write commands received. This is
 * more expensive than JSONRespWriter because the object is first built and then
 * it is serialized. To be used if hierarchical data is to be extracted directly
 * to the response without creating data sheets.
 *
 * @author simplity.org
 *
 */
public class JsonObjectRespWriter implements RespWriter {
	private static final Logger logger = LoggerFactory.getLogger(JsonObjectRespWriter.class);
	/**
	 * JSON object that holds all data
	 */
	private Object rootObject = this.currentObject;

	/**
	 * we keep objects that are being built into stack, in case of recursive
	 * calls
	 */
	private Stack<Object> stack = new Stack<Object>();

	/**
	 * current object that is receiving data. null if an array is receiving
	 * data.
	 */
	private JSONObject currentObject = new JSONObject();

	/**
	 * current array that is receiving data. null if an object is receiving
	 * data.
	 */
	private JSONArray currentArray = null;

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
	 * get the response text and close the writer. That is the reason this
	 * method is called getFinalResponse rather than getResponse. Side effect of
	 * closing the writer is hinted with this name
	 *
	 * @return response text.Null if nothing was written so far, or the writer
	 *         was piped to another output mechanism
	 */
	@Override
	public Object getFinalResponseObject() {
		return this.currentObject;
	}

	private void checkNullObject() {
		if (this.currentObject == null) {
			throw new ApplicationError("A field is being set when an array is open. Error in th call sequence.");
		}
	}

	private void checkNullArray() {
		if (this.currentArray == null) {
			throw new ApplicationError(
					"An array element is being set when an object is open (but not an array). Error in th call sequence.");
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#field(java.lang.String,
	 * org.simplity.kernel.value.Value)
	 */
	@Override
	public void setField(String fieldName, Object value) {
		this.checkNullObject();
		this.currentObject.put(fieldName, value);
	}

	@Override
	public void setField(String fieldName, Value value) {
		this.checkNullObject();
		if(value != null)
		 this.currentObject.put(fieldName, value.toObject());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#field(java.lang.Object)
	 */
	@Override
	public void addToArray(Object value) {
		this.checkNullArray();
		this.currentArray.put(value);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#object(java.lang.String,
	 * java.lang.Object)
	 */
	@Override
	public void setObject(String fieldName, Object value) {
		this.checkNullObject();
		if (value instanceof JSONArray || value instanceof JSONObject) {
			this.currentObject.put(fieldName, value);
		}
		throw new ApplicationError(
				"Only JSONObject and JSONArray instances can be added as objects. We received an object of type "
						+ value.getClass().getName());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#array(java.lang.String,
	 * java.lang.Object[])
	 */
	@Override
	public void setArray(String fieldName, Object[] arr) {
		this.checkNullObject();
		JSONArray jar = new JSONArray();
		for (Object obj : arr) {
			jar.put(obj);
		}
		this.currentObject.put(fieldName, jar);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#array(java.lang.String,
	 * org.simplity.kernel.data.DataSheet)
	 */
	@Override
	public void setArray(String fieldName, DataSheet sheet) {
		this.checkNullObject();
		JSONWriter writer = new JSONWriter();
		JsonUtil.sheetToArray(writer, sheet);
		JSONArray arr = new JSONArray(writer);
		this.currentObject.put(fieldName, arr);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginObject(java.lang.String)
	 */
	@Override
	public Object beginObject(String fieldName) {
		this.checkNullObject();
		JSONObject obj = this.currentObject.optJSONObject(fieldName);
		if (obj == null) {
			obj = new JSONObject();
			this.currentObject.put(fieldName, obj);
		}
		this.push();
		this.currentObject = obj;
		return obj;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginObject(java.lang.String)
	 */
	@Override
	public Object beginObjectAsArrayElement() {
		this.checkNullArray();
		JSONObject obj = new JSONObject();
		this.currentArray.put(obj);

		this.push();
		this.currentObject = obj;
		return obj;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#endObject()
	 */
	@Override
	public Object endObject() {
		this.checkNullObject();
		Object obj = this.currentObject;
		this.pop();
		return obj;

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginArray(java.lang.String)
	 */
	@Override
	public Object beginArray(String fieldName) {
		this.checkNullObject();
		JSONArray arr = this.currentObject.optJSONArray(fieldName);

		if (arr == null) {
			arr = new JSONArray();
			this.currentObject.put(fieldName, arr);
		}
		this.push();
		this.currentArray = arr;
		this.currentObject = null;
		return arr;
	}

	private void pop() {
		if (this.stack.isEmpty()) {
			throw new ApplicationError("endObject() when there is no open object");
		}
		Object obj = this.stack.pop();
		if (obj instanceof JSONObject) {
			this.currentArray = null;
			this.currentObject = (JSONObject) obj;
		} else {
			this.currentArray = (JSONArray) obj;
			this.currentObject = null;
		}
	}

	private void push() {
		if (this.currentObject == null) {
			this.stack.push(this.currentArray);
			this.currentArray = null;
		} else {
			this.stack.push(this.currentObject);
			this.currentObject = null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#beginArray()
	 */
	@Override
	public Object beginArrayAsArrayElement() {
		this.checkNullArray();
		JSONArray arr = new JSONArray();
		this.currentArray.put(arr);
		this.push();

		this.currentArray = arr;
		return arr;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.OutputWriter#endArray()
	 */
	@Override
	public Object endArray() {
		this.checkNullArray();
		Object arr = this.currentArray;
		this.pop();
		return arr;
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
		Writer writer = new OutputStreamWriter(stream);
		writer.write(this.rootObject.toString());
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
		 * we are to write based on our spec
		 */
		for (Map.Entry<String, Value> entry : ctx.getAllFields()) {
			this.setField(entry.getKey(), entry.getValue());
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
				this.beginObjectAsArrayElement();
				for (int j = 0; j < names.length; j++) {
					this.setField(names[j], row[j]);
				}
				this.endObject();
			}
			this.endArray();
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
		this.rootObject = responseObject;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.RespWriter#moveToField(java.lang.String)
	 */
	@Override
	public Object moveToObject(String qualifiedFieldName) {
		if(qualifiedFieldName == null || qualifiedFieldName.isEmpty() || qualifiedFieldName.equals(".")){
			if(this.rootObject instanceof JSONObject){
				return this.rootObject;
			}
			throw new ApplicationError("Root is an Array, but is being used as an object");
		}
		return this.moveToTarget(qualifiedFieldName, true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.RespWriter#moveToArray(java.lang.String)
	 */
	@Override
	public Object moveToArray(String qualifiedFieldName) {
		if(qualifiedFieldName == null || qualifiedFieldName.isEmpty() || qualifiedFieldName.equals(".")){
			if(this.rootObject instanceof JSONObject){
				throw new ApplicationError("Root is an Object, but is being used as an array");
			}
			return this.rootObject;
		}
		return this.moveToTarget(qualifiedFieldName, false);
	}

	/**
	 * get the value of the qualified and make it the current object. Create this if required.
	 * @param qualifiedFieldName
	 * @param toObject field value is to be JSONObject, and JSONArray
	 * @return value of field. null if the value does not exist, and it can not be created
	 */
	private Object moveToTarget(String qualifiedFieldName, boolean toObject) {
		String[] parts = qualifiedFieldName.split("\\.");

		Object target = this.rootObject;
		boolean targetIsObject = true;
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			Object child;
			int idx = 0;
			if (targetIsObject) {
				child = ((JSONObject) target).opt(part);
			} else {
				idx = Integer.parseInt(part);
				child = ((JSONArray) target).get(idx);
			}
			if (child == null) {
				logger.error("going to create required sub-tree for field {}", qualifiedFieldName);
				child = this.createChildTree(parts, i, target, targetIsObject, toObject);
				targetIsObject = toObject;
				break;
			}
			if (target instanceof JSONObject) {
				targetIsObject = true;
			} else if (target instanceof JSONArray) {
				targetIsObject = false;
			} else {
				logger.error("field {} is {}.It should be either a JSONObject or JSONArray for positioning.",
						qualifiedFieldName, target.getClass().getName());
				return null;
			}
		}
		if (targetIsObject != toObject) {
			if (toObject) {
				logger.error("field {} is to be JSONObject, but it is a JSONArray.", qualifiedFieldName);
			} else {
				logger.error("field {} is to be JSONObject, but it is a JSONArray.", qualifiedFieldName);
			}
			return null;
		}
		this.push();
		if (targetIsObject) {
			this.currentObject = (JSONObject) target;
		} else {
			this.currentArray = (JSONArray) target;
		}
		return target;
	}

	/**
	 * create a child-tree as per the qualified field name in a moveToField
	 * operation.
	 *
	 * @param parts
	 *            of the qualified field name
	 * @param startAt
	 *            parts are missing from here
	 * @param parent
	 *            to which the child is to be attached to
	 * @param childIsObject
	 *            true means child is to be a JSONOBJect, false means it should
	 *            be a JSONArray
	 * @return field value at the end of the child-tree that is created
	 */
	private Object createChildTree(String[] parts, int startAt, Object parent, boolean parentIsObject, boolean childIsObject) {
		/*
		 * algorithm looks quite complex if you just read it. You should have an
		 * example by your side to understand what is being done and why
		 */
		/*
		 * if we start from the root(child), we do not know whether each of its
		 * child is to be an array or an object. That depends on the next part.
		 * look-ahead algorithms becomes complex. Hence we start from the end
		 * and work backwards.
		 *
		 * start with the last child
		 */
		Object objectAtLeaf = null;
		if (childIsObject) {
			objectAtLeaf = new JSONObject();
		} else {
			objectAtLeaf = new JSONArray();
		}

		Object child = objectAtLeaf;
		for (int i = parts.length - 1; i > startAt; i--) {
			/*
			 * if the part is a number, then we are to create an array, else an
			 * object
			 */
			String part = parts[i];
			char c = part.charAt(0);
			if (c >= '0' && c <= '9') {
				JSONArray arr = new JSONArray();
				arr.put(Integer.parseInt(part), child);
				child = arr;
			} else {
				JSONObject obj = new JSONObject();
				obj.put(part, child);
				child = obj;
			}
		}
		/*
		 * child-tree is ready. Add it to the parent.
		 */
		String part = parts[startAt];
		if(parentIsObject){
			((JSONObject) parent).put(part, child);
		}else{
			((JSONArray) parent).put(Integer.parseInt(part), child);
		}
		return objectAtLeaf;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.RespWriter#setAsCurrentObject(java.lang.Object)
	 */
	@Override
	public void setAsCurrentObject(Object object) {
		this.push();
		if (object instanceof JSONObject) {
			this.currentObject = (JSONObject) object;
		} else if (object instanceof JSONArray) {
			this.currentArray = (JSONArray) object;
		} else {
			throw new ApplicationError(
					"JsonObject requires either a JSONObject or JSONArray as the current object but an object f class "
							+ object.getClass().getName() + " is being set");
		}
	}
}
