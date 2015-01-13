/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 31 rue Gustave Eiffel - 38000 Grenoble
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
package org.bonitasoft.studio.actors.ui.section;

import java.util.HashSet;
import java.util.Set;

import org.bonitasoft.studio.actors.i18n.Messages;
import org.bonitasoft.studio.actors.repository.ActorFilterDefRepositoryStore;
import org.bonitasoft.studio.actors.ui.wizard.ActorFilterDefinitionWizardDialog;
import org.bonitasoft.studio.actors.ui.wizard.AddActorWizard;
import org.bonitasoft.studio.actors.ui.wizard.FilterWizard;
import org.bonitasoft.studio.common.emf.tools.ModelHelper;
import org.bonitasoft.studio.common.properties.AbstractBonitaDescriptionSection;
import org.bonitasoft.studio.common.repository.RepositoryManager;
import org.bonitasoft.studio.connector.model.definition.ConnectorDefinition;
import org.bonitasoft.studio.model.process.AbstractProcess;
import org.bonitasoft.studio.model.process.Actor;
import org.bonitasoft.studio.model.process.ActorFilter;
import org.bonitasoft.studio.model.process.Assignable;
import org.bonitasoft.studio.model.process.Lane;
import org.bonitasoft.studio.model.process.ProcessPackage;
import org.bonitasoft.studio.pics.Pics;
import org.bonitasoft.studio.pics.PicsConstants;
import org.eclipse.emf.databinding.EMFDataBindingContext;
import org.eclipse.emf.databinding.EMFObservables;
import org.eclipse.emf.databinding.edit.EMFEditObservables;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetWidgetFactory;

/**
 * @author Romain Bioteau
 */
public abstract class AbstractActorsPropertySection extends AbstractBonitaDescriptionSection implements ISelectionChangedListener {

    protected ComboViewer actorComboViewer;
    protected EMFDataBindingContext emfDatabindingContext;
    protected Text filterText;
    protected ToolItem removeConnectorButton;
    protected Button updateConnectorButton;
    private StyledFilterLabelProvider filterLabelProvider;
    protected Button setButton;

    @Override
    public void createControls(Composite parent, TabbedPropertySheetPage aTabbedPropertySheetPage) {
        super.createControls(parent, aTabbedPropertySheetPage);
        TabbedPropertySheetWidgetFactory widgetFactory = aTabbedPropertySheetPage.getWidgetFactory();

        Composite mainComposite = widgetFactory.createComposite(parent, SWT.NONE);
        mainComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).extendedMargins(0, 25, 0, 25).spacing(10, 15).create());
        mainComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        createRadioComposite(widgetFactory, mainComposite);

        Label actorsLabel = widgetFactory.createLabel(mainComposite, Messages.selectActor);
        actorsLabel.setLayoutData(GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).create());

        createActorComboViewer(mainComposite);

        createAddActorButton(mainComposite);

        filterLabelProvider = new StyledFilterLabelProvider();
        createFiltersViewer(mainComposite);

        updateDatabinding();
    }

    private void createAddActorButton(Composite mainComposite) {
        final Button addActor = new Button(mainComposite, SWT.FLAT);
        addActor.setText(Messages.addActor);
        addActor.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                AddActorWizard actorWizard = new AddActorWizard(getEObject(), getEditingDomain());
                WizardDialog wizardDialog = new WizardDialog(Display.getCurrent().getActiveShell(), actorWizard);
                if (wizardDialog.open() == Dialog.OK) {
                    if (actorWizard.getNewActor() != null) {
                        actorComboViewer.setSelection((ISelection) new StructuredSelection(actorWizard.getNewActor()));
                    }
                }
            }
        });
    }

    private void createActorComboViewer(Composite mainComposite) {
        actorComboViewer = new ComboViewer(mainComposite, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        actorComboViewer.getCombo().setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
        actorComboViewer.setContentProvider(new ArrayContentProvider());
        actorComboViewer.setLabelProvider(new LabelProvider() {

            @Override
            public String getText(Object element) {
                if (element instanceof Actor) {
                    String doc = ((Actor) element).getDocumentation();
                    if (doc != null && !doc.isEmpty()) {
                        doc = " -- " + doc;
                    } else {
                        doc = "";
                    }
                    return ((Actor) element).getName() + doc;
                }
                return super.getText(element);
            }
        });
    }

    protected void createFiltersViewer(Composite parent) {

        final Label actorFilters = getWidgetFactory().createLabel(parent, Messages.actorFilter, SWT.NONE);
        actorFilters.setLayoutData(GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).create());

        final Composite viewerComposite = getWidgetFactory().createPlainComposite(parent, SWT.NONE);
        viewerComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create());
        viewerComposite.setLayout(GridLayoutFactory.fillDefaults().numColumns(4).margins(0, 0).create());

        setButton = createSetButton(viewerComposite);

        filterText = getWidgetFactory().createText(viewerComposite, "", SWT.BORDER | SWT.SINGLE | SWT.NO_FOCUS | SWT.READ_ONLY);
        filterText.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).create());

        updateConnectorButton = createUpdateButton(viewerComposite);
        removeConnectorButton = createRemoveButton(viewerComposite);
    }

    protected EStructuralFeature getFilterFeature() {
        return ProcessPackage.Literals.ASSIGNABLE__FILTERS;
    }

    protected ToolItem createRemoveButton(Composite buttonsComposite) {
        ToolBar toolBar = new ToolBar(buttonsComposite, SWT.FLAT);
        getWidgetFactory().adapt(toolBar);
        ToolItem toolItem = new ToolItem(toolBar, SWT.FLAT);
        toolItem.setImage(Pics.getImage(PicsConstants.clear));
        toolItem.setToolTipText(Messages.remove);
        toolItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (MessageDialog.openConfirm(Display.getDefault().getActiveShell(), Messages.deleteDialogTitle, createMessage())) {
                    Assignable assignable = (Assignable) getEObject();
                    ActorFilter filter = assignable.getFilters().get(0);
                    getEditingDomain().getCommandStack().execute(new RemoveCommand(getEditingDomain(), getEObject(), getFilterFeature(), filter));
                    filterText.setText("");
                    updateButtons();
                }

            }

            public String createMessage() {
                StringBuilder res = new StringBuilder(Messages.deleteDialogConfirmMessage);
                res.append(' ');
                Assignable assignable = (Assignable) getEObject();
                res.append(assignable.getFilters().get(0).getName());

                res.append(" ?"); //$NON-NLS-1$
                return res.toString();
            }
        });
        return toolItem;
    }

    protected Button createUpdateButton(Composite buttonsComposite) {
        Button updateButton = getWidgetFactory().createButton(buttonsComposite, Messages.edit, SWT.FLAT);
        updateButton.setLayoutData(GridDataFactory.fillDefaults().hint(85, SWT.DEFAULT).create());
        updateButton.addListener(SWT.Selection, new Listener() {

            @Override
            public void handleEvent(Event event) {
                Assignable assignable = (Assignable) getEObject();
                ActorFilter filter = assignable.getFilters().get(0);
                ActorFilterDefRepositoryStore defStore = (ActorFilterDefRepositoryStore) RepositoryManager.getInstance().getRepositoryStore(
                        ActorFilterDefRepositoryStore.class);
                ConnectorDefinition def = defStore.getDefinition(filter.getDefinitionId(), filter.getDefinitionVersion());
                if (def != null) {
                    WizardDialog wizardDialog = new ActorFilterDefinitionWizardDialog(Display.getCurrent().getActiveShell(), new FilterWizard(filter,
                            getFilterFeature(), getFilterFeatureToCheckUniqueID()));
                    if (wizardDialog.open() == Dialog.OK) {
                        Assignable newAssignable = (Assignable) getEObject();
                        ActorFilter newfilter = newAssignable.getFilters().get(0);
                        filterText.setText(filterLabelProvider.getText(newfilter));
                        updateButtons();
                    }
                }
            }

        });
        return updateButton;
    }

    protected Set<EStructuralFeature> getFilterFeatureToCheckUniqueID() {
        Set<EStructuralFeature> res = new HashSet<EStructuralFeature>();
        res.add(getFilterFeature());
        return res;
    }

    protected Button createSetButton(Composite buttonsComposite) {
        final Button setButton = getWidgetFactory().createButton(buttonsComposite, Messages.set, SWT.FLAT);
        setButton.setLayoutData(GridDataFactory.fillDefaults().hint(85, SWT.DEFAULT).create());
        setButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                WizardDialog wizardDialog = new ActorFilterDefinitionWizardDialog(Display.getCurrent().getActiveShell(), new FilterWizard(getEObject(),
                        getFilterFeature(), getFilterFeatureToCheckUniqueID()));
                if (wizardDialog.open() == Dialog.OK) {
                    Assignable assignable = (Assignable) getEObject();
                    if (assignable.getFilters().size() > 1) {
                        getEditingDomain().getCommandStack().execute(
                                RemoveCommand.create(getEditingDomain(), assignable, assignable.getFilters(), assignable.getFilters().get(0)));
                    }
                    if (!assignable.getFilters().isEmpty()) {
                        ActorFilter filter = assignable.getFilters().get(0);
                        filterText.setText(filterLabelProvider.getText(filter));
                    }
                    updateButtons();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

            }
        });
        return setButton;
    }

    protected abstract void createRadioComposite(TabbedPropertySheetWidgetFactory widgetFactory, Composite mainComposite);

    @Override
    public void refresh() {
        super.refresh();
        if (filterText != null && !filterText.isDisposed()) {
            Assignable assignable = (Assignable) getEObject();
            if (assignable != null && !assignable.getFilters().isEmpty()) {
                ActorFilter filter = assignable.getFilters().get(0);
                filterText.setText(filterLabelProvider.getText(filter));
            } else {
                filterText.setText("");
            }
            updateButtons();
        }
        updateDatabinding();
    }

    protected void updateDatabinding() {
        Assignable assignable = (Assignable) getEObject();
        if (assignable != null) {
            if (emfDatabindingContext != null) {
                emfDatabindingContext.dispose();
            }
            emfDatabindingContext = new EMFDataBindingContext();
            AbstractProcess process = ModelHelper.getParentProcess(assignable);
            emfDatabindingContext.bindValue(ViewersObservables.observeInput(actorComboViewer),
                    EMFObservables.observeValue(process, ProcessPackage.Literals.ABSTRACT_PROCESS__ACTORS));
            emfDatabindingContext.bindValue(ViewersObservables.observeSingleSelection(actorComboViewer),
                    EMFEditObservables.observeValue(getEditingDomain(), assignable, ProcessPackage.Literals.ASSIGNABLE__ACTOR));
        }
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        updateButtons();
    }

    private void updateButtons() {
        if (filterText != null) {
            Assignable assignable = (Assignable) getEObject();
            ActorFilter filter = null;
            if (!assignable.getFilters().isEmpty()) {
                filter = assignable.getFilters().get(0);
            }

            //       if(!setButton.isDisposed()){
            //           setButton.setEnabled(filter == null) ;
            //       }

            if (!removeConnectorButton.isDisposed()) {
                removeConnectorButton.setEnabled(filter != null);
            }

            if (!updateConnectorButton.isDisposed()) {
                if (filter != null) {
                    ActorFilterDefRepositoryStore connectorDefStore = (ActorFilterDefRepositoryStore) RepositoryManager.getInstance().getRepositoryStore(
                            ActorFilterDefRepositoryStore.class);
                    ConnectorDefinition def = connectorDefStore.getDefinition(filter.getDefinitionId(), filter.getDefinitionVersion());
                    updateConnectorButton.setEnabled(def != null);
                } else {
                    updateConnectorButton.setEnabled(false);
                }

            }
        }
    }

    @Override
    public String getSectionDescription() {
        final EObject selectedEobject = getEObject();
        if (selectedEobject instanceof AbstractProcess) {
            return Messages.addRemoveActors;
        } else if (selectedEobject instanceof Lane) {
            return Messages.actorDescriptionLane;
        } else {
            return Messages.actorDescriptionTask;
        }
    }
}
