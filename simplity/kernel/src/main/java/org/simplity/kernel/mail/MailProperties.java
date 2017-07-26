package org.simplity.kernel.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class MailProperties {
	private static final Logger logger = LoggerFactory.getLogger(MailProperties.class);

  protected String host;
  protected String port;

  private static Properties props;

  public static void initialSetup(MailProperties mailProperties) {

    logger.info("Setting up the Mail Agent");

    props = new Properties();
    props.setProperty("mail.smtp.host", mailProperties.host);
    props.setProperty("mail.smtp.port", mailProperties.port);
  }

  public static Properties getProperties() {
    return props;
  }
}
