/*
 * Copyright 2005, The University of Texas at Austin. All rights reserved.
 */
package orc.runtime;

import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import orc.Config;
import orc.env.Env;
import orc.error.runtime.TokenException;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.Visualizer;
import orc.runtime.regions.Execution;
import orc.runtime.values.GroupCell;
import orc.trace.Tracer;

/**
 * The Orc Engine provides the main loop for executing active tokens. Tokens are
 * always processed in a single thread, but tokens might be activated or resumed
 * from other threads, so some synchronization is necessary.
 * 
 * @author wcook, dkitchin, quark
 */
public class OrcEngine implements Runnable {
	/** Tokens which are ready to be processed. */
	private LinkedList<Token> readyTokens = new LinkedList<Token>();

	/**
	 * Tokens which have returned from a site. These are distinguished from
	 * {@link #readyTokens} to ensure that all ready tokens are processed before
	 * pending site returns.
	 */
	private LinkedList<Token> queuedReturns = new LinkedList<Token>();

	private int round = 1;

	/** Active logical clocks. */
	private LinkedList<WeakReference<LogicalClock>> clocks = new LinkedList<WeakReference<LogicalClock>>();

	/** Number of tokens <i>not</i> waiting at logical clocks. */
	private int pendingTokens = 0;

	public boolean debugMode = false;

	/**
	 * This flag is set by the Execution region when execution completes to
	 * terminate the engine.
	 */
	protected boolean halt = false;

	/**
	 * Currently this reference is just needed to keep pending tokens from being
	 * garbage-collected prematurely.
	 */
	private Execution region;

	/**
	 * Scheduler thread for Rtimer. This is here instead of the Rtimer site so
	 * we can easily cancel it when the job completes.
	 * 
	 * @see #scheduleTimer(TimerTask, long)
	 */
	private Timer timer;

	private Config config;
	private PrintStream stdout;
	private PrintStream stderr;
	private int maxpubs;
	private int pubs = 0;

	/** We'll notify this when the computation is finished. */
	private Tracer tracer;
	
	/** For debugging visualization */
	private Visualizer viz;

	public final static Globals<OrcEngine, Object> globals = new Globals<OrcEngine, Object>();

	public OrcEngine(Config config) {
		this.config = config;
		this.maxpubs = config.getMaxPubs();
		this.stdout = config.getStdout();
		this.stderr = config.getStderr();
		this.debugMode = config.debugMode();
	}

	public synchronized boolean isDead() {
		return halt;
	}

	/**
	 * Process active nodes, running indefinitely until signalled to stop by a
	 * call to terminate(). Typically you will use one of the other run methods
	 * to queue an active token to process first.
	 */
	public void run() {
		if (debugMode) System.out.println("Running...");
		timer = new Timer();
		Kilim.startEngine(config.getNumKilimThreads(), config
				.getNumSiteThreads());
		while (true) {
			if (!step())
				break;
		}
		timer.cancel();
		timer = null;
		Kilim.stopEngine();
		tracer.finish();
		tracer = null;
		globals.removeAll(this);
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
	 * Run Orc given a root node. Creates an initial environment and then
	 * executes the main loop.
	 * 
	 * @param root
	 *            node to run
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
		assert (root != null);
		assert (env != null);
		region = new Execution(this);
		tracer = config.getTracer();
		viz = config.getUseVisualizer() ? new Visualizer(root) : null;
		activate(new Token(root, env, GroupCell.ROOT, region, this, tracer
				.start()));
	}

	/**
	 * Run one step (process one token or handle one site response) Return false
	 * if engine should halt.
	 */
	protected boolean step() {
		// the next token to process
		Token todo;

		// synchronize while we decide what to do next;
		// this block will either return from the function
		// or set todo to a token to process
		synchronized (this) {
			if (halt)
				return false;
			if (!readyTokens.isEmpty()) {
				// If an active token is available, process it.
				todo = readyTokens.remove();
			} else if (!queuedReturns.isEmpty()) {
				// If a site return is available, make it active.
				// This marks the beginning of a new round.
				readyTokens.add(queuedReturns.remove());
				round++;
				reportRound();
				return true;
			} else {
				if (advanceLogicalClocks()) return true;
				try {
					// we will be notified when a site returns
					// or the engine terminates
					wait();
				} catch (InterruptedException e) {
					// return to the main loop and check for termination
				}
				return true;
			}
		}
		if (viz != null) viz.pick(todo.node);
		// notice that we do not synchronize while
		// processing a token; we don't want to risk
		// locking the engine if the processing blocks
		todo.process();
		return true;
	}

	/**
	 * Activate a token by adding it to the queue of active tokens
	 * 
	 * @param t
	 *            the token to be added
	 */
	synchronized public void activate(Token t) {
		readyTokens.addLast(t);
		notifyAll();
	}

	/**
	 * Activate a token by adding it to the queue of returning tokens
	 * 
	 * @param t
	 *            the token to be added
	 */
	synchronized public void resume(Token t) {
		t.getTracer().receive(t.getResult());
		queuedReturns.addLast(t);
		notifyAll();
	}

	/**
	 * Publish a result. This method is called by the Pub node when a
	 * publication is 'escaping' the bottom of the execution graph.
	 * 
	 * The default implementation prints the value's string representation to
	 * the console. Change this behavior by extending OrcEngine and overriding
	 * this method.
	 * 
	 * @param v
	 */
	public void publish(Object v) {
		stdout.println(String.valueOf(v));
		stdout.flush();
		// check if we have reached the maximum allowed number of publications
		if (maxpubs > 0 && maxpubs == ++pubs) terminate();
	}

	/**
	 * A token owned by this engine has encountered an exception. The token
	 * dies, remaining silent and leaving the execution, and then calls this
	 * method so that the engine can report or otherwise handle the failure.
	 */
	public void tokenError(Token t, TokenException problem) {
		stderr.println("Error: " + problem.getMessage());
		stderr.println("Source location: " + problem.getSourceLocation());
		if (debugMode) {
			problem.printStackTrace(stderr);
		}
		Throwable cause = problem.getCause();
		if (debugMode && cause != null) {
			stderr.println("Caused by:");
			cause.printStackTrace(stderr);
		}
	}

	public void debug(String s) {
		if (debugMode) System.out.println(s);
	}

	public void reportRound() {
		if (debugMode) {
			debug("---\n"
					+ "Round:   " + round + "\n"
					+ "Active:  " + readyTokens.size() + "\n"
					+ "Queued:  " + queuedReturns.size() + "\n"
					+ "Pending: " + pendingTokens + "\n");
			for (WeakReference<LogicalClock> clockr : clocks) {
				LogicalClock clock = clockr.get();
				if (clock != null) {
					debug("L-Clock: " + clock.getTime() + "\n");
				}
			}
			debug("---\n\n");
		}
	}

	public synchronized boolean addLogicalClock(LogicalClock clock) {
		return clocks.add(new WeakReference<LogicalClock>(clock));
	}

	/**
	 * Print something (for use by the print and println sites). By default,
	 * this prints to stdout, but this can be overridden to do something
	 * else if appropriate.
	 * 
	 * @see Token#print(String, boolean)
	 */
	public void print(String string, boolean newline) {
		if (newline) {
			stdout.println(string);
		} else {
			stdout.print(string);
		}
	}

	/**
	 * Schedule a timed task (used by Rtimer).
	 */
	public void scheduleTimer(TimerTask task, long delay) {
		timer.schedule(task, delay);
	}

	public String addGlobal(Object value) {
		return globals.add(this, value);
	}

	synchronized void addPendingToken(Token token) {
		++pendingTokens;
	}

	synchronized void removePendingToken(Token token) {
		assert(pendingTokens > 0);
		--pendingTokens;
		// we shouldn't advance the clocks immediately
		// even if the pending tokens are all freed,
		// because we may be about to add a new token
		// before the end of the round
	}

	/** Return true if some clock advanced. */
	private boolean advanceLogicalClocks() {
		// can't advance clocks if there are pending site calls
		if (pendingTokens > 0) return false;

		boolean out = false;
		Iterator<WeakReference<LogicalClock>> it = clocks.iterator();
		while (it.hasNext()) {
			WeakReference<LogicalClock> clockr = it.next();
			LogicalClock clock = clockr.get();
			if (clock == null)
				it.remove();
			else
				out = clock.advance() || out;
		}
		return out;
	}
	
	public Config getConfig() {
		return config;
	}
}
