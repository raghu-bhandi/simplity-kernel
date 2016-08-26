package org.simplity.service.test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class TestUtils {

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

}
