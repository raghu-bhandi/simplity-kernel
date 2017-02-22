package org.simplity.kernel.ldap;

import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.simplity.kernel.ApplicationError;

public class LdapConfig {

    public static final String INITIAL_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    protected String factory;
    protected String ldapurl;
    protected String authentication;
    protected String principal;
    protected String credentials;
    protected String validateDN;
    protected String validateAttrib;
    protected String searchBase;
    protected String name;
    protected boolean requested;
    
    public LdapConfig() {
	}
    
}
