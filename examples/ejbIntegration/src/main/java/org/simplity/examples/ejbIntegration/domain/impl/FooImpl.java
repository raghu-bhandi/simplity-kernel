package org.simplity.examples.ejbIntegration.domain.impl;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.simplity.examples.ejbIntegration.domain.Bar;
import org.simplity.examples.ejbIntegration.domain.Foo;

@Stateless
public class FooImpl implements Foo {

	@EJB
    private Bar bar;

    @Override
    public String tellMeSomething() {
        return "Foo tells you (this is what Bar said): " + bar.sayHello();
    }

}
