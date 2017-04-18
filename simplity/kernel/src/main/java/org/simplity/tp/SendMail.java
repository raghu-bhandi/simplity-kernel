/*
 * Copyright (c) 2017 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.tp;

/**
 * @author simplity.org
 *
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.smtp.SmtpAgent;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.test.mail.MockTransport;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class SendMail extends Action {

	String fromId;
	String toIds;
	String ccIds;
	String bccIds;
	String subject;
	String attachmentSheetName;

	Content content;

	public SendMail() {
	}

	@Override
	protected Value doAct(ServiceContext ctx) {


		Mail mail = new Mail();
		mail.fromId = fromId;
		mail.toIds = toIds;
		mail.ccIds = ccIds;
		mail.bccIds = bccIds;
		mail.subject = subject;

		if(content.type.compareTo(ContentType.TEMPLATE) == 0) {
			Configuration templateConfiguration = new Configuration();

			try {

				templateConfiguration.setDirectoryForTemplateLoading(new File(content.templatePath));
				Template template = templateConfiguration.getTemplate(content.template);
				
				Map<String, Object> data = new HashMap<String, Object>();

				for(int sheetIndex=0; sheetIndex < content.inputSheetName.length; sheetIndex++) {
					DataSheet dataSheet = ctx.getDataSheet(content.inputSheetName[sheetIndex]);
					
					String[] columnNames = dataSheet.getColumnNames();
					String[][] rawData = dataSheet.getRawData();
					
					if(dataSheet.length() == 1) {
						for(int i=0; i < dataSheet.width(); i++) {
							data.put(columnNames[i], rawData[1][i]);
						}
					} else {
						for(int i=0; i < dataSheet.width(); i++) {
							List<String> rowValues = new ArrayList<String>();
							for(int j=1; j <= dataSheet.length(); j++) {
								rowValues.add(rawData[j][i]);
							}
							data.put(columnNames[i], rowValues);
						}
					}
				}
				
				StringWriter stringWriter = new StringWriter();
				template.process(data, stringWriter);
				
				mail.content = stringWriter.toString();
				stringWriter.flush();
				stringWriter.close();

				ctx.setObject("mail", new ByteArrayInputStream(SendMail.serialize(mail)));
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (TemplateException e) {
				e.printStackTrace();
			} 
		} else if(content.type.compareTo(ContentType.TEXT) == 0) {
			try {
				mail.content = content.text;
				ctx.setObject("mail", new ByteArrayInputStream(SendMail.serialize(mail)));
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
//		DataSheet attachmentDataSheet = ctx.getDataSheet(attachmentSheetName);
//		String[][] rawData = attachmentDataSheet.getRawData();
//		mail.attachment = new MailAttachement[attachmentDataSheet.length()];
//		
//		for(int i=0; i < attachmentDataSheet.length(); i++) {
//			mail.attachment[i] = new MailAttachement();
//			mail.attachment[i].filename = rawData[i+1][0];
//			mail.attachment[i].filepath = rawData[i+1][1];
//		}
//		
		Session session = Session.getInstance(SmtpAgent.getProperties(), null);
		sendEmail(session, mail);

		return Value.newBooleanValue(true);

	}

	private void sendEmail(Session session, Mail mail) {
		try {
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
			msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(mail.bccIds, false));
			//msg.setContent(mail.content, "text/HTML; charset=UTF-8");
			
			Multipart multipart = new MimeMultipart();
			
			BodyPart bodyPart = new MimeBodyPart();
			bodyPart.setContent(mail.content, "text/HTML; charset=UTF-8");
			multipart.addBodyPart(bodyPart);
			
			DataSource dataSource = null;
			
//			for(int i=0; i < mail.attachment.length; i++) {
//				bodyPart = new MimeBodyPart();
//				dataSource = new FileDataSource(mail.attachment[i].filepath);
//				bodyPart.setDataHandler(new DataHandler(dataSource));
//				bodyPart.setFileName(mail.attachment[i].filename);
//	            multipart.addBodyPart(bodyPart);
//			}
			
            msg.setContent(multipart);
			msg.writeTo(System.out);
			MockTransport.send(msg);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}

	private static byte[] serialize(Object obj) throws IOException {

		ByteArrayOutputStream b = new ByteArrayOutputStream();

		ObjectOutputStream o = new ObjectOutputStream(b);
		o.writeObject(obj);

		return b.toByteArray();
	}
}

class Mail implements Serializable {
	private static final long serialVersionUID = -4314888435710523295L;

	public String fromId;
	public String toIds;
	public String ccIds;
	public String bccIds;
	public String subject;
	public String content;
	public MailAttachement[] attachment;
}

class MailAttachement implements Serializable {

	private static final long serialVersionUID = 8189730674999834850L;
	
	public String filename;
	public String filepath;
	
	public boolean isEmpty() {
		if (filename == null || filename.isEmpty())
			return true;
		return false;
	}
}

enum ContentType {
	TEXT, TEMPLATE
}