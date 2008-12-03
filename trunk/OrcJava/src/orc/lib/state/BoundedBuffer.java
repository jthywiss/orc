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
 * A bounded buffer.
 * With a bound of zero, behaves as a synchronous channel.
 * 
 * @author quark
 */
public class BoundedBuffer extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(Args args) throws TokenException {
		return new BufferInstance(args.intArg(0));
	}
	
	protected class BufferInstance extends DotSite {

		private LinkedList<Object> buffer;
		private LinkedList<Token> readers;
		private LinkedList<Token> writers;
		private Token closer;
		/** The number of open slots in the buffer. */
		private int open;
		private boolean closed = false;

		BufferInstance(int bound) {
			open = bound;
			buffer = new LinkedList<Object>();
			readers = new LinkedList<Token>();
			writers = new LinkedList<Token>();
		}
		
		@Override
		protected void addMembers() {
			addMember("get", new Site() {
				public void callSite(Args args, Token reader) {
					if (buffer.isEmpty()) {
						if (closed) {
							reader.die();
						} else {
							reader.setQuiescent();
							readers.addLast(reader);
						}
					} else {
						reader.resume(buffer.removeFirst());
						if (writers.isEmpty()) {
							++open;
						} else {
							Token writer = writers.removeFirst();
							writer.unsetQuiescent();
							writer.resume();
						}
						if (closer != null && buffer.isEmpty()) {
							closer.unsetQuiescent();
							closer.resume();
							closer = null;
						}
					}
				}
			});	
			addMember("getnb", new Site() {
				@Override
				public void callSite(Args args, Token reader) {
					if (buffer.isEmpty()) {
						reader.die();
					} else {
						reader.resume(buffer.removeFirst());
						if (writers.isEmpty()) {
							++open;
						} else {
							Token writer = writers.removeFirst();
							writer.unsetQuiescent();
							writer.resume();
						}
						if (closer != null && buffer.isEmpty()) {
							closer.unsetQuiescent();
							closer.resume();
							closer = null;
						}
					}
				}
			});
			addMember("put", new Site() {
				@Override
				public void callSite(Args args, Token writer) throws TokenException {
					Object item = args.getArg(0);
					if (closed) {
						writer.die();
					} else if (!readers.isEmpty()) {
						Token reader = readers.removeFirst();
						reader.unsetQuiescent();
						reader.resume(item);
						writer.resume();
					} else if (open == 0) {
						buffer.addLast(item);
						writer.setQuiescent();
						writers.addLast(writer);
					} else {
						buffer.addLast(item);
						--open;
						writer.resume();
					}
				}
			});
			addMember("putnb", new Site() {
				@Override
				public void callSite(Args args, Token writer) throws TokenException {
					Object item = args.getArg(0);
					if (closed) {
						writer.die();
					} else if (!readers.isEmpty()) {
						Token reader = readers.removeFirst();
						reader.unsetQuiescent();
						reader.resume(item);
						writer.resume();
					} else if (open == 0) {
						writer.die();
					} else {
						buffer.addLast(item);
						--open;
						writer.resume();
					}
				}
			});
			addMember("getAll", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					// restore open slots
					open += buffer.size() - writers.size();
					// collect all values in a list
					Object out = ListValue.make(buffer);
					buffer.clear();
					// resume all writers
					for (Token writer : writers) {
						writer.unsetQuiescent();
						writer.resume();
					}
					writers.clear();
					// notify closer if necessary
					if (closer != null) {
						closer.unsetQuiescent();
						closer.resume();
						closer = null;
					}
					return out;
				}
			});	
			addMember("getOpen", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return open;
				}
			});	
			addMember("getBound", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return open + buffer.size() - writers.size();
				}
			});	
			addMember("isClosed", new EvalSite() {
				@Override
				public Object evaluate(Args args) throws TokenException {
					return closed;
				}
			});	
			addMember("close", new Site() {
				@Override
				public void callSite(Args args, Token token) {
					closed = true;
					for (Token reader : readers) {
						reader.unsetQuiescent();
						reader.die();
					}
					if (buffer.isEmpty()) {
						token.resume();
					} else {
						closer = token;
						closer.setQuiescent();
					}
				}
			});	
			addMember("closenb", new Site() {
				@Override
				public void callSite(Args args, Token token) {
					closed = true;
					for (Token reader : readers) {
						reader.unsetQuiescent();
						reader.die();
					}
					token.resume();
				}
			});	
		}
	}
}
