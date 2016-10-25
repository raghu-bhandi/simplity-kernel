package com.infosys.submission.security;

import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceInterface;

public class AccessController implements org.simplity.service.AccessController{

	public boolean okToServe(Value userId, String serviceName, ServiceInterface service) {
		// TODO Auto-generated method stub
		return false;
	}

}
