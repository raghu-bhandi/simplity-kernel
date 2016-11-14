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

package org.simplity.test;

import org.simplity.json.JSONArray;
import org.simplity.json.JSONObject;
import org.simplity.json.JSONWriter;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.ValidationContext;

/**
 * Represents a data sheet to be provided as input for a service
 *
 * @author simplity.org
 *
 */
public class Sheet {
	private static final String ROW_SEP = ";";
	private static final String CELL_SEP = ",";
	/**
	 * name of the sheet as expected by the service
	 */
	String sheetName;
	/**
	 * column names. It is a good practice to have them in the same order as in
	 * the input specification of the service. For output, this is optional.
	 */
	String[] columnNames;
	/**
	 * semicolon separated list of rows, each row being a comma separated list
	 * of column values. Column values MUST correspond to the columnNames.
	 * Optional for output.
	 */
	String data;

	/**
	 * relevant for output. Not valid if data is specified.
	 */
	int minRows;
	/**
	 * relevant for output. Not valid if data is specified.
	 */
	int maxRows;

	int validate(ValidationContext vtx) {
		int nbr = 0;
		if (this.sheetName == null) {
			vtx.addError("sheetName is mandatory for sheet");
			nbr++;
		}
		if (this.maxRows < this.minRows) {
			vtx.addError("max rows is set to " + this.maxRows
					+ " while minRows is set to " + this.minRows);
			nbr++;
		}
		if (this.columnNames == null) {
			if (this.data != null) {
				vtx.addError("data can not be specified without columnNames for a sheet.");
				nbr++;
			}
			return nbr;
		}
		if (this.data == null) {
			return nbr;
		}

		int nbrCols = this.columnNames.length;
		for (String row : this.data.split(ROW_SEP)) {
			if (row.split(CELL_SEP).length != nbrCols) {
				vtx.addError("data text is not formatted properly. There are "
						+ nbrCols
						+ " columns but data rows have different number of columns.");
				nbr++;
				break;
			}
		}
		return nbr;
	}

	/**
	 * writes this data sheet as a name-value pair for the running object. does
	 * not add anything if there is no data
	 *
	 * @param writer
	 */
	void writeToJson(JSONWriter writer) {
		if (this.data == null) {
			return;
		}
		writer.key(this.sheetName);
		writer.array();
		if (this.data == null) {
			writer.endArray();
			return;
		}
		int nbrCols = this.columnNames.length;
		for (String row : this.data.split(ROW_SEP)) {
			String[] cells = row.split(CELL_SEP);
			int nbr = cells.length;
			if (nbr < nbrCols) {
				throw new ApplicationError("Data sheet " + this.sheetName
						+ " has an invalid data attribute.");
			}
			writer.object();
			for (int i = 0; i < cells.length; i++) {
				writer.key(this.columnNames[i]);
				writer.value(cells[i]);
			}
			writer.endObject();
		}
		writer.endArray();
	}

	/**
	 * @param arr
	 * @return
	 */
	String match(JSONArray arr) {
		int nbrRows = arr.length();
		if (nbrRows < this.minRows) {
			return "Sheet " + this.sheetName + " is to have a min of "
					+ this.minRows + " rows but we got " + nbrRows + " rows.";
		}
		if (this.maxRows > 0 && nbrRows > this.maxRows) {
			return "Sheet " + this.sheetName + " is expected with a max of "
					+ this.maxRows + " rows but we got " + nbrRows + " rows.";
		}

		if (this.columnNames == null) {
			return null;
		}
		/*
		 * very advanced test where we are trying to match data in a data sheet
		 */
		int nbrCols = this.columnNames.length;
		int rowIdx = 0;
		for (String row : this.data.split(ROW_SEP)) {
			String[] cells = row.split(CELL_SEP);
			int nbr = cells.length;
			if (nbr < nbrCols) {
				throw new ApplicationError("Data sheet " + this.sheetName
						+ " has an invalid data attribute.");
			}
			Object obj = arr.get(rowIdx);
			rowIdx++;
			if (obj instanceof JSONObject == false) {
				return "value returned for sheet " + this.sheetName
						+ " is invalid.";
			}
			JSONObject json = (JSONObject) obj;
			for (int i = 0; i < cells.length; i++) {
				Object val = json.opt(this.columnNames[i]);
				if (val == null || val.toString().equals(cells[i]) == false) {
					return "Sheet=" + this.sheetName + " row(zero based)="
							+ rowIdx + " column=" + this.columnNames[i]
									+ " expected Vaue=" + cells[i] + " actual value="
									+ val;
				}
			}
		}
		return null;
	}
}
