package org.osate.ge.internal.diagram.updating;

import java.util.Objects;
import org.osate.ge.internal.AgeGraphicalConfiguration;
import org.osate.ge.internal.businessObjectHandlers.BusinessObjectHandlerProvider;
import org.osate.ge.internal.diagram.DiagramElement;
import org.osate.ge.internal.services.ExtensionService;
import org.osate.ge.internal.services.ReferenceService;
import org.osate.ge.internal.util.BusinessObjectContextHelper;

/**
 * DiagramElementGraphicalConfigurationProvider implementation that uses business object handlers.
 *
 */
public class DefaultDiagramElementGraphicalConfigurationProvider implements DiagramElementInformationProvider, AutoCloseable {
	private final BusinessObjectHandlerProvider bohProvider;
	private final BusinessObjectContextHelper bocHelper;
	
	public DefaultDiagramElementGraphicalConfigurationProvider(final ReferenceService refService,
			final ExtensionService extService) {
		this.bohProvider = Objects.requireNonNull(extService, "extService must not be null");
		this.bocHelper = new BusinessObjectContextHelper(extService);
	}
	
	@Override
	public void close() {
		bocHelper.close();
	}
	
	@Override
	public String getName(final DiagramElement element) {
		final Object boh = element.getBusinessObjectHandler();
		return bocHelper.getName(element, boh);
	}
	
	@Override
	public AgeGraphicalConfiguration getGraphicalConfiguration(final DiagramElement element) {
		final Object boh = element.getBusinessObjectHandler();
		return bocHelper.getGraphicalConfiguration(element, boh);
	}
	
	@Override
	public Object getApplicableBusinessObjectHandler(final Object bo) {
		return bohProvider.getApplicableBusinessObjectHandler(bo);
	}
}
