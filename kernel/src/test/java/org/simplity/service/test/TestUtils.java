package org.simplity.service.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.simplity.kernel.data.DataSheet;
import org.simplity.kernel.data.MultiRowsSheet;
import org.simplity.kernel.value.Value;
import org.simplity.kernel.value.ValueType;

public class TestUtils {
	final static String DATA_PATH = "src/test/java/resources/data/";

	public static Set<String> listFiles(File folder) {
		Set<String> filePaths = new HashSet<String>();
		for (File fileItem : folder.listFiles()) {
			filePaths.add(fileItem.getAbsolutePath());
		}
		return filePaths;
	}

	public static String getFile(String mandPath, String... appendPath) {
		StringBuilder appendPathSb = new StringBuilder();
		if (appendPath != null) {
			for (String subPath : appendPath) {
				appendPathSb.append(subPath);
			}
		}
		appendPathSb.append(mandPath);
		return new File(appendPathSb.toString()).getAbsolutePath();
	}

	public static DataSheet loadDS(String dsname) throws IOException {
		DataSheet ds = null;
		File dsFile = new File(DATA_PATH, dsname + ".csv");
		BufferedReader bf = new BufferedReader(new FileReader(dsFile));
		String dataRow = "";

		int i = 0;
		while (dataRow != null) {
			dataRow = bf.readLine();
			i++;
			if (dataRow != null)
				// for first row, read the header and assign
				if (i == 1) {
					String dataHeader[] = dataRow.split(",");
					ValueType[] types = new ValueType[dataHeader.length];
					Arrays.fill(types, ValueType.TEXT);
					ds = new MultiRowsSheet(dataHeader, types);
					continue;
				} else {
					convertText2Value(dataRow.split(","), ds);
				}
		}
		return ds;

	}

	public static void convertText2Value(String[] rowValues, DataSheet ds) {
		ArrayList<Value> valueList = new ArrayList<Value>();
		for (String row : rowValues) {
			valueList.add(Value.newTextValue(row));
		}
		if (!valueList.isEmpty())
			ds.addRow(valueList.toArray(new Value[valueList.size()]));

	}
}
