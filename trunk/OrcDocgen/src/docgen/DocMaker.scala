//
// DocMaker.scala -- Scala class/trait/object DocMaker
// Project OrcDocgen
//
// $Id$
//
// Created by dkitchin on Dec 16, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package docgen

import java.io.File
import scala.xml._

/**
 * 
 *
 * @author dkitchin
 */
class DocMaker(toplevelName: String) {
  
	val claimedIdentifiers = new scala.collection.mutable.HashSet[String]()
	
  def nonblank(line: String): Boolean = !line.trim().isEmpty
  
  /* Split a chunk of text into paragraphs */
  def paragraphs(s: String): List[String] = {
    def groupLines(lines: List[String]): List[String] = {
      lines match {
        case first :: _ if nonblank(first) => {
          val (nonblankLines, rest) = lines span nonblank
          val para = (nonblankLines foldLeft "") { (s: String, t: String) => s + t }
          para :: groupLines(rest)
        }
        case _ :: rest => groupLines(rest)
        case _ => Nil
      }
    }
    groupLines(s.linesWithSeparators.toList)
  }
  
	/* Optionally add an xml:id tag to an xml element */
	def addId(xml: Elem, optionalID: Option[String]): Elem = {
		optionalID match {
			case None => xml
			case Some(id) => {
			  val idAttribute = new PrefixedAttribute("xml", "id", id, scala.xml.Null)
        xml.copy(attributes = xml.attributes.append(idAttribute))
			}
		}
	}
	
	
	/* Generate an XML header comment based on an optional filename */
	def generateHeaderComment(optionalFilename: Option[String]) = {
    val now = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date())
    val attribution = optionalFilename map { "from " + _ + " "} getOrElse ""
    Comment("Generated by OrcDocgen " + attribution + "on " + now)
	}
	
  
  def renderChapter(targets: List[File]): Node = {
    <chapter xml:id={toplevelName} xmlns="http://docbook.org/ns/docbook" xmlns:od="http://orc.csres.utexas.edu/OrcDocBook.xsd" xmlns:xi="http://www.w3.org/2001/XInclude">
      { generateHeaderComment(None) }
      <title>Standard Library</title>
        <xi:include href={toplevelName + ".intro" + ".xml"}/>
        { targets map { t => <xi:include href={t.getName()}/> } }
    </chapter>
  } 
    
  
  def renderSection(source: File)(implicit sectionName: String): Elem = {
    val docItemList = DocParsers.parseFile(source)
    val optionalDescription = {
      docItemList find { 
      	case DocText(s) if nonblank(s) => true
      	case _ => false 
      } match {
        case Some(DocText(s)) => ": " + s.lines.next()
        case _ => "" 
      }
    }
    val content = {
    	<section xmlns="http://docbook.org/ns/docbook" xmlns:od="http://orc.csres.utexas.edu/OrcDocBook.xsd"> 
        { generateHeaderComment(Some(source.getName())) }
        <title>{ sectionName + optionalDescription }</title>
        { renderItems(docItemList, "") }
      </section>
    }
    addId(content, Some(toplevelName + "." + sectionName))
  }
  
  def renderItems(items: List[DocItem], nextCode: String)(implicit sectionName: String): List[Node] = {
    items match {
      case DocText(s) :: rest => {
        val paraNodes = paragraphs(s) map { p: String => <para>{ Unparsed(p) }</para> }
        paraNodes ::: renderItems(rest, nextCode)
      }
      case (_ : DocDecl) :: _ => {
        val (decls, rest) = items span { _.isInstanceOf[DocDecl] }
        val newNextCode = {
          items find { _.isInstanceOf[DocOutside] } match {
            case Some(DocOutside(body)) => body
            case None => ""
          }
        }
        val declChunk = {
          <variablelist>
            <?dbfo list-presentation="list"?>
            { decls map { d: DocItem => renderDecl(d.asInstanceOf[DocDecl], newNextCode) } }
          </variablelist>  
        }
        declChunk :: renderItems(rest, nextCode)
      }
      case DocImpl :: rest => {
        renderImplementation(nextCode) :: renderItems(rest, nextCode)
      }
      // If we encounter uncommented content, ignore it.
      case (_ : DocOutside) :: rest => renderItems(rest, nextCode)
      
      // No more input
      case Nil => Nil
    }
    
  }
  
  
  def processDeclName(name: String): Option[String] = {
  	"""^\w+$""".r.findPrefixOf(name) match {
  	  case Some(ident) => Some(ident)
  	  case None => {
				"""^\([^\)]+\)$""".r.findPrefixOf(name) match {
					case Some(s) => s match {
						case "(+)" => Some("Add")
						case "(-)" => Some("Sub")
						case "(0-)" => Some("UMinus")
						case "(*)" => Some("Mult")
						case "(**)" => Some("Exponent")
						case "(/)" => Some("Div")
						case "(%)" => Some("Mod")
						case "(<:)" => Some("Less")
					  case "(<=)" => Some("Leq")
						case "(:>)" => Some("Greater")
						case "(>=)" => Some("Greq")
						case "(=)" => Some("Eq")
						case "(/=)" => Some("Inequal")
						case "(~)" => Some("Not")
						case "(&&)" => Some("And")
						case "(||)" => Some("Or")
						case "(:)" => Some("Cons")
						case _ => None
					}
					case None => None
				}
			}
		}
  }
    
  def renderDecl(decl: DocDecl, nextCode: String)(implicit sectionName: String) = {
  	
  	val optionalId = {
  		processDeclName(decl.name) match {
  			case Some(ident) => {
  			  val longId = toplevelName + "." + sectionName + "." + ident
  			  if (claimedIdentifiers contains longId) {
  			  	None
  			  }
  			  else {
  			  	claimedIdentifiers += longId
  			  	Some(longId)
  			  }
  			}
  			case None => None
  		}
  	}
  	
  	val content = {
  		<varlistentry>
  		  <term><code>{ decl.name }</code></term>
        <listitem>
  		 	  <para><code>{ decl.keyword + decl.typeDeclaration }</code></para>
  			  { renderItems(decl.body, nextCode) }
  			</listitem>
  		</varlistentry>
  	}
  	
  	addId(content, optionalId)
  }  
  
  
  def renderImplementation(code: String) = {
    <formalpara>
      <title>Implementation</title>
      <para><programlisting language="orc-demo">
        { PCData(code) }
      </programlisting></para>
    </formalpara>
  }
   
}