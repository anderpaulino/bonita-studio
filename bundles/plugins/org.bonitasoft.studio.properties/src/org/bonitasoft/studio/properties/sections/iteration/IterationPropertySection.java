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
package org.bonitasoft.studio.properties.sections.iteration;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.bonitasoft.studio.common.DataUtil;
import org.bonitasoft.studio.common.ExpressionConstants;
import org.bonitasoft.studio.common.databinding.CustomEMFEditObservables;
import org.bonitasoft.studio.common.emf.tools.ModelHelper;
import org.bonitasoft.studio.common.log.BonitaStudioLog;
import org.bonitasoft.studio.common.properties.EObjectSelectionProviderSection;
import org.bonitasoft.studio.common.repository.RepositoryManager;
import org.bonitasoft.studio.data.operation.RefactorDataOperation;
import org.bonitasoft.studio.data.provider.DataExpressionProvider;
import org.bonitasoft.studio.data.ui.property.section.DataLabelProvider;
import org.bonitasoft.studio.expression.editor.constant.ExpressionReturnTypeContentProvider;
import org.bonitasoft.studio.expression.editor.filter.AvailableExpressionTypeFilter;
import org.bonitasoft.studio.expression.editor.provider.IProposalListener;
import org.bonitasoft.studio.expression.editor.viewer.ExpressionViewer;
import org.bonitasoft.studio.groovy.DisplayEngineExpressionWithName;
import org.bonitasoft.studio.model.expression.Expression;
import org.bonitasoft.studio.model.expression.ExpressionPackage;
import org.bonitasoft.studio.model.process.Activity;
import org.bonitasoft.studio.model.process.Data;
import org.bonitasoft.studio.model.process.MultiInstanceType;
import org.bonitasoft.studio.model.process.MultiInstantiable;
import org.bonitasoft.studio.model.process.ProcessFactory;
import org.bonitasoft.studio.model.process.ProcessPackage;
import org.bonitasoft.studio.pics.Pics;
import org.bonitasoft.studio.pics.PicsConstants;
import org.bonitasoft.studio.properties.i18n.Messages;
import org.bonitasoft.studio.refactoring.core.RefactoringOperationType;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.SelectObservableValue;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.databinding.EMFDataBindingContext;
import org.eclipse.emf.databinding.EMFObservables;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.internal.core.search.JavaSearchScope;
import org.eclipse.jdt.internal.ui.dialogs.FilteredTypesSelectionDialog;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;



/**
 * @author Romain Bioteau
 *
 */
public class IterationPropertySection extends EObjectSelectionProviderSection implements ISelectionProvider {

    /**
     * @author Aurelien
     *         Display only Process data, it filters out step data. The step requires to be the context of the expressionviewer
     */
    private final class StepDataViewerFilter extends ViewerFilter {

        @Override
        public boolean select(final Viewer viewer, final Object parentElement, final Object element) {
            if (element instanceof Expression) {
                if (ExpressionConstants.VARIABLE_TYPE.equals(((Expression) element).getType())) {
                    if (parentElement instanceof Activity) {
                        final String expressionName = ((Expression) element).getName();
                        for (final Data activityData : ((Activity) parentElement).getData()) {
                            if (expressionName.equals(activityData.getName())) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

    private EMFDataBindingContext context;

    private ISWTObservableValue returnTypeComboTextObservable;

    private IObservableValue observeReturnTypeInput;

    /* (non-Javadoc)
     * @see org.bonitasoft.studio.common.properties.AbstractBonitaDescriptionSection#getSectionDescription()
     */
    @Override
    public String getSectionDescription() {
        return Messages.loopSectionDescription;
    }

    @Override
    protected void createContent(final Composite parent) {
        context = new EMFDataBindingContext();
        final TabbedPropertySheetWidgetFactory widgetFactory = getWidgetFactory();

        final Composite composite = widgetFactory.createPlainComposite(parent, SWT.NONE);
        composite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).margins(10, 5).create());
        composite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        final SelectObservableValue recurrenceTypeObservable = createRecurrenceTypeRadioGroup(composite, widgetFactory);

        final Composite stackedComposite = widgetFactory.createPlainComposite(composite, SWT.NONE);
        stackedComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        final StackLayout stackLayout = new StackLayout();
        stackedComposite.setLayout(stackLayout);

        final Composite noneComposite = widgetFactory.createComposite(stackedComposite, SWT.NONE);
        final Composite standardLoopContent = createStandardLoopContent(stackedComposite, widgetFactory);
        final Composite multiInstanceContent = createMultiInstanceContent(stackedComposite, widgetFactory);
        recurrenceTypeObservable.addValueChangeListener(new IValueChangeListener() {

            @Override
            public void handleValueChange(final ValueChangeEvent event) {
                final MultiInstanceType type = (MultiInstanceType) event.diff.getNewValue();
                switch (type) {
                    case NONE:
                        stackLayout.topControl = noneComposite;
                        stackedComposite.layout();
                        break;
                    case STANDARD:
                        stackLayout.topControl = standardLoopContent;
                        stackedComposite.layout();
                        break;
                    default:
                        stackLayout.topControl = multiInstanceContent;
                        stackedComposite.layout();
                        break;
                }

            }
        });

    }

    private Composite createStandardLoopContent(final Composite stackedComposite, final TabbedPropertySheetWidgetFactory widgetFactory) {
        final Composite standardLoopComposite = widgetFactory.createPlainComposite(stackedComposite, SWT.NONE);
        standardLoopComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).extendedMargins(0, 0, 20, 0).create());

        widgetFactory.createLabel(standardLoopComposite, "", SWT.NONE);//FIILER

        final Button testAfterButton = widgetFactory.createButton(standardLoopComposite, Messages.testAfterLabel, SWT.RADIO);
        testAfterButton.setLayoutData(GridDataFactory.swtDefaults().create());

        final Button testBefore = widgetFactory.createButton(standardLoopComposite, Messages.testBeforeLabel, SWT.RADIO);
        testBefore.setLayoutData(GridDataFactory.swtDefaults().create());

        final SelectObservableValue testEventObservable = new SelectObservableValue(Boolean.class);
        testEventObservable.addOption(Boolean.FALSE, SWTObservables.observeSelection(testAfterButton));
        testEventObservable.addOption(Boolean.TRUE, SWTObservables.observeSelection(testBefore));

        context.bindValue(testEventObservable, CustomEMFEditObservables.observeDetailValue(Realm.getDefault(), getEObjectObservable(),
                ProcessPackage.Literals.MULTI_INSTANTIABLE__TEST_BEFORE));

        widgetFactory.createLabel(standardLoopComposite, Messages.loopConditionLabel).setLayoutData(
                GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).create());

        final ExpressionViewer loopConditionExpressionViewer = new ExpressionViewer(standardLoopComposite, SWT.BORDER, widgetFactory);

        loopConditionExpressionViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
        loopConditionExpressionViewer.addFilter(new AvailableExpressionTypeFilter(new String[] { ExpressionConstants.CONSTANT_TYPE,
                ExpressionConstants.VARIABLE_TYPE,
                ExpressionConstants.PARAMETER_TYPE,
                ExpressionConstants.SCRIPT_TYPE }));
        loopConditionExpressionViewer.addFilter(new DisplayEngineExpressionWithName(
                new String[] { org.bonitasoft.engine.expression.ExpressionConstants.LOOP_COUNTER
                        .getEngineConstantName() }));
        loopConditionExpressionViewer.addFilter(new StepDataViewerFilter());

        context.bindValue(ViewersObservables.observeInput(loopConditionExpressionViewer), getEObjectObservable());
        context.bindValue(ViewersObservables.observeSingleSelection(loopConditionExpressionViewer), CustomEMFEditObservables.observeDetailValue(
                Realm.getDefault(), getEObjectObservable(), ProcessPackage.Literals.MULTI_INSTANTIABLE__LOOP_CONDITION));

        widgetFactory.createLabel(standardLoopComposite, Messages.maximumLoopLabel).setLayoutData(
                GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).create());

        final ExpressionViewer maximumLoopExpressionViewer = new ExpressionViewer(standardLoopComposite, SWT.BORDER, widgetFactory);
        maximumLoopExpressionViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
        maximumLoopExpressionViewer.addFilter(new AvailableExpressionTypeFilter(new String[] { ExpressionConstants.CONSTANT_TYPE,
                ExpressionConstants.VARIABLE_TYPE,
                ExpressionConstants.PARAMETER_TYPE,
                ExpressionConstants.SCRIPT_TYPE }));
        maximumLoopExpressionViewer.setMessage(Messages.optionalLabel, IStatus.INFO);
        maximumLoopExpressionViewer.addFilter(new StepDataViewerFilter());

        context.bindValue(ViewersObservables.observeInput(maximumLoopExpressionViewer), getEObjectObservable());
        context.bindValue(ViewersObservables.observeSingleSelection(maximumLoopExpressionViewer), CustomEMFEditObservables.observeDetailValue(
                Realm.getDefault(), getEObjectObservable(), ProcessPackage.Literals.MULTI_INSTANTIABLE__LOOP_MAXIMUM));

        return standardLoopComposite;
    }

    private Composite createMultiInstanceContent(final Composite stackedComposite, final TabbedPropertySheetWidgetFactory widgetFactory) {
        final Composite multiInstanceComposite = widgetFactory.createPlainComposite(stackedComposite, SWT.NONE);
        multiInstanceComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        multiInstanceComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).extendedMargins(30, 0, 15, 0).create());

        final Composite cardinalityRadioGroup = widgetFactory.createPlainComposite(multiInstanceComposite, SWT.NONE);
        cardinalityRadioGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(3, 1).create());
        cardinalityRadioGroup.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());

        final Button dataBasedInstanceButton = widgetFactory.createButton(cardinalityRadioGroup, Messages.dataBasedInstanceLabel, SWT.RADIO);
        dataBasedInstanceButton.setLayoutData(GridDataFactory.swtDefaults().create());

        final Button definedNumberOfInstancesButton = widgetFactory.createButton(cardinalityRadioGroup, Messages.definedNumberOfInstanceLabel, SWT.RADIO);
        definedNumberOfInstancesButton.setLayoutData(GridDataFactory.swtDefaults().create());

        final SelectObservableValue cardinalityObservable = new SelectObservableValue(Boolean.class);
        cardinalityObservable.addOption(Boolean.FALSE, SWTObservables.observeSelection(dataBasedInstanceButton));
        cardinalityObservable.addOption(Boolean.TRUE, SWTObservables.observeSelection(definedNumberOfInstancesButton));

        context.bindValue(cardinalityObservable, CustomEMFEditObservables.observeDetailValue(Realm.getDefault(), getEObjectObservable(),
                ProcessPackage.Literals.MULTI_INSTANTIABLE__USE_CARDINALITY));

        final Composite dataContainerComposite = widgetFactory.createPlainComposite(multiInstanceComposite, SWT.NONE);
        dataContainerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(3, 1).create());
        final StackLayout dataStackLayout = new StackLayout();
        dataContainerComposite.setLayout(dataStackLayout);

        final Composite cardinalityContent = createCardinalityContent(dataContainerComposite, widgetFactory);
        final Composite dataContent = createDataContent(dataContainerComposite, widgetFactory);

        cardinalityObservable.addValueChangeListener(new IValueChangeListener() {

            @Override
            public void handleValueChange(final ValueChangeEvent event) {
                final Boolean useCardinality = (Boolean) event.diff.getNewValue();
                if(useCardinality){
                    dataStackLayout.topControl = cardinalityContent;

                }else{
                    dataStackLayout.topControl = dataContent;
                }
                dataContainerComposite.layout();

            }
        });


        return multiInstanceComposite;

    }

    private void createCompletionConditionViewer(final TabbedPropertySheetWidgetFactory widgetFactory, final Composite multiInstanceComposite) {
        final Composite completionComposite = widgetFactory.createPlainComposite(multiInstanceComposite, SWT.NONE);
        completionComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(3, 1).create());
        completionComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).margins(0, 0).create());
        final Label label = widgetFactory.createLabel(completionComposite, Messages.multiInstance_completionConditionLabel);
        label.setLayoutData(GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).create());
        final ExpressionViewer completionConditionViewer = new ExpressionViewer(completionComposite, SWT.BORDER, widgetFactory);
        completionConditionViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        completionConditionViewer.addFilter(new AvailableExpressionTypeFilter(new String[] { ExpressionConstants.CONSTANT_TYPE,
                ExpressionConstants.VARIABLE_TYPE,
                ExpressionConstants.PARAMETER_TYPE,
                ExpressionConstants.SCRIPT_TYPE }));
        completionConditionViewer.addFilter(new DisplayEngineExpressionWithName(new String[] {
                org.bonitasoft.engine.expression.ExpressionConstants.NUMBER_OF_ACTIVE_INSTANCES.getEngineConstantName(),
                org.bonitasoft.engine.expression.ExpressionConstants.NUMBER_OF_COMPLETED_INSTANCES.getEngineConstantName(),
                org.bonitasoft.engine.expression.ExpressionConstants.NUMBER_OF_INSTANCES.getEngineConstantName(),
                org.bonitasoft.engine.expression.ExpressionConstants.NUMBER_OF_TERMINATED_INSTANCES.getEngineConstantName() }));
        completionConditionViewer.setMessage(Messages.multiInstance_completionConditionDescription, IStatus.INFO);

        context.bindValue(ViewersObservables.observeInput(completionConditionViewer), getEObjectObservable());
        context.bindValue(ViewersObservables.observeSingleSelection(completionConditionViewer), CustomEMFEditObservables.observeDetailValue(
                Realm.getDefault(), getEObjectObservable(), ProcessPackage.Literals.MULTI_INSTANTIABLE__COMPLETION_CONDITION));
    }

    private Composite createDataContent(final Composite dataContainerComposite, final TabbedPropertySheetWidgetFactory widgetFactory) {
        final Composite dataContent = widgetFactory.createPlainComposite(dataContainerComposite, SWT.NONE);
        dataContent.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        dataContent.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).create());

        createInputForDataGroup(widgetFactory, dataContent);

        final Composite imageComposite = widgetFactory.createPlainComposite(dataContent, SWT.NONE);
        imageComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, true).indent(0, 20).create());
        imageComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).margins(0, 0).spacing(0, 0).create());
        widgetFactory.createLabel(imageComposite, "").setImage(Pics.getImage("icon-arrow-right.png"));
        widgetFactory.createLabel(imageComposite, "").setImage(Pics.getImage("task_group.png"));
        widgetFactory.createLabel(imageComposite, "").setImage(Pics.getImage("icon-arrow-right.png"));


        createOutputGroup(widgetFactory, dataContent);

        createCompletionConditionViewer(widgetFactory, dataContent);

        return dataContent;
    }

    private void createOutputGroup(final TabbedPropertySheetWidgetFactory widgetFactory, final Composite dataContent) {
        final Composite outputComposite = widgetFactory.createPlainComposite(dataContent, SWT.NONE);
        outputComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).indent(0, 20).create());
        outputComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(1).create());

        final Button storeOutputButton = widgetFactory.createButton(outputComposite, Messages.storeOutputResult, SWT.CHECK);
        storeOutputButton.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());

        final ISWTObservableValue observeStoreOutputSelection = SWTObservables.observeSelection(storeOutputButton);
        context.bindValue(observeStoreOutputSelection, CustomEMFEditObservables.observeDetailValue(Realm.getDefault(),
                getEObjectObservable(), ProcessPackage.Literals.MULTI_INSTANTIABLE__STORE_OUTPUT));

        final Group outputGroup = widgetFactory.createGroup(outputComposite, Messages.output);
        outputGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        outputGroup.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 3).create());

        final Label outputDatalabel = widgetFactory.createLabel(outputGroup, Messages.outputData);
        outputDatalabel.setLayoutData(GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).create());
        final ComboViewer outputDataComboViewer = createComboViewer(widgetFactory, outputGroup, ProcessFactory.eINSTANCE.createActivity());
        outputDataComboViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).indent(5, 0).create());


        final IViewerObservableValue observeSingleSelection = ViewersObservables.observeSingleSelection(outputDataComboViewer);

        final IObservableValue observeDataValue = CustomEMFEditObservables.observeDetailValue(Realm.getDefault(),
                getEObjectObservable(), ProcessPackage.Literals.MULTI_INSTANTIABLE__OUTPUT_DATA);
        getEObjectObservable().addValueChangeListener(createInputValueChanged(outputDataComboViewer, observeSingleSelection, observeDataValue, true));
        context.bindValue(observeSingleSelection, observeDataValue);

        outputDataComboViewer.addSelectionChangedListener(createComboSelectionListener(outputDataComboViewer, true));

        final Label label = widgetFactory.createLabel(outputGroup, "");
        label.setImage(Pics.getImage("icon-arrow-down.png"));
        label.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).align(SWT.CENTER, SWT.CENTER).create());

        final Label outputListlabel = widgetFactory.createLabel(outputGroup, Messages.outputList);
        outputListlabel.setLayoutData(GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).create());
        final ComboViewer outputListComboViewer = createComboViewer(widgetFactory, outputGroup, ProcessFactory.eINSTANCE.createPool());
        outputListComboViewer.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).indent(5, 0).create());
        outputListComboViewer.addFilter(new ListDataFilter());


        final IObservableValue observeDetailValue = CustomEMFEditObservables.observeDetailValue(Realm.getDefault(),
                getEObjectObservable(), ProcessPackage.Literals.MULTI_INSTANTIABLE__LIST_DATA_CONTAINING_OUTPUT_RESULTS);
        final IViewerObservableValue observeSingleSelection2 = ViewersObservables.observeSingleSelection(outputListComboViewer);

        getEObjectObservable().addValueChangeListener(createInputValueChanged(outputListComboViewer, observeSingleSelection2, observeDetailValue, false));
        context.bindValue(observeSingleSelection2, observeDetailValue);

        context.bindValue(PojoObservables.observeValue(new RecursiveControlEnablement(outputGroup), "enabled"),
                observeStoreOutputSelection);

        outputListComboViewer.addSelectionChangedListener(createComboSelectionListener(outputListComboViewer, false));
    }

    private Object getDataFromName(final String newVariableName, final IObservableList input) {
        if (input instanceof IObservableList) {
            final Iterator<?> iterator = input.iterator();
            while (iterator.hasNext()) {
                final Object object = iterator.next();
                if (object instanceof Data) {
                    if (((Data) object).getName().equals(newVariableName)) {
                        return object;
                    }
                }

            }
        }
        return null;
    }

    private void createInputForDataGroup(final TabbedPropertySheetWidgetFactory widgetFactory, final Composite dataContent) {
        final Group inputGroup = widgetFactory.createGroup(dataContent, Messages.input);
        inputGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        inputGroup.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 3).create());

        final Label inputListlabel = widgetFactory.createLabel(inputGroup, Messages.inputList + " *");
        inputListlabel.setLayoutData(GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).create());

        final ControlDecoration inputListlabelDecoration = new ControlDecoration(inputListlabel, SWT.RIGHT);
        inputListlabelDecoration.setMarginWidth(-3);
        inputListlabelDecoration.setDescriptionText(Messages.inputListHint);
        inputListlabelDecoration.setImage(Pics.getImage(PicsConstants.hint));

        final ComboViewer inputListComboViewer = createComboViewer(widgetFactory, inputGroup, ProcessFactory.eINSTANCE.createPool());
        inputListComboViewer.getControl().setLayoutData(
                GridDataFactory.fillDefaults().grab(true, false).hint(200, SWT.DEFAULT).indent(5, 0).create());
        inputListComboViewer.addFilter(new ListDataFilter());

        final IViewerObservableValue observeSingleSelection = ViewersObservables.observeSingleSelection(inputListComboViewer);
        final IObservableValue observeInputCollectionValue = CustomEMFEditObservables.observeDetailValue(Realm.getDefault(),
                getEObjectObservable(), ProcessPackage.Literals.MULTI_INSTANTIABLE__COLLECTION_DATA_TO_MULTI_INSTANTIATE);


        getEObjectObservable()
        .addValueChangeListener(createInputValueChanged(inputListComboViewer, observeSingleSelection, observeInputCollectionValue, false));


        inputListComboViewer.addSelectionChangedListener(createComboSelectionListener(inputListComboViewer, false));

        context.bindValue(observeSingleSelection, observeInputCollectionValue);

        observeInputCollectionValue.addValueChangeListener(new IValueChangeListener() {

            private MultiInstantiable currentInstantiable;

            @Override
            public void handleValueChange(final ValueChangeEvent event) {
                final Object data = event.diff.getNewValue();
                if (data instanceof Data) {
                    if (((Data) data).isMultiple()) {
                        final String technicalTypeFor = getQualifiedNameFromMultipleData((Data) data);
                        if (!returnTypeComboTextObservable.isDisposed()) {
                            final String currentReturnType = (String) returnTypeComboTextObservable.getValue();
                            if (currentInstantiable == null || currentInstantiable.equals(getEObjectObservable().getValue())) {
                                if (!technicalTypeFor.equals(currentReturnType)) {
                                    observeReturnTypeInput.setValue(new Object());
                                    returnTypeComboTextObservable.setValue(technicalTypeFor);
                                }
                            }
                        }
                    }
                    currentInstantiable = (MultiInstantiable) getEObjectObservable().getValue();
                }

            }

        });

        final Label label = widgetFactory.createLabel(inputGroup, "");
        label.setImage(Pics.getImage("icon-arrow-down.png"));
        label.setLayoutData(GridDataFactory.fillDefaults().span(3, 1).align(SWT.CENTER, SWT.CENTER).create());

        final Label ieratorLabel = widgetFactory.createLabel(inputGroup, Messages.iterator + " *");
        ieratorLabel.setLayoutData(GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).create());

        final ControlDecoration ieratorLabelDecoration = new ControlDecoration(ieratorLabel, SWT.RIGHT);
        ieratorLabelDecoration.setDescriptionText(Messages.iteratorHint);
        ieratorLabelDecoration.setImage(Pics.getImage(PicsConstants.hint));
        ieratorLabelDecoration.setMarginWidth(-3);

        createReturnTypeCombo(widgetFactory, inputGroup);
    }

    private ISelectionChangedListener createComboSelectionListener(final ComboViewer inputListComboViewer, final boolean stepData) {
        return new ISelectionChangedListener() {

            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final Object selection = ((IStructuredSelection) event.getSelection()).getFirstElement();
                if (selection instanceof IProposalListener) {
                    EObject value = (EObject) getEObjectObservable().getValue();
                    if (!stepData) {
                        value = ModelHelper.getParentProcess(value);
                    }
                    if (value != null) {
                        final String newVariableName = ((IProposalListener) selection).handleEvent(value, null);
                        if (newVariableName != null) {
                            final IObservableList observeList = EMFObservables.observeList(value, ProcessPackage.Literals.DATA_AWARE__DATA);
                            inputListComboViewer.setInput(observeList);
                            inputListComboViewer.setSelection(new StructuredSelection(getDataFromName(newVariableName, observeList)));
                        }
                    }else{
                        inputListComboViewer.setSelection(new StructuredSelection());
                    }

                }

            }
        };
    }

    private IValueChangeListener createInputValueChanged(final ComboViewer comboViewer, final IViewerObservableValue observeSingleSelection,
            final IObservableValue dataObservableValue, final boolean stepData) {
        return new IValueChangeListener() {

            @Override
            public void handleValueChange(final ValueChangeEvent event) {
                final EObject parentProcess = getTargetEObject(event, stepData);
                if (parentProcess != null) {
                    final Object newInput = EMFObservables.observeList(parentProcess, ProcessPackage.Literals.DATA_AWARE__DATA);
                    comboViewer.setInput(newInput);
                    final Object dataValue = dataObservableValue.getValue();
                    if (dataValue instanceof Data && newInput != null) {
                        final Iterator<?> iterator = ((IObservableList) newInput).iterator();
                        while (iterator.hasNext()) {
                            final Object object = iterator.next();
                            if (object instanceof Data) {
                                if (((Data) object).getName().equals(((Data) dataValue).getName())) {
                                    observeSingleSelection.setValue(object);
                                }
                            }

                        }
                    }
                }

            }

            private EObject getTargetEObject(final ValueChangeEvent event, final boolean stepData) {
                if (stepData) {
                    return (MultiInstantiable) event.diff.getNewValue();
                } else {
                    return ModelHelper.getParentProcess((MultiInstantiable) event.diff.getNewValue());
                }
            }
        };
    }

    private void createInputForCardinalityGroup(final TabbedPropertySheetWidgetFactory widgetFactory, final Composite dataContent) {
        final Group inputGroup = widgetFactory.createGroup(dataContent, Messages.input);
        inputGroup.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        inputGroup.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 3).create());

        final Label label = widgetFactory.createLabel(inputGroup, Messages.numberOfInstancesToCreate, SWT.NONE);
        label.setLayoutData(GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).create());
        final ExpressionViewer cardinalityExpression = new ExpressionViewer(inputGroup, SWT.BORDER, widgetFactory);
        cardinalityExpression.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
        cardinalityExpression.addFilter(new AvailableExpressionTypeFilter(new String[] { ExpressionConstants.CONSTANT_TYPE,
                ExpressionConstants.VARIABLE_TYPE,
                ExpressionConstants.PARAMETER_TYPE,
                ExpressionConstants.SCRIPT_TYPE }));

        context.bindValue(ViewersObservables.observeInput(cardinalityExpression), getEObjectObservable());
        context.bindValue(ViewersObservables.observeSingleSelection(cardinalityExpression), CustomEMFEditObservables.observeDetailValue(Realm.getDefault(),
                getEObjectObservable(), ProcessPackage.Literals.MULTI_INSTANTIABLE__CARDINALITY_EXPRESSION));
    }

    protected void createReturnTypeCombo(final TabbedPropertySheetWidgetFactory widgetFactory, final Composite parent) {
        final Composite iteratorComposite = widgetFactory.createPlainComposite(parent, SWT.NONE);
        iteratorComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).indent(6, 0).create());
        iteratorComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(4).extendedMargins(0, 10, 0, 0).create());

        final Text instanceDataNameText = widgetFactory.createText(iteratorComposite, "", SWT.BORDER);
        instanceDataNameText.setLayoutData(GridDataFactory.swtDefaults().hint(150, SWT.DEFAULT).create());

        final IObservableValue iteratorObservable = CustomEMFEditObservables.observeDetailValue(Realm.getDefault(), getEObjectObservable(),
                ProcessPackage.Literals.MULTI_INSTANTIABLE__ITERATOR_EXPRESSION);

        final ISWTObservableValue observeinstanceDataNameText = SWTObservables.observeText(instanceDataNameText, SWT.Modify);

        final UpdateValueStrategy updateIteratorNameTarget = new UpdateValueStrategy();
        updateIteratorNameTarget.setConverter(new Converter(String.class, Expression.class) {

            @Override
            public Object convert(final Object name) {
                final Expression expression = (Expression) iteratorObservable.getValue();
                if (expression != null && name != null) {
                    final Data oldItem = DataExpressionProvider.dataFromIteratorExpression(
                            (MultiInstantiable) ModelHelper.getParentFlowElement(expression), expression);

                    final CompoundCommand cc = new CompoundCommand();
                    cc.append(SetCommand.create(getEditingDomain(), expression, ExpressionPackage.Literals.EXPRESSION__NAME, name));
                    cc.append(SetCommand.create(getEditingDomain(), expression, ExpressionPackage.Literals.EXPRESSION__CONTENT, name));
                    getEditingDomain().getCommandStack().execute(cc);

                    final RefactorDataOperation op = new RefactorDataOperation(RefactoringOperationType.UPDATE);
                    op.setContainer(ModelHelper.getParentProcess(expression));

                    final Data newItem = EcoreUtil.copy(oldItem);
                    newItem.setName(name.toString());
                    op.addItemToRefactor(newItem, oldItem);
                    op.setEditingDomain(getEditingDomain());
                    final IProgressService service = PlatformUI.getWorkbench().getProgressService();
                    try {
                        service.run(true, false, op);
                    } catch (final InvocationTargetException e) {
                        BonitaStudioLog.error(e);
                    } catch (final InterruptedException e) {
                        BonitaStudioLog.error(e);
                    }
                }
                return expression;
            }
        });

        final UpdateValueStrategy updateIteratorNameModel = new UpdateValueStrategy();
        updateIteratorNameModel.setConverter(new Converter(Expression.class, String.class) {

            @Override
            public Object convert(final Object from) {
                final Expression expression = (Expression) from;
                if (expression != null) {
                    return expression.getName();
                }
                return "";
            }
        });

        context.bindValue(SWTObservables.observeDelayedValue(200, observeinstanceDataNameText), iteratorObservable, updateIteratorNameTarget,
                updateIteratorNameModel);

        final Label iteratorTypeLabel = widgetFactory.createLabel(iteratorComposite, Messages.type + " *");
        iteratorTypeLabel.setLayoutData(GridDataFactory.swtDefaults().align(SWT.RIGHT, SWT.CENTER).create());

        final ControlDecoration ieratorTypeDecoration = new ControlDecoration(iteratorTypeLabel, SWT.RIGHT);
        ieratorTypeDecoration.setDescriptionText(Messages.typeHint);
        ieratorTypeDecoration.setImage(Pics.getImage(PicsConstants.hint));
        ieratorTypeDecoration.setMarginWidth(-3);

        final ComboViewer returnTypeCombo = new ComboViewer(new Combo(iteratorComposite, SWT.BORDER));
        returnTypeCombo.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).indent(5, 0).create());
        returnTypeCombo.setContentProvider(new ExpressionReturnTypeContentProvider());
        returnTypeCombo.setLabelProvider(new LabelProvider());
        returnTypeCombo.setSorter(new ViewerSorter() {

            @Override
            public int compare(final Viewer viewer, final Object e1, final Object e2) {
                final String t1 = (String) e1;
                final String t2 = (String) e2;
                return t1.compareTo(t2);
            }
        });

        final Button browseClassesButton = widgetFactory.createButton(iteratorComposite, Messages.Browse, SWT.PUSH);
        browseClassesButton.setLayoutData(GridDataFactory.fillDefaults().create());

        final UpdateValueStrategy updateInputModel = new UpdateValueStrategy();
        updateInputModel.setConverter(new Converter(Expression.class, Object.class) {

            @Override
            public Object convert(final Object from) {
                final MultiInstantiable instantiable = (MultiInstantiable) getEObjectObservable().getValue();
                if (instantiable instanceof MultiInstantiable && instantiable.getCollectionDataToMultiInstantiate() != null) {
                    return getQualifiedNameFromMultipleData(instantiable.getCollectionDataToMultiInstantiate());
                }
                return new Object();
            }
        });
        observeReturnTypeInput = ViewersObservables.observeInput(returnTypeCombo);
        context.bindValue(observeReturnTypeInput, iteratorObservable, null, updateInputModel);

        final UpdateValueStrategy updateIteratorReturnTypeTarget = new UpdateValueStrategy();
        updateIteratorReturnTypeTarget.setConverter(new Converter(String.class, Expression.class) {

            @Override
            public Object convert(final Object returnType) {
                final Expression expression = (Expression) iteratorObservable.getValue();
                if (expression != null && returnType != null && !((String) returnType).isEmpty()) {

                    final Data oldItem = DataExpressionProvider.dataFromIteratorExpression(
                            (MultiInstantiable) ModelHelper.getParentFlowElement(expression), expression);

                    getEditingDomain().getCommandStack().execute(
                            SetCommand.create(getEditingDomain(), expression, ExpressionPackage.Literals.EXPRESSION__RETURN_TYPE, returnType));

                    final RefactorDataOperation op = new RefactorDataOperation(RefactoringOperationType.UPDATE);
                    op.setContainer(ModelHelper.getParentProcess(expression));

                    final Data newItem = DataExpressionProvider.dataFromIteratorExpression(
                            (MultiInstantiable) ModelHelper.getParentFlowElement(expression), expression);

                    op.addItemToRefactor(newItem, oldItem);
                    op.setEditingDomain(getEditingDomain());
                    final IProgressService service = PlatformUI.getWorkbench().getProgressService();
                    try {
                        service.run(true, false, op);
                    } catch (final InvocationTargetException e) {
                        BonitaStudioLog.error(e);
                    } catch (final InterruptedException e) {
                        BonitaStudioLog.error(e);
                    }
                }
                return expression;
            }
        });

        final UpdateValueStrategy updateIteratorReturnTypeModel = new UpdateValueStrategy();
        updateIteratorReturnTypeModel.setConverter(new Converter(Expression.class, String.class) {

            @Override
            public Object convert(final Object from) {
                final Expression expression = (Expression) from;
                if (expression != null) {
                    return expression.getReturnType();
                }
                return Object.class.getName();
            }
        });

        returnTypeComboTextObservable = SWTObservables.observeText(returnTypeCombo.getCombo());
        context.bindValue(SWTObservables.observeDelayedValue(200, returnTypeComboTextObservable), iteratorObservable, updateIteratorReturnTypeTarget,
                updateIteratorReturnTypeModel);

        browseClassesButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                final String typeName = openClassSelectionDialog();
                returnTypeComboTextObservable.setValue(typeName);
            }
        });
    }

    /**
     * @param classText
     */
    @SuppressWarnings("restriction")
    private String openClassSelectionDialog() {
        final JavaSearchScope scope = new JavaSearchScope();
        try {
            scope.add(RepositoryManager.getInstance().getCurrentRepository().getJavaProject());
        } catch (final Exception ex) {
            BonitaStudioLog.error(ex);
        }
        final FilteredTypesSelectionDialog searchDialog = new FilteredTypesSelectionDialog(Display.getDefault().getActiveShell(), false, null, scope,
                IJavaSearchConstants.TYPE);
        if (searchDialog.open() == Dialog.OK) {
            return ((IType) searchDialog.getFirstResult()).getFullyQualifiedName();
        }
        return null;
    }


    protected ComboViewer createComboViewer(final TabbedPropertySheetWidgetFactory widgetFactory, final Composite composite,
            final EObject contextFoContentProvider) {
        final ComboViewer comboViewer = new ComboViewer(composite, SWT.BORDER | SWT.READ_ONLY);

        final ObservableListContentProviderWithCreateData contentProvider = new ObservableListContentProviderWithCreateData(contextFoContentProvider);
        comboViewer.setContentProvider(contentProvider);
        final IObservableSet knownElements = contentProvider.getKnownElements();
        final IObservableMap[] labelMaps = EMFObservables.observeMaps(knownElements, new EStructuralFeature[] { ProcessPackage.Literals.ELEMENT__NAME,
                ProcessPackage.Literals.DATA__DATA_TYPE, ProcessPackage.Literals.DATA__MULTIPLE });
        comboViewer.setLabelProvider(new DataLabelProvider(labelMaps));


        final ToolBar toolBar = new ToolBar(composite, SWT.FLAT);
        widgetFactory.adapt(toolBar);
        final ToolItem toolItem = new ToolItem(toolBar, SWT.FLAT);
        toolItem.setImage(Pics.getImage(PicsConstants.clear));
        toolItem.setToolTipText(Messages.clearSelection);
        toolItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                comboViewer.setSelection(new StructuredSelection());
            }
        });

        return comboViewer;
    }

    private Composite createCardinalityContent(final Composite dataContainerComposite, final TabbedPropertySheetWidgetFactory widgetFactory) {
        final Composite cardinalityContent = widgetFactory.createPlainComposite(dataContainerComposite, SWT.NONE);
        cardinalityContent.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).create());
        createInputForCardinalityGroup(widgetFactory, cardinalityContent);

        final Composite imageComposite = widgetFactory.createPlainComposite(cardinalityContent, SWT.NONE);
        imageComposite.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(false, true).indent(0, 20).create());
        imageComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).margins(0, 0).spacing(0, 0).create());
        widgetFactory.createLabel(imageComposite, "").setImage(Pics.getImage("icon-arrow-right.png"));
        widgetFactory.createLabel(imageComposite, "").setImage(Pics.getImage("task_group.png"));
        widgetFactory.createLabel(imageComposite, "").setImage(Pics.getImage("icon-arrow-right.png"));

        createOutputGroup(widgetFactory, cardinalityContent);
        createCompletionConditionViewer(widgetFactory, cardinalityContent);

        return cardinalityContent;
    }

    private SelectObservableValue createRecurrenceTypeRadioGroup(final Composite composite, final TabbedPropertySheetWidgetFactory widgetFactory) {
        final Composite recurrenceTypeComposite = widgetFactory.createPlainComposite(composite, SWT.NONE);
        recurrenceTypeComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(4).create());
        recurrenceTypeComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());

        final Button noneRadio = widgetFactory.createButton(recurrenceTypeComposite, Messages.noneLabel, SWT.RADIO);
        noneRadio.setLayoutData(GridDataFactory.fillDefaults().create());

        final Button standardRadio = widgetFactory.createButton(recurrenceTypeComposite,Messages.standardLoop, SWT.RADIO);
        standardRadio.setLayoutData(GridDataFactory.fillDefaults().create());
        standardRadio.setImage(Pics.getImage("decoration/loop.png"));

        final Button parallelMultiRadio = widgetFactory.createButton(recurrenceTypeComposite, Messages.parallelMultinstantition, SWT.RADIO);
        parallelMultiRadio.setLayoutData(GridDataFactory.fillDefaults().create());
        parallelMultiRadio.setImage(Pics.getImage("decoration/parallel_multiInstance.png"));

        final Button sequentialMultiRadio = widgetFactory.createButton(recurrenceTypeComposite,Messages.sequentialMultinstantition, SWT.RADIO);
        sequentialMultiRadio.setLayoutData(GridDataFactory.fillDefaults().create());
        sequentialMultiRadio.setImage(Pics.getImage("decoration/sequential_multiInstance.png"));

        final SelectObservableValue recurrenceTypeObservable = new SelectObservableValue(MultiInstanceType.class);
        recurrenceTypeObservable.addOption(MultiInstanceType.NONE, SWTObservables.observeSelection(noneRadio));
        recurrenceTypeObservable.addOption(MultiInstanceType.STANDARD, SWTObservables.observeSelection(standardRadio));
        recurrenceTypeObservable.addOption(MultiInstanceType.PARALLEL, SWTObservables.observeSelection(parallelMultiRadio));
        recurrenceTypeObservable.addOption(MultiInstanceType.SEQUENTIAL, SWTObservables.observeSelection(sequentialMultiRadio));

        context.bindValue(recurrenceTypeObservable,
                CustomEMFEditObservables.observeDetailValue(Realm.getDefault(), getEObjectObservable(),
                        ProcessPackage.Literals.MULTI_INSTANTIABLE__TYPE));

        return recurrenceTypeObservable;
    }

    @Override
    public void dispose() {
        if (context != null) {
            context.dispose();
        }
        super.dispose();
    }

    private String getQualifiedNameFromMultipleData(final Data data) {
        if (data.isMultiple()) {
            final Data copy = EcoreUtil.copy(data);
            copy.setMultiple(false);
            final String technicalTypeFor = DataUtil.getTechnicalTypeFor(copy);
            return technicalTypeFor;
        }
        return Object.class.getName();
    }



}