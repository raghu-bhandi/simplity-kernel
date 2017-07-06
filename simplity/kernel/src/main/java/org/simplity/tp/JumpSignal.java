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

package org.simplity.tp;

/** @author simplity.org */
public enum JumpSignal {
  /** break out of this block. Valid if the action is inside a loop */
  BREAK,
  /** continue with the next loop, valid if this is inside a loop */
  CONTINUE,
  /** stop the service normally */
  STOP;
  /** convention to signal stop */
  public static final String _STOP = "_stop";
  /** convention to signal continue to the next iteration of a loop */
  public static final String _CONTINUE = "_continue";
  /** convention to signal breaking out of a loop */
  public static final String _BREAK = "_break";
  /**
   * check if this text is a signal. null if it is not.
   *
   * @param text
   * @return signal or null;
   */
  public static JumpSignal getSignal(String text) {
    if (_STOP.equals(text)) {
      return STOP;
    }
    if (_BREAK.equals(text)) {
      return BREAK;
    }
    if (_CONTINUE.equals(text)) {
      return CONTINUE;
    }
    return null;
  }
}
