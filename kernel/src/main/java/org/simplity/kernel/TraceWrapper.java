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

/**
 * Application developers may choose to wrap the service trace before pushing it
 * to the logging stream
 *
 * @author simplity.org
 *
 */
public interface TraceWrapper {
	/**
	 * wrap service trace text to suit your logging standard.
	 *
	 * @param serviceName
	 *            name of service
	 * @param userId
	 *            userId on whose request this service was executed
	 * @param elapsedMillis
	 *            number of milli-seconds taken by the service
	 * @param traceText
	 *            accumulated chronological emits during execution of service
	 * @return wrapped text ready to be pushed to the logging stream. If null is
	 *         returned, we assume that you do not want this to be logged.
	 *         Either you have pushed it somewhere, or burnt it :-)
	 */
	public String wrap(String serviceName, String userId, int elapsedMillis,
			String traceText);
}
