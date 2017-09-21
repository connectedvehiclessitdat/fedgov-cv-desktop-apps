package gov.usdot.desktop.apps.renderer;

import gov.usdot.desktop.apps.data.GeoPoint;

public class ConsoleGeoPointRenderer implements GeoPointRenderer {

	public void render(GeoPoint... points) {
		for (GeoPoint point: points) {
			System.out.println(point);
		}
	}
	
	public void render(String message) {
		System.out.println(message);
	}

}
