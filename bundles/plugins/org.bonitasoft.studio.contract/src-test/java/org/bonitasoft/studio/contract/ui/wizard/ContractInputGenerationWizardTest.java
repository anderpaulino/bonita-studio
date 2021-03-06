/**
 * Copyright (C) 2015 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
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
package org.bonitasoft.studio.contract.ui.wizard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bonitasoft.studio.model.process.builders.BusinessObjectDataBuilder.aBusinessData;
import static org.bonitasoft.studio.model.process.builders.ContractBuilder.aContract;
import static org.bonitasoft.studio.model.process.builders.PoolBuilder.aPool;
import static org.mockito.Mockito.when;

import org.bonitasoft.studio.businessobject.core.repository.BusinessObjectModelRepositoryStore;
import org.bonitasoft.studio.model.businessObject.BusinessObjectBuilder;
import org.bonitasoft.studio.model.businessObject.FieldBuilder.SimpleFieldBuilder;
import org.bonitasoft.studio.model.process.BusinessObjectData;
import org.bonitasoft.studio.model.process.Pool;
import org.bonitasoft.studio.model.process.provider.ProcessItemProviderAdapterFactory;
import org.bonitasoft.studio.swt.rules.RealmWithDisplay;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.transaction.impl.TransactionalEditingDomainImpl;
import org.eclipse.jface.wizard.IWizardContainer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ContractInputGenerationWizardTest {

    @Rule
    public RealmWithDisplay realmWithDisplay = new RealmWithDisplay();

    @Mock
    private BusinessObjectModelRepositoryStore store;

    @Test
    public void should_first_wizard_page_be_selectBusinessDataWizardPage() {
        final BusinessObjectData data = aBusinessData().build();
        final Pool process = aPool().build();
        process.getData().add(data);

        final ContractInputGenerationWizard wizard = new ContractInputGenerationWizard(process, editingDomain(), store);
        wizard.addPages();

        assertThat(wizard.getPages()[0]).isInstanceOf(SelectBusinessDataWizardPage.class);
    }

    @Test
    public void should_first_wizard_page_be_CreateContractInputFromBusinessObjectWizardPage() {
        final BusinessObjectData data = aBusinessData().build();
        final Pool process = aPool().build();
        process.getData().add(data);

        final ContractInputGenerationWizard wizard = new ContractInputGenerationWizard(process, editingDomain(), store);
        wizard.addPages();

        assertThat(wizard.getPages()[0]).isInstanceOf(SelectBusinessDataWizardPage.class);
    }

    @Test
    public void should_add_a_contract_input_with_selected_mappings_on_finish() throws Exception {
        final BusinessObjectData data = aBusinessData().withName("employee").withClassname("org.company.Employee").build();
        final Pool process = aPool().havingContract(aContract()).build();
        process.getData().add(data);
        when(store.getBusinessObjectByQualifiedName("org.company.Employee")).thenReturn(
                BusinessObjectBuilder.aBO("org.company.Employee").withField(SimpleFieldBuilder.aStringField("firstName").build()).build());

        final ContractInputGenerationWizard wizard = new ContractInputGenerationWizard(process, editingDomain(), store);
        wizard.addPages();
        final IWizardContainer wizardContainer = Mockito.mock(IWizardContainer.class);
        when(wizardContainer.getShell()).thenReturn(realmWithDisplay.getShell());
        wizard.setContainer(wizardContainer);
        wizard.createPageControls(realmWithDisplay.createComposite());
        wizard.performFinish();

        assertThat(process.getContract().getInputs()).extracting("name").contains("employeeEmployee");
        assertThat(process.getContract().getInputs().get(0).getInputs()).extracting("name").contains("firstName");
    }

    @Test
    public void should_canFinish_return_false_when_no_data_is_defined() {
        final Pool process = aPool().havingContract(aContract()).build();
        final ContractInputGenerationWizard wizard = new ContractInputGenerationWizard(process, editingDomain(), store);
        wizard.addPages();
        final IWizardContainer wizardContainer = Mockito.mock(IWizardContainer.class);
        when(wizardContainer.getShell()).thenReturn(realmWithDisplay.getShell());
        wizard.setContainer(wizardContainer);
        wizard.createPageControls(realmWithDisplay.createComposite());
        assertThat(wizard.canFinish()).isFalse();
    }

    @Test
    public void should_canFinish_return_true_when_data_is_selected() {
        final Pool process = aPool().havingContract(aContract()).build();
        final BusinessObjectData data = aBusinessData().withClassname("com.company.Employee").build();
        process.getData().add(data);
        Mockito.doReturn(BusinessObjectBuilder.aBO("com.company.Employee").withField(SimpleFieldBuilder.aTextField("name").build()).build()).when(store)
                .getBusinessObjectByQualifiedName("com.company.Employee");
        final ContractInputGenerationWizard wizard = new ContractInputGenerationWizard(process, editingDomain(), store);
        wizard.addPages();
        final IWizardContainer wizardContainer = Mockito.mock(IWizardContainer.class);
        when(wizardContainer.getShell()).thenReturn(realmWithDisplay.getShell());
        wizard.setContainer(wizardContainer);
        wizard.createPageControls(realmWithDisplay.createComposite());
        assertThat(wizard.canFinish()).isTrue();
    }

    private EditingDomain editingDomain() {
        return new TransactionalEditingDomainImpl(new ProcessItemProviderAdapterFactory());
    }

}
