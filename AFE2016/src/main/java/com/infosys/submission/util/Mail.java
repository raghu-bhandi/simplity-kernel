package com.infosys.submission.util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class Mail  implements LogicInterface{

	public static Configuration cfg;  
	public Value execute(ServiceContext ctx) {		
		Map<String, Object>parameters = new HashMap<String, Object>();
		parameters.put ("submitter", ctx.getTextValue("submitterId"));
		parameters.put ("sponsor", ctx.getTextValue("sponsormailid"));
		parameters.put ("title", ctx.getTextValue("nomination"));
		parameters.put ("status", ctx.getTextValue("status"));
		
		Writer textValue = new OutputStreamWriter(System.out);
		Template template = null;
		try {
			switch(parameters.get("status").toString()){
			case "Submitted":
				template = cfg.getTemplate ("submit.ftlh");
			case "Approved":
				template = cfg.getTemplate ("approve.ftlh");
			case "Rejected":
				template = cfg.getTemplate ("reject.ftlh");
			}
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
