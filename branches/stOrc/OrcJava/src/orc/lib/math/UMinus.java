/**
 * 
 */
package orc.lib.math;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.Args.NumericUnaryOperator;
import orc.runtime.sites.EvalSite;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.MultiType;

/**
 * @author dkitchin
 *
 */
public class UMinus extends EvalSite {
	private static final MyOperator op = new MyOperator();
	private static final class MyOperator implements NumericUnaryOperator<Number> {
		public Number apply(BigInteger a) {
			return a.negate();
		}
		public Number apply(BigDecimal a) {
			return a.negate();
		}
		public Number apply(int a) {
			return -a;
		}
		public Number apply(long a) {
			return -a;
		}
		public Number apply(byte a) {
			return -a;
		}
		public Number apply(short a) {
			return -a;
		}
		public Number apply(double a) {
			return -a;
		}
		public Number apply(float a) {
			return -a;
		}
	}
	@Override
	public Object evaluate(Args args) throws TokenException {
		return Args.applyNumericOperator(args.numberArg(0), op);
	}
	
	public Type type() {
		return new MultiType(
				new ArrowType(Type.INTEGER, Type.INTEGER),
				new ArrowType(Type.NUMBER, Type.NUMBER)
				);
	}
}