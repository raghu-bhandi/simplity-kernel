package org.simplity.kernel.ldap;

import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;

public class LdapAgent {
	private static String factory;
	private static String ldapurl;
	private static String authentication;
	private static String principal;
	private static String credentials;

	public static void initialSetup(LdapProperties ldapProperties) {
		Tracer.trace("Setting up the LDAP Agent");

		factory = ldapProperties.factory;
		ldapurl = ldapProperties.ldapurl;
		authentication = ldapProperties.authentication;
		principal = ldapProperties.principal;
		credentials = ldapProperties.credentials;
	}

	public static DirContext getInitialDirContext() {
		DirContext ctx = null;
		Hashtable env = new Hashtable();
		env.put("java.naming.factory.initial", factory);
		env.put("java.naming.provider.url", ldapurl);
		env.put("java.naming.security.authentication", authentication);
		env.put("java.naming.security.principal", principal);
		env.put("java.naming.security.credentials", credentials);
		try {
			ctx = new InitialDirContext(env);
		} catch (NamingException ne) {
			throw new ApplicationError("LdapFactory Failed to connect to LDAP: " + ldapurl);
		}
		return ctx;
	}

	public static DirContext getInitialDirContext(String principalAuth,String credentialsAuth) {
		DirContext ctx = null;
		Hashtable env = new Hashtable();
		env.put("java.naming.factory.initial", factory);
		env.put("java.naming.provider.url", ldapurl);
		env.put("java.naming.security.authentication", authentication);
		env.put("java.naming.security.principal", principalAuth);
		env.put("java.naming.security.credentials", credentialsAuth);
		try {
			ctx = new InitialDirContext(env);
		} catch (NamingException ne) {	
			
			ne.printStackTrace();
		}
		return ctx;
	}
}
