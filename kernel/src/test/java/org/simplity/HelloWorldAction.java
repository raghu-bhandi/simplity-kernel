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
		Value value = Value.newTextValue("11111");
		ctx.setValue("hello", value);
		
		Value value1 = Value.newTextValue("222222");
		ctx.setValue("hello1", value1);
		
		Value value2 = Value.newTextValue("33333");
		ctx.setValue("hello2", value2);
		
		Value value3 = Value.newTextValue("444444");
		ctx.setValue("hello1", value3);
		
		
		return value;

	}
}
