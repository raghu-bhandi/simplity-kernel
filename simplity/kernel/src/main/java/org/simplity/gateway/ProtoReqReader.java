/*
 * Copyright (c) 2017 simplity.org
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

package org.simplity.gateway;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.proto.ProtoUtil;
import org.simplity.rest.Operation;
import org.simplity.rest.Operations;
import org.simplity.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

/**
 * request reader for json input
 *
 * @author simplity.org
 *
 */
public class ProtoReqReader implements ReqReader {
	private static final Logger logger = LoggerFactory.getLogger(ReqReader.class);
	/**
	 * payload parsed into a Message object.
	 */
	private final Message inputMessage;

	/**
	 * data from header, query and path
	 */
	private final JSONObject nonBodyData;

	/**
	 * instantiate a translator for the input payload
	 *
	 * @param message
	 * @param data
	 *            that has data from header, query string and path
	 */
	public ProtoReqReader(Message message, JSONObject data) {
		this.inputMessage = message;
		this.nonBodyData = data;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.service.DataTranslator#saveRawInput(org.simplity.service.
	 * ServiceContext, java.lang.String)
	 */
	@Override
	public Object getRawInput() {
		return this.inputMessage;
	}

	private static void throwError() {
		throw new ApplicationError("ProtoReqReader can not be used for pulling data.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.DataTranslator#getValueType(java.lang.String)
	 */
	@Override
	public InputValueType getValueType(String fieldName) {
		throwError();
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#getValueType(int)
	 */
	@Override
	public InputValueType getValueType(int idx) {
		throwError();
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.DataTranslator#getValue(java.lang.String)
	 */
	@Override
	public Object getValue(String fieldName) {
		throwError();
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#openObject(java.lang.String)
	 */
	@Override
	public boolean openObject(String attributeName) {
		throwError();
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#openObject(int)
	 */
	@Override
	public boolean openObject(int idx) {
		throwError();
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#closeObject()
	 */
	@Override
	public boolean closeObject() {
		throwError();
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#openArray(java.lang.String)
	 */
	@Override
	public boolean openArray(String attributeName) {
		throwError();
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#openArray(int)
	 */
	@Override
	public boolean openArray(int zeroBasedIdx) {
		throwError();
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#endArray()
	 */
	@Override
	public boolean closeArray() {
		throwError();
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#getValue(int)
	 */
	@Override
	public Object getValue(int zeroBasedIdx) {
		throwError();
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#getNbrElements()
	 */
	@Override
	public int getNbrElements() {
		throwError();
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#getAttributeNames()
	 */
	@Override
	public String[] getAttributeNames() {
		throwError();
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.ReqReader#readAll(org.simplity.service.
	 * ServiceContext)
	 */
	@Override
	public void pushDataToContext(ServiceContext ctx) {
		/*
		 * header related fields
		 */
		if (this.nonBodyData != null) {
			for (String name : JSONObject.getNames(this.nonBodyData)) {
				ctx.setValue(name, Value.parseObject(this.nonBodyData.get(name)));
			}
		}
		/*
		 * body data
		 */
		if (this.inputMessage == null) {
			return;
		}

		Map<FieldDescriptor, Object> fields = this.inputMessage.getAllFields();
		for (Map.Entry<FieldDescriptor, Object> entry : fields.entrySet()) {
			Object fieldValue = entry.getValue();
			if (fieldValue == null) {
				continue;
			}

			FieldDescriptor fd = entry.getKey();
			String fieldName = fd.getName();

			/*
			 * array
			 */
			if (fd.isRepeated() || fd.getJavaType() == JavaType.MESSAGE) {
				if (fd.getJavaType() == JavaType.MESSAGE) {
					List<FieldDescriptor> childFields = fd.getMessageType().getFields();
					ProtoUtil.extractEmbeddedData(fieldName, fieldValue, childFields, null, ctx);
					continue;
				}
				/*
				 * array of primitive. gets into a sheet with single column
				 */
				DataSheet sheet = ctx.getDataSheet(fieldName);
				Object[] arr = ((Collection<?>) fieldValue).toArray();
				if (sheet != null) {
					ProtoUtil.appendArrayToSheet(arr, sheet);
					continue;
				}

				sheet = ProtoUtil.arrayToSheet(fieldName, arr);
				ctx.putDataSheet(fieldName, sheet);
				continue;
			}
			/*
			 * primitive value
			 */
			ctx.setValue(fieldName, Value.parseObject(fieldValue));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.ReqReader#hasInputSpecs()
	 */
	@Override
	public boolean hasInputSpecs() {
		return true;
	}

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		 * test whether we get the right operation and class name
		 */
		String messageClassPrefix = "org.simplity.apiscdb.ScdbApi$";
		String apiPath = "c:/temp";
		Operations.setProtoClassPrefix(messageClassPrefix);
		Operations.loadAll(apiPath);
		String path = "/scdb/storagecontracts/contract";
		JSONObject headerData = new JSONObject();
		Operation operation = Operations.getOperation(path, "post", headerData);

		if (operation == null) {
			logger.error("No operation found");
			return;
		}
		String schemaName = operation.getBodySchemaName();
		String serviceName = operation.getServiceName();
		logger.info("got serviceName={} and schemaNAme={} for path={}", serviceName, schemaName, path);

		Message message = getSampleMessage(messageClassPrefix + schemaName);
		headerData.put("junk", "junk vakue for test");
		ReqReader reader = new ProtoReqReader(message, headerData);
		Value userId = Value.newTextValue("100");
		ServiceContext ctx = new ServiceContext(serviceName, userId);

		/*
		 * push an empty child sheet to context for roleType
		 */
		String[] namesForRole = {"id", "primaryIndividual","secondaryIndividual"};
		ValueType[] typesForRole = {ValueType.INTEGER, ValueType.TEXT, ValueType.TEXT};
		DataSheet sheet = new MultiRowsSheet(namesForRole, typesForRole);
		ctx.putDataSheet("roles", sheet);

		String[] namesForRoleType = {"roleId", "id", "name"};
		ValueType[] typesForRoleType = {ValueType.INTEGER, ValueType.INTEGER, ValueType.TEXT};
		sheet = new MultiRowsSheet(namesForRoleType, typesForRoleType);
		ctx.putDataSheet("roleType", sheet);
		String[] parentKeys = {"id"};
		String[] childKeys = {"roleId"};
		ctx.addSheetLink("roles", "roleType", parentKeys, childKeys);

		reader.pushDataToContext(ctx);

		logger.info("**** data sheets in context ***");
		for(Map.Entry<String, DataSheet> entry : ctx.getAllSheets()){
			DataSheet ds = entry.getValue();
			logger.info("Sheet {} has {} columns and {} rows", entry.getKey(), ds.width(), ds.length());
		}
		/*
		 * for retrieval simulate data-preparation
		 */
		ctx.prepareChildSheets("roles", "roleType", parentKeys, childKeys);

		RespWriter writer = new ProtoRespWriter(message.newBuilderForType());
		writer.pullDataFromContext(ctx);
		Object obj = writer.getFinalResponseObject();
		logger.info("**************************   final ******************");
		logger.info(obj.toString());

	}


	private static Message getSampleMessage(String className) {
		try {
			Class<?> cls = Class.forName(className);
			Method method = cls.getMethod("newBuilder");
			Builder builder = (Builder) method.invoke(null);
			Message message = ProtoUtil.createSample(builder, 1);
			logger.info(message.toString());
			return message;
		} catch (Exception e) {
			throw new ApplicationError(e, "Error while creating builder. " + e.getMessage());
		}
	}

	/**
	 *
	 * @param fileName
	 */
	private static void createFile(String fileName, Message message) {
		File file = new File(fileName);
		OutputStream stream = null;
		try {
			stream = new FileOutputStream(file);
			stream.write(message.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (Exception e) {
					//
				}
			}
		}
	}

	private static Message loadContracts(String fileName, Builder builder) {
		File file = new File(fileName);
		InputStream stream = null;
		try {
			stream = new FileInputStream(file);
			builder.mergeFrom(stream);
			return builder.build();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (Exception e) {
					//
				}
			}
		}
		return null;
	}

}
