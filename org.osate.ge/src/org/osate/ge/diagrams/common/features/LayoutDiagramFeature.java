/*******************************************************************************
 * Copyright (C) 2013 University of Alabama in Huntsville (UAH)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The US Government has unlimited rights in this work in accordance with W31P4Q-10-D-0092 DO 0073.
 *******************************************************************************/
package org.osate.ge.diagrams.common.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IMoveShapeFeature;
import org.eclipse.graphiti.features.IResizeConfiguration;
import org.eclipse.graphiti.features.IResizeShapeFeature;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.context.IResizeShapeContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.features.context.impl.MoveShapeContext;
import org.eclipse.graphiti.features.context.impl.ResizeShapeContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.pictograms.Anchor;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.FreeFormConnection;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.osate.aadl2.Feature;
import org.osate.aadl2.InternalFeature;
import org.osate.aadl2.ProcessorFeature;
import org.osate.ge.diagrams.common.AgeResizeConfiguration;
import org.osate.ge.layout.LayoutAlgorithm;
import org.osate.ge.layout.SimpleLayoutAlgorithm;
import org.osate.ge.services.BusinessObjectResolutionService;
import org.osate.ge.services.LayoutService;
import org.osate.ge.services.PropertyService;
import org.osate.ge.services.ShapeService;

/**
 * Lays out the pictogram elements included in a diagram using an algorithm.
 * To specify a minimum size for a shape, the resize feature or pattern for the shape must return a resize configuration that implements AgeResizeConfiguration.
 * @author philip.alldredge
 *
 */
public class LayoutDiagramFeature extends AbstractCustomFeature {
	private static String relayoutShapesPropertyKey = "relayout";
	// Settings that determine how many different layouts to test. See usage for more details.
	private final BusinessObjectResolutionService bor;
	private final ShapeService shapeService;
	private final LayoutService resizeHelper;
	private final PropertyService propertyService;
	
	@Inject
	public LayoutDiagramFeature(final IFeatureProvider fp, final ShapeService shapeService, final LayoutService resizeHelper, final PropertyService propertyService, final BusinessObjectResolutionService bor) {
		super(fp);
		this.shapeService = shapeService;
		this.resizeHelper = resizeHelper;
		this.propertyService = propertyService;
		this.bor = bor;
	}

	@Override
	public String getDescription() {
		return "Layout diagram automatically";
	}

	@Override
	public String getName() {
		return "Layout Diagram";
	}
	
	@Override
	public boolean isAvailable(final IContext context) {
		final ICustomContext customCtx = (ICustomContext)context;
		// Only make the feature available if the user is right clicking on the outer diagram.
		return customCtx.getPictogramElements().length == 1 && customCtx.getPictogramElements()[0] instanceof Diagram;
	}
	
	@Override
	public boolean canExecute(final ICustomContext context) {
		return true;
	}

	@Override
	public boolean canUndo(IContext context) {
		return false;
	}

	// Helper method to create a context for executing the feature
	public static ICustomContext createContext(final boolean relayoutShapes) {
		final ICustomContext context = new CustomContext();
		context.putProperty(relayoutShapesPropertyKey, relayoutShapes);
		return context;
	}
		
	@Override
	public void execute(final ICustomContext context) {
		boolean relayoutShapes = !Boolean.FALSE.equals(context.getProperty(relayoutShapesPropertyKey)); // Defaults to true
		layout(getDiagram(), relayoutShapes);
	}
	
	private boolean layout(final Diagram diagram, final boolean relayoutShapes) {
		// Convert the diagram shapes to shapes used by the layout algorithm
		final List<org.osate.ge.layout.Shape> rootLayoutShapes = new ArrayList<org.osate.ge.layout.Shape>();
		final Map<Object, Object> shapeMap = new HashMap<Object, Object>(); // Map that contains a mapping from layout/diagram shapes to the other one.
		for(final Shape shape : shapeService.getNonGhostChildren(diagram)) {
			final org.osate.ge.layout.Shape newLayoutShape = createLayoutShape(shape, shapeMap, null, relayoutShapes);
			if(newLayoutShape != null) {
				rootLayoutShapes.add(newLayoutShape);
			}
		}
				
		// Don't perform any automatic layout if there is not more than 1 child shape
		if(!hasUnlockedShapes(rootLayoutShapes)) {
			return false;
		}	

		// Create layout connections
		final List<org.osate.ge.layout.Connection> layoutConnections = new ArrayList<org.osate.ge.layout.Connection>();		
		for(final Connection connection : getDiagram().getConnections()) {
			// Prevents connections such as binding connections that aren't handled properly from being used during the layout process
			if(bor.getBusinessObjectForPictogramElement(connection) != null) {
				final org.osate.ge.layout.Shape startLayoutShape = getLayoutShape(connection.getStart(), shapeMap);
				final org.osate.ge.layout.Shape endLayoutShape = getLayoutShape(connection.getEnd(), shapeMap);
				if(startLayoutShape != null && endLayoutShape != null && startLayoutShape != endLayoutShape) {
					layoutConnections.add(new org.osate.ge.layout.Connection(startLayoutShape, endLayoutShape));
					
					// Remove all bendpoints because the layout algorithm assumes that all connections are straight lines
					if(connection instanceof FreeFormConnection) {
						final FreeFormConnection ffc = (FreeFormConnection)connection;
						ffc.getBendpoints().clear();
					}
				}
			}			
		}
	
		//final LayoutAlgorithm layoutAlg = new MonteCarloLayoutAlgorithm();
		final LayoutAlgorithm layoutAlg = new SimpleLayoutAlgorithm();
		layoutAlg.layout(rootLayoutShapes, layoutConnections);

		// Update the diagram's shapes
		for(final org.osate.ge.layout.Shape layoutShape : rootLayoutShapes) {
			updateShape(layoutShape, shapeMap);
		}
		
		return true;
	}
	
	private org.osate.ge.layout.Shape createLayoutShape(final Shape diagramShape, Map<Object, Object> shapeMap, final org.osate.ge.layout.Shape parentLayoutShape, final boolean relayoutShapes) {
		// Restrict what shapes are positioned
		final Object bo = bor.getBusinessObjectForPictogramElement(diagramShape);

		// Determine whether the shape may be moved 			
		// Don't change the position of shapes that have already been positioned if not repositioning all shapes
		final boolean locked;
		if(bo == null || propertyService.isManuallyPositioned(diagramShape) || (propertyService.isLayedOut(diagramShape) && !relayoutShapes)) {
			locked = true;
		} else {
			locked = false;
		}
		
		final boolean positionOnEdge = bo instanceof Feature || bo instanceof InternalFeature || bo instanceof ProcessorFeature;
		
		// Create the layout shape
		final GraphicsAlgorithm ga = diagramShape.getGraphicsAlgorithm();
		final IResizeShapeContext resizeContext = createNoOpResizeShapeContext(diagramShape);
		final IResizeShapeFeature resizeFeature = getFeatureProvider().getResizeShapeFeature(resizeContext);
		final boolean canResize = resizeFeature != null && resizeFeature.canResizeShape(resizeContext);
		final org.osate.ge.layout.Shape newLayoutShape = new org.osate.ge.layout.Shape(parentLayoutShape, ga.getX(), ga.getY(), ga.getWidth(), ga.getHeight(), canResize, locked, positionOnEdge);
		final IResizeConfiguration resizeConfiguration = resizeFeature == null ? null : resizeFeature.getResizeConfiguration(resizeContext);
		if(resizeConfiguration instanceof AgeResizeConfiguration) {
			final AgeResizeConfiguration ageConf = (AgeResizeConfiguration)resizeConfiguration;
			if(ageConf.hasMinimumSize()) {
				newLayoutShape.setMinimumSize(ageConf.getMinimumWidth(), ageConf.getMinimumHeight());
			}
		} else {
			newLayoutShape.setMinimumSize(50, 50);
		}

		shapeMap.put(newLayoutShape, diagramShape);
		shapeMap.put(diagramShape, newLayoutShape);

		// Don't layout shapes that are inside locked shapes
		// If bo is not null and it isn't manually positioned, then create layout shapes of its children
		if(bo != null && !propertyService.isManuallyPositioned(diagramShape)) {
			// Create layout shape for the diagram shape's children
			if(diagramShape instanceof ContainerShape) {
				final List<Shape> children = shapeService.getNonGhostChildren((ContainerShape)diagramShape);
				for(final Shape child : children) {
					createLayoutShape(child, shapeMap, newLayoutShape, relayoutShapes);
				}		
			}
		}

		return newLayoutShape;
	}
	
	private boolean hasUnlockedShapes(final List<org.osate.ge.layout.Shape> shapes) {
		for(final org.osate.ge.layout.Shape shape : shapes) {
			if(shape.isUnlocked() || hasUnlockedShapes(shape.getChildren())) {
				return true;
			}
		}
		
		return false;
	}
	
	/** 
	 * Creates a resize shape context that uses the shapes existing location and size. Useful for determining if resizing is supported.
	 * @param shape
	 * @return
	 */
	private IResizeShapeContext createNoOpResizeShapeContext(final Shape shape) {
		final GraphicsAlgorithm ga = shape.getGraphicsAlgorithm();
		if(ga == null) {
			return null;
		}
		
		final ResizeShapeContext context = new ResizeShapeContext(shape);
		context.setSize(ga.getWidth(), ga.getHeight());
		context.setLocation(ga.getX(), ga.getY());
		
		return context;
	}
	
	private void updateShape(final org.osate.ge.layout.Shape layoutShape, final Map<Object, Object> shapeMap) {
		for(final org.osate.ge.layout.Shape childLayoutShape : layoutShape.getChildren()) {
			updateShape(childLayoutShape, shapeMap);
		}

		final Shape diagramShape = (Shape)shapeMap.get(layoutShape);
		if(layoutShape.isUnlocked()) {
			final ResizeShapeContext context = new ResizeShapeContext(diagramShape);
			context.setSize(layoutShape.getWidth(), layoutShape.getHeight());
			context.setLocation(layoutShape.getX(), layoutShape.getY());
			
			// Try to resize the shape
			final IResizeShapeFeature feature = getFeatureProvider().getResizeShapeFeature(context);
			if(feature != null && feature.canResizeShape(context)) {
				feature.resizeShape(context);
			} else {
				// Try simply moving the shape
				move(diagramShape, layoutShape.getX(), layoutShape.getY());
			}
			
			propertyService.setIsLayedOut(diagramShape, true);
		}
		
		// Reposition docked shapes after the container has layed out to ensure the shapes are docked to the appropriate edge
		for(final org.osate.ge.layout.Shape childLayoutShape : layoutShape.getChildren()) {
			if(childLayoutShape.positionOnEdge()) {
				updateShape(childLayoutShape, shapeMap);
			}
		}
		
		// Check the diagram shape's size and container size?
		if(!(diagramShape instanceof Diagram) && diagramShape instanceof ContainerShape) {
			resizeHelper.checkShapeBounds((ContainerShape)diagramShape);
		}
	}
	
	private void move(final Shape shape, final int x, final int y) {
		final MoveShapeContext context = new MoveShapeContext(shape);
		context.setLocation(x, y);
		context.setSourceContainer(shape.getContainer());
		context.setTargetContainer(shape.getContainer());
		
		// Move the shape
		final IMoveShapeFeature feature = getFeatureProvider().getMoveShapeFeature(context);
		if(feature != null && feature.canMoveShape(context)) {
			feature.moveShape(context);
		}
	}
	
	/**
	 * Gets the layout shape that is the closest match to the specified anchor
	 * @param anchor
	 * @param shapeToNodeMap
	 * @return
	 */
	private static org.osate.ge.layout.Shape getLayoutShape(final Anchor anchor, final Map<Object, Object> shapeMap) {
		if(anchor != null && anchor.getParent() instanceof Shape) {
			Shape diagramShape = (Shape)anchor.getParent();
			while(diagramShape != null) {
				final org.osate.ge.layout.Shape layoutShape = (org.osate.ge.layout.Shape)shapeMap.get(diagramShape);
				if(layoutShape != null) {
					return layoutShape;
				}

				diagramShape = diagramShape.getContainer();
			}
		}
		return null;	
				
	}
	
}
