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
/**
 * classes for use as well as example of using sso in a project.
 * We do not have any ready-to-use login for any standard SSO as of now,
 * but we intend to do that as and when we encounter those issues.
 * following is a typical sequence of events for an sso
 *
 * 1. user requests a page say, a.htm on application site.
 *
 * 2. web server on application site invokes filter.
 *
 * 3. filter detects that there is no sso cookie. It keeps this url in its session,
 *  and responds with with redirect to sso-login page. this request has a call-back
 *  page that is hard-coded by
 *
 * 4. browser now displays the login page from sso site.
 *
 * 5. user enters sso credentials and submits the form
 *
 * 6. sso site authenticates user. It sets a cookie for the domain. It responds with
 * a redirection back to the hard-coded page set-up by the filter.
 *
 * 7. application site receives this request, and invokes the filter again.
 *
 * 8. filter now detects the cookie. it connects to the sso site using validation url.
 * gets details of user. It keeps the details, as well as the sso token in session.
 * It then uses the cached url that originally came (a.htm) and dispatches the request.
 *
 * 9. User now sees a.htm on her screen.
 *
 * 10. On any subsequent request, filter gets the cookie. It checks that value against
 * the cached token in session. On match, it allows the request, else redirects to
 * sso-login page again.
 *
 * HOW TO IMPLEMENT APPLICATION LOGIN.
 *
 * Typically, application would require its own login process to get some global
 * parameters cached for the logged-in user. If there is no SSO, they would have
 * written their login page, and would have set up a token of its own in session etc..
 * How do they work with sso?
 *
 * 1. In your current code, you would be detecting login some-where, failing which
 *  you would have redirected request to login, or would have responded to user with
 *  no-login-status.
 *
 * 2. In the above code, once you detect no-login, call Ss0Authenticator.getUserInfo()
 * instead.
 *
 * 3. If you get userInfo, you are fine. Simulate a successful login.
 *  That is, whatever you would do after user sends a valid pass-word.
 *  Typically, you would set a cookie, or set a csrf-token.
 *  You would also read some parameters from data base and save them in session
 *  for subsequent use.
 *
 * 4. If you get null, do the same thing you would have done when you detect no-login.
 *
 * HOW TO WHITELIST REQESTS BETWEEN SERVERS THAT MAY NOT HAVE SSO
 *
 * We do not recommend such an approach. It is better to set up SSO that can take care
 * of applications as valid users of other applications. This is simple and secure so
 * that web-apps need not make any exceptions. However, in case the it-set up requires
 * such an arrangement, here is how you can organize that.
 *
 * 1. device a way to authenticate that the request is coming from the desired application.
 * It could be signature, or any other mechanism that the it-setup would provide.
 *
 * 2. make a list of resources that can be requested. This should be used exclusively by
 * other application. (You may use wrappers on other services for this sake)
 *
 * 3. Request for the above resources will use this internal authentication instead of sso.
 *
 * 4.
 * @author simplity.org
 *
 */
package org.simplity.kernel.sso;