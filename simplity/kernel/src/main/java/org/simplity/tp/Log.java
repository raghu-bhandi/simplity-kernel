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
package org.simplity.tp;

import java.util.Map;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.util.JsonUtil;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * show values of fields/table : meant for debugging
 *
 * @author simplity.org
 *
 */
public class Log extends Action {
	/**
	 * field/table names to be logged
	 */
	String[] names;

	@Override
	protected Value doAct(ServiceContext ctx) {
		if (this.names == null) {
			return Value.VALUE_FALSE;
		}
		if(this.names[0].equals("*")){
			this.logAll(ctx);
			return Value.VALUE_TRUE;
		}
		Tracer.trace("Values at log action " + this.actionName);
		boolean found = false;
		for (String nam : this.names) {
			if (ctx.hasValue(nam)) {
				found = true;
				Tracer.trace(nam + " = " + ctx.getValue(nam));
			}
			if (ctx.hasDataSheet(nam)) {
				found = true;
				ctx.getDataSheet(nam).trace();
			}
			if (!found) {
				Tracer.trace(nam + " is not a field or sheet name");
			}
		}
		return Value.VALUE_TRUE;
	}

	private void logAll(ServiceContext ctx){
		for(Map.Entry<String, Value> entry : ctx.getAllFields()){
			Tracer.trace(entry.getKey() + " = " + entry.getValue());
		}
		JSONWriter writer = new JSONWriter();
		writer.object();
		Tracer.trace("Data Sheets = ");
		for(Map.Entry<String, DataSheet> entry : ctx.getAllSheets()){
			writer.key(entry.getKey());
			JsonUtil.sheetToJson(writer, entry.getValue(), null, false);
		}
	}
}
