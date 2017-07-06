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

package org.simplity.ide;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.simplity.kernel.Tracer;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

/**
 * An example logic action that consumes some elapsed time. This is useful in demo/test services
 * with asynchronous actions. This action will take anywhere from 1 to 10 seconds to complete.
 *
 * @author simplity.org
 */
public class AsynchHelloWorld implements LogicInterface {
  static final Logger logger = Logger.getLogger(AsynchHelloWorld.class.getName());

  /*
   * (non-Javadoc)
   *
   * @see org.simplity.tp.LogicInterface#execute(org.simplity.service.
   * ServiceContext)
   */
  @Override
  public Value execute(ServiceContext ctx) {
    // pick a time up to 10 seconds
    long l = Math.round(10000 * Math.random());

    logger.log(Level.INFO, "Hello World logic " + "will take a nap for " + l + "ms");
    Tracer.trace("Hello World logic " + "will take a nap for " + l + "ms");
    boolean interrupted = false;
    try {
      Thread.sleep(l);
    } catch (InterruptedException e) {

      logger.log(Level.INFO, "Hello World logic got woken...");
      Tracer.trace("Hello World logic got woken...");
      interrupted = true;
    }

    logger.log(Level.INFO, "Hello World... rather belated-");
    Tracer.trace("Hello World... rather belated-");
    /*
     * be a responsible method -we should not digest the interrupt. We are
     * to relay that.
     */
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
    return Value.VALUE_TRUE;
  }
}
