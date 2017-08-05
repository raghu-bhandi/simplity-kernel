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

import org.simplity.kernel.data.DataSheet;
import org.simplity.service.ServiceContext;

/**
 * @author simplity.org
 *
 */
public interface RespWriter {

	/**
	 * this text is the response. Disregard if something was written earlier,
	 * and do not allow any more writes
	 *
	 * @param string
	 * @return writer, so that methods can be cascaded
	 */
	public RespWriter writeCompleteResponse(String string);

	/*
	 * methods used by outputData when the data is fully ready, and we need to
	 * just send them to client
	 */

	/**
	 * write a field as attribute-value pair. Simplity deals with primitives as
	 * Value objects, and hence a specific method for this. use object() to set
	 * non-value attributes
	 *
	 * @param fieldName
	 * @param value
	 * @return writer, so that methods can be cascaded
	 */
	public RespWriter field(String fieldName, Object value);

	/**
	 * write a name-object pair. If the object is primitive value
	 * (simplity-value, or string and date) then this is similar to a field.
	 *
	 * If object is collection then we follow the rules for writing out them as
	 * collections.
	 *
	 * if it is any other object, we resort to just taking taking toString() and
	 * write it out as if it is a text field
	 *
	 * @param fieldName
	 * @param obj
	 * @return writer, so that methods can be cascaded
	 */
	public RespWriter object(String fieldName, Object obj);

	/**
	 * write an a name-array
	 *
	 * @param arrayName
	 * @param arr
	 * @return writer, so that methods can be cascaded
	 */
	public RespWriter array(String arrayName, Object[] arr);

	/**
	 * use the first column of the data sheet as an array
	 *
	 * @param arrayName
	 * @param sheet
	 * @return writer, so that methods can be cascaded
	 */
	public RespWriter array(String arrayName, DataSheet sheet);

	/*
	 * It may be expensive to build data sheet and then write them out.
	 * Especially if we are dealing with hierarchical data. So we provide
	 * methods to write them out as and when the lower level data is available,
	 * there by avoiding creation of large data-objects
	 */

	/**
	 * write a field as an element of an array (hence no name). this is valid
	 * only when a
	 * beginArray() is active.
	 *
	 * @param value
	 * @return writer, so that methods can be cascaded
	 */
	public RespWriter field(Object value);

	/**
	 * start an object as an element of an array (hence no name)
	 *
	 * @return writer, so that methods can be cascaded
	 */
	public RespWriter beginObject();

	/**
	 * start name-object pair, and allow its attributes to be written out
	 *
	 * @param objectName
	 * @return writer, so that methods can be cascaded
	 */
	public RespWriter beginObject(String objectName);

	/**
	 * close the last open object
	 *
	 * @return writer, so that methods can be cascaded
	 */
	public RespWriter endObject();

	/**
	 * begin an array as an attribute (hence name)
	 *
	 * @param arrayName
	 * @return writer, so that methods can be cascaded
	 */
	public RespWriter beginArray(String arrayName);

	/**
	 * begin an array as an element of parent array (hence no name)
	 *
	 * @return writer, so that methods can be cascaded
	 */
	public RespWriter beginArray();

	/**
	 * close the array
	 *
	 * @return writer, so that methods can be cascaded
	 */
	public RespWriter endArray();

	/**
	 *
	 * @return final response. writer is closed, if it is not already closed.
	 *         You have to choose to use this method or getFinalResponseObject
	 *         for optimal use by avoiding unnecessary
	 *         serialization/de-serialization
	 *
	 */
	public String getFinalResponseText();

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
	 * @param writer
	 *            to which output should be flushed to. null if the writer was
	 *            created with a writer.
	 * @throws IOException
	 */
	public void writeout(Writer writer) throws IOException;

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

	/**
	 * Service has delegated writing to this writer. write out response based on
	 * spec
	 *
	 * @param ctx
	 */
	public void writeAsPerSpec(ServiceContext ctx);

}
