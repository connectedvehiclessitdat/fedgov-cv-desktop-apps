package gov.usdot.desktop.apps.provider;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;

import com.oss.asn1.Coder;
import com.oss.asn1.ControlTableNotFoundException;
import com.oss.asn1.DecodeFailedException;
import com.oss.asn1.DecodeNotSupportedException;
import com.oss.asn1.EncodeFailedException;
import com.oss.asn1.EncodeNotSupportedException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import gov.usdot.asn1.generated.j2735.J2735;
import gov.usdot.asn1.generated.j2735.dsrc.Acceleration;
import gov.usdot.asn1.generated.j2735.dsrc.AccelerationSet4Way;
import gov.usdot.asn1.generated.j2735.dsrc.AntiLockBrakeStatus;
import gov.usdot.asn1.generated.j2735.dsrc.AuxiliaryBrakeStatus;
import gov.usdot.asn1.generated.j2735.dsrc.BrakeAppliedStatus;
import gov.usdot.asn1.generated.j2735.dsrc.BrakeBoostApplied;
import gov.usdot.asn1.generated.j2735.dsrc.BrakeSystemStatus;
import gov.usdot.asn1.generated.j2735.dsrc.DDateTime;
import gov.usdot.asn1.generated.j2735.dsrc.DDay;
import gov.usdot.asn1.generated.j2735.dsrc.DHour;
import gov.usdot.asn1.generated.j2735.dsrc.DMinute;
import gov.usdot.asn1.generated.j2735.dsrc.DMonth;
import gov.usdot.asn1.generated.j2735.dsrc.DOffset;
import gov.usdot.asn1.generated.j2735.dsrc.DSecond;
import gov.usdot.asn1.generated.j2735.dsrc.DYear;
import gov.usdot.asn1.generated.j2735.dsrc.Heading;
import gov.usdot.asn1.generated.j2735.dsrc.Latitude;
import gov.usdot.asn1.generated.j2735.dsrc.Longitude;
import gov.usdot.asn1.generated.j2735.dsrc.MsgCRC;
import gov.usdot.asn1.generated.j2735.dsrc.Position3D;
import gov.usdot.asn1.generated.j2735.dsrc.StabilityControlStatus;
import gov.usdot.asn1.generated.j2735.dsrc.SteeringWheelAngle;
import gov.usdot.asn1.generated.j2735.dsrc.TemporaryID;
import gov.usdot.asn1.generated.j2735.dsrc.TractionControlStatus;
import gov.usdot.asn1.generated.j2735.dsrc.TransmissionAndSpeed;
import gov.usdot.asn1.generated.j2735.dsrc.VehicleLength;
import gov.usdot.asn1.generated.j2735.dsrc.VehicleSize;
import gov.usdot.asn1.generated.j2735.dsrc.VehicleWidth;
import gov.usdot.asn1.generated.j2735.dsrc.VerticalAcceleration;
import gov.usdot.asn1.generated.j2735.dsrc.YawRate;
import gov.usdot.asn1.generated.j2735.semi.FundamentalSituationalStatus;
import gov.usdot.asn1.generated.j2735.semi.GroupID;
import gov.usdot.asn1.generated.j2735.semi.SemiDialogID;
import gov.usdot.asn1.generated.j2735.semi.SemiSequenceID;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage;
import gov.usdot.asn1.generated.j2735.semi.VehSitRecord;
import gov.usdot.asn1.generated.j2735.semi.VsmType;
import gov.usdot.asn1.generated.j2735.semi.VehSitDataMessage.Bundle;
import gov.usdot.asn1.j2735.CVTypeHelper;
import gov.usdot.asn1.j2735.J2735Util;
import gov.usdot.cv.common.asn1.GroupIDHelper;
import gov.usdot.cv.common.asn1.TemporaryIDHelper;
import gov.usdot.cv.common.asn1.TransmissionAndSpeedHelper;
import gov.usdot.cv.common.dialog.TrustEstablishment;
import gov.usdot.cv.common.dialog.TrustEstablishmentException;
import gov.usdot.cv.common.util.CrcCccitt;
import gov.usdot.desktop.apps.data.GeoPoint;
import gov.usdot.desktop.apps.misc.JsonHelper;
import gov.usdot.desktop.apps.renderer.GeoPointRenderer;

public class GpxFileGeoPointProvider implements GeoPointProvider {
	
    final private GeoPointRenderer renderer;
    final private JSONObject config;
    
	static final String successMsg = "Successfully established trust with SDC!";
	static final String failureMsg = "WARNING: Could not establish trust with SDC!";
	
	private static AtomicInteger requestCount = new AtomicInteger(0);
    
    private boolean started = false;
    private boolean paused = false;
    
	static final private String DEFAULT_HOST = "localhost";
	static final private int DEFAULT_PORT = 46751;
	static final private int DEFAULT_SLEEP = 2*1000;
	static final private double DEFAULT_SPEED = 35.5;
	static final private String DEFAULT_DIRECTION = "forward";
	static final private int DEFAULT_GROUP_ID = 0;
	
	private Coder coder = null;
	private InetAddress ipAddress = null;
	private DatagramSocket socket = null;
	private int port = DEFAULT_PORT;
	private int sleep = DEFAULT_SLEEP;
	private boolean hexEncode = false;
	private boolean verbose = false;
	private double speed = DEFAULT_SPEED;
	private String direction = DEFAULT_DIRECTION;
	private GroupID groupID;
	
	private int requestID;
	
	private JSONArray rtept = null;
    
    public GpxFileGeoPointProvider(GeoPointRenderer renderer, JSONObject config) {
        this.renderer = renderer;
        this.config = config;
    }
    
	
	public void setRequestID() {
		requestID = requestCount.incrementAndGet();
	}
    

	public void init() throws InitializationException {
		
		if ( config == null )
			throw new InitializationException("Mandatory configuration file was not provided.");
		
		if ( config.has("verbose") )
			verbose = config.getBoolean("verbose");
		
		groupID = GroupIDHelper.toGroupID(config.optInt("groupID", DEFAULT_GROUP_ID));
		
		try {
			J2735.initialize();
			coder = J2735.getPERUnalignedCoder();
			if ( verbose )
				coder.enableEncoderDebugging();
		} catch (ControlTableNotFoundException ex) {
			throw new InitializationException("Couldn't initialize J2735 parser", ex);
		} catch (com.oss.asn1.InitializationException ex) {
			throw new InitializationException("Couldn't initialize J2735 parser", ex);
		}
		
		String host = DEFAULT_HOST;
		if ( config.has("host"))
			host = config.getString("host");
		try {
			ipAddress = InetAddress.getByName( host );
		} catch (UnknownHostException ex) {
			throw new InitializationException(String.format("Couldn't get host '%s' by name", host), ex);
		}
		try {
			socket = new DatagramSocket();
		} catch (SocketException ex) {
			throw new InitializationException("Couldn't create datagram socket instance", ex);
		}
		
		if ( config.has("hex") )
			hexEncode = config.getBoolean("hex");

		if ( config.has("port"))
			port = config.getInt("port");
		
		if ( config.has("sleep") )
			sleep = config.getInt("sleep");
		
		if ( config.has("speed") )
			speed = config.getDouble("speed");
		
		if ( config.has("direction") )
			direction = config.getString("direction");
		
		if ( !config.has("file") )
			throw new InitializationException("Mandatory option file is not found in the configuration file provided.");
		String file = (String)config.get("file");
		JSONObject gpx;
		try {
			FileInputStream fis = new FileInputStream( file );
			String jsonTxt = IOUtils.toString( fis );
			gpx = (JSONObject) JSONSerializer.toJSON(jsonTxt);
		} catch (FileNotFoundException ex) {
			throw new InitializationException(String.format("Couldn't load GPX information from file '%s'.\nReason: %s", file, ex.getMessage()), ex);
		} catch (IOException ex) {
			throw new InitializationException(String.format("Couldn't load GPX information from file '%s'.\nReason: %s", file, ex.getMessage()), ex);
		} catch ( Exception ex) {
			throw new InitializationException(String.format("Couldn't load GPX information from file '%s'.\nReason: %s", file, ex.getMessage()), ex);
		}
		
		if ( !gpx.has("rte") )
			throw new InitializationException("Geo position file provided is not in GPX format: rte element is missing");
		
		JSONObject rte = gpx.getJSONObject("rte");
		if ( rte.isNullObject() )
			throw new InitializationException("Geo position file provided is not in GPX format: rte element is malformed");
		
		if ( !rte.has("rtept") )
			throw new InitializationException("Geo position file provided is not in GPX format: rtept element is missing");
		
		rtept = rte.getJSONArray("rtept");
		if ( !rtept.isArray() ) {
			rtept = null;
			throw new InitializationException("Geo position file provided is not in GPX format: rtept element is malformed");
		}
		
		if ( direction.equalsIgnoreCase("b") || direction.equalsIgnoreCase("backwards") )
			reverse();
		else if ( direction.equalsIgnoreCase("r") || direction.equalsIgnoreCase("random") )
			shuffle();
	}

	public void start() throws InitializationException {
	    setRequestID();
		try {
			if ( establishTrust() == false ) {
				stop();
				return;
			}
		} catch (TrustEstablishmentException ex) {
			throw new InitializationException("Couldn't establish trust. Reason: %s" + ex.getMessage(), ex);
		}
        started = true;
        int msgCount = 0;
        double latitude, longitude;
		for( int i = 0; i < rtept.size() && started; i++ ) {
            while( started && paused ) 
                nap(250);
            
			JSONObject pt = rtept.getJSONObject(i);
			if ( !pt.isNullObject() && pt.has("lat") && pt.has("lon") ) {
				latitude = pt.getDouble("lat");
				longitude = pt.getDouble("lon");
				try {
					send(latitude, longitude);
					msgCount++;
		            GeoPoint point = new GeoPoint(latitude, longitude);
		            if ( renderer != null )
		            	renderer.render(point);
		            nap(this.sleep);
		            if ( verbose )
		            	System.out.println(String.format("Current position: lat %f, lon %f", point.lat, point.lon));
				} catch (Exception ex) {
					System.out.print(String.format("Couldn't send geo point with lat %f, lon %f.\nReason: %s", latitude, longitude, ex.getMessage()));
				}
			}
		}
		System.out.printf("Sent %d VSDM messages.", msgCount);
        stop();
	}

	public void stop() throws InitializationException {
		if( renderer != null )
			renderer.render(GeoPoint.endOfData);
        started = false;
        paused = false;
	}

	public void pause() throws InitializationException {
        if ( started )
            paused = true;
	}

	public void resume() throws InitializationException {
        if ( started && paused )
            paused = false;
	}

	public boolean canPause() {
		return true;
	}
	
	public void dispose() throws InitializationException {
		rtept = null;
		if ( socket != null ) {
			if ( !socket.isClosed() )
				socket.close();
			socket = null;
		}
		coder = null;
		J2735.deinitialize();
	}

	public boolean canStop() {
		return true;
	}

	public void region(double nw_lat, double nw_lon, double se_lat, double se_lon) {		
	}

	public void save(String configFilePath) {		
	}	
	
    private void nap(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
        }
    }
	
	private void send(double latitude, double longitude) throws Exception {
		byte[] packet = createEncodedVehSitDataMessage(latitude, longitude);
		
		if ( hexEncode )
			packet = Hex.encodeHexString(packet).getBytes();
		
		DatagramPacket datagramPacket = new DatagramPacket(packet, packet.length, ipAddress, port);
		socket.send(datagramPacket);
		if ( sleep > 0 ) {
			try {
				Thread.sleep(sleep);
			} catch (InterruptedException igonore) {
			}
		}
	}
	
	private byte[] createEncodedVehSitDataMessage(double latitude, double longitude) throws Exception {
		try {
			VehSitDataMessage sitDataMsg = createVehSitDataMessage(latitude, longitude);
			ByteArrayOutputStream sink = new ByteArrayOutputStream();
			coder.encode(sitDataMsg, sink);
			byte[] responseBytes = sink.toByteArray();
			CrcCccitt.setMsgCRC(responseBytes);
			printMessage(responseBytes);
			return responseBytes;
		} catch (EncodeFailedException ex) {
			throw new Exception("Couldn't encode VehicleServiceResponse message because encoding failed", ex);
		} catch (EncodeNotSupportedException ex) {
			throw new Exception("Couldn't encode VehicleServiceResponse message because encoding is not supported", ex);
		}
	}
	
	private void printMessage(byte[] msg ) {
		try {
			System.out.println( J2735Util.decode(coder, msg) );
		} catch (DecodeFailedException ex) {
			System.out.println("Couldn't print message because decoding failed. Reason: " + ex.getMessage());
		} catch (DecodeNotSupportedException ex) {
			System.out.println("Couldn't print message because decoding is not supported. Reason: " + ex.getMessage());
		}
	}
	
	private int last_lat_1 = 0, last_lat_2 = 0, last_lon_1 = 0, last_lon_2 = 0;
	
	private VehSitDataMessage createVehSitDataMessage(double latitude, double longitude) {
		int lon_int = J2735Util.convertGeoCoordinateToInt(longitude);
		int lat_int = J2735Util.convertGeoCoordinateToInt(latitude);
		
		Calendar now = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
		DDateTime dt = new DDateTime(
				new DYear(now.get(Calendar.YEAR)), 
				new DMonth(now.get(Calendar.MONTH)+1), 
				new DDay(now.get(Calendar.DAY_OF_MONTH)), 
				new DHour(now.get(Calendar.HOUR_OF_DAY)), 
				new DMinute(now.get(Calendar.MINUTE)), 
				new DSecond(now.get(Calendar.SECOND)),
				new DOffset( -300 ));
	
		// update path history
		last_lat_2 = last_lat_1;
		last_lat_1 = lat_int;
		last_lon_2 = last_lon_1;
		last_lon_1 = lon_int;

		TemporaryID tempID = new TemporaryID("1234".getBytes());
		TransmissionAndSpeed speed = TransmissionAndSpeedHelper.createTransmissionAndSpeed(randomSpeed());
		Heading heading = new Heading(90);
		
		final Acceleration lonAccel = new Acceleration(1);
		final Acceleration latAccel = new Acceleration(1);
		final VerticalAcceleration vertAccel = new VerticalAcceleration(43);
		final YawRate yaw = new YawRate(0);
		final AccelerationSet4Way accelSet = new AccelerationSet4Way(lonAccel, latAccel, vertAccel, yaw);
	    
		final BrakeSystemStatus brakes = new BrakeSystemStatus(
					new BrakeAppliedStatus(new byte[] { (byte)0xf8 } ), 
					TractionControlStatus.unavailable, 
					AntiLockBrakeStatus.unavailable, 
					StabilityControlStatus.unavailable,
					BrakeBoostApplied.unavailable,
					AuxiliaryBrakeStatus.unavailable
				);
		
		SteeringWheelAngle steeringAngle = new SteeringWheelAngle(0);

		VehicleWidth vehWidth   = new  VehicleWidth(185); 	// Honda Accord 2014 width:   72.8 in -> ~ 185 cm
		VehicleLength vehLength = new VehicleLength(486);	// Honda Accord 2014 length: 191.4 in -> ~ 486 cm
		VehicleSize vehSize = new VehicleSize(vehWidth, vehLength);
		
		FundamentalSituationalStatus fundamental = new FundamentalSituationalStatus(speed, heading, steeringAngle, accelSet,  brakes, vehSize);
		Position3D pos1 = new Position3D(new Latitude(lat_int), new Longitude(lon_int));
		VehSitRecord vehSitRcd1 = new VehSitRecord(tempID, dt, pos1, fundamental);
		Position3D pos2 = new Position3D(new Latitude(last_lat_1), new Longitude(last_lon_1));
		VehSitRecord vehSitRcd2 = new VehSitRecord(tempID, dt, pos2, fundamental);
		Position3D pos3 = new Position3D(new Latitude(last_lat_2), new Longitude(last_lon_2));
		VehSitRecord vehSitRcd3 = new VehSitRecord(tempID, dt, pos3, fundamental);
		
		MsgCRC crc = new MsgCRC(new byte[2]);

		VsmType type = new VsmType(new byte[] { CVTypeHelper.bitWiseOr(CVTypeHelper.VsmType.VEHSTAT, CVTypeHelper.VsmType.ELVEH) }) ;
		VehSitDataMessage vsdm = new VehSitDataMessage(SemiDialogID.vehSitData, SemiSequenceID.data, GroupIDHelper.toGroupID(0), TemporaryIDHelper.toTemporaryID(requestID), type,  
			    new Bundle(new VehSitRecord[] { vehSitRcd1, vehSitRcd2, vehSitRcd3} ), crc);

		return vsdm;
	}
	
	private boolean establishTrust() throws TrustEstablishmentException {
		boolean establish = false;
		boolean fake = false;
		boolean ignore = false;
		int attempts = 3;
		int timeout = 4000;
		int destPort = -1;
		if ( config.has("trust") ) {
			JSONObject trust = config.getJSONObject("trust");
			establish = trust.optBoolean("establish", establish);
			fake = trust.optBoolean("fake", fake);	
			ignore = trust.optBoolean("ignore", ignore);
			attempts = trust.optInt("attempts", 3);
			timeout = trust.optInt("timeout", 4000);
			destPort = trust.optInt("port", -1);
		}
		if ( !establish )
			return true;
		TrustEstablishment te = new TrustEstablishment(coder, SemiDialogID.vehSitData, groupID, requestID, ipAddress, port, (InetAddress)null, destPort, port);
		te.setTimeout(timeout);
		te.setAttempts(attempts);
		te.setVerbose(verbose);
		boolean established = !fake ? te.establishTrust(attempts, timeout) : true;
		String message = established ? successMsg : failureMsg;
		if ( renderer != null)
			renderer.render(message);
		System.err.println(message);
		return established || ignore ;
	}
	
	private double randomSpeed() {
		return new java.util.Random().nextDouble()*(this.speed+Double.MIN_VALUE);
	}
	
	private void shuffle()
	{
	    int index;
	    JSONObject temp;
	    Random random = new Random();
	    for (int i = rtept.size() - 1; i > 0; i--)
	    {
	        index = random.nextInt(i + 1);
	    	temp = rtept.getJSONObject(index);
	        rtept.set(index, rtept.getJSONObject(i));
	        rtept.set(i, temp);
	    }
	}
	
	private void reverse()
	{
	    JSONObject temp;
	    for (int left = 0, right = rtept.size() - 1; left < right; left++, right--) {
	    	temp = rtept.getJSONObject(left);
	        rtept.set(left, rtept.getJSONObject(right));
	        rtept.set(right, temp);
	    }
	}

	private static void usage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("GpxFileGeoPointProvider options", options);
	}
	
	public static void main(String[] args) {
	    final CommandLineParser parser = new BasicParser();
	    final Options options = new Options();
	    options.addOption("h", "host", true, "Hostname or IP address to send the packet to (optional, default: " + DEFAULT_HOST + ")");
	    options.addOption("p", "port", true, "Port to send the packet to (optional, default: " + DEFAULT_PORT + ")");
	    options.addOption("r", "rate", true, "Sleep time in milliseconds after sending a packet (optional, default: " + DEFAULT_SLEEP + ")");
	    options.addOption("f", "file", true, "GPX file in JSON format that contains geo cordinats to send (mandatory if -c is not used)");
	    options.addOption("s", "speed", true, "Max speed of the vehicle. Actual speed will be between 0 and this value (optional, default: " + DEFAULT_SPEED + ")");
	    options.addOption("d", "direction", true, "Direction of the run: f or forward, b or backward, r or random (optional, default: " + DEFAULT_DIRECTION + ")");
	    options.addOption("c", "config", true, "Configuration file for GPX GpxFileGeoPointProvider that contains execution options (optional)");
	    options.addOption("v", "verbose", false, "Print OSS encoder debug information (optional, default: false)"); 
	    options.addOption("x", "hex", false, "Base16 (hex) encode payload before sending (optional, debug, default: false)"); 
	    options.addOption("g", "group", true, "GroupID to use in dialogs (optional, default: " + DEFAULT_GROUP_ID + ")");
		
		String host = DEFAULT_HOST;
		int port = DEFAULT_PORT;
		int sleep = DEFAULT_SLEEP;
		double speed = DEFAULT_SPEED;
		String direction = DEFAULT_DIRECTION;
		String file = null;
		JSONObject conf = new JSONObject();
		boolean hexEncode = false;
		boolean verbose = false;
		int groupID = DEFAULT_GROUP_ID;
		
	    try {
			final CommandLine commandLine = parser.parse(options, args);
			
		    if (commandLine.hasOption('c')) {
		        String configFile = commandLine.getOptionValue('c');
		        conf = JsonHelper.createJsonFromFile(configFile);
		        if ( conf == null ) {
		    		usage(options);
		    		return;
		    	}
		        if ( conf.has("file") )
		        	file = conf.getString("file");
		    }
			
		    if (commandLine.hasOption('f')) {
		        file = commandLine.getOptionValue('f');
		        if ( file == null || (file = file.trim()).length() == 0 ) {
		    		System.out.println("Invalid parameter: File parameter must not be empty string");
		    		usage(options);
		    		return;
		        }
		        conf.put("file", file);
		    } else if ( file == null ) {
		    	System.out.println("File information is mandatory and must be provided in the configuration file or via -f option");
	    		usage(options);
	    		return;
		    }
			
		    if (commandLine.hasOption('h')) {
		        host = commandLine.getOptionValue('h');
		        if ( host == null || (host = host.trim()).length() == 0 ) {
		    		System.out.println("Invalid parameter: Host parameter must not be empty string");
		    		usage(options);
		    		return;
		        }
		        conf.put("host", host);
		    }
		    
		    if (commandLine.hasOption('p')) {
		    	try {
		    		port = Integer.parseInt(commandLine.getOptionValue('p'));
		    		conf.put("port", port);
		    	} catch ( NumberFormatException ex) {
		    		System.out.println("Invalid parameter: Port parameter must be an integer");
		    		usage(options);
		    		return;
		    	}
		    }
		    
		    if (commandLine.hasOption('r')) {
		    	try {
		    		sleep = Integer.parseInt(commandLine.getOptionValue('r'));
		    		conf.put("sleep", sleep);
		    	} catch ( NumberFormatException ex) {
		    		System.out.println("Invalid parameter: Sleep parameter must be an integer");
		    		usage(options);
		    		return;
		    	}
		    }
		    
		    if (commandLine.hasOption('s')) {
		    	try {
		    		speed = Double.parseDouble(commandLine.getOptionValue('s'));
		    		conf.put("speed", speed);
		    	} catch ( NumberFormatException ex) {
		    		System.out.println("Invalid parameter: Speed parameter must be an number");
		    		usage(options);
		    		return;
		    	}
		    }
		    
		    if (commandLine.hasOption('d')) {
		    	direction = commandLine.getOptionValue('d');
		    	if ( !(direction.equalsIgnoreCase("f") || direction.equalsIgnoreCase("b") || direction.equalsIgnoreCase("r") ||
		    		   direction.equalsIgnoreCase("forward") || direction.equalsIgnoreCase("backward") || direction.equalsIgnoreCase("random") )) {
		    		System.out.printf("Invalid parameter: Direction parameter can't have value '%s'\n", direction);
		    		usage(options);
		    		return;
		    	}
		    	conf.put("direction", direction);
		    }
		    
		    if ( commandLine.hasOption('v') ) {
		    	verbose = true;
		    	conf.put("verbose", verbose);
		    }
		    
		    if ( commandLine.hasOption('x') ) {
		    	hexEncode = true;
		    	conf.put("hex", hexEncode);
		    }
		    
		    if (commandLine.hasOption('g')) {
		    	try {
		    		port = Integer.parseInt(commandLine.getOptionValue('g'));
		    		conf.put("groupID", groupID);
		    	} catch ( NumberFormatException ex) {
		    		System.out.println("Invalid parameter: GroupID parameter must be an integer");
		    		usage(options);
		    		return;
		    	}
		    }

		} catch (ParseException ex) {
			System.out.println("Command line arguments parsing failed. Reason: " + ex.getMessage());
			usage(options);
			return;
		}
	    
	    if ( !conf.has("host") )
	    	conf.put("host", host);
	    if ( !conf.has("port") )	    
	    	conf.put("port", port);
	    if ( !conf.has("sleep") )	    
	    	conf.put("sleep", sleep);
	    if ( !conf.has("file") )	    
	    	conf.put("file", file);
	    if ( !conf.has("speed") )	    
	    	conf.put("speed", speed);
	    if ( !conf.has("direction") )	    
	    	conf.put("direction", direction);
	    if ( !conf.has("verbose") )	    
	    	conf.put("verbose", verbose);
	    if ( !conf.has("hex") )	    
	    	conf.put("hex", hexEncode);
	    if ( !conf.has("groupID") )	    
	    	conf.put("groupID", groupID);
		
	    if ( verbose )
	    	System.out.println(conf.toString(3));
		
		GeoPointProvider provider = new GpxFileGeoPointProvider(null, conf);
		
		try {
			try {
				provider.init();
				provider.start();
			} finally {
				provider.dispose();
			}
		} catch (InitializationException ex ) {
			System.out.println("Initialization exception while sending packets. Reason: " + ex.getMessage());
		} catch ( Exception ex ) {
			System.out.println("Unexpected exception while sending packets. Reason: " + ex.getMessage());
		} 
	}
}
