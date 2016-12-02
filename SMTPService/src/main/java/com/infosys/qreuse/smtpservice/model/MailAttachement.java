package com.infosys.qreuse.smtpservice.model;

import java.io.Serializable;

public class MailAttachement implements Serializable{
	
	private static final long serialVersionUID = 8189730674999834850L;
	
	public String filekey;
	public String filename;

	public boolean isEmpty() {
		if (filekey==null || filekey.isEmpty())
			return true;
		return false;
	}
}
