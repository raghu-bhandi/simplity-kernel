package org.refactoring.grammar.converter;

import static java.nio.file.FileVisitResult.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.google.googlejavaformat.java.FormatterException;

public class PrintFiles extends SimpleFileVisitor<Path> {

	Path outputDir;
	Path inputDir;

	public PrintFiles(Path inputDir, Path outputDir) {
		this.outputDir = outputDir;
		this.inputDir = inputDir;

		this.outputDir.toFile().mkdir();
	}

	// Print information about
	// each type of file.
	@Override
	public FileVisitResult visitFile(Path infile, BasicFileAttributes attr) throws IOException {
		if (attr.isRegularFile()) {

			if (inputDir.getNameCount() < infile.getNameCount() - 1)
				new File(outputDir.resolve(infile.subpath(inputDir.getNameCount(), infile.getNameCount() - 1))
						.toString()).mkdirs();
			File outfile = new File(
					outputDir.resolve(infile.subpath(inputDir.getNameCount(), infile.getNameCount())).toString());
			outfile.createNewFile();

			if (infile.toFile().toString().endsWith(".java"))
				try {
					Converter.convert(infile, outfile.toPath());
				} catch (FormatterException e) {
					e.printStackTrace();
				}

			System.out.format("Regular file: %s ",
					outputDir.resolve(infile.subpath(inputDir.getNameCount(), infile.getNameCount()))+"\n");
		}
		return CONTINUE;
	}

	// If there is some error accessing
	// the file, let the user know.
	// If you don't override this method
	// and an error occurs, an IOException
	// is thrown.
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) {
		System.err.println(exc);
		return CONTINUE;
	}
}