/*******************************************************************************
 * Copyright (C) 2013 University of Alabama in Huntsville (UAH)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The US Government has unlimited rights in this work in accordance with W31P4Q-10-D-0092 DO 0073.
 *******************************************************************************/
package org.osate.ge.diagrams.pkg;

import java.util.List;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.graphiti.dt.IDiagramTypeProvider;
import org.eclipse.graphiti.features.IUpdateFeature;
import org.eclipse.graphiti.features.context.IUpdateContext;
import org.eclipse.graphiti.features.custom.ICustomFeature;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.pattern.IPattern;
import org.osate.aadl2.Aadl2Factory;
import org.osate.aadl2.Aadl2Package;
import org.osate.ge.diagrams.common.AgeFeatureProvider;
import org.osate.ge.diagrams.pkg.features.PackageSetExtendedClassifierFeature;
import org.osate.ge.diagrams.pkg.features.PackageUpdateDiagramFeature;
import org.osate.ge.diagrams.pkg.patterns.PackageClassifierPattern;
import org.osate.ge.diagrams.pkg.patterns.PackageGeneralizationPattern;

public class PackageFeatureProvider extends AgeFeatureProvider {
	public PackageFeatureProvider(final IDiagramTypeProvider dtp) {
		super(dtp);
		//addPattern(make(PackageClassifierPattern.class));
		addConnectionPattern(make(PackageGeneralizationPattern.class));
		
		final Aadl2Package p = Aadl2Factory.eINSTANCE.getAadl2Package();
		addPattern(createPackageClassifierPattern(p.getAbstractType()));
		addPattern(createPackageClassifierPattern(p.getAbstractImplementation()));
		addPattern(createPackageClassifierPattern(p.getBusType()));
		addPattern(createPackageClassifierPattern(p.getBusImplementation()));
		addPattern(createPackageClassifierPattern(p.getDataType()));
		addPattern(createPackageClassifierPattern(p.getDataImplementation()));
		addPattern(createPackageClassifierPattern(p.getDeviceType()));
		addPattern(createPackageClassifierPattern(p.getDeviceImplementation()));
		addPattern(createPackageClassifierPattern(p.getFeatureGroupType()));
		addPattern(createPackageClassifierPattern(p.getMemoryType()));
		addPattern(createPackageClassifierPattern(p.getMemoryImplementation()));
		addPattern(createPackageClassifierPattern(p.getProcessType()));
		addPattern(createPackageClassifierPattern(p.getProcessImplementation()));
		addPattern(createPackageClassifierPattern(p.getProcessorType()));
		addPattern(createPackageClassifierPattern(p.getProcessorImplementation()));
		addPattern(createPackageClassifierPattern(p.getSubprogramType()));
		addPattern(createPackageClassifierPattern(p.getSubprogramImplementation()));
		addPattern(createPackageClassifierPattern(p.getSubprogramGroupType()));
		addPattern(createPackageClassifierPattern(p.getSubprogramGroupImplementation()));
		addPattern(createPackageClassifierPattern(p.getSystemType()));
		addPattern(createPackageClassifierPattern(p.getSystemImplementation()));
		addPattern(createPackageClassifierPattern(p.getThreadType()));
		addPattern(createPackageClassifierPattern(p.getThreadImplementation()));
		addPattern(createPackageClassifierPattern(p.getThreadGroupType()));
		addPattern(createPackageClassifierPattern(p.getThreadGroupImplementation()));
		addPattern(createPackageClassifierPattern(p.getVirtualBusType()));
		addPattern(createPackageClassifierPattern(p.getVirtualBusImplementation()));
		addPattern(createPackageClassifierPattern(p.getVirtualProcessorType()));
		addPattern(createPackageClassifierPattern(p.getVirtualProcessorImplementation()));		
	}
	
	private IPattern createPackageClassifierPattern(final EClass classifierType) {
		final IEclipseContext childCtx = getContext().createChild();
		childCtx.set("Classifier Type", classifierType);
		return ContextInjectionFactory.make(PackageClassifierPattern.class, childCtx);
	}

	@Override
	public IUpdateFeature getUpdateFeature(IUpdateContext context) {
	   PictogramElement pictogramElement = context.getPictogramElement();
	   if(pictogramElement instanceof Diagram) {
		   return make(PackageUpdateDiagramFeature.class);
	   }
	   return super.getUpdateFeature(context);
	}
	
	@Override
	protected void addCustomFeatures(final List<ICustomFeature> features) {
		super.addCustomFeatures(features);
		features.add(make(PackageSetExtendedClassifierFeature.class));
	}
}