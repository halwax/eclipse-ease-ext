package org.eclipse.ease.ext.modules.subclipse;

import java.util.Optional;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.WrapToScript;
import org.tigris.subversion.subclipse.core.ISVNCoreConstants;
import org.tigris.subversion.subclipse.core.ISVNRemoteFolder;
import org.tigris.subversion.subclipse.core.ISVNRepositoryLocation;
import org.tigris.subversion.subclipse.core.SVNException;
import org.tigris.subversion.subclipse.core.SVNProviderPlugin;
import org.tigris.subversion.subclipse.core.commands.CheckoutCommand;
import org.tigris.subversion.subclipse.core.resources.SVNWorkspaceRoot;
import org.tigris.subversion.svnclientadapter.SVNRevision;

public class SubclipseModule extends AbstractScriptModule {

    @WrapToScript
    public Job initRepositoryJob(final String repositoryUrl) throws SVNException {

        Job job = new Job("Init svn repository " + repositoryUrl) {

            @Override
            protected IStatus run(final IProgressMonitor monitor) {

                SVNProviderPlugin provider = SVNProviderPlugin.getPlugin();
                Properties configuration = new Properties();
                configuration.setProperty("url", repositoryUrl);

                try {
                    ISVNRepositoryLocation repositoryLocation = provider.getRepositories().createRepository(configuration);
                    repositoryLocation.validateConnection(monitor);
                    provider.getRepositories().addOrUpdateRepository(repositoryLocation);
                } catch (SVNException e) {
                    return e.getStatus();
                }

                return Status.OK_STATUS;
            }
        };

        return job;
    }

    @WrapToScript
    public Optional<Job> checkOutProject(final String repositoryUrl, final String remoteFolder, final String projectName) {

        Job checkoutProjectJob = initCheckoutProjectJob(repositoryUrl, remoteFolder, projectName);
        checkoutProjectJob.schedule();

        return Optional.of(checkoutProjectJob);
    }

    @WrapToScript
    public Job initCheckoutProjectJob(final String repositoryUrl, final String remoteFolder, final String projectName) {
        Job job = new Job("Checkout project " + remoteFolder) {

            @Override
            protected IStatus run(final IProgressMonitor monitor) {

                SVNProviderPlugin providerPlugin = SVNProviderPlugin.getPlugin();

                try {
                    ISVNRepositoryLocation repositoryLocation = providerPlugin.getRepository(repositoryUrl);
                    ISVNRemoteFolder folder = repositoryLocation.getRemoteFolder(remoteFolder);

                    IProject project = null;
                    if (projectName == null) {
                        project = SVNWorkspaceRoot.getProject(folder, monitor);
                    } else {
                        project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
                    }

                    CheckoutCommand command = new CheckoutCommand(new ISVNRemoteFolder[] { folder }, new IProject[] { project });
                    command.setSvnRevision(SVNRevision.HEAD);
                    command.setDepth(ISVNCoreConstants.DEPTH_INFINITY);
                    command.setIgnoreExternals(false);
                    command.setRefreshProjects(true);
                    command.run(monitor);
                } catch (Exception e) {
                    if (e instanceof SVNException) {
                        return ((SVNException) e).getStatus();
                    } else {
                        throw new RuntimeException(e);
                    }
                }

                return Status.OK_STATUS;
            }

        };
        return job;
    }

}
