package org.simplity.kernel.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.simplity.kernel.ApplicationError;

public class LdapProperties {
  static final Logger logger = LoggerFactory.getLogger(LdapProperties.class);

  protected static String factory;
  protected static String ldapurl;
  protected static String authentication;
  protected static String principal;
  protected static String credentials;

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

  /**
   * The method is used to authenticating the credentials during login
   *
   * @param principalAuth
   * @param credentialsAuth
   * @return
   */
  public static DirContext getInitialDirContext(String principalAuth, String credentialsAuth) {
    DirContext ctx = null;
    Hashtable env = new Hashtable();
    env.put("java.naming.factory.initial", factory);
    env.put("java.naming.provider.url", ldapurl);
    env.put("java.naming.security.authentication", authentication);
    env.put("java.naming.security.principal", principalAuth);
    env.put("java.naming.security.credentials", credentialsAuth);
    try {
      ctx = new InitialDirContext(env);
    } catch (NamingException e) {

      logger.info("Unable to connect the LDAP, Authentication failed ;" + e.getMessage());
    }
    return ctx;
  }
}
