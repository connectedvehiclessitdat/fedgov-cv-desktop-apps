package gov.usdot.desktop.apps.misc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.IOUtils;

public class JsonHelper {
	
	public static JSONObject createJsonFromFile(String file ) {
    	try {
    		FileInputStream fis = new FileInputStream(file);
    		String jsonTxt = IOUtils.toString(fis);
    		return (JSONObject) JSONSerializer.toJSON(jsonTxt);
		} catch (FileNotFoundException ex) {
			System.out.print(String.format("Couldn't create JSONObject from file '%s'.\nReason: %s\n", file, ex.getMessage()));
		} catch (IOException ex) {
			System.out.print(String.format("Couldn't create JSONObject from file '%s'.\nReason: %s\n", file, ex.getMessage()));
		} catch ( Exception ex) {
			System.out.print(String.format("Couldn't create JSONObject from file '%s'.\nReason: %s\n", file, ex.getMessage()));
		}
    	return null;
	}
}
