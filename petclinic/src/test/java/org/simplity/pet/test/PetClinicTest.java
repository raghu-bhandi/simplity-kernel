package org.simplity.pet.test;

import java.net.URL;

import org.simplity.pet.action.FilterOwners;

import com.infosys.qreuse.simplity.test.SimpliTestCase;

public class PetClinicTest extends SimpliTestCase{
	@Override
	protected void setUp() throws Exception {
		applicationRoot="D:/Workspace/simplity/petclinic/src/main/webapp/WEB-INF/comp/";
		testuser="100";
		testpwd="pwd";		
		super.setUp();
	}
	
	public void testPetClinic(){
		servicetest="pet";
	}

}
