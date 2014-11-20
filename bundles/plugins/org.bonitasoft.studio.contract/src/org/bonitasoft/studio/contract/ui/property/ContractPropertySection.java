/**
 * Copyright (C) 2014 BonitaSoft S.A.
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
package org.bonitasoft.studio.contract.ui.property;

import org.bonitasoft.studio.common.databinding.CustomEMFEditObservables;
import org.bonitasoft.studio.common.properties.EObjectSelectionProviderSection;
import org.bonitasoft.studio.contract.core.validation.ContractDefinitionValidator;
import org.bonitasoft.studio.contract.i18n.Messages;
import org.bonitasoft.studio.contract.ui.property.constraint.ContractConstraintController;
import org.bonitasoft.studio.contract.ui.property.constraint.ContractConstraintsTableViewer;
import org.bonitasoft.studio.contract.ui.property.input.ContractInputController;
import org.bonitasoft.studio.contract.ui.property.input.ContractInputTreeViewer;
import org.bonitasoft.studio.model.process.Contract;
import org.bonitasoft.studio.model.process.ContractConstraint;
import org.bonitasoft.studio.model.process.ContractInput;
import org.bonitasoft.studio.model.process.ContractInputType;
import org.bonitasoft.studio.model.process.ProcessPackage;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IListChangeListener;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.list.ListChangeEvent;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.databinding.EMFDataBindingContext;
import org.eclipse.emf.databinding.EMFObservables;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;


/**
 * @author Romain Bioteau
 *
 */
public class ContractPropertySection extends EObjectSelectionProviderSection {

    private EMFDataBindingContext context;

    private ContractDefinitionValidator contractDefinitionValidator;

    private ContractInputController inputController;

    private ContractConstraintController constraintController;

    @Override
    public String getSectionDescription() {
        return Messages.contractSectionDescription;
    }

    @Override
    public String getSectionTitle() {
        return Messages.contractInputs;
    }

    protected void init() {
        setContractDefinitionValidator(new ContractDefinitionValidator(
                getMessageManager()));
        setInputController(new ContractInputController(getContractDefinitionValidator()));
        setConstraintController(new ContractConstraintController(getContractDefinitionValidator()));
        setContext(new EMFDataBindingContext());
    }

    @Override
    protected void createContent(final Composite parent) {
        init();
        final CTabFolder tabFolder = getWidgetFactory().createTabFolder(parent, SWT.FLAT | SWT.TOP);
        tabFolder.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        getWidgetFactory().adapt(tabFolder, true, true);

        final IObservableValue observeContractValue = CustomEMFEditObservables.observeDetailValue(Realm.getDefault(), getEObjectObservable(),
                ProcessPackage.Literals.TASK__CONTRACT);
        observeContractValue.addValueChangeListener(new IValueChangeListener() {

            @Override
            public void handleValueChange(final ValueChangeEvent event) {
                validate((Contract) event.diff.getNewValue());
            }
        });

        final CTabItem inputTabItem = getWidgetFactory().createTabItem(tabFolder, SWT.NULL);
        inputTabItem.setText(Messages.inputTabLabel);
        final Composite inputComposite = getWidgetFactory().createComposite(tabFolder);
        inputComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        inputComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(15, 15, 10, 5).create());

        createInputTabContent(inputComposite, observeContractValue);

        inputTabItem.setControl(inputComposite);

        final Composite constraintComposite = getWidgetFactory().createComposite(tabFolder);
        constraintComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        constraintComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(15, 15, 10, 5).create());

        createConstraintTabContent(constraintComposite, observeContractValue);

        final CTabItem constraintTabItem = getWidgetFactory().createTabItem(tabFolder, SWT.NULL);
        constraintTabItem.setText(Messages.constraintsTabLabel);
        constraintTabItem.setControl(constraintComposite);
        tabFolder.setSelection(0);
    }

    private void createConstraintTabContent(final Composite parent, final IObservableValue observeContractValue) {
        final Composite buttonsComposite = createButtonContainer(parent);
        final Button addButton = createButton(buttonsComposite, Messages.add);
        final Button upButton = createButton(buttonsComposite, Messages.up);
        final Button downButton = createButton(buttonsComposite, Messages.down);
        final Button removeButton = createButton(buttonsComposite, Messages.remove);

        final ContractConstraintsTableViewer constraintsTableViewer = new ContractConstraintsTableViewer(parent, getWidgetFactory());
        constraintsTableViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(500, 180).create());
        constraintsTableViewer.initialize(getConstraintController(), getContractDefinitionValidator());


        constraintsTableViewer.setInput(CustomEMFEditObservables.observeDetailList(Realm.getDefault(), observeContractValue,
                ProcessPackage.Literals.CONTRACT__CONSTRAINTS));
        constraintsTableViewer.createAddListener(addButton);
        constraintsTableViewer.createMoveUpListener(upButton);
        constraintsTableViewer.createMoveDownListener(downButton);
        constraintsTableViewer.createRemoveListener(removeButton);

        bindAddConstraintButtonEnablement(addButton, observeContractValue);
        bindRemoveButtonEnablement(removeButton, constraintsTableViewer);
        bindUpButtonEnablement(upButton, constraintsTableViewer);
        bindDownButtonEnablement(downButton, constraintsTableViewer);
    }

    private void createInputTabContent(final Composite parent, final IObservableValue observeContractValue) {
        final Composite buttonsComposite = createButtonContainer(parent);
        final Button addButton = createButton(buttonsComposite, Messages.add);
        final Button addChildButton = createButton(buttonsComposite, Messages.addChild);
        final Button removeButton = createButton(buttonsComposite, Messages.remove);

        final ContractInputTreeViewer inputsTableViewer = new ContractInputTreeViewer(parent, getWidgetFactory());
        inputsTableViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(500, 180).create());
        inputsTableViewer.initialize(getInputController(), getContractDefinitionValidator());

        inputsTableViewer.setInput(observeContractValue);

        inputsTableViewer.createAddListener(addButton);
        inputsTableViewer.createAddChildListener(addChildButton);
        inputsTableViewer.createRemoveListener(removeButton);

        bindRemoveButtonEnablement(removeButton, inputsTableViewer);
        bindAddChildButtonEnablement(addChildButton, inputsTableViewer);
    }

    private Button createButton(final Composite buttonsComposite, final String label) {
        final Button button = getWidgetFactory().createButton(buttonsComposite, label, SWT.PUSH);
        button.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).minSize(IDialogConstants.BUTTON_WIDTH, SWT.DEFAULT).create());
        return button;
    }

    private Composite createButtonContainer(final Composite parent) {
        final Composite buttonsComposite = getWidgetFactory().createComposite(parent);
        buttonsComposite.setLayoutData(GridDataFactory.fillDefaults().grab(false, true).align(SWT.FILL, SWT.TOP).create());
        buttonsComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 3).extendedMargins(0, 0, 25, 0).create());
        return buttonsComposite;
    }

    protected void bindAddConstraintButtonEnablement(final Button button, final IObservableValue contractObservable) {
        final ISWTObservableValue observeEnabled = SWTObservables.observeEnabled(button);
        final IObservableList observeDetailList = CustomEMFEditObservables.observeDetailList(Realm.getDefault(), contractObservable,
                ProcessPackage.Literals.CONTRACT__INPUTS);
        observeDetailList.addListChangeListener(new IListChangeListener() {

            @Override
            public void handleListChange(final ListChangeEvent event) {
                if (!button.isDisposed()) {
                    observeEnabled.setValue(!event.getObservableList().isEmpty());
                }
            }
        });
    }
    protected void bindRemoveButtonEnablement(final Button button, final Viewer viewer) {
        getContext().bindValue(SWTObservables.observeEnabled(button),
                ViewersObservables.observeSingleSelection(viewer),
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER),
                emptySelectionToBooleanStrategy());
    }

    protected void bindUpButtonEnablement(final Button button, final Viewer viewer) {
        getContext().bindValue(SWTObservables.observeEnabled(button),
                ViewersObservables.observeSingleSelection(viewer),
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER),
                isFirstElementToBooleanStrategy());
    }

    protected void bindDownButtonEnablement(final Button button, final Viewer viewer) {
        getContext().bindValue(SWTObservables.observeEnabled(button),
                ViewersObservables.observeSingleSelection(viewer),
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER),
                isLastElementToBooleanStrategy());
    }

    protected void bindAddChildButtonEnablement(final Button button, final Viewer viewer) {
        getContext().bindValue(SWTObservables.observeEnabled(button),
                ViewersObservables.observeSingleSelection(viewer),
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER),
                emptySelectionAndComplexTypeToBooleanStrategy());

        getContext().bindValue(
                SWTObservables.observeEnabled(button),
                EMFObservables.observeDetailValue(Realm.getDefault(), ViewersObservables.observeSingleSelection(viewer),
                        ProcessPackage.Literals.CONTRACT_INPUT__TYPE),
                new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER),
                complexTypeToBooleanStrategy());
    }

    private UpdateValueStrategy emptySelectionToBooleanStrategy() {
        final UpdateValueStrategy modelStrategy = new UpdateValueStrategy();
        modelStrategy.setConverter(new Converter(Object.class,Boolean.class) {

            @Override
            public Object convert(final Object from) {
                return from != null;
            }
        });
        return modelStrategy;
    }

    private UpdateValueStrategy isLastElementToBooleanStrategy() {
        final UpdateValueStrategy strategy = new UpdateValueStrategy();
        strategy.setConverter(new Converter(Object.class, Boolean.class) {

            @Override
            public Object convert(final Object from) {
                if (from instanceof ContractConstraint) {
                    final Contract contract = (Contract) ((ContractConstraint) from).eContainer();
                    final EList<ContractConstraint> constraints = contract.getConstraints();
                    return constraints.size() > 1 && constraints.indexOf(from) < constraints.size() - 1;
                }
                return false;
            }
        });
        return strategy;
    }

    private UpdateValueStrategy isFirstElementToBooleanStrategy() {
        final UpdateValueStrategy strategy = new UpdateValueStrategy();
        strategy.setConverter(new Converter(Object.class, Boolean.class) {

            @Override
            public Object convert(final Object from) {
                if (from instanceof ContractConstraint) {
                    final Contract contract = (Contract) ((ContractConstraint) from).eContainer();
                    final EList<ContractConstraint> constraints = contract.getConstraints();
                    return constraints.size() > 1 && constraints.indexOf(from) > 0;
                }
                return false;
            }
        });
        return strategy;
    }

    private UpdateValueStrategy emptySelectionAndComplexTypeToBooleanStrategy() {
        final UpdateValueStrategy modelStrategy = new UpdateValueStrategy();
        modelStrategy.setConverter(new Converter(Object.class, Boolean.class) {

            @Override
            public Object convert(final Object from) {
                if (from instanceof ContractInput) {
                    return ((ContractInput) from).getType() == ContractInputType.COMPLEX;
                }
                return false;
            }
        });
        return modelStrategy;
    }

    private UpdateValueStrategy complexTypeToBooleanStrategy() {
        final UpdateValueStrategy modelStrategy = new UpdateValueStrategy();
        modelStrategy.setConverter(new Converter(ContractInputType.class, Boolean.class) {

            @Override
            public Object convert(final Object from) {
                if (from instanceof ContractInputType) {
                    return from == ContractInputType.COMPLEX;
                }
                return false;
            }
        });
        return modelStrategy;
    }

    protected void validate(final Contract contract) {
        getContractDefinitionValidator().validate(contract);
    }


    public ContractDefinitionValidator getContractDefinitionValidator() {
        return contractDefinitionValidator;
    }

    public void setContractDefinitionValidator(final ContractDefinitionValidator contractValidator) {
        contractDefinitionValidator = contractValidator;
    }

    public ContractInputController getInputController() {
        return inputController;
    }

    public void setInputController(final ContractInputController inputController) {
        this.inputController = inputController;
    }

    public ContractConstraintController getConstraintController() {
        return constraintController;
    }

    public void setConstraintController(final ContractConstraintController constraintController) {
        this.constraintController = constraintController;
    }

    public EMFDataBindingContext getContext() {
        return context;
    }

    public void setContext(final EMFDataBindingContext context) {
        this.context = context;
    }

    @Override
    public void dispose() {
        super.dispose();
        final EMFDataBindingContext ctx = getContext();
        if (ctx != null) {
            ctx.dispose();
        }
    }

}