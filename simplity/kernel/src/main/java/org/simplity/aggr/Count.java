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

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.data.FieldsInterface;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;

/**
 * number of rows
 *
 * @author simplity.org
 *
 */
public class Count implements AggregatorWorker {
	/**
	 * field name to which sum is to be written out
	 */
	private final String outputName;

	private int count;
	/**
	 * keep track of accumulation and throw an exception in case of concurrency
	 * issues
	 */
	private boolean inProgress;

	/**
	 * create an an instance with the required parameters
	 *
	 * @param outputName
	 *            field/column name that is to be written out as sum. non-empty,
	 *            non-null;
	 */
	public Count(String outputName) {
		this.outputName = outputName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see aggr.Aggregator#init(org.simplity.service.ServiceContext)
	 */
	@Override
	public void init(ServiceContext ctx) {
		if (this.inProgress) {
			this.throwError();
		}
		this.inProgress = true;
	}

	private void throwError() {
		throw new ApplicationError("Aggregator instance should be ideally not re-used across aggregations."
				+ " In case it is used, it is to be ensured that the the sequence of calls is  "
				+ " init(), accumulate(), writeOut()/discard(), reset()");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see aggr.Aggregator#accumulate(org.simplity.kernel.data.FieldsInterface,
	 * org.simplity.service.ServiceContext)
	 */
	@Override
	public void accumulate(FieldsInterface currentRow, ServiceContext ctx){
		if (this.inProgress == false) {
			this.throwError();
		}
		this.count++;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see aggr.Aggregator#writeOut(org.simplity.kernel.data.FieldsInterface,
	 * org.simplity.service.ServiceContext)
	 */
	@Override
	public void writeOut(FieldsInterface outputRow, ServiceContext ctx) {
		if (this.inProgress == false) {
			this.throwError();
		}
		outputRow.setValue(this.outputName, Value.newIntegerValue(this.count));
		this.discard(ctx);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see aggr.Aggregator#reset(org.simplity.service.ServiceContext)
	 */
	@Override
	public void reset(ServiceContext ctx) {
		this.count = 0;
		this.inProgress = false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see aggr.Aggregator#discard(org.simplity.service.ServiceContext)
	 */
	@Override
	public void discard(ServiceContext ctx) {
		this.count = 0;
	}
}
