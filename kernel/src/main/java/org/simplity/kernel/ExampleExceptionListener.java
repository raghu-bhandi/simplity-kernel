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

package org.simplity.kernel;

import org.simplity.service.ExceptionListener;
import org.simplity.service.ServiceData;

/**
 * @author simplity.org
 *
 */
public class ExampleExceptionListener implements ExceptionListener {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ExceptionListener#listen(org.simplity.service.
	 * ServiceData, java.lang.Exception)
	 */
	@Override
	public void listen(ServiceData inputData, Exception e) {
		System.err.println(
				"Applicaiton has issues that require the attention of the support team");
		if (inputData != null) {
			System.err.println("Input : " + inputData.getPayLoad());
		}
		System.err.println("Error\n : " + e.getMessage());
	}

}
