//
// XMLExamplesTest.java -- Java class XMLExamplesTest
// Project OrcTests
//
// $Id$
//
// Copyright (c) 2013 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.test;

import java.io.File;
import java.lang.reflect.Field;

import junit.framework.Test;
import orc.ast.oil.nameless.Expression;
import orc.ast.oil.xml.OrcXML;
import orc.script.OrcScriptEngine;
import orc.script.OrcBindings;
import orc.test.TestUtils.OrcTestCase;

import javax.xml.validation.*;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.SAXException;

/**
 * Test Orc by running annotated sample programs from the "../OrcExamples" directory.
 * Each program is compiled, written to XML, subjected to validation against
 * an XML schema, and then read back as an AST. This second AST is run. 
 * Each program is given at most 10 seconds to complete.
 * <p>
 * We look for one or more comment blocks formatted per
 * <code>ExampleOutput</code>'s specs.
 * 
 * @see ExpectedOutput
 * @author quark, srosario, amshali, dkitchin, jthywiss
 */
public class XMLExamplesTest {
	public static Test suite() {
        return TestUtils.buildSuite(XMLExamplesTest.class.getSimpleName(), XMLExamplesTestCase.class, new OrcBindings(), new File("test_data"), new File("../OrcExamples"));
	}

    public static class XMLExamplesTestCase extends OrcTestCase {
        @Override
        public void runTest() throws Throwable {
            System.out.println("\n==== Starting " + orcFile + " ====");
            final OrcScriptEngine.OrcCompiledScript compiledScript = OrcForTesting.compile(orcFile.getPath(), bindings);
            final Expression expr = getAstRoot(compiledScript);

            // AST -> XML
            final scala.xml.Elem xmlFromExpr = OrcXML.astToXml(expr);

            // Locate .xsd file resource
            final ClassLoader clX = Thread.currentThread().getContextClassLoader();
            final ClassLoader clY = getClass().getClassLoader();
            final ClassLoader clZ = ClassLoader.getSystemClassLoader();
            final ClassLoader classLoader = clX != null ? clX : (clY != null ? clY : clZ);
            final java.io.InputStream xsdStream = classLoader.getResource("orc/ast/oil/xml/oil.xsd").openStream();

            // Schema validation
            try {
              final SchemaFactory schemaFactory = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
              final Schema schema = schemaFactory.newSchema(new StreamSource(xsdStream));
              final Validator validator = schema.newValidator();
              final StreamSource xmlsource = new StreamSource(new java.io.StringReader(xmlFromExpr.toString()));
              validator.validate(xmlsource);
            } 
            catch (SAXException e) {
              throw new AssertionError("XML validation failed: " + e.getMessage());
            }

            // XML -> AST
            final Expression exprFromXml = OrcXML.xmlToAst(xmlFromExpr);

            // Execution
            setAstRoot(compiledScript, exprFromXml);
            final String actual = OrcForTesting.run(compiledScript, 10L);
            if (!expecteds.contains(actual)) {
                throw new AssertionError("Unexpected output:\n" + actual);
            }
        }
    }

	static Expression getAstRoot(final OrcScriptEngine.OrcCompiledScript compiledScript) throws SecurityException, NoSuchFieldException, IllegalAccessException {
		// Violate access controls of OrcCompiledScript.astRoot field
		final Field astRootField = compiledScript.getClass().getDeclaredField("astRoot");
		astRootField.setAccessible(true);
		return (Expression) astRootField.get(compiledScript);
	}

	static void setAstRoot(final OrcScriptEngine.OrcCompiledScript compiledScript, final Expression astRoot) throws SecurityException, NoSuchFieldException, IllegalAccessException {
		// Violate access controls of OrcCompiledScript.astRoot field
		final Field astRootField = compiledScript.getClass().getDeclaredField("astRoot");
		astRootField.setAccessible(true);
		astRootField.set(compiledScript, astRoot);
	}

}
