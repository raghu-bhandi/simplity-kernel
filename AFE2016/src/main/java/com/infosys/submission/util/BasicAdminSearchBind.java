package com.infosys.submission.util;

import java.util.*;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;

/**
 * 
 * <p>
 * Title: BasicJNDISearch
 * </p>
 * 
 * <p>
 * Description: Provides a sample for performing JNDI Searches
 * </p>
 * 
 * @author Jim Willeke
 * @version 1.0
 */
public class BasicAdminSearchBind
{
    public BasicAdminSearchBind(String[] args)
    {
	super();

	try
	{
	    BasicAdminSearchBind.doBasicSearch(args);
	}
	catch (Exception ex)
	{
	    ex.printStackTrace();
	}
    }

    /**
     * 
     * @param stid
     *            String - Standard ID (uid)
     * @throws Exception
     *             -
     */
    public static void doBasicSearch(String[] args) throws Exception
    {
	System.out.println("Performing LDAP Search with:");
	System.out.println("	ldapHostName = " + args[0]);
	System.out.println("	    ldapPort = " + args[1]);
	System.out.println("	      bindDn = " + args[2]);
	System.out.println("	   bindDnPwd = " + args[3]);
	System.out.println("	  searchBase = " + args[4]);
	System.out.println("	      filter = (" + args[5] + "=" + args[6] + ")");
	System.out.println("	      Scope: = SUBTREE_SCOPE");
	// Get the context for the admin account
	String binddn = args[2]+"@ITLINFOSYS";
	DirContext adminCtx = getDirContext(args[0], args[1],binddn, args[3]);
	SearchControls constraints = new SearchControls();
	// Set the Scope of the search
	constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
	// Create the filter from args
	String filter = "(" + args[5] + "=" + args[6] + ")";
	// Search for objects with those matching attributes
	NamingEnumeration<?> answer = adminCtx.search(args[4], filter, constraints);
	//formatResults(answer);
	SearchResult sr = (SearchResult) answer.next();
	String userDN = sr.getNameInNamespace();
	//bind as returned entry
	try
	{
	    DirContext userCtx = bindAsEntry(args[0], args[1],  userDN, "Secret Password");
	    System.out.println("We are now bound as the User: "+ userDN);
	    // we could do something with the userCtx here.
	    userCtx.close();
	}
	catch (Exception e)
	{
	   System.err.println("We failed to make a bind as " + userDN + "\n" + e.getMessage());
	}
	adminCtx.close();
    }

    /**
     * 
     * @param ldapHostName
     * @param ldapPost
     * @param bindDn
     * @param bindDnPwd
     * @return
     * @throws NamingException 
     * @throws Exception
     */
    private static DirContext bindAsEntry(String ldapHostName, String ldapPost, String bindDn, String bindDnPwd) throws NamingException 
    {
	Hashtable<String, String> env = new Hashtable<String, String>(11);
	env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	env.put(Context.PROVIDER_URL, "ldap://" + ldapHostName + ":" + ldapPost);
    env.put(Context.SECURITY_AUTHENTICATION, "Simple");
	env.put(Context.SECURITY_PRINCIPAL, bindDn);
	env.put(Context.SECURITY_CREDENTIALS, bindDnPwd);
	// Create the initial context
	DirContext ctx = new InitialDirContext(env);
	return ctx;
    }

    /**
     * Generic method to obtain a reference to a DirContext
     * 
     * @param ldapHostName
     * @param ldapPost
     * @param bindDn
     * @param bindDnPwd
     */
    public static DirContext getDirContext(String ldapHostName, String ldapPost, String bindDn, String bindDnPwd) throws Exception
    {
	Hashtable<String, String> env = new Hashtable<String, String>(11);
    env.put(Context.PROVIDER_URL, "ldap://192.168.200.57:389/");
    env.put(Context.SECURITY_AUTHENTICATION, "Simple");
    env.put(Context.SECURITY_CREDENTIALS, "Renew@2020");
    env.put(LdapContext.CONTROL_FACTORIES, "com.sun.jndi.ldap.ControlFactory");
    env.put(Context.SECURITY_PRINCIPAL, "ENCORE" );
	
		
	DirContext ctx = new InitialDirContext(env);
	System.out.println("success");
	return ctx;
	
    }

    /*
     * Generic method to format the NamingEnumeration returned from a search.
     */
    public static void formatResults(NamingEnumeration<?> enumer) throws Exception
    {
	int count = 0;
	try
	{
	    while (enumer.hasMore())
	    {
		SearchResult sr = (SearchResult) enumer.next();
		System.out.println("SEARCH RESULT:" + sr.getName());
		formatAttributes(sr.getAttributes());
		System.out.println("====================================================");
		count++;
	    }
	    System.out.println("Search returned " + count + " results");
	}
	catch (NamingException e)
	{
	    e.printStackTrace();
	}
    }

    /*
     * Generic method to format the Attributes .Displays all the multiple values of each Attribute in the Attributes
     */
    public static void formatAttributes(Attributes attrs) throws Exception
    {
	if (attrs == null)
	{
	    System.out.println("This result has no attributes");
	}
	else
	{
	    try
	    {
		for (NamingEnumeration<?> enumer = attrs.getAll(); enumer.hasMore();)
		{
		    Attribute attrib = (Attribute) enumer.next();

		    System.out.println("ATTRIBUTE :" + attrib.getID());
		    for (NamingEnumeration<?> e = attrib.getAll(); e.hasMore();)
		    {
			Object value = e.next();
			boolean canPrint = isAsciiPrintable(value);
			if (canPrint)
			{
			    System.out.println("\t\t        = " + value);
			}
			else
			{
			    System.out.println("\t\t        = <-value is not printable->");
			}
		    }
		}
	    }
	    catch (NamingException e)
	    {
		e.printStackTrace();
	    }
	}
    }

    /**
     * Check to see if this Object can be printed.
     * 
     * @param obj
     * @return
     */
    public static boolean isAsciiPrintable(Object obj)
    {
	String str = null;
	try
	{
	    str = (String) obj;
	}
	catch (Exception e)
	{
	    return false;
	    // TODO Auto-generated catch block e.printStackTrace();
	}
	if (str == null)
	{
	    return false;
	}
	int sz = str.length();
	for (int i = 0; i < sz; i++)
	{
	    if (isAsciiPrintable(str.charAt(i)) == false)
	    {
		return false;
	    }
	}
	return true;
    }

    /**
     * Used by isAsciiPrintable(Object obj)
     * 
     * @param ch
     * @return
     */
    public static boolean isAsciiPrintable(char ch)
    {
	return ch >= 32 && ch < 127;
    }

    /**
     * Does a simple search on the LDAP Directory
     * 
     * String ldapHostName = args[0]; String ldapPort = args[1]; String bindDn = args[2]; String bindDnPwd = args[3]; String searchBase = args[4]; // String searchScope=args[4]; String searchAttribute = args[5];
     * String searchAttributeValue = args[6];
     * 
     * @param args
     * 
     */
    public static void main(String[] args)
    {
	if (args.length == 7)
	{
	    BasicAdminSearchBind basicjndisearch = new BasicAdminSearchBind(args);
	}
	else
	{
	    System.out.println("\nYou must provide ldapHostName, ldapPort, bindDn, bindDnPwd, searchBase, searchAttribute and searchAttributeValue on the command line!\n");
	}
    }
}