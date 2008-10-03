/**
 * 
 */
package orc.lib.state;

import java.util.LinkedList;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.runtime.values.ListValue;

/**
 * @author cawellington, dkitchin
 *
 * Implements the local site Buffer, which creates buffers (asynchronous channels).
 *
 */
public class Buffer extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(Args args) {
		return new BufferInstance();
	}
	
	
	protected class BufferInstance extends DotSite {

		private LinkedList<Object> localBuffer;
		private LinkedList<Token> pendingQueue;

		BufferInstance() {
			localBuffer = new LinkedList<Object>();
			pendingQueue = new LinkedList<Token>();
		}
		
		@Override
		protected void addMethods() {
			addMethod("get", new getMethod());	
			addMethod("put", new putMethod());
			addMethod("getnb", new Site() {
				@Override
				public void callSite(Args args, Token receiver) {
					if (localBuffer.isEmpty()) receiver.die();
					else receiver.resume(localBuffer.removeFirst());
				}
			});
			addMethod("getAll", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return ListValue.make(localBuffer);
				}
			});	
			addMethod("close", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					for (Token pending : pendingQueue) {
						pending.die();
					}
					return signal();
				}
			});	
		}
		
		private class getMethod extends Site {
			@Override
			public void callSite(Args args, Token receiver) {

				// If there are no buffered items, put this caller on the queue
				if (localBuffer.isEmpty()) {
					pendingQueue.addLast(receiver);
				}
				// If there is an item available, pop it and return it.
				else {
					receiver.resume(localBuffer.removeFirst());
				}

			}
		}
		
		private class putMethod extends Site {
			@Override
			public void callSite(Args args, Token sender) throws TokenException {

				Object item = args.getArg(0);
				
				// If there are no waiting callers, buffer this item.
				if (pendingQueue.isEmpty()) {
					localBuffer.addLast(item);
				}
				// If there are callers waiting, give this item to the top caller.
				else {
					Token receiver = pendingQueue.removeFirst();
					receiver.resume(item);
				}

				// Since this is an asynchronous buffer, a put call always returns.
				sender.resume();
			}
		}

	}
	
}
