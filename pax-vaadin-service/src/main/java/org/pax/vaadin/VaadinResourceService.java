package org.pax.vaadin;

import org.osgi.framework.Bundle;

public interface VaadinResourceService {
	
	void addResources(Bundle bundle);

	void removeResources(Bundle bundle);
	
}
