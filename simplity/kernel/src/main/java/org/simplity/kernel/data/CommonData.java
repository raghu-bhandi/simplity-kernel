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
package org.simplity.kernel.data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.value.BooleanValue;
import org.simplity.kernel.value.DateValue;
import org.simplity.kernel.value.DecimalValue;
import org.simplity.kernel.value.IntegerValue;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

/**
 * Default generic data structure that is created for implementing a service. We
 * would encourage sub-classing this rather than having this as an attribute.
 *
 * @author simplity.org
 *
 */
public class CommonData implements CommonDataInterface {
	protected static final int NOT_APPLICABLE = -1;

	/**
	 * generally temp and work related fields that are created during an
	 * algorithm, and are not part of any record. These fields are addressed
	 * with just field name with no qualifier
	 */
	protected final Map<String, Value> allFields = new HashMap<String, Value>();

	/**
	 * data sheets. a given data sheet may be either a SingleRowSheet or
	 * DataSheet
	 */
	protected final Map<String, DataSheet> allSheets = new HashMap<String, DataSheet>();

	/**
	 * sheets that are currently being iterated. Iteration over single-row sheet
	 * is dummy, and is simulated, and hence is never put into this list
	 */
	protected final Map<String, SheetIterator> iteratedSheets = new HashMap<String, CommonData.SheetIterator>();

	/**
	 * in java-class intensive projects, objects like DAO, DTO are used for data
	 * So, let us carry them as well.
	 */
	protected final Map<String, Object> allObjects = new HashMap<String, Object>();

	@Override
	public final Value getValue(String fieldName) {
		if (fieldName == null) {
			return null;
		}
		NameParts nameParts = new NameParts(fieldName);
		/*
		 * it is from a data sheet
		 */
		if (nameParts.sheet == null) {
			return this.allFields.get(fieldName);
		}

		if (nameParts.rowIdx == CommonData.NOT_APPLICABLE) {
			return nameParts.sheet.getValue(nameParts.fieldName);
		}
		return nameParts.sheet.getColumnValue(nameParts.fieldName,
				nameParts.rowIdx);
	}

	@Override
	public final void setValue(String fieldName, Value value) {
		if (fieldName == null) {
			return;
		}
		NameParts nameParts = new NameParts(fieldName);

		if (nameParts.sheet == null) {
			if (value == null) {
				this.allFields.remove(fieldName);
			} else {
				this.allFields.put(fieldName, value);
			}
			return;
		}

		if (nameParts.rowIdx != CommonData.NOT_APPLICABLE) {
			nameParts.sheet.setColumnValue(nameParts.fieldName,
					nameParts.rowIdx, value);
			return;
		}

		if (value == null) {
			nameParts.sheet.removeValue(fieldName);
		} else {
			nameParts.sheet.setValue(fieldName, value);
		}
	}

	@Override
	public final Value removeValue(String fieldName) {
		if (fieldName == null) {
			return null;
		}
		NameParts nameParts = new NameParts(fieldName);
		/*
		 * it is from a data sheet
		 */
		if (nameParts.sheet == null) {
			return this.allFields.remove(fieldName);
		}

		if (nameParts.rowIdx == CommonData.NOT_APPLICABLE) {
			return nameParts.sheet.removeValue(fieldName);
		}

		Value value = nameParts.sheet.getColumnValue(nameParts.fieldName,
				nameParts.rowIdx);
		nameParts.sheet.setColumnValue(nameParts.fieldName, nameParts.rowIdx,
				null);
		return value;
	}

	@Override
	public final boolean hasValue(String fieldName) {
		if (fieldName == null) {
			return false;
		}
		NameParts nameParts = new NameParts(fieldName);
		/*
		 * it is from a data sheet
		 */
		if (nameParts.sheet == null) {
			return this.allFields.containsKey(fieldName);
		}

		if (nameParts.rowIdx == CommonData.NOT_APPLICABLE) {
			return nameParts.sheet.hasValue(fieldName);
		}
		return nameParts.sheet.getColumnValue(nameParts.fieldName,
				nameParts.rowIdx) != null;
	}

	@Override
	public final DataSheet getDataSheet(String sheetName) {
		return this.allSheets.get(sheetName);
	}

	@Override
	public final void putDataSheet(String sheetName, DataSheet sheet) {
		this.allSheets.put(sheetName, sheet);
	}

	@Override
	public final boolean hasDataSheet(String sheetName) {
		return this.allSheets.containsKey(sheetName);
	}

	@Override
	public final DataSheet removeDataSheet(String sheetName) {
		return this.allSheets.remove(sheetName);
	}

	/*
	 * (non-Javadoc)
	 * @see org.simplity.kernel.data.CommonDataInterface#startIteration(java.lang.String)
	 */
	@Override
	/**
	 * iterator is non-null. This avoids for-loop null-pointer exceptions
	 */
	public final DataSheetIterator startIteration(String sheetName)
			throws AlreadyIteratingException {
		if (this.iteratedSheets.containsKey(sheetName)) {
			throw new AlreadyIteratingException();
		}
		DataSheet sheet = this.getDataSheet(sheetName);
		int nbrRows = 0;
		if(sheet != null){
			nbrRows = sheet.length();
		}

		if (nbrRows == 0 || sheet instanceof MultiRowsSheet == false) {
			return new SheetIterator(null, nbrRows);
		}
		/*
		 * we have to track this iterator
		 */
		SheetIterator	iter = new SheetIterator(sheetName, nbrRows);
		this.iteratedSheets.put(sheetName, iter);
		return iter;
	}

	void endIteration(String sheetName) {
		if (sheetName != null) {
			this.iteratedSheets.remove(sheetName);
		}
	}

	protected class SheetIterator implements DataSheetIterator {

		/**
		 * last index - zero based, that this sheet can go up to.
		 *
		 */
		private int lastIdx;
		/**
		 * bit tricky, because we want to accommodate the state
		 * started-but-before -first-get-next. -1 is that state, but we return 0
		 * as current index
		 */
		private int currentIdx = -1;

		/**
		 * null means need not worry about. non-null means we have to inform
		 * parent when this iteration completes
		 */
		private final String sheetName;

		/**
		 *
		 * @param sheetName
		 * @param nbrRows
		 */
		SheetIterator(String sheetName, int nbrRows) {
			this.lastIdx = nbrRows - 1;
			this.sheetName = sheetName;
		}

		/**
		 * get current index. default is zero
		 *
		 * @return
		 */
		int getIdx() {
			if (this.currentIdx < 0) {
				return 0;
			}
			return this.currentIdx;
		}

		@Override
		public boolean hasNext() {
			return this.currentIdx < this.lastIdx;
		}

		@Override
		public boolean moveToNextRow() {
			if (this.currentIdx < this.lastIdx) {
				this.currentIdx++;
				return true;
			}
			if (this.currentIdx == this.lastIdx) {
				this.cancelIteration();
			}
			return false;
		}

		@Override
		public void cancelIteration() {
			this.lastIdx = -1;
			CommonData.this.endIteration(this.sheetName);
		}
	}

	SheetIterator getIterator(String sheetName) {
		return this.iteratedSheets.get(sheetName);
	}

	/**
	 * simple data structure to split qualified name into sheet name and field
	 * name
	 */
	private class NameParts {
		String fieldName = null;
		DataSheet sheet = null;
		int rowIdx = CommonData.NOT_APPLICABLE;

		NameParts(String fullName) {
			if (fullName == null) {
				return;
			}
			int idx = fullName.indexOf('.');
			if (idx == -1) {
				this.fieldName = fullName;
				return;
			}

			this.fieldName = fullName.substring(idx + 1).trim();
			String sheetName = fullName.substring(0, idx).trim();
			this.sheet = CommonData.this.getDataSheet(sheetName);
			if (this.sheet == null) {
				throw new ApplicationError(
						"Data sheet "
								+ sheetName
								+ " is not available in context for a put/get/remove operation for field "
								+ fullName);
			}
			if (this.sheet instanceof MultiRowsSheet) {
				SheetIterator iter = CommonData.this.getIterator(sheetName);
				if (iter == null) {
					this.rowIdx = 0;
				} else {
					this.rowIdx = iter.getIdx();
				}
			}
		}
	}

	@Override
	public Set<Entry<String, Value>> getAllFields() {
		return this.allFields.entrySet();
	}


	@Override
	public Value[] getValues(String[] names) {
		Value[] values = new Value[names.length];
		int i = 0;
		for(String name : names){
			values[i++] = this.allFields.get(name);
		}
		return values;
	}

	/**
	 * Way to pass an object to subsequent action
	 *
	 * @param dataName
	 *            name by which this is referred
	 * @param object
	 *            object being set to this name
	 */
	public void setObject(String dataName, Object object) {
		this.allObjects.put(dataName, object);
	}

	/**
	 * @param dataName
	 *            name by which this is referred
	 * @return get the named object, or null if the object does not exists
	 */
	public Object getObject(String dataName) {
		return this.allObjects.get(dataName);
	}

	/**
	 * @param dataName
	 *            name of the object to be removed
	 * @return object being removed, null if object was not found
	 */
	public Object removeObject(String dataName) {
		return this.allObjects.remove(dataName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.CommonDataInterface#getTextValue(java.lang.String
	 * )
	 */
	@Override
	public String getTextValue(String fieldName) {
		Value val = this.getValue(fieldName);
		if (Value.isNull(val)) {
			return null;
		}
		String str = val.toString();
		if(str.isEmpty()){
			return null;
		}
		return str;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.CommonDataInterface#getLongValue(java.lang.String
	 * )
	 */
	@Override
	public long getLongValue(String fieldName) {
		Value val = this.getValue(fieldName);
		if (Value.isNull(val)) {
			return 0;
		}
		ValueType vt = val.getValueType();
		if (vt == ValueType.INTEGER) {
			return ((IntegerValue) val).getLong();
		}
		if (vt == ValueType.DECIMAL) {
			return ((DecimalValue) val).getLong();
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.CommonDataInterface#getDateValue(java.lang.String
	 * )
	 */
	@Override
	public Date getDateValue(String fieldName) {
		Value val = this.getValue(fieldName);
		if (Value.isNull(val)) {
			return null;
		}

		if (val.getValueType() == ValueType.DECIMAL) {
			return new Date(((DateValue) val).getDate());
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.CommonDataInterface#getBooleanValue(java.lang
	 * .String)
	 */
	@Override
	public boolean getBooleanValue(String fieldName) {
		Value val = this.getValue(fieldName);
		if (Value.isNull(val)) {
			return false;
		}
		if (val.getValueType() == ValueType.BOOLEAN) {
			return ((BooleanValue) val).getBoolean();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.CommonDataInterface#getDoubleValue(java.lang
	 * .String)
	 */
	@Override
	public double getDoubleValue(String fieldName) {
		Value val = this.getValue(fieldName);
		if (Value.isNull(val)) {
			return 0;
		}
		ValueType vt = val.getValueType();
		if (vt == ValueType.DECIMAL) {
			return ((DecimalValue) val).getDouble();
		}
		if (vt == ValueType.INTEGER) {
			return ((IntegerValue) val).getDouble();
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.CommonDataInterface#setTextValue(java.lang.String
	 * , java.lang.String)
	 */
	@Override
	public void setTextValue(String fieldName, String value) {
		this.setValue(fieldName, Value.newTextValue(value));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.CommonDataInterface#setLongValue(java.lang.String
	 * , long)
	 */
	@Override
	public void setLongValue(String fieldName, long value) {
		this.setValue(fieldName, Value.newIntegerValue(value));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.CommonDataInterface#setDoubleValue(java.lang
	 * .String, java.lang.Double)
	 */
	@Override
	public void setDoubleValue(String fieldName, double value) {
		this.setValue(fieldName, Value.newDecimalValue(value));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.CommonDataInterface#setDateValue(java.lang.String
	 * , java.util.Date)
	 */
	@Override
	public void setDateValue(String fieldName, Date value) {
		this.setValue(fieldName, Value.newDateValue(value));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.kernel.data.CommonDataInterface#setBooleanValue(java.lang
	 * .String, boolean)
	 */
	@Override
	public void setBooleanValue(String fieldName, boolean value) {
		this.setValue(fieldName, Value.newBooleanValue(value));
	}
}
