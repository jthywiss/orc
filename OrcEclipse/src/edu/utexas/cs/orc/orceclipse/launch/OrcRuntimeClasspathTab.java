//
// OrcRuntimeClasspathTab.java -- Java class OrcRuntimeClasspathTab
// Project OrcEclipse
//
// Created by jthywiss on Feb 28, 2010.
//
// Copyright (c) 2016 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.launch;

import org.eclipse.jdt.debug.ui.launchConfigurations.JavaClasspathTab;

import edu.utexas.cs.orc.orceclipse.Messages;
import edu.utexas.cs.orc.orceclipse.OrcPluginIds;

/**
 * A launch configuration tab that lets users set a classpath to load an
 * alternative Orc runtime engine. This subclasses the JDT classpath tab.
 *
 * @author jthywiss
 */
public class OrcRuntimeClasspathTab extends JavaClasspathTab {

    @Override
    public String getName() {
        return Messages.OrcRuntimeClasspathTab_RuntimeClasspathTabName;
    }

    @Override
    public String getId() {
        return OrcPluginIds.LaunchConfigurationTab.ORC_RUNTIME_CLASSPATH;
    }

    @Override
    public boolean isShowBootpath() {
        return false;
    }

}
