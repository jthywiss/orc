//
// OrcLaunchDelegate.java -- Java class OrcLaunchDelegate
// Project OrcEclipse
//
// $Id$
//
// Created by jthywiss on 04 Aug 2009.
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package edu.utexas.cs.orc.orceclipse.launch;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import orc.Orc;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.stringsubstitution.SelectedResourceManager;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader;
import org.eclipse.ui.statushandlers.StatusManager;
import org.kohsuke.args4j.CmdLineException;
import org.osgi.framework.BundleException;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;

import edu.utexas.cs.orc.orceclipse.Activator;
import edu.utexas.cs.orc.orceclipse.OrcConfigSettings;

/**
 * Launches an Orc program.
 * <p>
 * A launch configuration delegate performs launching for a
 * specific type of launch configuration. A launch configuration
 * delegate is defined by the <code>delegate</code> attribute
 * of a <code>launchConfigurationType</code> extension.
 *
 * @author jthywiss
 */
@SuppressWarnings("restriction")
public class OrcLaunchDelegate extends AbstractJavaLaunchConfigurationDelegate {

	/**
	 * Launch configuration extension type ID for an Orc Program Launch configuration
	 */
	public static final String LAUNCH_CONFIG_ID = "edu.utexas.cs.orc.orceclipse.launch.orcApplication";

	/**
	 * @return LaunchConfigurationType for Orc Applications
	 */
	public static ILaunchConfigurationType getLaunchConfigType() {
		return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(OrcLaunchDelegate.LAUNCH_CONFIG_ID);
	}

	/**
	 * @param configuration LaunchConfigurationWorkingCopy to update
	 */
	public static void setDefaults(final ILaunchConfigurationWorkingCopy configuration) {
		// Currently, a minimal Orc launch config. is an empty one (no required attributes)
		// So, do nothing here
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void launch(final ILaunchConfiguration configuration, final String mode, final ILaunch launch, IProgressMonitor monitor) throws CoreException {

		if (SelectedResourceManager.getDefault().getSelectedResource() == null) {
			StatusManager.getManager().handle(new Status(IStatus.INFO, Activator.getInstance().getID(), 1, "Unable to Launch -- No resource selected.", null), StatusManager.SHOW);
			return;
		}

		// Derived from org.eclipse.jdt.launching.JavaLaunchDelegate.java,
		// Revision 1.8 (02 Oct 2007), trunk rev as of 04 Aug 2009

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(MessageFormat.format("{0}...", new String[] { configuration.getName() }), 2); //$NON-NLS-1$
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}
		try {
			monitor.subTask("Verifying launch attributes...");
			
			IResource orcProgToLaunch = SelectedResourceManager.getDefault().getSelectedResource();
			OrcConfigSettings orcConfig;
			try {
				orcConfig = new OrcConfigSettings(orcProgToLaunch.getProject(), configuration);
			} catch (IOException e) {
				throw new ResourceException(IResourceStatus.OPERATION_FAILED, null, e.getLocalizedMessage(), e);
			}
			try {
				orcConfig.setInputFile(orcProgToLaunch.getRawLocation().toFile());
			} catch (CmdLineException e) {
				throw new ResourceException(IResourceStatus.NOT_FOUND_LOCAL, orcProgToLaunch.getRawLocation(), e.getLocalizedMessage(), e);
			}

			final String mainTypeName = "orc.Orc";
			final IVMRunner runner = getVMRunner(configuration, mode);

			final File workingDir = verifyWorkingDirectory(configuration);
			String workingDirName = null;
			if (workingDir != null) {
				workingDirName = workingDir.getAbsolutePath();
			}

			// Environment variables
			final String[] envp = getEnvironment(configuration);

			// Program & VM arguments
			String pgmArgs = orcConfig.composeCmdLine();
			pgmArgs = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(pgmArgs);
			final String vmArgs = getVMArguments(configuration);
			final ExecutionArguments execArgs = new ExecutionArguments(vmArgs, pgmArgs);

			// VM-specific attributes
			final Map vmAttributesMap = getVMSpecificAttributesMap(configuration);

			// Classpath
			final String[] classpath = getAbsoluteClasspathForClass(Orc.class);
			//TODO: Allow entries from getClasspath(configuration) to be added to the orc.Orc's classpath?

			// Create VM config
			final VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
			runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
			runConfig.setEnvironment(envp);
			runConfig.setVMArguments(execArgs.getVMArgumentsArray());
			runConfig.setWorkingDirectory(workingDirName);
			runConfig.setVMSpecificAttributesMap(vmAttributesMap);

			// Bootpath
			runConfig.setBootClassPath(getBootpath(configuration));

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}

			// done the verification phase
			monitor.worked(1);

			// Launch the configuration - 1 unit of work
			runner.run(runConfig, launch, monitor);
			for (final IProcess proc : launch.getProcesses()) {
				final String processLabel = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution("${resource_path}") + " [" + configuration.getType().getName() + "] (" + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(new Date(System.currentTimeMillis())) + ")";
				proc.setAttribute(IProcess.ATTR_PROCESS_LABEL, processLabel);
			}

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}
		} finally {
			monitor.done();
		}
	}

	private String[] getAbsoluteClasspathForClass(final Class classOfInterest) {
		//FIXME: Is this possible without using the internal OSGi BaseData and DefaultClassLoader classes?
		final BaseData basedata = ((DefaultClassLoader) classOfInterest.getClassLoader()).getClasspathManager().getBaseData();
		String[] classpath = null;
		try {
			classpath = basedata.getClassPath();
			for (int i = 0; i < classpath.length; i++) {
				final File classpathEntryFile = basedata.getBundleFile().getFile(classpath[i], false);
				if (classpathEntryFile != null) {
					classpath[i] = classpathEntryFile.getAbsolutePath();
				} else {
					// Cannot get the file for this classpath entry.
					// This happens, for example, for "." in a deployed plugin.
					// We'll just guess the bundle location itself, then.
					classpath[i] = basedata.getBundleFile().getBaseFile().getAbsolutePath();
				}
			}
		} catch (final BundleException e) {
			// This is thrown on an invalid JAR manifest, but then we wouldn't be here, so this is an "impossible" case
			Activator.log(e);
		}
		return classpath;
	}

	/*
	 * Derived from org.eclipse.jdt.internal.launching.StandardVMRunner.java,
	 * Revision 1.56 (31 Mar 2009), trunk rev as of 20 Aug 2009 
	 */
	protected static String convertClassPath(final List cp) {
		final StringBuffer buf = new StringBuffer();
		if (cp.size() == 0) {
			return ""; //$NON-NLS-1$
		}
		for (int i = 0; i < cp.size(); i++) {
			if (i > 0) {
				buf.append(File.pathSeparator);
			}
			buf.append(cp.get(i));
		}
		return buf.toString();
	}
}
