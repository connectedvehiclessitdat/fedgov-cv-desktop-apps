package gov.usdot.desktop.apps.provider.sdpc;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import gov.usdot.asn1.generated.j2735.J2735;
import gov.usdot.asn1.generated.j2735.semi.DataReceipt;
import gov.usdot.asn1.generated.j2735.semi.IntersectionSituationDataAcceptance;
import gov.usdot.asn1.generated.j2735.semi.SemiDialogID;
import gov.usdot.asn1.generated.j2735.semi.SemiSequenceID;
import gov.usdot.asn1.j2735.CVSampleMessageBuilder;
import gov.usdot.asn1.j2735.J2735Util;
import gov.usdot.cv.common.asn1.TemporaryIDHelper;
import gov.usdot.cv.common.dialog.TrustEstablishment;
import gov.usdot.cv.common.dialog.TrustEstablishmentException;
import gov.usdot.desktop.apps.data.GeoPoint;
import gov.usdot.desktop.apps.misc.JsonHelper;
import gov.usdot.desktop.apps.provider.GeoPointProvider;
import gov.usdot.desktop.apps.provider.InitializationException;
import gov.usdot.desktop.apps.renderer.GeoPointRenderer;
import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.util.JSON;
import com.oss.asn1.AbstractData;
import com.oss.asn1.Coder;
import com.oss.asn1.DecodeFailedException;
import com.oss.asn1.DecodeNotSupportedException;
import com.oss.asn1.EncodeFailedException;
import com.oss.asn1.EncodeNotSupportedException;

public class MongoDbSdpcSenderProvider implements GeoPointProvider {

	private GeoPointRenderer renderer;
	private JSONObject config;
	private Mongo mongoClient;
	private DB database;
    private boolean started = false;
    private boolean paused = false;
	private Coder coder;
	private MongoDbSdpcSenderConfig configuration;
	private MongoDbSdpcDbRecordProcessor processor;
	private int msgCount = 0;
	protected boolean debug = false;

	public MongoDbSdpcSenderProvider(GeoPointRenderer renderer, JSONObject config) throws IOException {
		assert( config != null);
		this.renderer = renderer;
		this.config = config;
	}
	
	public void init() throws InitializationException {
		try {
			configuration = new MongoDbSdpcSenderConfig(config);
			
			mongoClient = new Mongo(configuration.source.host, configuration.source.port);
			database = mongoClient.getDB(configuration.source.database);
			
			J2735.initialize();
			coder = J2735.getPERUnalignedCoder();
			
			boolean getBytes = configuration.destination.send;
			boolean getMessage = configuration.other.verbose;
			boolean getLatLon = renderer != null;
			processor = StringUtils.isBlank( configuration.other.processor ) ?
					new MongoDbSdpcDbRecordProcessor(coder, getBytes, getMessage, getLatLon, null ) :
					createDbRecordProcessor(configuration.other.processor, getBytes, getMessage, getLatLon, configuration.other.processor_config);
		} catch (Exception e) {
			throw new InitializationException("", e);
		}
	}
	
	private MongoDbSdpcDbRecordProcessor createDbRecordProcessor(String processorClassName, boolean getBytes, boolean getMessage, boolean getLatLon, JSONObject config) throws InitializationException {
		try {
			Class<?> processorClass = Class.forName(processorClassName);
			Constructor<?> processorCtor = processorClass.getConstructor(Coder.class, boolean.class, boolean.class, boolean.class, JSONObject.class);
			return (MongoDbSdpcDbRecordProcessor)processorCtor.newInstance(coder, getBytes,getMessage, getLatLon, config);
		} catch (ClassNotFoundException e) {
			throw new InitializationException(String.format("Processor class '%s' was not found", processorClassName),e);
		} catch (SecurityException e) {
			throw new InitializationException(String.format("Security exception for processor class '%s'", processorClassName),e);
		} catch (NoSuchMethodException e) {
			throw new InitializationException(String.format("Constructor for processor class '%s' was not found", processorClassName),e);
		} catch (IllegalArgumentException e) {
			throw new InitializationException(String.format("Illegal argument exception for processor class '%s' was not found", processorClassName),e);
		} catch (InstantiationException e) {
			throw new InitializationException(String.format("Couldn't instantiate processor class '%s' was not found", processorClassName),e);
		} catch (IllegalAccessException e) {
			throw new InitializationException(String.format("Illegal access exception for processor class '%s' was not found", processorClassName),e);
		} catch (InvocationTargetException e) {
			throw new InitializationException(String.format("Couldn't instantiate processor class '%s' was not found", processorClassName),e);
		}
	}

	public void dispose() throws InitializationException {
		if( mongoClient != null )
			mongoClient.close();
		try {J2735.deinitialize();} catch (Exception ignore) { }
	}

	public void start() throws InitializationException {
		new Thread() {
			@Override
			public void run() {
				execute();
			}
		}.start();
		started = true;
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

	public boolean canStop() {
		return true;
	}
	
	public void region(double nw_lat, double nw_lon, double se_lat, double se_lon) {	
		final String format = "{\"$query\":{\"location\":{\"$geoWithin\":{\"$box\":[[%.9f, %.9f],[%.9f,%.9f]]}}},\"$orderby\":{\"deCreatedAt\":-1}}"; 
		String queryStr = String.format(format, nw_lon, se_lat, se_lon, nw_lat);
		System.out.println("New query: " + queryStr);
		configuration.source.query = (BasicDBObject) JSON.parse(queryStr);
	}

	public void save(String configFilePath) throws InitializationException {	
	}
	
	protected void execute() {
		msgCount = 0;
		DBCollection collection = database.getCollection(configuration.source.collection);
		BasicDBObject query = configuration.source.query;
		DBCursor cursor = collection.find(query);
		if ( configuration.source.skip > 0 )
			cursor = cursor.skip(configuration.source.skip);
		if ( configuration.source.limit > 0 )
			cursor = cursor.limit(configuration.source.limit);
		
		if ( configuration.other.verbose )
			System.out.println("Running query: " + JSON.serialize(query));
		
		final String heading = String.format("Query Data from %s collection.", configuration.source.collection);
		
		System.out.println(heading);
		
		if ( renderer != null )
			renderer.render( heading );
		
		long dialogId = -1;
		int requestID = -1;
		
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
			
			while (cursor.hasNext() && started) {
				
	            while( started && paused ) 
	                nap(250);
	            
				DBObject dbObj = cursor.next();
				msgCount++;
				
				if ( dialogId == -1 ) {
					if ( debug )
						System.out.println(dbObj);
					dialogId = ((Number)dbObj.get("dialogId")).longValue();
					SemiDialogID dialogID = SemiDialogID.valueOf(dialogId);
					requestID = ((Number)dbObj.get("requestId")).intValue();
					try {
						if ( establishTrust(dialogID, requestID) == false ) {
							try {
								stop();
							} catch (InitializationException e) {
							}
							break;
						}
					} catch (TrustEstablishmentException ex) {
						System.out.println(String.format("Couldn't establish trust. Reason: %s" + ex.getMessage()));
						break;
					}
				}
				
				MongoDbSdpcDbRecordProcessor.Result result = processor.process(dbObj);
				
				if ( configuration.other.verbose )
					System.out.println(result.message);
				
				if (renderer != null && result.lat != null && result.lon != null )
					renderer.render(new GeoPoint(result.lat, result.lon));
				
				if ( configuration.destination.send ) {
					try {
						if ( configuration.other.verbose && debug )
							System.out.println(Hex.encodeHexString(result.bytes));
						send(socket, result.bytes);
					} catch (Exception e) {
						System.out.printf("Couldn't send message. Reason: %s", e.getMessage());
					}
				}
				
				if (configuration.other.delay > 0) 
					nap(configuration.other.delay);
			}
			if ( msgCount > 0 && configuration.destination.send && "intersectionSitData".equalsIgnoreCase(configuration.source.collection))
				sendIntersectionSituationDataAcceptance(socket, requestID);
		} catch (SocketException ex) {
			System.out.printf("Couldn't create datagram socket instance. Reason: %s.", ex.getMessage());
		} finally {
			if ( socket != null ) {
				if ( !socket.isClosed() )
					socket.close();
				socket = null;
			}
			cursor.close();
			System.out.printf("Processed %d VSDM messages.", msgCount);
			if (renderer != null) {
				renderer.render(GeoPoint.endOfData);
			}
		}
	}
	
	protected boolean establishTrust(SemiDialogID dialogID, int requestID) throws TrustEstablishmentException {
		if ( !configuration.trust.establish )
			return true;
		TrustEstablishment te = new TrustEstablishment(coder, dialogID, configuration.other.groupID, requestID, configuration.destination.host, configuration.destination.port, (InetAddress)null, configuration.destination.replyPort, configuration.destination.port);
		te.setTimeout(configuration.trust.timeout);
		te.setAttempts(configuration.trust.attempts);
		te.setVerbose(configuration.other.verbose);
		boolean established = te.establishTrust(configuration.trust.attempts, configuration.trust.timeout);
		String message = established ?  "Successfully established trust with SDC!" : "WARNING: Could not establish trust with SDC!";		
		if ( renderer != null )
			renderer.render(message);
		System.err.println(message);
		return established || configuration.trust.ignore ;
	}
	
	protected void sendIntersectionSituationDataAcceptance(final DatagramSocket socket, final int requestID) {
		if ( requestID == -1 ) {
			System.err.println("Skipped sending data acceptance message");
			return;
		}
		final IntersectionSituationDataAcceptance dataAcceptance = new IntersectionSituationDataAcceptance();
		dataAcceptance.setDialogID(SemiDialogID.intersectionSitDataDep);
		dataAcceptance.setSeqID(SemiSequenceID.accept);
		dataAcceptance.setRequestID(TemporaryIDHelper.toTemporaryID(requestID));
		dataAcceptance.setRecordsSent(msgCount);
		try {
			final byte[] payload = CVSampleMessageBuilder.messageToEncodedBytes(dataAcceptance);
			// schedule send to run in 1 second
			new Timer().schedule(new TimerTask() {          
			    @Override
			    public void run() {
			        try {
			        	send(socket, payload);
			        } catch (Exception ex) {
						printExceptionMessage("Couldn't send IntersectionSituationDataAcceptance message.", ex);
					}
			    }
			}, 1000);
			// and receive IntersectionSituationDataReceipt (DataReceipt)
			receiveIntersectionSituationDataReceipt(requestID, 4000);
		} catch (EncodeFailedException ex) {
			printExceptionMessage("Couldn't encode IntersectionSituationDataAcceptance message because encoding failed.", ex);
		} catch (EncodeNotSupportedException ex) {
			printExceptionMessage("Couldn't encode IntersectionSituationDataAcceptance message because encoding is not supported.", ex);
		}
	}
	
	public void receiveIntersectionSituationDataReceipt(final int requestID, final int timeout) {
		final int maxPacketSize = 4096;
		final int destPort = configuration.destination.replyPort;
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(destPort);
			socket.setSoTimeout(timeout);
			DatagramPacket packet = new DatagramPacket(new byte[maxPacketSize], maxPacketSize);
			socket.receive(packet);
			
			final byte[] data = packet.getData();
			final int length  = packet.getLength();				
			final int offset  = packet.getOffset();

			byte[] packetData = Arrays.copyOfRange(data, offset, length);
			try {				
				AbstractData pdu = J2735Util.decode(coder, packetData);
				String message;
				if (pdu instanceof DataReceipt) {
					DataReceipt dataReceipt = (DataReceipt) pdu;
					int dataReceiptRequestID = TemporaryIDHelper.fromTemporaryID(dataReceipt.getRequestID());
					message = dataReceiptRequestID == requestID ? 
						"Successfully Received DataReceipt" :
						String.format("Received Foreign DataReceipt. Expected requestID %d, actual %d", requestID, dataReceiptRequestID);
				} else {
					message = pdu != null ? 
						"Received unexpected message of type " + pdu.getClass().getName() :
						"Received null message";
				}
				System.out.println(message);
				if( renderer != null )
					renderer.render(message);
			} catch (DecodeFailedException ex) {
				printExceptionMessage("Couldn't decode J2735 ASN.1 BER message because decoding failed", ex);
			} catch (DecodeNotSupportedException ex) {
				printExceptionMessage("Couldn't decode J2735 ASN.1 BER message because decoding is not supported", ex);				
			}
		} catch (SocketException ex) {
			printExceptionMessage(String.format("Caught socket exception while recieving DataReceipt message on port %d. Max size is %d", destPort, maxPacketSize), ex);
		} catch (IOException ex) {
			printExceptionMessage(String.format("Caught IO exception exception while recieving DataReceipt message on port %d. Max size is %d", destPort, maxPacketSize), ex);
		} finally {
			if ( socket != null &&  !socket.isClosed() ) {
				socket.close();
				socket = null;
			}
		}
	}
	
	protected void send(DatagramSocket socket, byte[] payload) throws Exception {
		if ( payload == null )
			return;

	    try {
	    	if ( debug )
	    		System.out.printf("Sending message to host '%s' on port %d\n", configuration.destination.host.getHostAddress(), configuration.destination.port);
	        DatagramPacket packet = new DatagramPacket(payload, payload.length, configuration.destination.host, configuration.destination.port);
	        socket.send(packet);
	    } catch (SocketException ex) {
	    	throw new Exception("Couldn't send packet because socket closed.", ex);
	    } catch (IOException ex) {
	    	throw new Exception("Couldn't send packet due to IO exception.", ex);
	    } 
	}

    static private void nap(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
        }
    }
    
    public static void printExceptionMessage(String header, Exception ex) {
    	String message = ex.getMessage();
    	if ( StringUtils.isBlank(message) ) {
    		Throwable cause = ex.getCause();
    		if ( cause != null )
    			message = cause.getMessage();
    	}
    	if ( StringUtils.isBlank(message) ) {
    		System.err.println(header);
    		ex.printStackTrace();
    	} else {
    		System.err.println(header + " Reason: " + message);
    	}
    }
    
    public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("Usage: MongoDbSdpcSenderProvider configuration_file");
			return;
		}
		
        JSONObject conf = JsonHelper.createJsonFromFile(args[0]);
        if ( conf == null ) 
    		return;
		
		try {
			MongoDbSdpcSenderProvider provider = new MongoDbSdpcSenderProvider(null, conf);
			try {
				provider.init();
				provider.start();
				while( provider.started )
					MongoDbSdpcSenderProvider.nap(500);
			} finally {
				provider.dispose();
			}
		} catch (InitializationException ex ) {
			printExceptionMessage("Initialization exception while sending packets.", ex);
			ex.printStackTrace();
		} catch (IOException ex) {
			printExceptionMessage("IOException exception while sending packets.", ex);
			ex.printStackTrace();
		} catch ( Exception ex ) {
			printExceptionMessage("Unexpected exception while sending packets.", ex);
			ex.printStackTrace();
		} 
    }
}
