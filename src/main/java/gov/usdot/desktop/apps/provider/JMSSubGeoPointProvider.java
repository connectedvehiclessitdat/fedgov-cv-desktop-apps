package gov.usdot.desktop.apps.provider;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import gov.usdot.cv.common.dialog.DPCSubscription;
import gov.usdot.cv.common.dialog.DPCSubscriptionException;
import gov.usdot.desktop.apps.renderer.GeoPointRenderer;
import net.sf.json.JSONObject;

public class JMSSubGeoPointProvider extends JMSGeoPointProvider {
	
	private boolean isDisposed = false;
	private DPCSubscription dpcSubscription = null;
	private int subscriptionID = -1;

	public JMSSubGeoPointProvider(GeoPointRenderer renderer, JSONObject json) {
		super(renderer, json);
	}
	
	@Override
	public void init() throws InitializationException {
	}

	@Override
	public void dispose() {
		if ( !isDisposed ) {
			cancel();
			super.dispose();
			isDisposed = true;
		}
	}
	
	@Override
	public void start() throws InitializationException {
		subscribe();
		super.init();
		super.start();
	}
	
	@Override
	public void stop() throws InitializationException {
		super.stop();
		cancel();
		dispose();
	}
	
	@Override
	public void region(double nw_lat, double nw_lon, double se_lat, double se_lon) {
		JSONObject serviceRegion = createServiceRegion(nw_lat, nw_lon, se_lat, se_lon);
		JSONObject subscription = json.optJSONObject("subscription");
		if ( subscription == null ) 
			subscription = new JSONObject();
		subscription.put("serviceRegion", serviceRegion);
		json.put("subscription", subscription);
	}

	@Override
	public void save(String configFilePath) throws InitializationException {
		File file = new File(configFilePath);
		try {
			FileUtils.writeStringToFile(file, json.toString(3));
		} catch (IOException ex) {
			String msg = String.format("Couldn't save provider configuration to file '%s'. Reason: %s", file.getAbsolutePath(), ex.getMessage());
			throw new InitializationException(msg, ex);
		}
	}
	
	private void subscribe() throws InitializationException {
		if ( json.has("subscription") ) {
			try {
				dpcSubscription = new DPCSubscription(json.getJSONObject("subscription"));
				subscriptionID = dpcSubscription.request();
				json.put("brokerTopic", String.valueOf(subscriptionID));
			} catch (DPCSubscriptionException ex) {
				dpcSubscription = null;
				subscriptionID = -1;
				throw new InitializationException("Couldn't subscribe to LCSDW", ex);
			}
		}
	}

	private void cancel() {
		if ( dpcSubscription != null && subscriptionID != -1 ) {
			try {
				dpcSubscription.cancel(subscriptionID);
				subscriptionID = -1;
			} catch (DPCSubscriptionException ex) {
				System.err.printf(String.format("Couldn't cancel subscription %d to LCSDW. Reason: %s\n", subscriptionID, ex.getMessage()));
			}
		}
	}
	
	private JSONObject createServiceRegion(double nw_lat, double nw_lon, double se_lat, double se_lon) {
		JSONObject serviceRegion = new JSONObject();
		JSONObject nw = new JSONObject();
		nw.put("lat", nw_lat);
		nw.put("lon", nw_lon);
		serviceRegion.put("nw", nw);
		JSONObject se = new JSONObject();
		se.put("lat", se_lat);
		se.put("lon", se_lon);
		serviceRegion.put("se", se);
		return serviceRegion;
	}
}
