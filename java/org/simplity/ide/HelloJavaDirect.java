package org.simplity.ide;

import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceInterface;

/**
 * Example of a class that implements a full service
 * 
 * @author simplity.org
 *
 */
public class HelloJavaDirect implements ServiceInterface {
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
	 * @see org.simplity.kernel.comp.Component#getReady()
	 */
	@Override
	public void getReady() {
		// We are ever-ready

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
		return Value.VALUE_TRUE;
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
		return true;
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

}
