package org.simplity.ide;

import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

/**
 * An example logic action that list all tables and stored procedures defined in
 * rdbms
 *
 * @author simplity.org
 *
 */
public class GetTables implements LogicInterface {

	@Override
	public Value execute(ServiceContext ctx) {
		DataSheet sheet = DbDriver.getTables(null, null);
		ctx.putDataSheet("tables", sheet);
		sheet = DbDriver.getProcedures(null, null);
		ctx.putDataSheet("procs", sheet);
		return Value.VALUE_TRUE;
	}
}
