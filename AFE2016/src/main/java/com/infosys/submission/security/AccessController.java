package com.infosys.submission.security;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceInterface;

public class AccessController implements org.simplity.service.AccessController{

	public boolean okToServe(Value userId, String serviceName, ServiceInterface service) {
		if(!serviceName.equals("submission.getallnominations"))
			return true;
		Properties properties = new Properties();
		try {
			properties.load(this.getClass().getResourceAsStream("admin.properties"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(Arrays.asList(properties.getProperty("admin").split(",")).contains(userId.toString().toLowerCase())){
			return true;
		}
		return false;
	}

}
