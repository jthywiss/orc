package orc.ast.oil.xml;

import orc.Config;
import orc.error.compiletime.CompilationException;

public class Silent extends Expression {
	@Override
	public orc.ast.oil.Expr unmarshal(Config config) throws CompilationException {
		return new orc.ast.oil.Silent();
	}
}