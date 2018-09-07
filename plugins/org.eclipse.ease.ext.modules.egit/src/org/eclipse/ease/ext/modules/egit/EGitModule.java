package org.eclipse.ease.ext.modules.egit;

import java.io.File;

import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.ease.tools.ResourceTools;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class EGitModule extends AbstractScriptModule {

    @WrapToScript
    public File cloneRemoteRepository(final String remoteRepositoryLocation, final String localGitRepositoryPath,
            @ScriptParameter(defaultValue = ScriptParameter.NULL)
            final String user, @ScriptParameter(defaultValue = ScriptParameter.NULL)
            final String password, @ScriptParameter(defaultValue = ScriptParameter.NULL)
            final String branch)
            throws InvalidRemoteException, TransportException, GitAPIException {

        final Object localGitRepositoryResource = ResourceTools.resolve(localGitRepositoryPath, getScriptEngine().getExecutedFile());
        final File localGitRepositoryFolder = ResourceTools.toFile(localGitRepositoryResource);

        if (ResourceTools.isFolder(localGitRepositoryFolder)) {
            final CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setURI(remoteRepositoryLocation);
            cloneCommand.setDirectory(localGitRepositoryFolder);
            if (branch != null) {
                cloneCommand.setBranch(branch);
            }

            if ((user != null) && (password != null)) {
                cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(user, password));
            }

            final Git result = cloneCommand.call();
            File localGitRepositoryFile = result.getRepository().getDirectory();
            addToRepositoryConfiguration(localGitRepositoryFile);

            return localGitRepositoryFile;

        } else {
            throw new RuntimeException("invalid local folder detected: " + localGitRepositoryPath);
        }

    }

    @WrapToScript
    @SuppressWarnings("restriction")
	public void addToRepositoryConfiguration(File localGitRepositoryFile) {
		org.eclipse.egit.core.Activator.getDefault().getRepositoryUtil().addConfiguredRepository(localGitRepositoryFile);
	}

}
