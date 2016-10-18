package com.infosys;

import org.simplity.kernel.Application;
import org.simplity.kernel.util.XmlParseException;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceAgent;
import org.simplity.service.ServiceData;

public class JavaApplication {

	private static final String COMP_PATH = "./comp/";

	public static void main(String[] args) {
		try {
			Application.bootStrap(COMP_PATH);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ServiceData outData = new ServiceData();
		ServiceData inData = new ServiceData();
		
		inData.setServiceName("test.test");
		inData.setUserId(Value.newTextValue("100"));
		outData = ServiceAgent.getAgent().executeService(inData);
		System.out.println(outData.getPayLoad());

	}

}
