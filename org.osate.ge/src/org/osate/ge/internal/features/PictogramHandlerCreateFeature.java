package org.osate.ge.internal.features;

import java.util.Objects;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.graphiti.features.ICustomUndoRedoFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IMoveShapeFeature;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICreateContext;
import org.eclipse.graphiti.features.context.impl.MoveShapeContext;
import org.eclipse.graphiti.features.impl.AbstractCreateFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.osate.ge.internal.Categorized;
import org.osate.ge.internal.SimplePaletteEntry;
import org.osate.ge.di.CanCreate;
import org.osate.ge.di.CreateBusinessObject;
import org.osate.ge.di.GetCreateOwningBusinessObject;
import org.osate.ge.di.Names;
import org.osate.ge.internal.services.AadlModificationService;
import org.osate.ge.internal.services.BusinessObjectResolutionService;
import org.osate.ge.internal.services.ExtensionService;
import org.osate.ge.internal.services.ShapeService;
import org.osate.ge.internal.services.AadlModificationService.AbstractModifier;

// ICreateFeature implementation that delegates behavior to a pictogram handler
public class PictogramHandlerCreateFeature extends AbstractCreateFeature implements Categorized, ICustomUndoRedoFeature {
	private final BusinessObjectResolutionService bor;
	private final ExtensionService extService;
	private final AadlModificationService aadlModService;
	private final ShapeService shapeService;
	private final SimplePaletteEntry paletteEntry;
	private final Object handler;
	
	public PictogramHandlerCreateFeature(final BusinessObjectResolutionService bor, final ExtensionService extService, final AadlModificationService aadlModService, 
			final ShapeService shapeService, final IFeatureProvider fp, final SimplePaletteEntry paletteEntry, final Object pictogramHandler) {
		super(fp, paletteEntry.getLabel(), "");
		this.bor = Objects.requireNonNull(bor, "bor must not be null");
		this.extService = Objects.requireNonNull(extService, "extService must not be null");
		this.aadlModService = Objects.requireNonNull(aadlModService, "aadlModService must not be null");
		this.shapeService = Objects.requireNonNull(shapeService, "shapeService must not be null");
		this.paletteEntry = Objects.requireNonNull(paletteEntry, "paletteEntry must not be null");
		this.handler = Objects.requireNonNull(pictogramHandler, "pictogramHandler must not be null");
	}

	@Override
	public String getCategory() {
		return paletteEntry.getCategory();
	}

	@Override
	public String getCreateImageId() {
		return paletteEntry.getImageId();
	}
	
	@Override
	public boolean canCreate(final ICreateContext context) {
		final Object containerBo = bor.getBusinessObjectForPictogramElement(context.getTargetContainer());
		if(containerBo == null) {
			return false;
		}
		
		final IEclipseContext eclipseCtx = extService.createChildContext();
		try {
			eclipseCtx.set(Names.PALETTE_ENTRY_CONTEXT, paletteEntry.getContext());
			eclipseCtx.set(Names.CONTAINER_BO, containerBo);
			return (boolean)ContextInjectionFactory.invoke(handler, CanCreate.class, eclipseCtx);
		} finally {
			eclipseCtx.dispose();
		}
	}
	
	@Override
	public Object[] create(final ICreateContext context) {		
		final EObject ownerBo = getOwnerBo(context.getTargetContainer());
		if(ownerBo == null) {
			return EMPTY;
		}

		// Modify the AADL model
		final Object newBo = aadlModService.modify(ownerBo, new AbstractModifier<EObject, Object>() {
			@Override
			public Object modify(Resource resource, EObject ownerBo) {
				final IEclipseContext eclipseCtx = extService.createChildContext();
				try {
					eclipseCtx.set(Names.PALETTE_ENTRY_CONTEXT, paletteEntry.getContext());
					eclipseCtx.set(Names.OWNER_BO, ownerBo);
					final Object newBo = ContextInjectionFactory.invoke(handler, CreateBusinessObject.class, eclipseCtx);
					return newBo == null ? EMPTY : newBo;
				} finally {
					eclipseCtx.dispose();
				}
			}
		});
		
		final Shape newShape = shapeService.getChildShapeByReference(context.getTargetContainer(), newBo);
		if(newShape != null) {
			// Move the shape to the desired position
			final MoveShapeContext moveShapeCtx = new MoveShapeContext(newShape);
			moveShapeCtx .setLocation(context.getX(), context.getY());
			moveShapeCtx .setSourceContainer(newShape.getContainer());
			moveShapeCtx .setTargetContainer(newShape.getContainer());
			
			final IMoveShapeFeature feature = getFeatureProvider().getMoveShapeFeature(moveShapeCtx);
			if(feature != null && feature.canMoveShape(moveShapeCtx)) {
				feature.moveShape(moveShapeCtx);
			}
		}
		
		return newBo == null ? EMPTY : new Object[] {newBo};
	}
	
	private EObject getOwnerBo(final PictogramElement container) {
		final IEclipseContext eclipseCtx = extService.createChildContext();
		try {
			eclipseCtx.set(Names.PALETTE_ENTRY_CONTEXT, paletteEntry.getContext());
			eclipseCtx.set(Names.CONTAINER_BO, bor.getBusinessObjectForPictogramElement(container));
			final EObject ownerBo = (EObject) ContextInjectionFactory.invoke(handler, GetCreateOwningBusinessObject.class, eclipseCtx, null);
			if(ownerBo != null) {
				return (EObject)ownerBo;
			}
		} finally {
			eclipseCtx.dispose();
		}
		
		return (EObject)bor.getBusinessObjectForPictogramElement(container);
	}
	
	// ICustomUndoRedoFeature
	@Override
	public boolean canUndo(final IContext context) {
		return false;
	}
	
	@Override
	public void preUndo(IContext context) {
	}

	@Override
	public void postUndo(IContext context) {
	}

	@Override
	public boolean canRedo(IContext context) {
		return false;
	}

	@Override
	public void preRedo(IContext context) {
	}

	@Override
	public void postRedo(IContext context) {
	}
}
