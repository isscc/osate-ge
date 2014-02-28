/*******************************************************************************
 * Copyright (C) 2013 University of Alabama in Huntsville (UAH)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The US Government has unlimited rights in this work in accordance with W31P4Q-10-D-0092 DO 0073.
 *******************************************************************************/
package org.osate.ge.services.impl;

import org.eclipse.graphiti.features.IMappingProvider;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.osate.ge.diagrams.common.AadlElementWrapper;
import org.osate.ge.services.BusinessObjectResolutionService;

public class DefaultBusinessObjectResolutionService implements BusinessObjectResolutionService {
	private IMappingProvider mappingProvider;
	public DefaultBusinessObjectResolutionService(final IMappingProvider mp) {
		this.mappingProvider = mp;
	}
	
	@Override
	public Object getBusinessObjectForPictogramElement(PictogramElement pe) {
		return AadlElementWrapper.unwrap(mappingProvider.getBusinessObjectForPictogramElement(pe));
	}
}
