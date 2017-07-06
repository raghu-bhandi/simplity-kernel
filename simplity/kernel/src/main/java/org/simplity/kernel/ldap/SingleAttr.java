package org.simplity.kernel.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.simplity.kernel.Messages;
import org.simplity.kernel.util.TextUtil;
import org.simplity.service.ServiceContext;

public class SingleAttr {
  String objectId;
  String attrName;
  String fieldName;

  public boolean getAttribute(ServiceContext ctx) throws NamingException {
    Attributes attrs = null;
    Attribute a = null;
    DirContext ldapCtx = LdapProperties.getInitialDirContext();
    try {
      attrs = ldapCtx.getAttributes(this.objectId);
    } catch (NamingException e) {
      ctx.addMessage(Messages.ERROR, "LDAP object does not exist; " + e.getMessage());
      ldapCtx.close();
      return false;
    }
    try {
      a = attrs.get(this.attrName);
    } catch (NullPointerException e) {
      ctx.addMessage(
          Messages.ERROR,
          "LDAP attribute does not exist for object " + objectId + "; " + e.getMessage());
      return false;
    } finally {
      ldapCtx.close();
    }

    ctx.setTextValue(fieldName, a.get().toString());
    return true;
  }

  public void getfieldValues(ServiceContext ctx) {
    this.objectId = TextUtil.getFieldValue(ctx, this.objectId).toText();
    this.attrName = TextUtil.getFieldValue(ctx, this.attrName).toText();
    this.fieldName = TextUtil.getFieldValue(ctx, this.fieldName).toText();
  }
}
