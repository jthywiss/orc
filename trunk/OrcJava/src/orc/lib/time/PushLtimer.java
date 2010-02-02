package orc.lib.time;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.Site;
import orc.runtime.values.Closure;
import orc.type.Type;
import orc.type.structured.ArrowType;

public class PushLtimer extends Site {
	@Override
	public void callSite(Args args, Token caller) throws TokenException {
		caller.pushLtimer();
		caller.resume();
	}
	
	public Type type() {
		return new ArrowType(Type.SIGNAL);
	}
}
