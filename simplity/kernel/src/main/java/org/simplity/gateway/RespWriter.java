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

import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * @author simplity.org
 *
 */
public interface RespWriter {

	/**
	 * use this as the response object. If used, typically this is the only call
	 * before a writeOut() call.
	 *
	 * @param responseObject
	 */
	public void setAsResponse(Object responseObject);

	/**
	 * write a field as attribute-value pair. Simplity deals with primitives as
	 * Value objects, and hence a specific method for this. use object() to set
	 * non-value attributes
	 *
	 * @param fieldName
	 * @param value
	 */
	public void setField(String fieldName, Value value);

	/**
	 * write a field as attribute-value pair. Simplity deals with primitives as
	 * Value objects, and hence a specific method for this. use object() to set
	 * non-value attributes
	 *
	 * @param fieldName
	 * @param value
	 */
	public void setField(String fieldName, Object value);

	/**
	 * A non-primitive object. Writer may reject object assignments that it does
	 * not know how to handle.
	 *
	 * @param fieldName
	 * @param obj
	 * @return writer, so that methods can be cascaded
	 */
	public void setObject(String fieldName, Object obj);

	/**
	 * write a n a name-array
	 *
	 * @param arrayName
	 * @param arr
	 * @return writer, so that methods can be cascaded
	 */
	public void setArray(String arrayName, Object[] arr);

	/**
	 * use the first column of the data sheet as an array
	 *
	 * @param arrayName
	 * @param sheet
	 * @return writer, so that methods can be cascaded
	 */
	public void setArray(String arrayName, DataSheet sheet);

	/*
	 * It may be expensive to build data sheet and then write them out.
	 * Especially if we are dealing with hierarchical data. So we provide
	 * methods to write them out as and when the lower level data is available,
	 * there by avoiding creation of large data-objects
	 */
	/**
	 * start name-object pair, and allow its attributes to be written out
	 *
	 * @param objectName
	 * @return writer, so that methods can be cascaded
	 */
	public Object beginObject(String objectName);

	/**
	 * start an object as an element of an array (hence no name)
	 *
	 * @return writer, so that methods can be cascaded
	 */
	public Object beginObjectAsArrayElement();

	/**
	 * close the last open object
	 *
	 * @return writer, so that methods can be cascaded
	 */
	public Object endObject();

	/**
	 * begin an array as an attribute (hence name)
	 *
	 * @param arrayName
	 * @return writer, so that methods can be cascaded
	 */
	public Object beginArray(String arrayName);

	/**
	 * begin an array as an element of parent array (hence no name)
	 *
	 * @return writer, so that methods can be cascaded
	 */
	public Object beginArrayAsArrayElement();

	/**
	 * close the array
	 *
	 * @return writer, so that methods can be cascaded
	 */
	public Object endArray();

	/**
	 * write a field as an element of an array (hence no name). this is valid
	 * only when a
	 * beginArray() is active.
	 *
	 * @param value
	 * @return writer, so that methods can be cascaded
	 */
	public void addToArray(Object value);

	/**
	 *
	 * @return final response as specific object. writer is closed, if it is not
	 *         already closed. You have to choose to use this method or
	 *         getFinalResponseObject for optimal use by avoiding unnecessary
	 *         serialization/de-serialization
	 */
	public Object getFinalResponseObject();

	/**
	 * job is done. flush out.
	 *
	 * @param stream
	 *            to which output should be flushed to. null if the writer is
	 *            already bound to a stream.
	 * @throws IOException
	 */
	public void writeout(OutputStream stream) throws IOException;

	/**
	 * Service has delegated writing to this writer. write out response based on
	 * spec
	 *
	 * @param ctx
	 */
	public void pullDataFromContext(ServiceContext ctx);

	/**
	 * is this writer based on output specification so that it can select, and
	 * format required out put data
	 *
	 * @return true if it has details of output parameters. false if it is a
	 *         pipe and service should take care of that
	 */
	public boolean hasOutputSpec();

	/**
	 * make this qualified field as the target object for all setXXX commands.
	 * fields of root objects have no prefix. child filed is prefixed with
	 * parentField and a dot. for example "orders.orderDetails". This array is
	 * created if required.
	 *
	 * @param qualifiedFieldName
	 * @return null if no object found with that field name, and it could not be
	 *         created.
	 */

	public Object moveToObject(String qualifiedFieldName);

	/**
	 * make this qualified field as the target array for all setXXX commands.
	 * fields of root objects have no prefix. child filed is prefixed with
	 * parentField and a dot. for example "orders.orderDetails". This array is
	 * created if required.
	 *
	 * @param qualifiedFieldName
	 * @return null if no object found with that field name, and it could not be
	 *         created.
	 */

	public Object moveToArray(String qualifiedFieldName);

	/**
	 * Use this as the current object.
	 *
	 * @param object
	 *            typically an object that was returned earlier. Advanced users
	 *            can use this to build different objects all together.
	 */
	public void setAsCurrentObject(Object object);
	/*
	 * caching related functionality is temporary. This will be re-factored
	 * directly to service agent
	 */

	/**
	 * @return
	 * 		get the key based on which the response can be cached.
	 *         emptyString means no key is used for caching. null means it can
	 *         not be cached.
	 */
	public String getCachingKey();

	/**
	 * @return
	 * 		number of minutes the cache is valid for. 0 means it has no
	 *         expiry. This method is relevant only if getCachingKey returns
	 *         non-null (indication that the service can be cached)
	 */
	public int getCacheValidity();

	/**
	 * @param key
	 *            key based on which the response can be cached.
	 *            emptyString means no key is used for caching. null means it
	 *            can not be cached
	 * @param minutes
	 *            if non-null cache is to be invalidated after these many
	 *            minutes
	 */
	public void setCaching(String key, int minutes);

	/**
	 * @return
	 * 		cached keys that need to be invalidated
	 */
	public String[] getInvalidations();

	/**
	 * @param invalidations
	 *            cached keys that need to be invalidated
	 */
	public void setInvalidations(String[] invalidations);

}
