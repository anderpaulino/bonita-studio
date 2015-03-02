/**
 * Copyright (C) 2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.studio.businessobject.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.bonitasoft.studio.businessobject.core.repository.BusinessObjectModelFileStore;
import org.bonitasoft.studio.businessobject.core.repository.BusinessObjectModelRepositoryStore;
import org.bonitasoft.studio.common.DataTypeLabels;
import org.bonitasoft.studio.model.process.BusinessObjectData;
import org.bonitasoft.studio.model.process.BusinessObjectType;
import org.bonitasoft.studio.model.process.ProcessFactory;
import org.bonitasoft.studio.swt.AbstractSWTTestCase;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Romain Bioteau
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class BusinessObjectDataStyledLabelProviderTest extends AbstractSWTTestCase {

    @Mock
    private BusinessObjectModelRepositoryStore store;

    @Mock
    private BusinessObjectModelFileStore fStore;

    @Mock
    private ViewerCell cell;

    @Mock
    private ColorRegistry cr;

    private BusinessObjectDataStyledLabelProvider labelProviderUnderTest;

    private BusinessObjectData data;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        createDisplayAndRealm();
        labelProviderUnderTest = new BusinessObjectDataStyledLabelProvider(store);
        final BusinessObjectType businessObjectType = ProcessFactory.eINSTANCE.createBusinessObjectType();
        businessObjectType.setName(DataTypeLabels.businessObjectType);
        data = ProcessFactory.eINSTANCE.createBusinessObjectData();
        data.setName("currentLeaveRequest");
        data.setDataType(businessObjectType);
        data.setClassName("org.bonitasoft.hr.LeaveRequest");
        when(cell.getElement()).thenReturn(data);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        dispose();
    }

    @Test
    public void shouldCreateStrikethroughStyleStrikethroughInitialText() throws Exception {
        final String initialText = "myData -- hr.LeaveRequest";
        final StyledString ss = labelProviderUnderTest.createStrikethroughStyle(initialText, "hr.LeaveRequest");
        assertThat(ss.getStyleRanges()).isNotEmpty().hasSize(1);
        final StyleRange styleRange = ss.getStyleRanges()[0];
        assertThat(styleRange.strikeout).isTrue();
        assertThat(styleRange.length).isEqualTo(initialText.length());
    }

    @Test
    public void shouldBusinessObjectDefinitionExists_ReturnTrue() throws Exception {
        doReturn(fStore).when(store).getChildByQualifiedName("org.bonitasoft.hr.LeaveRequest");
        assertThat(labelProviderUnderTest.businessObjectDefinitionExists(data)).isTrue();
    }

    @Test
    public void shouldBusinessObjectDefinitionExists_ReturnFalse() throws Exception {
        doReturn(fStore).when(store).getChildByQualifiedName("org.bonitasoft.hr.LeaveRequest2");
        assertThat(labelProviderUnderTest.businessObjectDefinitionExists(data)).isFalse();
    }

    @Test
    public void shouldUpdateCell_SetStyledString() throws Exception {
        when(cell.getText()).thenReturn("myData -- hr.LeaveRequest");
        labelProviderUnderTest.update(cell);
    }

}
