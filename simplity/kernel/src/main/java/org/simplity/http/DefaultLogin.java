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
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.Tracer;
import org.simplity.service.ServiceProtocol;

/**
 * Our recommendation is that the client application should be designed to handle
 * login-service...-logout paradigm. It should keep track of login status. Hence we have a separate
 * url for login, service, and logout.
 *
 * <p>This is a dummy servlet that is useful during development. we expect a call from ours standard
 * client script. Refer to login() in simplity.js
 *
 * @author simplity.org
 */
public class DefaultLogin extends HttpServlet {
  static final Logger logger = Logger.getLogger(DefaultLogin.class.getName());

  /*
   * of course we will have several other issues like logging....
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String text = req.getHeader(ServiceProtocol.USER_TOKEN);
    if (text == null) {

      logger.log(Level.INFO, "No credentials received in header for login.");
      Tracer.trace("No credentials received in header for login.");
      return;
    }

    /*
     * we expect text to be userId + space + password. space and password
     * being optional.
     */
    int idx = text.indexOf(' ');
    String userId = text;
    String pwd = null;
    if (idx != -1) {
      userId = text.substring(0, idx);
      pwd = text.substring(idx + 1);
    }

    text = HttpAgent.login(userId, pwd, req.getSession(true));
    if (text == null) {
      FormattedMessage msg = HttpAgent.LOGIN_FAILED;
      FormattedMessage[] messages = {msg};
      text = HttpAgent.getResponseForError(messages);
    }
    Writer writer = resp.getWriter();
    writer.write(text);
    writer.close();
  }
}
