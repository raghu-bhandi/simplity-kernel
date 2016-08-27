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
package org.simplity.tp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.MessageType;
import org.simplity.kernel.Messages;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.data.DataPurpose;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.util.TextUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceInterface;
import org.simplity.service.ServiceProtocol;

/**
 * Transaction Processing Service
 *
 * @author simplity.org
 *
 */
public class Service implements ServiceInterface {

	/*
	 * constants used by on-the-fly services
	 */
	private static final char PREFIX_DELIMITER = '_';
	private static final String GET = "get";
	private static final String FILTER = "filter";
	private static final String SAVE = "save";
	/**
	 * used by suggestion service as well
	 */
	public static final String SUGGEST = "suggest";

	/**
	 * list service needs to use this
	 */
	private static final String LIST = "list";

	/**
	 * stop the execution of this service as success
	 */
	public static final String STOP = "_s";

	/**
	 * field name with which result of an action is available in service context
	 */
	public static final String RESULT_SUFFIX = "Result";

	private static final ComponentType MY_TYPE = ComponentType.SERVICE;

	/**
	 * simple name
	 */
	String name;

	/**
	 * module name.simpleName would be fully qualified name.
	 */
	String moduleName;
	/**
	 * database access type
	 */
	DbAccessType dbAccessType;

	/**
	 * do not parse the request text. Just set it to this field. Service will
	 * take care of that
	 */
	String requestTextFieldName;
	/**
	 * input fields/grids for this service. not valid if requestTextFieldName is
	 * specified
	 */
	InputData inputData;

	/**
	 * copy input records from another service
	 */
	String referredServiceForInput;
	/**
	 * copy output records from another service
	 */
	String referredServiceForOutput;

	/**
	 * use this field as response
	 */
	String responseTextFieldName;

	/**
	 * schema name, different from the default schema, to be used specifically
	 * for this service
	 */
	String schemaName;
	/**
	 * output fields and grids for this service. Not valid if
	 * responseTextFieldName is specified
	 */
	OutputData outputData;

	/**
	 * actions that make up this service
	 */
	Action[] actions;

	/**
	 * should this be executed in the background ALWAYS?.
	 */
	boolean executeInBackground;
	/**
	 * action names indexed to respond to navigation requests
	 */
	private final HashMap<String, Integer> indexedActions = new HashMap<String, Integer>();

	/*
	 * flag to avoid repeated getReady() calls
	 */
	private boolean gotReady;

	@Override
	public DbAccessType getDataAccessType() {
		return this.dbAccessType;
	}

	@Override
	public String getSimpleName() {
		return this.name;
	}

	@Override
	public boolean toBeRunInBackground() {
		return this.executeInBackground;
	}

	@Override
	public boolean okToCache() {
		return true;
	}

	@Override
	public String getQualifiedName() {
		if (this.moduleName == null) {
			return this.name;
		}
		return this.moduleName + '.' + this.name;
	}

	@Override
	public ServiceData respond(ServiceData inData) {
		ServiceContext ctx = new ServiceContext(this.name, inData.getUserId());
		String requestText = inData.getPayLoad();

		if (this.requestTextFieldName != null) {
			ctx.setObject(this.requestTextFieldName, requestText);
			Tracer.trace("Request text is not parsed but set as object value of "
					+ this.requestTextFieldName);
		} else {
			if (this.inputData != null) {
				try {
					if (requestText == null || requestText.length() == 0) {
						Tracer.trace("No input received from client, but we are expecting some input. Simulating an empty input.");
						requestText = "{}";
					}
					JSONObject json = new JSONObject(requestText);
					this.inputData.extractFromJson(json, ctx);
				} catch (Exception e) {
					ctx.addMessage(Messages.INVALID_DATA,
							"Invalid input data format. " + e.getMessage());
				}
			}
		}
		/*
		 * execute service, but only if input is all OK
		 */
		if (ctx.isInError() == false) {
			/*
			 * copy session variables
			 */
			for (String key : inData.getFieldNames()) {
				Object val = inData.get(key);
				if (val instanceof Value) {
					ctx.setValue(key, (Value) val);
				} else {
					ctx.setObject(key, val);
				}
			}
			try {
				ActionBlock worker = new ActionBlock(this.actions,
						this.indexedActions, ctx);
				if (this.dbAccessType == DbAccessType.NONE) {
					worker.act(null);
				} else {
					DbAccessType access = this.dbAccessType;
					/*
					 * get database handle. dbDriver is bit crazy. Does not
					 * return driver, but expects us to supply a client instance
					 * with whom it works.
					 */
					DbDriver.workWithDriver(worker, access, this.schemaName);
				}
			} catch (Exception e) {
				Tracer.trace(
						e,
						"Exception during execution of service. "
								+ e.getMessage());
				ctx.addInternalMessage(MessageType.ERROR, e.getMessage());
			}
		}
		ServiceData response = new ServiceData();
		for (FormattedMessage msg : ctx.getMessages()) {
			response.addMessage(msg);
		}
		/*
		 * create output pay load, but only if service succeeded
		 */
		if (ctx.isInError() == false) {
			if (this.responseTextFieldName != null) {
				/*
				 * service is supposed to have kept response ready for us
				 */
				Object obj = ctx.getObject(this.responseTextFieldName);
				if (obj == null) {
					obj = ctx.getValue(this.responseTextFieldName);
				}
				if (obj == null) {
					Tracer.trace("Service " + this.name
							+ " failed to set field "
							+ this.responseTextFieldName
							+ " to a response text. We will send no response");
				} else {
					response.setPayLoad(obj.toString());
				}
			} else if (this.outputData != null) {
				this.outputData.setResponse(ctx, response);
			} else {
				Tracer.trace("Service " + this.name
						+ " is designed to send no response.");
			}
		}
		return response;
	}

	@Override
	public Value executeAsAction(ServiceContext ctx, DbDriver driver) {
		ActionBlock actionBlock = new ActionBlock(this.actions,
				this.indexedActions, ctx);
		boolean result = actionBlock.workWithDriver(driver);
		if (result) {
			return Value.VALUE_TRUE;
		}
		return Value.VALUE_FALSE;
	}

	@Override
	public void getReady() {
		if (this.gotReady) {
			Tracer.trace("Service "
					+ this.getQualifiedName()
					+ " is being harassed by repeatedly asking it to getReady(). Please look into this..");
			return;
		}
		this.gotReady = true;

		if (this.actions == null) {
			throw new ApplicationError("Service " + this.getQualifiedName()
					+ " has no actions.");
		}
		int i = 0;
		for (Action action : this.actions) {
			action.getReady(i);
			if (this.indexedActions.containsKey(action.actionName)) {
				throw new ApplicationError("Service " + this.name
						+ " has duplicate action name " + action.actionName
						+ " as its action nbr " + (i + 1));
			}
			this.indexedActions.put(action.getName(), new Integer(i));
			i++;
		}
		if (this.requestTextFieldName != null) {
			Tracer.trace("Service "
					+ this.name
					+ " is designed to manage its own input. Request string coming from clinet will be set to field "
					+ this.requestTextFieldName);
			if (this.inputData != null) {
				Tracer.trace("dataInput specification would be ignored because requestTextFieldName is set to "
						+ this.requestTextFieldName);
			}
		} else {
			if (this.inputData == null) {
				Tracer.trace("Service " + this.name
						+ " is designed to take no inputs");
			}
		}
		if (this.responseTextFieldName != null) {
			Tracer.trace("Service "
					+ this.name
					+ " is designed to manage its own output. Response string would be picked up from field "
					+ this.responseTextFieldName);
			if (this.outputData != null) {
				Tracer.trace("output data specification would be ignored as responseTextFieldName is set to "
						+ this.responseTextFieldName);
			}
		} else {
			if (this.outputData == null) {
				Tracer.trace("WARNING : Service "
						+ this.name
						+ " is designed to send no response. This is unusual, but not an error");
			}
		}
		/*
		 * input record may have to be copied form referred service
		 */
		if (this.referredServiceForInput != null) {
			if (this.inputData != null) {
				throw new ApplicationError("Service " + this.getQualifiedName()
						+ " refers to service " + this.referredServiceForInput
						+ " but also specifies its own input records.");
			}
			ServiceInterface service = ComponentManager
					.getService(this.referredServiceForInput);
			if (service instanceof Service == false) {
				throw new ApplicationError("Service " + this.getQualifiedName()
						+ " refers to another service "
						+ this.referredServiceForInput
						+ ", but that is not an xml-based service.");
			}
			this.inputData = ((Service) service).inputData;
		}
		if (this.inputData != null) {
			this.inputData.getReady();
		}
		/*
		 * output record may have to be copied form referred service
		 */
		if (this.referredServiceForOutput != null) {
			if (this.outputData != null) {
				throw new ApplicationError("Service " + this.getQualifiedName()
						+ " refers to service " + this.referredServiceForOutput
						+ " but also specifies its own output records.");
			}
			ServiceInterface service = ComponentManager
					.getService(this.referredServiceForOutput);
			if (service instanceof Service == false) {
				throw new ApplicationError("Service " + this.getQualifiedName()
						+ " refers to another service "
						+ this.referredServiceForOutput
						+ ", but that is not an xml-based service.");
			}
			this.outputData = ((Service) service).outputData;
		}
		if (this.outputData != null) {
			this.outputData.getReady();
		}
	}

	/**
	 * @param serviceName
	 * @param record
	 * @return a service that is designed to read a row for the primary key from
	 *         the table associated with this record
	 */
	public static Service getReadService(String serviceName, Record record) {
		String recordName = record.getQualifiedName();
		Service service = new Service();
		service.dbAccessType = DbAccessType.READ_ONLY;
		service.setName(serviceName);

		/*
		 * what is to be input
		 */
		InputRecord inRec = new InputRecord();
		inRec.recordName = recordName;
		inRec.purpose = DataPurpose.READ;
		InputRecord[] inRecs = { inRec };
		InputData inData = new InputData();
		inData.inputRecords = inRecs;
		service.inputData = inData;

		/*
		 * We have just one action : read action
		 */
		Action action = new Read(record, getChildRecords(record, true));
		Action[] actions = { action };
		service.actions = actions;

		/*
		 * output fields from record.
		 */
		OutputRecord[] outRecs = getOutputRecords(record, false);
		OutputData outData = new OutputData();
		outData.outputRecords = outRecs;
		service.outputData = outData;

		return service;
	}

	/**
	 *
	 * @param serviceName
	 * @param record
	 * @return service that filter rows from table associated with this record,
	 *         and possibly reads related rows from child records
	 */
	public static Service getFilterService(String serviceName, Record record) {
		String recordName = record.getQualifiedName();
		Service service = new Service();
		service.dbAccessType = DbAccessType.READ_ONLY;
		service.setName(serviceName);

		/*
		 * input for filter
		 */
		InputRecord inRec = new InputRecord();
		inRec.recordName = recordName;
		inRec.purpose = DataPurpose.FILTER;
		InputRecord[] inRecs = { inRec };
		InputData inData = new InputData();
		inData.inputRecords = inRecs;
		service.inputData = inData;

		/*
		 * one filter action
		 */
		Action action = new Filter(record, getChildRecords(record, true));
		Action[] actions = { action };
		service.actions = actions;

		/*
		 * output as sheet, possibly child sheets as well
		 */
		OutputRecord[] outRecs = getOutputRecords(record, true);
		OutputData outData = new OutputData();
		outData.outputRecords = outRecs;
		service.outputData = outData;

		/*
		 * getReady() is called by component manager any ways..
		 */
		return service;
	}

	/**
	 *
	 * @param serviceName
	 * @param record
	 * @return service that returns a sheet with suggested rows for teh supplied
	 *         text value
	 */
	public static Service getSuggestionService(String serviceName, Record record) {
		Service service = new Service();
		service.dbAccessType = DbAccessType.READ_ONLY;
		service.setName(serviceName);

		/*
		 * input for suggest
		 */
		InputField f1 = new InputField(record.getSuggestionKeyName(),
				DataType.DEFAULT_TEXT, true, null);
		InputField f2 = new InputField(ServiceProtocol.SUGGEST_STARTING,
				DataType.DEFAULT_BOOLEAN, false, null);

		InputField[] inFields = { f1, f2 };
		InputData inData = new InputData();
		inData.inputFields = inFields;
		service.inputData = inData;

		/*
		 * use a suggest action to do the job
		 */
		Action action = new Suggest(record);
		Action[] actions = { action };
		service.actions = actions;

		/*
		 * output as sheet
		 */
		OutputRecord outRec = new OutputRecord(record.getDefaultSheetName());
		OutputRecord[] outRecs = { outRec };
		OutputData outData = new OutputData();
		outData.outputRecords = outRecs;
		service.outputData = outData;

		/*
		 * getReady() is called by component manager any ways..
		 */
		return service;
	}

	/**
	 *
	 * @param serviceName
	 * @param record
	 * @return service that returns a sheet with suggested rows for teh supplied
	 *         text value
	 */
	public static Service getListService(String serviceName, Record record) {
		Service service = new Service();
		service.dbAccessType = DbAccessType.READ_ONLY;
		service.setName(serviceName);

		/*
		 * input for suggest
		 */
		InputField f1 = new InputField(record.getSuggestionKeyName(),
				DataType.DEFAULT_TEXT, true, null);
		InputField f2 = new InputField(ServiceProtocol.SUGGEST_STARTING,
				DataType.DEFAULT_BOOLEAN, false, null);

		InputField[] inFields = { f1, f2 };
		InputData inData = new InputData();
		inData.inputFields = inFields;
		service.inputData = inData;

		/*
		 * use a suggest action to do the job
		 */
		Action action = new KeyValueList(record);
		Action[] actions = { action };
		service.actions = actions;

		/*
		 * output as sheet
		 */
		OutputRecord outRec = new OutputRecord(record.getDefaultSheetName());
		OutputRecord[] outRecs = { outRec };
		OutputData outData = new OutputData();
		outData.outputRecords = outRecs;
		service.outputData = outData;

		/*
		 * getReady() is called by component manager any ways..
		 */
		return service;
	}

	/**
	 * create a service that would save a rows, possibly along with some child
	 * rows
	 *
	 * @param serviceName
	 * @param record
	 * @return a service that would save a rows, possibly along with some child
	 *         rows
	 */
	public static ServiceInterface getSaveService(String serviceName,
			Record record) {
		String recordName = record.getQualifiedName();
		Service service = new Service();
		service.dbAccessType = DbAccessType.READ_WRITE;
		service.setName(serviceName);

		/*
		 * data for this record is expected in fields, while rows for
		 * child-records in data sheets
		 */
		InputData inData = new InputData();
		inData.inputRecords = getInputRecords(record);
		service.inputData = inData;

		/*
		 * save action
		 */
		Save action = new Save(record, getChildRecords(record, false));
		Action[] actions = { action };
		service.actions = actions;
		/*
		 * we think we have to read back the row, but not suer.. Here the aciton
		 * commented for that
		 */
		// Read action1 = new Read();
		// action1.executeOnCondition = "saveResult != 0";
		// action1.name = "read";
		// action1.recordName = recordName;
		// action1.childRecords = getChildRecords(record, true);
		// Action[] actions = { action, action1 };

		/*
		 * what should we output? We are not sure. As of now let us send back
		 * fields
		 */
		OutputRecord outRec = new OutputRecord();
		outRec.recordName = recordName;
		OutputData outData = new OutputData();
		OutputRecord[] outRecs = { outRec };
		outData.outputRecords = outRecs;
		service.outputData = outData;

		return service;
	}

	/*
	 * check for name and module name based on the requested name
	 */
	private void setName(String possiblyQualifiedName) {
		int idx = possiblyQualifiedName.lastIndexOf('.');
		if (idx == -1) {
			this.name = possiblyQualifiedName;
			this.moduleName = null;
		} else {
			this.name = possiblyQualifiedName.substring(idx + 1);
			this.moduleName = possiblyQualifiedName.substring(0, idx);
		}
		Tracer.trace("service name set to " + this.name + " and "
				+ this.moduleName);
	}

	private static RelatedRecord[] getChildRecords(Record record,
			boolean forRead) {
		String[] children;
		if (forRead) {
			children = record.getChildrenToOutput();
		} else {
			children = record.getChildrenToInput();
		}
		if (children == null) {
			return null;
		}
		RelatedRecord[] recs = new RelatedRecord[children.length];
		int i = 0;
		for (String child : children) {
			RelatedRecord rr = new RelatedRecord();
			rr.recordName = child;
			rr.sheetName = TextUtil.getSimpleName(child);
			rr.getReady();
			recs[i++] = rr;
		}
		return recs;
	}

	private static OutputRecord[] getOutputRecords(Record record,
			boolean forDataSheet) {
		String[] children = record.getChildrenToOutput();
		int nrecs = 1;
		if (children != null) {
			nrecs = children.length + 1;
		}
		OutputRecord[] recs = new OutputRecord[nrecs];
		/*
		 * put this record as the first one for fields (not sheet)
		 */
		OutputRecord outRec = new OutputRecord();
		recs[0] = outRec;
		if (forDataSheet) {
			String sheetName = record.getDefaultSheetName();
			outRec.sheetName = sheetName;
			if (children != null) {
				int i = 1;
				for (String child : children) {
					recs[i++] = ComponentManager.getRecord(child)
							.getOutputRecord(sheetName);
				}
			}
		} else {
			outRec.recordName = record.getQualifiedName();
		}

		return recs;
	}

	/**
	 *
	 * @param record
	 * @return
	 */
	private static InputRecord[] getInputRecords(Record record) {
		String recordName = record.getQualifiedName();
		String[] children = record.getChildrenToInput();
		int nrecs = 1;
		if (children != null) {
			nrecs = children.length + 1;
		}

		InputRecord inRec = new InputRecord();
		inRec.recordName = recordName;
		inRec.purpose = DataPurpose.SAVE;
		inRec.saveActionExpected = true;
		InputRecord[] recs = new InputRecord[nrecs];
		recs[0] = inRec;
		if (children != null) {
			String sheetName = record.getDefaultSheetName();
			int i = 1;
			for (String child : children) {
				recs[i++] = ComponentManager.getRecord(child).getInputRecord(
						sheetName);
			}
		}
		return recs;
	}

	@Override
	public int validate(ValidationContext ctx) {
		int count = 0;
		if (this.actions == null) {
			ctx.addError("No actions.");
		}
		Set<String> addedSoFar = new HashSet<String>();
		int i = 1;
		for (Action action : this.actions) {
			if (addedSoFar.add(action.actionName) == false) {
				ctx.addError("Duplicate action name " + action.actionName
						+ " at " + i);
				count++;
			}
			count += action.validate(ctx, this);
			i++;
		}
		i = 0;
		if (this.requestTextFieldName != null) {
			i++;
		}
		if (this.inputData != null) {
			count += this.inputData.validate(ctx);
			i++;
		}
		if (this.referredServiceForInput != null) {
			i++;
			ServiceInterface service = ComponentManager
					.getServiceOrNull(this.referredServiceForInput);
			if (service == null) {
				ctx.addError("referredServiceForInput set to "
						+ this.referredServiceForInput
						+ " but that service is not defined");
				count++;
			}
		}
		if (i > 1) {
			ctx.addError("More than one input specifications. Use one of dataInput, requestTextFieldName or referredServiceForInput.");
			count++;
		}
		i = 0;
		if (this.responseTextFieldName != null) {
			i++;
		}
		if (this.outputData != null) {
			count += this.outputData.validate(ctx);
			i++;
		}
		if (this.referredServiceForOutput != null) {
			i++;
			ServiceInterface service = ComponentManager
					.getServiceOrNull(this.referredServiceForOutput);
			if (service == null) {
				ctx.addError("referredServiceForOutput set to "
						+ this.referredServiceForOutput
						+ " but that service is not defined");
				count++;
			}
		}
		if (i > 1) {
			ctx.addError("More than one output specifications. Use one of dataOutput, responseTextFieldName or referredServiceForOutput.");
			count++;
		}
		if (this.schemaName != null && DbDriver.getDefaultSchema() == null) {
			ctx.addError("schemaName set to "
					+ this.schemaName
					+ " but this applicaiton is not configured for multiple schemas.");
			count++;
		}
		return count;
	}

	@Override
	public ComponentType getComponentType() {
		return MY_TYPE;
	}

	/**
	 * generate a service on the fly, if possible
	 *
	 * @param serviceName
	 *            that follows on-the-fly-service-name pattern
	 * @return service, or null if name is not a valid on-the-fly service name
	 */
	public static ServiceInterface generateService(String serviceName) {
		Tracer.trace("Trying " + serviceName + " as a fly-by-night operator");
		int idx = serviceName.indexOf(PREFIX_DELIMITER);
		if (idx == -1) {
			return null;
		}

		String operation = serviceName.substring(0, idx);
		String recordName = serviceName.substring(idx + 1);
		/*
		 * once we have established that this name follows our on-the-fly naming
		 * convention, we are quite likely to to get the record.
		 */

		Record record = null;
		try {
			record = (Record) ComponentType.REC.getComponent(recordName);
		} catch (Exception e) {
			Tracer.trace(e, "Error while loading record " + recordName);
		}
		if (record == null) {
			Tracer.trace(recordName
					+ " is not defined as a record, and hence we are unable to generate a servce named "
					+ serviceName);
			return null;
		}
		if (operation.equals(LIST)) {
			return ListService.getService(serviceName, record);
		}

		if (operation.equals(FILTER)) {
			return getFilterService(serviceName, record);
		}
		if (operation.equals(GET)) {
			return getReadService(serviceName, record);
		}
		if (operation.equals(SAVE)) {
			return getSaveService(serviceName, record);
		}
		if (operation.equals(SUGGEST)) {
			return SuggestionService.getService(serviceName, record);
		}

		Tracer.trace("We have no on-the-fly servce generator for operation "
				+ operation);
		return null;

	}
}