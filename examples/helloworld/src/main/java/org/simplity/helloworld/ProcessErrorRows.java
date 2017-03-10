package org.simplity.helloworld;

import java.util.List;
import org.simplity.kernel.FormattedMessage;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class ProcessErrorRows implements LogicInterface {

	@Override
	public Value execute(ServiceContext ctx) {
		System.out.println("Hello");
		List<FormattedMessage> msgs = ctx.getMessages();
		for(FormattedMessage msg:msgs){
			System.out.println(msg.text);
			System.out.println(msg.data[0]);
			System.out.println(msg.fieldName);
		}
		return null;
	}

}
