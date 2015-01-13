/**
 * Copyright (C) 2013 BonitaSoft S.A.
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
package org.bonitasoft.studio.validation.constraints.process;

import org.bonitasoft.studio.common.emf.tools.ModelHelper;
import org.bonitasoft.studio.model.process.AbstractProcess;
import org.bonitasoft.studio.model.process.Connection;
import org.bonitasoft.studio.model.process.InclusiveGateway;
import org.bonitasoft.studio.model.process.Pool;
import org.bonitasoft.studio.model.process.SequenceFlow;
import org.bonitasoft.studio.model.process.diagram.providers.ProcessMarkerNavigationProvider;
import org.bonitasoft.studio.validation.TokenDispatcher;
import org.bonitasoft.studio.validation.constraints.AbstractLiveValidationMarkerConstraint;
import org.bonitasoft.studio.validation.i18n.Messages;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.validation.IValidationContext;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;

/**
 * @author Romain Bioteau
 */
public class InclusiveMergeGatewayConstraint extends AbstractLiveValidationMarkerConstraint {

    public static final String ID = "org.bonitasoft.studio.validation.constraints.inclusiveMerge";

    @Override
    protected IStatus performLiveValidation(IValidationContext ctx) {
        return ctx.createSuccessStatus();
    }

    @Override
    protected String getMarkerType(DiagramEditor editor) {
        return ProcessMarkerNavigationProvider.MARKER_TYPE;
    }

    @Override
    protected String getConstraintId() {
        return ID;
    }

    @Override
    protected IStatus performBatchValidation(IValidationContext ctx) {
        EObject eObj = ctx.getTarget();
        if (eObj instanceof InclusiveGateway) {
            InclusiveGateway mergeGateway = (InclusiveGateway) eObj;
            TokenDispatcher tokenDispatcher = new TokenDispatcher();
            final AbstractProcess parentProcess = ModelHelper.getParentProcess(mergeGateway);
            tokenDispatcher.recomputeAllToken((Pool) parentProcess);
            if (mergeGateway.getIncoming().size() > 1) {
                String lastToken = null;
                for (Connection c : mergeGateway.getIncoming()) {
                    if (c instanceof SequenceFlow) {
                        String currentToken = ((SequenceFlow) c).getPathToken();
                        if (lastToken != null && !lastToken.equals(currentToken)) {
                            return ctx.createFailureStatus(Messages.invalidInclusiveMergeConstraints);
                        }
                        lastToken = currentToken;
                    }
                }
            }
        }
        return ctx.createSuccessStatus();
    }

}
