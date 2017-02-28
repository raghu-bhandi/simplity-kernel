package org.simplity.utils.torque.outlets.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.torque.generator.GeneratorException;
import org.apache.torque.generator.control.ControllerState;
import org.apache.torque.generator.outlet.OutletImpl;
import org.apache.torque.generator.outlet.OutletResult;
import org.apache.torque.generator.qname.QualifiedName;
import org.apache.torque.generator.source.SourceElement;
import org.simplity.kernel.dm.Field;
import org.simplity.kernel.dm.FieldType;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.ValueType;
import org.simplity.utils.utils.Utils;

public class SimplityRecordContent extends OutletImpl {
	public SimplityRecordContent(QualifiedName name) {
		super(name);
	}

	@Override
	public OutletResult execute(ControllerState controllerState) throws GeneratorException {
		SourceElement root = controllerState.getSourceElement();
		Record record = new Record();
		String recordName = root.getAttribute("name").toString();
		record.setQualifiedName(Utils.toCamelCase(recordName));
		record.setTableName(recordName);

		Map<String, List<ForeignKey>> foreignKeyList = new HashMap<String, List<ForeignKey>>();
		List<SourceElement> foreignKeys = root.getChildren("foreign-key");
		for (SourceElement foreignKey : foreignKeys) {
			String foreignTable = foreignKey.getAttribute("foreignTable").toString();
			List<SourceElement> references = foreignKey.getChildren("reference");
			for (SourceElement reference : references) {
				ForeignKey fk = new ForeignKey(foreignTable, reference.getAttribute("foreign").toString());
				String local = reference.getAttribute("local").toString();
				List<ForeignKey> fkList = foreignKeyList.get(local);
				if (fkList == null) {
					fkList = new ArrayList<ForeignKey>();
				}
				fkList.add(fk);
				foreignKeyList.put(local, fkList);
			}
		}

		List<SourceElement> columns = root.getChildren("column");
		Field[] fields = new Field[columns.size()];
		for (int j = 0; j < fields.length; j++) {
			Field field = new Field();
			fields[j] = field;
			SourceElement column = columns.get(j);
			String nam = column.getAttribute("name").toString();
			field.setName(Utils.toCamelCase(nam));
			field.setColumnName(nam);
			String sqlTypeName = column.getAttribute("type").toString();
			field.setSqlTypeName(sqlTypeName);
			String dataType = null;
			try{
				dataType = ValueType.getValueType(sqlTypeName).getDefaultDataType();			
			}catch(NullPointerException e){
				if(sqlTypeName.equals("CHAR")){
					dataType="_text";
				}
			}
			field.setDataType(dataType);
			if (column.getAttribute("primaryKey") != null) {
				field.setFieldType(FieldType.PRIMARY_KEY);
			}
			// set the reference details
			List<ForeignKey> fkList = new ArrayList<ForeignKey>();
			if ((fkList = foreignKeyList.get(nam)) != null) {
				field.setFieldType(FieldType.PARENT_KEY);
				ForeignKey fk = (ForeignKey) (fkList.get(0));
				field.setReferredRecord(Utils.toCamelCase(fk.foreignTable));
				field.setReferredField(Utils.toCamelCase(fk.foreignKey));
			}

		}
		record.setFields(fields);
		return new OutletResult(XmlUtil.objectToXmlString(record));
	}

	class ForeignKey {
		String foreignTable;
		String foreignKey;

		public ForeignKey(String foreignTable, String foreignKey) {
			super();
			this.foreignTable = foreignTable;
			this.foreignKey = foreignKey;
		}

	}
}
