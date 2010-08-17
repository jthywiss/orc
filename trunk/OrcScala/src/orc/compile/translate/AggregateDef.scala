//
// AggregateDef.scala -- Scala class AggregateDef
// Project OrcScala
//
// $Id$
//
// Created by dkitchin on Jun 3, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.compile.translate

import orc.util.OptionMapExtension._
import orc.ast.ext._
import orc.ast.oil.named
import orc.error.compiletime._
import orc.error.OrcExceptionExtension._

case class AggregateDef(clauses: List[Clause],
  typeformals: Option[List[String]],
  argtypes: Option[List[Type]],
  returntype: Option[Type],
  translator: Translator) extends orc.ast.AST {

  import translator._
  
  def unify[A](x: Option[A], y: Option[A], reportCollision: => Unit): Option[A] =
    (x, y) match {
      case (None, None) => None
      case (Some(x), None) => Some(x)
      case (None, Some(y)) => Some(y)
      case (Some(x), Some(y)) => reportCollision ; Some(x)
    }
  def unifyList[A](x: Option[List[A]], y: Option[List[A]], reportCollision: => Unit): Option[List[A]] =
    (x, y) match {
      case (None, None) => None
      case (Some(x), None) => Some(x)
      case (None, Some(y)) => Some(y)
      case (Some(Nil), Some(Nil)) => Some(Nil) // Nils are allowed to unify
      case (Some(x), Some(y)) => reportCollision ; Some(x)
    }

  def +(defn: DefDeclaration): AggregateDef =
    defn -> {
      case Def(_, List(formals), maybeReturnType, body) => {
        val (newformals, maybeArgTypes) = AggregateDef.formalsPartition(formals)
        val newclause = defn ->> Clause(newformals, body)
        val newArgTypes = unifyList(argtypes, maybeArgTypes, reportProblem(RedundantArgumentType() at defn))
        val newReturnType = unify(returntype, maybeReturnType, reportProblem(RedundantReturnType() at defn))
        AggregateDef(clauses ::: List(newclause), typeformals, newArgTypes, newReturnType, translator)
      }
      case DefCapsule(name, List(formals), maybeReturnType, body) => {
        this + Def(name, List(formals), maybeReturnType, new Capsule(body))
      }
      case DefSig(_, typeformals2, argtypes2, maybeReturnType) => {
        val argtypes3 = argtypes2 head // List[List[Type]] has only one entry
        val newTypeFormals = unifyList(typeformals, Some(typeformals2), reportProblem(RedundantTypeParameters() at defn))
        val newArgTypes = unifyList(argtypes, Some(argtypes3), reportProblem(RedundantArgumentType() at defn))
        val newReturnType = unify(returntype, Some(maybeReturnType), reportProblem(RedundantReturnType() at defn))
        AggregateDef(clauses, newTypeFormals, newArgTypes, newReturnType, translator)
      }
    }

  def +(lambda: Lambda): AggregateDef = {
    val (newformals, maybeArgTypes) = AggregateDef.formalsPartition(lambda.formals.head)
    val newclause = lambda ->> Clause(newformals, lambda.body)
    val newArgTypes = unifyList(argtypes, maybeArgTypes, reportProblem(RedundantArgumentType() at lambda))
    val newReturnType = unify(returntype, lambda.returntype, reportProblem(RedundantReturnType() at lambda))
    AggregateDef(clauses ::: List(newclause), typeformals, newArgTypes, newReturnType, translator)
  }

  def convert(x : named.BoundVar, context: Map[String, named.Argument], typecontext: Map[String, named.Type]): named.Def = {
    if (clauses.isEmpty) { reportProblem(UnusedFunctionSignature() at this) }

    val (newTypeFormals, dtypecontext) = convertTypeFormals(typeformals.getOrElse(Nil), this)
    val newtypecontext = typecontext ++ dtypecontext
    val newArgTypes = argtypes map { _ map { convertType(_, newtypecontext) } }
    val newReturnType = returntype map { convertType(_, newtypecontext) }

    val (newformals, newbody) = Clause.convertClauses(clauses, context, newtypecontext, translator)

    named.Def(x, newformals, newbody, newTypeFormals, newArgTypes, newReturnType)
  }

  def capsuleCheck {
    var existsCapsule = false
    var existsNotCapsule = false
    for (aclause <- clauses) {
      aclause match {
        case Clause(_, Capsule(_)) =>
          if (existsNotCapsule) { reportProblem(CapsuleDefInNoncapsuleContext() at aclause) }
          else existsCapsule = true
        case _ =>
          if (existsCapsule) { reportProblem(NoncapsuleDefInCapsuleContext() at aclause) }
          else existsNotCapsule = true
      }
    }
  }

}

object AggregateDef {

  def formalsPartition(formals: List[Pattern]): (List[Pattern], Option[List[Type]]) = {
    val maybePartitioned =
      formals optionMap {
        case TypedPattern(p, t) => Some(p, t)
        case _ => None
      }
    maybePartitioned match {
      case Some(l) => {
        val (ps, ts) = l.unzip
        (ps, Some(ts))
      }
      case None => (formals, None)
    }
  }

  def empty(tl: Translator) = new AggregateDef(Nil, None, None, None, tl)

  def apply(defn: DefDeclaration, tl: Translator) = defn -> { empty(tl) + _ }
  def apply(lambda: Lambda, tl: Translator) = lambda -> { empty(tl) + _ }

}

