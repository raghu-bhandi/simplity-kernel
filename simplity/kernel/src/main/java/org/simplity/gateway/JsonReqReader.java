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

import java.util.Stack;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * request reader for json input
 *
 * @author simplity.org
 *
 */
public class JsonReqReader implements ReqReader {
	private static final Logger logger = LoggerFactory.getLogger(ReqReader.class);
	/**
	 * payload parsed into a JSON Object. Null if input is not a json
	 */
	private final JSONObject inputJson;
	/**
	 * raw payload. null if it is parsed into a valid json
	 */
	private final String inputText;

	/**
	 * stack of open objects.
	 */
	private Stack<Object> openObjects = new Stack<Object>();

	/**
	 * current object that is open. null if the current object is an array
	 */
	private JSONObject currentObject;

	/**
	 * current array that is open. null if the current object is a OBJECT.
	 */
	private JSONArray currentArray;

	/**
	 * instantiate a translator for the input payload
	 *
	 * @param payload
	 */
	public JsonReqReader(String payload) {
		logger.info("Payload received : = ", payload);
		if (payload == null || payload.isEmpty()) {
			logger.info("Input is empty for translator.");
			this.inputJson = null;
			this.inputText = null;
			return;
		}
		JSONObject json = null;
		try {
			json = new JSONObject(payload);
		} catch (Exception e) {
			logger.info("Input is not a valid json. We treat that as a single value");
		}
		if (json == null) {
			this.inputJson = null;
			this.inputText = payload;
		} else {
			this.currentObject = this.inputJson = json;
			this.inputText = null;
		}
	}

	/**
	 * instantiate input translator for a json
	 *
	 * @param json
	 */
	public JsonReqReader(JSONObject json) {
		logger.info("Payload Json : = ", json.toString());
		this.currentObject = this.inputJson = json;
		this.inputText = null;
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
		if (this.inputText != null) {
			return this.inputText;
		}
		return this.inputJson;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.DataTranslator#getValueType(java.lang.String)
	 */
	@Override
	public InputValueType getValueType(String fieldName) {
		if (this.currentObject == null) {
			return InputValueType.NULL;
		}
		return getType(this.currentObject.opt(fieldName));
	}

	private static InputValueType getType(Object val) {
		if (val == null) {
			return InputValueType.NULL;
		}
		if (val instanceof JSONArray) {
			return InputValueType.ARRAY;
		}
		if (val instanceof JSONObject) {
			return InputValueType.OBJECT;
		}
		return InputValueType.VALUE;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#getValueType(int)
	 */
	@Override
	public InputValueType getValueType(int idx) {
		if (this.currentArray == null) {
			return InputValueType.NULL;
		}
		return getType(this.currentArray.opt(idx));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.DataTranslator#getValue(java.lang.String)
	 */
	@Override
	public Object getValue(String fieldName) {
		if (this.currentObject == null) {
			return null;
		}
		return this.currentObject.opt(fieldName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#openObject(java.lang.String)
	 */
	@Override
	public boolean openObject(String attributeName) {
		if (this.currentObject != null) {
			Object obj = this.currentObject.opt(attributeName);
			if (obj != null && obj instanceof JSONObject) {
				this.openObjects.push(this.currentObject);
				this.currentObject = (JSONObject) obj;
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#openObject(int)
	 */
	@Override
	public boolean openObject(int idx) {
		if (this.currentArray != null) {
			Object obj = this.currentArray.opt(idx);
			if (obj != null && obj instanceof JSONObject) {
				this.openObjects.push(this.currentArray);
				this.currentObject = (JSONObject) obj;
				this.currentArray = null;
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#closeObject()
	 */
	@Override
	public boolean closeObject() {
		if (this.currentObject == null) {
			return false;
		}
		return this.pop();
	}

	private boolean pop() {
		if (this.openObjects.isEmpty()) {
			return false;
		}
		Object obj = this.openObjects.pop();
		if (obj instanceof JSONObject) {
			this.currentObject = (JSONObject) obj;
			this.currentArray = null;
		} else {
			this.currentArray = (JSONArray) obj;
			this.currentObject = null;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#openArray(java.lang.String)
	 */
	@Override
	public boolean openArray(String attributeName) {
		if (this.currentObject != null) {
			Object obj = this.currentObject.opt(attributeName);
			if (obj != null && obj instanceof JSONArray) {
				this.openObjects.push(this.currentObject);
				this.currentObject = null;
				this.currentArray = (JSONArray) obj;
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#openArray(int)
	 */
	@Override
	public boolean openArray(int zeroBasedIdx) {
		if (this.currentArray != null) {
			Object obj = this.currentArray.opt(zeroBasedIdx);
			if (obj != null && obj instanceof JSONArray) {
				this.openObjects.push(this.currentArray);
				this.currentArray = (JSONArray) obj;
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#endArray()
	 */
	@Override
	public boolean closeArray() {
		if (this.currentArray == null) {
			return false;
		}
		return this.pop();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#getValue(int)
	 */
	@Override
	public Object getValue(int zeroBasedIdx) {
		if (this.currentArray == null) {
			return null;
		}
		return this.currentArray.opt(zeroBasedIdx);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#getNbrElements()
	 */
	@Override
	public int getNbrElements() {
		if (this.currentArray == null) {
			return 0;
		}
		return this.currentArray.length();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ReqReader#getAttributeNames()
	 */
	@Override
	public String[] getAttributeNames() {
		if (this.currentObject == null) {
			return new String[0];
		}
		return JSONObject.getNames(this.currentObject);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.gateway.ReqReader#readAll(org.simplity.service.
	 * ServiceContext)
	 */
	@Override
	public void pushDataToContext(ServiceContext ctx) {
		if(JSONObject.getNames(this.inputJson)==null){
			return;
		}
		for (String key : JSONObject.getNames(this.inputJson)) {
			Object value = this.inputJson.opt(key);
			switch (getType(value)) {
			case ARRAY:
				JSONArray arr = (JSONArray) value;
				if (arr.length() != 0) {
					DataSheet sheet = getSheet(arr);
					if (sheet != null) {
						ctx.putDataSheet(key, sheet);
						logger.info("Table " + key + " extracted with " + sheet.length() + " rows");
					}
				}
				break;

			case OBJECT:
				DataSheet sheet = getSheet((JSONObject) value);
				if (sheet != null) {
					ctx.putDataSheet(key, sheet);
					logger.info("Object " + key + " extracted as a single-row data sheet.");
				}
				break;
			case VALUE:
				ctx.setValue(key, Value.parseObject(value));
				break;
			case NULL:
				break;
			default:
				logger.error("JsonReqReader needs to have code to handle json value type of " + getType(value));
			}

		}
	}

	/**
	 * @param value
	 * @return
	 */
	private static DataSheet getSheet(JSONObject object) {
		String[] names = JSONObject.getNames(object);
		int nbr = names.length;
		ValueType[] types = new ValueType[nbr];
		Value[] values = new Value[nbr];

		for (int i = 0; i < names.length; i++) {
			Object obj = object.opt(names[i]);
			InputValueType ivt = getType(obj);

			if (ivt == null) {
				types[i] = ValueType.TEXT;
				values[i] = Value.newUnknownValue(ValueType.TEXT);
				continue;
			}

			if (ivt == InputValueType.VALUE) {
				Value value = Value.parseObject(obj);
				values[i] = value;
				types[i] = value.getValueType();
				continue;
			}
			/*
			 * we can not handle embedded object structures
			 */
			logger.error(
					"Input contains arbitrary object structure that can not be parsed without input specification. Value not extracted");
			return null;

		}
		DataSheet ds = new MultiRowsSheet(names, types);
		ds.addRow(values);
		return ds;
	}

	private static DataSheet getSheet(JSONArray arr) {
		/*
		 * we guess the fields based on the attributes of first element in
		 * the array
		 */
		JSONObject exampleObject = arr.optJSONObject(0);
		if (exampleObject == null) {
			logger.info("Json array has its first object as null, and hence we abandoned parsing it.");
			return null;
		}
		DataSheet ds = getSheet(exampleObject);
		String[] names = ds.getColumnNames();
		int nbrCols = names.length;
		int nbr = arr.length();
		for (int i = 1; i < nbr; i++){
			JSONObject obj = arr.optJSONObject(i);
			if (obj == null) {
				logger.info("Row " + (i + 1) + " is null. Not extracted");
				continue;
			}
			Value[] row = new Value[nbrCols];
			for (int j = 0; j < names.length; j++) {
				row[j] = Value.parseObject(obj.opt(names[j]));
			}
			ds.addRow(row);
		}
		return ds;
	}

	/* (non-Javadoc)
	 * @see org.simplity.gateway.ReqReader#hasInputSpecs()
	 */
	@Override
	public boolean hasInputSpecs() {
		return true;
	}
}
