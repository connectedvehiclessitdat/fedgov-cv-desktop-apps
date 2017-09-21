package gov.usdot.desktop.apps.provider.sdpc;

import java.io.ByteArrayOutputStream;

import net.sf.json.JSONObject;

import gov.usdot.asn1.generated.j2735.dsrc.TemporaryID;
import gov.usdot.asn1.generated.j2735.semi.IntersectionSituationData;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage;
import gov.usdot.cv.common.asn1.TemporaryIDHelper;

import com.mongodb.DBObject;
import com.oss.asn1.AbstractData;
import com.oss.asn1.Coder;
import com.oss.asn1.EncodeFailedException;
import com.oss.asn1.EncodeNotSupportedException;

public class MongoDbSdpcCustomRequestIdDbRecordProcessor extends MongoDbSdpcDbRecordProcessor {
	
	static final String REQUEST_ID_NAME = "requestID";
	static final int DEFAULT_REQUEST_ID = 0xA0A0A0A0;
	final TemporaryID requestID;
	
	public MongoDbSdpcCustomRequestIdDbRecordProcessor( Coder coder, boolean getBytes, boolean getMessage, boolean getLatLon, JSONObject config) {
		super(coder,getBytes, getMessage, getLatLon, config);
		int requestId = DEFAULT_REQUEST_ID;
		if ( config != null && config.has( REQUEST_ID_NAME ) )
			requestId = config.getInt( REQUEST_ID_NAME );
		requestID = TemporaryIDHelper.toTemporaryID(requestId);
	}
	
	@Override
	public Result process(DBObject dbObj) {
		Result result = super.process(dbObj);
		
		if ( result.bytes == null ) 
			return result;
		
		AbstractData msg = result.message != null ? result.message : decode(result.bytes);
		if ( msg instanceof VehSitDataMessage ) {
			VehSitDataMessage vmsg = (VehSitDataMessage)msg;
			vmsg.setRequestID(requestID);
		} else if ( msg instanceof IntersectionSituationData ) {
			IntersectionSituationData imsg = (IntersectionSituationData)msg;
			imsg.setRequestID(requestID);
		}

		return new Result(encode(msg), result.message != null ? msg : null, result.lat, result.lon);
	}
	
	protected byte[] encode(AbstractData pdu) {
		try {
			ByteArrayOutputStream sink = new ByteArrayOutputStream();
			coder.encode(pdu, sink);
			return sink.toByteArray();
		} catch (EncodeFailedException e) {
			System.out.println("Encode failed. Reason: " + e.getMessage());
		} catch (EncodeNotSupportedException e) {
			System.out.println("Encode not supported. Reason: " + e.getMessage());
		}
		return null;
	}
	
}
