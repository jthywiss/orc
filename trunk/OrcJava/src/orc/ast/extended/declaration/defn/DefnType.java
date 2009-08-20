package orc.ast.extended.declaration.defn;

import java.util.List;

import orc.ast.extended.Visitor;
import orc.ast.extended.expression.Expression;
import orc.ast.extended.pattern.Pattern;
import orc.ast.extended.type.ArrowType;
import orc.ast.extended.type.Type;

public class DefnType extends Defn {

		public List<Type> argTypes;
		public Type resultType;
		public List<String> typeParams;
	
		public DefnType(String name, List<Type> argTypes, Type resultType, List<String> typeParams) {
			this.name = name;
			this.argTypes = argTypes;
			this.resultType = resultType;
			this.typeParams = typeParams;
		}

		public String toString() {
			return "def " + name + " (" + Expression.join(argTypes, ", ") + ") :: " + resultType;
		}

		/* (non-Javadoc)
		 * @see orc.ast.extended.ASTNode#accept(orc.ast.oil.Visitor)
		 */
		public <E> E accept(Visitor<E> visitor) {
			return visitor.visit(this);
		}

		@Override
		public void extend(AggregateDefn adef) {
			adef.setTypeParams(typeParams);
			adef.setArgTypes(argTypes);
			if (resultType != null) { adef.setResultType(resultType); }
			
			adef.addLocation(getSourceLocation());
		}	
	
}
