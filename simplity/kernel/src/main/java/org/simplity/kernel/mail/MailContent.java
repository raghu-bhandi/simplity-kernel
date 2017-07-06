package org.simplity.kernel.mail;

public class MailContent {

  MailContentType type;
  String templatePath;
  String template;
  String[] inputSheetName;
  String text;

  public MailContent() {}

  public MailContentType getType() {
    return type;
  }

  public void setType(MailContentType type) {
    this.type = type;
  }

  public String getTemplatePath() {
    return templatePath;
  }

  public void setTemplatePath(String templatePath) {
    this.templatePath = templatePath;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public String[] getInputSheetName() {
    return inputSheetName;
  }

  public void setInputSheetName(String[] inputSheetName) {
    this.inputSheetName = inputSheetName;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
