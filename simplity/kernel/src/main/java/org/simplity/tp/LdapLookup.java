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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.value.Value;
import org.simplity.service.DataExtractor;
import org.simplity.service.ServiceContext;

import org.simplity.kernel.ldap.Exists;
import org.simplity.kernel.ldap.MultiAttrs;
import org.simplity.kernel.ldap.SingleAttr;

/** @author simplity.org */
public class LdapLookup extends Action {

  Exists exists;

  SingleAttr singleAttr;

  MultiAttrs multiAttrs;

  /** object instance for re-use */
  private DataExtractor dataExtractor;

  @Override
  protected Value doAct(ServiceContext ctx) {
    boolean returnValue = false;

    try {

      if (exists != null) {
        exists.getfieldValues(ctx);
        returnValue = exists.doesObjectExist(ctx);
      }

      if (singleAttr != null) {
        singleAttr.getfieldValues(ctx);
        returnValue = singleAttr.getAttribute(ctx);
        if (ctx.isInError()) {
          return Value.VALUE_FALSE;
        }
      }

      if (multiAttrs != null) {
        multiAttrs.getfieldValues(ctx);
        returnValue = multiAttrs.getAttributes(ctx);
        if (ctx.isInError()) {
          return Value.VALUE_FALSE;
        }
      }

    } catch (NamingException e) {
      e.printStackTrace();
    }
    return Value.newBooleanValue(returnValue);
  }

  public NamingEnumeration getObjects(String base, String filter, SearchControls controls) {
    NamingEnumeration answer = null;
    DirContext ldapCtx = null;
    try {
      answer = ldapCtx.search(base, filter, controls);
    } catch (NamingException ne) {
      throw new ApplicationError("LdapRead : NamingException" + ne.getMessage());
    }
    return answer;
  }

  @Override
  public void getReady(int idx, Service service) {
    super.getReady(idx, service);
  }

  @Override
  public int validate(ValidationContext ctx, Service service) {
    int count = super.validate(ctx, service);
    if ((this.exists == null ^ singleAttr == null ^ multiAttrs == null)
        ^ (this.exists == null && singleAttr == null && multiAttrs == null)) {
      ctx.addError("one of lookup types are required");
      count++;
    }
    return count;
  }
}
