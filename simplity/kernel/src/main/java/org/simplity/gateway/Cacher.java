/*
 * Copyright (c) 2016 simplity.org
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

/**
 * @author simplity.org
 *
 */
public interface Cacher {
	/**
	 * cache the response object
	 *
	 * @param serviceName
	 *            non-null service name the response is meant for.
	 * @param attributes
	 *            null implies that this response is valid for the service with
	 *            no key, and for ever. if non-null, cached response is subject
	 *            to restrictions as per attributes. key and expiry are known
	 *            attributes as of now
	 * @param response
	 *            cached object to be retrieved on demand
	 */
	public void cache(String serviceName, CashingAttributes attributes, Object response);

	/**
	 * get a cached response
	 *
	 * @param serviceName
	 *            non-null
	 * @param inData
	 *            can be null in case the caching is at service level. method
	 *            fails in case the caching for this service is by key
	 * @return cached object if found. null otherwise
	 */
	public Object get(String serviceName, Object inData);

	/**
	 * remove from cache
	 *
	 * @param serviceName
	 *            non-null
	 * @param attrs
	 *            null if this service is not cached-by key, or if we have to
	 *            remove all caches for this service irrespective of the key
	 */
	public void uncache(String serviceName, CashingAttributes attrs);
}
