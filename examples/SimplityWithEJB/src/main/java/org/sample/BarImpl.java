package org.sample;

import java.io.Serializable;

import javax.ejb.Stateless;

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
