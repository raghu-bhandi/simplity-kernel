package org.sample;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
public class FooImpl implements Foo {

	@EJB
    private Bar bar;

    @Override
    public String tellMeSomething() {
        return "Foo tells you (this is what Bar said): " + bar.sayHello();
    }

}
