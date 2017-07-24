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

package org.simplity.tp;

import org.simplity.gateway.Gateway;
import org.simplity.gateway.OutboundAgent;
import org.simplity.kernel.ApplicationError;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.InputData;
import org.simplity.service.OutputData;
import org.simplity.service.ServiceContext;

/**
 * base class for all actions that request an external call. This design is make
 * the current service robust against all eventualities of an external call
 *
 * @author simplity.org
 *
 */
public abstract class ExternalService extends Action {
	/**
	 * what if the service fails for whatever reason. Like service is not
	 * available, time-out, network error etc.. Note that this is different from
	 * service actually returning with some error code.
	 */
	Action actionOnServiceFailure;
	/**
	 * what data do we send or this service request?. Data must be available in
	 * the service context as fields/sheets. It is formatted the right way as
	 * pay-load while making this service call.
	 */

	InputData dataToBeReceived;
	/**
	 * what data is expected back? pay-load from the external service is read
	 * and data is extracted into service context
	 */
	OutputData dataToBeSent;
	/**
	 * what is the gateway to be used to make this service call? Gateway is
	 * set-up at the application level
	 */
	String gatewayId;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Action#doAct(org.simplity.service.ServiceContext)
	 */
	@Override
	protected Value delegate(ServiceContext ctx, DbDriver driver) {
		OutboundAgent agent = Gateway.getAgent(this.gatewayId);
		if (agent == null) {
			throw new ApplicationError("Outbound agent is not set-up for " + this.gatewayId);
		}

		if (this.preServe(agent, ctx) == false) {
			return Value.VALUE_TRUE;
		}

		if (agent.serve(ctx, this.dataToBeSent, this.dataToBeReceived) == false) {
			if (this.actionOnServiceFailure != null) {
				this.actionOnServiceFailure.act(ctx, driver);
			}
			return Value.VALUE_FALSE;
		}
		return this.postServe(agent, ctx);
	}

	/**
	 * preparation for the actual service. like setting up path, and other
	 * parameters. This method is invoked before
	 * <code>OutboundAgent.serve</code>
	 *
	 * @param agent
	 * @param ctx
	 * @return true if all ok and the execution should continue. false means
	 *         abort the mission
	 */
	abstract boolean preServe(OutboundAgent agent, ServiceContext ctx);

	/**
	 * anything to be done after service execution before winding-up.
	 *
	 * @param agent
	 * @param ctx
	 * @return value to be returned to service as value of this action
	 */
	abstract Value postServe(OutboundAgent agent, ServiceContext ctx);

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Block#getReady(int)
	 */
	@Override
	public void getReady(int idx, Service service) {
		super.getReady(idx, service);
		if (this.dataToBeSent != null) {
			this.dataToBeSent.getReady();
		}
		if (this.dataToBeReceived != null) {
			this.dataToBeReceived.getReady();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.tp.Block#validate(org.simplity.kernel.comp.
	 * ValidationContext, org.simplity.tp.Service)
	 */
	@Override
	public int validate(ValidationContext vtx, Service service) {
		int count = super.validate(vtx, service);
		if (this.gatewayId == null) {
			vtx.addError("gatewayId is required for an external service action");
			count++;
		}
		return count;
	}
}
