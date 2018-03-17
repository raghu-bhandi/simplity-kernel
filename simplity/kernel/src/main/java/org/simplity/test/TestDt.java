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

package org.simplity.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;

import org.simplity.kernel.comp.ComponentManager;
import org.simplity.kernel.dt.DataType;
import org.simplity.kernel.util.DateUtil;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.simplity.service.AbstractService;
import org.simplity.service.PayloadType;
import org.simplity.service.ServiceData;

/** @author simplity.org */
public class TestDt extends AbstractService {
	private static final Logger testLogger = LoggerFactory.getLogger(TestDt.class);

  /*
   * (non-Javadoc)
   *
   * @see org.simplity.service.ServiceInterface#respond(org.simplity.service.
   * ServiceData)
   */
  @Override
  public ServiceData respond(ServiceData inputData, PayloadType payloadType) {
    String input = inputData.getPayLoadAsJsonText();
    if (input == null || input.isEmpty()) {
      input = "{}";
    }
    JSONObject json = new JSONObject(input);
    JSONWriter writer = new JSONWriter();
    writer.object();
    int nbrOk = 0;
    int nbrNotOk = 0;
    for (String key : json.keySet()) {
      DataType dt = this.getDt(key);
      String val = json.get(key).toString();
      /*
       * special case of value for a date field
       */
      if (dt.getValueType() == ValueType.DATE) {
        val = this.getDateValue(val);
      }
      Value parsedValue = dt.parseValue(val);
      if (parsedValue == null) {
        nbrNotOk++;

        testLogger.info("Invalid value : " + val);

      } else {
        nbrOk++;
        writer.key(key);
        writer.value(parsedValue.toObject());
      }
    }
    writer.key("nbrOk");
    writer.value(nbrOk);
    writer.key("nbrNotOk");
    writer.value(nbrNotOk);
    writer.endObject();
    ServiceData outputData = new ServiceData(inputData.getUserId(), inputData.getServiceName());
    outputData.setPayLoad(writer.toString());
    return outputData;
  }

  /**
   * fieldName is of the form dataTypeName_something.
   *
   * @param fieldName
   * @return dataType, never null
   * @throws ApplicaitonError in case of malformed name, or non-existing data type name
   */
  private DataType getDt(String key) {
    int idx = key.indexOf('_');
    if (idx == -1) {
      throw new ApplicationError("field name " + key + " is missing _ to mark its data type.");
    }
    String dtName = key.substring(0, idx);
    DataType dt = ComponentManager.getDataTypeOrNull(dtName);
    if (dt == null) {
      throw new ApplicationError(
          "field name "
              + key
              + " uses "
              + dtName
              + " as its data type prefix, but that is not a valid data type.");
    }
    return dt;
  }

  /**
   * date value may be of the form +nnn or -nnn to represent number of days after/before today.
   * Parse and replace the right date value in yyyy-MM-dd format.
   *
   * @param value
   * @return value as it is, or replaced with a date relative to today
   */
  private String getDateValue(String value) {
    if (value.isEmpty() == false) {
      char ch = value.charAt(0);
      if (ch == '+' || ch == '-') {
        try {
          int days = Integer.parseInt(value);
          Date date = new Date();
          date = DateUtil.addDays(date, days);
          return DateUtil.formatDate(date);
        } catch (Exception e) {

          testLogger.info("Did not parse " + value + " as special date value : " + e.getMessage());
        }
      }
    }
    return value;
  }
}
