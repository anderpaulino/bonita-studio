/**
 * Copyright (C) 2014 BonitaSoft S.A.
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
package org.bonitasoft.studio.engine.export.switcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bonitasoft.studio.common.emf.tools.ExpressionHelper;
import org.bonitasoft.studio.model.connectorconfiguration.ConnectorConfiguration;
import org.bonitasoft.studio.model.connectorconfiguration.ConnectorParameter;
import org.bonitasoft.studio.model.expression.AbstractExpression;
import org.bonitasoft.studio.model.expression.ExpressionFactory;
import org.bonitasoft.studio.model.expression.ListExpression;
import org.bonitasoft.studio.model.expression.TableExpression;
import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * @author Romain Bioteau
 */
public class GroovyConnectorConfigurationConverter {

    private static final String VARIABLES_INPUT = "variables";
    private static final String SCRIPT_INPUT = "script";
    private static final String FAKE_SCRIPT_EXPRESSION = "fakeScriptExpression";

    public ConnectorConfiguration convert(final ConnectorConfiguration connectorConfiguration) {
        checkConnectorConfiguration(connectorConfiguration);
        final ConnectorConfiguration configuration = EcoreUtil.copy(connectorConfiguration);
        final org.bonitasoft.studio.model.expression.Expression fakeExpression = getFakeExpression(configuration);
        final org.bonitasoft.studio.model.expression.Expression scriptExpression = ExpressionHelper.createConstantExpression(
                fakeExpression.getContent(), String.class.getName());
        final org.bonitasoft.studio.model.expression.TableExpression variablesExpression = createVariableExpression(fakeExpression);
        ConnectorParameter paramToDelete = null;
        for (final ConnectorParameter param : configuration.getParameters()) {
            if (SCRIPT_INPUT.equals(param.getKey())) {
                param.setExpression(scriptExpression);
            }
            if (VARIABLES_INPUT.equals(param.getKey())) {
                param.setExpression(variablesExpression);
            }
            if (FAKE_SCRIPT_EXPRESSION.equals(param.getKey())) {
                paramToDelete = param;
            }
        }
        if (paramToDelete != null) {
            configuration.getParameters().remove(paramToDelete);
        }
        return configuration;
    }

    private void checkConnectorConfiguration(final ConnectorConfiguration connectorConfig) {
        Assert.isLegal(connectorConfig != null);
        Assert.isLegal(connectorConfig.getDefinitionId().equals("scripting-groovy-script"));
        final Set<String> keys = new HashSet<String>();
        for (final ConnectorParameter connectorParameter : connectorConfig.getParameters()) {
            keys.add(connectorParameter.getKey());
        }
        Assert.isLegal(keys.containsAll(Arrays.asList(SCRIPT_INPUT, FAKE_SCRIPT_EXPRESSION, VARIABLES_INPUT)));
    }

    private org.bonitasoft.studio.model.expression.TableExpression createVariableExpression(
            final org.bonitasoft.studio.model.expression.Expression fakeExpression) {
        final TableExpression variableExpression = ExpressionFactory.eINSTANCE.createTableExpression();
        for (final EObject dep : fakeExpression.getReferencedElements()) {
            final org.bonitasoft.studio.model.expression.Expression depValueExpression = ExpressionHelper.createExpressionFromEObject(dep);
            if (depValueExpression != null) {
                final ListExpression depLine = ExpressionFactory.eINSTANCE.createListExpression();
                final org.bonitasoft.studio.model.expression.Expression depNameExpression = ExpressionHelper.createConstantExpression(
                        depValueExpression.getName(), String.class.getName());
                final EList<org.bonitasoft.studio.model.expression.Expression> expressions = depLine.getExpressions();
                expressions.addAll(Arrays.asList(depNameExpression, depValueExpression));
                variableExpression.getExpressions().add(depLine);
            }
        }
        return variableExpression;
    }

    private org.bonitasoft.studio.model.expression.Expression getFakeExpression(final ConnectorConfiguration configuration) {
        for (final ConnectorParameter param : configuration.getParameters()) {
            if (FAKE_SCRIPT_EXPRESSION.equals(param.getKey())) {
                final AbstractExpression expression = param.getExpression();
                if (expression instanceof org.bonitasoft.studio.model.expression.Expression) {
                    return (org.bonitasoft.studio.model.expression.Expression) expression;
                }
            }
        }
        return null;
    }

    public boolean appliesTo(final ConnectorConfiguration configuration) {
        return "scripting-groovy-script".equals(configuration.getDefinitionId());
    }

}
