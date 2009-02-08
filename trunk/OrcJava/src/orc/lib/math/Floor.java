package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericBinaryOperator;
import orc.runtime.sites.EvalSite;
import orc.type.ArrowType;
import orc.type.Type;

public class Floor extends EvalSite {
	
	@Override
	public Object evaluate(Args args) throws TokenException {
		Number n = args.numberArg(0);
		return n.intValue();
	}
	
	public static Type type() {
		return new ArrowType(Type.NUMBER, Type.INTEGER);
	}
}
