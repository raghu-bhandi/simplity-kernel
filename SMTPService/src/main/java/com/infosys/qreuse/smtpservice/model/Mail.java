package com.infosys.qreuse.smtpservice.model;

import java.io.Serializable;

public class Mail implements Serializable{
	private static final long serialVersionUID = -4314888435710523295L;
	
	public String fromId;
	public 	String toIds;
	public String ccIds;
	public String bccIds;
	public String subject;
	public String content;
	public MailAttachement attachment;
}
