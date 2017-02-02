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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.kernel.comp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simplity.kernel.Application;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Message;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.db.Sql;
import org.simplity.kernel.db.StoredProcedure;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.file.FileManager;
import org.simplity.kernel.util.XmlParseException;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.service.ServiceInterface;
import org.simplity.tp.Service;

/**
 * @author simplity.org
 *
 */
public class ValidationContext {
	private static String[] MSG_HDR = { "compType", "compName",
			"errorMessage" };
	private static String[] REF_HDR = { "compType", "compName", "refType",
			"refName" };
	private static String[] COMP_HDR = { "compType", "compName", "nbrErrors" };

	/**
	 * accumulated component names.
	 */
	private List<String[]> allComps = new ArrayList<String[]>();

	/**
	 * accumulated messages.
	 */
	private List<String[]> allMessages = new ArrayList<String[]>();
	/**
	 * accumulated references.
	 */
	private List<String[]> allRefs = new ArrayList<String[]>();

	/**
	 * state : name of component that has begun validating
	 */
	private String currentCompName;
	/**
	 * state : componentType being validated
	 */
	private String currentType;

	/**
	 * references for the current component being validated. we use a set to
	 * avoid duplicate references
	 */
	private Set<String> references = new HashSet<String>();

	/**
	 * number of errors for the current component
	 */
	private int currentErrors = 0;

	/**
	 *
	 */
	public ValidationContext() {
		/*
		 * push header rows
		 */
		this.allMessages.add(MSG_HDR);
		this.allRefs.add(REF_HDR);
		this.allComps.add(COMP_HDR);
	}

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
			throw new ApplicationError("beginValidation() invoked for "
					+ compName + " before endValidation() is invoked for "
					+ this.currentCompName);
		}
		this.currentCompName = compName;
		this.currentType = compType.toString();
	}

	/**
	 * mark end of validation for the current component.
	 */
	public void endValidation() {
		if (this.currentType == null) {
			throw new ApplicationError(
					"endValidation() invoked on ValidationContext with no startValidation()");
		}

		if (this.references.size() > 0) {
			for (String txt : this.references) {
				String[] parts = txt.split(" ");
				String[] row = { this.currentType, this.currentCompName,
						parts[0], parts[1] };
				this.allRefs.add(row);
			}
			this.references.clear();
		}
		String[] row = { this.currentType, this.currentCompName,
				this.currentErrors + "" };
		this.allComps.add(row);
		this.currentErrors = 0;
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
		String[] row = { this.currentType, this.currentCompName, error };
		this.allMessages.add(row);
		this.currentErrors++;
	}

	/**
	 *
	 * @param refType
	 * @param refName
	 */
	public void addReference(ComponentType refType, String refName) {
		if (this.currentType == null) {
			throw new ApplicationError(
					"Reference being added without a call to startValidation(). "
							+ refType + " : " + refName);
		}
		this.references.add(refType + " " + refName);
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
				this.addError(
						className + " should implement " + klass.getName());
				return 1;
			}
		} catch (Exception e) {
			this.addError(className + " is not defined as a java class.");
			return 1;
		}

		return 0;
	}

	/**
	 * check existence of a class and whether it implements the right interface
	 *
	 * @param fieldName
	 * @param fieldValue
	 * @return 0 if all OK, 1 if an error got added
	 */
	public int checkMandatoryField(String fieldName, Object fieldValue) {
		if (fieldValue == null) {
			this.addError(fieldName + " is a required field.");
			return 1;
		}
		return 0;
	}

	/**
	 * is this service in error? If so add a message and return true.
	 *
	 * @param serviceName
	 *            name of service
	 * @param attName
	 *            attribute that uses this service
	 * @return true if service is in error. false otherwise
	 */
	public boolean checkServiceName(String serviceName, String attName) {
		if (serviceName == null) {
			return false;
		}
		this.addReference(ComponentType.SERVICE, serviceName);
		ServiceInterface service = ComponentManager
				.getServiceOrNull(serviceName);
		if (service == null) {
			this.addError(attName + " is set to" + serviceName
					+ " but it is not a valid service name.");
			return true;
		}
		return false;
	}

	/**
	 *
	 * @return all references
	 */
	public String[][] getReferences() {
		return this.allRefs.toArray(new String[0][]);
	}

	/**
	 *
	 * @return all messages
	 */
	public String[][] getMessages() {
		return this.allMessages.toArray(new String[0][]);
	}

	/**
	 *
	 * @return all components that were validated
	 */
	public String[][] getComps() {
		return this.allComps.toArray(new String[0][]);
	}

	/**
	 * validate all components
	 *
	 * @return a data structure that has the result of validation of all
	 *         components
	 */
	public ValidationResult validateAll() {
		String compFolder = ComponentType.getComponentFolder();
		String appFileName = compFolder + Application.CONFIG_FILE_NAME;
		Application app = new Application();
		try {
			XmlUtil.xmlToObject(appFileName, app);
		} catch (XmlParseException e) {
			this.addError("Application parameter file " + appFileName
					+ " has syntax errors :\n" + e.getMessage());
		}
		app.validate(this);
		this.validateGroup(compFolder + "msg/", Message.class);
		this.validateGroup(compFolder + "dt/", DataType.class);
		/*
		 * let us not worry about function at this time
		 */
		this.validateComps(compFolder + "rec/", Record.class);
		this.validateComps(compFolder + "sql/", Sql.class);
		this.validateComps(compFolder + "proc/", StoredProcedure.class);
		this.validateComps(compFolder + "service/tp/", Service.class);

		String[][] empty = new String[0][];
		return new ValidationResult(this.allMessages.toArray(empty),
				this.allComps.toArray(empty), this.allRefs.toArray(empty));
	}

	private void validateComps(String folder, Class<? extends Component> cls) {
		for (String file : FileManager.getResources(folder)) {
			if (file.endsWith(".xml") == false) {
				Tracer.trace("Skipping Non-resource " + file);
				continue;
			}
			Component comp;
			try {
				comp = cls.newInstance();
			} catch (Exception e) {
				this.addError("Unable to get new instance for component "
						+ cls.getName()
						+ ". Abandoning validation of components of this type");
				return;
			}
			try {
				XmlUtil.xmlToObject(file, comp);
			} catch (Exception e) {
				this.addError("Resource " + file + " failed to load. "
						+ e.getMessage());
				continue;
			}
			comp.validate(this);
		}
	}

	private void validateGroup(String folder, Class<?> cls) {
		Map<String, Object> comps = new HashMap<String, Object>();
		for (String file : FileManager.getResources(folder)) {
			if (file.endsWith(".xml") == false) {
				Tracer.trace("Skipping Non-resource " + file);
				continue;
			}
			Tracer.trace("Going to load components from " + file
					+ " for validation");
			try {
				XmlUtil.xmlToCollection(file, comps,
						cls.getPackage().getName() + '.');
			} catch (Exception e) {
				this.addError("Resource " + file + " failed to load. "
						+ e.getMessage());
				continue;
			}
		}
		for (Map.Entry<String, Object> entry : comps.entrySet()) {
			Object obj = entry.getValue();
			if (obj instanceof Component) {
				((Component) obj).validate(this);
			} else {
				this.addError("Component " + entry.getKey()
						+ " turned out to be a " + obj.getClass().getName()
						+ " that does not implement org.simplity.comp.Component");
			}
		}
	}

}
