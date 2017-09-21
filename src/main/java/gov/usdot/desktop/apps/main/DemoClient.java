package gov.usdot.desktop.apps.main;

import gov.usdot.cv.common.util.UnitTestHelper;
import gov.usdot.desktop.apps.provider.GeoPointProvider;
import gov.usdot.desktop.apps.provider.InitializationException;
import gov.usdot.desktop.apps.renderer.GeoPointRenderer;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;

public class DemoClient {

	static
	{
	    UnitTestHelper.initLog4j(Level.WARN);
	}
	
	public static void main(String[] args) throws IOException, InitializationException, ClassNotFoundException, 
		InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {

		if (args.length < 1) {
			System.out.println("No configuration file specified");
			System.exit(-1);
		}
		
		String configFile = args[0];
		FileInputStream fis = new FileInputStream(configFile);
		String jsonTxt = IOUtils.toString(fis);
		JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonTxt);
		
		Class<?> providerClass = Class.forName(json.getString("providerClass"));
		Constructor<?> consumerCtor = providerClass.getConstructor(GeoPointRenderer.class, JSONObject.class);
		
		GeoPointRenderer renderer = null;
		if (json.containsKey("rendererClass")) {
			String className = json.getString("rendererClass");
			if (!className.isEmpty()) {
				Class<?> rendererClass = Class.forName(className);
				renderer = (GeoPointRenderer)rendererClass.newInstance();
			}
		}
		
		GeoPointProvider provider = (GeoPointProvider)consumerCtor.newInstance(renderer, json);
		
		provider.init();
		provider.start();
		
		boolean running = true;
		while (running) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		provider.stop();
		provider.dispose();
	}

}
