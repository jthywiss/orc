//
// Project OrcScala
//
// $Id$
//
// Created by amp on Jan 26, 2015.
//
// Copyright (c) 2014 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.compile.translate

import scala.language.reflectiveCalls
import orc.ast.ext
import orc.lib.builtin
import orc.error.compiletime._
import orc.error.OrcExceptionExtension._
import orc.ast.oil.named._
import orc.ast.ext.ExtendedASTTransform
import orc.values.Field
import scala.collection.mutable
import orc.ast.ext.ClassSubclassLiteral

trait ClassBasicInfo {
  val linearization: Class.Linearization
  val members: Set[String]
  val concreteMembers: Set[String]
}
case class AnonymousClassInfo(linearization: Class.Linearization, members: Set[String], concreteMembers: Set[String]) extends ClassBasicInfo
case class ClassInfo(name: BoundVar, linearization: Class.Linearization, members: Set[String], concreteMembers: Set[String], literal: ext.ClassLiteral) extends ClassBasicInfo

/** Helper functions for class conversion
  * @author amp
  */
case class ClassForms(val translator: Translator) {
  import translator._

  /** Used to generate unique names for pattern match temporary fields.
    *
    */
  var tmpId = 1
  // TODO: tmpId could be local to each class if we supported locals that are not visible from subclasses and don't conflict with them.

  def concatUniquePreferRight[T](a: List[T], b: List[T]): List[T] = a match {
    case v +: vs => if (b contains v) concatUniquePreferRight(vs, b) else v +: concatUniquePreferRight(vs, b)
    case Seq() => b
  }
  implicit class UniqueConcatOperator[T](a: List[T]) {
    def +>(b: List[T]) = concatUniquePreferRight(a, b)
  }

  /** Return the ClassInfo of the ClassExpression. As a side effect add any classes needed by
    * it to additionalClasses. If name is defined then give the class that name.
    *
    */
  def linearize(e: ext.ClassExpression, additionalClasses: mutable.Buffer[ClassInfo], name: Option[BoundVar] = None)(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: collection.Map[String, ClassInfo]): ClassBasicInfo = {
    e match {
      case lit: ext.ClassLiteral =>
        val info = makeClassInfo(name, lit, AnonymousClassInfo(Nil, Set(), Set()))
        additionalClasses += info
        info
      case ext.ClassMixin(a, b) =>
        val bi = linearize(b, additionalClasses)
        val ai = linearize(a, additionalClasses)
        // TODO: Make sure this catches enough cases to be useful.
        checkForReorderings(ai, bi, e)
        AnonymousClassInfo(bi.linearization +> ai.linearization,
          bi.members ++ ai.members, bi.concreteMembers ++ ai.concreteMembers)
      case ext.ClassSubclassLiteral(s, b) =>
        val si = linearize(s, additionalClasses)
        val info = makeClassInfo(name, b, si)
        additionalClasses += info
        info
      case ext.ClassVariable(v) =>
        classcontext.getOrElse(v, {
          throw (UnboundClassVariableException(v) at e)
        })
    }
  }

  def checkForReorderings(a: ClassBasicInfo, b: ClassBasicInfo, e: ext.ClassExpression)(implicit classcontext: collection.Map[String, ClassInfo]): Unit = {
    for ((c :: tail) <- a.linearization.tails) {
      val tailSet = tail.toSet
      // If c appears in the linearization of b
      if (b.linearization.contains(c)) {
        // If something after c in the linearization of a appears before c in the linearization of b
        val beforeCinB = b.linearization.takeWhile(_ != c)
        val misorderedClasses = beforeCinB.filter(tailSet contains _)
        if (!misorderedClasses.isEmpty) {
          val conflictingClasses = (c :: misorderedClasses).toSet
          // TODO: Check if both c and at least one class in misorderedClasses declare the same member
          translator.reportProblem(ConflictingOrderWarning(
            a.linearization.filter(conflictingClasses.contains).map(_.name.toString),
            b.linearization.filter(conflictingClasses.contains).map(_.name.toString)) at e)
        }
      }
    }
  }

  /** Return the list of field names declared in the ClassLiteral.
    *
    * The first set is all members; the second is only concrete members.
    */
  def findFieldNames(lit: ext.ClassLiteral): (Set[String], Set[String]) = {
    val bindingVisitor = new ExtendedASTTransform {
      var otherNames = Set[String]()
      var concreteNames = Set[String]()

      override def onPattern() = {
        case p @ ext.VariablePattern(n) => concreteNames += n; p
        case p @ ext.AsPattern(pat, n) => concreteNames += n; this(pat); p
      }

      override def onDeclaration() = {
        case d: ext.Val => this(d.p); d
        case d @ ext.ValSig(n, _) => otherNames += n; d
        case d: ext.CallableSig => otherNames += d.name; d
        case d: ext.NamedDeclaration => concreteNames += d.name; d
        case d => d
      }
    }
    lit.decls.foreach(bindingVisitor.apply)
    (bindingVisitor.otherNames ++ bindingVisitor.concreteNames, bindingVisitor.concreteNames)
  }

  /** Build a ClassInfo object based on the given info.
    *
    * This just does a few computations to extract the needed information build the ClassInfo.
    */
  def makeClassInfo(name: Option[BoundVar], lit: ext.ClassLiteral, superClassInfo: ClassBasicInfo)(implicit context: Map[String, Argument], typecontext: Map[String, Type]): ClassInfo = {
    val clsname = name getOrElse new BoundVar(Some(Var.getNextVariableName("synCls")))
    val linearization = Classvar(clsname) :: superClassInfo.linearization
    val (allmembers, concretemembers) = findFieldNames(lit)
    ClassInfo(clsname, linearization, superClassInfo.members ++ allmembers, superClassInfo.concreteMembers ++ concretemembers, lit)
  }

  /** Given a ClassLiteral and a set of fields on this, build all field expressions for the class.
    *
    * Return the BoundVar for this and the mapping of fields to expressions.
    */
  def convertClassLiteral(lit: ext.ClassLiteral, fieldNames: Set[String])(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo]): (BoundVar, BoundVar, Map[Field, Expression]) = {
    val thisName = lit.thisname.getOrElse("this")
    val thisVar = new BoundVar(Some(thisName))
    val superVar = new BoundVar(Some("super"))

    val thisContext = List((thisName, thisVar), ("this", thisVar), ("super", superVar))

    // Remove field names from the context this makes fields shadow outside definitions
    val memberContext = (context -- fieldNames) ++ thisContext

    def convertFields(d: Seq[ext.Declaration]): Seq[(Field, Option[Expression])] = d match {
      // NOTE: The first two cases are optimizations. The third case also covers them, but also introduces extra fields.
      case ext.Val(ext.VariablePattern(x), f) +: rest =>
        (Field(x), Some(convert(f)(memberContext, implicitly, implicitly))) +: convertFields(rest)
      case ext.Val(ext.TypedPattern(ext.VariablePattern(x), t), f) +: rest =>
        (Field(x), Some(HasType(convert(f)(memberContext, implicitly, implicitly), convertType(t)))) +: convertFields(rest)
      case ext.Val(p, f) +: rest =>
        val tmpField = Field(s"$$pattmp$$$tmpId")
        tmpId += 1
        // FIXME: Is it a problem if the same variable is bound in multiple subtrees?
        val x = new BoundVar()
        val (source, dcontext, target) = convertPattern(p, x)(memberContext, implicitly)
        val newf = convert(f)(memberContext, implicitly, implicitly)
        val generatedFields = for ((name, code) <- dcontext.toSeq) yield (Field(name), Some(FieldAccess(thisVar, tmpField) > x > target(code)))
        (tmpField, Some(source(newf))) +: (generatedFields ++ convertFields(rest))
      case ext.ValSig(v, t) +: rest =>
        val field = Field(v)
        // TODO: Store type for later use in type checking.
        (field, None) +: convertFields(rest)
      case ext.CallableSingle(defs, rest) =>
        assert(defs.forall(_.name == defs.head.name))
        val field = Field(defs.head.name)
        val name = new BoundVar(Some("$" + defs.head.name))
        val agg = defs.foldLeft(AggregateDef.empty(translator))(_ + _)
        if (agg.clauses.isEmpty) {
          // Abstract
          // TODO: Store type for later use in type checking.
          (field, None) +: convertFields(rest)
        } else {
          // Concrete
          val newdef = agg ->> agg.convert(name, memberContext, implicitly, implicitly)
          (field, Some(DeclareCallables(List(newdef), name))) +: convertFields(rest)
        }
      case Seq() =>
        Nil
      case decl :: _ =>
        throw (MalformedExpression("Invalid declaration form in class") at decl)
    }

    // Fields will be unbound variables. Search for them and add look ups as tightly around them as possible.
    val fieldUnboundVars = fieldNames map UnboundVar
    val desugarMembers = new NamedASTTransform {
      override def onExpression(context: List[BoundVar], typecontext: List[BoundTypevar]) = {
        case e if unboundVarArguments(e).exists(fieldUnboundVars contains _) =>
          val toBind = unboundVarArguments(e) & fieldUnboundVars
          toBind.foldRight(e) { (unboundVar, e) =>
            val localVar = new BoundVar(unboundVar.optionalVariableName)
            Graft(localVar, FieldAccess(thisVar, Field(unboundVar.name)), e.subst(localVar, unboundVar))
          }
      }
    }

    val newbindings = convertFields(lit.decls).collect({ case (f, Some(e)) => (f, e) }).toMap.mapValues(desugarMembers.apply)
    (thisVar, superVar, Map() ++ newbindings)
  }

  /** Convert a ClassInfo to a real class.
    */
  def makeClassFromInfo(info: ClassInfo)(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo]): Class = {
    val (self, superVar, fields) = convertClassLiteral(info.literal, info.members)
    info.literal ->> Class(info.name, self, superVar, fields, info.linearization)
  }

  /** Build a New expression from the extended equivalents.
    *
    * This will generate any needed synthetic classes as a DeclareClasses node wrapped around the New.
    */
  def makeNew(newe: ext.Expression, e: ext.ClassExpression)(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo], translator: Translator): Expression = {
    val additionalClasses = mutable.Buffer[ClassInfo]()
    val info = linearize(e, additionalClasses)

    val abstractMembers = info.members -- info.concreteMembers
    if (!abstractMembers.isEmpty) {
      throw (InstantiatingAbstractClassException(info.linearization.flatMap(_.name.optionalVariableName), abstractMembers) at e)
    }

    if (additionalClasses.isEmpty)
      newe ->> New(info.linearization)
    else
      e ->> DeclareClasses(additionalClasses.map(makeClassFromInfo).toList, newe ->> New(info.linearization))
  }

  /** Build a sequence of Classes from a sequence of extended class declarations.
    *
    * This also returns a new class context to be used for subexpressions.
    */
  def makeClassGroup(clss: Seq[ext.ClassDeclaration])(implicit context: Map[String, Argument], typecontext: Map[String, Type], classcontext: Map[String, ClassInfo]): (Seq[Class], Map[String, Type], Map[String, ClassInfo]) = {
    // Check for duplicate names
    for (c :: cs <- clss.tails) {
      cs.find(_.name == c.name) match {
        case Some(c2) => throw (DuplicateClassException(c.name) at c2)
        case None => ()
      }
    }

    // Build class names and linearizations
    val additionalClasses = mutable.Buffer[ClassInfo]()
    val incrementalClassContext = mutable.Map() ++ classcontext
    for (ext.ClassDeclaration(name, superclass, body) <- clss) {
      val e = superclass match {
        case Some(s) => ext.ClassSubclassLiteral(s, body)
        case None => body
      }
      implicit val classcontext = incrementalClassContext
      val info = linearize(e, additionalClasses, Some(new BoundVar(Some(name))))
      info match {
        case i: ClassInfo => {
          incrementalClassContext += name -> i
        }
        case _ => throw new AssertionError(s"Linearize returned an anonymous class info: $info")
      }
    }

    val recursiveClassContext = incrementalClassContext.toMap
    val recursiveTypeContext = typecontext ++ additionalClasses.map(info => info.name.optionalVariableName.get -> ClassType(info.name.optionalVariableName.get))
    val newClss = for (info <- additionalClasses) yield {
      makeClassFromInfo(info)(implicitly, recursiveTypeContext, recursiveClassContext)
    }

    (newClss, recursiveTypeContext, recursiveClassContext)
  }

  private def arguments(e: Expression): Set[Argument] = e match {
    case Call(target, args, typeargs) => args.toSet + target
    case FieldAccess(o, f) => Set(o)
    case a: Argument => Set(a)
    case _ => Set()
  }
  
  private def unboundVarArguments(e: Expression): Set[UnboundVar] = arguments(e).collect {
    case v: UnboundVar => v
  }
}
