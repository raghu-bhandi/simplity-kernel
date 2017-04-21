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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.smtp.Mail;
import org.simplity.kernel.smtp.MailAttachment;
import org.simplity.kernel.smtp.MailConnector;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

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
	String inlineAttachmentSheetName;

	Content content;

	public SendMail() {
	}

	/**
	 * receives data from service, creates mail object, sets fromId, toIds, ccIds, bccIds, subject
	 * set mail content (either text or template)
	 */
	@Override
	protected Value doAct(ServiceContext ctx) {

		Mail mail = new Mail();
		mail.fromId = fromId;
		mail.toIds = toIds;
		mail.ccIds = ccIds;
		mail.bccIds = bccIds;
		mail.subject = subject;

		Map<String, Object> data = new HashMap<String, Object>();
		
		DataSheet attachmentDataSheet = ctx.getDataSheet(attachmentSheetName);
		if(attachmentDataSheet != null) {
			String[][] rawAttachmentData = attachmentDataSheet.getRawData();
			mail.attachment = new MailAttachment[attachmentDataSheet.length()];
			
			for(int i=0; i < attachmentDataSheet.length(); i++) {
				mail.attachment[i] = new MailAttachment(rawAttachmentData[i+1][0], rawAttachmentData[i+1][1].replace("\\", "/"));
				//mail.attachment[i].name = rawAttachmentData[i+1][0];
				//mail.attachment[i].filepath = rawAttachmentData[i+1][1].replace("\\", "/");
			}
		}
		
		DataSheet inlineAttachmentDataSheet = ctx.getDataSheet(inlineAttachmentSheetName);
		if(inlineAttachmentDataSheet != null) {
			String[][] rawInlineAttachmentData = inlineAttachmentDataSheet.getRawData();
			mail.inlineAttachment = new MailAttachment[inlineAttachmentDataSheet.length()];
			
			for(int i=0; i < inlineAttachmentDataSheet.length(); i++) {
				mail.inlineAttachment[i] = new MailAttachment(rawInlineAttachmentData[i+1][0], rawInlineAttachmentData[i+1][1].replace("\\", "/"));
				//mail.inlineAttachment[i].name = rawInlineAttachmentData[i+1][0];
				//mail.inlineAttachment[i].filepath = rawInlineAttachmentData[i+1][1].replace("\\", "/");
			}
		}
		
		if(content.type.compareTo(ContentType.TEMPLATE) == 0) {
			Configuration templateConfiguration = new Configuration();

			try {

				templateConfiguration.setDirectoryForTemplateLoading(new File(content.templatePath));
				Template template = templateConfiguration.getTemplate(content.template);
				
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

		//sendEmail(mail);
		new MailConnector().sendEmail(mail);
		
		return Value.newBooleanValue(true);

	}

	/**
	 * create MimeMessage and values (fromId, toIds, ccIds, bccIds, subject, content, and attachment) 
	 * and send the object to MailConnector
	 */
/*
	private void sendEmail(Mail mail) {
		Session session = Session.getInstance(SmtpAgent.getProperties(), null);
		try {
			MimeMessage msg = new MimeMessage(session);
			msg.addHeader("Content-type", "text/html; charset=UTF-8");
			msg.addHeader("Content-Transfer-Encoding", "8bit");
			msg.setFrom(new InternetAddress(mail.fromId, "NoReply-JD"));
			msg.setReplyTo(InternetAddress.parse(mail.fromId, false));
			msg.setSubject(mail.subject, "UTF-8");
			msg.setSentDate(new Date());
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mail.toIds, false));
			msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(mail.ccIds, false));
			msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(mail.bccIds, false));
			
			Multipart multipart = new MimeMultipart();
			
			MimeBodyPart bodyPart = new MimeBodyPart();
			bodyPart.setText(mail.content, "US-ASCII", "html");
			multipart.addBodyPart(bodyPart);
			
			if(mail.inlineAttachment != null) {
				MailAttachment[] inlineMailAttachment = mail.inlineAttachment;
				for(int i=0; i < inlineMailAttachment.length; i++) {
					bodyPart = new MimeBodyPart();
					bodyPart.setDisposition(MimeBodyPart.INLINE);
					bodyPart.attachFile(inlineMailAttachment[i].filepath); // attach inline image file
					bodyPart.setHeader("Content-ID", inlineMailAttachment[i].name);
		            multipart.addBodyPart(bodyPart);
				}
			}
			
			if(mail.attachment != null) {
				DataSource dataSource = null;
				MailAttachment[] mailAttachment = mail.attachment;
				for(int i=0; i < mailAttachment.length; i++) {
					bodyPart = new MimeBodyPart();
					dataSource = new FileDataSource(mailAttachment[i].filepath);
					bodyPart.setDataHandler(new DataHandler(dataSource));
					bodyPart.setFileName(mailAttachment[i].name);
		            multipart.addBodyPart(bodyPart);
				}
			}
			
            msg.setContent(multipart);
			msg.writeTo(System.out); // remove this after test
			Transport.send(msg);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}
*/
	private static byte[] serialize(Object obj) throws IOException {

		ByteArrayOutputStream b = new ByteArrayOutputStream();

		ObjectOutputStream o = new ObjectOutputStream(b);
		o.writeObject(obj);

		return b.toByteArray();
	}
}
/*
class Mail implements Serializable {
	private static final long serialVersionUID = -4314888435710523295L;

	public String fromId;
	public String toIds;
	public String ccIds;
	public String bccIds;
	public String subject;
	public String content;
	public MailAttachment[] attachment;
	public MailAttachment[] inlineAttachment;
}

class MailAttachement implements Serializable {

	private static final long serialVersionUID = 8189730674999834850L;
	
	public String name;
	public String filepath;
	
	public MailAttachement() {
		
	}
	
	/**
	 * name - id of the attachment
	 * filepath - complete file name (along with file path)
	 */
/*
	public MailAttachement(String name, String filepath) {
		this.name = name;
		this.filepath = filepath;
	}

}
*/
enum ContentType {
	TEXT, TEMPLATE
}