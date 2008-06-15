/*
 * Created on Jun 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package orc;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import orc.ast.extended.Declare;
import orc.ast.extended.declaration.Declaration;
import orc.ast.oil.Expr;
import orc.ast.simple.arg.Var;
import orc.env.Env;
import orc.parser.OrcLexer;
import orc.parser.OrcParser;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.result.QueueResult;
import orc.runtime.values.Value;

/**
 * Main class for Orc. Parses Orc file and executes it.
 * @author wcook, dkitchin
 */
public class Orc {

	/**
	 * 
	 * Orc toplevel main function. Command line arguments are forwarded to Config for parsing.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		// Read configuration options from the environment and the command line
		Config cfg = new Config();
		cfg.processEnvVars();
		cfg.processArgs(args);	
		
		Node n = compile(cfg.getInstream(), cfg.getTarget(), cfg);
		//System.out.println(n);
		if (n == null) return;
        
		// Configure the runtime engine
		OrcEngine engine = new OrcEngine();
        engine.debugMode = cfg.debugMode();
		
		// Run Orc with these options
		try {
				System.out.println("Running...");
		        // Run the Orc program
		        engine.run(n);
		        
			} catch (Exception e) {
				System.err.println("exception: " + e);
				if (cfg.debugMode())
					e.printStackTrace();
			} catch (Error e) {
				System.err.println(e.toString());
				if (cfg.debugMode())
					e.printStackTrace();
			}
		
		System.exit(0);
	}
	
	public static orc.ast.simple.Expression compile(Reader source, Config cfg) {
		return compile(source, cfg, true);
	}
	
	public static orc.ast.simple.Expression compile(Reader source, Config cfg, boolean includeStdlib) {
		
		try {
		
		//System.out.println("Parsing...");
		// Parse the goal expression
		OrcLexer lexer = new OrcLexer(source);
		OrcParser parser = new OrcParser(lexer);
		orc.ast.extended.Expression e = parser.startRule();
		
		//System.out.println("Importing declarations...");
		LinkedList<Declaration> decls = new LinkedList<Declaration>();
		
		if (includeStdlib) {
			// Load declarations from the default includes directory.
			File incdir = new File("./inc");
			for (File f : incdir.listFiles())
			{
				// Include loading doesn't recurse into subdirectories.
				if (f.isFile()) {
					OrcLexer flexer = new OrcLexer(new FileReader(f));
					OrcParser fparser = new OrcParser(flexer);
					decls.addAll(fparser.decls());
				}
			}
		}
		
		// Load declarations from files specified by the configuration options
		for (File f : cfg.getBindings())
		{
			OrcLexer flexer = new OrcLexer(new FileReader(f));
			OrcParser fparser = new OrcParser(flexer);
			decls.addAll(fparser.decls());
		}
		
		// Add the declarations to the parse tree
		Collections.reverse(decls);
		for (Declaration d : decls)
		{
			e = new Declare(d, e);
		}
		
		//System.out.println("Simplifying the abstract syntax tree...");
		// Simplify the AST
		return e.simplify();
		
		} catch (Exception e) {
			System.err.println("exception: " + e);
			if (cfg.debugMode())
				e.printStackTrace();
		} catch (Error e) {
			System.err.println(e.toString());
			if (cfg.debugMode())
			e.printStackTrace();
		}
		
		return null;		
	}
	
	protected static Node compile(Reader source, Node target, Config cfg) {
		
		try {

		orc.ast.simple.Expression es = compile(source, cfg);
		
		//System.out.println("Compiling to an execution graph...");
		// Compile the AST, directing the output towards the configured target
		Expr ex = es.convert(new Env<Var>());
		Node en = ex.compile(target);
		return en;
		
		} catch (Exception e) {
			System.err.println("exception: " + e);
			if (cfg.debugMode())
				e.printStackTrace();
		} catch (Error e) {
			System.err.println(e.toString());
			if (cfg.debugMode())
			e.printStackTrace();
		}
		
		return null;
	}

	public static OrcInstance runEmbedded(String source) { 
		return runEmbedded(new File(source)); 
	}
	
	public static OrcInstance runEmbedded(File source) { 
		try {
			return runEmbedded(new FileReader(source));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public static OrcInstance runEmbedded(Reader source) { 
		return runEmbedded(source, new Config());
	}
	
	/**
	 * Compile an Orc source program from the given input stream.
	 * Start a new Orc engine running this program in a separate thread.
	 * Returns an OrcInstance object with information about the running instance.
	 * 
	 * @param source
	 * @param cfg
	 * @return
	 */
	public static OrcInstance runEmbedded(Reader source, Config cfg) {
		
		BlockingQueue<Value> q = new LinkedBlockingQueue<Value>();
	
		// Try to run Orc with these options
		try {	
				Node n = compile(source, new QueueResult(q), cfg);
		        
				// Configure the runtime engine.
				OrcEngine engine = new OrcEngine();
		        engine.debugMode = cfg.debugMode();
		        
		        // Create an OrcInstance object, to be run in its own thread
		        Env env = new Env();
		        OrcInstance inst = new OrcInstance(engine, n, env, q);
		        
		        // Run the Orc instance in its own thread
		        Thread t = new Thread(inst);
		        t.start();
		        
		        // Return the instance object.
		        return inst;
			} catch (Exception e) {
				System.err.println("exception: " + e);
				if (cfg.debugMode())
					e.printStackTrace();
			} catch (Error e) {
				System.err.println(e.toString());
				if (cfg.debugMode())
					e.printStackTrace();
			}
			
		return null;
	}	
}

