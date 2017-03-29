package org.simplity.examples.ejbIntegration;

import javax.ejb.Local;

@Local
public interface Bar {

    String sayHello();

}
