package org.simplity.examples.springIntegration;

import org.simplity.utils.SimpliTestCase;

public class HelloWorldTest extends SimpliTestCase{

	@Override
	protected void setUp() throws Exception {
		applicationRoot = Thread.currentThread().getContextClassLoader().getResource("comp").getPath();
		testuser = "100";
		testpwd = "abrakadabra";
		super.setUp();
	}
	
	public void testHello(){		
		servicetest = "helloworldtestrun";	
	}
	

}
