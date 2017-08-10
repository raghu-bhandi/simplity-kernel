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

package org.simplity.kernel.data;

import java.util.HashMap;
import java.util.Map;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to break a child data by its parent row. It manages to provide
 * the data sheet with filtered rows for the given parent row.
 *
 * @author simplity.org
 *
 */
public class ChildSheets {
	private static final Logger logger = LoggerFactory.getLogger(ChildSheets.class);
	/**
	 * array of column index that has the value for the key in parent row
	 */
	private final int[] parentIndexes;
	/**
	 * child sheets mapped by parent key values
	 */
	private final Map<String, DataSheet> children = new HashMap<String, DataSheet>();

	/**
	 * create an instance for parent-child combination
	 *
	 * @param parentKeys
	 * @param childKeys
	 * @param parentSheet
	 * @param childSheet
	 */
	public ChildSheets(String[] parentKeys, String[] childKeys, DataSheet parentSheet, DataSheet childSheet) {
		/*
		 * indexes of parent keys to a row in that data sheet
		 */
		this.parentIndexes = getKeys(parentKeys, parentSheet);
		int[] indexes = getKeys(childKeys, childSheet);

		String[] colNames = childSheet.getColumnNames();
		ValueType[] types = childSheet.getValueTypes();
		int nbrRows = childSheet.length();
		int nbrKeys = parentKeys.length;
		/*
		 * split this sheet into a number of child-sheets :one for each unique
		 * combination of key values
		 */
		StringBuilder keys = new StringBuilder();
		for (int rowIdx = 0; rowIdx < nbrRows; rowIdx++) {
			Value[] row = childSheet.getRow(rowIdx);
			for (int keyIdx = 0; keyIdx < indexes.length; keyIdx++) {
				keys.append(row[indexes[keyIdx]].toString());
			}
			String key =keys.toString();
			keys.setLength(0);
			/*
			 * do we have a filtered child-sheet for these keys combination?
			 */
			DataSheet sheet = this.children.get(key);
			if (sheet == null) {
				sheet = new MultiRowsSheet(colNames, types);
				logger.info("Child sheet created for key = " + key);
				this.children.put(key, sheet);
			}

			sheet.addRow(row);
		}
	}

	/**
	 *
	 * @param parentRow
	 * @return a child data sheet for the given parent row
	 */
	public DataSheet getChildSheet(Value[] parentRow) {
		String key = parentRow[this.parentIndexes[0]].toString();
		if (this.parentIndexes.length > 1) {
			for (int i = 1; i < this.parentIndexes.length; i++) {
				key += parentRow[this.parentIndexes[i]];
			}
		}

		return this.children.get(key);
	}

	private static int[] getKeys(String[] names, DataSheet sheet) {
		int[] result = new int[names.length];
		for (int i = 0; i < names.length; i++) {
			int idx = sheet.getColIdx(names[i]);
			if (idx < 0) {
				throw new ApplicationError("Column name " + names[i] + " is not found in data sheet");
			}
			result[i] = idx;
		}
		return result;
	}
}
