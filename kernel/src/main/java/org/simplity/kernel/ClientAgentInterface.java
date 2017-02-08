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

import org.simplity.kernel.value.Value;
import org.simplity.service.ExceptionListener;

/**
 * this interface is introduced for the sake of initializing agents at start-up
 * without putting dependency
 *
 * @author simplity.org
 *
 */
public interface ClientAgentInterface {

	/**
	 * @param autoUserId
	 *            in case login is disabled and a default loginId is to be used
	 *            for all services
	 * @param cacher
	 *            client cache manager
	 * @param listener
	 *            exception listener
	 * @param sendTraceToClient
	 *            if true, traces are to be made accessible to client
	 */
	public void setUp(Value autoUserId, ClientCacheManager cacher,
			ExceptionListener listener, boolean sendTraceToClient);
}