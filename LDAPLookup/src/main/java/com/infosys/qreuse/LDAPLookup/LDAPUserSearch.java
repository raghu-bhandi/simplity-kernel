package com.infosys.qreuse.LDAPLookup;

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

import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class LDAPUserSearch implements LogicInterface {
	public static final String COMPANY = "company";
	public static final String EXTENSION = "extensionAttribute1";
	public static final String PROJECT_CODE = "extensionAttribute2";
	public static final String DETAILS = "info";
	public static final String MOBILE = "mobile";
	public static final String MAIL = "mail";
	public static final String MAILNICKNAME = "mailNickname"; 
	public static final String CN = "cn"; 
	public static final String DEPARTMENT = "department"; 
	
	public static List<Employee> findUserDetails(String userName) {
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
			System.out.println("success");
			// Create the search controls
			SearchControls searchCtls = new SearchControls();

			// Specify the search scope
			searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);

			// specify the LDAP search filter
			String searchFilter = "(&(objectClass=user)(mail=" + userName + "*@infosys.com))";

			// Specify the Base for the search
			String searchBase = "DC=ad,DC=infosys,DC=com";

			// initialize counter to total the group members
			int totalResults = 0;

			// Specify the attributes to return
			String returnedAtts[] = { "mailNickname","company","department","cn"};
			searchCtls.setReturningAttributes(returnedAtts);

			// Search for objects using the filter
			NamingEnumeration answer = ctx.search(searchBase, searchFilter, searchCtls);

			// Loop through the search results
			while (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();

				System.out.println(">>>" + sr.getName());

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
								} else if (attr.getID().equalsIgnoreCase(EXTENSION)) {
									employee.setExtension(value);

								} else if (attr.getID().equalsIgnoreCase(PROJECT_CODE)) {
									employee.setProjectCode(value);

								} else if (attr.getID().equalsIgnoreCase(DETAILS)) {
									employee.setDetails(value);

								} else if (attr.getID().equalsIgnoreCase(MOBILE)) {
									employee.setMobile(value);
								} else if (attr.getID().equalsIgnoreCase(MAIL)) {
									employee.setDetails(value);
								} else if (attr.getID().equalsIgnoreCase(MAILNICKNAME)) {
									employee.setMail(value);
								} else if (attr.getID().equalsIgnoreCase(CN)) {
									employee.setEmployeeName(value);
								} else if (attr.getID().equalsIgnoreCase(DEPARTMENT)) {
									employee.setUnit(value);
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

		catch (NamingException e) {
			System.err.println("Problem searching directory: " + e);
		}
		return employees;
	}

	public Value execute(ServiceContext ctx) {
		String email = ctx.getTextValue("mailidpart");
		String[] columnNames = { "mail" , "eNo" , "Name" , "Unit"};
		List<Value[]> data = new ArrayList<Value[]>();
		List<Employee> e = findUserDetails(email);
		for (Employee emp : e) {
			Value[] empdetails = new Value[4];
			empdetails[0] = Value.newTextValue(emp.getMail());
			empdetails[1] = Value.newTextValue(emp.getEmployeeId());
			empdetails[2] = Value.newTextValue(emp.getEmployeeName());
			empdetails[3] = Value.newTextValue(emp.getUnit());
			data.add(empdetails);
		}
		DataSheet sheet = new MultiRowsSheet(columnNames, data);
		ctx.putDataSheet("employees", sheet);
		return Value.newBooleanValue(true);
	}

}