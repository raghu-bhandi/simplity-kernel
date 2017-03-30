package org.simplity.examples.ejbIntegration.domain;

import java.io.Serializable;

import javax.ejb.Remote;

@Remote
public interface Foo extends Serializable{
	static final long serialVersionUID = 1L;

    String tellMeSomething();

}
