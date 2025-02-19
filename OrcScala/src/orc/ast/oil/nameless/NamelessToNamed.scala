//
// NamelessToNamed.scala -- Scala trait NamelessToNamed
// Project OrcScala
//
// Created by dkitchin on Jul 10, 2010.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.ast.oil.nameless

import orc.ast.oil.named
import orc.ast.hasOptionalVariableName
import orc.ast.oil.named.{ BoundTypevar, BoundVar }

/** @author dkitchin
  */
// Conversions from nameless to named representations
trait NamelessToNamed {

  def namelessToNamed(e: Expression, context: List[BoundVar], typecontext: List[BoundTypevar]): named.Expression = {
    import hasOptionalVariableName._
    
    def recurse(e: Expression): named.Expression = namelessToNamed(e, context, typecontext)
    e -> {
      case Stop() => named.Stop()
      case a: Argument => namelessToNamed(a, context)
      case Call(target, args, typeargs) => {
        val newtarget = namelessToNamed(target, context)
        val newargs = args map { namelessToNamed(_, context) }
        val newtypeargs = typeargs map { _ map { namelessToNamed(_, typecontext) } }
        named.Call(newtarget, newargs, newtypeargs)
      }
      case Parallel(left, right) => named.Parallel(recurse(left), recurse(right))
      case Sequence(left, right) => {
        val x = new BoundVar(Some(unusedVariable))
        named.Sequence(recurse(left), x, namelessToNamed(right, x :: context, typecontext))
      }
      case Graft(value, body) => {
        val x = new BoundVar(Some(unusedVariable))
        named.Graft(x, recurse(value), namelessToNamed(body, x :: context, typecontext))
      }
      case Trim(f) => named.Trim(recurse(f))
      case Otherwise(left, right) => named.Otherwise(recurse(left), recurse(right))
      case New(st, bindings, t) => {
        // FIXME: this probably looses the self name information.
        val self = new BoundVar(Some(id"self${st match { case Some(v) => v; case _ => "" }}"))
        val defcontext = self :: context
        val newbindings = Map() ++ bindings.mapValues(namelessToNamed(_, defcontext, typecontext))
        named.New(self, st.map(namelessToNamed(_, typecontext)), newbindings, t.map(namelessToNamed(_, typecontext)))
      }
      case FieldAccess(obj, field) => named.FieldAccess(namelessToNamed(obj, context), field)
      case DeclareCallables(openvars, defs, body) => {
        val opennames = openvars map context
        val defnames = defs map { d => new BoundVar(d.optionalVariableName) }
        val defcontext = defnames.reverse ::: opennames.reverse ::: context
        val bodycontext = defnames.reverse ::: context
        val newdefs = for ((x, d) <- defnames zip defs) yield namelessToNamed(x, d, defcontext, typecontext)
        val newbody = namelessToNamed(body, bodycontext, typecontext)
        named.DeclareCallables(newdefs, newbody)
      }
      case DeclareType(t, body) => {
        val x = new BoundTypevar()
        val newTypeContext = x :: typecontext
        /* A type may be defined recursively, so its name is in scope for its own definition */
        val newt = namelessToNamed(t, newTypeContext)
        val newbody = namelessToNamed(body, context, newTypeContext)
        named.DeclareType(x, newt, newbody)
      }
      case HasType(body, expectedType) => {
        named.HasType(recurse(body), namelessToNamed(expectedType, typecontext))
      }
      case VtimeZone(timeOrder, body) => named.VtimeZone(namelessToNamed(timeOrder, context), recurse(body))
      case Hole(holeContext, holeTypeContext) => {
        val newHoleContext = holeContext mapValues { namelessToNamed(_, context) }
        val newHoleTypeContext = holeTypeContext mapValues { namelessToNamed(_, typecontext) }
        named.Hole(newHoleContext, newHoleTypeContext)
      }
    }
  }

  def namelessToNamed(a: Argument, context: List[BoundVar]): named.Argument =
    a -> {
      case Constant(v) => named.Constant(v)
      case Variable(i) => context(i)
      case UnboundVariable(s) => named.UnboundVar(s)
    }

  def namelessToNamed(t: Type, typecontext: List[BoundTypevar]): named.Type = {
    def toType(t: Type): named.Type = namelessToNamed(t, typecontext)
    t -> {
      case TypeVar(i) => typecontext(i)
      case UnboundTypeVariable(u) => named.UnboundTypevar(u)
      case Top() => named.Top()
      case Bot() => named.Bot()
      case FunctionType(typearity, argtypes, returntype) => {
        val typeformals = (for (_ <- 0 until typearity) yield new BoundTypevar()).toList
        val newTypeContext = typeformals ::: typecontext
        val newArgTypes = argtypes map { namelessToNamed(_, newTypeContext) }
        val newReturnType = namelessToNamed(returntype, newTypeContext)
        named.FunctionType(typeformals, newArgTypes, newReturnType)
      }
      case TupleType(elements) => named.TupleType(elements map toType)
      case RecordType(entries) => {
        val newEntries = entries map { case (s, t) => (s, toType(t)) }
        named.RecordType(newEntries)
      }
      case TypeApplication(i, typeactuals) => {
        val tycon = typecontext(i)
        val newTypeActuals = typeactuals map toType
        named.TypeApplication(tycon, newTypeActuals)
      }
      case AssertedType(assertedType) => named.AssertedType(toType(assertedType))
      case TypeAbstraction(typearity, t) => {
        val typeformals = (for (_ <- 0 until typearity) yield new BoundTypevar()).toList
        val newTypeContext = typeformals ::: typecontext
        val newt = namelessToNamed(t, newTypeContext)
        named.TypeAbstraction(typeformals, newt)
      }
      case ImportedType(classname) => named.ImportedType(classname)
      case ClassType(classname) => named.ClassType(classname)
      case VariantType(typearity, variants) => {
        val self = new BoundTypevar()
        val typeformals = (for (_ <- 0 until typearity) yield new BoundTypevar()).toList
        val newTypeContext = self :: typeformals ::: typecontext
        val newVariants =
          for ((name, variant) <- variants) yield {
            (name, variant map { namelessToNamed(_, newTypeContext) })
          }
        named.VariantType(self, typeformals, newVariants)
      }
      case IntersectionType(a, b) => named.IntersectionType(toType(a), toType(b))
      case UnionType(a, b) => named.UnionType(toType(a), toType(b))
      case StructuralType(members) => named.StructuralType(members.mapValues(toType))
      case NominalType(a) => named.NominalType(toType(a))
    }
  }

  def namelessToNamed(x: BoundVar, defn: Callable, context: List[BoundVar], typecontext: List[BoundTypevar]): named.Callable = {
    defn -> {
      case Callable(typearity, arity, body, argtypes, returntype) => {
        val formals = (for (_ <- 0 until arity) yield new BoundVar(None)).toList
        val typeformals = (for (_ <- 0 until typearity) yield new BoundTypevar()).toList
        val newContext = formals ::: context
        val newTypeContext = typeformals ::: typecontext
        val newbody = namelessToNamed(body, newContext, newTypeContext)
        val newArgTypes = argtypes map { _ map { namelessToNamed(_, newTypeContext) } }
        val newReturnType = returntype map { namelessToNamed(_, newTypeContext) }
        defn match {
          case _: Def =>
            named.Def(x, formals, newbody, typeformals, newArgTypes, newReturnType)
          case _: Site =>
            named.Site(x, formals, newbody, typeformals, newArgTypes, newReturnType)
        }
      }
    }
  }

}
