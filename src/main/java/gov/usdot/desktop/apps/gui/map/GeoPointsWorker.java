package gov.usdot.desktop.apps.gui.map;

import gov.usdot.desktop.apps.data.GeoPoint;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.SwingWorker;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;

public class GeoPointsWorker extends SwingWorker<Boolean, GeoPoint> {

    final private JMapViewer mapViewer;
    final private GeoPointsView mainView;
    final private BlockingQueue<GeoPoint> inputQueue;
    final private BlockingQueue<Marker> removeQueue = new LinkedBlockingQueue<Marker>();
    final private Color markerColor;
    final private int markerTTL;
    private boolean forceMarkerVisible = false;

	class Marker {
        Marker(MapMarkerDot marker) {
            this.marker = marker;
            this.addedAt = System.currentTimeMillis();
        }
        final MapMarkerDot marker;
        final long addedAt;
    }
    
    GeoPointsWorker(GeoPointsMapper geoPointsMapper) {
        this(geoPointsMapper, Color.green, 1000);
    }

    GeoPointsWorker(GeoPointsMapper geoPointsMapper, Color markerColor, int markerTTL) {
        assert (geoPointsMapper != null);
        this.markerColor = markerColor;
        this.markerTTL = markerTTL;
        mainView = geoPointsMapper.getMainView();
        mapViewer = geoPointsMapper.getMapViewer();
        inputQueue = mainView.getInputQueue();
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        while( true ) {
            GeoPoint point = inputQueue.take();
            publish(point);
            if ( point.isEndOfData() )
                break;
        }
        return true;
    }

    // Can safely update the GUI from this method.
    @Override
    protected void done() {
        try {
            // Retrieve the return value of doInBackground.
            if (get() == false) {
                System.out.println("Worker thread completed unsucceessfully");
            }
            mainView.stop();
            while( removeExpiredMarkers() )
                Thread.sleep((markerTTL+1)/2);
        } catch (InterruptedException e) {
            System.out.println(String.format("Worker thread was interrupted. Reason: ", e.getMessage()));
        } catch (ExecutionException e) {
            System.out.println(String.format("Worker thread got execution exception. Reason: ", e.getMessage()));
        }
    }
    
    // Can safely update the GUI from this method.
    @Override
    protected void process(List<GeoPoint> points) {
        removeExpiredMarkers();
        for (GeoPoint point : points) {
            if ( point == null || point.isFlushData() )
                continue;
            if ( point.isEndOfData() )
                return;
            MapMarkerDot marker = new MapMarkerDot(markerColor, point.lat, point.lon);
            marker.setBackColor(markerColor);
            mapViewer.addMapMarker(marker);
            if ( isForceMarkerVisible() )
            	makeMarkerVisible(point.lat, point.lon);
        	if ( markerTTL > 0 ) {           
	            try {
	                removeQueue.put(new Marker(marker));
	            } catch (InterruptedException e) {
	                System.out.println(String.format("Worker thread was interrupted. Reason: ", e.getMessage()));
	            }
        	}
        }
    }
    
    protected boolean removeExpiredMarkers() {
        long currentTime = System.currentTimeMillis();
        while( true ) {
            Marker marker = removeQueue.peek();
            if ( marker == null )
                return false;
            if ( (marker.addedAt + this.markerTTL) > currentTime )
                return true;
            removeQueue.remove(marker);
            mapViewer.removeMapMarker(marker.marker);
        }
    }
    
    public boolean isForceMarkerVisible() {
		return forceMarkerVisible;
	}

	public void setForceMarkerVisible(boolean forceMarkerVisible) {
		this.forceMarkerVisible = forceMarkerVisible;
	}

    protected void makeMarkerVisible(double lat, double lon) {
        if ( mapViewer.getMapPosition(lat, lon) == null )
            mapViewer.setDisplayPositionByLatLon(lat,lon, mapViewer.getZoom());
    }

}
