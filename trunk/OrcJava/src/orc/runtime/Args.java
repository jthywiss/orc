package orc.runtime;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import orc.error.OrcError;
import orc.error.runtime.ArgumentTypeMismatchException;
import orc.error.runtime.ArityMismatchException;
import orc.error.runtime.InsufficientArgsException;
import orc.error.runtime.TokenException;
import orc.runtime.values.Field;
import orc.runtime.values.LazyListValue;
import orc.runtime.values.ListLike;
import orc.runtime.values.NilValue;
import orc.runtime.values.TupleValue;
import orc.runtime.values.Value;

/**
 * 
 * Container for arguments to a site. 
 * 
 * @author dkitchin
 *
 */

public class Args implements Serializable, Iterable<Object> {
	Object[] values;
	
	public Args(List<Object> values) {
		this.values = new Object[values.size()];
		this.values = values.toArray(this.values);
	}
	
	public Args(Object[] values) {
		this.values = values;
	}
	
	public int size() {
		return values.length;
	}
	
	/**
	 * Classic 'let' functionality. 
	 * Reduce a list of argument values into a single value as follows:
	 * 
	 * Zero arguments: return a signal
	 * One argument: return that value
	 * Two or more arguments: return a tuple of values
	 * 
	 */
	public Object condense() {
		if (values.length == 0) {
			return Value.signal();
		} else if (values.length == 1) {
			return values[0];
		} else {
			return new TupleValue(values);
		}
	}
	
	/**
	 * Helper function to retrieve the nth value (starting from 0), with error
	 * checking.
	 * @deprecated
	 */
	public Value valArg(int n) throws TokenException {
		Object a = getArg(n);
		if (a == null) throw new ArgumentTypeMismatchException(n, "Value", "null");
		try {
			return (Value)a;
		} catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "Value", a.getClass().toString());
		} 
	}
	
	public String fieldName() throws TokenException {
		if (values.length != 1) {
			//throw new TokenException("Arity mismatch resolving field reference.");
			throw new ArityMismatchException(1, values.length);
		}
		Object v = values[0];
		if (v == null) throw new ArgumentTypeMismatchException(0, "message", "null");
		if (v instanceof Field) {
			return ((Field)v).getKey();
		} else {
			//throw new TokenException("Bad type for field reference.");
			throw new ArgumentTypeMismatchException(0, "message", v.getClass().toString());
		}
	}
	
	/**
	 * Helper function to retrieve the nth element as an object (starting from
	 * 0), with error checking
	 * 
	 * @throws TokenException
	 */
	public Object getArg(int n) throws TokenException {
		try {
			return values[n];
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new InsufficientArgsException(n, values.length);
		}
	}
	
	/**
	 * Return the entire tuple as an object array.
	 * Please don't mutate the array.
	 */
	public Object[] asArray() throws TokenException {
		return values;
	}
		
	/**
	 * Helper function for integers
	 * @throws TokenException 
	 */
	public int intArg(int n) throws TokenException {
		Object a = getArg(n);
		if (a == null) throw new ArgumentTypeMismatchException(n, "int", "null");
		try
			{ return ((Number)a).intValue(); }
		catch (ClassCastException e) { 
			// throw new TokenException("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); 
			throw new ArgumentTypeMismatchException(n, "int", a.getClass().toString());
		} 
	}

	/**
	 * Helper function for longs
	 * @throws TokenException 
	 */
	public long longArg(int n) throws TokenException {
		Object a = getArg(n);
		if (a == null) throw new ArgumentTypeMismatchException(n, "long", "null");
		try
			{ return ((Number)a).longValue(); }
		catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "long", a.getClass().toString());
		}
			// { throw new TokenException("Argument " + n + " should be an int, got " + a.getClass().toString() + " instead."); } 
	}

	public Number numberArg(int n) throws TokenException {
		Object a = getArg(n);
		if (a == null) throw new ArgumentTypeMismatchException(n, "Number", "null");
		try
			{ return (Number)a; }
		catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "Number", a.getClass().toString());
		}
	}
	
	/**
	 * Helper function for booleans
	 * @throws TokenException 
	 */
	public boolean boolArg(int n) throws TokenException {
		Object a = getArg(n);
		if (a == null) throw new ArgumentTypeMismatchException(n, "boolean", "null");
		try
			{ return ((Boolean)a).booleanValue(); }
		catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "boolean", a.getClass().toString());
		}
			//{ throw new TokenException("Argument " + n + " to site '" + this.toString() + "' should be a boolean, got " + a.getClass().toString() + " instead."); } 
	
	}

	/**
	 * Helper function for strings.
	 * Note that this requires a strict String type.
	 * If you don't care whether the argument is really a string,
	 * use valArg(n).toString().
	 * @throws TokenException 
	 */
	public String stringArg(int n) throws TokenException {
		Object a = getArg(n);
		if (a == null) throw new ArgumentTypeMismatchException(n, "String", "null");
		try {
			return (String)a;
		} catch (ClassCastException e) {
			throw new ArgumentTypeMismatchException(n, "String", a.getClass().toString());
		}
	}
	
	/**
	 * ListValue view for iterators. Because iterators are not cloneable and are
	 * mutable, we have to cache the head and tail.
	 * 
	 * @author quark
	 */
	private static class IteratorListValue extends LazyListValue {
		private Iterator iterator;

		private TupleValue cons = null;

		private boolean forced = false;

		public IteratorListValue(Iterator iterator) {
			this.iterator = iterator;
		}

		private void force() {
			if (forced)
				return;
			forced = true;
			if (iterator.hasNext()) {
				cons = new TupleValue(iterator.next(), new IteratorListValue(
						iterator));
			}
		}

		@Override
		public void uncons(Token caller) {
			force();
			if (cons == null)
				caller.die();
			else
				caller.resume(cons);
		}

		@Override
		public void unnil(Token caller) {
			force();
			if (cons == null)
				caller.resume(Value.signal());
			else
				caller.die();
		}
	}

	public ListLike listLikeArg(int n) throws TokenException {
		Object a = getArg(n);
		if (a == null) {
			throw new ArgumentTypeMismatchException(n, "ListLike", "null");
		} else if (a instanceof ListLike) {
			return (ListLike) a;
		} else if (a instanceof Iterable) {
			Iterator it = ((Iterable) a).iterator();
			return new IteratorListValue(it);
		} else if (a instanceof Object[]) {
			Iterator it = Arrays.asList((Object[]) a).iterator();
			return new IteratorListValue(it);
		} else {
			throw new ArgumentTypeMismatchException(n, "ListLike", a.getClass().toString());
		}
	}
	
	/** A unary operator on numbers */
	public interface NumericUnaryOperator<T> {
		public T apply(BigInteger a);
		public T apply(BigDecimal a);
		public T apply(int a);
		public T apply(long a);
		public T apply(byte a);
		public T apply(short a);
		public T apply(double a);
		public T apply(float a);
	}
	
	/** A binary operator on numbers */
	public interface NumericBinaryOperator<T> {
		public T apply(BigInteger a, BigInteger b);
		public T apply(BigDecimal a, BigDecimal b);
		public T apply(int a, int b);
		public T apply(long a, long b);
		public T apply(byte a, byte b);
		public T apply(short a, short b);
		public T apply(double a, double b);
		public T apply(float a, float b);
	}
	
	/**
	 * Dispatch a binary operator based on the widest
	 * type of two numbers.
	 */
	public static <T> T applyNumericOperator(Number a, Number b, NumericBinaryOperator<T> op) {
		if (a instanceof BigDecimal) {
			if (b instanceof BigDecimal) {
				return op.apply((BigDecimal) a, (BigDecimal) b);
			} else {
				return op.apply((BigDecimal) a, BigDecimal.valueOf(b.doubleValue()));
			}
		} else if (b instanceof BigDecimal) {
			if (a instanceof BigDecimal) {
				return op.apply((BigDecimal) a, (BigDecimal) b);
			} else {
				return op.apply(BigDecimal.valueOf(a.doubleValue()), (BigDecimal) b);
			}
		} else if (a instanceof Double || b instanceof Double) {
			return op.apply(a.doubleValue(), b.doubleValue());
		} else if (a instanceof Float || b instanceof Float) {
			return op.apply(a.floatValue(), b.floatValue());
		} else if (a instanceof BigInteger) {
			if (b instanceof BigInteger) {
				return op.apply((BigInteger) a, (BigInteger) b);
			} else {
				return op.apply((BigInteger) a, BigInteger.valueOf(b.longValue()));
			}
		} else if (b instanceof BigInteger) {
			if (a instanceof BigInteger) {
				return op.apply((BigInteger) a, (BigInteger) b);
			} else {
				return op.apply(BigInteger.valueOf(a.longValue()), (BigInteger) b);
			}
		} else if (a instanceof Long || b instanceof Long) {
			return op.apply(a.longValue(), b.longValue());
		} else if (a instanceof Integer || b instanceof Integer) {
			return op.apply(a.intValue(), b.intValue());	
		} else if (a instanceof Short || b instanceof Short) {
			return op.apply(a.shortValue(), b.shortValue());
		} else if (a instanceof Byte || b instanceof Byte) {
			return op.apply(a.byteValue(), b.byteValue());
		} else {
			throw new OrcError("Unexpected Number type in ("
					+ a.getClass().toString()
					+ ", " + b.getClass().toString() + ")");
		}
	}

	/**
	 * Dispatch a unary operator based on the type of a number.
	 */
	public static <T> T applyNumericOperator(Number a, NumericUnaryOperator<T> op) {
		if (a instanceof BigDecimal) {
			return op.apply((BigDecimal) a);
		} else if (a instanceof Double) {
			return op.apply(a.doubleValue());
		} else if (a instanceof Float) {
			return op.apply(a.floatValue());
		} else if (a instanceof BigInteger) {
			return op.apply((BigInteger) a);
		} else if (a instanceof Long) {
			return op.apply(a.longValue());
		} else if (a instanceof Integer) {
			return op.apply(a.intValue());	
		} else if (a instanceof Short) {
			return op.apply(a.shortValue());
		} else if (a instanceof Byte) {
			return op.apply(a.byteValue());
		} else {
			throw new OrcError("Unexpected Number type in ("
					+ a.getClass().toString() + ")");
		}
	}

	public Iterator<Object> iterator() {
		return Arrays.asList(values).iterator();
	}
}
