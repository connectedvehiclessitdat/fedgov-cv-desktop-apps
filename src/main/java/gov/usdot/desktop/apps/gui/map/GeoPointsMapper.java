package gov.usdot.desktop.apps.gui.map;

import gov.usdot.cv.common.util.UnitTestHelper;
import gov.usdot.desktop.apps.gui.controls.TextAreaOutputStream;
import gov.usdot.desktop.apps.misc.JsonHelper;
import gov.usdot.desktop.apps.provider.GeoPointProvider;
import gov.usdot.desktop.apps.provider.InitializationException;
import gov.usdot.desktop.apps.renderer.GeoPointRenderer;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.swing.text.DefaultCaret;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.sf.json.JSONObject;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.openstreetmap.gui.jmapviewer.events.JMVCommandEvent;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;
import org.openstreetmap.gui.jmapviewer.JMapViewerTree;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.OsmFileCacheTileLoader;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.JMapViewerEventListener;

public class GeoPointsMapper  implements JMapViewerEventListener {
	
	static
	{
	    UnitTestHelper.initLog4j(Level.WARN);
	}
    
    private final String mapTooltip = "<html>Use left double click or mouse wheel to zoom.<br>To move the map around right-click and drag.</html>";
    private GeoPointsView view = null;
    private JMapViewerTree treeMap = null;
    
    private JSONObject guiConfig;

	private String guiConfigFile;
    private JSONObject providerConfig;
	private String providerConfigFile;
	private String providerClassName;
	private GeoPointProvider provider;
	
	private boolean autostartEnabled = false;
	private int autostartDelay = 2000;
	
	private Rectangle startupBounds = null;

	public GeoPointsMapper( boolean designMode, String guiConfigFile, String providerClassName, String providerConfigFile) throws InitializationException, ParseException {
    	
        this.guiConfigFile = guiConfigFile;
    	this.guiConfig = this.guiConfigFile != null ? JsonHelper.createJsonFromFile(this.guiConfigFile) : null;
    	this.providerClassName = providerClassName;
    	this.setProviderConfigFile(providerConfigFile);
    	this.providerConfig = providerConfigFile != null ? JsonHelper.createJsonFromFile(providerConfigFile) : null;
    	
        view = new GeoPointsView();
       
        DefaultCaret caret = (DefaultCaret)view.console.getCaret();   
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);  
        
        treeMap = new JMapViewerTree("Connected Vehicle");
        JMapViewer map = treeMap.getViewer();
        map.setPreferredSize(new Dimension(100,100));
        map.setDisplayToFitMapMarkers();
        map.setScrollWrapEnabled(true);
        view.setApplication(this);
        view.mapView.setViewportView(treeMap);
        //map.addJMVListener(this); // to enable processCommand
        
        map.setTileSource(new OsmTileSource.Mapnik());
        try {
			map.setTileLoader(new OsmFileCacheTileLoader(map));
		} catch (Exception e) {
			System.err.println("Coudn't create cache tile loader\n");
			map.setTileLoader(new OsmTileLoader(map));
		}
        map.setMapMarkerVisible(true);
        map.setTileGridVisible(false);
        map.setZoomContolsVisible(true);
        map.setDisplayPositionByLatLon(42.731941,-84.552254, 14); 
        map.setToolTipText(mapTooltip);
        
        if ( guiConfigFile != null )
        	this.open( new File(this.guiConfigFile), false );
        
        map.setVisible(true);
        treeMap.setVisible(true);
        
        provider = createGeoPointProvider(view);
        provider.init();
        view.setProvider(provider);
        
        view.designToolBar.setVisible(designMode);
        view.addActionKeys();
        

        PrintStream con=new PrintStream(new TextAreaOutputStream(view.console));

        System.setOut(con);
        System.setErr(con);
        
        view.pack();
        if ( startupBounds != null )
        	view.setBounds(startupBounds);
        else
        	view.setLocationRelativeTo(null);
        view.setVisible(true);
        
        if ( isAutostartEnabled() ) {
        	final int delay = getAutostartDelay();
        	
        	System.out.printf("Autostart enabled. Starting in %d milliseconds.\n", delay);
        	
        	new Thread(new Runnable() {
                public void run() {
                    try {
    					Thread.sleep(delay);
    		        	SwingUtilities.invokeLater(new Runnable() {
    		                public void run() {
    		                	view.start();
    		                }
    		            });
    				} catch (InterruptedException ex) {
    				}
                }
            }).start();
        }
    }
    
    public GeoPointsView getMainView() {
        return this.view;
    }
    
    public JMapViewer getMapViewer(){
        return treeMap.getViewer();
    }

    private GeoPointProvider createGeoPointProvider(GeoPointRenderer renderer) throws ParseException, InitializationException {
    	JSONObject p = null;
    	if ( StringUtils.isBlank(providerClassName )) {
    		if ( guiConfig != null ) {
    			if ( guiConfig.has("provider") ) {
    				p = guiConfig.getJSONObject("provider");
    				if ( p != null && p.has("className") ) {
    					providerClassName = p.getString("className");
    				}
    			}
    		}
    	}
    	
    	if ( StringUtils.isBlank( providerClassName ) )
    		throw new ParseException("Geo points provider class name must be specified as the command line option or in the GUI configuration file");

    	if ( this.providerConfig == null && guiConfig != null ) {
    		if ( p == null ) {
    			if ( guiConfig.has("provider") )
    				p = guiConfig.getJSONObject("provider");
    		}
			if ( p != null && p.has("config") ) {
				String config = p.getString("config");
				providerConfigFile = config;
				providerConfig = JsonHelper.createJsonFromFile(config);
			}
    	}
    	
		try {
			Class<?> providerClass = Class.forName(providerClassName);
			Constructor<?> providerCtor = providerClass.getConstructor(GeoPointRenderer.class, JSONObject.class);
			GeoPointProvider provider = (GeoPointProvider)providerCtor.newInstance(renderer, this.providerConfig);
			return provider;
		} catch (ClassNotFoundException e) {
			throw new InitializationException(String.format("Provider class '%s' was not found", providerClassName),e);
		} catch (SecurityException e) {
			throw new InitializationException(String.format("Security exception for provider class '%s'", providerClassName),e);
		} catch (NoSuchMethodException e) {
			throw new InitializationException(String.format("Constructor for provider class '%s' was not found", providerClassName),e);
		} catch (IllegalArgumentException e) {
			throw new InitializationException(String.format("Illegal argument exception for provider class '%s' was not found", providerClassName),e);
		} catch (InstantiationException e) {
			throw new InitializationException(String.format("Couldn't instantiate provider class '%s' was not found", providerClassName),e);
		} catch (IllegalAccessException e) {
			throw new InitializationException(String.format("Illegal access exception for provider class '%s' was not found", providerClassName),e);
		} catch (InvocationTargetException e) {
			throw new InitializationException(String.format("Couldn't instantiate provider class '%s' was not found", providerClassName),e);
		}
    }

	public String getProviderClassName() {
		return providerClassName;
	}

	public void setProviderClassName(String providerClassName) {
		this.providerClassName = providerClassName;
	}
	
	public String getProviderConfigFile() {
		return providerConfigFile;
	}

	public void setProviderConfigFile(String providerConfigFile) {
		this.providerConfigFile = providerConfigFile;
	}
	

	public boolean isAutostartEnabled() {
		return autostartEnabled;
	}

	public void setAutostartEnabled(boolean autostartEnabled) {
		this.autostartEnabled = autostartEnabled;
	}

	public int getAutostartDelay() {
		return autostartDelay;
	}

	public void setAutostartDelay(int autostartDelay) {
		this.autostartDelay = autostartDelay;
	}
	
	public void open(File file, boolean verifyProvideChange) {
		GeoPointsMapperConfig config = new GeoPointsMapperConfig(this);
		config.open(file, verifyProvideChange);
	}
	
	public String getGuiConfigFile() {
		return guiConfigFile;
	}

	public void setGuiConfigFile(String guiConfigFile) {
		this.guiConfigFile = guiConfigFile;
	}
	
	public JSONObject getGuiConfig() {
		return this.guiConfig;
	}

	public void setGuiConfig(JSONObject guiConfig) {
		this.guiConfig = guiConfig;
	}
	
	public void save(File file) {
		GeoPointsMapperConfig config = new GeoPointsMapperConfig(this);
		try {
			if ( provider != null )
			provider.save(providerConfigFile);
		} catch (InitializationException e) {
			System.err.printf("Save provider configuration failed. Reason: %s\n", e.getMessage());
		}
		config.save(file);
	}	
	
    public Rectangle getStartupBounds() {
		return startupBounds;
	}

	public void setStartupBounds(Rectangle startupBounds) {
		this.startupBounds = startupBounds;
	}
    
    public void processCommand(JMVCommandEvent command) {
        System.out.println(command);
    }
	
	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("GeoPointsMapper options", options);
	}
	
    public static void main(String[] args) {
	    final CommandLineParser parser = new BasicParser();
	    final Options options = new Options();
	    options.addOption("d", "design", false, "Run in design mode (optional, default: false)");
	    options.addOption("g", "guiconf", true, "GUI configuration file (optional)");
	    options.addOption("p", "provider", true, "Geo points provider class name (mandatory if not specified in the GUI configuration file)");
	    options.addOption("c", "provconf", true, "Provider configuration file (optional)");
	    
	    final boolean designMode;
	    final String guiConfigFile;
	    final String providerConfigFile;
	    final String providerClassName;
	    
	    try {
			final CommandLine commandLine = parser.parse(options, args);
			
			designMode = commandLine.hasOption('d');

		    if (commandLine.hasOption('g')) {
		    	guiConfigFile = commandLine.getOptionValue('g');
		    } else {
		    	guiConfigFile = null;
		    }
		    
		    if (commandLine.hasOption('p')) {
		    	providerClassName = commandLine.getOptionValue('p');
		    } else {
		    	providerClassName = null;
		    }
		    
		    if (commandLine.hasOption('c')) {
		    	providerConfigFile = commandLine.getOptionValue('c');
		    } else {
		    	providerConfigFile = null;
		    }
		    
		} catch (ParseException ex) {
			System.out.println("Command line arguments parsing failed. Reason: " + ex.getMessage());
			usage(options);
			return;
		}
            
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex) {
                }
                try {
					new GeoPointsMapper(designMode, guiConfigFile, providerClassName, providerConfigFile);
				} catch (InitializationException ex) {
					String message = ex.getMessage();
					if ( StringUtils.isBlank(message) ) {
						Throwable th = ex.getCause();
						if ( th != null )
							message = th.getMessage();
					}
					System.err.print(String.format("Couldn't initialize provider. Reason: %s.", message));
					System.exit(1);
				} catch (ParseException ex) {
					System.out.println("Command line arguments parsing failed. Reason: " + ex.getMessage());
					usage(options);
					System.exit(2);
				}
            }
        });
    }

}
