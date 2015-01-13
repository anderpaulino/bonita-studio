/**
 * Copyright (C) 2014 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
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
package org.bonitasoft.studio.actors.action;

import java.io.FileNotFoundException;

import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.studio.actors.operation.PublishOrganizationOperation;
import org.bonitasoft.studio.actors.repository.OrganizationFileStore;
import org.bonitasoft.studio.actors.repository.OrganizationRepositoryStore;
import org.bonitasoft.studio.common.extension.IEngineAction;
import org.bonitasoft.studio.common.repository.Repository;
import org.bonitasoft.studio.common.repository.RepositoryManager;
import org.bonitasoft.studio.preferences.BonitaPreferenceConstants;
import org.bonitasoft.studio.preferences.BonitaStudioPreferencesPlugin;

/**
 * @author Romain Bioteau
 */
public class PublishActiveOrganizationAction implements IEngineAction {

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.common.extension.IEngineAction#run(org.bonitasoft.engine.session.APISession)
     */
    @Override
    public void run(APISession session) throws Exception {
        if (BonitaStudioPreferencesPlugin.getDefault().getPreferenceStore().getBoolean(BonitaPreferenceConstants.LOAD_ORGANIZATION)) {
            String artifactId = BonitaStudioPreferencesPlugin.getDefault().getPreferenceStore().getString(BonitaPreferenceConstants.DEFAULT_ORGANIZATION) + "."
                    + OrganizationRepositoryStore.ORGANIZATION_EXT;
            OrganizationRepositoryStore store = RepositoryManager.getInstance().getRepositoryStore(OrganizationRepositoryStore.class);
            OrganizationFileStore organizationFileStore = store.getChild(artifactId);
            if (organizationFileStore == null) {
                throw new FileNotFoundException(artifactId);
            }
            PublishOrganizationOperation op = new PublishOrganizationOperation(organizationFileStore.getContent());
            op.setSession(session);
            op.run(Repository.NULL_PROGRESS_MONITOR);
        }
    }

}
