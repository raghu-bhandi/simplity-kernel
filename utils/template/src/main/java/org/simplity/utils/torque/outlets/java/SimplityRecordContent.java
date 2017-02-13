package org.simplity.utils.torque.outlets.java;

import java.util.List;

import org.apache.torque.generator.GeneratorException;
import org.apache.torque.generator.control.ControllerState;
import org.apache.torque.generator.outlet.OutletImpl;
import org.apache.torque.generator.outlet.OutletResult;
import org.apache.torque.generator.qname.QualifiedName;
import org.apache.torque.generator.source.SourceElement;
import org.simplity.kernel.dm.Field;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.util.XmlUtil;

public class SimplityRecordContent extends OutletImpl {
	public SimplityRecordContent(QualifiedName name) {
		super(name);
	}
	@Override
	public OutletResult execute(ControllerState controllerState) throws GeneratorException {
		SourceElement root = controllerState.getSourceElement();
		Record record = new Record();
		String recordName = root.getAttribute("name").toString();
		record.setQualifiedName(recordName);
		record.setModuleName(recordName);
		record.setTableName(recordName);
		List<SourceElement> columns = root.getChildren("column");
		Field[] fields = new Field[columns.size()];
		for (int j = 0; j < fields.length; j++) {
			Field field = new Field();
			fields[j] = field;
			SourceElement column = columns.get(j);
			String nam = column.getAttribute("name").toString();
			field.setColumnName(nam);

			String sqlTypeName = column.getAttribute("type").toString();
			field.setSqlTypeName(sqlTypeName);
			field.setDataType(sqlTypeName);
			field.setNullable(true);
			field.setRequired(!field.isNullable());
		}
		record.setFields(fields);
		return new OutletResult(XmlUtil.objectToXmlString(record));		
	}
}
