package org.refactoring.grammar.converter;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.refactoring.grammar.parser.JavaParser.ClassDeclarationContext;
import org.refactoring.grammar.parser.JavaParser.CompilationUnitContext;
import org.refactoring.grammar.parser.JavaParser.EnumDeclarationContext;
import org.refactoring.grammar.parser.JavaParser.ImportDeclarationContext;
import org.refactoring.grammar.parser.JavaParser.PackageDeclarationContext;
import org.refactoring.grammar.parser.JavaParser.StatementExpressionContext;
import org.refactoring.grammar.parser.JavaParser.TypeDeclarationContext;

public class ConverterListener extends AbstractJavaListener {
	TokenStreamRewriter rewriter;
	Token startImportToken;
	Token startDeclToken;
	boolean flagImportDecDone = false;
	private TerminalNode name;

	public ConverterListener(TokenStream tokens) {
		rewriter = new TokenStreamRewriter(tokens);
	}

	@Override
	public void exitPackageDeclaration(PackageDeclarationContext ctx) {

		startImportToken = ctx.stop;

		for (TypeDeclarationContext typeDecls : ((CompilationUnitContext) (ctx.getParent())).typeDeclaration()) {
			if (typeDecls.classDeclaration() != null) {
				ClassDeclarationContext classCtx = typeDecls.classDeclaration();
				startDeclToken = classCtx.classBody().start;
				name = classCtx.Identifier();
				break;
			}
			if (typeDecls.enumDeclaration() != null) {
				EnumDeclarationContext enumCtx = typeDecls.enumDeclaration();
				if (enumCtx.enumBodyDeclarations() != null) {
					startDeclToken = enumCtx.enumBodyDeclarations().start;
					name = enumCtx.Identifier();
				}
				break;
			}
		}

	}

	@Override
	public void enterStatementExpression(StatementExpressionContext ctx) {

		if (ctx.getText().startsWith("Tracer.trace")) {
			if (ctx.expression().expressionList().expression().size() == 1) {
				String logStatement = "\nlogger.log(Level.INFO, " + ctx.expression().expressionList().getText()
						+ " );\n";
				rewriter.insertBefore(ctx.start, logStatement);
			}
			if (ctx.expression().expressionList().expression().size() == 2) {
				String logStatement = "\nlogger.log(Level.SEVERE, " 
									 + ctx.expression().expressionList().expression(1).getText()
									 + ","
									 + ctx.expression().expressionList().expression(0).getText()
									 + " );\n";
				rewriter.insertBefore(ctx.start, logStatement);
			}

			if (!flagImportDecDone) {
				// add the required imports
				String field = "\nimport org.slf4j.Logger;";
				rewriter.insertAfter(startImportToken, field);
				field = "\nimport java.util.logging.Logger;";
				rewriter.insertAfter(startImportToken, field);

				// add the required logger declaration
				field = "\nfinal static Logger logger = Logger.getLogger(" + name + ".class.getName());\n";
				rewriter.insertAfter(startDeclToken, field);
				
				flagImportDecDone = true;
			}
		}
	}

}
