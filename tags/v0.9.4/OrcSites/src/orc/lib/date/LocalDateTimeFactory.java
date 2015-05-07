package orc.lib.date;

import orc.error.runtime.InsufficientArgsException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;

import org.joda.time.LocalDateTime;

public class LocalDateTimeFactory extends EvalSite {
	@Override
	public Object evaluate(Args args) throws TokenException {
		switch (args.size()) {
		case 0: return new LocalDateTime();
		case 1: return new LocalDateTime(args.getArg(0));
		case 3: return new LocalDateTime(args.intArg(0), args.intArg(1), args.intArg(2),
				0, 0, 0, 0);
		case 7: return new LocalDateTime(args.intArg(0), args.intArg(1), args.intArg(2),
				args.intArg(3), args.intArg(4), args.intArg(5), args.intArg(6));
		default: throw new InsufficientArgsException(6, args.size());
		}
	}
}
