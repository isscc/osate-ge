/*******************************************************************************
 * Copyright (C) 2013 University of Alabama in Huntsville (UAH)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The US Government has unlimited rights in this work in accordance with W31P4Q-10-D-0092 DO 0073.
 *******************************************************************************/
package org.osate.ge.diagrams.common.patterns;

import javax.inject.Inject;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.graphiti.features.context.ICreateConnectionContext;
import org.eclipse.graphiti.features.context.IDeleteContext;
import org.eclipse.graphiti.mm.GraphicsAlgorithmContainer;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Polyline;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Style;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ConnectionDecorator;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.osate.aadl2.AbstractFeature;
import org.osate.aadl2.ComponentClassifier;
import org.osate.aadl2.ComponentType;
import org.osate.aadl2.Context;
import org.osate.aadl2.DataAccess;
import org.osate.aadl2.DirectedFeature;
import org.osate.aadl2.DirectionType;
import org.osate.aadl2.Feature;
import org.osate.aadl2.FeatureGroup;
import org.osate.aadl2.FlowEnd;
import org.osate.aadl2.FlowKind;
import org.osate.aadl2.FlowSpecification;
import org.osate.aadl2.Parameter;
import org.osate.aadl2.Port;
import org.osate.aadl2.Subcomponent;
import org.osate.ge.diagrams.common.AadlElementWrapper;
import org.osate.ge.services.AadlFeatureService;
import org.osate.ge.services.AadlModificationService;
import org.osate.ge.services.BusinessObjectResolutionService;
import org.osate.ge.services.ConnectionService;
import org.osate.ge.services.DiagramModificationService;
import org.osate.ge.services.GraphicsAlgorithmManipulationService;
import org.osate.ge.services.HighlightingService;
import org.osate.ge.services.NamingService;
import org.osate.ge.services.ShapeService;
import org.osate.ge.services.StyleService;
import org.osate.ge.services.UserInputService;
import org.osate.ge.services.VisibilityService;
import org.osate.ge.services.AadlModificationService.AbstractModifier;

public class FlowSpecificationPattern extends AgeConnectionPattern {
	private final StyleService styleUtil;
	private final GraphicsAlgorithmManipulationService graphicsAlgorithmUtil;
	private final HighlightingService highlightingHelper;
	private final ConnectionService connectionHelper;
	private final ShapeService shapeService;
	private final AadlModificationService aadlModService;
	private final DiagramModificationService diagramModService;
	private final UserInputService userInputService;
	private final AadlFeatureService featureService;
	private final NamingService namingService;
	private final BusinessObjectResolutionService bor;
	
	@Inject
	public FlowSpecificationPattern(final VisibilityService visibilityHelper, final StyleService styleUtil, final GraphicsAlgorithmManipulationService graphicsAlgorithmUtil, 
			final HighlightingService highlightingHelper, final ConnectionService connectionHelper, final ShapeService shapeService, AadlModificationService aadlModService, 
			final DiagramModificationService diagramModService, final UserInputService userInputService, final AadlFeatureService featureService, 
			final NamingService namingService, final BusinessObjectResolutionService bor) {
		super(visibilityHelper);
		this.styleUtil = styleUtil;
		this.graphicsAlgorithmUtil = graphicsAlgorithmUtil;
		this.highlightingHelper = highlightingHelper;
		this.connectionHelper = connectionHelper;
		this.shapeService = shapeService;
		this.aadlModService = aadlModService;
		this.diagramModService = diagramModService;
		this.userInputService = userInputService;
		this.featureService = featureService;
		this.namingService = namingService;
		this.bor = bor;
	}

	@Override
	public boolean isMainBusinessObjectApplicable(final Object mainBusinessObject) {
		return AadlElementWrapper.unwrap(mainBusinessObject) instanceof FlowSpecification;
	}

	@Override
	protected void createDecorators(final Connection connection) {
		final FlowSpecification fs = getFlowSpecification(connection);
		final IPeCreateService peCreateService = Graphiti.getPeCreateService();
		
		// Before removing all the decorators, get position of the label(if one exists)
		int labelX = 5;
		int labelY = 10;
		for(final ConnectionDecorator d : connection.getConnectionDecorators()) {
			if(d.getGraphicsAlgorithm() instanceof Text) {
				final Text text = (Text)d.getGraphicsAlgorithm();
				labelX = text.getX();
				labelY = text.getY();
			}
		}
		
		connection.getConnectionDecorators().clear();
		
		switch(fs.getKind()) {
		case PATH:
			{
				// Create the arrow
		        final ConnectionDecorator arrowConnectionDecorator = peCreateService.createConnectionDecorator(connection, false, 1.0, true);    
		        createArrow(arrowConnectionDecorator, styleUtil.getDecoratorStyle());	
				break;
			}
			
		case SOURCE:
			{
				final ConnectionDecorator arrowConnectionDecorator = peCreateService.createConnectionDecorator(connection, false, 0.0, true);
				createArrow(arrowConnectionDecorator, styleUtil.getDecoratorStyle());
				final ConnectionDecorator vbarConnectionDecorator = peCreateService.createConnectionDecorator(connection, false, 1.0, true);
				createVbar(vbarConnectionDecorator, styleUtil.getDecoratorStyle());	
				break;
			}
			
		case SINK:
			{
				final ConnectionDecorator arrowConnectionDecorator = peCreateService.createConnectionDecorator(connection, false, 0.0, true);
				graphicsAlgorithmUtil.mirror(createArrow(arrowConnectionDecorator, styleUtil.getDecoratorStyle()));
				final ConnectionDecorator vbarConnectionDecorator = peCreateService.createConnectionDecorator(connection, false, 1.0, true);
				createVbar(vbarConnectionDecorator, styleUtil.getDecoratorStyle());	
				break;
			}
		}
		
		// Set color for the decorators
		for(final ConnectionDecorator cd : connection.getConnectionDecorators()) {
			highlightingHelper.highlight(fs, getSubcomponent(connection), cd.getGraphicsAlgorithm());
		}
		
		// Create Label
		final IGaService gaService = Graphiti.getGaService();
		final ConnectionDecorator textDecorator = peCreateService.createConnectionDecorator(connection, true, 0.5, true);
		final Text text = gaService.createDefaultText(getDiagram(), textDecorator);
		text.setStyle(styleUtil.getLabelStyle());
		gaService.setLocation(text, labelX, labelY);
	    text.setValue(fs.getName());
	    getFeatureProvider().link(textDecorator, new AadlElementWrapper(fs));
	}
	
	@Override
	protected void createGraphicsAlgorithm(final Connection connection) {
		final FlowSpecification fs = getFlowSpecification(connection);
		final IGaService gaService = Graphiti.getGaService();
		final Polyline polyline = gaService.createPlainPolyline(connection);
		final Style style = styleUtil.getFlowSpecificationStyle();
		polyline.setStyle(style);
		highlightingHelper.highlight(fs, getSubcomponent(connection), polyline);
	}
	
	private GraphicsAlgorithm createArrow(final GraphicsAlgorithmContainer gaContainer, final Style style) {
	    final IGaService gaService = Graphiti.getGaService();
	    final GraphicsAlgorithm ga = gaService.createPlainPolygon(gaContainer, new int[] {
	    		-6, 4, 
	    		2, 0, 
	    		-6, -4});
	    ga.setStyle(style);
	    return ga;
	}
	
	private GraphicsAlgorithm createVbar(final GraphicsAlgorithmContainer gaContainer, final Style style) {
	    final IGaService gaService = Graphiti.getGaService();
	    final GraphicsAlgorithm ga = gaService.createPlainPolyline(gaContainer, new int[] {
	    		0, 8,
	    		0, -8});
	    ga.setStyle(style);

	    return ga;
	}
	
	final FlowSpecification getFlowSpecification(final Connection connection) {
		return (FlowSpecification)AadlElementWrapper.unwrap(getBusinessObjectForPictogramElement(connection));
	}
	
	@Override
	protected Anchor[] getAnchors(final Connection connection) {
		final FlowSpecification fs = getFlowSpecification(connection);
		final ContainerShape ownerShape = connectionHelper.getOwnerShape(connection);
		return (ownerShape == null) ? null : connectionHelper.getAnchors(ownerShape, fs);		
	}
	
	@Override
	public boolean canDelete(final IDeleteContext context) {
		final Object bo = bor.getBusinessObjectForPictogramElement(context.getPictogramElement());
		if(!(bo instanceof FlowSpecification)) {
			return false;
		}
		
		final FlowSpecification fs = (FlowSpecification)bo;
		final Connection connection = (Connection)context.getPictogramElement();
		if(connection.getStart() == null) {
			return false; 
		}
		
		final AnchorContainer startContainer = connection.getStart().getParent();
		if(!(startContainer instanceof Shape)) {
			return false;
		}
		
		final ComponentClassifier cc = getComponentClassifier((Shape)startContainer);
		return fs.getContainingClassifier() == cc;
	}
	
	@Override
	public void delete(final IDeleteContext context) {
		if(!userInputService.confirmDelete(context)) {
			return;
		}

		// Make the modification
		final Connection connection = (Connection)context.getPictogramElement();
		final FlowSpecification fs = (FlowSpecification)bor.getBusinessObjectForPictogramElement(connection);
		aadlModService.modify(fs, new AbstractModifier<FlowSpecification, Object>() {
			private DiagramModificationService.Modification diagramMod;
			
			@Override
			public Object modify(final Resource resource, final FlowSpecification fs) {
				// Start the diagram modification
	 			diagramMod = diagramModService.startModification();	 			
	 			
	 			// Mark other diagrams for updating
 				diagramMod.markRelatedDiagramsAsDirty(fs.getContainingClassifier());

	 			EcoreUtil.remove(fs);
				
				return null;
			}
			
	 		@Override
			public void beforeCommit(final Resource resource, final FlowSpecification fs, final Object modificationResult) {
				diagramMod.commit();
			}
		});	
		
		// Clear selection
		getFeatureProvider().getDiagramTypeProvider().getDiagramBehavior().getDiagramContainer().selectPictogramElements(new PictogramElement[0]);
	}
	
	// Gets the first ComponentClassifier container for the shape
	private ComponentClassifier getComponentClassifier(final Shape shape) {
		return shapeService.getClosestBusinessObjectOfType(shape, ComponentClassifier.class);
	}	
	
	private Subcomponent getSubcomponent(final Connection connection) {
		if(connection.getStart() == null) {
			return null;
		}
		
		final AnchorContainer startContainer = connection.getStart().getParent();
		if(!(startContainer instanceof Shape)) {
			return null;
		}
		
		
		return shapeService.getClosestBusinessObjectOfType((Shape)startContainer, Subcomponent.class);
	}
	
	// This pattern only handles the creation of flow paths. Flow sources and flow sinks are handled by features via context menus.
	@Override
	public String getCreateName() {
		return "Flow Path";
	}
	
	@Override
	public boolean canStartConnection(final ICreateConnectionContext context) {
		return isValidCreateEndShape(context.getSourcePictogramElement(), DirectionType.IN);
    }
	
	@Override
	public boolean canCreate(final ICreateConnectionContext context) {
		return isValidCreateEndShape(context.getTargetPictogramElement(), DirectionType.OUT);
	}
	
	private Shape getFeatureShape(final Shape shape) {
		return shapeService.getClosestAncestorWithBusinessObjectType(shape, Feature.class);
	}
	
	private Context getContext(final Shape featureShape) {
		return shapeService.getClosestBusinessObjectOfType(featureShape.getContainer(), Context.class);
	}
	
	@Override
	public Connection create(final ICreateConnectionContext context) {
		final Shape inFeatureShape = getFeatureShape((Shape)context.getSourcePictogramElement());
		final Feature inFeature = (Feature)bor.getBusinessObjectForPictogramElement(inFeatureShape);
		final Shape outFeatureShape = getFeatureShape((Shape)context.getTargetPictogramElement());
		final Feature outFeature = (Feature)bor.getBusinessObjectForPictogramElement(outFeatureShape);
		
		// Get the Component Type
		final ComponentType ct = shapeService.getClosestBusinessObjectOfType((Shape)context.getSourcePictogramElement(), ComponentType.class);
		
		// Determine the name for the new flow specification
		final String newFlowSpecName = namingService.buildUniqueIdentifier(ct, "new_flow_spec");

		// Make the modification
		aadlModService.modify(ct, new AbstractModifier<ComponentType, FlowSpecification>() {
			private DiagramModificationService.Modification diagramMod;
			
			@Override
			public FlowSpecification modify(final Resource resource, final ComponentType ct) {
				// Start the diagram modification
     			diagramMod = diagramModService.startModification();
     			
     			final FlowSpecification fs = ct.createOwnedFlowSpecification();
     			fs.setKind(FlowKind.PATH);
     			fs.setName(newFlowSpecName);

     			// Create the flow ends
     			final FlowEnd inFlowEnd = fs.createInEnd();
     			inFlowEnd.setFeature(inFeature);
     			inFlowEnd.setContext(getContext(inFeatureShape));
     			
     			final FlowEnd outFlowEnd = fs.createOutEnd();
     			outFlowEnd.setFeature(outFeature);
     			outFlowEnd.setContext(getContext(outFeatureShape));
     			
     			diagramMod.markRelatedDiagramsAsDirty(ct);

				return fs;
			}
			
			@Override
			public void beforeCommit(final Resource resource, final ComponentType ct, final FlowSpecification newFlowSpecification) {
				diagramMod.commit();
			}
		});

		return null;
	}
	
	/**
	 * Returns whether a specified pictogram element is a shape that is contained in a feature shape that can be used as an end point for a flow path. If the feature is a directed
	 * feature, its direction must be IN OUT or match the specified direction
	 * @param pe
	 * @param requiredDirection
	 * @return
	 */
	private boolean isValidCreateEndShape(final PictogramElement pe, final DirectionType requiredDirection) {
		if(!(pe instanceof Shape)) {
			return false;
		}

		// Check that the pictogram represents a feature
		final Shape shape = (Shape)pe;
		final Feature feature = shapeService.getClosestBusinessObjectOfType(shape, Feature.class);
		if(feature == null) {
			return false;
		}
		
		// Check that the feature is of the appropriate type
		if(!(feature instanceof Port || feature instanceof Parameter || feature instanceof DataAccess || feature instanceof FeatureGroup || feature instanceof AbstractFeature)) {
			return false;
		}
		
		// If it is a direct feature, it must have the specified direction or be an in out feature. Take into account feature group, inverse, etc..
		if(feature instanceof DirectedFeature) {
			// Determine the actual direction of the feature. Since it could effected by things like inverse feature groups, etc
			final DirectedFeature df = (DirectedFeature)feature;
			DirectionType direction = df.getDirection();
	 		if(direction == DirectionType.IN || direction == DirectionType.OUT) {
	 			if(featureService.isFeatureInverted(shape)) {
	 				direction = (direction == DirectionType.IN) ? DirectionType.OUT : DirectionType.IN;
	 			}
	 		}	
	 		
 			if(direction != requiredDirection && direction != DirectionType.IN_OUT) {
 				return false;
 			}
		}
		
		return true;
	}
}