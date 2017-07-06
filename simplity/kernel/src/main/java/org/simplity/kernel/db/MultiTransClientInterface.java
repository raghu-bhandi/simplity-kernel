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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.simplity.kernel.db;

/**
 * Interface to be implemented by a class that wants to use a connection for multiple transaction.
 *
 * <p>instance can use driver.commit() or driver.rollback();
 *
 * @author simplity.org
 */
public interface MultiTransClientInterface {
  /**
   * Method invoked by db driver asking the call back object to do its transactions. This method is
   * invoked after allotting the required resources. resources are released once this method
   * returns. Object instance can use dbDriver to do commit() and rollback() Note that the rdbDriver
   * instance will work only inside this method. You should not keep a pointer to this instance
   * beyond this method. However, even if you do, the instance CAN NOT be used for any rdb
   * operation, because it would have released all resources.
   *
   * @param driver
   * @return 0 if the transaction is to be rolled-back, > 0 if transaction is to be committed. < 0
   *     (negative) if it is the end of transaction.
   */
  public int doMultiplTrans(DbDriver driver);
}
