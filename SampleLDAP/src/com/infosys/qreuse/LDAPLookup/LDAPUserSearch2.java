package com.infosys.qreuse.LDAPLookup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

public class LDAPUserSearch2 {
	public static final String COMPANY = "company";
	public static final String EXTENSION = "extensionAttribute1";
	public static final String PROJECT_CODE = "extensionAttribute2";
	public static final String DETAILS = "info";
	public static final String MOBILE = "mobile";
	public static final String MAIL = "mail";
	public static final String MAILNICKNAME = "mailNickname";
	public static final String CN = "cn";
	public static final String DEPARTMENT = "department";
	public static final String DESIGNATION = "msExchExtensionAttribute16";

	public static List<Employee> findDesignation(String designation) {
		List<Employee> employees = new ArrayList<Employee>();

		Hashtable<String, String> env = new Hashtable<String, String>(11);
		env.put(Context.PROVIDER_URL, "ldap://192.168.200.57:3268/");
		env.put(Context.SECURITY_AUTHENTICATION, "Simple");
		env.put(Context.SECURITY_CREDENTIALS, "Renew@2020");
		env.put(Context.SECURITY_PRINCIPAL, "ENCORE@ITLINFOSYS");
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(LdapContext.CONTROL_FACTORIES, "com.sun.jndi.ldap.ControlFactory");
		try {

			// Create the initial directory context
			LdapContext ctx = new InitialLdapContext(env, null);
			// Create the search controls
			SearchControls searchCtls = new SearchControls();

			// Specify the search scope
			searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// specify the LDAP search filter
			String searchFilter = "(&(objectClass=user)(company=" + designation + "))";

			// Specify the Base for the search
			//String searchBase = "DC=ad,DC=infosys,DC=com";
			String searchBase = "DC=ad,DC=infosys,DC=com";

			// initialize counter to total the group members
			int totalResults = 0;

			// Specify the attributes to return
			String returnedAtts[] = { COMPANY, EXTENSION, PROJECT_CODE, DETAILS, MOBILE, MAIL, CN, DEPARTMENT,
					MAILNICKNAME, DESIGNATION };
			searchCtls.setReturningAttributes(returnedAtts);

			// Search for objects using the filter
			NamingEnumeration answer = ctx.search(searchBase, searchFilter, searchCtls);

			// Loop through the search results
			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				// Print out the groups

				Attributes attrs = sr.getAttributes();
				if (attrs != null) {
					Employee employee = new Employee();
					try {
						for (NamingEnumeration ae = attrs.getAll(); ae.hasMore();) {
							Attribute attr = (Attribute) ae.next();
							if (attr.size() == 1) {
								String value = (String) attr.get(0);
								if (attr.getID().equalsIgnoreCase(COMPANY)) {
									employee.setEmployeeId(value);
									continue;
								}
								if (attr.getID().equalsIgnoreCase(MAIL)) {
									employee.setMail(value);
									continue;
								}
								if (attr.getID().equalsIgnoreCase(CN)) {
									employee.setEmployeeName(value);
									continue;
								}
								if (attr.getID().equalsIgnoreCase(DEPARTMENT)) {
									employee.setUnit(value);
									continue;
								}
								if (attr.getID().equalsIgnoreCase(DESIGNATION)) {
									employee.setDesignation(value);
									continue;
								}

							}

						}
						employees.add(employee);

					} catch (NamingException e) {
						System.err.println("Problem listing membership: " + e);
					}

				}
			}

			// System.out.println("Total groups: " + totalResults);
			ctx.close();

		}

		catch (

		NamingException e) {
			System.err.println("Problem searching directory: " + e);
		}
		return employees;

	}

	public static void main(String[] args) {
//		File fl = new File("D:/workspace/simplity/SampleLDAP/src/list5.txt");
//		File fo = new File("D:/workspace/simplity/SampleLDAP/src/out4.txt");
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(fl));
//			BufferedWriter bw = new BufferedWriter(new FileWriter(fo));
//			String designation;
//			while ((designation = br.readLine()) != null) {	
//				String str = designation + " : " + LDAPUserSearch2.findDesignation(designation);
//				str.replace("\n", "");
//				str = str + "\n";
//				bw.write(str);
//			}
//			bw.flush();
//			bw.close();
//			br.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		 String designation = "16681";
		 System.out.println(LDAPUserSearch2.findDesignation(designation));
	}

}
