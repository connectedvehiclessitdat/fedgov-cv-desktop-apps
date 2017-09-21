package gov.usdot.desktop.apps.provider;

import gov.usdot.asn1.generated.j2735.J2735;
import gov.usdot.asn1.j2735.J2735Util;
import gov.usdot.desktop.apps.data.GeoPoint;
import gov.usdot.desktop.apps.renderer.GeoPointRenderer;
import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.oss.asn1.AbstractData;
import com.oss.asn1.Coder;
import com.oss.asn1.DecodeFailedException;
import com.oss.asn1.DecodeNotSupportedException;

public class MongoDBGeoPointProvider implements GeoPointProvider {

	private GeoPointRenderer renderer;
	private JSONObject json;
	private Mongo mongoClient;
	private DB database;
	private DBCollection collection;
	private boolean stopped = false;
	private Coder coder;

	public MongoDBGeoPointProvider(GeoPointRenderer renderer, JSONObject json) {
		this.renderer = renderer;
		this.json = json;
	}

	public void init() throws InitializationException {
		try {
			mongoClient = new Mongo(json.getString("mongoHost"),
					json.getInt("mongoPort"));

			database = mongoClient.getDB(json.getString("mongoDatabase"));

			collection = database.getCollection(json.getString("mongoCollection"));
			
			J2735.initialize();
			coder = J2735.getPERUnalignedCoder();
		} catch (Exception e) {
			throw new InitializationException("", e);
		}

	}

	public void dispose() throws InitializationException {
		mongoClient.close();
		try {J2735.deinitialize();} catch (Exception ignore) { }
	}

	public void start() throws InitializationException {
		stopped = false;
		Thread t = new Thread(new MongoQueryRunner());
		t.start();
	}

	public void stop() throws InitializationException {
		stopped = true;
	}

	public void pause() throws InitializationException {
		// pause/resume not supported
	}

	public void resume() throws InitializationException {
		// pause/resume not supported
	}

	public boolean canPause() {
		return false;
	}

	public boolean canStop() {
		return true;
	}
	
	public void region(double nw_lat, double nw_lon, double se_lat, double se_lon) {		
	}

	public void save(String configFilePath) throws InitializationException {		
	}

	private class MongoQueryRunner implements Runnable {
		
		private int msgCount = 0;

		public MongoQueryRunner() {
		}

		public void run() {
			int delay = json.getInt("replayDelayMillis");
			BasicDBObject query = new BasicDBObject(json.getString("mongoQueryKey"), 
					json.getInt("mongoQueryValue"));
			
			DBObject sortObj = new BasicDBObject();
			sortObj.put("year", 1);
			sortObj.put("month", 1);
			sortObj.put("day", 1);
			sortObj.put("hour", 1);
			sortObj.put("minute", 1);
			sortObj.put("second", 1);
			DBCursor cursor = collection.find(query).sort(sortObj);
			
			System.out.println(getQueryMessage());
			if ( renderer != null )
				renderer.render("Query Historical Vehicle Situation Data");

			try {
				while (cursor.hasNext() && !stopped) {
					DBObject dbObj = cursor.next();
					msgCount++;
					
					if (json.getBoolean("printData")) {
						AbstractData vehicleMessage = decodeMessage((String)dbObj.get("encodedMsg"));
						System.out.println(vehicleMessage);
					}
					
					if (renderer != null) {
						renderer.render(new GeoPoint((Double) dbObj.get("lat"), (Double) dbObj.get("long")));
					}
					if (delay > 0) {
						try { Thread.sleep(delay); } catch (InterruptedException ignore) {}
					}
				}
			} finally {
				cursor.close();
				System.out.printf("Processed %d VSDM messages.", msgCount);
				if (renderer != null) {
					renderer.render(GeoPoint.endOfData);
				}
			}
		}
		
		private AbstractData decodeMessage(String base64Msg) {
			byte[] bytes = Base64.decodeBase64(base64Msg);
			AbstractData vehicleMessage = null;
			try {
				vehicleMessage = J2735Util.decode(coder, bytes);
			} catch (DecodeFailedException e) {
				e.printStackTrace();
			} catch (DecodeNotSupportedException e) {
				e.printStackTrace();
			}
			return vehicleMessage;
		}
		
		private String getQueryMessage() {
			StringBuilder sb = new StringBuilder();
			sb.append("Query Mongo Database: {\n");
			if ( json.has("mongoDatabase") )
				sb.append("    mongoDatabase:   ").append(json.getString("mongoDatabase")).append(",\n");
			if ( json.has("mongoCollection") )
				sb.append("    mongoCollection: ").append(json.getString("mongoCollection")).append(",\n");
			if ( json.has("mongoQueryKey") )
				sb.append("    mongoQueryKey:   ").append(json.getString("mongoQueryKey")).append(",\n");
			if ( json.has("mongoQueryValue") )
				sb.append("    mongoQueryValue: ").append(json.getString("mongoQueryValue")).append("\n}\n");
			return sb.toString();
		}

	}

}
