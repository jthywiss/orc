//
// XML.scala -- Scala class/trait/object XML
// Project OrcScala
//
// $Id$
//
// Created by amshali on Jul 12, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//
package orc.oil.nameless


import scala.xml._
import orc.oil.nameless._


object OrcXML {
  
  def writeXML(e : NamelessAST) {
    val xmlout = <orc>
    {toXML(e)}
    </orc>    
    val pp = new PrettyPrinter(0, 0)
    println(pp.format(xmlout))
  }
  
  def toXML(e : NamelessAST) : Elem = {
    e match {
      case Stop() => <stop/>
      case Call(target, args, typeArgs) =>
        <call>
        <target>{toXML(target)}</target>
        <args>
            {for (a <- args) yield
              <arg>
                {toXML(a)}
              </arg>
            }
        </args>
        {
          typeArgs match {
            case Some(l) => 
              <typeargs>
                {for (a <- l) yield
                  <arg>
                    {toXML(a)}
                  </arg>
                }
              </typeargs>
            case None => <typeargs/>
          }
        }
        </call>
      case Parallel(left, right) => 
        <parallel>
        <left>{toXML(left)}</left>
        <right>{toXML(right)}</right>
        </parallel>
      case Sequence(left, right) => 
        <sequence>
        <left>{toXML(left)}</left>
        <right>{toXML(right)}</right>
        </sequence>
      case Prune(left, right) => 
        <prune>
        <left>{toXML(left)}</left>
        <right>{toXML(right)}</right>
        </prune>
      case Otherwise(left, right) => 
        <otherwise>
        <left>{toXML(left)}</left>
        <right>{toXML(right)}</right>
        </otherwise> 
      case DeclareDefs(unclosedVars, defs, body: Expression) =>
        <declaredefs>
          <unclosedvars>
          {for (a <- unclosedVars) yield
            <uv>{a}</uv>
          }
          </unclosedvars>
          <defs>
          {for (a <- defs) yield
            <adef>
              {toXML(a)}
            </adef>
          }
          </defs>
          <body>
            {toXML(body)}
          </body>
        </declaredefs>
      case DeclareType(t: Type, body: Expression) =>
        <declaretype>
        <atype>{toXML(t)}</atype>
        <body>{toXML(body)}</body>
        </declaretype> 
      case HasType(body: Expression, expectedType: Type) => 
        <hastype>
          <body>{toXML(body)}</body>
          <expectedtype>{toXML(expectedType)}</expectedtype>
        </hastype>
      case Constant(v: Any) => <constant>{anyToXML(v)}</constant>
      case Constant(null) => <constant><nil/></constant>
      case Variable(i: Int) => <variable>{i}</variable>
      case Top() => <top/>
      case Bot() => <bot/>
      case TypeVar(i) => <typevar>{i}</typevar>
      case TupleType(elements) =>
        <tupletype>
        {for (a <- elements) yield
          <element>{toXML(a)}</element>
        }
        </tupletype>
      case RecordType(entries) =>
        <recordtype>
        {for ((n, t) <- entries) yield
          <entry>
          <name>{n}</name>
          <rtype>{toXML(t)}</rtype>
          </entry>
        }        
        </recordtype>
      case TypeApplication(tycon: Int, typeactuals) =>
        <typeapplication>
          <typeconst>{tycon}</typeconst>
          <typeactuals>
            {for (t <- typeactuals) yield
              <typeactual>{toXML(t)}</typeactual>
            }
          </typeactuals>
        </typeapplication>
      case AssertedType(assertedType: Type) =>
        <assertedtype>{toXML(assertedType)}</assertedtype>
      case FunctionType(typeFormalArity: Int, argTypes, returnType: Type) =>
        <functiontype>
          <typearity>{typeFormalArity}</typearity>
          <argtypes>
            {for (t <- argTypes) yield
              <arg>{toXML(t)}</arg>
            }
          </argtypes>
          <returntype>{toXML(returnType)}</returntype>
        </functiontype>
      case TypeAbstraction(typeFormalArity: Int, t: Type) =>
        <typeabstraction>
          <typearity>{typeFormalArity}</typearity>
          <atype>{toXML(t)}</atype>
        </typeabstraction>
      case ImportedType(classname: String) =>
        <importedtype>{classname}</importedtype>
      case ClassType(classname: String) =>
        <classtype>{classname}</classtype>
      case VariantType(variants) =>
        <varianttype>
          {for ((n, l) <- variants) yield
            <variant>
              <name>n</name>
              <params>
                {l map {
                  case Some(t) => <param>{toXML(t)}</param>
                  case None => <param/>
                }}
              </params>
            </variant>
          }
        </varianttype> 
      case Def(typeFormalArity: Int, arity: Int, body: Expression, argTypes: Option[List[Type]], returnType: Option[Type]) =>
        <definition>
          <typearity>{typeFormalArity}</typearity>
          <arity>{arity}</arity>
          <body>{toXML(body)}</body>
          {argTypes match {
            case Some(l) => <argtypes>{ l map { x => <arg>{toXML(x)}</arg>}}</argtypes>
            case None => <argtypes/>
          }}
          {returnType match {
            case Some(t) => <returntype>{toXML(t)}</returntype>
            case None => <returntype/>
          }}
        </definition>
      case _ => throw new Error("Invalid Node for XML conversion!")
    }
  }
  
  def anyToXML(a : Any) : Elem = {
    a match { 
      case i:Int => <int>{a}</int> 
      case f:Float => <float>{a}</float> 
      case d:Double => <double>{a}</double> 
      case l:Long => <long>{a}</long> 
      case c:Char => <char>{a}</char> 
      case b:Boolean => <boolean>{b}</boolean> 
      case b:Byte => <byte>{b}</byte> 
      case b:Short => <short>{b}</short> 
      case s:String => <string>{a}</string>
      case i:scala.math.BigInt => <bigint>{a}</bigint>
      case x:orc.values.sites.JavaClassProxy => <jclassproxy>{x.name}</jclassproxy>
      case x:orc.values.sites.Site => 
        <site>{a.asInstanceOf[AnyRef].getClass().getName}</site>
      case orc.values.Signal => <signal/>
      case orc.values.Field(s) => <field>{s}</field>
      case x:AnyRef => println(">>>> "+x.getClass); <any>{a}</any>
    }
  }

  def fromXML(inxml: Seq[scala.xml.Node]) : Expression = {
    inxml match {
      case <orc>{prog@ _*}</orc> => fromXML(prog)
      case <stop/> => Stop()
      case <parallel><left>{left@ _*}</left>
        <right>{right@ _*}</right></parallel> => Parallel(fromXML(left), fromXML(right))
      case <sequence><left>{left@ _*}</left>
        <right>{right@ _*}</right></sequence> => Sequence(fromXML(left), fromXML(right))
      case <prune><left>{left@ _*}</left>
        <right>{right@ _*}</right></prune> => Prune(fromXML(left), fromXML(right))
      case <otherwise><left>{left@ _*}</left>
        <right>{right@ _*}</right></otherwise> => Otherwise(fromXML(left), fromXML(right))
      case <declaredefs>
          <unclosedvars>{uvars@ _*}</unclosedvars>
          <defs>{defs@ _*}</defs>
          <body>{body@ _*}</body>
        </declaredefs> => {
          val t1 = for (<uv>{i @ _*}</uv> <- uvars) yield i.text.toInt
          val t2 = for (<adef>{d @ _*}</adef> <- defs) yield defFromXML(d)
          val t3 = fromXML(body)
          DeclareDefs(t1.toList, t2.toList, t3)
        }
      case <call>
      <target>{target@ _*}</target>
      <args>{args@ _*}</args>
      <typeargs>{typeargs@ _*}</typeargs>
      </call> => {
        val t1 = argumentFromXML(target)
        val t2 = for (<arg>{a @ _*}</arg> <- args) yield argumentFromXML(a)
        val t3 = for (<arg>{a @ _*}</arg> <- args) yield typeFromXML(a)
        Call(t1, t2.toList, if (t3.size==0) None else Some(t3.toList))
      }
    }    
  }
  
  def argumentFromXML(inxml: Seq[scala.xml.Node]) : Argument = {
    inxml match {
      case <constant>{c@ _*}</constant> => Constant(c.text)
      case <variable>{v@ _*}</variable> => Variable(v.text.toInt)
    }
  }
  
  def defFromXML(inxml: Seq[scala.xml.Node]) : Def = {
    inxml match {
      case <definition>
      <typearity>{typeFormalArity@ _*}</typearity>
      <arity>{arity@ _*}</arity>
      <body>{body@ _*}</body>
      <argtypes>{argTypes@ _*}</argtypes>
      <returntype>{returnType@ _*}</returntype>
      </definition> => {
        val t1 = if (argTypes.text.trim == "") None 
            else Some((for (<arg>{a @ _*}</arg> <- argTypes) yield typeFromXML(a)).toList)
        
        val t2 = if (returnType.text.trim == "") None
          else Some(typeFromXML(returnType))
        Def(typeFormalArity.text.toInt, arity.text.toInt, fromXML(body), t1, t2)
      }   
    }
  }
  
  def typeFromXML(inxml: Seq[scala.xml.Node]) : Type = {
    Top()
  }
  
  import orc.compile.StandardOrcCompiler
  import orc.run.StandardOrcRuntime
  import orc.compile.parse.OrcReader
  import orc.run._
  import orc.values.OrcValue
  import orc.values.sites.Site
  import orc.values.Format
  import scala.concurrent.SyncVar
  import orc.OrcOptions
  import orc.ExperimentOptions

  def main(args: Array[String]) {
    if (args.length < 1) {
      throw new Exception("Please supply a source file name as the first argument.\n" +
                          "Within Eclipse, use ${resource_loc}")
    }
    ExperimentOptions.filename = args(0)
    val compiler = new StandardOrcCompiler()
    val reader = OrcReader(new java.io.FileReader(ExperimentOptions.filename), ExperimentOptions.filename, compiler.openInclude(_, _, ExperimentOptions))
    val compiledOil = compiler(reader, ExperimentOptions)
    if (compiledOil != null) {
      OrcXML.writeXML(compiledOil)
    }
    else {
      Console.err.println("Compilation failed.")
    }
  }

}
