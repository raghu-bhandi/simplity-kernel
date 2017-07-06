package org.simplity.kernel.mail;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Properties;

import org.simplity.kernel.Tracer;

public class MailProperties {
  static final Logger logger = Logger.getLogger(MailProperties.class.getName());

  protected String host;
  protected String port;

  private static Properties props;

  public static void initialSetup(MailProperties mailProperties) {

    logger.log(Level.INFO, "Setting up the Mail Agent");
    Tracer.trace("Setting up the Mail Agent");
    props = new Properties();
    props.setProperty("mail.smtp.host", mailProperties.host);
    props.setProperty("mail.smtp.port", mailProperties.port);
  }

  public static Properties getProperties() {
    return props;
  }
}
