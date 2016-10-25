package com.infosys.submission.servlet;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.infosys.submission.util.Mail;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

public class Startup extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	public void init(ServletConfig config) throws ServletException { 		
		Mail.cfg = new Configuration();
		try {	
			Mail.cfg.setDirectoryForTemplateLoading(new File(config.getServletContext().getRealPath("/WEB-INF/templates")));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Mail.cfg.setDefaultEncoding("UTF-8");
		Mail.cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
		super.init();
	}
}
