package org.eclipse.ease.ext.modules.ide;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.ease.modules.platform.PlatformModule;
import org.eclipse.ease.tools.ResourceTools;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.internal.ide.filesystem.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@SuppressWarnings("restriction")
public class IDEModule extends AbstractScriptModule {

    @SuppressWarnings("unchecked")
    @WrapToScript
    public <T> Optional<T> tryLoadService(final Class<T> clazz) {
        return Optional.of((T) PlatformModule.getService(clazz));
    }

    @WrapToScript
    public Job initCleanWorkspaceJob() {

        WorkspaceJob job = new WorkspaceJob("Refresh and clean workspace") {

            @Override
            public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {

                enableAutoBuild(false);

                for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
                    if (project.exists() && project.isOpen()) {
                        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                    }
                }

                enableAutoBuild(true);

                ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);

                return Status.OK_STATUS;
            }
        };
        return job;
    }

    @WrapToScript
    public IWorkingSet initWorkingSet(final String workingSetName, final String workingSetId) {
        IWorkingSetManager workingSetManager = getWorkingSetManager();
        IWorkingSet workingSet = workingSetManager.getWorkingSet(workingSetName);
        if (workingSet == null) {
            workingSet = workingSetManager.createWorkingSet(workingSetName, new IAdaptable[] {});
            workingSet.setId(workingSetId);
            workingSetManager.addWorkingSet(workingSet);
        }
        return workingSet;
    }

    @WrapToScript
    public boolean enableAutoBuild(final boolean enable) throws CoreException {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceDescription description = workspace.getDescription();
        boolean autoBuilding = description.isAutoBuilding();
        if (autoBuilding != enable) {
            description.setAutoBuilding(autoBuilding);
            workspace.setDescription(description);
        }
        return autoBuilding;
    }

    @WrapToScript
    public IWorkingSetManager getWorkingSetManager() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        return workbench.getWorkingSetManager();
    }

    @WrapToScript
    public File toFile(final Object resourceObj) throws FileNotFoundException {

        if (resourceObj instanceof File) {
            return (File) resourceObj;
        }

        return findFile(resourceObj).orElseThrow(
                () -> new FileNotFoundException(String.format("Can't resolve %s to a file!", resourceObj)));
    }

    @WrapToScript
    public IResource toResource(final Object resourceObj) {
        return findResource(resourceObj).orElseThrow(
                () -> new IllegalArgumentException(String.format("Can't resolve %s to a resource!", resourceObj)));
    }

    @WrapToScript
    public Optional<File> findFile(final Object resourceObj) {
        Object resolvedResourceObj = findResource(resourceObj).map(resource -> (Object) resource).orElseGet(() -> ResourceTools.resolve(resourceObj));
        if (resolvedResourceObj instanceof File) {
            return Optional.of((File) resolvedResourceObj);
        } else if (resolvedResourceObj instanceof IResource) {
            IResource resource = (IResource) resolvedResourceObj;
            IPath location = resource.getLocation();
            return Optional.ofNullable(location.toFile());
        }
        return Optional.empty();
    }

    @WrapToScript
    public Optional<Job> tryInitLinkImportProjectJob(final String projectName, final Object resourceObj)
            throws CoreException, FileNotFoundException {

        File projectFolder = toFile(resourceObj);

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        if (workspace.getRoot().getProject(projectName).exists()) {
            return Optional.empty();
        }

        File projectConfigFile = new File(projectFolder, ".project");
        if (!projectConfigFile.exists()) {
            return Optional.empty();
        }

        final IProjectDescription projectDescription = workspace
                .loadProjectDescription(Path.fromOSString(projectConfigFile.getAbsolutePath()));

        WorkspaceJob job = new WorkspaceJob("Link Import project " + projectName) {

            @Override
            public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
                IProject project = workspace.getRoot().getProject(projectName);
                project.create(projectDescription, monitor);
                project.open(monitor);
                return Status.OK_STATUS;
            }
        };

        return Optional.of(job);
    }

    @WrapToScript
    public Optional<Job> tryInitCopyImportProjectJob(final String projectName, final Object resourceObj, final boolean overwrite)
            throws CoreException, FileNotFoundException {

        final File projectFolder = toFile(resourceObj);

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject project = workspace.getRoot().getProject(projectName);

        if (project.exists() && !overwrite) {
            return Optional.empty();
        }

        final IOverwriteQuery overwriteQuery = new IOverwriteQuery() {

            @Override
            public String queryOverwrite(final String pathString) {
                return ALL;
            }

        };

        WorkspaceJob job = new WorkspaceJob("Copy Import project " + projectName) {

            @Override
            public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
                FileSystemStructureProvider fileSystemStructureProvider = new FileSystemStructureProvider();
                ImportOperation importOperation = new ImportOperation(project.getFullPath(), projectFolder,
                        fileSystemStructureProvider, overwriteQuery);
                importOperation.setCreateContainerStructure(false);
                try {
                    importOperation.run(monitor);
                } catch (InvocationTargetException | InterruptedException e) {
                    if (e.getCause() instanceof CoreException) {
                        return ((CoreException) e.getCause()).getStatus();
                    }
                    Bundle bundle = FrameworkUtil.getBundle(IDEModule.class);
                    return new Status(IStatus.ERROR, bundle.getSymbolicName(), 2, e.getCause().getLocalizedMessage(),
                            e);
                }
                return importOperation.getStatus();
            }

        };

        job.schedule();

        return Optional.of(job);
    }

    @WrapToScript
    public Optional<IResource> findResource(final Object resourceObj) {
        IResource resource = null;
        Object file = ResourceTools.resolve(resourceObj, getScriptEngine().getExecutedFile());
        if (file instanceof IResource) {
            resource = (IResource) file;
        } else {
            Object folder = ResourceTools.resolve(resourceObj, getScriptEngine().getExecutedFile());
            if (folder instanceof IContainer) {
                resource = (IResource) folder;
            }
        }
        return Optional.ofNullable(resource);
    }

}
