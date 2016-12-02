package com.infosys.qreuse.smtpservice;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.db.DbAccessType;
import org.simplity.kernel.db.DbDriver;
import org.simplity.kernel.value.Value;
import org.simplity.service.ServiceContext;
import org.simplity.tp.LogicInterface;

public class AuditTrail implements LogicInterface {


	private String saveSql = "INSERT INTO AUDITTRAIL (fromId,toId,subject,mail) values (?,?,?,?)";

	@Override
	public Value execute(ServiceContext ctx) {
		boolean allOk = true;
		long generatedKey = 0;
		Connection con = DbDriver.getConnection(DbAccessType.READ_WRITE, null);
		try {
			PreparedStatement stmt = con.prepareStatement(this.saveSql);
			stmt.setString(1, ctx.getTextValue("fromId"));
			stmt.setString(2, ctx.getTextValue("toIds"));
			stmt.setString(3, ctx.getTextValue("subject"));
			stmt.setBinaryStream(4, (InputStream) ctx.getObject("mail"));
			stmt.executeUpdate();
			stmt.close();
		} catch (Exception e) {
			Tracer.trace(e, "Error while writing audit trails");
			allOk = false;
		} finally {
			DbDriver.closeConnection(con, DbAccessType.READ_WRITE, allOk);
		}
		if (generatedKey == 0) {
			return null;
		}
		return Value.newTextValue("" + generatedKey);
	
		
	}

}
