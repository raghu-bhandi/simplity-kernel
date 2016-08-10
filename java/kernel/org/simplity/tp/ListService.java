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
package org.simplity.tp;

import java.io.StringWriter;
import java.io.Writer;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbClientInterface;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceData;

/**
 * service that delivers list (key-value pairs) based on pre-defined records
 *
 * @author simplity.org
 *
 */
public class ListService extends Service {
	private static ListService instance = new ListService();
	private static final int PREFIX_LENGTH = "list_".length();

	/**
	 * returns an instance for the required name. As of now, our design allows a
	 * singleton to handle all services in a thread-safe way
	 *
	 * @param serviceName
	 * @param record
	 * @return list service
	 */
	public static ListService getService(String serviceName, Record record) {
		return instance;
	}

	@Override
	public Value executeAsAction(ServiceContext ctx, DbDriver driver) {
		throw new ApplicationError(
				"List service can not be called as sub-service. Use keyValueList action instead");
	}

	@Override
	public ServiceData respond(ServiceData inData) {
		String payLoad = inData.getPayLoad();
		ServiceData outData = new ServiceData();

		if (payLoad == null) {
			Tracer.trace("No input found for this service");
			return outData;
		}
		/*
		 * create service context
		 */
		String serviceName = inData.getServiceName();
		Value userId = inData.getUserId();
		ServiceContext ctx = new ServiceContext(serviceName, userId);
		/*
		 * let us extract all data
		 */
		JsonUtil.extractAll(payLoad, ctx);
		/*
		 * do we have require data to process this request?
		 */
		String recordName = inData.getServiceName().substring(PREFIX_LENGTH);
		Record record = ComponentManager.getRecord(recordName);
		String keyName = record.getSuggestionKeyName();
		Value keyValue = ctx.getValue(keyName);
		if (keyValue == null) {
			Tracer.trace("No value found in field  available in field "
					+ keyName);
			return outData;
		}

		/*
		 * create the helper and invoke it thru db driver
		 */
		ListHelper myHelper = new ListHelper(record, keyValue.toString(),
				userId);
		DbDriver.workWithDriver(myHelper, DbAccessType.READ_ONLY);
		DataSheet sheet = myHelper.getresult();
		if (sheet != null) {
			Writer w = new StringWriter();
			JSONWriter writer = new JSONWriter(w);
			JsonUtil.sheetToJson(writer, sheet, null);
			outData.setPayLoad(w.toString());
		}

		return outData;
	}

	class ListHelper implements DbClientInterface {
		private final Record record;
		private final String keyValue;
		private final Value userId;
		private DataSheet result;

		ListHelper(Record record, String keyValue, Value userId) {
			this.keyValue = keyValue;
			this.record = record;
			this.userId = userId;
		}

		@Override
		public boolean workWithDriver(DbDriver driver) {
			this.result = this.record.list(this.keyValue, driver, this.userId);
			return true;
		}

		DataSheet getresult() {
			return this.result;
		}
	}

	@Override
	public void getReady() {
		this.dbAccessType = DbAccessType.READ_ONLY;
	}
}