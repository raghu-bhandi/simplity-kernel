package org.simplity.kernel.ldap;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

public class Exists {
	String objectId;
	String fieldName;

	public boolean doesObjectExist(ServiceContext ctx) throws NamingException {
		DirContext ldapCtx =  LdapProperties.getInitialDirContext();
		try {
			ctx.setBooleanValue(fieldName,ldapCtx.lookup(objectId) != null);
			return true;
		} catch (NamingException ne) {
			return false;
		} finally {
			ldapCtx.close();
		}
	}

	public void getfieldValues(ServiceContext ctx) {
		objectId = (TextUtil.getFieldValue(ctx, objectId).toText());
		fieldName = (TextUtil.getFieldValue(ctx, fieldName).toText());
	}
}

