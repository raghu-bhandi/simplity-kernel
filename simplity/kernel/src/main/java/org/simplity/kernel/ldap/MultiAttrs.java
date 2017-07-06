package org.simplity.kernel.ldap;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.util.TextUtil;
import org.simplity.service.ServiceContext;

public class MultiAttrs {
  String objectId;
  String attrIDs[];
  String outputDataSheetName;

  public boolean getAttributes(ServiceContext ctx) throws NamingException {
    Attributes attrs = null;
    DirContext ldapCtx = LdapProperties.getInitialDirContext();
    try {
      attrs = ldapCtx.getAttributes(objectId, attrIDs);
    } catch (NamingException ne) {
      throw new ApplicationError("LdapRead: Problem getting Attributes; " + ne.getMessage());
    } finally {
      ldapCtx.close();
    }

    if (attrs == null) {
      throw new ApplicationError("LdapRead: Problem getting Attributes; ");
    }

    String[] columnNames = {"key", "value"};
    String[][] data = new String[attrIDs.length][2];

    for (int i = 0; i < attrIDs.length; i++) {
      data[i][0] = attrIDs[i];
      data[i][1] = attrs.get(attrIDs[i]).get().toString();
    }

    MultiRowsSheet ms = new MultiRowsSheet(columnNames, data);
    ctx.putDataSheet(outputDataSheetName, ms);
    return true;
  }

  public void getfieldValues(ServiceContext ctx) {
    objectId = TextUtil.getFieldValue(ctx, objectId).toText();
    outputDataSheetName = TextUtil.getFieldValue(ctx, outputDataSheetName).toText();
  }
}
