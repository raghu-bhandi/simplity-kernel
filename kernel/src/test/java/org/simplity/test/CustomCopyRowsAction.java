package org.simplity.test;

import java.io.IOException;

import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.test.TestUtils;
import org.simplity.tp.LogicInterface;

public class CustomCopyRowsAction implements LogicInterface{

	@Override
	public Value execute(ServiceContext ctx) {
		DataSheet fromSheet = null;
		DataSheet toSheet = null;
		try {
			fromSheet = TestUtils.loadDS("weekendBoxOffice");
			toSheet = TestUtils.loadDS("weekendBoxOffice1");
		} catch (IOException e) {
			e.printStackTrace();
		}
		ctx.putDataSheet("weekendBoxOffice", fromSheet);
		ctx.putDataSheet("weekendBoxOffice1", toSheet);

		return Value.newTextValue(" from CustomFilterAction");
	
	}


}
