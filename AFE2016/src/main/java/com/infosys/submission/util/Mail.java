package com.infosys.submission.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;


public class Mail  implements LogicInterface{
	
	public Value execute(ServiceContext ctx) {
		Configuration cfg = new Configuration();
		try {
			cfg.setDirectoryForTemplateLoading(new File(this.getClass().getClassLoader().getResource("").getPath()+"/templates"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		
		Map<String, Object>parameters = new HashMap<String, Object>();
		parameters.put ("submitter", ctx.getTextValue("submitterId"));
		parameters.put ("sponsor", ctx.getTextValue("sponsormailid"));
		parameters.put ("title", ctx.getTextValue("nomination"));
		
		Writer textValue = new OutputStreamWriter(System.out);
		try {
			Template template = cfg.getTemplate ("submit.ftlh");
			template.process (parameters,textValue );
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TemplateException e) {
			e.printStackTrace();
		}

		Value mailContents = Value.newTextValue(textValue.toString());
		System.out.println(mailContents);
		return mailContents;
	}

}
