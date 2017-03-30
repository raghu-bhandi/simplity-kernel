package org.simplity.examples.ejbIntegration.domain;

import javax.ejb.Local;

@Local
public interface Bar {

    String sayHello();

}
