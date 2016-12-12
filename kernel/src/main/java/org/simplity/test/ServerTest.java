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

import org.simplity.kernel.Application;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.util.XmlUtil;

/**
 * Wrapper around test cases for us to load all test cases for a service. Idea
 * is to have the xml for service to have logic as well as test cases in the
 * same file, but classes are optimized for run-time
 *
 * @author simplity.org
 *
 */
public class ServerTest {
	/**
	 * name
	 */
	String name;

	/**
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String root = "e:/repos/simplity/pet/WebContent/WEB-INF/comp/";
		String s = "service/tp/filterOwners.xml";
		// String s = "test/filter_owners.xml";
		Application.bootStrap(root);
		ServerTest st = new ServerTest();
		XmlUtil.xmlToObject(root + s, st);
		Tracer.trace("We are done");
	}
}
