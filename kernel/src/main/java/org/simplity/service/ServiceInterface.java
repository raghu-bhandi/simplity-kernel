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
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.simplity.service;

import org.simplity.kernel.comp.Component;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;

/***
 * Defines a Service
 *
 */
public interface ServiceInterface extends Component {

	/**
	 * this service is called as a step in another service.
	 *
	 * @param ctx
	 * @param driver
	 * @return return value of this action(That is, value to be returned to
	 *         service after executing this service as an action)
	 */
	public Value executeAsAction(ServiceContext ctx, DbDriver driver);

	/**
	 * should the service be fired in the background (in a separate thread)?
	 *
	 * @return true if this service is marked for background execution
	 */
	public boolean toBeRunInBackground();

	/**
	 * can this service instance be cached for performance. caching is enabled
	 * only during production. During development, caching is disabled to ensure
	 * that the developer gets to execute the latest version of the service
	 * always
	 *
	 * @return true if caching is ok, false otherwise
	 */
	public boolean okToCache();

	/**
	 * @return data base access required by this service
	 */
	public DbAccessType getDataAccessType();

	/**
	 * @param inputData
	 * @return service data that has response to be sent to client, as well as data meant for client-agent
	 */
	public ServiceData respond(ServiceData inputData);
}
