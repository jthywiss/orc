//
// OrcObject.scala -- Scala class OrcObject
// Project OrcScala
//
// $Id: OrcRecord.scala 2933 2011-12-15 16:26:02Z jthywissen $
//
// Created by dkitchin on Jul 10, 2010. Updated to object from record by amp in Dec 2014.
//
// Copyright (c) 2015 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.values

import orc.error.runtime.ArgumentTypeMismatchException
import orc.error.runtime.ArityMismatchException
import orc.error.runtime.NoSuchMemberException
import scala.collection.immutable.Map
import orc.run.core.Future

/** The runtime object representing Orc objects.
  *
  * Since they are recursive the entries need to be set after the object exists.
  * This is done by initializing entries to null and then having assert checks.
  *
  * @author amp
  */
case class OrcObject(var entries: Map[Field, Future] = null) extends OrcValue {
  def setFields(_entries: Map[Field, Future]) = {
    assert(entries eq null)
    entries = _entries
  }

  def apply(f: Field) = {
    assert(entries ne null)
    entries(f)
  }

  override def toOrcSyntax() = {
    assert(entries ne null)
    val formattedEntries =
      for ((s, v) <- entries) yield {
        s + " = " + Format.formatValue(v)
      }
    val contents =
      formattedEntries.toList match {
        case Nil => ""
        case l => l reduceRight { _ + " # " + _ }
      }
    "{ " + contents + " }"
  }
}
