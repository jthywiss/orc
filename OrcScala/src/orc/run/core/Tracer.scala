//
// Tracer.scala -- Scala object Tracer
// Project OrcScala
//
// Created by jthywiss on Feb 21, 2017.
//
// Copyright (c) 2017 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.run.core

/** Tracer utility for orc.run.core package.
  *
  * @see orc.util.Tracer
  * @author jthywiss
  */
object Tracer {

  final val TokenCreation = 1L
  orc.util.Tracer.registerEventTypeId(TokenCreation, "TokCreat", _ => "", tokenStateNameMap(_))

  final val TokenStateTransition = 2L
  orc.util.Tracer.registerEventTypeId(TokenStateTransition, "TokState", tokenStateNameMap(_), tokenStateNameMap(_))

  final val TokenExecStateTransition = 3L
  orc.util.Tracer.registerEventTypeId(TokenExecStateTransition, "TokExecS", tokenExecStateNameMap(_), tokenExecStateNameMap(_))

  final val JavaCall = 5L
  orc.util.Tracer.registerEventTypeId(JavaCall, "JavaCall")

  final val JavaReturn = 6L
  orc.util.Tracer.registerEventTypeId(JavaReturn, "JavaRetn")

  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  final val traceTokenState = true

  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  final val traceTokenScheduling = false

  /* Because of aggressive inlining, changing this flag requires a clean rebuild */
  final val traceJavaCall = true

  @inline
  def traceTokenCreation(token: Token, newState: TokenState) {
    if (traceTokenState) orc.util.Tracer.trace(TokenCreation, token.debugId, 0L, tokenStateIdFor(newState))
  }

  @inline
  def traceTokenStateTransition(token: Token, oldState: TokenState, newState: TokenState) {
    if (traceTokenState) orc.util.Tracer.trace(TokenStateTransition, token.debugId, tokenStateIdFor(oldState), tokenStateIdFor(newState))
  }

  @inline
  def traceTokenExecStateTransition(token: Token, newState: TokenExecState) {
    if (traceTokenScheduling) orc.util.Tracer.trace(TokenExecStateTransition, token.debugId, 0L, tokenExecStateIdFor(newState))
  }

  @inline
  def traceJavaCall(h: orc.Handle) {
    // FIXME: Remove the need for cast so these don't crash on runtimes that don't use ExternalSiteCallHandle.
    if (traceJavaCall) orc.util.Tracer.trace(JavaCall, h.asInstanceOf[ExternalSiteCallHandle].caller.debugId, 0L, 0L)
  }

  @inline
  def traceJavaReturn(h: orc.Handle) {
    // FIXME: Remove the need for cast so these don't crash on runtimes that don't use ExternalSiteCallHandle.
    if (traceJavaCall) orc.util.Tracer.trace(JavaReturn, h.asInstanceOf[ExternalSiteCallHandle].caller.debugId, 0L, 0L)
  }

  private def tokenStateIdFor(ts: TokenState) = ts match {
    case Live => 1L
    case _: Publishing => 2L
    case _: Blocked => 3L
    case _: Suspending => 4L
    case _: Suspended => 5L
    case Halted => 6L
    case Killed => 7L
  }

  private val tokenStateNameMap = Map(
      1L -> "Live",
      2L -> "Publishing",
      3L -> "Blocked",
      4L -> "Suspending",
      5L -> "Suspended",
      6L -> "Halted",
      7L -> "Killed")

  private def tokenExecStateIdFor(ts: TokenExecState) = ts match {
    case TokenExecState.Killed => 1L
    case TokenExecState.Running => 2L
    case TokenExecState.Scheduled => 3L
    case TokenExecState.DoneRunning => 4L
  }

    private val tokenExecStateNameMap = Map(
      0L -> "",
      1L -> "Killed",
      2L -> "Running",
      3L -> "Scheduled",
      4L -> "DoneRunning")

}