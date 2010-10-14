//
// Named.scala -- Named representation of OIL syntax
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on May 28, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil.named



import orc.ast.oil._
import orc.ast.AST


sealed abstract class NamedAST extends AST with NamedToNameless {
  def prettyprint() = (new PrettyPrint()).reduce(this)
  override def toString() = prettyprint()
  
  override val subtrees: List[NamedAST] = this match {
    case Call(target, args, typeargs) => target :: ( args ::: typeargs.toList.flatten )
    case left || right => List(left, right)
    case Sequence(left,x,right) => List(left, x, right)
    case Prune(left,x,right) => List(left, x, right)
    case left ow right => List(left, right)
    case Atomic(body) => List(body)
    case DeclareDefs(defs, body) => defs ::: List(body)
    case HasType(body, expectedType) => List(body, expectedType)
    case DeclareType(u, t, body) => List(u, t, body)
    case Def(f, formals, body, typeformals, argtypes, returntype) => {
      f :: ( formals ::: ( List(body) ::: typeformals ::: argtypes.toList.flatten ::: returntype.toList ) )
    }
    case TupleType(elements) => elements
    case TypeApplication(tycon, typeactuals) => tycon :: typeactuals
    case AssertedType(assertedType) => List(assertedType)
    case TypeAbstraction(typeformals, t) => typeformals ::: List(t)
    case VariantType(variants) => {
      for ((_, variant) <- variants; Some(t) <- variant) yield t
    }
    case _ => Nil
  }
  
}


sealed abstract class Expression
extends NamedAST 
with NamedInfixCombinators 
with hasVars
with Substitution[Expression]
with ContextualSubstitution
with Guarding { 
  lazy val withoutNames: nameless.Expression = namedToNameless(this, Nil, Nil)
}

case class Stop() extends Expression
case class Call(target: Argument, args: List[Argument], typeargs: Option[List[Type]]) extends Expression
case class Parallel(left: Expression, right: Expression) extends Expression
case class Sequence(left: Expression, x: BoundVar, right: Expression) extends Expression
case class Prune(left: Expression, x: BoundVar, right: Expression) extends Expression
case class Otherwise(left: Expression, right: Expression) extends Expression
case class Atomic(body: Expression) extends Expression
case class DeclareDefs(defs: List[Def], body: Expression) extends Expression
case class DeclareType(name: BoundTypevar, t: Type, body: Expression) extends Expression
case class HasType(body: Expression, expectedType: Type) extends Expression
case class Hole(context: Map[String, Argument], typecontext: Map[String, Type]) extends Expression {
  def apply(e: Expression): Expression = e.subst(context, typecontext)
}

/* Match an expression with exactly one hole. 
 * Matches as Module(f), where f is a function which takes
 * a hole-filling expression and returns this expression
 * with the hole filled. 
 */
object Module {
  def unapply(e: Expression): Option[Expression => Expression] = {
    if (countHoles(e) == 1) {
      def fillWith(fill: Expression): Expression = {
        val transform = new NamedASTTransform {
          override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {
            case h : Hole => h(fill)
          }
        }
        transform(e)
      }
      Some(fillWith)
    }
    else {
      None
    }
  }
  
  def countHoles(e: Expression): Int = {
    var holes = 0 
    val search = new NamedASTTransform {
      override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {
        case h : Hole => holes += 1 ; h
      }
    }
    search(e)
    holes
  }
}

sealed abstract class Argument extends Expression
case class Constant(value: AnyRef) extends Argument
trait Var extends Argument
  case class UnboundVar(name: String) extends Var
  class BoundVar(val optionalName: Option[String] = None) extends Var {
    def this(name: String) = this(Some(name))
    def productIterator = if (optionalName.isDefined) List(optionalName.get).iterator else Nil.iterator
  }




sealed case class Def(name: BoundVar, formals: List[BoundVar], body: Expression, typeformals: List[BoundTypevar], argtypes: Option[List[Type]], returntype: Option[Type]) 
extends NamedAST 
with hasFreeVars 
with hasFreeTypeVars
with Substitution[Def] { 
  lazy val withoutNames: nameless.Def = namedToNameless(this, Nil, Nil)  
}




sealed abstract class Type 
extends NamedAST 
with hasFreeTypeVars 
with Substitution[Type] { 
  lazy val withoutNames: nameless.Type = namedToNameless(this, Nil) 
}	
case class Top() extends Type
case class Bot() extends Type
case class TupleType(elements: List[Type]) extends Type
case class RecordType(entries: Map[String,Type]) extends Type
case class TypeApplication(tycon: Type, typeactuals: List[Type]) extends Type
case class AssertedType(assertedType: Type) extends Type	
case class FunctionType(typeformals: List[BoundTypevar], argtypes: List[Type], returntype: Type) extends Type
case class TypeAbstraction(typeformals: List[BoundTypevar], t: Type) extends Type
case class ImportedType(classname: String) extends Type
case class ClassType(classname: String) extends Type
case class VariantType(variants: List[(String, List[Option[Type]])]) extends Type


trait Typevar extends Type
  case class UnboundTypevar(name: String) extends Typevar
  class BoundTypevar(val optionalName: Option[String] = None) extends Typevar {
    def this(name: String) = this(Some(name))
    def productIterator = if (optionalName.isDefined) List(optionalName.get).iterator else Nil.iterator

  }
