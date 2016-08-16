/*
 * Copyright (c) 2015 EXILANT Technologies Private Limited (www.exilant.com)
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
package org.simplity.kernel.dt;

import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.value.InvalidValueException;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

/**
 * numeric with or without decimals
 *
 * @author simplity.org
 *
 */
public class NumericDataType extends DataType {
	/**
	 * min digits before decimal places required for this value
	 */
	long minValue = Long.MIN_VALUE;

	/**
	 * maximum number of whole digits (excluding decimal digits)
	 */
	long maxValue = Long.MAX_VALUE;

	/**
	 * maximum number of fractional digits. Anything more than this is rounded
	 * off
	 */
	int nbrFractionDigits = 0;

	@Override
	public Value validateValue(Value value) {
		long intVal = 0;
		/*
		 * is it numeric?
		 */
		try {
			intVal = value.toInteger();
		} catch (InvalidValueException e) {
			return null;
		}
		/*
		 * max/min is long and we do not care about fraction
		 */
		if (intVal > this.maxValue || intVal < this.minValue) {
			return null;
		}
		/*
		 * ensure that we have the right type
		 */
		ValueType thisType = this.getValueType();
		if (thisType == value.getValueType()) {
			return value;
		}
		if (thisType == ValueType.INTEGER) {
			return Value.newIntegerValue(intVal);
		}
		return Value.newDecimalValue(intVal);
	}

	@Override
	public ValueType getValueType() {
		if (this.nbrFractionDigits == 0) {
			return ValueType.INTEGER;
		}
		return ValueType.DECIMAL;
	}

	@Override
	public int getMaxLength() {
		return Long.toString(this.maxValue).length() + this.nbrFractionDigits
				+ 1;
	}

	@Override
	public int getScale() {
		return this.nbrFractionDigits;
	}

	@Override
	protected int validateSpecific(ValidationContext ctx) {
		int count = 0;
		if (this.minValue > this.maxValue) {
			ctx.addError("Invalid number range. Min vaue of " + this.minValue
					+ " is greater that max value of " + this.maxValue);
			count = 1;
		}
		if (this.nbrFractionDigits < 0) {
			ctx.addError("nbrFractionDigits is set to a negative value of "
					+ this.nbrFractionDigits);
			count++;
		}
		return count;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.dt.DataType#synthesiseDscription()
	 */
	@Override
	protected String synthesiseDscription() {
		StringBuilder sbf = new StringBuilder("Expecting ");
		if (this.nbrFractionDigits == 0) {
			sbf.append("an integer ");
		} else {
			sbf.append("a decimal number ");
		}
		sbf.append("between ").append(this.minValue).append(" and ")
				.append(this.maxValue);
		return sbf.toString();
	}
}
