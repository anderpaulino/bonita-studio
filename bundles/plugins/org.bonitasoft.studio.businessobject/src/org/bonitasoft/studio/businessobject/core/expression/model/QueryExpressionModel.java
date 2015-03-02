/*******************************************************************************
 * Copyright (C) 2009, 2014 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel – 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 *******************************************************************************/
package org.bonitasoft.studio.businessobject.core.expression.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Romain Bioteau
 * 
 */
public class QueryExpressionModel {

    private List<BusinessObjectExpressionQuery> businessObjects = new ArrayList<BusinessObjectExpressionQuery>();

    public List<BusinessObjectExpressionQuery> getBusinessObjects() {
        return businessObjects;
    }

    public void setBusinessObjects(List<BusinessObjectExpressionQuery> businessObjects) {
        this.businessObjects = businessObjects;
    }

}
