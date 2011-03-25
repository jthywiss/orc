package orc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import orc.ast.extended.Declare;
import orc.ast.extended.declaration.Declaration;
import orc.ast.oil.Compiler;
import orc.ast.oil.Expr;
import orc.ast.oil.SiteResolver;
import orc.ast.oil.UnguardedRecursionChecker;
import orc.ast.oil.xml.Oil;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.error.compiletime.CompilationException;
import orc.parser.OrcParser;
import orc.progress.NullProgressListener;
import orc.progress.ProgressListener;
import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Pub;
import orc.type.Type;

/**
 * Main class for Orc. Parses Orc file and executes it.
 * 
 * <p>Run with the argument "-help" to get a list of command-line options.
 * 
 * @author wcook, dkitchin
 */
public class Orc {

	/**
	 * 
	 * Orc toplevel main function. Command line arguments are forwarded to Config for parsing.
	 */
	public static void main(String[] args) {
		
		// Read configuration options from the environment and the command line
		Config cfg = new Config();
		cfg.processArgs(args);	
		
		Node n;
		try {
			final Expr ex;
			if (cfg.hasOilInputFile()) {
				Oil oil = Oil.fromXML(cfg.getOilReader());
				ex = SiteResolver.resolve(oil.unmarshal(cfg), cfg);
			} else {
				ex = compile(cfg.getReader(), cfg);
			}
			if (cfg.hasOilOutputFile()) {
				Writer out = cfg.getOilWriter();
				new Oil(ex).toXML(out);
				out.close();
			}
			n = Compiler.compile(ex, new Pub());
		} catch (CompilationException e) {
			System.err.println(e);
			return;
		} catch (IOException e) {
			System.err.println(e);
			return;
		}
        
		// Configure the runtime engine
		OrcEngine engine = new OrcEngine(cfg);
		
		// Run the Orc program
		engine.run(n);
	}
	
	public static Expr compile(Reader source, Config cfg) throws IOException, CompilationException {
		return compile(source, cfg, NullProgressListener.singleton);
	}
	
	public static Expr compile(Reader source, Config cfg, ProgressListener progress) throws IOException, CompilationException {

		//System.out.println("Parsing...");
		// Parse the goal expression
		progress.setNote("Parsing");
		OrcParser parser = new OrcParser(cfg, source, cfg.getInputFilename());
		orc.ast.extended.Expression e = parser.parseProgram();
		if (progress.isCanceled()) return null;
		progress.setProgress(0.3);
		
		
		//System.out.println(e);
		
		//System.out.println("Importing declarations...");
		LinkedList<Declaration> decls = new LinkedList<Declaration>();
		
		progress.setNote("Parsing include files");
		if (!cfg.getNoPrelude()) {
			// Load declarations from the default include file.
			String preludename = "prelude.inc";
			OrcParser fparser = new OrcParser(cfg, cfg.openInclude(preludename), preludename);
			decls.addAll(fparser.parseModule());
		}
		if (progress.isCanceled()) return null;
		// Load declarations from files specified by the configuration options
		for (String f : cfg.getIncludes()) {
			OrcParser fparser = new OrcParser(cfg, cfg.openInclude(f), f);
			decls.addAll(fparser.parseModule());
			if (progress.isCanceled()) return null;
		}
		if (progress.isCanceled()) return null;
		progress.setProgress(0.6);
		
		progress.setNote("Simplifying the AST");
		// Add the declarations to the parse tree
		Collections.reverse(decls);
		for (Declaration d : decls)
		{
			e = new Declare(d, e);
		}
		//System.out.println("Simplifying the abstract syntax tree...");
		// Simplify the AST
		Expr ex = e.simplify().convert(new Env<Var>(), new Env<String>());
		// System.out.println(ex);
		if (progress.isCanceled()) return null;
		progress.setProgress(0.7);
		
		progress.setNote("Resolving sites");
		ex = SiteResolver.resolve(ex, cfg);
		if (progress.isCanceled()) return null;
		progress.setProgress(0.8);
		
		// Optionally perform typechecking
		if (cfg.getTypeChecking()) {
			progress.setNote("Typechecking");
			
			Type rt = ex.typesynth(new Env<Type>(), new Env<Type>());
			System.out.println("... :: " + rt);
			System.out.println("Program typechecked successfully.");
			System.out.println();
	
		}
		
		progress.setNote("Checking for unguarded recursion");
		UnguardedRecursionChecker.check(ex);
		
		progress.setProgress(1.0);
		return ex;
	}
	
	public static Node compile(Config cfg) throws CompilationException, IOException {
		return Compiler.compile(compile(cfg.getReader(), cfg), new Pub());
	}

	/** @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws CompilationException 
	 * @deprecated */
	public static OrcInstance runEmbedded(String source) throws CompilationException, FileNotFoundException, IOException { 
		return runEmbedded(new File(source)); 
	}
	
	/** @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws CompilationException 
	 * @deprecated */
	public static OrcInstance runEmbedded(File source) throws CompilationException, FileNotFoundException, IOException { 
		return runEmbedded(new FileReader(source));
	}
	
	/** @throws IOException 
	 * @throws CompilationException 
	 * @deprecated */
	public static OrcInstance runEmbedded(Reader source) throws CompilationException, IOException { 
		return runEmbedded(source, new Config());
	}
	
	/**
	 * Compile an Orc source program from the given input stream.
	 * Start a new Orc engine running this program in a separate thread.
	 * Returns an OrcInstance object with information about the running instance.
	 * 
	 * @deprecated
	 * @param source
	 * @param cfg
	 * @throws IOException 
	 * @throws CompilationException 
	 */
	public static OrcInstance runEmbedded(Reader source, Config cfg) throws CompilationException, IOException {
		final BlockingQueue<Object> q = new LinkedBlockingQueue<Object>();
	
		// Try to run Orc with these options
		Node result = new Node() {
			@Override
			public void process(Token t) {
				q.add(t.getResult());
				t.die();
			}
		};
		Node n = Compiler.compile(compile(source, cfg), result);
        
		// Configure the runtime engine.
		OrcEngine engine = new OrcEngine(cfg);

		// Create an OrcInstance object, to be run in its own thread
		OrcInstance inst = new OrcInstance(engine, n, q);

		// Run the Orc instance in its own thread
		Thread t = new Thread(inst);
		t.start();

		// Return the instance object.
		return inst;
	}
}