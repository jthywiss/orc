//
// AstEditOperation.scala -- Scala trait AstEditOperation
// Project OrcScala
//
// $Id: AstEditOperation.scala 2773 2011-04-20 01:12:36Z jthywissen $
//
// Created by jthywiss on Sep 30, 2010.
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.lib.progswap

import orc.run.Orc

/**
 * An instance of AstEditOperation represents one tree-edit operation in a tree-edit script. A script is simply
 * a list of operations, which modify an OIL AST into a new OIL AST.
 *
 * @see AstEditScript
 * @author jthywiss
 */
trait AstEditOperation {
  /**
   * Determine whether a token would be in the scope of this <code>AstEditOperation</code>.
   * 
   * @param token Token to check
   * @return true if affected
   */
  def isTokenAffected(token: Orc#Token): Boolean

  /**
   * Determine whether a token is "safe" to migrate under this <code>AstEditOperation</code>.
   * Will return true for tokens that are not affected by this operation, which may be
   * unsafe to move under some other operation in the edit script.
   *
   * @param token Token to check
   * @return true if safe to migrate
   */
  def isTokenSafe(token: Orc#Token): Boolean

  /**
   * Attempt to move a token as specified by this <code>AstEditOperation</code>.
   * Tokens not affected by (in the scope of) this operation will be ignored.
   *
   * @param token Token to move
   * @return true if successfully moved
   */
  def migrateToken(token: Orc#Token): Boolean

  /**
   * Update the closures in this token's environment to reflect the changes specified
   * by the given <code>AstEditScript</code>. 
   *
   * @param token Token containing environments to update
   * @param editList Edit script to apply to token's environments
   * @see orc.env.Env
   */
  def migrateClosures(token: Orc#Token, editList: AstEditScript) {
    editList.map(_.migrateClosures(token))
  }

  /**
   * Update the closures in this token's environment to reflect the changes specified
   * by this <code>AstEditOperation</code>. 
   *
   * @param token Token containing environments to update
   * @see orc.env.Env
   */
  def migrateClosures(token: Orc#Token): Unit

  /**
   * Update the frame stack (continuations) in this token to reflect the changes specified
   * by the given <code>AstEditScript</code>. 
   *
   * @param token Token containing frame stack to update
   * @param editList Edit script to apply to token's environments
   * @see orc.runtime.Token.FrameContinuation
   */
  def migrateFrameStack(token: Orc#Token, editList: AstEditScript) {
    editList.map(_.migrateFrameStack(token))
  }

  /**
   * Update the frame stack (continuations) in this token to reflect the changes specified
   * by this <code>AstEditOperation</code>. 
   *
   * @param token Token containing frame stack to update
   * @see orc.runtime.Token.FrameContinuation
   */
  def migrateFrameStack(token: Orc#Token): Unit
}