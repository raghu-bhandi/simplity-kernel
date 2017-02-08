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
 * Data structure that captures parameters for a client agent
 *
 * @author simplity.org
 *
 */
public class ClientAgentParams {
	/**
	 * class name of clientAgent
	 */
	String clientAgent;
	/**
	 * should we send server trace to client?
	 */
	boolean sendTraceToClient;
	/**
	 * if you want to disable login, and use a dummy user id for all services,
	 * typically during development/demo. Ensure that this value is numeric in
	 * case you have set userIdIsNumber="true"
	 */
	String autoLoginUserId;
	/**
	 * Response to service request can be cached at two levels : at the Web tier
	 * or at the service level. specify fully qualified class name you want use
	 * as cache manager at we tier. This class must implement HttpCacheManager
	 * interface. You may start with the a simple one called
	 * org.siplity.http.SimpleCacheManager that caches services based on service
	 * definition inside http sessions.
	 */
	String clientCacheManager;
}
