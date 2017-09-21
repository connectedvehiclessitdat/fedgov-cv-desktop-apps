package gov.usdot.desktop.apps.renderer;

import gov.usdot.desktop.apps.data.GeoPoint;

public interface GeoPointRenderer {

	public void render(GeoPoint... points);
	public void render(String message);

}
