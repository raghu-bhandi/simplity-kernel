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

import java.util.Map;

import org.simplity.kernel.ApplicationError;

/**
 * static class to manage all aspects of a gateway
 *
 * @author simplity.org
 *
 */
public abstract class Gateway {

	private static Map<String, Gateway> gateways;
	/**
	 * get the right agent who knows how to handle service request to the
	 * desired server
	 *
	 * @param gatewayId
	 *            application specific name that identifies the server to
	 *            which the request needs to be sent to
	 * @return null if no such serverId is set-up
	 */
	public static OutboundAgent getAgent(String gatewayId) {
		Gateway gateway = gateways.get(gatewayId);
		if(gateway == null){
			throw new ApplicationError("Gatewy " + gatewayId + " is not set up");
		}
		return gateway.getAgentInstance();

	}

	/**
	 * name of this gateway
	 */
	String name;

	/**
	 *
	 * @return name of this gateway
	 */
	public String getName(){
		return this.name;
	}
	/**
	 * @return
	 */
	protected abstract OutboundAgent getAgentInstance();
}
