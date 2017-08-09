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
import java.util.Map;

import org.simplity.gateway.TtTroubleTicket.TroubleTicket.TroubleTicket_Statu;
import org.simplity.json.JSONObject;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;

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
	 * @param data that has data from header, query string and path
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
	public void readAsPerSpec(ServiceContext ctx) {
		/*
		 * header related fields
		 */
		if(this.nonBodyData != null){
			for(String name: JSONObject.getNames(this.nonBodyData)){
				ctx.setValue(name, Value.parseObject(this.nonBodyData.get(name)));
			}
		}
		/*
		 * body data
		 */
		if(this.inputMessage == null){
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
			if (fd.isRepeated()) {
				DataSheet sheet = null;
				if (fd.getJavaType() == JavaType.MESSAGE) {
					Message[] messages = ((Collection<?>) fieldValue).toArray(new Message[0]);
					sheet = getDataSheet(messages);
				} else {
					sheet = arrayToSheet(fieldName, ((Collection<?>) fieldValue).toArray());
				}
				if (sheet != null) {
					ctx.putDataSheet(fieldName, sheet);
					logger.info("Table " + fieldName + " extracted with " + sheet.length() + " rows");
				}
				continue;
			}

			/*
			 * embedded object
			 */
			if (fd.getJavaType() == JavaType.MESSAGE) {
				DataSheet sheet = messageToSheet((Message) fieldValue);
				if (sheet != null) {
					ctx.putDataSheet(fieldName, sheet);
					logger.info("Object " + fieldName + " extracted as a single-row data sheet.");
				}
				continue;
			}
			/*
			 * primitive value
			 */
			ctx.setValue(fieldName, Value.parseObject(fieldValue));
		}

	}

	/**
	 * create a data sheet for an array of primitive values.
	 *
	 * @param fieldName
	 * @param arr
	 * @return data sheet with one column
	 */
	private static DataSheet arrayToSheet(String fieldName, Object[] arr) {
		String[] names = { fieldName };
		Value[][] values = new Value[arr.length][1];
		for (int i = 0; i < arr.length; i++) {
			values[i][0] = Value.parseObject(arr[i]);
		}
		return new MultiRowsSheet(names, values);
	}

	/**
	 * @param value
	 * @return
	 */
	private static DataSheet messageToSheet(Message message) {
		/*
		 * create a sheet with one row of data. We need names and types to
		 * create data sheet, and in the process we also get first data row
		 */
		Map<FieldDescriptor, Object> fields = message.getAllFields();
		int nbr = fields.size();
		String[] names = new String[nbr];
		ValueType[] types = new ValueType[nbr];
		Value[] firstRow = new Value[nbr];

		int col = 0;
		for (Map.Entry<FieldDescriptor, Object> entry : fields.entrySet()) {
			FieldDescriptor field = entry.getKey();
			if (field.isRepeated() || field.getType() == Type.MESSAGE) {
				throw new ApplicationError(
						"We have not built features to accept arbitrary object structure, Only one level of child array/message is implemented");
			}

			names[col] = field.getName();
			Object fieldValue = entry.getValue();
			Value value = Value.parseObject(fieldValue);
			firstRow[col] = value;
			types[col] = value.getValueType();
			col++;
		}
		/*
		 *
		 */
		DataSheet ds = new MultiRowsSheet(names, types);
		ds.addRow(firstRow);
		return ds;
	}

	/**
	 * create data sheet for an array of messages
	 *
	 * @param messages
	 * @return
	 */
	private static DataSheet getDataSheet(Message[] messages) {
		Message message = messages[0];
		if (message == null) {
			logger.info("array has its first object as null, and hence we abandoned parsing it.");
			return null;
		}
		DataSheet ds = messageToSheet(message);
		String[] names = ds.getColumnNames();
		int nbrCols = names.length;
		/*
		 * first row was already added. start with i = 1;
		 */
		for (int i = 1; i < messages.length; i++) {
			message = messages[i];
			if (message == null) {
				logger.info("Row " + (i + 1) + " is null. Not extracted");
				continue;
			}
			Value[] row = new Value[nbrCols];
			int j = 0;
			/*
			 * getAllFields() guarantees that the fields are always in the same
			 * order. Hence row will have the right columns
			 */
			for (Object fieldValue : message.getAllFields().values()) {
				if (j == nbrCols) {
					throw new ApplicationError(
							"ProtoReqReader has encountered a problem with field value collections.");
				}
				row[j] = Value.parseObject(fieldValue);
				j++;
			}
			ds.addRow(row);
		}
		return ds;
	}

	/**
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		/*
		 * first create a local file with a trouble ticket. We can use this to
		 * simulate HttpRequestInpiutStream
		 */
		String fileName = "d:/tmp/a";
		TtTroubleTicket.TroubleTicket ticket = createTicket();
		createFile(fileName, ticket);
		/*
		 * create ticket using input stream
		 */
		String classPrefix = "org.simplity.gateway.TtTroubleTicket$";
		String className = "TroubleTicket";
		Class<?> cls = Class.forName(classPrefix + className);
		logger.info("Created class " + cls.getName());
		Method method = cls.getMethod("newBuilder");
		Object obj = method.invoke(null);

		//TtTroubleTicket.TroubleTicket.Builder builder = TtTroubleTicket.TroubleTicket.newBuilder();
		TtTroubleTicket.TroubleTicket.Builder builder = (TtTroubleTicket.TroubleTicket.Builder)obj;
		InputStream input = new FileInputStream(fileName);
		builder.mergeFrom(input);
		input.close();
		ticket = builder.build();
		logger.info("Input ticket is \n" + ticket.toString());
		/*
		 * create reqReader to extract data into ctx
		 */
		ReqReader reeder = new ProtoReqReader(ticket, new JSONObject());
		ServiceContext ctx = new ServiceContext("junk", Value.newTextValue("100"));
		reeder.readAsPerSpec(ctx);

		/*
		 * simulate service execution by changing data in ctx
		 */
		ctx.setTextValue("severity", "warning");
		DataSheet sheet = ctx.getDataSheet("note");
		Value[] row = { Value.newTextValue("addedAuthor"), Value.newTextValue("added date"),
				Value.newTextValue("added text") };
		sheet.addRow(row);

		/*
		 * write data to file simulating response output stream
		 */
		RespWriter writer = new ProtoRespWriter(TtTroubleTicket.TroubleTicket.newBuilder());
		writer.writeAsPerSpec(ctx);
		writer.getFinalResponseObject();
		logger.info("response text = \n " + writer.getFinalResponseText());
		/*
		 * though we are calling this .txt, it is actually in internal format of
		 * protobuf. However, it can be opened and checked for data as text file, with some special characters
		 */
		OutputStream stream = new FileOutputStream(fileName + ".txt");
		writer.writeout(stream);
		stream.close();
	}

	private static TtTroubleTicket.TroubleTicket createTicket() {
		TtTroubleTicket.TroubleTicket.Builder builder = TtTroubleTicket.TroubleTicket.newBuilder();
		builder.setCorrelationId("cor123");
		builder.setCreationDate("2017-06-27T12:21:23.234Z");
		builder.setDescription("tt description");
		builder.setId("id1");

		TtTroubleTicket.Note.Builder noteBuilder = TtTroubleTicket.Note.newBuilder();
		for (int i = 1; i < 3; i++) {
			noteBuilder.clear();
			noteBuilder.setAuthor("auth" + i);
			noteBuilder.setDate("2016-12-31");
			noteBuilder.setText("author-" + i + " text");
			builder.addNote(noteBuilder.build());
		}

		TtTroubleTicket.RelatedObject.Builder raBuilder = TtTroubleTicket.RelatedObject.newBuilder();
		for (int i = 1; i < 4; i++) {
			raBuilder.clear();
			raBuilder.setInvolvement("inv-" + i);
			raBuilder.setReference("reference-" + i);
			builder.addRelatedObject(raBuilder.build());
		}

		TtTroubleTicket.RelatedParty.Builder rpBuilder = TtTroubleTicket.RelatedParty.newBuilder();
		for (int i = 1; i < 6; i++) {
			rpBuilder.clear();
			rpBuilder.setHref("urlll-" + i);
			rpBuilder.setRole("rollole-" + i);
			builder.addRelatedParty(rpBuilder.build());
		}

		builder.setSeverity("error");
		builder.setStatus(TroubleTicket_Statu.TROUBLETICKET_STATU_ACKNOWLEDGED);
		builder.setType("ttType1");

		return builder.build();
	}

	/**
	 *
	 * @param fileName
	 */
	private static void createFile(String fileName, TtTroubleTicket.TroubleTicket ticket) {
		File file = new File(fileName);
		OutputStream stream = null;
		try {
			stream = new FileOutputStream(file);
			stream.write(ticket.toByteArray());
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

	private static TtTroubleTicket.TroubleTicket loadTicket(String fileName) {
		File file = new File(fileName);
		InputStream stream = null;
		try {
			stream = new FileInputStream(file);
			TtTroubleTicket.TroubleTicket.Builder builder = TtTroubleTicket.TroubleTicket.newBuilder();
			builder.mergeFrom(stream);
			return builder.build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
