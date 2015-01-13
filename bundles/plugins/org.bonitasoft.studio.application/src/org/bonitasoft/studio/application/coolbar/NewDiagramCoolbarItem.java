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
package org.bonitasoft.studio.application.coolbar;

import org.bonitasoft.studio.application.i18n.Messages;
import org.bonitasoft.studio.common.extension.IBonitaContributionItem;
import org.bonitasoft.studio.common.log.BonitaStudioLog;
import org.bonitasoft.studio.pics.Pics;
import org.bonitasoft.studio.pics.PicsConstants;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;

/**
 * @author Romain Bioteau
 */
public class NewDiagramCoolbarItem implements IBonitaContributionItem {

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#dispose()
     */
    @Override
    public void dispose() {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#fill(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void fill(final Composite parent) {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#fill(org.eclipse.swt.widgets.Menu, int)
     */
    @Override
    public void fill(final Menu parent, final int index) {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#fill(org.eclipse.swt.widgets.ToolBar, int)
     */
    @Override
    public void fill(final ToolBar parent, final int index) {

    }

    private Command getCommand() {
        final ICommandService service = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
        final Command cmd = service.getCommand("org.bonitasoft.studio.diagram.command.newDiagram");
        return cmd;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#fill(org.eclipse.swt.widgets.CoolBar, int)
     */
    @Override
    public void fill(final CoolBar parent, final int index) {
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#getId()
     */
    @Override
    public String getId() {
        return "org.bonitasoft.studio.coolbar.newDiagram";
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#isDirty()
     */
    @Override
    public boolean isDirty() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#isDynamic()
     */
    @Override
    public boolean isDynamic() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#isGroupMarker()
     */
    @Override
    public boolean isGroupMarker() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#isSeparator()
     */
    @Override
    public boolean isSeparator() {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#isVisible()
     */
    @Override
    public boolean isVisible() {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#saveWidgetState()
     */
    @Override
    public void saveWidgetState() {

    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#setParent(org.eclipse.jface.action.IContributionManager)
     */
    @Override
    public void setParent(final IContributionManager parent) {

    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#setVisible(boolean)
     */
    @Override
    public void setVisible(final boolean visible) {

    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#update()
     */
    @Override
    public void update() {

    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.action.IContributionItem#update(java.lang.String)
     */
    @Override
    public void update(final String id) {

    }

    @Override
    public void fill(final ToolBar toolbar, final int index, final int iconSize) {
        final ToolItem item = new ToolItem(toolbar, SWT.PUSH);
        item.setToolTipText(Messages.NewProcessButtonLabel);
        if (iconSize < 0) {
            item.setText(Messages.NewProcessButtonLabel);
            item.setImage(Pics.getImage(PicsConstants.coolbar_new_48));
            item.setDisabledImage(Pics.getImage(PicsConstants.coolbar_new_disabled_48));
        } else {
            item.setImage(Pics.getImage(PicsConstants.coolbar_new_16));
            item.setDisabledImage(Pics.getImage(PicsConstants.coolbar_new_disabled_16));
        }
        item.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                item.setEnabled(false);
                final Command cmd = getCommand();
                try {
                    cmd.executeWithChecks(new ExecutionEvent());
                } catch (final Exception ex) {
                    BonitaStudioLog.error(ex);
                }
                item.setEnabled(true);
            }
        });

    }

}
