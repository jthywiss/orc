//
// OrcParseController.java -- Java class OrcParseController
// Project OrcEclipse
//
// $Id: OrcParseController.java 1230 2009-08-18 14:58:16Z jthywissen $
//
// Created by jthywiss on Aug 9, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.imp.parser;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import orc.Config;
import orc.ast.extended.ASTNode;
import orc.error.compiletime.CompilationException;
import orc.error.compiletime.CompileMessageRecorder.Severity;
import orc.parser.OrcParser;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.imp.model.ISourceProject;
import org.eclipse.imp.parser.IMessageHandler;
import org.eclipse.imp.parser.ISourcePositionLocator;
import org.eclipse.imp.parser.ParseControllerBase;
import org.eclipse.imp.parser.SimpleAnnotationTypeInfo;
import org.eclipse.imp.preferences.IPreferencesService;
import org.eclipse.imp.preferences.PreferenceConstants;
import org.eclipse.imp.runtime.RuntimePlugin;
import org.eclipse.imp.services.IAnnotationTypeInfo;
import org.eclipse.imp.services.ILanguageSyntaxProperties;
import org.eclipse.jface.text.IRegion;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.ImpToOrcMessageAdapter;

/**
 * A parsing environment (file, project, etc.) that IMP constructs when it will request Orc parsing.
 *
 * @author jthywiss
 */
public class OrcParseController extends ParseControllerBase {

	/**
	 * Singleton that provides particular information about Orc's syntax to IMP.
	 *
	 * @author jthywiss
	 */
	public static class OrcSyntaxProperties implements ILanguageSyntaxProperties {

		/* (non-Javadoc)
		 * @see org.eclipse.imp.services.ILanguageSyntaxProperties#getBlockCommentContinuation()
		 */
		public String getBlockCommentContinuation() {
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.services.ILanguageSyntaxProperties#getBlockCommentEnd()
		 */
		public String getBlockCommentEnd() {
			return "-}";
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.services.ILanguageSyntaxProperties#getBlockCommentStart()
		 */
		public String getBlockCommentStart() {
			return "{-";
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.services.ILanguageSyntaxProperties#getFences()
		 */
		public String[][] getFences() {
			return orcFences;
		}

		private static String[][] orcFences = { { "(", ")" }, { "[", "]" }, { "{", "}" }, { "{-", "-}" }, };

		/**
		 * Not used. Orc doesn't have multi-component identifiers at present.
		 * @param ident ignored
		 * @return Dummy value -- an int array with one zero element 
		 */
		public int[] getIdentifierComponents(final String ident) {
			return dummyComponents;
		}

		private static int dummyComponents[] = { 0 };

		/* (non-Javadoc)
		 * @see org.eclipse.imp.services.ILanguageSyntaxProperties#getIdentifierConstituentChars()
		 */
		public String getIdentifierConstituentChars() {
			return "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_'";
		}

		/* (non-Javadoc)
		 * @see org.eclipse.imp.services.ILanguageSyntaxProperties#getSingleLineCommentPrefix()
		 */
		public String getSingleLineCommentPrefix() {
			return "--";
		}

	}

	private final static SimpleAnnotationTypeInfo annotationTypeInfo = new SimpleAnnotationTypeInfo();
	private final static OrcSyntaxProperties syntaxProperties = new OrcSyntaxProperties();
	private OrcSourcePositionLocator sourcePositionLocator;
	private OrcParser parser;
	private String currentParseString;
	private ASTNode currentAst;
	private OrcLexer lexer;

	/**
	 * Constructs an object of class OrcParseController.
	 */
	public OrcParseController() {
		super(Activator.getInstance().getLanguageID());
		// The follow might seem to be needed, but the editor handles problem markers better without it!
//		if (!annotationTypeInfo.getProblemMarkerTypes().contains(OrcBuilder.PROBLEM_MARKER_ID)) {
//			annotationTypeInfo.addProblemMarkerType(OrcBuilder.PROBLEM_MARKER_ID);
//		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.ParseControllerBase#initialize(org.eclipse.core.runtime.IPath, org.eclipse.imp.model.ISourceProject, org.eclipse.imp.parser.IMessageHandler)
	 */
	@Override
	public void initialize(final IPath filePath, final ISourceProject project, final IMessageHandler handler) {
		super.initialize(filePath, project, handler);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.imp.parser.IParseController#getSourcePositionLocator()
	 */
	public ISourcePositionLocator getSourcePositionLocator() {
		if (sourcePositionLocator == null) {
			sourcePositionLocator = new OrcSourcePositionLocator(this);
		}
		return sourcePositionLocator;
	}

	/**
	 * Returns an Iterator that iterates over the tokens contained within the given region, including any tokens that are only partially contained.
	 * <p>
	 * <code>parse</code> must be called before this method is called.
	 * 
	 * @see org.eclipse.imp.parser.IParseController#getTokenIterator(org.eclipse.jface.text.IRegion)
	 */
	public Iterator getTokenIterator(final IRegion region) {
		// Will throw a NullPointerException if called before parsing
		return lexer.iterator(region);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.imp.parser.IParseController#getAnnotationTypeInfo()
	 */
	public IAnnotationTypeInfo getAnnotationTypeInfo() {
		return annotationTypeInfo;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.imp.parser.IParseController#getSyntaxProperties()
	 */
	public ILanguageSyntaxProperties getSyntaxProperties() {
		return syntaxProperties;
	}

	/**
	 * Parse the given source and return the resulting AST.
	 * The AST should is cached, so that immediately after a
	 * successful parse, {@link #getCurrentAst()} returns the same AST as this method
	 * produced.
	 * <p>
	 * {@link #initialize(IPath, ISourceProject, IMessageHandler)} must be called before calling this method.
	 * 
	 * @return the AST, if any, resulting from the parse
	 * @param contents String containing the source text to parse
	 * @param monitor ProgressMonitor to check for cancellation
	 * @see org.eclipse.imp.parser.IParseController#parse(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Object parse(final String contents, final IProgressMonitor monitor) {

		currentParseString = contents;

		final Config config = new Config();
		// TODO: Set options per project settings

		config.setMessageRecorder(new ImpToOrcMessageAdapter(handler));
		config.getMessageRecorder().beginProcessing(fFilePath.toFile());
		if (lexer == null) {
			lexer = new OrcLexer(this);
		}
		parser = new OrcParser(config, new StringReader(contents), getPath().toOSString());

		try {
			currentAst = parser.parseProgram();
		} catch (final CompilationException e) {
			config.getMessageRecorder().recordMessage(Severity.FATAL, 0, e.getMessageOnly(), e.getSourceLocation(), null, e);
		} catch (final IOException e) {
			config.getMessageRecorder().recordMessage(Severity.FATAL, 0, e.getLocalizedMessage(), null, null, e);
		}

		// Walk AST and tie id refs to id defs
//		parser.resolve(currentAst);

		maybeDumpTokens();

		return currentAst;
	}

	/**
	 * @return the OrcParser, as used by the last invocation of {@link #parse(String, IProgressMonitor)}, or null
	 */
	public OrcParser getParser() {
		return parser;
	}

	/**
	 * @return the current parse string, as passed to the last invocation of {@link #parse(String, IProgressMonitor)}, or null
	 */
	public String getCurrentParseString() {
		return currentParseString;
	}

	/**
	 * @return the current AST, as created by the last invocation of {@link #parse(String, IProgressMonitor)}, or null
	 */
	@Override
	public ASTNode getCurrentAst() {
		return currentAst;
	}

	/**
	 * @return the {@link OrcLexer} for this ParseController, 
	 *   as created by the last invocation of {@link #parse(String, IProgressMonitor)}, 
	 *   or null
	 */
	public OrcLexer getLexer() {
		return lexer;
	}

	protected void maybeDumpTokens() {
		final IPreferencesService ourPrefSvc = Activator.getInstance().getPreferencesService();
		final IPreferencesService impPrefSvc = RuntimePlugin.getInstance().getPreferencesService();

		final boolean dump = ourPrefSvc.isDefined(PreferenceConstants.P_DUMP_TOKENS) ? ourPrefSvc.getBooleanPreference(PreferenceConstants.P_DUMP_TOKENS) : impPrefSvc.getBooleanPreference(PreferenceConstants.P_DUMP_TOKENS);

		if (dump) {
			for (final OrcLexer.OrcToken currToken : lexer) {
				System.out.println(currToken);
			}
			System.out.println("END-OF-FILE");
		}
	}
}
