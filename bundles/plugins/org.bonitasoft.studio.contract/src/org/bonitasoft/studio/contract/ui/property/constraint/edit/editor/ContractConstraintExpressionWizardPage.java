/**
 * Copyright (C) 2014-2015 BonitaSoft S.A.
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
package org.bonitasoft.studio.contract.ui.property.constraint.edit.editor;

import java.net.MalformedURLException;
import java.util.List;

import org.bonitasoft.studio.common.emf.tools.ModelHelper;
import org.bonitasoft.studio.common.log.BonitaStudioLog;
import org.bonitasoft.studio.contract.ContractPlugin;
import org.bonitasoft.studio.contract.core.constraint.ConstraintInputIndexer;
import org.bonitasoft.studio.contract.i18n.Messages;
import org.bonitasoft.studio.contract.ui.property.constraint.edit.editor.contentassist.ContractInputCompletionProposalComputer;
import org.bonitasoft.studio.groovy.ui.viewer.GroovySourceViewerFactory;
import org.bonitasoft.studio.groovy.ui.viewer.GroovyViewer;
import org.bonitasoft.studio.model.process.ContractConstraint;
import org.bonitasoft.studio.model.process.ContractContainer;
import org.bonitasoft.studio.model.process.ContractInput;
import org.bonitasoft.studio.model.process.ProcessPackage;
import org.eclipse.core.databinding.observable.list.IObservableList;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.emf.databinding.EMFDataBindingContext;
import org.eclipse.emf.databinding.EMFObservables;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.IPropertySourceProvider;

/**
 * @author Romain Bioteau
 */
public class ContractConstraintExpressionWizardPage extends WizardPage implements IDocumentListener {

    protected static final String MVEL_BASICS_URL = "http://mvel.codehaus.org/Language+Guide+for+2.0";

    private final ContractConstraint constraint;
    private final List<ContractInput> inputs;
    private ConstraintInputIndexer inputIndexer;
    private IObservableValue expressionContentObservable;

    private GroovyViewer groovyViewer;
    private final GroovySourceViewerFactory groovyViewerFactory;
    private final MVELEditorFactory editorFactory;

    private final WebBrowserFactory browserFactory;

    public ContractConstraintExpressionWizardPage(final ContractConstraint constraint,
            final List<ContractInput> inputs,
            final GroovySourceViewerFactory sourceViewerFactory,
            final MVELEditorFactory editorFactory,
            final WebBrowserFactory browserFactory) {
        super(ContractConstraintExpressionWizardPage.class.getName());
        setDescription(Messages.constraintEditorDescription);
        this.constraint = constraint;
        this.inputs = inputs;
        groovyViewerFactory = sourceViewerFactory;
        this.editorFactory = editorFactory;
        this.browserFactory = browserFactory;
    }

    @Override
    public void dispose() {
        if (groovyViewer != null) {
            groovyViewer.dispose();
        }
        super.dispose();
    }

    protected SourceViewer getSourceViewer() {
        return groovyViewer.getSourceViewer();
    }

    @Override
    public void createControl(final Composite parent) {
        final EMFDataBindingContext context = new EMFDataBindingContext();
        final Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());
        final GroovyViewer viewer = createSourceViewer(container);
        getSourceViewer().getTextWidget().setData(ContractInputCompletionProposalComputer.INPUTS, inputs);
        getSourceViewer().getDocument().addDocumentListener(this);

        expressionContentObservable = EMFObservables.observeValue(constraint, ProcessPackage.Literals.CONTRACT_CONSTRAINT__EXPRESSION);

        final IObservableList inputsObservable = EMFObservables.observeList(constraint, ProcessPackage.Literals.CONTRACT_CONSTRAINT__INPUT_NAMES);
        inputIndexer = new ConstraintInputIndexer(constraint, inputs, viewer.getGroovyCompilationUnit());
        getSourceViewer().getDocument().set(expressionContentObservable.getValue().toString());
        context.addValidationStatusProvider(new ConstraintExpressionEditorValidator(expressionContentObservable, inputsObservable, viewer
                .getGroovyCompilationUnit(), new MVELProblemRequestor()));

        setControl(container);
        WizardPageSupport.create(this, context);

    }

    protected GroovyViewer createSourceViewer(final Composite container) {
        groovyViewer = groovyViewerFactory.createSourceViewer(container, editorFactory.newInstance());
        ContractContainer contractContainer = null;
        if (!inputs.isEmpty()) {
            contractContainer = ModelHelper.getFirstContainerOfType(inputs.get(0), ContractContainer.class);
        }
        groovyViewer.setContext(null, contractContainer, null, null);
        return groovyViewer;
    }

    @Override
    public void documentAboutToBeChanged(final DocumentEvent event) {
        //NOTHING TO DO
    }

    @Override
    public void documentChanged(final DocumentEvent event) {
        final String expression = event.getDocument().get();
        if (inputIndexer != null) {
            inputIndexer.schedule();
        }
        expressionContentObservable.setValue(expression);
    }

    @Override
    public void performHelp() {
        try {
            browserFactory.openExteranlBrowser(MVEL_BASICS_URL);
        } catch (final MalformedURLException e) {
            BonitaStudioLog.error("Invalid URL format for :" + MVEL_BASICS_URL, e, ContractPlugin.PLUGIN_ID);
        }
    }

    public boolean performFinish(final ContractConstraint constraintToUpdate, final IPropertySourceProvider propertySourceProvider) {
        final IPropertySource constraintPropertySource = propertySourceProvider.getPropertySource(constraintToUpdate);
        constraintPropertySource.setPropertyValue(ProcessPackage.Literals.CONTRACT_CONSTRAINT__EXPRESSION, constraint.getExpression());
        if (inputIndexer != null) {
            try {
                inputIndexer.join();
            } catch (final InterruptedException e) {
                BonitaStudioLog.error("Failed to join input indexer job.", e, ContractPlugin.PLUGIN_ID);
                return false;
            }
        }
        constraintPropertySource.setPropertyValue(ProcessPackage.Literals.CONTRACT_CONSTRAINT__INPUT_NAMES, constraint.getInputNames());
        return true;
    }

}