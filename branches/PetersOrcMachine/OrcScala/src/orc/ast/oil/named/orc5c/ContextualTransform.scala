//
// ContextualTransform.scala -- Scala class/trait/object ContextualTransform
// Project OrcScala
//
// $Id$
//
// Created by amp on May 31, 2013.
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.named.orc5c

trait ContextualTransform extends Orc5CASTFunction {
  import Bindings._
  
  def order[E <: Orc5CAST](pf: PartialFunction[E, E], descend: E => E)(e: E): E
  
  def apply(a: Argument): Argument = transform(a)(TransformContext())
  def apply(e: Expression): Expression = transform(e)(TransformContext())
  def apply(t: Type): Type = transform(t)(TransformContext())
  def apply(d: Def): Def = transform(d)(TransformContext())

  def onExpression(implicit ctx: TransformContext): PartialFunction[Expression, Expression] = new PartialFunction[Expression, Expression] {
    def isDefinedAt(e: Expression) = pf.isDefinedAt(e in ctx)
    def apply(e: Expression) = pf(e in ctx)
    
    val pf = onExpressionCtx
  }
  def onExpressionCtx: PartialFunction[WithContext[Expression], Expression] = EmptyFunction

  def onArgument(implicit ctx: TransformContext): PartialFunction[Argument, Argument] = EmptyFunction

  def onType(implicit ctx: TransformContext): PartialFunction[Type, Type] = EmptyFunction

  def onDef(implicit ctx: TransformContext): PartialFunction[Def, Def] = EmptyFunction

  def recurseWithContext(implicit ctx: TransformContext) =
    new Orc5CASTFunction {
      def apply(a: Argument) = transform(a)
      def apply(e: Expression) = transform(e)
      def apply(t: Type) = transform(t)
      def apply(d: Def) = transform(d)
    }

  def transform(a: Argument)(implicit ctx: TransformContext): Argument = {
    order[Argument](onArgument, (x:Argument) => x)(a)
    // FIXME: This is probably going to cause variable renaming.
    /*val pf = onArgument
    if (pf isDefinedAt a) {
      val v = pf(a)
      a.pushDownPosition(v.pos)
      // We are replacing an argument, do not transfer variable name
      v
    } else
      a*/
  }

  def transform(expr: Expression)(implicit ctx: TransformContext): Expression = {
    val recurse = recurseWithContext
    order[Expression](onExpression, {
        case Stop() => Stop()
        case a: Argument => recurse(a)
        case Call(target, args, typeargs) => {
          val newtarget = recurse(target)
          val newargs = args map { recurse(_) }
          val newtypeargs = typeargs map { _ map { recurse(_) } }
          Call(newtarget, newargs, newtypeargs)
        }
        case left || right => recurse(left) || recurse(right)
        case e@(left > x > right) => recurse(left) > x > transform(right)(ctx + SeqBound(ctx, e.asInstanceOf[Sequence]))
        case e@(left < x <| right) => transform(left)(ctx + LateBound(ctx, e.asInstanceOf[LateBind])) < x <| recurse(right)
        case left ow right => recurse(left) ow recurse(right)
        case Limit(f) => Limit(recurse(f))
        case e@DeclareDefs(defs, body) => {
          val newctxrec = ctx extendBindings (defs map { RecursiveDefBound(ctx, e, _) })
          val newdefs = defs map { transform(_)(newctxrec) }
          val newctx = ctx extendBindings (defs map { DefBound(newctxrec, e, _) })
          val newbody = transform(body)(newctx)
          DeclareDefs(newdefs, newbody)
        }
        case e@DeclareType(u, t, body) => {
          val newctx = ctx + TypeBinding(ctx, u)
          val newt = transform(t)(newctx)
          val newbody = transform(body)(newctx)
          DeclareType(u, newt, newbody)
        }
        case HasType(body, expectedType) => HasType(recurse(body), recurse(expectedType))
        case VtimeZone(timeOrder, body) => VtimeZone(recurse(timeOrder), recurse(body))
      })(expr)
    /*val pf = onExpression
    if (pf isDefinedAt e) {
      e -> pf
    } else {
      val recurse = recurseWithContext
      e -> {
        case Stop() => Stop()
        case a: Argument => recurse(a)
        case Call(target, args, typeargs) => {
          val newtarget = recurse(target)
          val newargs = args map { recurse(_) }
          val newtypeargs = typeargs map { _ map { recurse(_) } }
          Call(newtarget, newargs, newtypeargs)
        }
        case left || right => recurse(left) || recurse(right)
        case left > x > right => recurse(left) > x > transform(right)(ctx + SeqBound(ctx, e.asInstanceOf[Sequence]))
        case e@(left < x <| right) => transform(left)(ctx + LateBound(ctx, e)) < x <| recurse(right)
        case left ow right => recurse(left) ow recurse(right)
        case Limit(f) => recurse(f)
        case e@DeclareDefs(defs, body) => {
          val newctxrec = ctx extendBindings (defs map { RecursiveDefBound(ctx, e, _) })
          val newdefs = defs map { transform(_)(newctxrec) }
          val newctx = ctx extendBindings (defs map { DefBound(ctx, e, _) })
          val newbody = transform(body)(newctx)
          DeclareDefs(newdefs, newbody)
        }
        case e@DeclareType(u, t, body) => {
          val newctx = ctx + TypeBinding(ctx, u)
          val newt = transform(t)(newctx)
          val newbody = transform(body)(newctx)
          DeclareType(u, newt, newbody)
        }
        case HasType(body, expectedType) => HasType(recurse(body), recurse(expectedType))
        case VtimeZone(timeOrder, body) => VtimeZone(recurse(timeOrder), recurse(body))
      }
    }*/
  }

  def transform(t: Type)(implicit ctx: TransformContext): Type = {
    def recurse(t: Type) = transform(t)
    order[Type](onType, {
        case Bot() => Bot()
        case Top() => Top()
        case ImportedType(cl) => ImportedType(cl)
        case ClassType(cl) => ClassType(cl)
        case u: Typevar => u
        case TupleType(elements) => TupleType(elements map recurse)
        case RecordType(entries) => {
          val newEntries = entries map { case (s, t) => (s, recurse(t)) }
          RecordType(newEntries)
        }
        case TypeApplication(tycon, typeactuals) => {
          TypeApplication(recurse(tycon), typeactuals map recurse)
        }
        case AssertedType(assertedType) => AssertedType(recurse(assertedType))
        case FunctionType(typeformals, argtypes, returntype) => {
          val newtypecontext = ctx.extendTypeBindings(typeformals map {TypeBinding(ctx, _)})
          val newargtypes = argtypes map { transform(_)(newtypecontext) }
          val newreturntype = transform(returntype)(newtypecontext)
          FunctionType(typeformals, newargtypes, newreturntype)
        }
        case TypeAbstraction(typeformals, t) => {
          TypeAbstraction(typeformals, transform(t)(ctx.extendTypeBindings(typeformals map {TypeBinding(ctx, _)})))
        }
        case VariantType(self, typeformals, variants) => {
          val newTypeContext = ctx.extendTypeBindings(typeformals map {TypeBinding(ctx, _)}) + TypeBinding(ctx, self)
          val newVariants =
            for ((name, variant) <- variants) yield {
              (name, variant map { transform(_)(newTypeContext) })
            }
          VariantType(self, typeformals, newVariants)
        }
      })(t)
    /*val pf = onType
    if (pf isDefinedAt t) {
      t -> pf
    } else {
      def recurse(t: Type) = transform(t)
      t -> {
        case Bot() => Bot()
        case Top() => Top()
        case ImportedType(cl) => ImportedType(cl)
        case ClassType(cl) => ClassType(cl)
        case u: Typevar => u
        case TupleType(elements) => TupleType(elements map recurse)
        case RecordType(entries) => {
          val newEntries = entries map { case (s, t) => (s, recurse(t)) }
          RecordType(newEntries)
        }
        case TypeApplication(tycon, typeactuals) => {
          TypeApplication(recurse(tycon), typeactuals map recurse)
        }
        case AssertedType(assertedType) => AssertedType(recurse(assertedType))
        case FunctionType(typeformals, argtypes, returntype) => {
          val newtypecontext = ctx.extendTypeBindings(typeformals map {TypeBinding(ctx, _)})
          val newargtypes = argtypes map { transform(_)(newtypecontext) }
          val newreturntype = transform(returntype)(newtypecontext)
          FunctionType(typeformals, newargtypes, newreturntype)
        }
        case TypeAbstraction(typeformals, t) => {
          TypeAbstraction(typeformals, transform(t)(ctx.extendTypeBindings(typeformals map {TypeBinding(ctx, _)})))
        }
        case VariantType(self, typeformals, variants) => {
          val newTypeContext = ctx.extendTypeBindings(typeformals map {TypeBinding(ctx, _)}) + TypeBinding(ctx, self)
          val newVariants =
            for ((name, variant) <- variants) yield {
              (name, variant map { transform(_)(newTypeContext) })
            }
          VariantType(self, typeformals, newVariants)
        }
      }
    }*/
  }

  def transform(d: Def)(implicit ctx: TransformContext): Def = {
    order[Def](onDef, {
      case d @ Def(name, formals, body, typeformals, argtypes, returntype) => {
        val newcontext = ctx extendBindings (formals map { ArgumentBound(ctx, d, _) }) extendTypeBindings (typeformals map { TypeBinding(ctx, _) })
        val newbody = transform(body)(newcontext)
        val newargtypes = argtypes map { _ map { transform(_)(newcontext) } }
        val newreturntype = returntype map { transform(_)(newcontext) }
        Def(name, formals, newbody, typeformals, newargtypes, newreturntype)
      }
    })(d)
    /*val pf = onDef
    if (pf isDefinedAt d) {
      d -> pf
    } else {
      d -> {
        case d@Def(name, formals, body, typeformals, argtypes, returntype) => {
          val newcontext = ctx extendBindings (formals map {ArgumentBound(ctx, d, _)}) extendTypeBindings (typeformals map {TypeBinding(ctx, _)})
          val newbody = transform(body)(newcontext)
          val newargtypes = argtypes map { _ map { transform(_)(newcontext) } }
          val newreturntype = returntype map { transform(_)(newcontext) }
          Def(name, formals, newbody, typeformals, newargtypes, newreturntype)
        }
      }
    }*/
  }

}

object ContextualTransform {
  trait NonDescending extends ContextualTransform {
    def order[E <: Orc5CAST](pf: PartialFunction[E, E], descend: E => E)(e: E): E = {
      e ->> pf.applyOrElse(e, descend)
    }
  }
  trait Pre extends ContextualTransform {
    def order[E <: Orc5CAST](pf: PartialFunction[E, E], descend: E => E)(e: E): E = {
      val e1 = e ->> pf.lift(e).getOrElse(e)
      e1 ->> descend(e1)
    }
  }
  trait Post extends ContextualTransform {
    def order[E <: Orc5CAST](pf: PartialFunction[E, E], descend: E => E)(e: E): E = {
      val e1 = e ->> descend(e)
      e1 ->> pf.lift(e1).getOrElse(e1)
    }
  }
}
