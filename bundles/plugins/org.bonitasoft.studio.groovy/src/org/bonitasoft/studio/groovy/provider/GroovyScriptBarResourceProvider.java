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
package org.bonitasoft.studio.groovy.provider;

import static com.google.common.collect.Iterables.toArray;
import static com.google.common.io.Files.toByteArray;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.bonitasoft.engine.bpm.bar.BarResource;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder;
import org.bonitasoft.studio.common.FragmentTypes;
import org.bonitasoft.studio.common.ProjectUtil;
import org.bonitasoft.studio.common.emf.tools.ModelHelper;
import org.bonitasoft.studio.common.extension.BARResourcesProvider;
import org.bonitasoft.studio.common.repository.Repository;
import org.bonitasoft.studio.common.repository.RepositoryAccessor;
import org.bonitasoft.studio.common.repository.jdt.CreateJarOperation;
import org.bonitasoft.studio.groovy.repository.GroovyFileStore;
import org.bonitasoft.studio.groovy.repository.GroovyRepositoryStore;
import org.bonitasoft.studio.groovy.repository.ProvidedGroovyRepositoryStore;
import org.bonitasoft.studio.model.configuration.Configuration;
import org.bonitasoft.studio.model.configuration.ConfigurationPackage;
import org.bonitasoft.studio.model.configuration.Fragment;
import org.bonitasoft.studio.model.configuration.FragmentContainer;
import org.bonitasoft.studio.model.process.AbstractProcess;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.core.ICompilationUnit;

/**
 * @author Romain Bioteau
 */
public class GroovyScriptBarResourceProvider implements BARResourcesProvider {

    private static final String EXTERNAL_LIB_BAR_LOCATION = BARResourcesProvider.FORMS_FOLDER_IN_BAR + "/lib/";

    @Inject
    private RepositoryAccessor repositoryAccessor;

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.common.extension.BARResourcesProvider#getResourcesForConfiguration(org.bonitasoft.studio.model.process.AbstractProcess,
     * org.bonitasoft.studio.model.configuration.Configuration, org.bonitasoft.engine.bpm.model.DesignProcessDefinition, java.util.Map)
     */
    @Override
    public void addResourcesForConfiguration(final BusinessArchiveBuilder builder, final AbstractProcess process, final Configuration configuration,
            final Set<EObject> exludedObject) throws Exception {
        if (configuration != null) {
            addGroovyScriptDependenciesToClasspath(builder, configuration, configuration.getProcessDependencies());
            addGroovyScriptDependenciesToExternalLib(builder, configuration, configuration.getApplicationDependencies());
        }
        addProvidedScriptsToClasspath(builder);
    }

    private void addProvidedScriptsToClasspath(final BusinessArchiveBuilder builder) throws InvocationTargetException, InterruptedException, IOException {
        final Set<ICompilationUnit> compilationUnits = new HashSet<ICompilationUnit>();
        final ProvidedGroovyRepositoryStore providedStore = repositoryAccessor.getRepositoryStore(ProvidedGroovyRepositoryStore.class);
        for (final GroovyFileStore file : providedStore.getChildren()) {
            compilationUnits.add(file.getCompilationUnit());
        }
        if (!compilationUnits.isEmpty()) {
            final File targetJar = new File(ProjectUtil.getBonitaStudioWorkFolder(), ProvidedGroovyRepositoryStore.EXPORTED_PROVIDED_JAR_NAME);
            final CreateJarOperation createJarOperation = new CreateJarOperation(targetJar, toArray(compilationUnits, ICompilationUnit.class));
            createJarOperation.run(Repository.NULL_PROGRESS_MONITOR);
            if (createJarOperation.getStatus().isOK()) {
                builder.addClasspathResource(new BarResource(targetJar.getName(), toByteArray(targetJar)));
            }
            targetJar.delete();
        }
    }

    protected void addGroovyScriptDependenciesToClasspath(
            final BusinessArchiveBuilder builder,
            final Configuration configuration,
            final List<FragmentContainer> containers) throws InvocationTargetException, InterruptedException, IOException {
        final ICompilationUnit[] compilationUnits = collectCompilationUnits(configuration, containers);
        if (compilationUnits.length > 0) {
            final File targetJar = new File(ProjectUtil.getBonitaStudioWorkFolder(), GroovyRepositoryStore.EXPORTED_JAR_NAME);
            final CreateJarOperation createJarOperation = new CreateJarOperation(targetJar, compilationUnits);
            createJarOperation.run(Repository.NULL_PROGRESS_MONITOR);
            if (createJarOperation.getStatus().isOK()) {
                builder.addClasspathResource(new BarResource(targetJar.getName(), toByteArray(targetJar)));
            }
            targetJar.delete();
        }
    }

    protected void addGroovyScriptDependenciesToExternalLib(
            final BusinessArchiveBuilder builder,
            final Configuration configuration,
            final List<FragmentContainer> containers) throws InvocationTargetException, InterruptedException, IOException {
        final ICompilationUnit[] compilationUnits = collectCompilationUnits(configuration, containers);
        if (compilationUnits.length > 0) {
            final File targetJar = new File(ProjectUtil.getBonitaStudioWorkFolder(), GroovyRepositoryStore.EXPORTED_JAR_NAME);
            final CreateJarOperation createJarOperation = new CreateJarOperation(targetJar, compilationUnits);
            createJarOperation.run(Repository.NULL_PROGRESS_MONITOR);
            if (createJarOperation.getStatus().isOK()) {
                builder.addExternalResource(new BarResource(EXTERNAL_LIB_BAR_LOCATION + targetJar.getName(), toByteArray(targetJar)));
            }
            targetJar.delete();
        }
    }

    private ICompilationUnit[] collectCompilationUnits(final Configuration configuration, final List<FragmentContainer> containers) {
        final Set<ICompilationUnit> result = new HashSet<ICompilationUnit>();
        if (configuration != null) {
            final GroovyRepositoryStore store = repositoryAccessor.getRepositoryStore(GroovyRepositoryStore.class);
            for (final FragmentContainer fc : containers) {
                for (final EObject fragment : ModelHelper.getAllItemsOfType(fc, ConfigurationPackage.Literals.FRAGMENT)) {
                    if (((Fragment) fragment).getType().equals(FragmentTypes.GROOVY_SCRIPT)) {
                        if (((Fragment) fragment).isExported()) {
                            final GroovyFileStore file = store.getChild(((Fragment) fragment).getValue());
                            if (file != null) {
                                result.add(file.getCompilationUnit());
                            }
                        }
                    }
                }
            }
        }
        return toArray(result, ICompilationUnit.class);
    }

}
