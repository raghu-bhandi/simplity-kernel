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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel;

import javax.servlet.http.HttpSession;

import org.simplity.service.ServiceData;

/** @author simplity.org */
public interface ClientCacheManager {

  /**
   * get a cached response
   *
   * @param inData
   * @param session
   * @return service data that is the response to this request
   */
  public ServiceData respond(ServiceData inData, HttpSession session);

  /**
   * Cache this response, if you want
   *
   * @param inData
   * @param outData
   * @param session
   */
  public void cache(ServiceData inData, ServiceData outData, HttpSession session);

  /**
   * remove/invalidate any cache for this service
   *
   * @param serviceName
   * @param session
   */
  public void invalidate(String serviceName, HttpSession session);
}
