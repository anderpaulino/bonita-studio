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
package org.bonitasoft.studio.properties.sections.forms.wizard.editingsupport;

import static org.fest.assertions.Assertions.assertThat;

import org.bonitasoft.studio.diagram.form.custom.model.WidgetMapping;
import org.bonitasoft.studio.model.form.FormFactory;
import org.bonitasoft.studio.model.process.Data;
import org.bonitasoft.studio.model.process.ProcessFactory;
import org.eclipse.jface.viewers.ColumnViewer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Romain Bioteau
 */
@RunWith(MockitoJUnitRunner.class)
public class MandatoryEditingSupportTest {

    private MandatoryEditingSupport mandatoryEditingSupport;
    @Mock
    private ColumnViewer columViewer;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        mandatoryEditingSupport = new MandatoryEditingSupport(columViewer);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void shouldCanEdit_ReturnTrue() throws Exception {
        Data data = ProcessFactory.eINSTANCE.createData();
        data.setDataType(ProcessFactory.eINSTANCE.createStringType());
        WidgetMapping widgetMapping = new WidgetMapping(data);
        assertThat(mandatoryEditingSupport.canEdit(widgetMapping)).isTrue();
    }

    @Test
    public void shouldCanEdit_ReturnFalse() throws Exception {
        Data data = ProcessFactory.eINSTANCE.createData();
        data.setDataType(ProcessFactory.eINSTANCE.createStringType());
        WidgetMapping widgetMapping = new WidgetMapping(data);
        widgetMapping.setWidgetType(FormFactory.eINSTANCE.createTextInfo());
        assertThat(mandatoryEditingSupport.canEdit(widgetMapping)).isFalse();
        assertThat(mandatoryEditingSupport.canEdit(widgetMapping.getWidgetType())).isFalse();
    }

}
