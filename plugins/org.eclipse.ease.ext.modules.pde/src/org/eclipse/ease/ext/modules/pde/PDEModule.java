package org.eclipse.ease.ext.modules.pde;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ease.IScriptEngine;
import org.eclipse.ease.ext.modules.ide.IDEModule;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.IEnvironment;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;

public class PDEModule extends AbstractScriptModule {
	
	private IDEModule ideModule = new IDEModule();
	
	@Override
	public void initialize(IScriptEngine engine, IEnvironment environment) {
		super.initialize(engine, environment);
		ideModule.initialize(engine, environment);
	}
	
	@WrapToScript
	public Optional<Job> tryInitLoadDefaultTargetPlatformJob(final Object resourceObj) {
		
		IResource resource = ideModule.toResource(resourceObj);
		
		if(resource instanceof IFile) {
			IFile file = (IFile) resource;
			
			Optional<ITargetPlatformService> targetPlatformServiceOpt = ideModule.tryLoadService(ITargetPlatformService.class);
			if(targetPlatformServiceOpt.isPresent()) {
				ITargetPlatformService targetPlatformService = targetPlatformServiceOpt.get();
				
				WorkspaceJob job = new WorkspaceJob("Set target " + file.getName()) {
					
					@Override
					public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
						
						ITargetHandle targetHandle = targetPlatformService.getTarget(file);
						ITargetDefinition targetDefinition = targetHandle.getTargetDefinition();						
						IStatus status = targetDefinition.resolve(monitor);
						
						LoadTargetDefinitionJob.load(targetDefinition);
						
						return status;
					}
				};
				
				return Optional.of(job);
				
			}			
		}
		
		return Optional.empty();
	}

}
