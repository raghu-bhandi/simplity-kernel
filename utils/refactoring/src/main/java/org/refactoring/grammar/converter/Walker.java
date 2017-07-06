package org.refactoring.grammar.converter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Walker {

	public static void main(String[] args) {
		Path inputDir = new File(args[0]).toPath();
		Path outputDir = new File(args[1]).toPath();
		
		PrintFiles pf = new PrintFiles(inputDir,outputDir);
		try {
			Files.walkFileTree(inputDir, pf);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}


}
