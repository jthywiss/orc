//
// EnableOrcNature.java -- Java class EnableOrcNature
// Project OrcEclipse
//
// Created by jthywiss on Jul 27, 2009.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.Messages;
import edu.utexas.cs.orc.orceclipse.build.OrcNature;

/**
 * Adds an Orc "nature" to the selected project's attributes.
 * <p>
 * (This class is a UI action delegate. The action is defined in the
 * <code>plugin.xml</code> file.)
 */
public class EnableOrcNature implements IWorkbenchWindowActionDelegate {
    private IProject fProject;

    /**
     * Constructs an object of class EnableOrcNature.
     */
    public EnableOrcNature() {
        /* Nothing to do */
    }

    @Override
    public void dispose() {
        /* Nothing to do */
    }

    @Override
    public void init(final IWorkbenchWindow window) {
        /* Nothing to do */
    }

    @Override
    public void run(final IAction action) {
        try {
            if (fProject.getNature("org.eclipse.jdt.core.javanature") != null) { //$NON-NLS-1$
                MessageDialog.openError(null, Messages.EnableOrcNature_AlreadyJavaErrorTitle, Messages.EnableOrcNature_AlreadJavaErrorMessage);
                return;
            }
        } catch (final CoreException e) {
            // This is OK, it means we don't have javanature
        }
        try {
            OrcNature.addToProject(fProject);
        } catch (final CoreException e) {
            Activator.logAndShow(e);
        }
    }

    @Override
    public void selectionChanged(final IAction action, final ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection ss = (IStructuredSelection) selection;
            final Object first = ss.getFirstElement();

            if (first instanceof IProject) {
                fProject = (IProject) first;
            } else if (first instanceof IJavaProject) {
                fProject = ((IJavaProject) first).getProject();
            }
        }
    }
}
