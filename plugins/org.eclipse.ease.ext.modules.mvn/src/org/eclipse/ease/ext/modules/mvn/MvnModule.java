package org.eclipse.ease.ext.modules.mvn;

import java.io.File;
import java.util.Optional;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ease.IScriptEngine;
import org.eclipse.ease.ext.modules.ide.IDEModule;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.IEnvironment;
import org.eclipse.ease.modules.WrapToScript;

public class MvnModule extends AbstractScriptModule {

	private IDEModule ideModule = new IDEModule();
	
	@Override
	public void initialize(IScriptEngine engine, IEnvironment environment) {
		super.initialize(engine, environment);
		ideModule.initialize(engine, environment);
	}
	
	@WrapToScript
	public Optional<Job> tryInitMvnUpdateUserSettingsJob(Object resourceObj) {
		
		Optional<File> settingsFileOpt = ideModule.findFile(resourceObj);
		if(!settingsFileOpt.filter((file) -> file.exists() && file.isFile()).isPresent()) {
			return Optional.empty();
		}
		
		File settingsFile = settingsFileOpt.get();
		
		return Optional.empty();
	}

}
