package com.infosys.qreuse.smtpservice;

import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class Validate implements LogicInterface{

	public Value execute(ServiceContext ctx) {
		String userId    = ctx.getTextValue("_userId");
		String userToken = ctx.getTextValue("_userToken");
		
		String allowedUser = ctx.getTextValue("verifiedUser");
		
		return null;
	}

}
