package com.infosys.qreuse.smtpservice.model;

import javax.mail.Session;

public class Mail {
	public String fromId;
	public 	String toIds;
	public String ccIds;
	public String bccIds;
	public String subject;
	public String content;
	public MailAttachement attachment;
}
