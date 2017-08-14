package orc.run.porce;

import com.oracle.truffle.api.frame.VirtualFrame;

public class NewFuture extends Expression {
	@Override
    public Object execute(VirtualFrame frame) {
		return executeFuture(frame);
	}

	@Override
    public orc.Future executeFuture(VirtualFrame frame) {
		return new orc.run.porce.runtime.Future();
	}

	public static NewFuture create() {
		return new NewFuture();
	}
}
