package org.eclipse.ease.ext.modules.jdt;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ease.IScriptEngine;
import org.eclipse.ease.ext.modules.ide.IDEModule;
import org.eclipse.ease.modules.AbstractScriptModule;
import org.eclipse.ease.modules.IEnvironment;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpPostSaveListener;
import org.eclipse.jdt.internal.corext.fix.CleanUpPreferenceUtil;
import org.eclipse.jdt.internal.corext.fix.CleanUpRegistry;
import org.eclipse.jdt.internal.launching.LaunchingPlugin;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.internal.launching.VMDefinitionsContainer;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.AbstractSaveParticipantPreferenceConfiguration;
import org.eclipse.jdt.internal.ui.preferences.PreferencesAccess;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileManager;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.FormatterProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.IProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileVersioner;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IWorkingSet;

@SuppressWarnings("restriction")
public class JDTModule extends AbstractScriptModule {

	private interface ProfileSetup {

		public IProfileVersioner newProfileVersioner();

		public ProfileManager newProfileManager(List<Profile> profiles, IScopeContext instance,
				PreferencesAccess originalPreferences, IProfileVersioner profileVersioner);

		public ProfileStore newProfileStore(IProfileVersioner profileVersioner);

	}

	private final IDEModule ideModule = new IDEModule();

	@Override
	public void initialize(final IScriptEngine engine, final IEnvironment environment) {
		super.initialize(engine, environment);
		ideModule.initialize(engine, environment);
	}

	@WrapToScript
	public void setJavaVersion(final String javaVersion) {
		IEclipsePreferences nodeCore = InstanceScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);
		if (nodeCore != null) {
			nodeCore.put("org.eclipse.jdt.core.compiler.source", javaVersion);
			nodeCore.put("org.eclipse.jdt.core.compiler.codegen.targetPlatform", javaVersion);
			nodeCore.put("org.eclipse.jdt.core.compiler.compliance", javaVersion);
		}
	}

	@WrapToScript
	public void setDefaultJRE(final Object resourceObj) throws CoreException, FileNotFoundException {

		File installationLocation = ideModule.toFile(resourceObj);

		VMStandin vmStandin = null;
		List<VMStandin> standins = new ArrayList<>();
		IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
		for (IVMInstallType type : types) {
			IVMInstall[] vmInstalls = type.getVMInstalls();
			for (IVMInstall install : vmInstalls) {
				standins.add(new VMStandin(install));
				if (install.getInstallLocation().equals(installationLocation)) {
					vmStandin = new VMStandin(install);
				}
			}
		}
		if (vmStandin == null) {
			vmStandin = toVMStandin(installationLocation);
			standins.add(vmStandin);
		}

		saveVMs(standins, vmStandin);
	}

	@WrapToScript
	public Map<String, String> exportSaveActions() {
		return new HashMap<>(CleanUpPreferenceUtil.loadSaveParticipantOptions(InstanceScope.INSTANCE));
	}

	@WrapToScript
	public void importSaveActions(final Map<String, String> saveActions) {

		CleanUpRegistry cleanUpRegistry = JavaPlugin.getDefault().getCleanUpRegistry();
		Map<String, String> optionsMap = cleanUpRegistry.getDefaultOptions(CleanUpConstants.DEFAULT_SAVE_ACTION_OPTIONS)
				.getMap();
		optionsMap.putAll(saveActions);
		CleanUpPreferenceUtil.saveSaveParticipantOptions(InstanceScope.INSTANCE, optionsMap);

		IScopeContext scope = PreferencesAccess.getOriginalPreferences().getInstanceScope();
		scope.getNode(JavaUI.ID_PLUGIN)
				.putBoolean(AbstractSaveParticipantPreferenceConfiguration.EDITOR_SAVE_PARTICIPANT_PREFIX
						+ CleanUpPostSaveListener.POSTSAVELISTENER_ID, true);
	}

	@WrapToScript
	public void importJavaCleanup(final Object resourceObj) throws CoreException, FileNotFoundException {

		File file = ideModule.toFile(resourceObj);

		importJdtProfiles(file, new ProfileSetup() {

			@Override
			public IProfileVersioner newProfileVersioner() {
				return new CleanUpProfileVersioner();
			}

			@Override
			public ProfileManager newProfileManager(final List<Profile> profiles, final IScopeContext instance,
					final PreferencesAccess originalPreferences, final IProfileVersioner profileVersioner) {
				return new CleanUpProfileManager(profiles, instance, originalPreferences, profileVersioner);
			}

			@Override
			public ProfileStore newProfileStore(final IProfileVersioner profileVersioner) {
				return new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, profileVersioner);
			}
		});
	}

	@WrapToScript
	public void importJavaFormatter(final Object resourceObj) throws CoreException, FileNotFoundException {

		File file = ideModule.toFile(resourceObj);

		importJdtProfiles(file, new ProfileSetup() {

			@Override
			public IProfileVersioner newProfileVersioner() {
				return new ProfileVersioner();
			}

			@Override
			public ProfileManager newProfileManager(final List<Profile> profiles, final IScopeContext context,
					final PreferencesAccess originalPreferences, final IProfileVersioner profileVersioner) {
				return new FormatterProfileManager(profiles, context, originalPreferences, profileVersioner);
			}

			@Override
			public ProfileStore newProfileStore(final IProfileVersioner profileVersioner) {
				return new FormatterProfileStore(profileVersioner);
			}
		});
	}

	@WrapToScript
	public IWorkingSet initJavaWorkingSet(final String workingSetName) {
		return ideModule.initWorkingSet(workingSetName, "org.eclipse.jdt.ui.JavaWorkingSetPage");
	}

	@WrapToScript
	public boolean addJavaProjectToWorkingSet(final IWorkingSet workingSet, final IProject project)
			throws CoreException {
		Optional<IJavaProject> javaProjectOpt = tryGetJavaProject(project);
		if (javaProjectOpt.isPresent()) {
			IJavaProject javaProject = javaProjectOpt.get();
			List<IAdaptable> elements = new ArrayList<>();
			elements.addAll(Arrays.asList(workingSet.getElements()));
			elements.addAll(Arrays.asList(workingSet.adaptElements(new IAdaptable[] { javaProject })));
			workingSet.setElements(elements.toArray(new IAdaptable[elements.size()]));
			return true;
		}
		return false;
	}

	@WrapToScript
	public boolean removeJavaProjectFromWorkingSet(final IWorkingSet workingSet, final IProject project)
			throws CoreException {

		Optional<IJavaProject> javaProjectOpt = tryGetJavaProject(project);
		if (javaProjectOpt.isPresent()) {
			IJavaProject javaProject = javaProjectOpt.get();
			List<IAdaptable> elements = new ArrayList<>();
			elements.addAll(Arrays.asList(workingSet.getElements()));
			boolean removed = elements.remove(javaProject);
			if (removed) {
				workingSet.setElements(elements.toArray(new IAdaptable[elements.size()]));
			}
			return removed;
		}

		return false;
	}

	@WrapToScript
	public Optional<IJavaProject> tryGetJavaProject(final IProject project) throws CoreException {
		if (project.exists() && project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
			IProjectNature projectNature = project.getNature(JavaCore.NATURE_ID);
			if (projectNature instanceof IJavaProject) {
				IJavaProject javaProject = (IJavaProject) projectNature;
				return Optional.of(javaProject);
			}
		}
		return Optional.empty();
	}

	@WrapToScript
	public void clearWorkingSet(final IWorkingSet workingSet) {
		workingSet.setElements(new IAdaptable[0]);
	}

	protected void saveVMs(final List<VMStandin> standins, final VMStandin defaultStandin) throws CoreException {

		VMDefinitionsContainer vmContainer = new VMDefinitionsContainer();
		String defaultVMId = JavaRuntime.getCompositeIdFromVM(defaultStandin);
		vmContainer.setDefaultVMInstallCompositeID(defaultVMId);

		for (int i = 0; i < standins.size(); i++) {
			vmContainer.addVM(standins.get(i));
		}

		String vmDefXML = vmContainer.getAsXML();
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(LaunchingPlugin.ID_PLUGIN);
		if (prefs != null) {
			prefs.put(JavaRuntime.PREF_VM_XML, vmDefXML);
		}
		JavaRuntime.savePreferences();
	}

	protected VMStandin toVMStandin(final File installationLocation) {

		StandardVMType type = new StandardVMType() {

			@Override
			public String getId() {
				return "org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType";
			}
		};
		String uniqueId = createUniqueId(type);

		VMStandin vmStandin = new VMStandin(type, uniqueId);
		String vmName = installationLocation.getName();
		int index = vmName.lastIndexOf(".ee");
		if (index > 0) {
			vmName = vmName.substring(0, index);
		}
		vmStandin.setName(vmName);
		vmStandin.setInstallLocation(installationLocation);
		vmStandin.setLibraryLocations(null);

		return vmStandin;
	}

	protected String createUniqueId(final StandardVMType type) {
		String id = null;
		do {
			id = String.valueOf(System.currentTimeMillis());
		} while (type.findVMInstall(id) != null);
		return id;
	}

	protected void importJdtProfiles(final File file, final ProfileSetup profileSetup) throws CoreException {

		IProfileVersioner profileVersioner = profileSetup.newProfileVersioner();

		ProfileStore profileStore = profileSetup.newProfileStore(profileVersioner);
		List<Profile> profiles = profileStore.readProfiles(InstanceScope.INSTANCE);
		if (profiles == null) {
			profiles = new ArrayList<>();
		}

		ProfileManager profileManager = profileSetup.newProfileManager(profiles, InstanceScope.INSTANCE,
				PreferencesAccess.getOriginalPreferences(), profileVersioner);
		List<Profile> newProfiles = profileStore.readProfilesFromFile(file);
		for (Profile newProfile : newProfiles) {
			CustomProfile newCustomProfile = (CustomProfile) newProfile;
			profileVersioner.update(newCustomProfile);
			profileManager.addProfile(newCustomProfile);
		}

		profileStore.writeProfiles(profileManager.getSortedProfiles(), InstanceScope.INSTANCE);
		profileManager.commitChanges(InstanceScope.INSTANCE);
	}

}
