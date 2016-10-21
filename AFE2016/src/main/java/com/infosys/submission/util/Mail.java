package com.infosys.submission.util;

import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class Mail  implements LogicInterface{
	
	public Value execute(ServiceContext ctx) {
		String submitter = ctx.getTextValue("submitterId");
		String sponsor = ctx.getTextValue("sponsormailid");
		String title = ctx.getTextValue("nomination");
		
		System.out.println("Mailed contents");
		return null;
	}

}
