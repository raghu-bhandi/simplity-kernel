package com.infosys.qreuse.LDAPLookup;

public class Employee {

	private String employeeId;
	private String mail;
	private String unit;
	private String employeeName;
	private String designation;
	public String getEmployeeId() {
		return employeeId;
	}
	public void setEmployeeId(String employeeId) {
		this.employeeId = employeeId;
	}
	public String getMail() {
		return mail;
	}
	public void setMail(String mail) {
		this.mail = mail;
	}
	public String getUnit() {
		return unit;
	}
	public void setUnit(String unit) {
		this.unit = unit;
	}
	public String getEmployeeName() {
		return employeeName;
	}
	public void setEmployeeName(String employeeName) {
		this.employeeName = employeeName;
	}
	public String getDesignation() {
		return designation;
	}
	public void setDesignation(String designation) {
		this.designation = designation;
	}
	@Override
	public String toString() {
		return "\"" + employeeId +"\",\"" + mail + "\",\"" + unit + "\",\""
				+ employeeName + "\",\"" + designation + "\"";
	}
	
}
