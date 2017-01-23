package com.infosy.test;

import com.infosys.qreuse.simplity.test.SimpliTestCase;
public class SimplityTest extends SimpliTestCase{

	@Override
	protected void setUp() throws Exception {
		applicationRoot = "D:/Workspace/experiments/SimplityTest/simplitytest/src/main/webapp/WEB-INF/comp/";
		testuser = "100";
		testpwd = "abrakadabra";
		super.setUp();
	}
	
	public void testHello(){
		servicetest = "helloworldtestrun";		
	}
	

}
