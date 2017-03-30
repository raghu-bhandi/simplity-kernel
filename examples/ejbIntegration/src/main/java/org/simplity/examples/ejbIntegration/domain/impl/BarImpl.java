package org.simplity.examples.ejbIntegration.domain.impl;

import java.io.Serializable;

import javax.ejb.Stateless;

import org.simplity.examples.ejbIntegration.domain.Bar;

@Stateless
public class BarImpl implements Bar,Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    public String sayHello() {
        return "Hello, I am from the SAMPLE bar bean!";
    }

}
