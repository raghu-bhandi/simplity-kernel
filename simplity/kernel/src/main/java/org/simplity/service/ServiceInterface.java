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

/** * Defines a Service */
public interface ServiceInterface extends Component {

  /**
   * this service is called as a step in another service.
   *
   * @param ctx
   * @param driver
   * @param useOwnDriverForTransaction if true, supplied driver is for read-only. Parent service is
   *     not managing transactions, and this service should manage its transaction if needed
   * @return return 0 if this service did not do intended work. positive number if it did its work.
   *     This returned value is used as workDone for the action.
   */
  public Value executeAsAction(
      ServiceContext ctx, DbDriver driver, boolean useOwnDriverForTransaction);

  /**
   * should the service be fired in the background (in a separate thread)?
   *
   * @return true if this service is marked for background execution
   */
  public boolean toBeRunInBackground();

  /**
   * can this service instance be cached for performance. caching is enabled only during production.
   * During development, caching is disabled to ensure that the developer gets to execute the latest
   * version of the service always
   *
   * @return true if caching is ok, false otherwise
   */
  public boolean okToCache();

  /** @return data base access required by this service */
  public DbAccessType getDataAccessType();

  /**
   * @param inputData input data, possibly a pay-load that came from a client
   * @param payloadType if this is meant to be communicated back to client
   * @return service data that has response to be sent to client, as well as data meant for
   *     client-agent
   */
  public ServiceData respond(ServiceData inputData, PayloadType payloadType);

    /**
	 * execute this service based on the input made available in the context.
	 *
	 * @param ctx
	 *            any input data requirement of this service is assumed to be
	 *            already made available here.
	 */
  public void serve(ServiceContext ctx);

	/**
	 * what kind of data is expected as input for this service from the client.
	 * It is up to the caller (client-agent) to ensure that a valid data is made
	 * available in service context before calling this service. Service does
	 * not repeat the validations, and hence if the caller has not validated
	 * data,it will lead to run-time exceptions
	 *
	 * @return input data specification. null if this is a sub-service, or a
	 *         utility service that may accept anything and everything that has
	 *         come from client
	 */

  public InputData getInputSpecification();

	/**
	 * what data need to be sent as response to the client. Service would have
	 * ensured that the data elements are available in the service context for
	 * the caller to pick them up and prepare a response
	 *
	 * @return output data specification, or null if everything from context is
	 *         to be sent back as response.
	 */
	public OutputData getOutputSpecification();
}
