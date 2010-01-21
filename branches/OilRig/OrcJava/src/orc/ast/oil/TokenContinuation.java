//
// TokenContinuation.java -- Java interface TokenContinuation
// Project OrcJava
//
// $Id$
//
// Created by jthywiss on Jan 20, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.ast.oil;

import orc.runtime.Token;

/**
 * 
 *
 * @author jthywiss
 */
public interface TokenContinuation {
	void execute(Token t);
}
