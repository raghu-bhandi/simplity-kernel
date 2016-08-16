/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
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
package org.simplity.kernel.expr;

import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.DynamicSheet;
import org.simplity.kernel.value.InvalidValueException;
import org.simplity.kernel.value.Value;

/**
 * internal class to test expression during development. To be re-factored into
 * a Maven structure and put into test folder
 *
 * @author simplity.org
 *
 */
class TestExpression {
	/**
	 * bad example for reference/testing
	 */
	private static final String[] badExamples = { null, "", " ", "\n\t\r  ",
			"()", "(())", ".a", "_a", "a a", "a$a", "1a", "+1", ".1", "1.1.1",
			"1,1", "'''", "\"\"\"", "/a/", "//", "/12-/", "/2015-13-13/",
			"/2015-12-32", "/2015-02-29/", "/2015.02.01/", "--a", "!?a", "?1",
			"-''", "~12", "!\"a\"", "?foo()", "a ++ b", " a + b - ",
			" + a - b", "(a}", "foo{{}" };

	/**
	 * good examples for reference/testing. "4 & 4" is good for parsing, but
	 * will fail during execution
	 */
	private static final String[] goodExamples = { "a", "a.a", "a1", "a__a",
			"1", "1.005", "1.", "''", "'\"'",
			"\"''' -_!@#$%^&*()_+{}[]|\\:;'<,>.?/\"", "/1960-05-06/",
			"/2016-02-29/", "!a", "?a", "~b", "-a", "-1.234", "foo()",
			"-foo()", "a+b", "a - b", "a--b", "a +-12 * foo(1,a, ( 1 + 2))",
			"(a)", "a <= 12  " };
	/**
	 * expressions that should parse OK but will generate exception during
	 * execution
	 */
	private static final String[] runTimeErrors = { "4 < 'abd'" };

	/**
	 * expressions that should evaluate to a value as in testResults
	 */
	private static final String[] integralExpressions = { "1 + 2", " 1 + -2",
			"-1 + -1", "123456789 - 12345678", " -2 * -3", "2 * 4", "2 * -5",
			"6 / 2", "7/2", "2/5", "7%5", "8%8", " 2 + 3 * 6 - 4 + 4 / 3",
			"(2 + 4) * ( 5 -3) ",
			"(/2015-01-28/-/2016-08-30/) * (/2015-01-31/-/2014-01-31/)" };

	/**
	 * expected result for testExpressions
	 */
	private static final long[] integralResults = { 3, -1, -2, 111111111, 6, 8,
			-10, 3, 3, 0, 2, 0, 17, 12, -580, 12 };

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		int nbrIssues = 0;
		nbrIssues += TestExpression.testInvalids();
		nbrIssues += TestExpression.testValids();
		nbrIssues += TestExpression.testRuntimeErrors();
		nbrIssues += TestExpression.testResults();
		if (nbrIssues == 0) {
			System.out.println("Congratulations!!!");
		} else {
			System.out.println("Oooops! You have to fix " + nbrIssues
					+ " issues.");
		}
	}

	@SuppressWarnings("unused")
	private static int testInvalids() {
		int nbrIssues = 0;
		for (String text : TestExpression.badExamples) {
			try {
				Expression expr = new Expression(text);
				System.out.println("expression : " + text);
				System.out.println("FAILED TO DETECT ISSUES");
				nbrIssues++;
			} catch (InvalidExpressionException e) {
				// System.out.println(e.getMessage());
			}
		}
		return nbrIssues;
	}

	@SuppressWarnings("unused")
	private static int testValids() {
		int nbrIssues = 0;
		for (String text : TestExpression.goodExamples) {
			try {
				Expression expr = new Expression(text);
			} catch (InvalidExpressionException e) {
				System.out.println("expression : " + text);
				System.out.println(e.getMessage());
				nbrIssues++;
			}
		}
		return nbrIssues;
	}

	private static int testRuntimeErrors() {
		int nbrIssues = 0;
		DataSheet data = new DynamicSheet();
		for (String text : TestExpression.runTimeErrors) {
			try {
				Expression expr = new Expression(text);
				expr.evaluate(data);
			} catch (InvalidExpressionException e) {
				System.out.println("expression : " + text);
				System.out.println(e.getMessage());
				nbrIssues++;
			} catch (InvalidOperationException e) {
				// This is what we expect
			}
		}
		return nbrIssues;
	}

	private static int testResults() {
		int nbrIssues = 0;
		DataSheet data = new DynamicSheet();
		for (int i = 0; i < TestExpression.integralExpressions.length; i++) {
			String text = TestExpression.integralExpressions[i];
			long result = TestExpression.integralResults[i];
			String msg = "";
			try {
				Expression expr = new Expression(text);
				Value value = expr.evaluate(data);
				if (value.toInteger() == result) {
					continue;
				}
				msg = "Got " + value + " but we expected " + result
						+ " as result \n" + expr.toTrace();

			} catch (InvalidExpressionException e) {
				msg = e.getMessage();
			} catch (InvalidOperationException e) {
				msg = e.getMessage();
			} catch (InvalidValueException e) {
				msg = e.getMessage();
			}
			System.out.println("expression : " + text);
			System.out.println(msg);
			nbrIssues++;

		}
		return nbrIssues;
	}
}
