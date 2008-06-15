package orc.orchard.oil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.StringReader;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import orc.orchard.InvalidOilException;
import orc.orchard.InvalidProgramException;
import orc.orchard.java.CompilerService;

public class Oil implements Serializable {
	@XmlAttribute
	public String version;
	public Expression expression;
	public Oil() {}
	public Oil(String version, Expression expression) {
		this.version = version;
		this.expression = expression;
	}
	public String toString() {
		return super.toString() + "(" + version + ", " + expression + ")";
	}
	public orc.ast.oil.Expr unmarshal() throws InvalidOilException {
		return expression.unmarshal();
	}
	public String toXML() {
		ByteArrayOutputStream out = new ByteArrayOutputStream(); 
		JAXB.marshal(this, out);
		return out.toString();
	}
	public static Oil fromXML(String xml) {
		StringReader in = new StringReader(xml);
		return JAXB.unmarshal(in, Oil.class);
	}
	
	/**
	 * Simple test of Oil marhsaling and unmarshaling.
	 * @param args
	 * @throws InvalidOilException
	 */
	public static void main(String[] args) throws InvalidOilException {
		CompilerService compiler = new CompilerService();
		Oil oil;
		try {
			oil = compiler.compile("def M(x) = x | Rtimer(1000) >> M(x+1) M(1)");
		} catch (InvalidProgramException e) {
			// this is impossible by construction
			throw new AssertionError(e);			
		}
		String xml = oil.toXML();
		System.out.println(xml);
		oil = Oil.fromXML(xml);
		System.out.println(oil.unmarshal().toString());
	}
}