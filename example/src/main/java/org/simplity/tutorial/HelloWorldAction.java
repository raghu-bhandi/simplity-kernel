package org.simplity.tutorial;

import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

/**
 * This is how we say "Hello World"
 *
 * @author James Bond
 *
 */
public class HelloWorldAction implements LogicInterface {

	@Override
	public Value execute(ServiceContext ctx) {
		ctx.setValue("hello", Value.newTextValue("Helloooo World"));
		return Value.VALUE_TRUE;
	}
}
