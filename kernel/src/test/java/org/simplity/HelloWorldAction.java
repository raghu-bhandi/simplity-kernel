package org.simplity;

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
		Value value = Value.newTextValue("Hellooooo World from a Java class 1234sdfas !!");
		ctx.setValue("hello", value);
		return value;

	}
}
