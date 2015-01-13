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
package org.bonitasoft.studio.migration.custom.migration.form;

import org.bonitasoft.studio.model.form.FileWidgetDownloadType;
import org.bonitasoft.studio.model.form.FileWidgetInputType;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.edapt.migration.CustomMigration;
import org.eclipse.emf.edapt.migration.Instance;
import org.eclipse.emf.edapt.migration.Metamodel;
import org.eclipse.emf.edapt.migration.MigrationException;
import org.eclipse.emf.edapt.migration.Model;

/**
 * @author aurelie
 */
public class FileWidgetResourceMigration extends CustomMigration {

    /*
     * (non-Javadoc)
     * @see org.eclipse.emf.edapt.migration.CustomMigration#migrateAfter(org.eclipse.emf.edapt.migration.Model, org.eclipse.emf.edapt.migration.Metamodel)
     */
    @Override
    public void migrateAfter(final Model model, final Metamodel metamodel) throws MigrationException {
        super.migrateAfter(model, metamodel);
        final EEnum downloadTypeEnum = metamodel.getEEnum("form.FileWidgetDownloadType");
        final EList<Instance> fileWidgets = model.getAllInstances("form.FileWidget");
        for (final Instance fileWidget : fileWidgets) {

            if (fileWidget.get("inputType") != null) {
                final EEnumLiteral literal = fileWidget.get("inputType");
                if (literal.getLiteral().equals(FileWidgetInputType.RESOURCE.getLiteral())) {
                    fileWidget.set("downloadType", downloadTypeEnum.getEEnumLiteral(FileWidgetDownloadType.BROWSE_VALUE));
                }
            }
        }

    }
}
