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
package org.simplity.kernel.data;

import java.util.List;
import java.util.Map;

import org.simplity.json.JSONWriter;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.value.Value;

/**
 * represents a data sheet that is organized as a child of another sheet. Used
 * for outputting merged hierarchical data
 * 
 * @author simplity.org
 *
 */
public class HierarchicalSheet {
	private final String name;
	private final String[] fieldNames;
	private final Map<String, List<Value[]>> data;
	private final int parentKeyIdx;
	private final HierarchicalSheet[] children;

	/**
	 * create an immutable sheet
	 * 
	 * @param name
	 * @param fieldNames
	 * @param data
	 * @param idx
	 * @param children
	 */
	public HierarchicalSheet(String name, String[] fieldNames, Map<String, List<Value[]>> data, int idx,
			HierarchicalSheet[] children) {
		this.name = name;
		this.fieldNames = fieldNames;
		this.data = data;
		this.parentKeyIdx = idx;
		this.children = children;
	}

	/**
	 * write out rows from this sheet for the given parent row. This includes a
	 * recursive writing of rows from children
	 * 
	 * @param writer
	 * @param parentRow
	 */
	public void toJson(JSONWriter writer, Value[] parentRow) {
		String key = parentRow[this.parentKeyIdx].toString();
		List<Value[]> rows = this.data.get(key);
		if (rows == null) {
			Tracer.trace("No rows found in child sheet " + this.name + " for parent key " + key);
			return;
		}
		writer.key(this.name);
		writer.array();
		for (Value[] row : rows) {
			writer.object();
			int i = 0;
			for (String fieldName : this.fieldNames) {
				Value value = row[i];
				if (value != null) {
					writer.key(fieldName).value(value.toObject());
				}
				i++;
			}
			/*
			 * do we have children?
			 */
			if (this.children != null) {
				for (HierarchicalSheet child : this.children) {
					child.toJson(writer, row);
				}
			}
			writer.endObject();
		}
		writer.endArray();
	}
}
