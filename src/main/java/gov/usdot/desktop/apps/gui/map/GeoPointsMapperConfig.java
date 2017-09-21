package gov.usdot.desktop.apps.gui.map;

import gov.usdot.desktop.apps.misc.JsonHelper;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JSplitPane;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapRectangleImpl;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.MapQuestOpenAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.MapQuestOsmTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

public class GeoPointsMapperConfig {
	
	private final GeoPointsMapper app;
	private JSONObject appConfig;
	
	public GeoPointsMapperConfig(GeoPointsMapper app) {
		assert(app != null);
		this.app = app;
		this.appConfig = this.app.getGuiConfig();
	}
	
	public void save(File file) {
		JSONObject config = save();
		if ( config != null ) {
			try {
				FileUtils.writeStringToFile(file, config.toString(3));
			} catch (IOException e) {
				String msg = String.format("Couldn't save application configuration to file %s. Reason: %s", file.getAbsolutePath(), e.getMessage());
				System.err.println(msg);
				JOptionPane.showMessageDialog(app.getMainView(), msg, "Save Configuration Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	public JSONObject save() {
		GeoPointsView view = app.getMainView();
		JMapViewer map = app.getMapViewer();
		assert(view != null && map != null );

		if ( appConfig == null )
			appConfig = new JSONObject();
		// auto-start
		JSONObject autostart = new JSONObject();
		autostart.put("enable", app.isAutostartEnabled());
		autostart.put("delay", app.getAutostartDelay());
		appConfig.put("autostart", autostart);
		
		// window
		JSONObject window = new JSONObject();
		window.put("title", view.getTitle());
		
		Rectangle rect = view.getBounds();
		JSONObject bounds = new JSONObject();
		bounds.put("left", rect.getX());
		bounds.put("top", rect.getY());
		bounds.put("width", rect.getWidth());
		bounds.put("height", rect.getHeight());
		window.put("bounds", bounds);
		
		JSplitPane splitterPane = view.getSplitterPane();
		JSONObject splitter = new JSONObject();
		splitter.put("position", splitterPane.getDividerLocation());
		splitter.put("lastPos", splitterPane.getLastDividerLocation());
		splitter.put("weight", splitterPane.getResizeWeight());
		window.put("splitter", splitter);
		window.put("restore", true);
		appConfig.put("window", window);
		// map
		String source = "OsmMapnik";
		boolean showGrid = false;
		boolean showToolTip = false;
		if ( appConfig.has("map") ) {
			JSONObject m = appConfig.getJSONObject("map");
			if ( m.has("source") )
				source = m.getString("source");
			if ( m.has("show") ) {
				JSONObject show = m.getJSONObject("show");
				if ( show.has("grid"))
					showGrid = show.getBoolean("grid");
				if ( show.has("toolTip"))
					showToolTip = show.getBoolean("toolTip");
			}
		}
		JSONObject m = new JSONObject();
		m.put("source", source);
		JSONObject center = new JSONObject();
        Coordinate c = map.getPosition();
        center.put("lat", c.getLat());
        center.put("lon", c.getLon());
        m.put("center", center);
		m.put("zoom", map.getZoom());
		m.put("restore", true);
		JSONObject show = new JSONObject();;
		show.put("grid", showGrid);
		show.put("zoomCtrl", map.getZoomContolsVisible());
		show.put("toolTip", showToolTip); 
		m.put("show", show);
		appConfig.put("map", m);
		// marker
		JSONObject marker = new JSONObject();
		marker.put("forceVisible", view.isForceMarkerVisible());
		marker.put("color", view.getColor( view.getMarkerColor() ));
		marker.put("expire", view.getMarkerTTL());
		marker.put("flush", view.getMarkerFlushInterval());
		appConfig.put("marker", marker);
		// draw
		JSONObject draw = new JSONObject();
		saveMapRectangles(view, map, draw);
		saveMapMarkers(view, map, draw);
		appConfig.put("draw", draw);
		// provider
		JSONObject provider = new JSONObject();
		provider.put("className", app.getProviderClassName());
		provider.put("config", app.getProviderConfigFile());
		appConfig.put("provider", provider);
		return appConfig;
	}
	
	static private void saveMapRectangles(GeoPointsView view, JMapViewer map, JSONObject draw) {
		draw.put("singleRectangle", view.isSingleRectangle());
		List<MapRectangle> rectangles = map.getMapRectangleList();
		if ( rectangles != null && rectangles.size() > 0 ) {
			JSONArray rects = new JSONArray();
			for(MapRectangle rectangle : rectangles )
				rects.add(saveMapRectangle(view,rectangle));
			draw.put("rectangles", rects);
		}
	}
	
	static public JSONObject saveMapRectangle(GeoPointsView view, MapRectangle rectangle) {
		JSONObject rect = new JSONObject();
		rect.put("color", view.getColor(rectangle.getColor()));
		rect.put("thickness", ((BasicStroke)rectangle.getStroke()).getLineWidth());
		Coordinate tl = rectangle.getTopLeft();
		JSONObject nw = new JSONObject();
		nw.put("lat", tl.getLat());
		nw.put("lon", tl.getLon());
		rect.put("nw", nw);
		Coordinate br = rectangle.getBottomRight();
		JSONObject se = new JSONObject();
		se.put("lat", br.getLat());
		se.put("lon", br.getLon());
		rect.put("se", se);
		return rect;
	}
	
	static private void saveMapMarkers(GeoPointsView view, JMapViewer map, JSONObject draw) {
		List<MapMarker> markers = map.getMapMarkerList();
		if ( markers == null || markers.size() == 0 )
			return;
		List<MapMarker> permanentMarkers = new ArrayList<MapMarker>(markers.size());
		int markerColorRGB = view.getMarkerColor().getRGB();
		for ( MapMarker marker : markers ) {
			if ( marker.getBackColor().getRGB() != markerColorRGB )
				permanentMarkers.add(marker);
		}
		if ( permanentMarkers.size() > 0 ) {
			JSONArray marks = new JSONArray();
			for(MapMarker mark : permanentMarkers )
				marks.add(saveMapMarker(view,mark));
			draw.put("markers", marks);
		}
	}
	
	static public JSONObject saveMapMarker(GeoPointsView view, MapMarker marker) {
		JSONObject rect = new JSONObject();
		rect.put("color", view.getColor(marker.getBackColor()));
		Coordinate c = marker.getCoordinate();
		rect.put("lat", c.getLat());
		rect.put("lon", c.getLon());
		return rect;
	}
	
	public void open(File file, boolean verifyProvideChange) {
		if ( file == null ) {
			System.err.println("Invalid file value: null");
			return;
		}
		String filePath = file.getAbsolutePath();
		if ( !file.exists() ) {
			System.err.printf("File '%s' does not exist\n", filePath );
			return;
		}
		JSONObject config = JsonHelper.createJsonFromFile(filePath);
		if ( config == null ) {
			System.err.printf("File '%s' is not a valid JSON file\n", filePath );
			return;
		}
		apply(config, verifyProvideChange);
		app.setGuiConfigFile(filePath);
		app.setGuiConfig(config);
	}
	
	public void apply(JSONObject config, boolean verifyProvideChange) {
		GeoPointsView view = app.getMainView();
		JMapViewer map = app.getMapViewer();
		
		if ( config.has("autostart") ) {
			JSONObject autostart = config.getJSONObject("autostart");
			app.setAutostartEnabled(autostart.getBoolean("enable"));
			app.setAutostartDelay(autostart.getInt("delay"));
		}
		if ( config.has("window") ) {
			JSONObject window = config.getJSONObject("window");
			boolean restore = window.has("restore") ? window.getBoolean("restore") : true;
			if ( restore ) {
				if ( window.has("title") )
					view.setTitle(window.getString("title"));
				if ( window.has("bounds") ) {
					JSONObject bounds = window.getJSONObject("bounds");
					Rectangle rectangle = new Rectangle(bounds.getInt("left"), bounds.getInt("top"), bounds.getInt("width"), bounds.getInt("height"));
					app.setStartupBounds(rectangle);
				}
				if ( window.has("splitter") ) {
					JSONObject splitter = window.getJSONObject("splitter");
					JSplitPane jsp = view.getSplitterPane();
					if ( splitter.has("position") )
						jsp.setDividerLocation(splitter.getInt("position"));
					if ( splitter.has("lastPos") )
						jsp.setLastDividerLocation(splitter.getInt("lastPos"));
					if ( splitter.has("weight") )
						jsp.setResizeWeight(splitter.getDouble("weight"));
				}
			}
		}
		if ( config.has("map") ) {
			JSONObject m = config.getJSONObject("map");
			boolean restore = m.has("restore") ? m.getBoolean("restore") : true;
			if ( restore ) {
				if ( m.has("source") ) {
					String source = m.getString("source");
					TileSource tileSource = null;
					if ( source.equalsIgnoreCase("OsmMapnik") )
						tileSource = new OsmTileSource.Mapnik();
					else if ( source.equalsIgnoreCase("OsmCycleMap") )
						tileSource = new OsmTileSource.CycleMap();
					else if ( source.equalsIgnoreCase("BingAerial") )
						tileSource = new BingAerialTileSource();
					else if ( source.equalsIgnoreCase("MapQuestOsm") )
						tileSource = new MapQuestOsmTileSource();
					else if ( source.equalsIgnoreCase("MapQuestOpenAerial") )
						tileSource = new MapQuestOpenAerialTileSource();				
					if ( tileSource != null )
						map.setTileSource(tileSource);
				}
				int zoom = m.has("zoom") ? m.getInt("zoom") : map.getZoom();
				if ( m.has("center") ) {
					JSONObject center = m.getJSONObject("center");
					map.setDisplayPositionByLatLon(center.getDouble("lat"), center.getDouble("lon"), zoom);
				} else {
					map.setZoom(zoom);
				}
				if ( m.has("show") ) {
					JSONObject show = m.getJSONObject("show");
					if ( show.has("grid") )
						map.setTileGridVisible(show.getBoolean("grid"));
					if ( show.has("zoomCtrl") )
						map.setZoomContolsVisible(show.getBoolean("zoomCtrl"));
					if ( show.has("toolTip") ) {
						if ( !show.getBoolean("toolTip") )
							map.setToolTipText(null);
					}
				}
			}
		}
		if ( config.has("marker") ) {
			JSONObject marker = config.getJSONObject("marker");
			if ( marker.has("forceVisible") )
				view.setForceMarkerVisible(marker.getBoolean("forceVisible"));
			if ( marker.has("color") )
				view.setMarkerColor(view.getColor(marker.getString("color")));
			if ( marker.has("expire") )
				view.setMarkerTTL(marker.getInt("expire") );
			if ( marker.has("flush") )
				view.setMarkerFlushInterval(marker.getInt("flush"));
		}
		if ( config.has("draw") ) {
			JSONObject draw = config.getJSONObject("draw");
			if ( draw.has("singleRectangle") )
				view.setSingleRectangle(draw.getBoolean("singleRectangle"));
			// draw.put("singleRectangle", view.isSingleRectangle());
			if ( draw.has("rectangles") ) {
				for( Object rectangle :  draw.getJSONArray("rectangles"))
					if ( rectangle instanceof JSONObject )
						drawRectangle(view, map, (JSONObject)rectangle );
			}
			if ( draw.has("markers") ) {
				for( Object marker :  draw.getJSONArray("markers"))
					if ( marker instanceof JSONObject )
						drawMarker(view, map, (JSONObject)marker );
			}
		}
		if ( verifyProvideChange && config.has("provider") ) {
			JSONObject provider = config.getJSONObject("provider");
			String className = provider.getString("className");
			if ( !className.equals( app.getProviderClassName()) ) {
				System.out.println("WARNING: Provider mismatch. This open operation will not change the provider.\nYou must restart the application in order to switch providers.");
			}
		}
	}
	
	public void drawRectangle(GeoPointsView view, JMapViewer map, JSONObject rectangle) {
		assert(rectangle != null);
		if ( !rectangle.has("nw") || !rectangle.has("se") ) {
			System.err.println("Skipping rectangle without NW or SE");
			return;
		}
		JSONObject nw = rectangle.getJSONObject("nw");
		if ( !nw.has("lat") || !nw.has("lon") ) {
			System.err.println("Skipping rectangle without NW lat or lon");
			return;
		}
		Coordinate tl = new Coordinate(nw.getDouble("lat"), nw.getDouble("lon") );
		JSONObject se = rectangle.getJSONObject("se");
		if ( !se.has("lat") || !se.has("lon") ) {
			System.err.println("Skipping rectangle without SE lat or lon");
			return;
		}
		Coordinate br = new Coordinate(se.getDouble("lat"), se.getDouble("lon") );
		
		MapRectangleImpl rect = new MapRectangleImpl(tl, br);
		
		if ( rectangle.has("color") )
	        rect.setColor(view.getColor(rectangle.getString("color") ));
		
		if ( rectangle.has("thickness") )
			rect.setStroke(new java.awt.BasicStroke( (float)rectangle.getDouble("thickness") ) );

        map.addMapRectangle(rect);
	}
	
	public void drawMarker(GeoPointsView view, JMapViewer map, JSONObject marker) {
		assert(marker != null);
		
		if ( !marker.has("lat") || !marker.has("lon") ) {
			System.err.println("Skipping rectangle without lat or lon");
			return;
		}
		
		double lat = marker.getDouble("lat");
		double lon = marker.getDouble("lon");
		
		Color color = Color.gray;

		if ( marker.has("color") )
	        color = view.getColor( marker.getString("color"));

		MapMarkerDot markerDot = new MapMarkerDot(Color.black, lat, lon );
        markerDot.setBackColor(color);
        map.addMapMarker(markerDot);
	}
}
