package org.sample;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class HelloLogic implements LogicInterface{

	@EJB(name = "some/Foo")
	   private Foo foo;

	
	@Override
	public Value execute(ServiceContext ctx) {
		try {
			InitialContext ic = new InitialContext();
			foo = (Foo)ic.lookup("org.sample.Foo");
			String text = "From HelloLogic: " + foo.tellMeSomething();
			ctx.setTextValue("hello", text);
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return Value.newTextValue("success");
	}

}
