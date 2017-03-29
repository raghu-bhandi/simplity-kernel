package org.simplity.examples.springIntegration;

import org.simplity.utils.SimpliTestCase;

public class HelloWorldTest extends SimpliTestCase{

	@Override
	protected void setUp() throws Exception {
		applicationRoot = "path-to-comp/WEB-INF/comp/";
		testuser = "100";
		testpwd = "abrakadabra";
		super.setUp();
	}
	
	public void testHello(){		
		servicetest = "helloworldtestrun";	
		
	}
	

}
