package org.simplity.kernel.ldap;

import java.util.Hashtable;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.jms.JmsSetup;

public class LdapAgent {
	private static String factory;
	private static String ldapurl;
	private static String authentication;
	private static String principal;
	private static String credentials;

	public static void initialSetup(LdapConfig ldapConfig) {
		factory = ldapConfig.factory;
		ldapurl = ldapConfig.ldapurl;
		authentication = ldapConfig.authentication;
		principal = ldapConfig.principal;
		credentials = ldapConfig.credentials;
		Tracer.trace("Setting up the LDAP Agent");
	}

	public static DirContext getInitialDirContext(String localPrincipal, String localCredentials)
			throws ApplicationError {
		if (localPrincipal == null) {
			localPrincipal = principal;
			localCredentials = credentials;
		}
		DirContext ctx = null;
		Hashtable env = new Hashtable();
		env.put("java.naming.factory.initial", factory);
		env.put("java.naming.provider.url", ldapurl);
		env.put("java.naming.security.authentication", authentication);
		env.put("java.naming.security.principal", localPrincipal);
		env.put("java.naming.security.credentials", localCredentials);
		try {
			ctx = new InitialDirContext(env);
		} catch (NamingException ne) {
			throw new ApplicationError("LdapFactory Failed to connect to LDAP: " + ldapurl);
		}
		return ctx;
	}
}
