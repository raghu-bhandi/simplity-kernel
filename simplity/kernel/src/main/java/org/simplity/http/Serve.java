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
package org.simplity.http;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Tracer;

/**
 * this is an example servlet to demonstrate how a project can use HttpAgent to deliver services in
 * the App layer to http clients
 *
 * @author simplity.org
 */
public class Serve extends HttpServlet {
  static final Logger logger = Logger.getLogger(Serve.class.getName());

  /*
   * we may have to co-exist with other application. It is possible that our
   * start-up never started. One check may not be too expensive. Our start-up
   * calls keep this as marker..
   */
  private static boolean startedUp = false;
  private static boolean startUpFailed = false;
  /*
   * of course we will have several other issues like logging....
   */
  private static final long serialVersionUID = 1L;

  /**
   * notify that the start-up is successful, and we can go ahead and serve
   *
   * @param succeeded Success flag
   */
  public static void updateStartupStatus(boolean succeeded) {
    if (succeeded) {
      startedUp = true;

      logger.log(Level.INFO, "Web Agent is given a green signal by Startup to start serving.");
      Tracer.trace("Web Agent is given a green signal by Startup to start serving.");
    } else {
      startUpFailed = true;

      logger.log(
          Level.INFO,
          "Web agent Serve will not be available on this server as Startup reported a failure on boot-strap.");
      Tracer.trace(
          "Web agent Serve will not be available on this server as Startup reported a failure on boot-strap.");
    }
  }

  /** post is to be used by client in AJAX call. */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    if (startedUp == false) {
      if (startUpFailed == false) {
        Startup.bootStrap(this.getServletContext());
      }
      if (startUpFailed) {
        /*
         * application had error during bootstrap.
         */
        this.reportError(
            resp,
            "Application start-up had an error. Please refer to the logs. No service is possible.");
        return;
      }
    }
    try {
      HttpAgent.serve(req, resp);
    } catch (Exception e) {
      String msg = "We have an internal error. ";

      logger.log(Level.SEVERE, msg, e);
      Tracer.trace(e, msg);
      this.reportError(resp, msg + e.getMessage());
    }
  }

  /**
   * Get is to be used ONLY IF POST is not possible for some reason. From security angle POST is
   * preferred
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    this.doPost(req, resp);
  }

  private void reportError(HttpServletResponse resp, String msg) throws IOException {

    logger.log(Level.INFO, msg);
    Tracer.trace(msg);
    FormattedMessage message = new FormattedMessage("internalerror", MessageType.ERROR, msg);
    FormattedMessage[] messages = {message};
    String response = HttpAgent.getResponseForError(messages);
    resp.getWriter().write(response);
  }
}
