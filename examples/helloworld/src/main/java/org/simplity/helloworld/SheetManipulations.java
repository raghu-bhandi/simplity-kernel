package org.simplity.helloworld;

import java.util.List;
import java.util.Set;

import org.simplity.helloworld.entity.Orders;
import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class SheetManipulations implements LogicInterface{

	@Override
	public Value execute(ServiceContext ctx) {
		MultiRowsSheet sheet = (MultiRowsSheet) ctx.getDataSheet("orders");
		List<Object> columnList = sheet.columnAsList("ordNum");
		DataSheet returnsheet = MultiRowsSheet.toDatasheet(columnList, "ordNum");
		Set<Object> sheetSet = sheet.toSet("org.simplity.helloworld.entity.Orders");
		for(Object obj:sheetSet){
			Orders order = (Orders)obj;
			System.out.println("Order Number: "+order.getOrdNum());
		}
		ctx.putDataSheet("ordNumSheet", returnsheet);
		return null;
	}

}
