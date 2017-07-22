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

package org.simplity.gateway;

import org.simplity.service.InputData;
import org.simplity.service.OutputData;
import org.simplity.service.ServiceContext;

/**
 * Service agent to get response for an external service that a simplity service
 * wants as part of its execution step
 *
 * @author simplity.org
 *
 */
public interface OutboundAgent {
	/**
	 *
	 * @param serviceId
	 *            this is actual service name in case the server is a simplity
	 *            server, or it could be URL for a http request etc..
	 * @param ctx
	 *            service context in which the caller service is executing
	 * @param dataTobeSent
	 *            what data needs to be sent along with this request. if null,
	 *            no need to send any more data. However, in case of standards
	 *            like swagger, agent may be equipped to extract whatever data
	 *            is required as per the swagger specification
	 * @param dataToBeReceived
	 *            what data is expected back. If null, it is left to the agent
	 *            to probably extract all the data into ctx
	 * @return true if all OK, and the response received from the service is put
	 *         back into the service context. false in case of any problem in
	 *         locating, contacting or getting the response from the server that
	 *         is known to serve this service. Error message is added to the
	 *         service context
	 */
	public boolean serve(String serviceId, ServiceContext ctx, OutputData dataTobeSent, InputData dataToBeReceived);
}
