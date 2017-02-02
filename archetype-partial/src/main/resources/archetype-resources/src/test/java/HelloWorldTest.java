package com.infosys.qreuse;

import com.infosys.qreuse.simplity.test.SimpliTestCase;
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
