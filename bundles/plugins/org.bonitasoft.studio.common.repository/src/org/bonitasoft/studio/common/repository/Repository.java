/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.studio.common.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.xbean.classloader.NonLockingJarFileClassLoader;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.forms.client.model.FormFieldValue;
import org.bonitasoft.forms.server.validator.IFormFieldValidator;
import org.bonitasoft.studio.common.DateUtil;
import org.bonitasoft.studio.common.FileUtil;
import org.bonitasoft.studio.common.ProductVersion;
import org.bonitasoft.studio.common.extension.BonitaStudioExtensionRegistryManager;
import org.bonitasoft.studio.common.jface.FileActionDialog;
import org.bonitasoft.studio.common.log.BonitaStudioLog;
import org.bonitasoft.studio.common.repository.filestore.FileStoreChangeEvent;
import org.bonitasoft.studio.common.repository.model.IRepository;
import org.bonitasoft.studio.common.repository.model.IRepositoryFileStore;
import org.bonitasoft.studio.common.repository.model.IRepositoryStore;
import org.bonitasoft.studio.common.repository.operation.ExportBosArchiveOperation;
import org.bonitasoft.studio.common.repository.operation.ImportBosArchiveOperation;
import org.bonitasoft.studio.common.repository.preferences.RepositoryPreferenceConstant;
import org.bonitasoft.studio.common.repository.store.RepositoryStoreComparator;
import org.bonitasoft.studio.common.repository.store.SourceRepositoryStore;
import org.bonitasoft.studio.pics.Pics;
import org.codehaus.groovy.eclipse.core.compiler.CompilerUtils;
import org.codehaus.groovy.frameworkadapter.util.SpecifiedVersion;
import org.eclipse.core.internal.resources.ProjectDescriptionReader;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.edapt.migration.MigrationException;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClasspathValidation;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.JavaProject;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.CPListElement;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.framework.adaptor.BundleClassLoader;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.xml.sax.InputSource;

/**
 * @author Romain Bioteau
 */
public class Repository implements IRepository {

    private static final String REPOSITORY_STORE_EXTENSION_POINT_ID = "org.bonitasoft.studio.repositoryStore";

    public static final IProgressMonitor NULL_PROGRESS_MONITOR = new NullProgressMonitor();

    private static final String CLASS = "class";

    private String name;

    private IProject project;

    private SortedMap<Class<?>, IRepositoryStore<? extends IRepositoryFileStore>> stores;

    private IProgressMonitor monitor;

    public Repository() {
    }

    @Override
    public void createRepository(final String repositoryName) {
        name = repositoryName;
        project = ResourcesPlugin.getWorkspace().getRoot().getProject(repositoryName);
    }

    @Override
    public void create(final boolean migrateStoreIfNeeded) {
        try {
            final long init = System.currentTimeMillis();
            if (BonitaStudioLog.isLoggable(IStatus.OK)) {
                BonitaStudioLog.debug("Creating repository " + project.getName() + "...", CommonRepositoryPlugin.PLUGIN_ID);
            }
            final boolean projectExists = project.exists();
            if (!projectExists) {
                project.create(NULL_PROGRESS_MONITOR);
            }
            disableBuild();
            open();
            if (!projectExists) {
                initializeProject(project);
            }

            initRepositoryStores(migrateStoreIfNeeded);
            refreshClasspath(project);

            enableBuild();
            try {
                getProject().build(IncrementalProjectBuilder.FULL_BUILD, NULL_PROGRESS_MONITOR);
            } catch (final CoreException e) {
                BonitaStudioLog.error(e, CommonRepositoryPlugin.PLUGIN_ID);
            }
            if (BonitaStudioLog.isLoggable(IStatus.OK)) {
                final long duration = System.currentTimeMillis() - init;
                BonitaStudioLog.debug("Repository " + project.getName() + " created in " + DateUtil.getDisplayDuration(duration),
                        CommonRepositoryPlugin.PLUGIN_ID);
            }
        } catch (final Exception e) {
            BonitaStudioLog.error(e);
        }
    }

    @Override
    public void create() {
        create(false);
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.common.repository.IRepository#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.common.repository.IRepository#isShared()
     */
    @Override
    public boolean isShared() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.common.repository.IRepository#getProject()
     */
    @Override
    public IProject getProject() {
        return project;
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.common.repository.IRepository#open()
     */
    @SuppressWarnings("restriction")
    @Override
    public void open() {
        try {
            if (!project.isOpen()) {
                BonitaStudioLog.log("Opening project: " + project.getName());
                project.open(NULL_PROGRESS_MONITOR);
                final JavaProject jProject = (JavaProject) project.getNature(JavaCore.NATURE_ID);
                if (jProject != null) {
                    if (!jProject.isOpen()) {
                        jProject.open(NULL_PROGRESS_MONITOR);
                    }
                    new ClasspathValidation(jProject).validate();
                } else {
                    BonitaStudioLog.log("Cannot retrieve the JavaProject Nature from the project: " + project.getName());
                    project.open(NULL_PROGRESS_MONITOR);//Open anyway
                }
            }
            updateStudioShellText();
        } catch (final CoreException e) {
            BonitaStudioLog.error(e);
        }
    }

    @Override
    public void updateStudioShellText() {
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                if (PlatformUI.isWorkbenchRunning() && PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
                    final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                    final String currentName = shell.getText();
                    final int index = currentName.indexOf(" - ");
                    String newName = index > 0 ? currentName.substring(0, index)
                            : currentName;
                    if (!getName()
                            .equals(RepositoryPreferenceConstant.DEFAULT_REPOSITORY_NAME)) {
                        newName = newName + " - " + getName();
                    }
                    shell.setText(newName);
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.common.repository.IRepository#close()
     */
    @Override
    public void close() {
        try {
            BonitaStudioLog.debug("Closing repository " + project.getName(), CommonRepositoryPlugin.PLUGIN_ID);
            if (project.isOpen()) {
                if (stores != null) {
                    for (final IRepositoryStore<? extends IRepositoryFileStore> store : stores.values()) {
                        store.close();
                    }
                }
                project.close(NULL_PROGRESS_MONITOR);
            }
        } catch (final CoreException e) {
            BonitaStudioLog.error(e);
        }
        if (stores != null) {
            stores.clear();
            stores = null;
        }
    }

    protected synchronized void initRepositoryStores(final boolean migrateStoreIfNeeded) {
        if (stores == null || stores.isEmpty()) {
            disableBuild();
            stores = new TreeMap<Class<?>, IRepositoryStore<? extends IRepositoryFileStore>>(new Comparator<Class<?>>() {

                @Override
                public int compare(final Class<?> o1, final Class<?> o2) {
                    return o1.getName().compareTo(o2.getName());
                }

            });
            final IConfigurationElement[] elements = BonitaStudioExtensionRegistryManager.getInstance().getConfigurationElements(
                    REPOSITORY_STORE_EXTENSION_POINT_ID);
            for (final IConfigurationElement configuration : elements) {
                try {
                    final IRepositoryStore<? extends IRepositoryFileStore> store = createRepositoryStore(configuration);
                    if (migrateStoreIfNeeded) {
                        try {
                            store.migrate();
                        } catch (final MigrationException e) {
                            BonitaStudioLog.error(e, CommonRepositoryPlugin.PLUGIN_ID);
                        }
                    }
                    stores.put(store.getClass(), store);
                } catch (final CoreException e) {
                    BonitaStudioLog.error(e);
                }
            }
        }
    }

    protected IRepositoryStore<? extends IRepositoryFileStore> createRepositoryStore(
            final IConfigurationElement configuration) throws CoreException {
        final IRepositoryStore<? extends IRepositoryFileStore> store = (IRepositoryStore<?>) configuration.createExecutableExtension(CLASS);
        monitorSubtask(Messages.bind(Messages.creatingStore, store.getDisplayName()));
        store.createRepositoryStore(this);
        monitorWorked(1);
        return store;
    }

    @Override
    public void enableBuild() {
        Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_BUILD);
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IWorkspaceDescription desc = workspace.getDescription();
        if (!desc.isAutoBuilding()) {
            final boolean enableAutobuild = PlatformUI.isWorkbenchRunning();
            desc.setAutoBuilding(enableAutobuild);
            try {
                workspace.setDescription(desc);
            } catch (final CoreException e) {
                BonitaStudioLog.error(e, CommonRepositoryPlugin.PLUGIN_ID);
            }
            RepositoryManager.getInstance().getPreferenceStore().setValue(RepositoryPreferenceConstant.BUILD_ENABLE, enableAutobuild);
        }
    }

    @Override
    public void disableBuild() {
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IWorkspaceDescription desc = workspace.getDescription();
        if (desc.isAutoBuilding()) {
            desc.setAutoBuilding(false);
            try {
                workspace.setDescription(desc);
            } catch (final CoreException e) {
                BonitaStudioLog.error(e, CommonRepositoryPlugin.PLUGIN_ID);
            }
            RepositoryManager.getInstance().getPreferenceStore().setValue(RepositoryPreferenceConstant.BUILD_ENABLE, false);
        }
    }

    protected void initializeProject(final IProject project) throws CoreException, JavaModelException {
        /* create the project only one time, it is called by each repository which is part of it */
        createProjectDescriptor(project);
        createJavaProject(project);
    }

    protected void createJavaProject(final IProject project) {
        monitorSubtask(Messages.initializingJavaProject);
        final IJavaProject javaProject = JavaCore.create(project);
        javaProject.setOption(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_6);
        javaProject.setOption(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_6);
        javaProject.setOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_6);
        javaProject.setOption(JavaCore.CORE_JAVA_BUILD_INVALID_CLASSPATH, "ignore");
        CompilerUtils.setCompilerLevel(project, SpecifiedVersion._18);
        monitorWorked(1);
    }

    protected void createProjectDescriptor(final IProject project) throws CoreException {
        final IProjectDescription descriptor = project.getDescription();
        descriptor.setComment(ProductVersion.CURRENT_VERSION);
        final String[] natures = descriptor.getNatureIds();
        final List<String> existingNatures = Arrays.asList(natures);
        final Set<String> additionalNatures = getNatures();
        final Set<String> notExistingNature = new HashSet<String>();
        for (final String natureId : additionalNatures) {
            BonitaStudioLog.log("Plan to add Project Nature " + natureId);
            @SuppressWarnings("restriction")
            final Object naturDesc = ((Workspace) ResourcesPlugin.getWorkspace()).getNatureManager().getNatureDescriptor(natureId);
            if (naturDesc == null) {
                notExistingNature.add(natureId);
                BonitaStudioLog.log("Project nature " + natureId + " not found");
            }
            if (existingNatures.contains(natureId)) {
                notExistingNature.add(natureId);
                BonitaStudioLog.log("Project nature " + natureId + " already exists");
            }
        }
        additionalNatures.removeAll(notExistingNature);

        final String[] arryOfNatures = additionalNatures.toArray(new String[] {});
        final String[] newNatures = new String[natures.length + arryOfNatures.length];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        for (int i = natures.length; i < natures.length + arryOfNatures.length; i++) {
            newNatures[i] = arryOfNatures[i - natures.length];
        }
        descriptor.setNatureIds(newNatures);
        addBuilders(descriptor);
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    BonitaStudioLog.log("Setting Project Descritor with natures:" + descriptor.getNatureIds());
                    project.setDescription(descriptor, null);
                } catch (final CoreException e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }

    protected void addBuilders(final IProjectDescription desc) {
        final ICommand[] existingCommands = desc.getBuildSpec();
        final Map<String, ICommand> existingBuilders = new HashMap<String, ICommand>();
        for (final ICommand builder : existingCommands) {
            existingBuilders.put(builder.getBuilderName(), builder);
        }
        for (final String builderId : getBuilders()) {
            if (!existingBuilders.containsKey(builderId)) {
                // add builder to project
                final ICommand command = desc.newCommand();
                command.setBuilderName(builderId);
                existingBuilders.put(builderId, command);
            }
        }
        desc.setBuildSpec(existingBuilders.values().toArray(new ICommand[existingBuilders.values().size()]));
    }

    protected List<String> getBuilders() {
        return new ArrayList<String>();
    }

    protected Set<String> getNatures() {
        final Set<String> result = new HashSet<String>();
        result.add("org.eclipse.xtext.ui.shared.xtextNature");
        result.add("org.bonitasoft.studio.common.repository.bonitaNature");
        result.add(JavaCore.NATURE_ID);
        result.add("org.eclipse.pde.PluginNature");
        result.add("org.eclipse.jdt.groovy.core.groovyNature");
        return result;
    }

    private boolean classpathExists() {
        return project.findMember(".classpath") != null;
    }

    protected void refreshClasspath(final IProject extensionsProject) throws CoreException {
        monitorSubtask(Messages.initializingProjectClasspath);
        createProjectManifest(extensionsProject);

        final IJavaProject javaProject = getJavaProject();
        final List<IClasspathEntry> entries = addClasspathEntries();

        try {
            addSpecificEntriesForDevMode(entries);
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        BonitaStudioLog.debug("Updating build path...", CommonRepositoryPlugin.PLUGIN_ID);
        javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), true, Repository.NULL_PROGRESS_MONITOR);
        monitorWorked(1);
    }

    protected void addSpecificEntriesForDevMode(final List<IClasspathEntry> entries) throws IOException, MalformedURLException {
        /* Workaround for dev mode */
        if (Platform.inDevelopmentMode()) { // WORKAROUND FOR DEV MODE: see Eclipse Bug 111238
            // BOS Common
            final String bosCommonResource = AbstractConnector.class.getCanonicalName().replace('.', '/') + ".class";
            final URL bosBundleResource = AbstractConnector.class.getClassLoader().getResource(bosCommonResource);
            URL serverResource = FileLocator.resolve(bosBundleResource);
            if (serverResource.toString().startsWith("jar:file")) {
                serverResource = new URL(serverResource.toString().substring("jar:".length(), serverResource.toString().lastIndexOf('!')));
            }
            entries.add(JavaCore.newLibraryEntry(Path.fromOSString(serverResource.getFile()), null, null));

            // Forms client
            final String formsClientResource = FormFieldValue.class.getCanonicalName().replace('.', '/') + ".class";
            final URL formsClientBundleResource = FormFieldValue.class.getClassLoader().getResource(formsClientResource);
            URL formsResource = FileLocator.resolve(formsClientBundleResource);
            if (formsResource.toString().startsWith("jar:file")) {
                formsResource = new URL(formsResource.toString().substring("jar:".length(), formsResource.toString().lastIndexOf('!')));
            }
            entries.add(JavaCore.newLibraryEntry(Path.fromOSString(formsResource.getFile()), null, null));

            // Forms server
            final String formsServerResourcePath = IFormFieldValidator.class.getCanonicalName().replace('.', '/') + ".class";
            final URL formsServerBundleResource = FormFieldValue.class.getClassLoader().getResource(formsServerResourcePath);
            URL formsServerResource = FileLocator.resolve(formsServerBundleResource);
            if (formsServerResource.toString().startsWith("jar:file")) {
                formsServerResource = new URL(formsServerResource.toString().substring("jar:".length(), formsServerResource.toString().lastIndexOf('!')));
            }
            entries.add(JavaCore.newLibraryEntry(Path.fromOSString(formsServerResource.getFile()), null, null));

        }
    }

    @SuppressWarnings("rawtypes")
    protected List<IClasspathEntry> addClasspathEntries() {
        final List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
        // SET Java container
        entries.add(JavaCore.newContainerEntry(new Path("repositoryDependencies"), true)); //$NON-NLS-1$
        entries.add(JavaCore.newContainerEntry(JavaRuntime.newJREContainerPath(JavaRuntime.getExecutionEnvironmentsManager().getEnvironment("JavaSE-1.6"))));
        entries.add(JavaCore.newContainerEntry(new Path("org.eclipse.pde.core.requiredPlugins"))); //$NON-NLS-1$
        entries.add(JavaCore.newContainerEntry(new Path("GROOVY_SUPPORT"), true)); //$NON-NLS-1$

        // Add src folders in classpath
        for (final IRepositoryStore repository : getAllStores()) {
            if (repository instanceof SourceRepositoryStore) {
                final IClasspathEntry newSourceEntry = JavaCore.newSourceEntry(repository.getResource().getFullPath());
                if (!entries.contains(newSourceEntry)) {
                    entries.add(newSourceEntry);
                }
            }
        }
        return entries;
    }

    @SuppressWarnings("restriction")
    protected void createProjectManifest(final IProject extensionsProject) throws CoreException {
        final IFolder metaInf = project.getFolder("META-INF"); //$NON-NLS-1$
        if (!metaInf.exists()) {
            metaInf.create(false, true, null);
        }
        final IFile projectManifest = extensionsProject.getFolder("META-INF").getFile("MANIFEST.MF"); //$NON-NLS-1$
        final InputStream is = Repository.class.getResourceAsStream("MANIFEST.MF.template");
        final BundleClassLoader cl = (BundleClassLoader) BusinessArchive.class.getClassLoader();
        final InputStream is2 = FileUtil.replaceStringInFile(is, "XXX_ENGINE_BUNDLE_XXX", cl.getBundle().getSymbolicName());
        if (!projectManifest.exists()) {
            projectManifest.create(is2, false, null);
        } else {
            projectManifest.setContents(is2, IResource.FORCE, null);
        }
    }

    @Override
    public void refresh(IProgressMonitor monitor) {
        if (isBuildEnable()) {
            try {
                if (monitor == null) {
                    monitor = NULL_PROGRESS_MONITOR;
                }
                if (!getProject().isSynchronized(IResource.DEPTH_INFINITE)) {
                    getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
                }
                final IJavaProject javaProject = getJavaProject();
                if (javaProject != null) {
                    refreshClasspath(javaProject, null, monitor);
                    getProject().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

                }
            } catch (final Exception ex) {
                BonitaStudioLog.error(ex);
            }
        }
    }

    protected void refreshClasspath(final IJavaProject javaProject, List<CPListElement> classPathElementList,
            final IProgressMonitor monitor) throws JavaModelException, CoreException {
        final JavaModelManager manager = JavaModelManager.getJavaModelManager();
        manager.getJavaModel().refreshExternalArchives(null, Repository.NULL_PROGRESS_MONITOR);
        if (classPathElementList == null) {
            final CPListElement[] existingCPElement = CPListElement.createFromExisting(javaProject);
            classPathElementList = Arrays.asList(existingCPElement);
        }
        BuildPathsBlock.flush(classPathElementList, javaProject.getOutputLocation(), javaProject, null, monitor);
    }

    @Override
    public boolean isBuildEnable() {
        return RepositoryManager.getInstance().getPreferenceStore().getBoolean(RepositoryPreferenceConstant.BUILD_ENABLE);
    }

    @Override
    public IJavaProject getJavaProject() {
        if (getProject() != null && getProject().isAccessible()) {
            try {
                final IJavaProject project = (IJavaProject) getProject().getNature(JavaCore.NATURE_ID);
                return project;
            } catch (final CoreException ex) {
                BonitaStudioLog.error(ex);
                return null;
            }
        }
        return null;
    }

    @Override
    public void delete() {
        BonitaStudioLog.debug("Deleting repository " + project.getName(), CommonRepositoryPlugin.PLUGIN_ID);
        refresh(NULL_PROGRESS_MONITOR);
        try {
            if (!project.isOpen()) {
                project.open(NULL_PROGRESS_MONITOR);
            }
            project.delete(true, true, NULL_PROGRESS_MONITOR);
            if (CommonRepositoryPlugin.getCurrentRepository().equals(getName())) {
                RepositoryManager.getInstance().setRepository(RepositoryPreferenceConstant.DEFAULT_REPOSITORY_NAME);
            }
        } catch (final CoreException e) {
            BonitaStudioLog.error(e);
        }
    }

    @Override
    public <T> T getRepositoryStore(final Class<T> repositoryStoreClass) {
        if (stores == null || stores.isEmpty()) {
            initRepositoryStores(false);
            enableBuild();
        }
        return repositoryStoreClass.cast(stores.get(repositoryStoreClass));
    }

    @SuppressWarnings("restriction")
    @Override
    public String getVersion() {
        if (project.isOpen()) {
            try {
                return project.getDescription().getComment();
            } catch (final CoreException e) {
                BonitaStudioLog.error(e);
            }
        } else {
            final File projectFile = new File(project.getLocation().toFile(), ".project");
            if (projectFile.exists()) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(projectFile);
                    final InputSource source = new InputSource(fis);
                    final ProjectDescriptionReader reader = new ProjectDescriptionReader();
                    final IProjectDescription desc = reader.read(source);
                    return desc.getComment();
                } catch (final FileNotFoundException e) {
                    BonitaStudioLog.error(e);
                } finally {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (final IOException e) {
                            BonitaStudioLog.error(e);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public synchronized List<IRepositoryStore<? extends IRepositoryFileStore>> getAllStores() {
        if (stores == null) {
            initRepositoryStores(false);
            enableBuild();
        }
        final List<IRepositoryStore<? extends IRepositoryFileStore>> result = new ArrayList<IRepositoryStore<? extends IRepositoryFileStore>>(stores.values());
        Collections.sort(result, new RepositoryStoreComparator());
        return result;
    }

    @Override
    public List<IRepositoryStore<? extends IRepositoryFileStore>> getAllSharedStores() {
        final List<IRepositoryStore<? extends IRepositoryFileStore>> result = new ArrayList<IRepositoryStore<? extends IRepositoryFileStore>>();
        for (final IRepositoryStore<? extends IRepositoryFileStore> sotre : getAllStores()) {
            if (sotre.isShared()) {
                result.add(sotre);
            }
        }
        Collections.sort(result, new RepositoryStoreComparator());
        return result;
    }

    @Override
    public String getDisplayName() {
        return getName() + " [" + getVersion() + "]";
    }

    @Override
    public Image getIcon() {
        if (isShared()) {
            return Pics.getImage("shared-repository.png", CommonRepositoryPlugin.getDefault());
        } else {
            return Pics.getImage("local-repository.png", CommonRepositoryPlugin.getDefault());
        }
    }

    @Override
    public void importFromArchive(final File archiveFile, final boolean askOverwrite, final boolean validateAfterImport) {
        final boolean disableConfirmation = FileActionDialog.getDisablePopup();
        FileActionDialog.setDisablePopup(!askOverwrite);
        final ImportBosArchiveOperation operation = new ImportBosArchiveOperation(validateAfterImport);
        operation.setArchiveFile(archiveFile.getAbsolutePath());
        operation.setCurrentRepository(RepositoryManager.getInstance().getCurrentRepository());
        operation.run(NULL_PROGRESS_MONITOR);
        FileActionDialog.setDisablePopup(disableConfirmation);
    }

    @Override
    public void exportToArchive(final String fileName) {
        final ExportBosArchiveOperation operation = new ExportBosArchiveOperation();
        operation.setDestinationPath(fileName);
        final Set<IResource> allResources = new HashSet<IResource>();
        for (final IRepositoryStore<?> store : getAllExportableStores()) {
            allResources.add(store.getResource());
        }
        operation.setResources(allResources);
        final IStatus status = operation.run(NULL_PROGRESS_MONITOR);
        if (!status.isOK()) {
            logErrorStatus(status);
        }
    }

    protected void logErrorStatus(final IStatus status) {
        final StringBuilder sb = new StringBuilder();
        if (status.isMultiStatus()) {
            for (final IStatus childStatus : status.getChildren()) {
                sb.append(childStatus.getMessage()).append("\n");
            }

        }
        BonitaStudioLog.error("Export to archive failed.\n" + status.getMessage() + "\n" + sb.toString(), CommonRepositoryPlugin.PLUGIN_ID);
    }

    @Override
    public IRepositoryFileStore getFileStore(final IResource resource) {
        for (final IRepositoryStore<? extends IRepositoryFileStore> store : getAllStores()) {
            final IRepositoryFileStore file = store.getChild(resource.getName());
            if (file != null) {
                return file;
            }
        }
        return null;
    }

    @Override
    public IRepositoryStore<? extends IRepositoryFileStore> getRepositoryStore(final IResource resource) {
        for (final IRepositoryStore<? extends IRepositoryFileStore> store : getAllStores()) {
            if (resource instanceof IFile) {
                final IResource storeResource = store.getResource();
                IResource parent = resource.getParent();
                while (parent != null && !storeResource.equals(parent)) {
                    parent = parent.getParent();
                }
                if (parent != null) {
                    return store;
                }
            } else {
                if (store.getResource() != null && store.getResource().equals(resource)) {
                    return store;
                }
            }
        }
        return null;
    }

    @Override
    public void notifyFileStoreEvent(final FileStoreChangeEvent event) {
        // NO BEHAVIOR
    }

    @Override
    public List<IRepositoryStore<? extends IRepositoryFileStore>> getAllShareableStores() {
        final List<IRepositoryStore<? extends IRepositoryFileStore>> result = new ArrayList<IRepositoryStore<? extends IRepositoryFileStore>>();
        for (final IRepositoryStore<? extends IRepositoryFileStore> store : getAllStores()) {
            if (store.canBeShared()) {
                result.add(store);
            }
        }
        Collections.sort(result, new RepositoryStoreComparator());
        return result;

    }

    @Override
    public URLClassLoader createProjectClassloader() {
        final List<URL> jars = new ArrayList<URL>();
        try {
            if (!classpathExists()) {
                refreshClasspath(getProject());
            }

            // Synchronize with build jobs
            Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, NULL_PROGRESS_MONITOR);
            Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, NULL_PROGRESS_MONITOR);

            final IProject project = getProject();
            final String workspacePath = project.getLocation().toFile().getParent();
            final String outputPath = workspacePath + getJavaProject().getOutputLocation().toString();
            jars.add(new File(outputPath).toURI().toURL());
            for (final IClasspathEntry entry : getJavaProject().getRawClasspath()) {
                if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                    File jar = entry.getPath().toFile();
                    if (!jar.exists()) { // jar location relative to project
                        jar = new File(workspacePath + File.separator + jar);
                    }
                    jars.add(jar.toURI().toURL());
                }
            }
        } catch (final Exception e) {
            BonitaStudioLog.error(e);
        }

        return new NonLockingJarFileClassLoader(getName() + "_URLClassLoader", jars.toArray(new URL[jars.size()]), BusinessArchive.class.getClassLoader());
    }

    @Override
    public List<IRepositoryStore<? extends IRepositoryFileStore>> getAllExportableStores() {
        final List<IRepositoryStore<? extends IRepositoryFileStore>> result = new ArrayList<IRepositoryStore<? extends IRepositoryFileStore>>();
        for (final IRepositoryStore<? extends IRepositoryFileStore> store : getAllStores()) {
            if (store.canBeExported()) {
                result.add(store);
            }
        }
        Collections.sort(result, new RepositoryStoreComparator());
        return result;
    }

    @Override
    public IRepositoryFileStore asRepositoryFileStore(final IFile res) {
        final IPath projectRelativePath = res.getProjectRelativePath();
        if (projectRelativePath.segmentCount() > 0) {
            final IPath path = projectRelativePath.removeFirstSegments(0);
            if (path.segmentCount() > 1) {
                final String repoName = path.segments()[0];
                final IFile file = getProject().getFile(path);
                if (file.exists()) {
                    for (final IRepositoryStore<?> store : getAllStores()) {
                        if (belongToRepositoryStore(store, file)) {
                            final IRepositoryFileStore fileStore = store.createRepositoryFileStore(file.getName());
                            if (fileStore != null && fileStore.getParentStore().getName().startsWith(repoName)) {
                                return fileStore;
                            }
                        }
                    }
                }
            }

        }
        return null;
    }

    private boolean belongToRepositoryStore(final IRepositoryStore<?> store, final IFile file) {
        final IFolder parentFolder = store.getResource();
        if (parentFolder == null) {
            return false;
        }
        IContainer current = file.getParent();
        while (current != null && !parentFolder.equals(current)) {
            current = current.getParent();
        }
        return current != null && parentFolder.equals(current);
    }

    @Override
    public void migrate() throws CoreException, MigrationException {
        Assert.isNotNull(project);
        for (final IRepositoryStore<?> store : getAllStores()) {
            store.migrate();
        }
        createProjectDescriptor(project);
        final IFile classpathFile = project.getFile(".classpath");
        if (classpathFile.exists()) {
            classpathFile.delete(true, false, NULL_PROGRESS_MONITOR);
        }
        refreshClasspath(project);
    }

    public void setProgressMonitor(final IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    protected void monitorWorked(final int work) {
        if (monitor != null) {
            monitor.worked(work);
        }
    }

    protected void monitorSubtask(final String subtask) {
        if (monitor != null && subtask != null) {
            monitor.subTask(subtask);
        }
    }

    @Override
    public boolean isOnline() {
        return true;
    }

}
