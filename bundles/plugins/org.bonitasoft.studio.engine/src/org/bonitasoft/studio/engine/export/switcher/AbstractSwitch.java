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
package org.bonitasoft.studio.engine.export.switcher;

import java.util.Set;

import org.bonitasoft.engine.bpm.connector.ConnectorEvent;
import org.bonitasoft.engine.bpm.process.impl.ActorDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.ConnectorDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.DataDefinitionBuilder;
import org.bonitasoft.engine.bpm.process.impl.DescriptionBuilder;
import org.bonitasoft.engine.bpm.process.impl.FlowElementBuilder;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.expression.Expression;
import org.bonitasoft.studio.common.log.BonitaStudioLog;
import org.bonitasoft.studio.engine.EnginePlugin;
import org.bonitasoft.studio.engine.export.EngineExpressionUtil;
import org.bonitasoft.studio.model.connectorconfiguration.ConnectorParameter;
import org.bonitasoft.studio.model.expression.Operation;
import org.bonitasoft.studio.model.kpi.AbstractKPIBinding;
import org.bonitasoft.studio.model.kpi.DatabaseKPIBinding;
import org.bonitasoft.studio.model.parameter.Parameter;
import org.bonitasoft.studio.model.process.AbstractProcess;
import org.bonitasoft.studio.model.process.Actor;
import org.bonitasoft.studio.model.process.ConnectableElement;
import org.bonitasoft.studio.model.process.Connector;
import org.bonitasoft.studio.model.process.Data;
import org.bonitasoft.studio.model.process.DataAware;
import org.bonitasoft.studio.model.process.Element;
import org.bonitasoft.studio.model.process.util.ProcessSwitch;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.ecore.EObject;

/**
 * @author Romain Bioteau
 */
public abstract class AbstractSwitch extends ProcessSwitch<Element> {

    protected final Set<EObject> eObjectNotExported;

    public static final String DB_CONNECTOR_FOR_KPI_ID = "database-jdbc";
    public static final String DB_CONNECTOR_VERSION = "1.0.0";
    public static final String DB_DRIVER = "driver";
    public static final String DB_URL = "url";
    public static final String DB_QUERY = "script";
    public static final String DB_USER = "username";
    public static final String DB_PASSWORD = "password";

    public AbstractSwitch(final Set<EObject> eObjectNotExported) {
        this.eObjectNotExported = eObjectNotExported;
    }

    protected void addActors(final ProcessDefinitionBuilder builder, final AbstractProcess process) {
        for (final Actor a : process.getActors()) {
            final ActorDefinitionBuilder actorBuilder = builder.addActor(a.getName());
            if (a.getDocumentation() != null) {
                actorBuilder.addDescription(a.getDocumentation());
            }
            if (a.isInitiator()) {
                actorBuilder.setActorInitiator(a.getName());
            }
        }
    }

    protected void addParameters(final ProcessDefinitionBuilder builder, final AbstractProcess process) {
        for (final Parameter p : process.getParameters()) {
            final String description = p.getDescription();
            builder.addParameter(p.getName(), p.getTypeClassname()).addDescription(description == null ? "" : description);
        }
    }

    protected void addConnector(final FlowElementBuilder builder, final ConnectableElement element) {
        for (Connector connector : element.getConnectors()) {
            if (!eObjectNotExported.contains(connector)) {
                if (isGroovyConnector(connector)) {
                    final GroovyConnectorConfigurationConverter groovyConnectorConfigurationConverter = new GroovyConnectorConfigurationConverter();
                    connector = groovyConnectorConfigurationConverter.convert(connector);
                }
                final ConnectorDefinitionBuilder connectorBuilder = builder.addConnector(connector.getName(), connector.getDefinitionId(),
                        connector.getDefinitionVersion(), ConnectorEvent.valueOf(connector.getEvent()));
                handleConnectorBehaviorOnFailure(connector, connectorBuilder);
                handleConnectorInputs(connector, connectorBuilder);
                handleConnectorOutputs(connector, connectorBuilder);
            }
        }
    }

    private boolean isGroovyConnector(final Connector connector) {
        return "scripting-groovy-script".equals(connector.getDefinitionId());
    }

    private void handleConnectorBehaviorOnFailure(final Connector connector, final ConnectorDefinitionBuilder connectorBuilder) {
        if (connector.isIgnoreErrors()) {
            connectorBuilder.ignoreError();
        } else if (connector.isThrowErrorEvent()) {
            connectorBuilder.throwErrorEventWhenFailed(connector.getNamedError());
        }
    }

    private void handleConnectorInputs(final Connector connector, final ConnectorDefinitionBuilder connectorBuilder) {
        for (final ConnectorParameter parameter : connector.getConfiguration().getParameters()) {
            final Expression inputExpression = EngineExpressionUtil.createExpression(parameter.getExpression());
            if (inputExpression != null) {
                connectorBuilder.addInput(parameter.getKey(), inputExpression);
            } else {
                if (BonitaStudioLog.isLoggable(IStatus.OK)) {
                    BonitaStudioLog.debug("Expression of input " + parameter.getKey() + " is null for connector " + connector.getName(),
                            EnginePlugin.PLUGIN_ID);
                }
            }
        }
    }

    private void handleConnectorOutputs(final Connector connector, final ConnectorDefinitionBuilder connectorBuilder) {
        for (final Operation outputOperation : connector.getOutputs()) {
            if (outputOperation.getLeftOperand() != null
                    && outputOperation.getLeftOperand().getContent() != null
                    && !outputOperation.getLeftOperand().getContent().isEmpty()
                    && outputOperation.getRightOperand() != null
                    && outputOperation.getRightOperand().getContent() != null) {
                connectorBuilder.addOutput(EngineExpressionUtil.createOperation(outputOperation));
            }
        }
    }

    protected void addKPIBinding(final FlowElementBuilder builder, final ConnectableElement element) {
        for (final AbstractKPIBinding kpiBinding : element.getKpis()) {
            if (kpiBinding instanceof DatabaseKPIBinding) {
                final ConnectorDefinitionBuilder connectorBuilder = builder.addConnector(kpiBinding.getName(), DB_CONNECTOR_FOR_KPI_ID, DB_CONNECTOR_VERSION,
                        ConnectorEvent.valueOf(kpiBinding.getEvent()));
                final DatabaseKPIBinding dbKPI = (DatabaseKPIBinding) kpiBinding;
                connectorBuilder.addInput(DB_DRIVER, EngineExpressionUtil.createExpression(dbKPI.getDriverclassName()));
                connectorBuilder.addInput(DB_URL, EngineExpressionUtil.createExpression(dbKPI.getJdbcUrl()));
                connectorBuilder.addInput(DB_QUERY, EngineExpressionUtil.createExpression(dbKPI.getRequest()));
                final Expression dbUserExpression = EngineExpressionUtil.createExpression(dbKPI.getUser());
                if (dbUserExpression != null) {
                    connectorBuilder.addInput(DB_USER, dbUserExpression);
                }
                final Expression dbPasswordExpression = EngineExpressionUtil.createExpression(dbKPI.getPassword());
                if (dbPasswordExpression != null) {
                    connectorBuilder.addInput(DB_PASSWORD, dbPasswordExpression);
                }
            }
        }
    }

    protected void addData(final FlowElementBuilder dataContainerBuilder,
            final DataAware dataAwareContainer) {
        for (final Data data : dataAwareContainer.getData()) {
            Expression expr = EngineExpressionUtil.createExpression(data.getDefaultValue());
            if (expr == null && data.isMultiple()) {
                expr = EngineExpressionUtil.createEmptyListExpression();
            }
            final ProcessSwitch<DataDefinitionBuilder> dataSwitch = getDataSwitch(dataContainerBuilder, data, expr);
            final DataDefinitionBuilder dataBuilder = dataSwitch.doSwitch(data.getDataType());
            if (data.isTransient() && dataBuilder != null) {
                dataBuilder.isTransient();
            }
        }
    }

    protected DataSwitch getDataSwitch(
            final FlowElementBuilder dataContainerBuilder, final Data data,
            final Expression defaultValueExpression) {
        return new DataSwitch(data, defaultValueExpression, dataContainerBuilder);
    }

    protected void addDescription(final DescriptionBuilder builder, final String description) {
        if (description != null && !description.isEmpty()) {
            builder.addDescription(description);
        }
    }

}
