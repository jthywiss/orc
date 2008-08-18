/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import orc.Config;
import orc.env.Env;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.nodes.Node;
import orc.runtime.regions.Execution;
import orc.runtime.values.GroupCell;
import orc.trace.NullTracer;
import orc.trace.OutputStreamTracer;
import orc.trace.Tracer;

/**
 * The Orc Engine provides the main loop for executing active tokens.
 * Tokens are always processed in a single thread, but tokens might
 * be activated or resumed from other threads, so some synchronization
 * is necessary. 
 * 
 * @author wcook, dkitchin, quark
 */
public class OrcEngine implements Runnable {

	LinkedList<Token> activeTokens = new LinkedList<Token>();
	LinkedList<Token> queuedReturns = new LinkedList<Token>();
	Set<LogicalClock> clocks = new HashSet<LogicalClock>();
	int round = 1;
	public boolean debugMode = false;
	/**
	 * This flag is set by the Execution region when execution completes
	 * to terminate the engine.
	 */
	protected boolean halt = false;
	/**
	 * Currently this reference is just needed to keep pending tokens
	 * from being garbage-collected prematurely.
	 */
	private Execution region;
	/**
	 * Scheduler thread for Rtimer. This is here instead
	 * of the Rtimer site so we can easily cancel it when
	 * the job completes.
	 * @see #scheduleTimer(TimerTask, long)
	 */
	private Timer timer;
	private Config config;
	
	public OrcEngine(Config config) {
		this.config = config;
		this.debugMode = config.debugMode();
	}

	public synchronized boolean isDead() { return halt; }

	/**
	 * Process active nodes, running indefinitely until
	 * signalled to stop by a call to terminate().
	 * Typically you will use one of the other run methods
	 * to queue an active token to process first.
	 */
	public void run() {
		timer = new Timer();
		Kilim.startEngine(config.getNumKilimThreads(),
				config.getNumSiteThreads());
		while (true) {
			// FIXME: can we avoid synchronizing this whole block?
			synchronized(this) {
				if (halt) break;
				if (!step()) {
					try {
						wait();
					} catch (InterruptedException e) {
						// terminate execution
						break;
					}
				}
			}
		}
		timer.cancel();
		timer = null;
		Kilim.stopEngine();
	}
	
	/**
	 * Terminate execution.
	 */
	public synchronized void terminate() {
		halt = true;
		debug("Engine terminated.");
		notifyAll();
	}

	/**
	 * Run Orc given a root node.
	 * Creates an initial environment and then 
	 * executes the main loop.
	 * @param root  node to run
	 */ 
	public void run(Node root) {
		start(root);
		run();
	}

	public void run(Node root, Env env) {
		start(root, env);
		run();
	}
	
	public void start(Node root) {
		start(root, new Env<Object>());
	}
	
	public void start(Node root, Env env) {
		assert(root != null);
		assert(env != null);
		region = new Execution(this);
		Tracer tracer = config.getTracer();
		tracer.start();
		activate(new Token(root, env, new GroupCell(), region, this, tracer));
	}
	
	/**
	 * Run one step (process one token, handle one site response, or advance
	 * all logical clocks). Returns true if work was done.
	 */
	protected boolean step() {
		/* If an active token is available, process it. */
		if (!activeTokens.isEmpty()){
			activeTokens.remove().process();
			return true;
		}
		
		/* If a site return is available, make it active.
		 * This marks the beginning of a new round. 
		 */
		if (!queuedReturns.isEmpty()){
			activeTokens.add(queuedReturns.remove());
			round++; reportRound();
			return true;
		}
		
		/* If the engine is quiescent, advance all logical clocks. */
		boolean progress = false;
		
		for (LogicalClock clock : clocks) {
			progress = clock.advance() || progress;
		}
		
		/* If some logical clock actually advanced, return. */
		return progress;
	}
	
	/**
	 * Activate a token by adding it to the queue of active tokens
	 * @param t	the token to be added
	 */
	synchronized public void activate(Token t) {
		activeTokens.addLast(t);
		notifyAll();
	}
	
	/**
	 * Activate a token by adding it to the queue of returning tokens
	 * @param t	the token to be added
	 */
	synchronized public void resume(Token t) {
		t.getTracer().resume(t.getResult());
		queuedReturns.addLast(t);
		notifyAll();
	}
	
	/**
	 * Publish a result. This method is called by the Pub node
	 * when a publication is 'escaping' the bottom of the
	 * execution graph.
	 * 
	 * The default implementation prints the value's string
	 * representation to the console. Change this behavior
	 * by extending OrcEngine and overriding this method.
	 * 
	 * @param v
	 */
	public void publish(Object v) {
		System.out.println(String.valueOf(v));
		System.out.flush();
	}
	
	/**
	 * A token owned by this engine has encountered an exception.
	 * The token dies, remaining silent and leaving the execution,
	 * and then calls this method so that the engine can report or 
	 * otherwise handle the failure.
	 */
	public void tokenError(Token t, TokenException problem) {
		System.out.println();
		System.out.println("Token " + t + " encountered an error. ");
		System.out.println("Problem: " + problem);
		System.out.println("Source location: " + problem.getSourceLocation());
		if (debugMode) {
			problem.printStackTrace();
		}
		Throwable cause = problem.getCause();
		if (debugMode && cause != null) {
			System.out.println("Caused by:");
			cause.printStackTrace();
		}
		System.out.println();
	}
	
	
	public void debug(String s) {
		if (debugMode) System.out.println(s);
	}
	
	public void reportRound() {
		if (debugMode){
			debug("---\n" + 
			      "Round:   " + round + "\n" +
			      "Active:  " + activeTokens.size() + "\n" +
			      "Queued:  " + queuedReturns.size() + "\n");
			for(LogicalClock clock : clocks) {
			      debug("L-Clock: " + clock.getTime() + "\n");
			}
			debug("---\n\n");
		}
	}
	
	public synchronized boolean addClock(LogicalClock clock) {
		return clocks.add(clock);
	}
	/**
	 * Print something (for use by the print and println sites). By default,
	 * this prints to System.out, but this can be overridden to do something
	 * else if appropriate.
	 * @see Token#print(String)
	 */
	public void print(String string, boolean newline) {
		if (newline) {
			System.out.println(string);
		} else {
			System.out.print(string);
		}
	}
	
	/**
	 * Schedule a timed task (used by Rtimer).
	 */
	public void scheduleTimer(TimerTask task, long delay) {
		timer.schedule(task, delay);
	}
}
