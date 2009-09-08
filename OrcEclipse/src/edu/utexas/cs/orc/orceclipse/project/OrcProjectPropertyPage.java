//
// OrcProjectPropertyPage.java -- Java class OrcProjectPropertyPage
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on Sep 6, 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.project;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PathEditor;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.kohsuke.args4j.CmdLineException;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.OrcConfigSettings;

/**
 * Property page for editing Orc project properties.
 *
 * @author jthywiss
 */
public class OrcProjectPropertyPage extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {
	private IProject project;

	/**
	 * Constructs an object of class OrcProjectPropertyPage.
	 *
	 */
	public OrcProjectPropertyPage() {
		super();
		// TODO Auto-generated constructor stub
	}

	/*
	 *  (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPropertyPage#getElement()
	 */
	public IAdaptable getElement() {
		return project;
	}

	/**
	 * Sets the element that owns properties shown on this page.
	 * <p>
	 * In the case of <code>OrcProjectPropertyPage</code>, this must be a project.
	 * A <code>ClassCastException</code> will result from setting an object that
	 * does not implement {@link IProject}.
	 * 
	 * @param element the project
	 */
	public void setElement(final IAdaptable element) {
		this.project = (IProject) element;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
	 */
	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(OrcConfigSettings.TYPE_CHECK_ATTR_NAME, "Type check", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor(OrcConfigSettings.NO_PRELUDE_ATTR_NAME, "Do not include standard prelude", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor(OrcConfigSettings.EXCEPTIONS_ON_ATTR_NAME, "Enable exceptions (experimental)", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new PathEditor(OrcConfigSettings.INCLUDE_PATH_ATTR_NAME, "Include path:", "Choose a directory to add to the Orc include search path", getFieldEditorParent()));
		addField(new PathEditor(OrcConfigSettings.SITE_CLASSPATH_ATTR_NAME, "Site class path:", "Choose a directory to add to the Orc site class path", getFieldEditorParent()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#doGetPreferenceStore()
	 */
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return new ScopedPreferenceStore(new ProjectScope(project), Activator.getInstance().getLanguageID());
	}
}
