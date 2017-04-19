package org.osate.ge.internal.businessObjectHandlers;

import javax.inject.Named;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.ge.di.GetGraphic;
import org.osate.ge.di.GetName;
import org.osate.ge.di.IsApplicable;
import org.osate.ge.di.Names;
import org.osate.ge.graphics.Graphic;
import org.osate.ge.internal.di.GetDefaultLabelConfiguration;
import org.osate.ge.internal.graphics.AadlGraphics;
import org.osate.ge.internal.labels.LabelConfiguration;
import org.osate.ge.internal.labels.LabelConfigurationBuilder;

public class ComponentInstanceHandler {	
	private static final LabelConfiguration nameLabelConfiguration = LabelConfigurationBuilder.create().top().horizontalCenter().build();
	
	@IsApplicable
	public boolean isApplicable(final @Named(Names.BUSINESS_OBJECT) ComponentInstance ci) {
		return true;
	}
		
	@GetGraphic
	public Graphic getGraphicalRepresentation(final @Named(Names.BUSINESS_OBJECT) ComponentInstance ci) {
		return AadlGraphics.getGraphic(ci.getComponentClassifier());
	}	
	
	@GetName
	public String getName(final @Named(Names.BUSINESS_OBJECT) ComponentInstance ci) {
		return ci.getFullName();
	}
	
	@GetDefaultLabelConfiguration
	public LabelConfiguration getNameLabelConfiguration() {
		return nameLabelConfiguration;
	}
}
