//
// OrcCompiler.java -- Java class OrcCompiler
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.Callable;

import orc.ast.extended.ASTNode;
import orc.ast.extended.Declare;
import orc.ast.extended.declaration.Declaration;
import orc.ast.oil.Expr;
import orc.ast.oil.SiteResolver;
import orc.ast.oil.UnguardedRecursionChecker;
import orc.ast.oil.xml.Oil;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.ParsingException;
import orc.error.compiletime.CompileMessageRecorder.Severity;
import orc.parser.OrcParser;
import orc.progress.ProgressListener;
import orc.type.Type;

/**
 * Provides Orc's compilation functions.
 * <p>
 * To use, construct with an Orc config, then invoke {@link #call()}
 */
public class OrcCompiler implements Callable<Expr> {

	private final Config config;

	/**
	 * Constructs an object of class OrcCompiler.
	 *
	 * @param config The Orc configuration to use for compilation
	 */
	public OrcCompiler(final Config config) {
		if (config == null) {
			throw new NullPointerException("Cannot construct OrcCompiler with a null config");
		}
		this.config = config;
	}

	/**
	 * Run the compiler, using the configuration supplied to the constructor.
	 * 
	 * @return OIL AST for the compiled Orc program
	 * @see java.util.concurrent.Callable#call()
	 */
	public Expr call() throws IOException {
		try {
			Expr oilAst = null;
			config.getMessageRecorder().beginProcessing(new File(config.getInputFilename()));
			if (config.hasOilInputFile()) {

				oilAst = loadOil(config.getOilReader());

			} else if (config.hasInputFile()) {

				final ASTNode astRoot = parse(config.getReader());

				oilAst = compileAstToOil(astRoot);

				oilAst = refineOilAfterCompileBeforeSave(oilAst);

			} else {
				throw new IllegalStateException("Cannot compile without an input file specified");
			}

			if (config.hasOilOutputFile()) {

				saveOil(oilAst, config.getOilWriter());

				config.getOilWriter().close();
			}

			oilAst = refineOilAfterLoadSaveBeforeDag(oilAst);

			config.getMessageRecorder().endProcessing(new File(config.getInputFilename()));
			return oilAst;
		} catch (final CompilationException e) {
			config.getMessageRecorder().recordMessage(Severity.FATAL, 0, e.getMessageOnly(), e.getSourceLocation(), null, e);
		}
		return null;
	}

	/**
	 * Parse the Orc program text supplied by the reader into an Orc extended AST.
	 * 
	 * @param sourceReader Reader that supplies the Orc source program text
	 * @return Orc Extended AST corresponding to the supplied text
	 * @throws ParsingException If the text could not be successfully parsed
	 * @throws IOException If an include file could not be read
	 */
	public ASTNode parse(final Reader sourceReader) throws ParsingException, IOException {
		final ProgressListener progress = config.getProgressListener();

		// Parse the goal expression
		progress.setNote("Parsing");
		final OrcParser parser = new OrcParser(config, sourceReader, config.getInputFilename());
		orc.ast.extended.Expression e = parser.parseProgram();
		if (progress.isCanceled()) {
			return null;
		}
		progress.setProgress(0.3);

		//System.out.println(e);

		//System.out.println("Importing declarations...");
		progress.setNote("Parsing include files");
		final LinkedList<Declaration> decls = new LinkedList<Declaration>();

		if (!config.getNoPrelude()) {
			// Load declarations from the default include file.
			final String preludename = "prelude.inc";
			final OrcParser fparser = new OrcParser(config, config.openInclude(preludename, null), preludename);
			decls.addAll(fparser.parseModule());
		}
		if (progress.isCanceled()) {
			return null;
		}
		// Load declarations from files specified by the configuration options
		for (final String f : config.getIncludes()) {
			final OrcParser fparser = new OrcParser(config, config.openInclude(f, null), f);
			decls.addAll(fparser.parseModule());
			if (progress.isCanceled()) {
				return null;
			}
		}
		if (progress.isCanceled()) {
			return null;
		}
		progress.setProgress(0.6);

		// Add the declarations to the parse tree
		Collections.reverse(decls);
		for (final Declaration d : decls) {
			e = new Declare(d, e);
		}
		return e;
	}

	/**
	 * Translate an Orc extended AST into an OIL AST
	 * 
	 * @param astRoot Root node of the Orc extended AST
	 * @return OIL AST corresponding to the supplied extended AST
	 * @throws CompilationException If the AST contains compilation errors
	 */
	public Expr compileAstToOil(final ASTNode astRoot) throws CompilationException {
		final ProgressListener progress = config.getProgressListener();

		progress.setNote("Simplifying the AST");
		//System.out.println("Simplifying the abstract syntax tree...");
		// Simplify the AST
		Expr ex = ((orc.ast.extended.Expression) astRoot).simplify().convert(new Env<Var>(), new Env<String>());
		// System.out.println(ex);
		if (progress.isCanceled()) {
			return null;
		}
		progress.setProgress(0.7);

		progress.setNote("Resolving sites");
		ex = SiteResolver.resolve(ex, config);
		if (progress.isCanceled()) {
			return null;
		}
		progress.setProgress(0.8);

		// Optionally perform typechecking
		if (config.getTypeChecking()) {
			progress.setNote("Typechecking");

			final Type rt = ex.typesynth(new Env<Type>(), new Env<Type>());
			config.getStdout().println("... :: " + rt);
			config.getStdout().println("Program typechecked successfully.");
			config.getStdout().println();
		}

		if (progress.isCanceled()) {
			return null;
		}
		progress.setNote("Checking for unguarded recursion");
		UnguardedRecursionChecker.check(ex);

		if (progress.isCanceled()) {
			return null;
		}
		progress.setProgress(1.0);
		return ex;
	}

	/**
	 * Read an OIL file into an OIL AST and resolve the sites.
	 * 
	 * @param oilReader Reader supplying the OIL file to be loaded
	 * @return OIL AST corresponding to the file
	 * @throws IOException If the file could not be read
	 * @throws CompilationException If the sites on the OIL could not be resolved
	 */
	public Expr loadOil(final Reader oilReader) throws IOException, CompilationException {
		final ProgressListener progress = config.getProgressListener();
		progress.setNote("Loading OIL");
		final Oil oil = Oil.fromXML(config.getOilReader());
		progress.setProgress(0.2);
		if (progress.isCanceled()) {
			return null;
		}
		progress.setNote("Converting to AST");
		final Expr ex = oil.unmarshal(config);
		progress.setProgress(0.5);
		if (progress.isCanceled()) {
			return null;
		}
		progress.setNote("Loading sites");
		return SiteResolver.resolve(ex, config);
	}

	/**
	 * Write an OIL AST into an OIL file
	 * 
	 * @param oilAst OIL AST to be saved
	 * @param oilWriter Destination for OIL file
	 * @throws CompilationException If the OIL AST could not be marshaled for saving
	 */
	public void saveOil(final Expr oilAst, final Writer oilWriter) throws CompilationException {
		final ProgressListener progress = config.getProgressListener();
		progress.setNote("Writing OIL");
		new Oil(oilAst).toXML(oilWriter);
	}

	/**
	 * Subclass hook for modifying the OIL AST before it is saved to an OIL XML file
	 * and before the OIL AST is run.  This hook is not called for loaded OIL XML files.
	 * 
	 * @param oilAst OIL AST generated from Orc source text
	 * @return Refined OIL AST to be saved (and run)
	 */
	protected Expr refineOilAfterCompileBeforeSave(final Expr oilAst) {
		// Override and extend
		return oilAst;
	}

	/**
	 * Subclass hook for modifying the OIL AST after it is loaded from an OIL XML file
	 * (or generated from source code) and before the OIL AST is run.
	 * This hook is called for loaded OIL XML files.
	 * 
	 * @param oilAst OIL AST read from file or compiled from text
	 * @return Refined OIL AST to be run
	 */
	protected Expr refineOilAfterLoadSaveBeforeDag(final Expr oilAst) {
		// Override and extend
		return oilAst;
	}
}
