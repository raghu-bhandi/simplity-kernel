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
package org.simplity.kernel.db;

/**
 * Interface to be implemented by a class that wants to work with RdbDriver.
 *
 * <pre>
 * Typical way of working is:
 * 1. call driver to get a connection.
 * 2. work with connection.
 * 3. call driver to return/close connection.
 * </pre>
 *
 * Connection and records set are such crucial resources, that we do not want to take any chance.
 * Hence instead of the above sequence, we have devised the following.
 *
 * <pre>
 * 1. call static method RdbDriver.workWithDriver(callbackObject, dbAccessType)
 * 2. driver will create an appropriate driver object instance
 * 3. calls callbackObject.workWithDriver(rdbdriverInstance);
 * 4. close connection, and release all resources associated with rdbDriver instance.
 * 5. control is returned back to you. (workWithDriver() returns.)
 *
 * even if you keep rdbDriverInstance, it can not be used for any operation, as it is not in that state.
 * </pre>
 *
 * @author simplity.org
 */
public interface DbClientInterface {
  /**
   * use an instance of rdbDriver to take care of all your rdb operations. Note that the rdbDriver
   * instance will work only inside this method. You should not keep a pointer to this instance
   * beyond this method. However, even if you do, the instance CAN NOT be used for any rdb
   * operation, because it would have released all resources.
   *
   * @param driver
   * @return true if operation is successful. All operations are committed. False in case any error,
   *     in which case operations are rolled-back.
   */
  public boolean workWithDriver(DbDriver driver);
}
