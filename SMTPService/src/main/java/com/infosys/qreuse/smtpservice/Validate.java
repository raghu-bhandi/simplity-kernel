package com.infosys.qreuse.smtpservice;

import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceProtocol;
import org.simplity.tp.LogicInterface;

public class Validate implements LogicInterface {

	public Value execute(ServiceContext ctx) {

		String userId = ctx.getTextValue("_userId");
		String allowedUser = ctx.getTextValue("verifiedUser");

		if (userId.equalsIgnoreCase(allowedUser)) {
			ctx.setTextValue("_userId", userId);
		} else {
			ctx.removeValue("_userId");
		}
		return userId == allowedUser ? Value.newBooleanValue(true) : Value.newBooleanValue(false);
	}

}
