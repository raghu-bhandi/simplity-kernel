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

package org.simplity.kernel;

/**
 * enumerates the popular logging framework that Simplity understands and can
 * attach itself to to emit service logs
 *
 * @author simplity.org
 *
 */
public enum LoggingFramework {
	/**
	 * log4j v1 that uses
	 */
	LOG4J_CLASSIC,
	/**
	 * log4j v2 that uses org.apache.logging.log4J.Logger
	 */
	LOG4J,
	/**
	 * commons logging, or JCL, that uses org.apache.coomons.Log
	 */
	COMMONS_LOGGING,
	/**
	 * SLF4J the stub that should be used by all future framework. logback uses this.
	 */
	SLF4J,

	/**
	 * standard java logging, or JULI, that uses java.util.Logger
	 */
	JULI
}
