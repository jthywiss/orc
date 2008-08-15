package orc.trace.events;

import java.io.IOException;
import java.io.Writer;

import orc.trace.handles.LastHandle;
import orc.trace.handles.RepeatHandle;
import orc.trace.values.Value;

/**
 * A top-level publication in a thread.
 */
public class PublishEvent extends Event {
	public final Value value;
	public PublishEvent(ForkEvent thread, Value value) {
		super(new RepeatHandle<ForkEvent>(thread));
		this.value = value;
	}
	@Override
	public void prettyPrint(Writer out) throws IOException {
		super.prettyPrint(out);
		out.write("(");
		value.prettyPrint(out, 1);
		out.write(")");
	}
}
