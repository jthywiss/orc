
//
// Sites2Util.scala -- Scala traits and classes for 2-ary sites
// Project OrcScala
//
// AUTOGENERATED. Do not edit.
//
// Copyright (c) 2018 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.values.sites

import scala.reflect.ClassTag

import orc.VirtualCallContext
import orc.SiteResponseSet
import orc.Invoker
import orc.OrcRuntime
import orc.DirectInvoker
import orc.error.runtime.HaltException
import orc.error.runtime.ArityMismatchException
import orc.error.runtime.ArgumentTypeMismatchException

import InvocationBehaviorUtilities._

trait Site2 extends Site with SpecificArity {
  val arity = 2
}

abstract class Site2Base[A0 : ClassTag, A1 : ClassTag] extends HasGetInvoker2[A0, A1] with Site2 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: Site2Base[A0, A1], AA0 <: A0, AA1 <: A1]
        (exampleTarget: T, example0: AA0, example1: AA1)
        (_impl: (VirtualCallContext, T, AA0, AA1) => SiteResponseSet): Invoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, Array[Any](example0, example1).asInstanceOf[Array[AnyRef]])
          with Site2Base.ImplInvoker[T, AA0, AA1] {
      val impl = _impl
    }
  }
}

object Site2Base {
  trait ImplInvoker[T, AA0, AA1] extends Invoker {
    val impl: (VirtualCallContext, T, AA0, AA1) => SiteResponseSet

    def invoke(ctx: VirtualCallContext, target: AnyRef, arguments: Array[AnyRef]): SiteResponseSet = {
      orc.run.StopWatches.implementation {
        impl(ctx, target.asInstanceOf[T], arguments(0).asInstanceOf[AA0], arguments(1).asInstanceOf[AA1])
      }
    }
  }
}

abstract class Site2Simple[A0 : ClassTag, A1 : ClassTag] extends Site2Base[A0, A1] {
  def eval(ctx: VirtualCallContext, arg0: A0, arg1: A1): SiteResponseSet

  final def getInvoker(runtime: OrcRuntime, arg0: A0, arg1: A1) =
    invoker(this, arg0, arg1) { (ctx, self, arg0, arg1) =>
      self.eval(ctx, arg0, arg1)
    }
}

trait PartialSite2 extends PartialSite with SpecificArity {
  val arity = 2
}

abstract class PartialSite2Base[A0 : ClassTag, A1 : ClassTag] extends HasGetDirectInvoker2[A0, A1] with PartialSite2 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: PartialSite2Base[A0, A1], AA0 <: A0, AA1 <: A1]
        (exampleTarget: T, example0: AA0, example1: AA1)
        (_impl: (T, AA0, AA1) => Option[Any]): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, Array[Any](example0, example1).asInstanceOf[Array[AnyRef]])
          with PartialSite2Base.ImplInvoker[T, AA0, AA1] {
      val impl = _impl
    }
  }
}

object PartialSite2Base {
  trait ImplInvoker[T, AA0, AA1] extends DirectInvoker {
    val impl: (T, AA0, AA1) => Option[Any]

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      (try {
        orc.run.StopWatches.implementation {
          impl(target.asInstanceOf[T], arguments(0).asInstanceOf[AA0], arguments(1).asInstanceOf[AA1])
        }
      } catch {
        case e: Exception =>
          throw new HaltException(e)
      }) match {
        case Some(v) => v.asInstanceOf[AnyRef]
        case None => throw new HaltException()
      }
    }
  }
}

abstract class PartialSite2Simple[A0 : ClassTag, A1 : ClassTag] extends PartialSite2Base[A0, A1] {
  def eval(arg0: A0, arg1: A1): Option[Any]

  final def getInvoker(runtime: OrcRuntime, arg0: A0, arg1: A1) =
    invoker(this, arg0, arg1) { (self, arg0, arg1) =>
      self.eval(arg0, arg1)
    }
}

trait TotalSite2 extends TotalSite with SpecificArity {
  val arity = 2
}

abstract class TotalSite2Base[A0 : ClassTag, A1 : ClassTag] extends HasGetDirectInvoker2[A0, A1] with TotalSite2 {
  /** Create an invoker which works for any instance of this Site class.
    *
    * exampleTarget should be this, examplesArguments should be the arguments
    * with the correct types for this invoker.
    */
  protected def invoker[T <: TotalSite2Base[A0, A1], AA0 <: A0, AA1 <: A1]
        (exampleTarget: T, example0: AA0, example1: AA1)
        (_impl: (T, AA0, AA1) => Any): DirectInvoker = {
    new TargetClassAndArgumentClassSpecializedInvoker(exampleTarget, Array[Any](example0, example1).asInstanceOf[Array[AnyRef]])
          with TotalSite2Base.ImplInvoker[T, AA0, AA1] {
      val impl = _impl
    }
  }
}

object TotalSite2Base {
  trait ImplInvoker[T, AA0, AA1] extends DirectInvoker {
    val impl: (T, AA0, AA1) => Any

    def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
      try {
        orc.run.StopWatches.implementation {
          impl(target.asInstanceOf[T], arguments(0).asInstanceOf[AA0], arguments(1).asInstanceOf[AA1]).asInstanceOf[AnyRef]
        }
      } catch {
        case e: Exception =>
          throw new HaltException(e)
      }
    }
  }
}

abstract class TotalSite2Simple[A0 : ClassTag, A1 : ClassTag] extends TotalSite2Base[A0, A1] {
  def eval(arg0: A0, arg1: A1): Any

  final def getInvoker(runtime: OrcRuntime, arg0: A0, arg1: A1) =
    invoker(this, arg0, arg1) { (self, arg0, arg1) =>
      self.eval(arg0, arg1)
    }
}

abstract class HasGetInvoker2[A0 : ClassTag, A1 : ClassTag] {
  val argumentTypeStrings = Array(implicitly[ClassTag[A0]].runtimeClass.getSimpleName, implicitly[ClassTag[A1]].runtimeClass.getSimpleName)
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): Invoker = {
    if (args.length != 2) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArityMismatchException(2, args.size)
        }
      }
    } else if (!valueHasTypeByTag[A0](args(0))) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArgumentTypeMismatchException(0, argumentTypeStrings(0), if (args(0) != null) args(0).getClass().toString() else "null")
        }
      }
    } else if (!valueHasTypeByTag[A1](args(1))) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArgumentTypeMismatchException(1, argumentTypeStrings(1), if (args(1) != null) args(1).getClass().toString() else "null")
        }
      }
    } else {
      getInvoker(runtime, args(0).asInstanceOf[A0], args(1).asInstanceOf[A1])
    }
  }

  def getInvoker(runtime: OrcRuntime, arg0: A0, arg1: A1): Invoker
}

abstract class HasGetDirectInvoker2[A0 : ClassTag, A1 : ClassTag] {
  val argumentTypeStrings = Array(implicitly[ClassTag[A0]].runtimeClass.getSimpleName, implicitly[ClassTag[A1]].runtimeClass.getSimpleName)
  final def getInvoker(runtime: OrcRuntime, args: Array[AnyRef]): DirectInvoker = {
    if (args.length != 2) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArityMismatchException(2, args.size)
        }
      }
    } else if (!valueHasTypeByTag[A0](args(0))) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArgumentTypeMismatchException(0, argumentTypeStrings(0), if (args(0) != null) args(0).getClass().toString() else "null")
        }
      }
    } else if (!valueHasTypeByTag[A1](args(1))) {
      new TargetClassAndArgumentClassSpecializedInvoker(this, args) with DirectInvoker {
        @throws[Throwable]
        def invokeDirect(target: AnyRef, arguments: Array[AnyRef]): AnyRef = {
          throw new ArgumentTypeMismatchException(1, argumentTypeStrings(1), if (args(1) != null) args(1).getClass().toString() else "null")
        }
      }
    } else {
      getInvoker(runtime, args(0).asInstanceOf[A0], args(1).asInstanceOf[A1])
    }
  }

  def getInvoker(runtime: OrcRuntime, arg0: A0, arg1: A1): DirectInvoker
}

