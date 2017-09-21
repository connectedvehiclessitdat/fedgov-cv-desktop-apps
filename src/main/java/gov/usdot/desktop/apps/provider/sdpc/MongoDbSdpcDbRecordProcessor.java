package gov.usdot.desktop.apps.provider.sdpc;

import gov.usdot.asn1.j2735.J2735Util;

import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;

import com.mongodb.DBObject;
import com.oss.asn1.AbstractData;
import com.oss.asn1.Coder;
import com.oss.asn1.DecodeFailedException;
import com.oss.asn1.DecodeNotSupportedException;

public class MongoDbSdpcDbRecordProcessor {

	public class Result {
		public final byte[] bytes;
		public final AbstractData message;
		public final Double lat;
		public final Double lon;
		
		public Result(byte[] bytes, AbstractData message, Double lat, Double lon) {
			this.bytes = bytes;
			this.message = message;
			this.lat = lat;
			this.lon = lon;
		}
	}
	
	protected final Coder coder;
	private final boolean getBytes;
	private final boolean getMessage; 
	private final boolean getLatLon;
	
	public MongoDbSdpcDbRecordProcessor(Coder coder, boolean getBytes, boolean getMessage, boolean getLatLon, JSONObject config) {
		this.coder = coder;
		this.getBytes = getBytes;
		this.getMessage = getMessage;
		this.getLatLon = getLatLon;
	}

	public Result process(DBObject dbObj) {

		byte[] bytes = null;
		if ( getBytes || getMessage ) {
			String encodedMsg = (String)dbObj.get("encodedMsg");
			if ( encodedMsg != null )
				bytes = Base64.decodeBase64(encodedMsg);
		}

		AbstractData message = null;
		if ( getMessage )
			message = decode(bytes);

		Double lat = null, lon = null;
		if ( getLatLon ) {
			lat = (Double) dbObj.get("lat");
			lon = (Double) dbObj.get("long");
		}
		
		return new Result(bytes, message, lat, lon);
	}
	
	protected AbstractData decode(byte[] bytes) {
		AbstractData message = null;
		try {
			message = J2735Util.decode(coder, bytes);
		} catch (DecodeFailedException e) {
			System.out.println("Decode failed. Reason: " + e.getMessage());
		} catch (DecodeNotSupportedException e) {
			System.out.println("Decode not supported. Reason: " + e.getMessage());
		}
		return message;
	}

}
