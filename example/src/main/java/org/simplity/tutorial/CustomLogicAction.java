package org.simplity.tutorial;

import java.io.IOException;

import org.simplity.kernel.Tracer;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tutorial.utils.TestUtils;
import org.simplity.tp.LogicInterface;

public class CustomLogicAction implements LogicInterface {

	@Override
	public Value execute(ServiceContext ctx) {
		System.out.println("__CustomLogicAction__");
		ctx.setValue("island", Value.newTextValue("NeverLand"));
		ctx.setValue("leader", Value.newTextValue("Peter Pan"));
		ctx.setValue("gang", Value.newTextValue("Lost Boys"));

		ctx.setValue("adversary1", Value.newTextValue("Captain Hook"));
		ctx.setValue("adversary2", Value.newTextValue("Mr.Smee"));
		ctx.setValue("adversaries", Value.newTextValue(ctx.getValue("adversary1") + "," + ctx.getValue("adversary2")));

		DataSheet sheet = null;
		try {
			sheet = TestUtils.loadDS("data/weekendBoxOffice.csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
		ctx.putDataSheet("weekendBoxOffice", sheet);

		return Value.newTextValue(" from CustomLogicAction");
	}
}
