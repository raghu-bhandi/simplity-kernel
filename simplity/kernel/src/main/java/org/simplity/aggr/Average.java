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

package org.simplity.aggr;

/**
 * sum of a column
 *
 * @author simplity.org
 *
 */
public class Average extends MathAggregator {

	/**
	 * @param inputName
	 * @param inputIsDecimal
	 * @param outputName
	 * @param outputIsDecimal
	 */
	public Average(String inputName, boolean inputIsDecimal, String outputName, boolean outputIsDecimal) {
		super(inputName, inputIsDecimal, outputName, outputIsDecimal);
	}

	/* (non-Javadoc)
	 * @see aggr.MathAggregator#accumulateInteger(long)
	 */
	@Override
	protected void accumulateInteger(long value) {
		this.accumulatedValue += value;

	}

	/* (non-Javadoc)
	 * @see aggr.MathAggregator#accumulateDecimal(double)
	 */
	@Override
	protected void accumulateDecimal(double value) {
		this.accumulatedValue += value;
	}

	/* (non-Javadoc)
	 * @see aggr.MathAggregator#getDecimalResult()
	 */
	@Override
	protected double getDecimalResult() {
		return this.accumulatedValue;
	}

	/* (non-Javadoc)
	 * @see aggr.MathAggregator#getIntegerResult()
	 */
	@Override
	protected long getIntegerResult() {
		return Math.round(this.accumulatedValue);
	}
}
