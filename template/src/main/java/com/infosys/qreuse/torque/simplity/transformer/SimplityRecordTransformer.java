package com.infosys.qreuse.torque.simplity.transformer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.torque.generator.control.ControllerState;
import org.apache.torque.generator.source.SourceElement;
import org.apache.torque.generator.source.transform.SourceTransformer;
import org.apache.torque.generator.source.transform.SourceTransformerException;
import org.simplity.kernel.Tracer;
import org.simplity.kernel.dm.Field;
import org.simplity.kernel.dm.Record;
import org.simplity.kernel.dt.DataTypeSuggester;
import org.simplity.kernel.util.XmlUtil;
import org.simplity.kernel.value.BooleanValue;
import org.simplity.kernel.value.Value;

public class SimplityRecordTransformer implements SourceTransformer {
	/** Default database name if none is set. */
	private static final String DEFAULT_DATABASE_NAME = "default";

	public SourceElement transform(SourceElement toTransformRoot, ControllerState controllerState)
			throws SourceTransformerException {
		DataTypeSuggester suggester = new DataTypeSuggester();
		List<SourceElement> tables = toTransformRoot.getChildren("table");
		SourceElement root = new SourceElement("root");
		for (int i = 1; i < tables.size(); i++) {
			String recordName = tables.get(i).getAttribute("name").toString();
			Record record = new Record();
			record.setQualifiedName(recordName);
			record.setModuleName(recordName);
			record.setTableName(recordName);
			List<SourceElement> columns = tables.get(i).getChildren("column");
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
			File file = new File("D:/" + recordName + ".xml");
			OutputStream out = null;
			try {
				if (file.exists() == false) {
					file.createNewFile();
				}
				out = new FileOutputStream(file);
				XmlUtil.objectToXml(out, record);
			} catch (Exception e) {
				continue;
			} finally {
				if (out != null) {
					try {
						out.close();
					} catch (Exception ignore) {
						//
					}
				}
			}
		}
		return toTransformRoot;
	}
}
