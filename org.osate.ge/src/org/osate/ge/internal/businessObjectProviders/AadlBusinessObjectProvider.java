package org.osate.ge.internal.businessObjectProviders;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Named;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.osate.aadl2.AadlPackage;
import org.osate.aadl2.AnnexLibrary;
import org.osate.aadl2.AnnexSubclause;
import org.osate.aadl2.BehavioredImplementation;
import org.osate.aadl2.Classifier;
import org.osate.aadl2.ComponentClassifier;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.ComponentPrototype;
import org.osate.aadl2.ComponentType;
import org.osate.aadl2.DefaultAnnexLibrary;
import org.osate.aadl2.FeatureGroup;
import org.osate.aadl2.FeatureGroupType;
import org.osate.aadl2.GroupExtension;
import org.osate.aadl2.ImplementationExtension;
import org.osate.aadl2.ModeTransition;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.PackageSection;
import org.osate.aadl2.Realization;
import org.osate.aadl2.Subcomponent;
import org.osate.aadl2.SubprogramAccess;
import org.osate.aadl2.SubprogramCall;
import org.osate.aadl2.SubprogramCallSequence;
import org.osate.aadl2.SubprogramClassifier;
import org.osate.aadl2.SubprogramImplementation;
import org.osate.aadl2.SubprogramProxy;
import org.osate.aadl2.SubprogramSubcomponent;
import org.osate.aadl2.SubprogramSubcomponentType;
import org.osate.aadl2.SubprogramType;
import org.osate.aadl2.TypeExtension;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.ConnectionInstance;
import org.osate.aadl2.instance.ConnectionReference;
import org.osate.aadl2.instance.FeatureInstance;
import org.osate.ge.BusinessObjectContext;
import org.osate.ge.di.Activate;
import org.osate.ge.di.Names;
import org.osate.ge.internal.model.SubprogramCallOrder;
import org.osate.ge.internal.services.ExtensionService;
import org.osate.ge.internal.util.AadlFeatureUtil;
import org.osate.ge.internal.util.AadlHelper;
import org.osate.ge.internal.util.AadlSubcomponentUtil;

public class AadlBusinessObjectProvider {
	@Activate
	public Stream<?> getBusinessObjects(final @Named(Names.BUSINESS_OBJECT_CONTEXT) BusinessObjectContext boc,
			final ExtensionService extService) {
		final Object bo = boc.getBusinessObject();
		if(bo instanceof AadlPackage) {
			return getChildren((AadlPackage)bo, extService);
		} else if(bo instanceof Classifier) {
			return getChildren((Classifier)bo, true);
		} else if(bo instanceof FeatureGroup) {
			final FeatureGroupType fgt = AadlFeatureUtil.getFeatureGroupType(boc, (FeatureGroup)bo);
			return fgt == null ? null : AadlFeatureUtil.getAllFeatures(fgt).stream();
		} else if(bo instanceof Subcomponent) {
			return getChildren((Subcomponent)bo, boc);
		} else if(bo instanceof SubprogramCall) {
			return getChildren((SubprogramCall)bo);
		} else if(bo instanceof SubprogramCallSequence) {
			return getChildren((SubprogramCallSequence)bo);
		} else if(bo instanceof ModeTransition) {
			return ((ModeTransition)bo).getOwnedTriggers().stream();
		} else if(bo instanceof ComponentInstance) {
			return getChildren((ComponentInstance)bo);
		} else if(bo instanceof FeatureInstance) {
			return ((FeatureInstance)bo).getFeatureInstances().stream();
		}
		
		return null;
	}
	
	// Declarative Model
	@Activate
	public static Stream<Object> getChildren(final AadlPackage pkg, final ExtensionService extService) {
		// Build a list of all named elements in the public and private sections of the package
		final Set<Object> children = new HashSet<>();
		populateChildren(pkg, pkg.getPublicSection(), children, extService);
		populateChildren(pkg, pkg.getPrivateSection(), children, extService);	
		
		return children.stream();
	}
	
	private static void populateChildren(final AadlPackage pkg, final PackageSection ps, final Set<Object> children, final ExtensionService extService) {
		if(ps == null) {
			return;
		}
		
		children.addAll(ps.getOwnedClassifiers());
		
		for(final AnnexLibrary annexLibrary : ps.getOwnedAnnexLibraries()) {
			//children.addAll(ps.getOwnedAnnexLibraries());
			final NamedElement parsedAnnexLibrary = getParsedAnnexLibrary(annexLibrary);
			final boolean specializedHandling = parsedAnnexLibrary != null && extService.getApplicableBusinessObjectHandler(parsedAnnexLibrary) != null;

			// Create the generic shape if specialized handling wasn't used
			if(specializedHandling) {
				children.add(parsedAnnexLibrary);
			} else {
				children.add(annexLibrary);
			}
		}
	}
	
	private static NamedElement getParsedAnnexLibrary(final NamedElement annexLibrary) {
		if(annexLibrary instanceof DefaultAnnexLibrary) {
			final NamedElement parsedLib = ((DefaultAnnexLibrary) annexLibrary).getParsedAnnexLibrary();
			
			// Don't return libraries which inherit from DefaultAnnexLibrary
			if(parsedLib instanceof DefaultAnnexLibrary) {
				return null;
			}
			
			return parsedLib;
		}
		
		return null;
	}
	
	private static Stream<?> getChildren(final Subcomponent sc, 
			final BusinessObjectContext scBoc) {
		final ComponentClassifier cc = AadlSubcomponentUtil.getComponentClassifier(scBoc, sc);
		if(cc == null) {
			return null;
		}
		
		return getChildren(cc, false);
	}
	
	private static Stream<?> getChildren(final SubprogramCall call) {
		final SubprogramType subprogramType = getSubprogramType(call);
		if(subprogramType != null) {
			return Stream.concat(AadlFeatureUtil.getAllDeclaredFeatures(subprogramType).stream(), subprogramType.getAllFlowSpecifications().stream());
		}
		
		return null;
	}
	
	private static Stream<?> getChildren(final SubprogramCallSequence cs) {
		return Stream.concat(cs.getOwnedSubprogramCalls().stream(), SubprogramCallOrder.getSubprogramCallOrders(cs.getOwnedSubprogramCalls()).stream());
	}
	
	/**
	 * Finds and returns the SubprogramType associated with the specified model element.
	 * @param element
	 * @return
	 */
	private static SubprogramType getSubprogramType(final EObject element) {
		if(element instanceof SubprogramType) {
			return (SubprogramType)element;
		} else if(element instanceof SubprogramCall) {
			return getSubprogramType(((SubprogramCall) element).getCalledSubprogram());
		} else if(element instanceof SubprogramImplementation) {
			return ((SubprogramImplementation)element).getType();
		} else if(element instanceof SubprogramSubcomponent) {
			return getSubprogramType(((SubprogramSubcomponent) element).getSubprogramSubcomponentType());
		} else if(element instanceof SubprogramAccess) {
			final SubprogramSubcomponentType scType = ((SubprogramAccess) element).getSubprogramFeatureClassifier();
			return getSubprogramType(scType);
		} else if(element instanceof SubprogramProxy) {
			final SubprogramClassifier scClassifier = ((SubprogramProxy) element).getSubprogramClassifier();
			return getSubprogramType(scClassifier);
		} else if(element instanceof ComponentPrototype) {
			final ComponentClassifier constrainingClassifier = ((ComponentPrototype) element).getConstrainingClassifier();
			return getSubprogramType(constrainingClassifier);
		} else {
			// Unsupported case. Possibly prototype
			return null;
		}
	}
	
	private static Stream<?> getChildren(final Classifier classifier, boolean includeGeneralizations) {
		Stream<?> children = Stream.empty();
		
		// Shapes
		children = Stream.concat(children, AadlFeatureUtil.getAllDeclaredFeatures(classifier).stream());
		
		if(classifier instanceof ComponentImplementation) {
			final ComponentImplementation ci = (ComponentImplementation)classifier;
			children = Stream.concat(children, AadlHelper.getAllInternalFeatures(ci).stream());
			children = Stream.concat(children, AadlHelper.getAllProcessorFeatures(ci).stream());
			children = Stream.concat(children, ci.getAllSubcomponents().stream());
		}
		
		if(classifier instanceof BehavioredImplementation) {
			children = Stream.concat(children, AadlHelper.getAllSubprogramCallSequences((BehavioredImplementation)classifier).stream());
		}
		
		if(classifier instanceof ComponentClassifier) {
			children = Stream.concat(children, ((ComponentClassifier)classifier).getAllModes().stream());
		}
		
		children = Stream.concat(children, getAllDefaultAnnexSubclauses(classifier).stream());

		// Connections
		if(classifier instanceof ComponentClassifier) {
			children = Stream.concat(children, ((ComponentClassifier)classifier).getAllModeTransitions().stream());
		}
		
		if(classifier instanceof ComponentImplementation) {
			children = Stream.concat(children, ((ComponentImplementation)classifier).getAllConnections().stream());
		}
		
		final ComponentType componentType;
		if(classifier instanceof ComponentType) {
			componentType = (ComponentType)classifier;
		} else if(classifier instanceof ComponentImplementation) {
			componentType = ((ComponentImplementation)classifier).getType();
		} else {
			componentType = null;
		}
		
		if(componentType != null) {			
			children = Stream.concat(children, componentType.getAllFlowSpecifications().stream());
		}
	
		// Add generalizations
		if(includeGeneralizations) {
			if(classifier instanceof ComponentType) {
				final ComponentType ct = ((ComponentType)classifier);
				final TypeExtension te = ct.getOwnedExtension();
				if(te != null) {
					children = Stream.concat(children, Stream.of(te));
				}
			} else if(classifier instanceof ComponentImplementation) {
				final ComponentImplementation componentImplementation = ((ComponentImplementation)classifier);
	
				// Implementation Extension
				final ImplementationExtension ie = componentImplementation.getOwnedExtension();
				if(ie != null) {
					children = Stream.concat(children, Stream.of(ie));				
				}
				
				// Realization
				final Realization realization = componentImplementation.getOwnedRealization();
				if(realization != null) {	
					children = Stream.concat(children, Stream.of(realization));			
				}				
			} else if(classifier instanceof FeatureGroupType) {
				final FeatureGroupType featureGroupType = ((FeatureGroupType)classifier);
				final GroupExtension ge = featureGroupType.getOwnedExtension();
				if(ge != null) {
					children = Stream.concat(children, Stream.of(ge));
				}
			}
		}
		
		return children;
	}
	
	/**
	 * Returns all the default annex subclauses owned by a classifier or any extended or implemented classifiers.
	 * @param topClassifier
	 * @return
	 */
	private static EList<AnnexSubclause> getAllDefaultAnnexSubclauses(final Classifier topClassifier) {
		final EList<AnnexSubclause> result = new BasicEList<AnnexSubclause>();
		if(topClassifier == null) {
			return result;
		}
		
		final EList<Classifier> classifiers = topClassifier.getSelfPlusAllExtended();
		if (topClassifier instanceof ComponentImplementation) {
			ComponentType ct = ((ComponentImplementation) topClassifier).getType();
			final EList<Classifier> tclassifiers = ct.getSelfPlusAllExtended();
			classifiers.addAll(tclassifiers);
		}
		
		for (Classifier classifier : classifiers) {
			result.addAll(classifier.getOwnedAnnexSubclauses());
		}
		return result;
	}
	
	// Instance Model
	private static Stream<?> getChildren(ComponentInstance ci) {
		Stream.Builder<Object> connectionReferenceStreamBuilder = Stream.builder();
		for(final ConnectionInstance connectionInstance : ci.getConnectionInstances()) {
			for(final ConnectionReference cr : connectionInstance.getConnectionReferences()) {
				connectionReferenceStreamBuilder.add(cr);
			}
		}	

		return Stream.concat(Stream.concat(ci.getComponentInstances().stream(), 
				ci.getFeatureInstances().stream()),
				connectionReferenceStreamBuilder.build());
	}
}
