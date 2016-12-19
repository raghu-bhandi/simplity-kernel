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

package org.simplity.service;

import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;

/**
 * @author simplity.org
 *
 */
public abstract class AbstractService implements ServiceInterface {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.comp.Component#getSimpleName()
	 */
	@Override
	public String getSimpleName() {
		return this.getClass().getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.comp.Component#getQualifiedName()
	 */
	@Override
	public String getQualifiedName() {
		return this.getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.comp.Component#getReady()
	 */
	@Override
	public void getReady() {
		// this class is ever-ready
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.simplity.kernel.comp.Component#validate(org.simplity.kernel.comp.
	 * ValidationContext)
	 */
	@Override
	public int validate(ValidationContext ctx) {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.comp.Component#getComponentType()
	 */
	@Override
	public ComponentType getComponentType() {
		return ComponentType.SERVICE;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.simplity.service.ServiceInterface#executeAsAction(org.simplity.service
	 * .ServiceContext, org.simplity.kernel.db.DbDriver)
	 */
	@Override
	public Value executeAsAction(ServiceContext ctx, DbDriver driver) {
		throw new ApplicationError(this.getSimpleName()
				+ " is not designed to be run as an sub-service action.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.service.ServiceInterface#toBeRunInBackground()
	 */
	@Override
	public boolean toBeRunInBackground() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.service.ServiceInterface#okToCache()
	 */
	@Override
	public boolean okToCache() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.service.ServiceInterface#getDataAccessType()
	 */
	@Override
	public DbAccessType getDataAccessType() {
		return DbAccessType.NONE;
	}
}
