package gov.usdot.desktop.apps.provider;

import net.sf.json.JSONObject;
import gov.usdot.desktop.apps.data.GeoPoint;
import gov.usdot.desktop.apps.renderer.GeoPointRenderer;

public class SimpleGeoPointProvider implements GeoPointProvider {

    final private GeoPointRenderer renderer;
    
    private boolean started = false;
    private boolean paused = false;

    public SimpleGeoPointProvider(GeoPointRenderer renderer) {
    	this(renderer, null);
    }
    
    public SimpleGeoPointProvider(GeoPointRenderer renderer, JSONObject config) {
        this.renderer = renderer;
    }
    
    final GeoPoint geoPoints[] = new GeoPoint[]{
        new GeoPoint(42.732594, -84.552243),
        new GeoPoint(42.732577, -84.550647),
        new GeoPoint(42.733595, -84.550612),
        new GeoPoint(42.733596, -84.550221),
        new GeoPoint(42.733598, -84.549995),
        new GeoPoint(42.733596, -84.549206),
        new GeoPoint(42.733590, -84.548219),
        new GeoPoint(42.733596, -84.547764),
        new GeoPoint(42.733596, -84.546571),
        new GeoPoint(42.733133, -84.546495),
        new GeoPoint(42.729911, -84.545779),
        new GeoPoint(42.728270, -84.545364),
        new GeoPoint(42.727831, -84.545251),
        new GeoPoint(42.727686, -84.545223),
        new GeoPoint(42.727137, -84.545008),
        new GeoPoint(42.726921, -84.544928),
        new GeoPoint(42.726942, -84.544727),
        new GeoPoint(42.726936, -84.543868),
        new GeoPoint(42.726885, -84.543602),
        new GeoPoint(42.726759, -84.543370),
        new GeoPoint(42.726570, -84.543216),
        new GeoPoint(42.726343, -84.543173),
        new GeoPoint(42.726141, -84.543258),
        new GeoPoint(42.725990, -84.543430),
        new GeoPoint(42.725923, -84.543595),
        new GeoPoint(42.725820, -84.544072),
        new GeoPoint(42.725829, -84.544602),
        new GeoPoint(42.725857, -84.547339),
        new GeoPoint(42.725815, -84.549200),
        new GeoPoint(42.725980, -84.550868),
        new GeoPoint(42.726012, -84.551658),
        new GeoPoint(42.726024, -84.552688),
        new GeoPoint(42.726044, -84.556148),
        new GeoPoint(42.726066, -84.560199),
        new GeoPoint(42.726449, -84.565617),
        new GeoPoint(42.726457, -84.567287),
        new GeoPoint(42.726468, -84.567949),
        new GeoPoint(42.726468, -84.568098),
        new GeoPoint(42.726480, -84.569770),
        new GeoPoint(42.726494, -84.570388),
        new GeoPoint(42.726506, -84.570820),
        new GeoPoint(42.726560, -84.572777),
        new GeoPoint(42.727537, -84.572802),
        new GeoPoint(42.728804, -84.572770),
        new GeoPoint(42.728817, -84.572445),
        new GeoPoint(42.728811, -84.571661),
        new GeoPoint(42.730118, -84.571617),
        new GeoPoint(42.730117, -84.569190),
        new GeoPoint(42.730088, -84.567251),
        new GeoPoint(42.730091, -84.566860),
        new GeoPoint(42.730065, -84.564629),
        new GeoPoint(42.730059, -84.563030),
        new GeoPoint(42.730056, -84.562202),
        new GeoPoint(42.730054, -84.561591),
        new GeoPoint(42.730043, -84.560915),
        new GeoPoint(42.730030, -84.560059),
        new GeoPoint(42.730009, -84.558548),
        new GeoPoint(42.730020, -84.557011),
        new GeoPoint(42.730020, -84.556961),
        new GeoPoint(42.730018, -84.555480),
        new GeoPoint(42.730017, -84.555371),
        new GeoPoint(42.729996, -84.553927),
        new GeoPoint(42.729957, -84.552269),
        new GeoPoint(42.731333, -84.552252),
        new GeoPoint(42.732135, -84.552246),};

    public void init() {
    }

    public void dispose() {
    }
    
    private void nap(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
        }
    }

    public void start() {
        started = true;
        for (int i = 0; i < geoPoints.length && started; i++) {
            while( started && paused ) 
                nap(200);
            GeoPoint point = geoPoints[i];
            renderer.render(point);
            nap(1000);
            System.out.println(String.format("Current position: lat %f, lon %f", point.lat, point.lon));
        }
        stop();
    }

    public void stop() {
        renderer.render(GeoPoint.endOfData);
        started = false;
        paused = false;
    }

    public void pause() {
        if ( started )
            paused = true;
    }

    public void resume() {
        if ( started && paused )
            paused = false;
    }

    public boolean canPause() {
        return true;
    }

    public boolean canStop() {
        return true;
    }

	public void region(double nw_lat, double nw_lon, double se_lat, double se_lon) {		
	}

	public void save(String configFilePath) throws InitializationException {		
	}	    
}
