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

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.ldap.LdapAgent;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * @author simplity.org
 *
 */
public class LdapLookup extends Action {
	DirContext ldapCtx;

	@Override
	protected Value doAct(ServiceContext ctx) {
		Hashtable<String, String> env = new Hashtable<String, String>(11);
		try {
			// Create initial context
			DirContext ldapCtx = LdapAgent.getInitialDirContext();

			
			// Close the context when we're done
			ldapCtx.close();
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return Value.VALUE_TRUE;
	}

	public Attributes getAllAttributes(String name) throws NamingException {
		return ldapCtx.getAttributes(name);
	}
	
    public NamingEnumeration getAttributes(String userId)
        {
            Attributes attrs = null;
            NamingEnumeration neAttrs = null;
            try
            {
                attrs = ldapCtx.getAttributes(userId);
                neAttrs = attrs.getAll();
            }
            catch(NamingException ne)
            {
                throw new ApplicationError("LdapRead: Problem getting Attributes; "+ ne.getMessage());
            }
            return neAttrs;
        }	
    public NamingEnumeration getAttributes(String userId, String attrIDs[])
        {
            Attributes attrs = null;
            NamingEnumeration neAttrs = null;
            try
            {
                attrs = ldapCtx.getAttributes(userId, attrIDs);
                neAttrs = attrs.getAll();
            }
            catch(NamingException ne)
            {
                throw new ApplicationError("LdapRead: Problem getting Attributes; "+ ne.getMessage());
            }
            return neAttrs;
        }    
    public Object getSingleValueOfAttribute(Attribute a)
        {
            Object attrValue;
            try
            {
                attrValue = a.get();
            }
            catch(Exception npe)
            {
                throw new ApplicationError("LdapRead: Problem getting Attribute; "+ npe.getMessage());
            }
            return attrValue;
        }    
    
    public Object getSingleValueOfAttribute(String userId, String attrName)
        {
            Attributes attrs = null;
            Object attrValue = null;
            Attribute a = null;
            try
            {
                attrs = ldapCtx.getAttributes(userId);
                a = attrs.get(attrName);
                attrValue = getSingleValueOfAttribute(a);
            }
            catch(Exception npe)
            {
                throw new ApplicationError("LdapRead: Problem getting Attribute"+ npe.getMessage());
            }
            return attrValue;
        }    
    
    public Attribute getAttribute(String objectId, String attrName)
        {
            Attributes attrs = null;
            Attribute a = null;
            try
            {
                attrs = ldapCtx.getAttributes(objectId);
            }
            catch(NamingException ne)
            {
                throw new ApplicationError("LdapRead: Object " + objectId + " does not exist ");
            }
            try
            {
                a = attrs.get(attrName);
            }
            catch(NullPointerException npe)
            {
                throw new ApplicationError("LdapRead:  Attribute " + attrName + " is not set ");
            }
            return a;
        }

        public Attribute getAttribute(Attributes attributes, String attrName)
        {
            Attribute a = null;
            try
            {
                a = attributes.get(attrName);
            }
            catch(NullPointerException npe)
            {
                throw new ApplicationError("LdapRead:  Attribute " + attrName + " is not set ");
            }
            return a;
        }

        public Collection getAttributeValues(String objectId, String attrName)
        {
            TreeSet valueList = new TreeSet();
            NamingEnumeration ne = null;
            Attribute at = null;
            try
            {
                at = getAttribute(objectId, attrName);
                if(at == null)
                    return valueList;
                for(ne = at.getAll(); ne.hasMore(); valueList.add(ne.next()));
            }
            catch(NamingException nep)
            {
                throw new ApplicationError("LdapRead: getAttributeValues failed");
            }
            return valueList;
        }

        public boolean doesObjectExist(String name)
        {
            try
            {
                return lookup(name) != null;
            }
            catch(NamingException ne)
            {
                return false;
            }
        }


        private Object lookup(String name)
            throws NamingException
        {
            return ldapCtx.lookup(name);
        }

        public NamingEnumeration getObjects(String base, String filter, SearchControls controls)
        {
            NamingEnumeration answer = null;
            try
            {
                answer = ldapCtx.search(base, filter, controls);
            }
            catch(NamingException ne)
            {
                throw new ApplicationError("LdapRead : NamingException"+ ne.getMessage());
            }
            return answer;
        }    
}
