//
// OrcConfigSettings.java -- Java class OrcConfigSettings
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Sep 3, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse;

import java.io.File;
import java.io.IOException;

import orc.Config;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.imp.preferences.PreferencesService;
import org.kohsuke.args4j.CmdLineException;

/**
 * Orc configuration ({@link orc.Config}) extended to read its state from
 * Eclipse preferences and run configurations.
 * <p>
 * Note that OrcConfigSettings uses Eclipse's default preferences hierarchy,
 * as wrapped by IMP's {@link org.eclipse.imp.preferences.PreferencesService},
 * AND extends that by treating the launch configuration (if supplied) as a new
 * lowest level preference.  So, the resulting hierarchy is:
 * LAUNCH_CONFIGURATION -> PROJECT -> INSTANCE -> CONFIGURATION -> DEFAULT
 *
 * @see org.eclipse.imp.preferences.PreferencesService
 * @see org.eclipse.debug.core.ILaunchConfiguration
 * @author jthywiss
 */
public class OrcConfigSettings extends Config {
	public static final String TYPE_CHECK_ATTR_NAME = Activator.getInstance().getID() + ".TYPE_CHECK";
	public static final String NO_PRELUDE_ATTR_NAME = Activator.getInstance().getID() + ".NO_PRELUDE";
	public static final String EXCEPTIONS_ON_ATTR_NAME = Activator.getInstance().getID() + ".EXCEPTIONS_ON";
	public static final String INCLUDE_PATH_ATTR_NAME = Activator.getInstance().getID() + ".INCLUDE_PATH";
	public static final String SITE_CLASSPATH_ATTR_NAME = Activator.getInstance().getID() + ".SITE_CLASSPATH";
	public static final String OIL_OUT_ATTR_NAME = Activator.getInstance().getID() + ".OIL_OUT";
	public static final String MAX_PUBS_ATTR_NAME = Activator.getInstance().getID() + ".MAX_PUBS";
	public static final String NUM_SITE_THREADS_ATTR_NAME = Activator.getInstance().getID() + ".NUM_SITE_THREADS";
	public static final String TRACE_OUT_ATTR_NAME = Activator.getInstance().getID() + ".TRACE_OUT";
	public static final String DEBUG_LEVEL_ATTR_NAME = Activator.getInstance().getID() + ".DEBUG_LEVEL";

	private static final Config defaultConfig = new Config();

	public static final boolean TYPE_CHECK_DEFAULT = defaultConfig.getTypeChecking();
	public static final boolean NO_PRELUDE_DEFAULT = defaultConfig.getNoPrelude();
	public static final boolean EXCEPTIONS_ON_DEFAULT = defaultConfig.getExceptionsOn();
	public static final String INCLUDE_PATH_DEFAULT = defaultConfig.getIncludePath().length() == 0 ? defaultConfig.getIncludePath() : defaultConfig.getIncludePath().concat(":"); //Eclipse path pref entries always have a trailing :
	public static final String SITE_CLASSPATH_DEFAULT = defaultConfig.getClassPath().length() == 0 ? defaultConfig.getClassPath() : defaultConfig.getClassPath().concat(":"); //Eclipse path pref entries always have a trailing :
	public static final String OIL_OUT_DEFAULT = defaultConfig.getOilOutputFile().getPath();
	public static final int MAX_PUBS_DEFAULT = defaultConfig.getMaxPubs();
	public static final int NUM_SITE_THREADS_DEFAULT = defaultConfig.getNumSiteThreads();
	public static final String TRACE_OUT_DEFAULT = defaultConfig.getTraceOutputFile().getPath();
	public static final int DEBUG_LEVEL_DEFAULT = defaultConfig.getDebugLevel();

	/**
	 * Constructs an object of class OrcConfigSettings.
	 *
	 * @param project
	 * @param launchConfig
	 * @throws IOException 
	 * @throws CoreException 
	 */
	public OrcConfigSettings(final IProject project, final ILaunchConfiguration launchConfig) throws IOException, CoreException {
		super();
		if (project != null) {
			fillFromProject(project);
		}
		if (launchConfig != null) {
			fillFromLaunchConfig(launchConfig);
		}
	}

	/**
	 * @param project
	 * @throws IOException 
	 */
	private void fillFromProject(final IProject project) throws IOException {
		final PreferencesService prefSvc = Activator.getInstance().getPreferencesService();
		prefSvc.setProject(project);

		// Will also look upwards in prefs levels if not found in project.

		if (prefSvc.isDefined(TYPE_CHECK_ATTR_NAME)) {
			setTypeChecking(prefSvc.getBooleanPreference(TYPE_CHECK_ATTR_NAME));
		}
		if (prefSvc.isDefined(NO_PRELUDE_ATTR_NAME)) {
			setNoPrelude(prefSvc.getBooleanPreference(NO_PRELUDE_ATTR_NAME));
		}
		if (prefSvc.isDefined(EXCEPTIONS_ON_ATTR_NAME)) {
			setNoPrelude(prefSvc.getBooleanPreference(EXCEPTIONS_ON_ATTR_NAME));
		}
		if (prefSvc.isDefined(INCLUDE_PATH_ATTR_NAME)) {
			setIncludePath(prefSvc.getStringPreference(INCLUDE_PATH_ATTR_NAME));
		}
		if (prefSvc.isDefined(SITE_CLASSPATH_ATTR_NAME)) {
			setClassPath(prefSvc.getStringPreference(SITE_CLASSPATH_ATTR_NAME));
		}
		if (prefSvc.isDefined(OIL_OUT_ATTR_NAME)) {
			setOilOutputFile(new File(prefSvc.getStringPreference(OIL_OUT_ATTR_NAME)));
		}
		if (prefSvc.isDefined(MAX_PUBS_ATTR_NAME)) {
			setMaxPubs(prefSvc.getIntPreference(MAX_PUBS_ATTR_NAME));
		}
		if (prefSvc.isDefined(NUM_SITE_THREADS_ATTR_NAME)) {
			setNumSiteThreads(prefSvc.getIntPreference(NUM_SITE_THREADS_ATTR_NAME));
		}
		if (prefSvc.isDefined(TRACE_OUT_ATTR_NAME)) {
			try {
				setTraceOutputFile(new File(prefSvc.getStringPreference(TRACE_OUT_ATTR_NAME)));
			} catch (final CmdLineException e) {
				throw new IOException(e.getMessage());
			}
		}
		if (prefSvc.isDefined(DEBUG_LEVEL_ATTR_NAME)) {
			setDebugLevel(prefSvc.getIntPreference(DEBUG_LEVEL_ATTR_NAME));
		}
	}

	/**
	 * @param launchConfig
	 * @throws CoreException 
	 * @throws IOException 
	 */
	private void fillFromLaunchConfig(final ILaunchConfiguration launchConfig) throws CoreException, IOException {

		// The use of *_DEFAULT below is for type info. during compile-time overloaded method
		// resolution.  At runtime, *_DEFAULT is never used, because we test hasAttribute before
		// calling getAttribute.

		if (launchConfig.hasAttribute(TYPE_CHECK_ATTR_NAME)) {
			setTypeChecking(launchConfig.getAttribute(TYPE_CHECK_ATTR_NAME, TYPE_CHECK_DEFAULT));
		}
		if (launchConfig.hasAttribute(NO_PRELUDE_ATTR_NAME)) {
			setNoPrelude(launchConfig.getAttribute(NO_PRELUDE_ATTR_NAME, NO_PRELUDE_DEFAULT));
		}
		if (launchConfig.hasAttribute(EXCEPTIONS_ON_ATTR_NAME)) {
			setNoPrelude(launchConfig.getAttribute(EXCEPTIONS_ON_ATTR_NAME, EXCEPTIONS_ON_DEFAULT));
		}
		if (launchConfig.hasAttribute(INCLUDE_PATH_ATTR_NAME)) {
			setIncludePath(launchConfig.getAttribute(INCLUDE_PATH_ATTR_NAME, INCLUDE_PATH_DEFAULT));
		}
		if (launchConfig.hasAttribute(SITE_CLASSPATH_ATTR_NAME)) {
			setClassPath(launchConfig.getAttribute(SITE_CLASSPATH_ATTR_NAME, SITE_CLASSPATH_DEFAULT));
		}
		if (launchConfig.hasAttribute(OIL_OUT_ATTR_NAME)) {
			setOilOutputFile(new File(launchConfig.getAttribute(OIL_OUT_ATTR_NAME, OIL_OUT_DEFAULT)));
		}
		if (launchConfig.hasAttribute(MAX_PUBS_ATTR_NAME)) {
			setMaxPubs(launchConfig.getAttribute(MAX_PUBS_ATTR_NAME, MAX_PUBS_DEFAULT));
		}
		if (launchConfig.hasAttribute(NUM_SITE_THREADS_ATTR_NAME)) {
			setNumSiteThreads(launchConfig.getAttribute(NUM_SITE_THREADS_ATTR_NAME, NUM_SITE_THREADS_DEFAULT));
		}
		if (launchConfig.hasAttribute(TRACE_OUT_ATTR_NAME)) {
			try {
				setTraceOutputFile(new File(launchConfig.getAttribute(TRACE_OUT_ATTR_NAME, TRACE_OUT_DEFAULT)));
			} catch (final CmdLineException e) {
				throw new IOException(e.getMessage());
			}
		}
		if (launchConfig.hasAttribute(DEBUG_LEVEL_ATTR_NAME)) {
			setDebugLevel(launchConfig.getAttribute(DEBUG_LEVEL_ATTR_NAME, DEBUG_LEVEL_DEFAULT));
		}
	}

	protected static void initDefaultPrefs() {
		// We don't want to use a preferences.ini / preferences.properties file for default preferences,
		// but instead get them from the Config class's defaults. Activator gives us the opportunity to set the defaults here.
		final IEclipsePreferences defaultPrefs = new DefaultScope().getNode(Activator.getInstance().getLanguageID());
		defaultPrefs.putBoolean(TYPE_CHECK_ATTR_NAME, TYPE_CHECK_DEFAULT);
		defaultPrefs.putBoolean(NO_PRELUDE_ATTR_NAME, NO_PRELUDE_DEFAULT);
		defaultPrefs.putBoolean(EXCEPTIONS_ON_ATTR_NAME, EXCEPTIONS_ON_DEFAULT);
		defaultPrefs.put(INCLUDE_PATH_ATTR_NAME, INCLUDE_PATH_DEFAULT);
		defaultPrefs.put(SITE_CLASSPATH_ATTR_NAME, SITE_CLASSPATH_DEFAULT);
		defaultPrefs.put(OIL_OUT_ATTR_NAME, OIL_OUT_DEFAULT);
		defaultPrefs.putInt(MAX_PUBS_ATTR_NAME, MAX_PUBS_DEFAULT);
		defaultPrefs.putInt(NUM_SITE_THREADS_ATTR_NAME, NUM_SITE_THREADS_DEFAULT);
		defaultPrefs.put(TRACE_OUT_ATTR_NAME, TRACE_OUT_DEFAULT);
		defaultPrefs.putInt(DEBUG_LEVEL_ATTR_NAME, DEBUG_LEVEL_DEFAULT);
		//No need to flush() nodes in default scope
	}
}
