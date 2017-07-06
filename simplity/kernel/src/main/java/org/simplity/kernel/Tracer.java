/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

/**
 * common logging class used by all core simplity classes to provide a log that is naturally
 * sequenced in chronological order
 *
 * @author simplity.org
 */
public class Tracer {
  private static final char NEW_LINE = '\n';

  /**
   * reports just a trace information. This should be used to trace application level data/state
   * that helps in tracing the progress of service execution. It should not be used for debugging
   * internal APIs
   *
   * @param text text to be logged
   */
  public static void trace(String text) {
    ServiceLogger.log(Tracer.NEW_LINE + text);
  }

  /**
   * @param e exception being caught
   * @param msg additional message
   */
  public static void trace(Throwable e, String msg) {
    Throwable ex = e;
    if (ex instanceof SQLException) {
      Exception ex1 = ((SQLException) ex).getNextException();
      if (ex1 != null) {
        ex = ex1;
      }
    }
    StringWriter writer = new StringWriter();
    PrintWriter pw = new PrintWriter(writer);
    ex.printStackTrace(pw);
    trace(msg);
    trace(writer.getBuffer().toString());
  }
}
