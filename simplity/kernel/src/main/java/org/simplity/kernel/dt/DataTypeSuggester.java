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

package org.simplity.kernel.dt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.value.ValueType;

/**
 * utility class that suggests a suitable data type based on known attributes, and list of existing
 * data types. We keep improving this based on usage patterns
 *
 * @author simplity.org
 */
public class DataTypeSuggester {
	private static final Logger logger = LoggerFactory.getLogger(DataTypeSuggester.class);

  /*
   * lengths of available text data types in increasing order
   */
  private int[] lengths;
  /*
   * data type names corresponding to lengths[]
   */
  private String[] names;

  /** */
  public DataTypeSuggester() {
    Map<Integer, String> types = new HashMap<Integer, String>();
    for (Object obj : ComponentType.DT.getAll()) {
      if (obj instanceof TextDataType == false) {
        continue;
      }
      TextDataType dt = (TextDataType) obj;
      if (dt.getRegex() != null) {
        continue;
      }
      types.put(new Integer(dt.getMaxLength()), dt.getName());
    }
    int n = types.size();
    if (n == 0) {

      logger.info(
          "There are no text data types to suggest from. we will ALWAYS suggest default one.");
    }

    this.lengths = new int[n];
    this.names = new String[n];
    int i = 0;
    for (Integer len : types.keySet()) {
      this.lengths[i++] = len.intValue();
    }
    Arrays.sort(this.lengths);
    i = 0;
    for (int len : this.lengths) {
      this.names[i++] = types.get(new Integer(len));
    }
  }

  /**
   * @param sqlTypeInt
   * @param sqlTypeText
   * @param size
   * @param nbrDecimals
   * @return an existing data type for known sql types, or sqlTypeName for types that we do not
   *     manage
   */
  public String suggest(int sqlTypeInt, String sqlTypeText, int size, int nbrDecimals) {
    if (nbrDecimals > 0) {
      return ValueType.DECIMAL.getDefaultDataType();
    }
    switch (sqlTypeInt) {
      case Types.BIGINT:
      case Types.INTEGER:
        /*
         * numeric with nbrDecimals zero is landing here
         */
      case Types.NUMERIC:
      case Types.ROWID:
      case Types.TINYINT:
        return ValueType.INTEGER.getDefaultDataType();

      case Types.DECIMAL:
      case Types.DOUBLE:
      case Types.FLOAT:
      case Types.REAL:
        return ValueType.DECIMAL.getDefaultDataType();

      case Types.DATE:
      case Types.TIME:
      case Types.TIMESTAMP:
        return ValueType.DATE.getDefaultDataType();

      case Types.BOOLEAN:
      case Types.BIT:
        return ValueType.BOOLEAN.getDefaultDataType();

      case Types.CHAR:
      case Types.LONGNVARCHAR:
      case Types.LONGVARCHAR:
      case Types.NCHAR:
      case Types.NVARCHAR:
      case Types.VARCHAR:
        if (this.lengths != null) {
          int i = 0;
          for (int max : this.lengths) {
            if (size <= max) {
              return this.names[i];
            }
            i++;
          }
        }
        return ValueType.TEXT.getDefaultDataType();

      case Types.BLOB:
        return ValueType.BLOB.getDefaultDataType();

      case Types.CLOB:
      case Types.NCLOB:
        return ValueType.CLOB.getDefaultDataType();

      default:
        logger.info("we do not support SqlType " + sqlTypeText + " for a column in a table.");

        /*
         * we set the data type to te sql type so that the user gets an
         * error, or gets an opportunity to define it
         */
        return sqlTypeText;
    }
  }
}
