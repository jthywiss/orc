package orc.trace.values;

import java.io.IOException;
import java.io.StringWriter;

import orc.trace.query.Frame;
import orc.trace.query.Term;
import orc.trace.query.patterns.Variable;

public abstract class AbstractValue implements Value {
	public String toString() {
		try {
			StringWriter writer = new StringWriter();
			prettyPrint(writer, 0);
			return writer.toString();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
	public Frame unify(Frame frame, Term that) {
		return equals(that) ? frame : null;
	}
	public Term evaluate(Frame frame) {
		return this;
	}
	public boolean occurs(Variable var) {
		return false;
	}
}
