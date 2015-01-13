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
package org.bonitasoft.studio.validators.repository;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bonitasoft.studio.common.log.BonitaStudioLog;
import org.bonitasoft.studio.common.repository.model.IRepositoryFileStore;
import org.bonitasoft.studio.common.repository.store.AbstractEMFRepositoryStore;
import org.bonitasoft.studio.pics.Pics;
import org.bonitasoft.studio.validators.ValidatorPlugin;
import org.bonitasoft.studio.validators.descriptor.validator.ValidatorDescriptor;
import org.bonitasoft.studio.validators.descriptor.validator.util.ValidatorAdapterFactory;
import org.bonitasoft.studio.validators.i18n.Messages;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.swt.graphics.Image;

/**
 * @author Romain Bioteau
 */
public class ValidatorDescriptorRepositoryStore extends AbstractEMFRepositoryStore {

    private static final String STORE_NAME = "validators";
    private static final Set<String> extensions = new HashSet<String>();
    public static final String VALIDATOR_EXT = "validator";
    static {
        extensions.add(VALIDATOR_EXT);
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.common.repository.model.IRepositoryStore#createRepositoryFileStore(java.lang.String)
     */
    @Override
    public IRepositoryFileStore createRepositoryFileStore(String fileName) {
        return new ValidatorDescriptorFileStore(fileName, this);
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.common.repository.model.IRepositoryStore#getName()
     */
    @Override
    public String getName() {
        return STORE_NAME;
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.common.repository.model.IRepositoryStore#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return Messages.validators;
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.common.repository.model.IRepositoryStore#getIcon()
     */
    @Override
    public Image getIcon() {
        return Pics.getImage("Validator.png", ValidatorPlugin.getDefault());
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.common.repository.model.IRepositoryStore#getCompatibleExtensions()
     */
    @Override
    public Set<String> getCompatibleExtensions() {
        return extensions;
    }

    public ValidatorDescriptor getValidatorDescriptor(String classname) {
        for (ValidatorDescriptor validator : getValidatorDescriptors()) {
            final String classNameToCheck = validator.getClassName();
            if (classNameToCheck != null) {
                if (classNameToCheck.equals(classname)) {
                    return validator;
                }
            } else {
                BonitaStudioLog.info("A validator descriptor have a null classname: " + validator, ValidatorPlugin.PLUGIN_ID);
            }
        }
        return null;
    }

    public List<ValidatorDescriptor> getValidatorDescriptors() {
        List<ValidatorDescriptor> result = new ArrayList<ValidatorDescriptor>();
        for (IRepositoryFileStore fileStore : getChildren()) {
            ValidatorDescriptor def = (ValidatorDescriptor) fileStore.getContent();
            result.add(def);
        }
        return result;
    }

    @Override
    public IRepositoryFileStore getChild(String fileName) {
        IRepositoryFileStore file = super.getChild(fileName);
        if (file == null) {
            URL url = ValidatorPlugin.getDefault().getBundle().getResource(STORE_NAME + "/" + fileName);
            if (url != null) {
                return new URLValidatorDescriptorFileStore(url, this);
            } else {
                return null;
            }
        } else {
            return file;
        }

    }

    @Override
    public List<IRepositoryFileStore> getChildren() {
        List<IRepositoryFileStore> result = super.getChildren();
        Enumeration<URL> connectorImplementations = ValidatorPlugin.getDefault().getBundle().findEntries(STORE_NAME, "*." + VALIDATOR_EXT, false);
        if (connectorImplementations != null) {
            while (connectorImplementations.hasMoreElements()) {
                URL url = connectorImplementations.nextElement();
                String[] segments = url.getFile().split("/");
                String fileName = segments[segments.length - 1];
                if (fileName.lastIndexOf(".") != -1) {
                    String extension = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
                    if (extensions.contains(extension)) {
                        result.add(new URLValidatorDescriptorFileStore(url, this));
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected void addAdapterFactory(ComposedAdapterFactory adapterFactory) {
        adapterFactory.addAdapterFactory(new ValidatorAdapterFactory());
    }

}
