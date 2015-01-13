/**
 * Copyright (C) 2012 BonitaSoft S.A.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.edapt.migration.CustomMigration;
import org.eclipse.emf.edapt.migration.Instance;
import org.eclipse.emf.edapt.migration.Metamodel;
import org.eclipse.emf.edapt.migration.MigrationException;
import org.eclipse.emf.edapt.migration.Model;

/**
 * @author Romain Bioteau
 */
public class LabelPositionMigration extends CustomMigration {

    private List<String> widgets = new ArrayList<String>();

    @Override
    public void migrateBefore(Model model, Metamodel metamodel)
            throws MigrationException {
        for (Instance widget : model.getAllInstances("form.Widget")) {
            Object position = widget.get("labelPosition");
            if (position == null) {
                widgets.add(widget.getUuid());
            }
        }
    }

    @Override
    public void migrateAfter(Model model, Metamodel metamodel)
            throws MigrationException {
        for (Instance widget : model.getAllInstances("form.Widget")) {
            if (widgets.contains(widget.getUuid())) {
                widget.set("labelPosition", model.getMetamodel().getEEnumLiteral("form.LabelPosition.Left"));
            }
        }
    }

}
