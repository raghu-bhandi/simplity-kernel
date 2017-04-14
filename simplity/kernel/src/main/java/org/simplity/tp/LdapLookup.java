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

import java.util.Collection;
import java.util.Hashtable;
import java.util.TreeSet;

import javax.jms.TextMessage;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.ldap.LdapAgent;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.DataExtractor;
import org.simplity.service.ServiceContext;

/**
 * @author simplity.org
 *
 */
public class LdapLookup extends Action {
	String objectId;
	String attrName;
	String fieldName;
	
	private String parsedObjectId;
	private String parsedAttrName;
	
	private Value parsedObjectIdValue;
	private Value parsedAttrNameValue;

	/**
	 * object instance for re-use
	 */
	private DataExtractor dataExtractor;

	private DirContext ldapCtx;

	@Override
	protected Value doAct(ServiceContext ctx) {
		if (this.parsedObjectId != null) {
			this.parsedObjectIdValue = ctx.getValue(this.parsedObjectId);
		} else {
			this.parsedObjectIdValue = Value.newTextValue(this.objectId);
		}

		if (this.parsedAttrName != null) {
			this.parsedAttrNameValue = ctx.getValue(this.parsedAttrName);
		} else {
			this.parsedAttrNameValue = Value.newTextValue(this.attrName);
		}		
		Hashtable<String, String> env = new Hashtable<String, String>(11);
		try {
			// Create initial context
			ldapCtx = LdapAgent.getInitialDirContext();
			Attribute attr = this.getAttribute(ctx,objectId, attrName);
			if(ctx.isInError()){
				return Value.VALUE_FALSE;
			}
			this.extractAttributes(attr, ctx);
			ldapCtx.close();
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return Value.VALUE_TRUE;
	}

	private void extractAttributes(Attribute attr, ServiceContext ctx) {		
			try {
				NamingEnumeration<?> attrAll = attr.getAll();

				if (attrAll != null) {
					while (attrAll.hasMore()) {
						ctx.setTextValue(this.fieldName, attrAll.next().toString());
					}
				}
			} catch (NamingException e) {
				throw new ApplicationError(e, "Unable to extract the attributes for the lookup");
			}
			return;
	}

	public Attributes getAllAttributes(String name) throws NamingException {
		return ldapCtx.getAttributes(name);
	}

	public NamingEnumeration getAttributes(String userId) {
		Attributes attrs = null;
		NamingEnumeration neAttrs = null;
		try {
			attrs = ldapCtx.getAttributes(userId);
			neAttrs = attrs.getAll();
		} catch (NamingException ne) {
			throw new ApplicationError("LdapRead: Problem getting Attributes; " + ne.getMessage());
		}
		return neAttrs;
	}

	public NamingEnumeration getAttributes(String userId, String attrIDs[]) {
		Attributes attrs = null;
		NamingEnumeration neAttrs = null;
		try {
			attrs = ldapCtx.getAttributes(userId, attrIDs);
			neAttrs = attrs.getAll();
		} catch (NamingException ne) {
			throw new ApplicationError("LdapRead: Problem getting Attributes; " + ne.getMessage());
		}
		return neAttrs;
	}

	public Object getSingleValueOfAttribute(Attribute a) {
		Object attrValue;
		try {
			attrValue = a.get();
		} catch (Exception npe) {
			throw new ApplicationError("LdapRead: Problem getting Attribute; " + npe.getMessage());
		}
		return attrValue;
	}

	public Object getSingleValueOfAttribute(String userId, String attrName) {
		Attributes attrs = null;
		Object attrValue = null;
		Attribute a = null;
		try {
			attrs = ldapCtx.getAttributes(userId);
			a = attrs.get(attrName);
			attrValue = getSingleValueOfAttribute(a);
		} catch (Exception npe) {
			throw new ApplicationError("LdapRead: Problem getting Attribute" + npe.getMessage());
		}
		return attrValue;
	}

	public Attribute getAttribute(ServiceContext ctx, String objectId, String attrName) {
		Attributes attrs = null;
		Attribute a = null;
		try {
			attrs = ldapCtx.getAttributes(objectId);
		} catch (NamingException e) {
			ctx.addMessage(Messages.ERROR,"LDAP object does not exist; " + e.getMessage());
			return null;
		}
		try {
			a = attrs.get(attrName);
		} catch (NullPointerException e) {
			ctx.addMessage(Messages.ERROR,"LDAP attribute does not exist for object "+ objectId+"; " + e.getMessage());
			return null;
		}
		return a;
	}

	public Collection getAttributeValues(ServiceContext ctx,String objectId, String attrName) {
		TreeSet valueList = new TreeSet();
		NamingEnumeration ne = null;
		Attribute at = null;
		try {
			at = getAttribute(ctx,objectId, attrName);
			if (at == null)
				return valueList;
			for (ne = at.getAll(); ne.hasMore(); valueList.add(ne.next()))
				;
		} catch (NamingException nep) {
			throw new ApplicationError("LdapRead: getAttributeValues failed");
		}
		return valueList;
	}

	public boolean doesObjectExist(String name) {
		try {
			return lookup(name) != null;
		} catch (NamingException ne) {
			return false;
		}
	}

	private Object lookup(String name) throws NamingException {
		return ldapCtx.lookup(name);
	}

	public NamingEnumeration getObjects(String base, String filter, SearchControls controls) {
		NamingEnumeration answer = null;
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
		this.parsedObjectId = TextUtil.getFieldName(this.objectId);
		this.parsedAttrName = TextUtil.getFieldName(this.attrName);
	
	}

	@Override
	public int validate(ValidationContext ctx, Service service) {
		int count = super.validate(ctx, service);
		if (this.objectId == null) {
			ctx.addError("objectId is required");
			count++;
		}
		if (this.attrName == null) {
			ctx.addError("attrName is required");
			count++;
		}
		return count;

	}
}
