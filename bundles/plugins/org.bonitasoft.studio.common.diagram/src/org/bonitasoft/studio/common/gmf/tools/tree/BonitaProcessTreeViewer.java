/**
 * Copyright (C) 2011-2014 BonitaSoft S.A.
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
package org.bonitasoft.studio.common.gmf.tools.tree;

import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.studio.common.emf.tools.ModelHelper;
import org.bonitasoft.studio.common.gmf.tools.GMFTools;
import org.bonitasoft.studio.common.log.BonitaStudioLog;
import org.bonitasoft.studio.model.expression.Expression;
import org.bonitasoft.studio.model.form.Form;
import org.bonitasoft.studio.model.form.FormPackage;
import org.bonitasoft.studio.model.form.Validator;
import org.bonitasoft.studio.model.process.AbstractProcess;
import org.bonitasoft.studio.model.process.Element;
import org.bonitasoft.studio.model.process.MainProcess;
import org.bonitasoft.studio.model.process.MessageFlow;
import org.eclipse.emf.common.ui.URIEditorInput;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.gmf.runtime.common.ui.services.editor.EditorService;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IGraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.gmf.runtime.notation.Diagram;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;

/**
 * @author Romain Bioteau
 */
public class BonitaProcessTreeViewer extends BonitaTreeViewer {

    /**
     * Constructs a TreeViewer with the default root editpart.
     */
    public BonitaProcessTreeViewer() {
        super();
    }

    @Override
    protected void handleTreeSelection(final Tree tree) {
        final List<Object> newSelection = retrieveEditPartOfSelectElementInTree(tree);
        diagramEditPart.getViewer().deselectAll();
        if (!newSelection.isEmpty()) {
            final DiagramEditor editor = (DiagramEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            final IGraphicalEditPart selectableEP = (IGraphicalEditPart) newSelection.get(0);
            if (!selectableEP.isSelectable()) {
                BonitaStudioLog.error(selectableEP.toString() + " is not a selectable edit part", "org.bonitasoft.studio.common.diagram");
                return;
            }
            editor.getDiagramGraphicalViewer().select(selectableEP);
            BonitaProcessTreeSelectionProvider.getInstance().fireSelectionChanged(selectableEP, (EObject) tree.getSelection()[0].getData());
            scrollDiagram(selectableEP);
        }

    }

    private List<Object> retrieveEditPartOfSelectElementInTree(final Tree tree) {
        final TreeItem[] ties = tree.getSelection();
        final List<Object> newSelection = new ArrayList<Object>();
        for (int i = 0; i < ties.length; i++) {
            final Object tieData = ties[i].getData();
            if (tieData instanceof EObject) {
                final EObject elem = (EObject) tieData;
                final IGraphicalEditPart foundEp = findEditPartFor(elem);
                if (foundEp != null) {
                    newSelection.add(foundEp);
                }
            }
        }
        return newSelection;
    }

    @Override
    protected IGraphicalEditPart findEditPartFor(EObject elem) {
        IGraphicalEditPart ep = null;
        if (elem.eContainer() instanceof MainProcess && !(elem instanceof AbstractProcess) && !(elem instanceof MessageFlow)) {
            return diagramEditPart;
        } else {
            if (elem instanceof Element) {
                ep = GMFTools.findEditPart(diagramEditPart, elem);
            } else {
                while (elem != null && !(elem instanceof Element)) {
                    elem = elem.eContainer();
                }
                if (elem == null) {
                    return null;
                } else {
                    ep = GMFTools.findEditPart(diagramEditPart, elem);
                }
            }
            while (ep == null) {
                if (elem.eContainer() == null) {
                    break;
                }
                elem = elem.eContainer();
                if (elem instanceof Element) {
                    ep = GMFTools.findEditPart(diagramEditPart, elem);
                }
            }
            return ep;
        }
    }

    @Override
    protected void handlTreeDoubleClick() {
        if (!filteredTree.getViewer().getSelection().isEmpty()) {
            EObject element = (EObject) ((IStructuredSelection) filteredTree.getViewer().getSelection()).getFirstElement();
            if (element.eClass().getEPackage().getName().equals(FormPackage.eINSTANCE.getName())) {
                while (element != null && !(element instanceof Form)) {
                    element = element.eContainer();
                }
            }

            if (element instanceof Form) {
                openFormEditor((Form) element);
            }
        }
    }

    private void openFormEditor(final Form element) {
        final Diagram diag = ModelHelper.getDiagramFor(element);

        /*
         * need to get the URI after save because the name can change as it is
         * synchronized with the MainProcess name
         */
        final URI uri = EcoreUtil.getURI(diag);

        /* open the form editor */
        final DiagramEditor editor = (DiagramEditor) EditorService.getInstance().openEditor(new URIEditorInput(uri, ((Element) element).getName()));
        editor.getDiagramEditPart().getViewer().deselectAll();
        final EObject elem = (EObject) ((IStructuredSelection) filteredTree.getViewer().getSelection()).getFirstElement();
        Element selectedElem = null;
        if (elem instanceof Validator || elem instanceof Expression) {
            selectedElem = (Element) elem.eContainer();
        } else {
            selectedElem = (Element) elem;
        }
        final IGraphicalEditPart ep = GMFTools.findEditPart(editor.getDiagramEditPart(), selectedElem);
        editor.getDiagramEditPart().getViewer().select(ep);
        editor.getDiagramEditPart().getViewer().setSelection(new StructuredSelection(ep));
        BonitaFormTreeSelectionProvider.getInstance().fireSelectionChanged(ep, elem);
    }

}
