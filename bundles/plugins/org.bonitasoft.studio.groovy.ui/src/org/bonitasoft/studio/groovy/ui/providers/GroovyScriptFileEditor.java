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
package org.bonitasoft.studio.groovy.ui.providers;

import org.bonitasoft.studio.common.jface.databinding.observables.DocumentObservable;
import org.bonitasoft.studio.common.jface.databinding.validator.InputLengthValidator;
import org.bonitasoft.studio.expression.editor.provider.IExpressionEditor;
import org.bonitasoft.studio.expression.editor.provider.IExpressionNatureProvider;
import org.bonitasoft.studio.expression.editor.viewer.ExpressionViewer;
import org.bonitasoft.studio.groovy.ui.viewer.GroovyViewer;
import org.bonitasoft.studio.model.expression.Expression;
import org.bonitasoft.studio.model.expression.ExpressionPackage;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.emf.databinding.EMFDataBindingContext;
import org.eclipse.emf.databinding.EMFObservables;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextListener;
import org.eclipse.jface.text.TextEvent;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

/**
 * @author Romain Bioteau
 */
public class GroovyScriptFileEditor extends GroovyScriptExpressionEditor implements IExpressionEditor {

    public static final String CONTEXT_DATA_KEY = "context";

    public static final String BONITA_KEYWORDS_DATA_KEY = "bonita.keywords";

    public static final String PROCESS_VARIABLES_DATA_KEY = "process.variables";

    public GroovyScriptFileEditor() {
        super();
    }

    /*
     * (non-Javadoc)
     * @see org.bonitasoft.studio.expression.editor.provider.IExpressionEditor#createExpressionEditor(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public Control createExpressionEditor(Composite parent, EMFDataBindingContext ctx) {
        createDataChooserArea(parent);
        mainComposite = new Composite(parent, SWT.NONE);
        mainComposite.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(SWT.DEFAULT, 300).create());
        mainComposite.setLayout(new FillLayout(SWT.VERTICAL));
        createGroovyEditor(parent);
        return mainComposite;
    }

    @Override
    public boolean canFinish() {
        return true;
    }

    @Override
    protected void createDataChooserArea(Composite composite) {

    }

    @Override
    public void bindExpression(EMFDataBindingContext dataBindingContext, final EObject context, Expression inputExpression, ViewerFilter[] filters,
            ExpressionViewer expressionViewer) {
        this.inputExpression = inputExpression;
        this.context = context;

        IObservableValue contentModelObservable = EMFObservables.observeValue(inputExpression, ExpressionPackage.Literals.EXPRESSION__CONTENT);

        groovyViewer.getDocument().set(inputExpression.getContent());
        IExpressionNatureProvider natureProvider = null;
        if (expressionViewer != null) {
            natureProvider = expressionViewer.getExpressionNatureProvider();
        }
        groovyViewer.setContext(context, filters, natureProvider);
        groovyViewer.getSourceViewer().getTextWidget().setData(BONITA_KEYWORDS_DATA_KEY, null);
        groovyViewer.getSourceViewer().getTextWidget().setData(PROCESS_VARIABLES_DATA_KEY, null);
        groovyViewer.getSourceViewer().getTextWidget().setData(CONTEXT_DATA_KEY, null);
        /*
         * dataBindingContext.bindValue(new DocumentObservable(sourceViewer), contentModelObservable,
         * new UpdateValueStrategy().setAfterGetValidator(new InputLengthValidator("", GroovyViewer.MAX_SCRIPT_LENGTH)), null);
         * sourceViewer.addTextListener(new ITextListener() {
         * @Override
         * public void textChanged(TextEvent event) {
         * sourceViewer.getTextWidget().notifyListeners(SWT.Modify, new Event());
         * }
         * });
         */
        final IValidator lenghtValidator = new InputLengthValidator("", GroovyViewer.MAX_SCRIPT_LENGTH);
        sourceViewer.getDocument().addDocumentListener(new IDocumentListener() {

            @Override
            public void documentChanged(DocumentEvent event) {
                final String text = event.getDocument().get();
                if (lenghtValidator.validate(text).isOK()) {
                    //groovyViewer.resetFoldingStructure();
                    GroovyScriptFileEditor.this.inputExpression.setContent(text);
                }

            }

            @Override
            public void documentAboutToBeChanged(DocumentEvent event) {
            }
        });
    }

}
