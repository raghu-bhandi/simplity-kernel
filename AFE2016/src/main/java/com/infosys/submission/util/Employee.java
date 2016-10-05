package com.infosys.submission.util;

public class Employee {

	@Override
	public String toString() {
		return "Employee [employeeId=" + employeeId + ", extension=" + extension + ", projectCode=" + projectCode
				+ ", details=" + details + ", mobile=" + mobile + "]";
	}

	private String employeeId;
	private String extension;
	private String projectCode;
	private String details;
	private String mobile;

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

}
