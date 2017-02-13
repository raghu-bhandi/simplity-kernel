package org.simplity.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.simplity.kernel.Application;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.comp.ValidationResult;
import org.simplity.test.TestContext;
import org.simplity.test.TestRun;

import junit.framework.TestCase;
import junit.framework.TestResult;

public class SimpliTestCase extends TestCase {
	public String applicationRoot;
	public String testuser;
	public String testpwd;
	public String servicetest;

	private TestRun testRun;
	private TestContext ctx = new TestContext();

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		try {
			Application.bootStrap(applicationRoot);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ctx.start(testuser, testpwd);
	}

	@Override
	public void run(TestResult result) {
		super.run(result);
		Method[] methods = this.getClass().getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(this.getName())) {
				try {
					methods[i].invoke(this);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
		}
		StringBuilder errMessage = new StringBuilder();
		errMessage.append(System.getProperty("line.separator"));		
		
		ValidationContext val = new ValidationContext();
		ValidationResult valresult = val.validateAll();
		String[][] messages = valresult.getAllMessages();
		for (int i = 1; i < messages.length; i++) {
			String compType = messages[i][0];
			String compName = messages[i][1];
			String errorMessage = messages[i][2];
			errMessage.append("Semantic error: "+ compType + " " + compName + " failed on validation with message " + errorMessage);
			errMessage.append(System.getProperty("line.separator"));
		}
		
		if ( messages.length > 1) {
			result.addError(this, new Throwable(errMessage.toString()));
			return;
		}
		
		testRun = ComponentManager.getTestRunOrNull(servicetest);
		testRun.run(ctx);
		String[][] report = ctx.getReport();
		
		for (int i = 0; i < report.length; i++) {
			String serviceName = report[i][0];
			String testCaseName = report[i][1];
			String millis = report[i][2];
			String cleared = report[i][3];
			String errorMessage = report[i][4];
			if (cleared.equals("false")) {
				errMessage.append("Test failure: "+testCaseName + " failed with message " + errorMessage);
				errMessage.append(System.getProperty("line.separator"));
			}
		}



		if (ctx.getNbrFailed() > 0) {
			result.addError(this, new Throwable(errMessage.toString()));
			return;
		}

	}

}
