/**
 * Copyright (C) 2009 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.studio.properties.sections.transientdata;

import java.util.Set;

import org.bonitasoft.studio.data.ui.property.section.AbstractDataSection;
import org.bonitasoft.studio.properties.i18n.Messages;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 *
 * @author Baptiste Mesta
 */
public class ApplicationTransientDataSection extends AbstractDataSection {

	/* (non-Javadoc)
	 * @see org.bonitasoft.studio.properties.sections.data.DataSection#createPromoteDataButton(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Button createMoveDataButton(final Composite parent) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.bonitasoft.studio.properties.sections.data.DataSection#createLabel(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createLabel(final Composite dataComposite) {
		final Label label = getWidgetFactory().createLabel(dataComposite, Messages.pageFlowTransientData);
		label.setLayoutData(GridDataFactory.swtDefaults().span(2, 1).create());
	}

	@Override
    protected void createBusinessData() {

    }

    @Override
	protected Set<EStructuralFeature> getDataFeatureToCheckUniqueID() {
		final Set<EStructuralFeature> dataFeatureToCheckUniqueID = super.getDataFeatureToCheckUniqueID();
		dataFeatureToCheckUniqueID.add(getDataFeature());
		return dataFeatureToCheckUniqueID;
	}

	@Override
    protected boolean getShowAutoGenerateForm() {
		return false;
	}

	@Override
	public String getSectionDescription() {
		return null;
	}

	@Override
	public boolean isPageFlowContext() {
		return true;
	}

}
