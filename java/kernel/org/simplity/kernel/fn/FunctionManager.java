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
package org.simplity.kernel.fn;

import java.util.HashMap;
import java.util.Map;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.value.Value;

/**
 * manages functions that are defined for this project, as well as internally
 * defined functions
 *
 * @author simplity.org
 *
 */
public class FunctionManager {
	private static final Map<String, Function> functions = new HashMap<String, Function>();

	/**
	 * use this for run time call where you do not want to handle null
	 *
	 * @param functionName
	 * @return function
	 */
	public static Function getFunction(String functionName) {
		Function fn = FunctionManager.functions.get(functionName);
		if (fn == null) {
			throw new ApplicationError(functionName
					+ " is not defined as a function");
		}
		return fn;
	}

	/**
	 * add a function with
	 *
	 * @param qualifiedFunctionName
	 * @param function
	 */
	public static void defineFunction(String qualifiedFunctionName,
			Function function) {
		FunctionManager.functions.put(qualifiedFunctionName, function);
	}

	/**
	 * evaluate a function and return its value
	 *
	 * @param functionName
	 * @param valueList
	 * @param data
	 * @return value is never null. However value.isNull() could be true
	 */
	public static Value evaluate(String functionName, Value[] valueList,
			FieldsInterface data) {
		return FunctionManager.getFunction(functionName).execute(valueList,
				data);
	}
}
