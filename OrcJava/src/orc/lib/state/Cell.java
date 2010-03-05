//
// Cell.java -- Java class Cell
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

package orc.lib.state;

import java.util.LinkedList;
import java.util.Queue;

import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.TokenException;
import orc.lib.state.types.CellType;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.Site;
import orc.type.Type;
import orc.type.TypeVariable;
import orc.type.structured.ArrowType;

/**
 * Write-once cell. 
 * Read operations block while the cell is empty.
 * Write operatons fail once the cell is full.
 *
 * @author dkitchin
 */
public class Cell extends EvalSite {

	/* (non-Javadoc)
	 * @see orc.runtime.sites.Site#callSite(java.lang.Object[], orc.runtime.Token, orc.runtime.values.GroupCell, orc.runtime.OrcEngine)
	 */
	@Override
	public Object evaluate(final Args args) {
		return new CellInstance();
	}

	@Override
	public Type type() throws TypeException {
		final Type X = new TypeVariable(0);
		final Type CellOfX = new CellType().instance(X);
		return new ArrowType(CellOfX, 1);
	}

	protected class CellInstance extends DotSite {

		private Queue<Token> readQueue;
		Object contents;

		CellInstance() {
			this.contents = null;

			/* Note that the readQueue also signals whether the cell contents have been assigned.
			 * If it is non-null (as it is initially), the cell is empty.
			 * If it is null, the cell has been written.
			 * 
			 * This allows the cell to contain a null value if needed, and it also
			 * frees the memory associated with the read queue once the cell has been assigned.
			 */
			this.readQueue = new LinkedList<Token>();
		}

		@Override
		protected void addMembers() {
			addMember("read", new readMethod());
			addMember("write", new writeMethod());
			addMember("readnb", new Site() {
				@Override
				public void callSite(final Args args, final Token caller) throws TokenException {
					if (readQueue != null) {
						caller.die();
					} else {
						caller.resume(contents);
					}
				}
			});
		}

		private class readMethod extends Site {
			@Override
			public void callSite(final Args args, final Token reader) {

				/* If the read queue is not null, the cell has not been set.
				 * Add this caller to the read queue.
				 */
				if (readQueue != null) {
					reader.setQuiescent();
					readQueue.add(reader);
				}
				/* Otherwise, return the contents of the cell */
				else {
					reader.resume(contents);
				}
			}
		}

		private class writeMethod extends Site {
			@Override
			public void callSite(final Args args, final Token writer) throws TokenException {

				final Object val = args.getArg(0);

				/* If the read queue is not null, the cell has not yet been set. */
				if (readQueue != null) {
					/* Set the contents of the cell */
					contents = val;

					/* Wake up all queued readers and report the written value to them. */
					for (final Token reader : readQueue) {
						reader.unsetQuiescent();
						reader.resume(val);
					}

					/* Null out the read queue. 
					 * This indicates that the cell has been written.
					 * It also allowed the associated memory to be reclaimed.
					 */
					readQueue = null;

					/* A successful write publishes a signal. */
					writer.resume();
				} else {
					/* A failed write kills the writer. */
					writer.die();
				}

			}
		}

	}

}
