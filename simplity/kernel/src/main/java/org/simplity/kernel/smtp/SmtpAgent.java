package org.simplity.kernel.smtp;

import java.util.Properties;

import org.simplity.kernel.Tracer;

public class SmtpAgent {
	static Properties props;

	public static void initialSetup(SmtpProperties smtpProperties) {
		Tracer.trace("Setting up the SMTP Agent");
		props = new Properties();
		props.setProperty("mail.smtp.host", smtpProperties.host);
		props.setProperty("mail.smtp.port", smtpProperties.port);
	}

	public static Properties getProperties() {
		return props;
	}

}
