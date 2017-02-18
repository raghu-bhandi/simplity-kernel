package org.simplity.kernel.jms;

import org.simplity.kernel.ApplicationError;

public class JmsSetup {
	String jmss;
	String destinationName;
	String connectionFactory;
	String acknowledgeMode;
	String transacted;
	String persistent;
	String priority;
	String timeToLive;
	String initialContext;
	String providerUrl;

	protected JmsSetup() {
	}

	void getReady() {
		if (connectionFactory == null)
			throw new ApplicationError("connectionFactory property is not set, this is a required attribute");

		if (destinationName == null)
			throw new ApplicationError("destinationName property is not set, this is a required attribute");
		if (acknowledgeMode == null)
			throw new ApplicationError("acknowledgeMode is not set, this is required attribute");

		if (persistent == null)
			throw new ApplicationError("persistent is not set, this is required attribute");

		if (priority == null)
			throw new ApplicationError("priority is not set, this is required attribute");

		if (timeToLive == null)
			throw new ApplicationError("timeToLive is not set, this is required attribute");

		if (transacted == null)
			throw new ApplicationError("transacted is not set, this is required attribute");
	}
}
