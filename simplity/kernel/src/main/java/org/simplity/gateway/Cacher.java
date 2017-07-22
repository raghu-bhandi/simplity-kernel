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

import java.util.Date;

/**
 * @author simplity.org
 *
 */
public interface Cacher {
	/**
	 * keep an object with a key till its expiry
	 *
	 * @param key
	 *            non-null. unique key by which this object is cached.
	 * @param object
	 *            non-null.
	 * @param expiry cached object would invalidate at this time. null implies no expiry
	 */
	public void put(String key, Object object, Date expiry);

	/**
	 * get object that was cached with this key
	 *
	 * @param key
	 *            non-null
	 * @return cached object if found. null if never kept, or if it expired
	 */
	public Object get(String key);

	/**
	 * remove from cache.
	 *
	 * @param key
	 *            non-null
	 */
	public void remove(String key);
}
