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

package org.simplity.kernel.comp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.dt.DataType;

/**
 * @author simplity.org
 *
 */
public class ValidationContext {

	@SuppressWarnings("unchecked")
	private Map<String, String[]>[] allMessages = (Map<String, String[]>[]) (new Object[ComponentType
	                                                                                    .values().length]);
	@SuppressWarnings("unchecked")
	private List<ReferredComponent>[][] allRefs = (List<ReferredComponent>[][]) (new Object[ComponentType
	                                                                                        .values().length][]);
	private ComponentType currentType;
	private String currentCompName;
	private List<String> messages = new ArrayList<String>();
	private List<ReferredComponent>[] refs = null;

	/**
	 * start validating a component. This is not recursive. validation is a
	 * sequential process. Validation for one component must be completed (by
	 * calling endValidation()) before starting the next one.
	 *
	 * @param compType
	 * @param compName
	 */
	public void beginValidation(ComponentType compType, String compName) {
		if (this.currentType != null) {
			throw new ApplicationError("startValidaiton() invoked for "
					+ compName + " before endValidation() is invoked for "
					+ this.currentCompName);
		}
		this.currentCompName = compName;
		this.currentType = compType;
		this.refs = this.allRefs[compType.getIdx()];
	}

	/**
	 * mark end of validation for the current component.
	 */
	public void endValidation() {
		if (this.currentType == null) {
			throw new ApplicationError(
					"endValidation() invoked on ValidationContext with no startValidation()");
		}
		if (this.messages.size() > 0) {
			int idx = this.currentType.getIdx();
			Map<String, String[]> msgs = this.allMessages[idx];
			if (msgs == null) {
				msgs = new HashMap<String, String[]>();
				this.allMessages[idx] = msgs;
			}
			msgs.put(this.currentCompName, this.messages.toArray(new String[0]));
			this.messages.clear();
		}

		this.currentCompName = null;
		this.currentType = null;
	}

	/**
	 * add an error message
	 *
	 * @param error
	 */
	public void addError(String error) {
		if (this.currentType == null) {
			throw new ApplicationError(
					"Error message being added without a call to startValidation(). "
							+ error);
		}
		this.messages.add(error);
	}

	/**
	 *
	 * @param refType
	 * @param refName
	 */
	public void addReference(ComponentType refType, String refName) {
		if (this.currentType == null) {
			throw new ApplicationError(
					"Referrence being added without a call to startValidation(). "
							+ refType + " : " + refName);
		}
		int idx = refType.getIdx();
		List<ReferredComponent> refList = this.refs[idx];
		if (refList == null) {
			refList = new ArrayList<ReferredComponent>();
			this.refs[idx] = refList;
		}
		refList.add(new ReferredComponent(refType, refName));
	}

	/**
	 * Many components refer to record. Here is a short-cut for them. Add this
	 * record as reference, and add error in case it i not defined
	 *
	 * @param recName
	 * @param fieldName
	 * @param isRequired
	 *
	 * @return 0 if all ok, 1 if an error got added
	 */
	public int checkRecordExistence(String recName, String fieldName,
			boolean isRequired) {
		if (recName == null) {
			if (isRequired) {
				this.addError(fieldName + " requires a valid record name");
				return 1;
			}
			return 0;
		}
		this.addReference(ComponentType.REC, recName);
		try {
			Record rec = ComponentManager.getRecordOrNull(recName);
			if (rec == null) {
				this.addError(fieldName + " is set to " + recName
						+ " but that record is not defined.");
				return 1;
			}
		} catch (Exception e) {
			// means it is defined, but has some errors
		}
		return 0;
	}

	/**
	 * Many components refer to data type. Here is a short-cut for them. Add
	 * this data type as reference, and add error in case it i not defined
	 *
	 * @param dataTypeName
	 * @param fieldName
	 * @param isRequired
	 *
	 * @return 0 if all ok, 1 if an error got added
	 */
	public int checkDtExistence(String dataTypeName, String fieldName,
			boolean isRequired) {
		if (dataTypeName == null) {
			if (isRequired) {
				this.addError(fieldName + " requires a valid data type");
				return 1;
			}
			return 0;
		}
		this.addReference(ComponentType.DT, dataTypeName);
		try {
			DataType dt = ComponentManager.getDataTypeOrNull(dataTypeName);
			if (dt == null) {
				this.addError(fieldName + " is set to " + dataTypeName
						+ " but that data type is not defined.");
				return 1;
			}
		} catch (Exception e) {
			// means it is defined, but has some errors
		}
		return 0;
	}

	/**
	 * check existence of a class and whether it implements the right interface
	 *
	 * @param className
	 * @param klass
	 * @return 0 if all OK, 1 if an error got added
	 */
	public int checkClassName(String className, Class<?> klass) {
		try {
			Class<?> cls = Class.forName(className);
			if (cls.isAssignableFrom(klass) == false) {
				this.addError(className + " should implement "
						+ klass.getName());
				return 1;
			}
		} catch (Exception e) {
			this.addError(className + " is not defined as a java class.");
			return 1;
		}

		return 0;
	}

}
