package com.infosys.qreuse.smtpservice;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.simplity.kernel.file.AttachmentManager;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

import com.infosys.qreuse.smtpservice.model.Mail;
import com.infosys.qreuse.smtpservice.model.MailAttachement;

public class Mailer implements LogicInterface {
	Properties props = new Properties();

	private void sendEmail(Session session,Mail mail) throws MessagingException, UnsupportedEncodingException {
		MimeMessage msg = new MimeMessage(session);
		msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
		msg.addHeader("format", "flowed");
		msg.addHeader("Content-Transfer-Encoding", "8bit");
		msg.setFrom(new InternetAddress(mail.fromId, "NoReply-JD"));
		msg.setReplyTo(InternetAddress.parse(mail.fromId, false));
		msg.setSubject(mail.subject, "UTF-8");
		msg.setSentDate(new Date());
		msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.toIds, false));
		msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(mail.ccIds, false));
		setContent(msg, mail.content, mail.attachment);
		
		try {
			msg.writeTo(System.out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Transport.send(msg);
	}

	private void setContent(MimeMessage msg, String content, MailAttachement attachment) throws MessagingException {
		if (attachment.isEmpty()) {
			msg.setText(content, "UTF-8");
			return;
		}

		// Create the message body part
		BodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setText(content);

		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);
		messageBodyPart = new MimeBodyPart();	
		String tempkey = AttachmentManager.moveFromStorage(attachment.filekey);
		DataSource source = new FileDataSource(FileManager.getTempFile(tempkey).getAbsolutePath());
		
		//cleanup
		AttachmentManager.removeFromStorage(attachment.filekey);
		
		messageBodyPart.setDataHandler(new DataHandler(source));
		messageBodyPart.setFileName(attachment.filename);
		multipart.addBodyPart(messageBodyPart);
		msg.setContent(multipart);
	}

	@Override
	public Value execute(ServiceContext ctx) {
		try {
			props.load(this.getClass().getClassLoader().getResourceAsStream("config.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Mail mail = new Mail();
		mail.fromId = (String)ctx.getObject("fromId");
		mail.toIds = ctx.getTextValue("toIds");
		mail.ccIds = ctx.getTextValue("ccIds");
		mail.bccIds = ctx.getTextValue("bccIds");
		mail.subject = ctx.getTextValue("subject");
		mail.content = ctx.getTextValue("content");
		
		mail.attachment = new MailAttachement();
		mail.attachment.filekey = ctx.getTextValue("filekey");
		mail.attachment.filename = ctx.getTextValue("filename");

		Session session = Session.getInstance(props, null);
		try {
			sendEmail(session, mail);
		} catch (UnsupportedEncodingException | MessagingException e) {
			e.printStackTrace();
		}
		return Value.newBooleanValue(true);
	}

}
