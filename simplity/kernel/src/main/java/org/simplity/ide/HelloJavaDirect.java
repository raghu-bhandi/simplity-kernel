package org.simplity.ide;

import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.AbstractService;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceData;

/**
 * Example of a class that implements a full service
 *
 * @author simplity.org
 *
 */
public class HelloJavaDirect extends AbstractService {
	private static final String MY_NAME = "JamesBond";

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getSimpleName()
	 */
	@Override
	public String getSimpleName() {
		return MY_NAME;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.kernel.comp.Component#getQualifiedName()
	 */
	@Override
	public String getQualifiedName() {
		return MY_NAME;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.simplity.service.ServiceInterface#executeAsAction(org.simplity.
	 * service
	 * .ServiceContext, org.simplity.kernel.db.DbDriver)
	 */
	@Override
	public Value executeAsAction(ServiceContext ctx, DbDriver driver, boolean transactionIsDelegated) {
		Value value = Value.newTextValue("Hellooooo directly from Java");
		ctx.setValue("hello", value);
		return value;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.simplity.service.ServiceInterface#respond(org.simplity.service.
	 * ServiceData)
	 */
	@Override
	public ServiceData respond(ServiceData inputData) {
		ServiceData outData = new ServiceData();
		outData.setPayLoad("{\"hello\":\"Hellooooo directly from Java\"}");
		return outData;
	}
}
