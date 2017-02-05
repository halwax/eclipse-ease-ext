package org.eclipse.ease.ext.modules.team;

import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.ease.ext.modules.ide.IDEModule;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;

public class TeamModule extends AbstractScriptModule {

	private IDEModule ideModule = new IDEModule();
	
	@WrapToScript
	public boolean isTeamPrivateOrDerived(final Object resourceObj) {
		Optional<IResource> resourceOpt = ideModule.findResource(resourceObj);
		if(resourceOpt.isPresent()) {
			IResource resource = resourceOpt.get();
			return resource.isTeamPrivateMember() || resource.isDerived(IResource.CHECK_ANCESTORS);
		}
		return false;
	}
	
	@WrapToScript
	public boolean isInSync(final Object resourceObj) throws TeamException {
		Optional<IResource> resourceOpt = ideModule.findResource(resourceObj);
		if(!resourceOpt.isPresent()) {
			return false;
		}
		IResource resource = resourceOpt.get();
		
		if(!isShared(resource)) {
			return false;
		}
		
		Optional<SyncInfo> syncInfoOpt = tryGetSyncInfo(resource);
		if(syncInfoOpt.isPresent()) {
			return SyncInfo.isInSync(syncInfoOpt.get().getKind());
		}
		return false;
	}
	
	@WrapToScript
	public boolean isShared(final Object resourceObj) throws TeamException {
		Optional<IResource> resourceOpt = ideModule.findResource(resourceObj);
		if(resourceOpt.isPresent()) {
			IResource resource = resourceOpt.get();
			if(isTeamPrivateOrDerived(resource)) {
				return false;
			}
			return tryGetResourceVariant(resource).isPresent();
		}
		
		return false;
	}
	
	protected Optional<IResourceVariant> tryGetResourceVariant(IResource resource) throws TeamException {
		Optional<SyncInfo> syncInfoOpt = tryGetSyncInfo(resource);
		if(syncInfoOpt.isPresent()) {
			return Optional.ofNullable(syncInfoOpt.get().getRemote());
		}
		return Optional.empty();
	}
	
	protected Optional<SyncInfo> tryGetSyncInfo(IResource resource) throws TeamException {
		IProject project = resource.getProject();
		if(RepositoryProvider.isShared(project)) {
			RepositoryProvider provider = RepositoryProvider.getProvider(project);
			if(provider!=null) {
				Subscriber subscriber = provider.getSubscriber();
				SyncInfo syncInfo = subscriber.getSyncInfo(resource);
				return Optional.ofNullable(syncInfo);
			}
		}
		return Optional.empty();
	}

}
