package orc.ast.extended.type;


/**
 * 
 * Abstract superclass of syntactic types in the extended AST.
 * 
 * Syntactic types occur in all of the AST forms. The typechecker
 * converts them to a different form (subclasses of orc.type.Type)
 * for its own internal use.
 * 
 * Syntactic types do not have methods like meet, join, and subtype; only their
 * typechecker counterparts do. Thus, syntactic types permit only the simplest
 * analyses; more complex analyses must wait until the syntactic type is
 * converted within the typechecker.
 * 
 * All syntactic types can be written explicitly in a program, whereas
 * many of the typechecker's internal types are not representable in programs.
 * 
 * @author dkitchin
 *
 */
public abstract class Type {

	/* Create singleton representatives for some common types */
	public static final Type BLANK = new Blank();
	public static final Type TOP = new Top();
	public static final Type BOT = new Bot();
	
	/** 
	 * Convert this extended AST type into a simple AST type.
	 */
	public abstract orc.ast.simple.type.Type simplify();
	
}
