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

package org.simplity.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Context that holds name-value pairs and test results during a test run.
 *
 */
public class TestContext {
	private Map<String, Object> values = new HashMap<String, Object>();
	private List<TestResult> results = new ArrayList<TestResult>();

	/**
	 * save a key-value pair
	 *
	 * @param key
	 * @param value
	 */
	public void setValue(String key, Object value) {
		this.values.put(key, value);

	}

	/**
	 * get a value for the key
	 *
	 * @param key
	 * @return value, or null if no such key was added earlier
	 */
	public Object getValue(String key) {
		return this.values.get(key);
	}

	/**
	 * add a test result
	 *
	 * @param result
	 */
	public void addResult(TestResult result) {
		this.results.add(result);
	}

	/**
	 * @return the results
	 */
	public TestResult[] getResults() {
		return this.results.toArray(new TestResult[0]);
	}
}
