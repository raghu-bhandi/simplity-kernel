package com.infosys.qreuse.LDAPLookup;

public class Employee {


	@Override
	public String toString() {
		return "Employee [employeeId=" + employeeId + ", extension=" + extension + ", projectCode=" + projectCode
				+ ", details=" + details + ", mobile=" + mobile + ", mail=" + mail + ", unit=" + unit
				+ ", employeeName=" + employeeName + "]";
	}

	private String employeeId;
	private String extension;
	private String projectCode;
	private String details;
	private String mobile;
	private String mail;
	private String mailNickname;
	private String unit;
	private String employeeName;

	public void setEmployeeId(String value) {
		this.employeeId = value;
	}

	public void setExtension(String value) {
		this.extension = value;
		
	}

	public void setProjectCode(String value) {
		this.projectCode = value;
		
	}

	public void setDetails(String value) {
		this.details = value;		
	}

	public void setMobile(String value) {
		this.mobile = value;
		
	}

	public String getEmployeeId() {
		return employeeId;
	}

	public String getExtension() {
		return extension;
	}

	public String getProjectCode() {
		return projectCode;
	}

	public String getDetails() {
		return details;
	}

	public String getMobile() {
		return mobile;
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

	public String getMailNickname() {
		return mailNickname;
	}

	public void setMailNickname(String mailNickname) {
		this.mailNickname = mailNickname;
	}

	
}
