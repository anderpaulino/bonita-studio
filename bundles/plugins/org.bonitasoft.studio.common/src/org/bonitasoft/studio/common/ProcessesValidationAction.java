package org.bonitasoft.studio.common;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bonitasoft.studio.common.emf.tools.ModelHelper;
import org.bonitasoft.studio.common.jface.FileActionDialog;
import org.bonitasoft.studio.common.jface.ValidationDialog;
import org.bonitasoft.studio.common.log.BonitaStudioLog;
import org.bonitasoft.studio.model.process.AbstractProcess;
import org.bonitasoft.studio.model.process.MainProcess;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

public class ProcessesValidationAction {

    private final List<AbstractProcess> listOfProcessesToValidate;
    private IStatus status;

    public ProcessesValidationAction(final List<AbstractProcess> processes) {
        listOfProcessesToValidate = processes;
    }

    public void performValidation() {
        final ICommandService cmdService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
        final Command cmd = cmdService.getCommand("org.bonitasoft.studio.validation.batchValidation");
        if (cmd.isEnabled()) {
            final IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
            final Set<String> procFiles = new HashSet<String>();
            for (final AbstractProcess p : listOfProcessesToValidate) {
                final Resource eResource = p.eResource();
                if (eResource != null) {
                    procFiles.add(URI.decode(eResource.getURI().lastSegment()));
                }
            }
            try {
                final Parameterization showReportParam = new Parameterization(cmd.getParameter("showReport"), Boolean.FALSE.toString());
                final Parameterization filesParam = new Parameterization(cmd.getParameter("diagrams"), procFiles.toString());
                status = (IStatus) handlerService.executeCommand(new ParameterizedCommand(cmd, new Parameterization[] { showReportParam,
                        filesParam }), null);
            } catch (final Exception e) {
                BonitaStudioLog.error(e);
            }
        }
    }

    private boolean statusContainsError() {
        if (status != null) {
            for (final IStatus s : status.getChildren()) {
                if (s.getSeverity() == IStatus.WARNING || s.getSeverity() == IStatus.ERROR) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean displayConfirmationDialog() {
        if (statusContainsError()) {
            if (!FileActionDialog.getDisablePopup()) {
                final String errorMessage = Messages.errorValidationMessage
                        + PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getTitle()
                        + Messages.errorValidationContinueAnywayMessage;
                final int result = new ValidationDialog(Display.getDefault().getActiveShell(), Messages.validationFailedTitle, errorMessage,
                        ValidationDialog.YES_NO_SEEDETAILS).open();
                if (result == ValidationDialog.NO) {
                    return false;
                } else {
                    if (result == ValidationDialog.SEE_DETAILS) {
                        showValidationPart();
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public boolean displayOkSeeMoreDetailsDialog() {
        if (statusContainsError()) {
            final String errorMessage = Messages.errorValidationMessage
                    + PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getTitle();
            final int result = new ValidationDialog(Display.getDefault().getActiveShell(), Messages.validationFailedTitle, errorMessage,
                    ValidationDialog.OK_SEEDETAILS).open();
            if (result == ValidationDialog.SEE_DETAILS) {
                showValidationPart();
            }
        }
        return true;
    }

    public IStatus getStatus() {
        return status;
    }

    public static void showValidationPart() {
        final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        final IEditorPart part = activePage.getActiveEditor();
        if (part != null && part instanceof DiagramEditor) {
            final MainProcess proc = ModelHelper.getMainProcess(((DiagramEditor) part).getDiagramEditPart().resolveSemanticElement());
            final String partName = proc.getName() + " (" + proc.getVersion() + ")";
            for (final IEditorReference ref : activePage.getEditorReferences()) {
                if (partName.equals(ref.getPartName())) {
                    activePage.activate(ref.getPart(true));
                    break;
                }
            }

        }
        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                try {
                    activePage.showView("org.bonitasoft.studio.validation.view");
                } catch (final PartInitException e) {
                    BonitaStudioLog.error(e);
                }
            }
        });
    }

}
