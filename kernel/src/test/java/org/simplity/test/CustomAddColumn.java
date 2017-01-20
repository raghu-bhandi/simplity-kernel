package org.simplity.test;

import java.io.IOException;

import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.test.TestUtils;
import org.simplity.tp.LogicInterface;

public class CustomAddColumn implements LogicInterface{

	@Override
	public Value execute(ServiceContext ctx) {
		DataSheet sheet = null;
		try {
			sheet = TestUtils.loadDS("weekendBoxOffice");
		} catch (IOException e) {
			e.printStackTrace();
		}
		ctx.putDataSheet("weekendBoxOffice", sheet);

		return Value.newTextValue(" from CustomAddColumn");
	
	}
}
