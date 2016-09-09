package org.simplity.tutorial;

import org.simplity.kernel.comp.ComponentType;
import org.simplity.kernel.comp.ValidationContext;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.ServiceData;
import org.simplity.service.ServiceInterface;

/**
 * @author admin
 *
 */
public class HelloJavaDirect implements ServiceInterface {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.comp.Component#getSimpleName()
	 */
	@Override
	public String getSimpleName() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.comp.Component#getQualifiedName()
	 */
	@Override
	public String getQualifiedName() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.kernel.comp.Component#getReady()
	 */
	@Override
	public void getReady() {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.service.ServiceInterface#toBeRunInBackground()
	 */
	@Override
	public boolean toBeRunInBackground() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.service.ServiceInterface#okToCache()
	 */
	@Override
	public boolean okToCache() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.service.ServiceInterface#getDataAccessType()
	 */
	@Override
	public DbAccessType getDataAccessType() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.simplity.service.ServiceInterface#respond(org.simplity.service.
	 * ServiceData)
	 */
	@Override
	public ServiceData respond(ServiceData inputData) {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
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
