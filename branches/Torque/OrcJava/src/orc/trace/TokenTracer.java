package orc.trace;

import java.util.List;

import orc.error.Locatable;
import orc.error.runtime.TokenException;
import orc.runtime.values.Closure;
import orc.trace.events.HaltEvent;

/**
 * Interface for writing traces from a single Orc thread. Methods correspond to
 * events which may be traced; in essence this is like a visitor of execution
 * events. Some guidelines used to organize events:
 * <ul>
 * <li>Steps shared by several logical events are made explicit. So for example
 * when a thread encounters an error, this results in
 * {@link #error(TokenException)} followed by {@link #die()}. We could make the
 * {@link #die()} implicit but making it explicit facilitates code reuse in the
 * client and simplifies queries.
 * <li>When two threads interact, at least two events are involved: one for the
 * cause and one for the effect. This ensures that we can reconstruct the
 * behavior of a thread looking only at events in that thread. The effect event
 * includes a pointer back to the cause event.
 * </ul>
 * 
 * <p>
 * "Trace" objects ({@link StoreTrace} et al) serve as abstract handles for
 * events and are used to record relationships between events in different
 * threads. Since Java doesn't have existential types, implementors have to cast
 * these objects to the appropriate concrete types internally. This is safe as
 * long as all the TokenTracers produced by a single {@link Tracer} use use
 * compatible concrete trace types.
 * 
 * @author quark
 */
public abstract class TokenTracer implements Locatable {
	/** Abstract handle for a store event */
	public interface StoreTrace {}
	/** Abstract handle for a pull event */
	public interface PullTrace {}
	/** Abstract handle for a before event */
	public interface BeforeTrace {}
	/** Abstract handle for a halt event. EXPERIMENTAL */
	public interface HaltTrace {}
	/**
	 * Create a new thread. By convention the new thread should
	 * evaluate the right side of the combinator.
	 */
	public abstract TokenTracer fork();
	/**
	 * Terminate a thread.
	 */
	public abstract void die();
	/**
	 * Call a site.
	 */
	public abstract void send(Object site, Object[] arguments);
	/**
	 * Store a value for a future. The return value should be used when tracing
	 * the results of this store. If this returns null, clients are free to
	 * <i>not</i> call {@link #choke(StoreTrace)}.
	 * 
	 * <p>
	 * The engine guarantees that all 
	 * {@link #choke(orc.trace.TokenTracer.StoreTrace)} and
	 * {@link #unblock(orc.trace.TokenTracer.StoreTrace)} events will occur
	 * <i>before</i> the {@link #die()} event for this tracer.
	 * 
	 * @see #choke(orc.trace.TokenTracer.StoreTrace)
	 * @see #unblock(orc.trace.TokenTracer.StoreTrace)
	 */
	public abstract StoreTrace store(PullTrace event, Object value);
	/**
	 * Killed through the setting of a future.
	 * Should be followed by {@link #die()}.
	 */
	public abstract void choke(StoreTrace store);
	/**
	 * Return from a site call. Should be called after
	 * {@link #send(Object, Object[])}.
	 */
	public abstract void receive(Object value);
	/**
	 * Block a thread waiting for a future.
	 */
	public abstract void block(PullTrace pull);
	/**
	 * Receive a future we were waiting for.
	 */
	public abstract void unblock(StoreTrace store);
	/**
	 * Print to stdout.
	 */
	public abstract void print(String value, boolean newline);
	/**
	 * Publish a value from the program.
	 * Should be followed by {@link #die()}.
	 */
	public abstract void publish(Object value);
	/**
	 * Report an error.
	 * Should be followed by {@link #die()}.
	 */
	public abstract void error(TokenException error);
	/**
	 * Create a new future for a pull.
	 * Should be followed by {@link #fork()}.
	 */
	public abstract PullTrace pull();
	/**
	 * Enter a closure.
	 * EXPERIMENTAL
	 */
	public abstract void enter(Closure closure);
	/**
	 * Leave "depth" closures
	 * EXPERIMENTAL
	 */
	public abstract void leave(int depth);
	/**
	 * Called when a token reads a value from a group cell which has
	 * already been stored.
	 * @param storeTrace the trace produced when {@link #store(orc.trace.TokenTracer.PullTrace, Object)} was called
	 */
	public abstract void useStored(StoreTrace storeTrace);
	
	/**
	 * Called when a site halts.
	 * 
	 * This method should actually be abstract. It is implemented here to
	 * avoid changing the already existing tracers for now.
	 * 
	 * EXPERIMENTAL. 
	 *  
	 * @param causes The set of (previous) halt events that caused this halt.
	 * For e.g. halts in the right side of a pull might cause site calls waiting
	 * on the variable of the pull to halt.  
	 */
	public HaltTrace halt(List<HaltTrace> haltCauses) {return new HaltEvent();}
	/** 
	 * Called when the left branch of the otherwise expression
	 * halts. The events of the right branch follow this call.
	 * 
	 * This method should actually be abstract. It is implemented here to
	 * avoid changing the already existing tracers for now.
	 * 
	 * EXPERIMENTAL 
	 */
	public void enterOther(List<HaltTrace> haltCauses) {}; 
}
