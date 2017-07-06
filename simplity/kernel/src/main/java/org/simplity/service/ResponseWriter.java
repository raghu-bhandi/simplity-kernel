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

package org.simplity.service;

import org.simplity.kernel.value.Value;

/**
 * Though we are using JSON as of now, let us use an interface so that we are future-proof
 *
 * @author simplity.org
 */
public interface ResponseWriter {
  /** initialize. Typically start an outer object that contains everything. */
  public void init();

  /**
   * done with writing. this MUST be called before getting the response. Else response may not be
   * well-formed etc..
   *
   * @throws ResponseWriterException if this is not in end() able
   */
  public void end() throws ResponseWriterException;

  /**
   * start an object
   *
   * @return this for convenience
   * @throws ResponseWriterException
   */
  public ResponseWriter object() throws ResponseWriterException;

  /**
   * @return this for convenience
   * @throws ResponseWriterException
   */
  public ResponseWriter endObject() throws ResponseWriterException;

  /**
   * @return this for convenience
   * @throws ResponseWriterException
   */
  public ResponseWriter array() throws ResponseWriterException;

  /**
   * @return this for convenience
   * @throws ResponseWriterException
   */
  public ResponseWriter endArray() throws ResponseWriterException;

  /**
   * @param key
   * @return this for convenience
   * @throws ResponseWriterException
   */
  public ResponseWriter key(String key) throws ResponseWriterException;

  /**
   * @param value
   * @return this for convenience
   * @throws ResponseWriterException
   */
  public ResponseWriter value(long value) throws ResponseWriterException;
  /**
   * @param value
   * @return this for convenience
   * @throws ResponseWriterException
   */
  public ResponseWriter value(boolean value) throws ResponseWriterException;

  /**
   * @param value
   * @return this for convenience
   * @throws ResponseWriterException
   */
  public ResponseWriter value(double value) throws ResponseWriterException;

  /**
   * @param value
   * @return this for convenience
   * @throws ResponseWriterException
   */
  public ResponseWriter value(Value value) throws ResponseWriterException;

  /**
   * @param value
   * @return this for convenience
   * @throws ResponseWriterException
   */
  public ResponseWriter value(Object value) throws ResponseWriterException;

  /**
   * @return this for convenience
   * @throws ResponseWriterException
   */
  public String getResponse() throws ResponseWriterException;
}
