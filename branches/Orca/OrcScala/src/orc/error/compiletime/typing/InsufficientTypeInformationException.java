//
// InsufficientTypeInformationException.java -- Java class InsufficientTypeInformationException
// Project OrcScala
//
// $Id$
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.error.compiletime.typing;

import scala.util.parsing.input.Position;

@SuppressWarnings("serial") //We don't care about serialization compatibility of Orc Exceptions
public abstract class InsufficientTypeInformationException extends TypeException {

	public InsufficientTypeInformationException(final String message) {
		super(message);
	}

	public InsufficientTypeInformationException(final String message, final Position location) {
		super(message, location);
	}
}