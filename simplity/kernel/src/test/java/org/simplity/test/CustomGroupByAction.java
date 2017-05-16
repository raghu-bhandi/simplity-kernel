package org.simplity.test;

import java.io.IOException;

import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.service.test.TestUtils;
import org.simplity.tp.LogicInterface;

public class CustomGroupByAction implements LogicInterface {
	final String id = "id";
	final String name = "name";
	final String address = "address";
	final String score = "score";

	final String out_id = "out_id";
	final String out_name = "out_name";
	final String out_address = "out_address";
	final String out_total = "out_total";

	final String bk_id = "bk_id";
	final String bk_name = "bk_name";
	final String bk_address = "bk_address";
	final String bk_score = "bk_score";

	final String aggField = "score";
	final String totalAagField = "total";

	@Override
	public Value execute(ServiceContext ctx) {
		ctx.setBooleanValue("WriteOK", false);
		if (!checkIfBkCtxExists(ctx)) {
			ctx.setValue(bk_id, ctx.getValue(id));
			ctx.setValue(bk_name, ctx.getValue(name));
			ctx.setValue(bk_address, ctx.getValue(address));
			ctx.setValue(bk_score, ctx.getValue(score));
			ctx.setLongValue(totalAagField, ctx.getLongValue(score));
			return Value.newTextValue("First Record");
		} else {
			if (ctx.getValue(bk_id).equals(ctx.getValue(id)) && ctx.getValue(bk_name).equals(ctx.getValue(name))) {
				long agg = ctx.getLongValue(aggField);
				long total = ctx.getLongValue(totalAagField);
				ctx.setLongValue(totalAagField, agg + total);
			} else {
				ctx.setValue(out_id, ctx.getValue(bk_id));
				ctx.setValue(out_name, ctx.getValue(bk_name));
				ctx.setValue(out_address, ctx.getValue(bk_address));
				ctx.setValue(out_total, ctx.getValue(totalAagField));

				ctx.setValue(bk_id, ctx.getValue(id));
				ctx.setValue(bk_name, ctx.getValue(name));
				ctx.setValue(bk_address, ctx.getValue(address));
				ctx.setValue(bk_score, ctx.getValue(score));
				ctx.setLongValue(totalAagField, ctx.getLongValue(score));

				ctx.setBooleanValue("WriteOK", true);
			}
		}
		return Value.newTextValue(" from GroupBy");
	}

	private boolean checkIfBkCtxExists(ServiceContext ctx) {
		if (!Value.isNull(ctx.getValue(bk_id)) && !Value.isNull(ctx.getValue(bk_id))) {
			return true;
		}
		return false;
	}

}
