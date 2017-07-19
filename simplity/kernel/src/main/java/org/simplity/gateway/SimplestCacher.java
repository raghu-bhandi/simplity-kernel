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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cacher that can cche by service name, with no capability to use keys or
 * expiry
 *
 * @author simplity.org
 *
 */
public class SimplestCacher implements Cacher {
	private static Logger logger = LoggerFactory.getLogger(SimplestCacher.class);
	private Map<String, Object> store = new HashMap<String, Object>();

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.Cacher#cache(java.lang.String,
	 * org.simplity.gateway.CashingAttributes, java.lang.Object)
	 */
	@Override
	public void cache(String serviceName, CashingAttributes attributes, Object object) {
		if (attributes != null && (attributes.expiresAt != null || attributes.keyNames != null)) {
			logger.error(
					"SimplestCacher is not designed to handle expiry or keys based caching. Object not cached for service "
							+ serviceName);
		}
		this.store.put(serviceName, object);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.Cacher#get(java.lang.String, java.lang.Object)
	 */
	@Override
	public Object get(String serviceName, Object inData) {
		return this.store.get(serviceName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.Cacher#uncache(java.lang.String,
	 * org.simplity.gateway.CashingAttributes)
	 */
	@Override
	public void uncache(String serviceName, CashingAttributes attrs) {
		this.store.remove(serviceName);
	}
}
