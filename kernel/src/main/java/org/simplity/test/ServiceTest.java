/*
 * Copyright (c) 2016 simplity.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.test;

import java.util.Date;
import java.util.List;

import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.Component;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.service.JavaAgent;

/**
 * Wrapper around test cases for us to load all test cases for a service. Idea
 * is to have the xml for service to have logic as well as test cases in the
 * same file, but classes are optimized for run-time
 *
 * @author simplity.org
 *
 */
public class ServiceTest implements Component {
	private static final ComponentType MY_TYPE = ComponentType.SERVICE_TEST;
	/**
	 * name
	 */
	String name;
	/**
	 * module name
	 */
	String moduleName;
	/**
	 * test cases for this service
	 */
	TestCase[] testCases;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getSimpleName()
	 */
	@Override
	public String getSimpleName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getQualifiedName()
	 */
	@Override
	public String getQualifiedName() {
		if (this.moduleName == null) {
			return this.name;
		}
		return this.name + '.' + this.moduleName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getReady()
	 */
	@Override
	public void getReady() {
		// this component is not cached for repeated use. No point in
		// optimizing;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.comp.Component#validate(org.simplity.kernel.comp.
	 * ValidationContext)
	 */
	@Override
	public int validate(ValidationContext vtx) {
		vtx.beginValidation(MY_TYPE, this.getQualifiedName());
		int nbr = 0;
		if (this.testCases != null) {
			for (TestCase testCase : this.testCases) {
				nbr += testCase.validate(vtx);
			}
		}
		vtx.endValidation();
		return nbr;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getComponentType()
	 */
	@Override
	public ComponentType getComponentType() {
		return ComponentType.SERVICE_TEST;
	}

	/**
	 * carry out tests
	 *
	 * @param agent
	 * @param results
	 *            to which test results are added
	 * @return number of test cases executed
	 */
	public int test(JavaAgent agent, List<TestResult> results) {
		/*
		 * it is quite possible that this got loaded from a service.xml where
		 * designer has not written any test case
		 */
		if (this.testCases == null) {
			Tracer.trace(this.getQualifiedName()
					+ " has no test cases. Cleared this service with no testing !!");
			return 0;
		}

		String serviceName = this.getQualifiedName();
		for (TestCase testCase : this.testCases) {
			String input = testCase.getInput();
			Date startedAt = new Date();
			String output = agent.serve(serviceName, input);
			int millis = (int) (new Date().getTime() - startedAt.getTime());
			String errorMessage = testCase.compareOutput(output);
			results.add(new TestResult(serviceName, testCase.testCaseName,
					millis, errorMessage));
		}
		return this.testCases.length;
	}
}
