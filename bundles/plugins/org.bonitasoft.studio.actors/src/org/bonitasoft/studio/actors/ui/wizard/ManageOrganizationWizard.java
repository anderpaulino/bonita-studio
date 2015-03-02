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
package org.bonitasoft.studio.actors.ui.wizard;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.studio.actors.i18n.Messages;
import org.bonitasoft.studio.actors.model.organization.Organization;
import org.bonitasoft.studio.actors.model.organization.User;
import org.bonitasoft.studio.actors.preference.ActorsPreferenceConstants;
import org.bonitasoft.studio.actors.repository.OrganizationRepositoryStore;
import org.bonitasoft.studio.actors.ui.wizard.page.AbstractOrganizationWizardPage;
import org.bonitasoft.studio.actors.ui.wizard.page.GroupsWizardPage;
import org.bonitasoft.studio.actors.ui.wizard.page.ManageOrganizationWizardPage;
import org.bonitasoft.studio.actors.ui.wizard.page.RolesWizardPage;
import org.bonitasoft.studio.actors.ui.wizard.page.UsersWizardPage;
import org.bonitasoft.studio.actors.validator.OrganizationValidator;
import org.bonitasoft.studio.common.log.BonitaStudioLog;
import org.bonitasoft.studio.common.repository.RepositoryManager;
import org.bonitasoft.studio.common.repository.model.IRepositoryFileStore;
import org.bonitasoft.studio.pics.Pics;
import org.bonitasoft.studio.preferences.BonitaPreferenceConstants;
import org.bonitasoft.studio.preferences.BonitaStudioPreferencesPlugin;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.Parameterization;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * @author Romain Bioteau
 */
public class ManageOrganizationWizard extends Wizard {

    private final List<Organization> organizations;
    private final List<Organization> organizationsWorkingCopy;
    private final OrganizationRepositoryStore store;
    private final OrganizationValidator validator = new OrganizationValidator();
    private Organization activeOrganization;
    private boolean activeOrganizationHasBeenModified = false;
    String userName;

    public ManageOrganizationWizard() {
        organizations = new ArrayList<Organization>();
        organizationsWorkingCopy = new ArrayList<Organization>();
        setWindowTitle(Messages.manageOrganizationTitle);
        store = (OrganizationRepositoryStore) RepositoryManager.getInstance().getCurrentRepository().getRepositoryStore(OrganizationRepositoryStore.class);
        for (final IRepositoryFileStore file : store.getChildren()) {
            organizations.add((Organization) file.getContent());
        }
        final String activeOrganizationName = BonitaStudioPreferencesPlugin.getDefault().getPreferenceStore()
                .getString(BonitaPreferenceConstants.DEFAULT_ORGANIZATION);
        for (final Organization orga : organizations) {
            final Organization copy = EcoreUtil.copy(orga);
            if (activeOrganizationName.equals(orga.getName())) {
                activeOrganization = copy;
                final EContentAdapter adapter = new EContentAdapter() {

                    @Override
                    public void notifyChanged(final Notification notification) {
                        super.notifyChanged(notification);
                        activeOrganizationHasBeenModified = true;
                    }
                };
                activeOrganization.eAdapters().add(adapter);
            }
            organizationsWorkingCopy.add(copy);

        }

        setDefaultPageImageDescriptor(Pics.getWizban());
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        addPage(new ManageOrganizationWizardPage(organizationsWorkingCopy));
        final GroupsWizardPage p = new GroupsWizardPage();
        final RolesWizardPage p1 = new RolesWizardPage();
        final UsersWizardPage p2 = new UsersWizardPage();
        addPage(p);
        addPage(p1);
        addPage(p2);
    }

    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        if (page instanceof ManageOrganizationWizardPage) {
            final Organization orga = ((ManageOrganizationWizardPage) page).getSelectedOrganization();
            if (orga != null) {
                for (final IWizardPage p : getPages()) {
                    if (p instanceof AbstractOrganizationWizardPage) {
                        ((AbstractOrganizationWizardPage) p).setOrganization(orga);
                    }
                }
            }
        }
        return super.getNextPage(page);
    }

    @Override
    public boolean performFinish() {
        try {
            getContainer().run(true, false, new IRunnableWithProgress() {

                @Override
                public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    monitor.beginTask(Messages.saveOrganization, IProgressMonitor.UNKNOWN);
                    for (final Organization organization : organizationsWorkingCopy) {
                        monitor.subTask(Messages.validatingOrganizationContent);
                        final String errorMessage = isOrganizationValid(organization);
                        if (errorMessage != null) {
                            throw new InterruptedException(organization.getName() + ": " + errorMessage);
                        }
                        final String fileName = organization.getName() + "." + OrganizationRepositoryStore.ORGANIZATION_EXT;
                        IRepositoryFileStore file = store.getChild(fileName);
                        Organization oldOrga = null;
                        if (file == null) {
                            file = store.createRepositoryFileStore(fileName);
                        } else {
                            oldOrga = (Organization) file.getContent();
                        }
                        if (oldOrga != null) {
                            final RefactorActorMappingsOperation refactorOp = new RefactorActorMappingsOperation(oldOrga, organization);
                            refactorOp.run(monitor);
                        }
                        file.save(organization);
                    }
                    for (final Organization orga : organizations) {
                        boolean exists = false;
                        for (final Organization orgCopy : organizationsWorkingCopy) {
                            if (orgCopy.getName().equals(orga.getName())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            final IRepositoryFileStore f = store.getChild(orga.getName() + "." + OrganizationRepositoryStore.ORGANIZATION_EXT);
                            if (f != null) {
                                f.delete();
                            }
                        }
                    }
                    monitor.done();
                }
            });
        } catch (final InterruptedException e) {
            openErrorStatusDialog(e.getMessage());
            return false;
        } catch (final InvocationTargetException e) {
            BonitaStudioLog.error(e);
            return false;
        }
        final IPreferenceStore preferenceStore = BonitaStudioPreferencesPlugin.getDefault().getPreferenceStore();
        final String pref = preferenceStore.getString(ActorsPreferenceConstants.TOGGLE_STATE_FOR_PUBLISH_ORGANIZATION);
        final boolean publishOrganization = preferenceStore.getBoolean(ActorsPreferenceConstants.PUBLISH_ORGANIZATION);
        if (publishOrganization && MessageDialogWithToggle.ALWAYS.equals(pref)) {
            try {
                publishOrganization(preferenceStore);
            } catch (final InvocationTargetException e) {
                BonitaStudioLog.error(e);

            } catch (final InterruptedException e) {
                BonitaStudioLog.error(e);
            }
        } else {
            if (MessageDialogWithToggle.NEVER.equals(pref) && activeOrganizationHasBeenModified) {
                final String[] buttons = { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };
                final MessageDialogWithToggle mdwt = new MessageDialogWithToggle(Display.getDefault().getActiveShell(), Messages.organizationHasBeenModifiedTitle,
                        null, Messages.bind(Messages.organizationHasBeenModifiedMessage, activeOrganization.getName()), MessageDialog.WARNING, buttons, 0,
                        Messages.doNotDisplayAgain, false);
                mdwt.setPrefStore(preferenceStore);
                mdwt.setPrefKey(ActorsPreferenceConstants.TOGGLE_STATE_FOR_PUBLISH_ORGANIZATION);
                final int index = mdwt.open();
                if (index == 2) {
                    try {
                        publishOrganization(preferenceStore);
                        if (mdwt.getToggleState()) {
                            preferenceStore.setDefault(ActorsPreferenceConstants.PUBLISH_ORGANIZATION, true);
                        }
                    } catch (final InvocationTargetException e) {
                        BonitaStudioLog.error(e);

                    } catch (final InterruptedException e) {
                        BonitaStudioLog.error(e);
                    }
                } else {
                    if (mdwt.getToggleState()) {
                        preferenceStore.setDefault(ActorsPreferenceConstants.PUBLISH_ORGANIZATION, false);
                        preferenceStore.setDefault(ActorsPreferenceConstants.TOGGLE_STATE_FOR_PUBLISH_ORGANIZATION, MessageDialogWithToggle.ALWAYS);
                    }
                }
            }

        }
        return true;
    }

    private void publishOrganization(final IPreferenceStore preferenceStore) throws InvocationTargetException, InterruptedException {
        getContainer().run(true, false, new IRunnableWithProgress() {

            @Override
            public void run(final IProgressMonitor maonitor) throws InvocationTargetException, InterruptedException {
                maonitor.beginTask(Messages.synchronizingOrganization, IProgressMonitor.UNKNOWN);
                userName = preferenceStore.getString(BonitaPreferenceConstants.USER_NAME);

                if (isUserExist(activeOrganization.getUsers().getUser(), userName)) {

                    final ICommandService service = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
                    final Command cmd = service.getCommand("org.bonitasoft.studio.engine.installOrganization");

                    final Map<String, Object> parameters = new HashMap<String, Object>();
                    parameters.put("artifact", activeOrganization.getName() + "." + OrganizationRepositoryStore.ORGANIZATION_EXT);

                    final IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
                    Parameterization p;
                    try {
                        p = new Parameterization(cmd.getParameter("artifact"), activeOrganization.getName() + "."
                                + OrganizationRepositoryStore.ORGANIZATION_EXT);
                        handlerService.executeCommand(new ParameterizedCommand(cmd, new Parameterization[] { p }), null);
                    } catch (final Exception e) {
                        throw new InvocationTargetException(e);
                    }

                    Display.getDefault().syncExec(new Runnable() {

                        @Override
                        public void run() {
                            MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.synchronizeInformationTitle,
                                    Messages.bind(Messages.synchronizeOrganizationSuccessMsg, activeOrganization.getName()));
                        }
                    });

                } else {
                    Display.getDefault().syncExec(new Runnable() {

                        @Override
                        public void run() {
                            MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.userDoesntExistAnymoreTitle,
                                    Messages.bind(Messages.userDoesntExistAnymore, userName));
                        }
                    });
                }

            }
        });
    }

    private boolean isUserExist(final List<User> users, final String userName) {
        for (final User user : users) {
            if (user.getUserName().equals(userName)) {
                return true;
            }
        }
        return false;
    }

    protected void openErrorStatusDialog(final String errorMessage) {
        MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.organizationValidationFailed, errorMessage);
    }

    protected String isOrganizationValid(final Organization organization) {
        final IStatus status = validator.validate(organization);
        if (!status.isOK()) {
            return status.getMessage();
        }
        return null;
    }


}
