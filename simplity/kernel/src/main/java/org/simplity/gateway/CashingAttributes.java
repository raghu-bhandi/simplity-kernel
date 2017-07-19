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
 * data structure that has information about caching
 *
 * @author simplity.org
 *
 */
public class CashingAttributes {
	/**
	 * null if this does not expire
	 */
	public final Date expiresAt;

	/**
	 * array of key field names, if this is cached based on keys
	 */
	public final String[] keyNames;
	/**
	 * array of values for the keys. null if this is not cached by keys
	 */
	public final String[] values;

	/**
	 *
	 * @param expiresAt
	 *            null implies this does not expire.
	 * @param names
	 *            array of key names. null if this is not cached by keys.
	 * @param values
	 *            array of values for the keys. null if this is not cached by
	 *            keys
	 */
	public CashingAttributes(Date expiresAt, String[] names, String[] values) {
		this.expiresAt = expiresAt;
		this.keyNames = names;
		this.values = values;
	}
}
