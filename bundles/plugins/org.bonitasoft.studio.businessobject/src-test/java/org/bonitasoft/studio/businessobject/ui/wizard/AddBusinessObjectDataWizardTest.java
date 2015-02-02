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
package org.bonitasoft.studio.businessobject.ui.wizard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.bonitasoft.studio.businessobject.core.repository.BusinessObjectModelRepositoryStore;
import org.bonitasoft.studio.businessobject.i18n.Messages;
import org.bonitasoft.studio.common.DataTypeLabels;
import org.bonitasoft.studio.model.process.AbstractProcess;
import org.bonitasoft.studio.model.process.DataType;
import org.bonitasoft.studio.model.process.MainProcess;
import org.bonitasoft.studio.model.process.ProcessFactory;
import org.bonitasoft.studio.swt.AbstractSWTTestCase;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Romain Bioteau
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AddBusinessObjectDataWizardTest extends AbstractSWTTestCase {

    private AddBusinessObjectDataWizard wizardUnderTest;

    private AbstractProcess container;

    private TransactionalEditingDomain editingDomain;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        createDisplayAndRealm();
        final BusinessObjectModelRepositoryStore store = mock(BusinessObjectModelRepositoryStore.class);
        final MainProcess diagram = ProcessFactory.eINSTANCE.createMainProcess();
        container = ProcessFactory.eINSTANCE.createPool();
        container.setName("Test Process");
        diagram.getElements().add(container);
        final DataType bType = ProcessFactory.eINSTANCE.createBusinessObjectType();
        bType.setName(DataTypeLabels.businessObjectType);
        diagram.getDatatypes().add(bType);
        final Resource r = new ResourceImpl(URI.createFileURI("test.proc"));
        r.getContents().add(diagram);
        final ResourceSet rSet = new ResourceSetImpl();
        rSet.getResources().add(r);
        editingDomain = TransactionalEditingDomain.Factory.INSTANCE.createEditingDomain(rSet);
        wizardUnderTest = new AddBusinessObjectDataWizard(container, store, editingDomain);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        dispose();
    }

    @Test
    public void shouldAddPages_AddAddBusinessObjectDataWizardPage() throws Exception {
        assertThat(wizardUnderTest.getPages()).isEmpty();
        wizardUnderTest.addPages();
        assertThat(wizardUnderTest.getPages()).hasSize(1);
        assertThat(wizardUnderTest.getPage(BusinessObjectDataWizardPage.class.getName())).isNotNull();
    }

    @Test
    public void shouldCreateAddBusinessObjectDataWizardPage_SetPageTitleAndDescription() throws Exception {
        assertThat(wizardUnderTest.getPages()).isEmpty();
        final BusinessObjectDataWizardPage page = wizardUnderTest.createAddBusinessObjectDataWizardPage();
        assertThat(page.getTitle()).isNotNull().isEqualTo(Messages.bind(Messages.addBusinessObjectDataTitle, container.getName()));
        assertThat(page.getDescription()).isNotNull().isEqualTo(Messages.addBusinessObjectDataDescription);
    }

    @Test
    public void shouldPerformFinish_ReturnTrueIfABusinessObjectData_HasBeenAddedToContainer() throws Exception {
        wizardUnderTest.addPages();
        assertThat(wizardUnderTest.performFinish()).isTrue();
        assertThat(container.getData()).isNotNull().hasSize(1);
    }

}