package org.refactoring.grammar.converter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

// import ANTLR's runtime libraries
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.refactoring.grammar.parser.JavaLexer;
import org.refactoring.grammar.parser.JavaParser;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;

public class Converter {
	public static void convert(Path inpath, Path outpath) throws IOException, FormatterException {
		CharStream input = CharStreams.fromPath(inpath);
		// create a lexer that feeds off of input CharStream
		JavaLexer lexer = new JavaLexer(input);
		// create a buffer of tokens pulled from the lexer
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		// create a parser that feeds off the tokens buffer
		JavaParser parser = new JavaParser(tokens);
		ParseTree tree = parser.compilationUnit();

		ParseTreeWalker walker = new ParseTreeWalker(); // create standard
														// walker
		ConverterListener extractor = new ConverterListener(tokens);
		walker.walk(extractor, tree); // initiate walk of tree with listener

		FileWriter fw = new FileWriter(outpath.toFile());
		String formattedSource = new Formatter().formatSource(extractor.rewriter.getText());
		fw.write(formattedSource);
		fw.close();
	}

	public static void main(String[] args) throws IOException, FormatterException {
		File infile = new File(args[0]);
		File outfile = new File(args[1]);
		if (!outfile.exists()) {
			outfile.createNewFile();
		}
		Converter.convert(infile.toPath(), outfile.toPath());
	}
}