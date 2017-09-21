package gov.usdot.desktop.apps.data;

import gov.usdot.asn1.generated.j2735.dsrc.Position3D;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage;
import gov.usdot.asn1.generated.j2735.semi.VehSitRecord;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage.Bundle;
import gov.usdot.asn1.j2735.J2735Util;

import com.oss.asn1.AbstractData;

public class GeoPoint {
	
	public static final GeoPoint endOfData = new GeoPoint(Double.NaN, Double.NaN);
	public static final GeoPoint flushData = new GeoPoint(Double.NaN, 0);
	
	final public double lat;
	final public double lon;
	
	public GeoPoint(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}
	
	public GeoPoint(AbstractData vehicleMessage) {
		
		if (vehicleMessage instanceof VehSitDataMessage) {
			VehSitDataMessage vsd = (VehSitDataMessage)vehicleMessage;
			Bundle bundle = vsd.getBundle();
			assert(bundle != null);
			int count = bundle.getSize();
			assert(count >  0);
			VehSitRecord vsr = bundle.get(0);
			Position3D pos = vsr.getPos();
			this.lat = J2735Util.convertGeoCoordinateToDouble(pos.getLat().intValue());
			this.lon = J2735Util.convertGeoCoordinateToDouble(pos.get_long().intValue());
		} else {
			this.lat = endOfData.lat;
			this.lon = endOfData.lon;
		}
	}
	
    public boolean isEndOfData() {
        return Double.isNaN(lat) && Double.isNaN(lon);
    }
    
    public boolean isFlushData() {
        return Double.isNaN(lat) && lon == flushData.lon;
    }

	@Override
	public String toString() {
		return "GeoPoint [lat=" + lat + ", lon=" + lon + "]";
	}
	
}
