package com.infosys.qreuse.smtpservice.model;

public class MailAttachement {
	public String filekey;
	public String filename;

	public boolean isEmpty() {
		if (filekey==null || filekey.isEmpty())
			return true;
		return false;
	}
}
