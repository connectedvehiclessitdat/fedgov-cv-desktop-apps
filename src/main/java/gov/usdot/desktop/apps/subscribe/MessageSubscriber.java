package gov.usdot.desktop.apps.subscribe;

import gov.usdot.cv.security.SecureConfig;
import gov.usdot.cv.security.SecurityHelper;
import gov.usdot.cv.security.crypto.CryptoProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * Subscribes to the JMS Server for messages and processes them as they come in. 
 */
public class MessageSubscriber {

	private static Logger logger = Logger.getLogger(MessageSubscriber.class);
	private JMSConnection jmsConn;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS");
	private CryptoProvider cryptoProvider;
	
	private static long VEH_SIT_DATA_ID = 154L;
	private static long ADVISORY_SIT_DATA_ID = 156L; //Traveler
	private static long INTERSECTION_SIT_DATA_ID = 162;
	
	private static final String OUTPUT_DIR = "output";
	
	public MessageSubscriber() {
		CryptoProvider.initialize();
        cryptoProvider = new CryptoProvider();
	}
	
	public void start() {
		boolean connected = false;
		while (!connected) {
			try {
				Properties props = new Properties();
				props.load(new FileInputStream("config/jms.properties"));
				JMSParameters jmsParams = new JMSParameters(props);
				jmsConn = new JMSConnection(jmsParams);
				jmsConn.openConnection();
				jmsConn.addTopicListener(jmsParams.getTopic(), new VehSitDataMessageListener(jmsParams.isSecure()));
				jmsConn.startConnection();
				connected = true;
			} catch (Exception e) {
				logger.error("Error starting MessageSubscriber, retrying in 30 seconds", e);
				try { Thread.sleep(30000); } catch (InterruptedException ignore) { }
			}
		}
	}
	
	public void stop() {
		if (jmsConn != null) {
			jmsConn.closeConnection();
		}
	}
	
	private class VehSitDataMessageListener implements MessageListener {
		private boolean secure;
		
		public VehSitDataMessageListener(boolean secure) {
			this.secure = secure;
		}
		
		public void onMessage(Message message) {
			try {
				if (message instanceof BytesMessage) {
					BytesMessage bytesMessage = (BytesMessage)message;
					byte[] bytes = new byte[(int)bytesMessage.getBodyLength()];
					bytesMessage.readBytes(bytes);
					
					if (secure) {
						bytes = SecurityHelper.decrypt(bytes, cryptoProvider);
					}
					
					long dialogID = getMessageID(bytes);
					String fileName = getFileName(dialogID);
					if (fileName != null) {
						File f = new File(OUTPUT_DIR,fileName);
						logger.info("Writing file: " + f.getAbsolutePath());
						FileOutputStream output = new FileOutputStream(f);
						IOUtils.write(bytes, output);
					} else {
						logger.error("Unknown dialogID: " + dialogID);
					}
					
				} else {
					logger.error("Unexpected message type " + message.getClass().getName());
				}
			} catch (Exception e) {
				logger.error("Error in VehSitDataMessageListener", e);
			}
		}
	}
	
	private String getFileName(long dialogID) {
		String fileName = null;
		String prefix = null;
		if (dialogID == VEH_SIT_DATA_ID) {
			prefix = "vsd";
		} else if (dialogID == INTERSECTION_SIT_DATA_ID) {
			prefix = "isd";
		} else if (dialogID == ADVISORY_SIT_DATA_ID) {
			prefix = "adv";
		}
		
		if (prefix != null) {
			fileName = String.format("%s_%s_%s.ber", prefix, sdf.format(new Date()), System.currentTimeMillis());
		}
		return fileName;
	}
	
	private long getMessageID(byte message[])
    {
		if (message == null) {
			logger.error("Message to decode is null, cannot decode!");
			return 0;
		}
		
		// We need to find where the DialogID starts in the message, this is how we do it
		// For more info on why we do this see http://luca.ntop.org/Teaching/Appunti/asn1.html
		// Look at Section 3.1 under Length octets, short form (length < 128) and long form (length > 127)
		// Examples below:
		// Short form: 30 7C 80 (2nd octet is < 128, 2nd octet gives length of remaining message, 3rd octet is start of dialogID)
		// Long form1: 30 81 9B 80 (2nd octet is > 127 (0x81=129) so long form, bits 7-1 of 2nd octet give 
		// number of length octets, bits 7-1 of 0x81 = 1 so we know that 9B is length of remaining message and 80 is dialogID)
		// Long form2: 30 82 02 09 80 (2nd octet is > 127 (0x82=130) so long form, bits 7-1 of 2nd octet give 
		// number of length octets, bits 7-1 of 0x82 = 2 so we know that 02 09 is length of remaining message and 80 is dialogID)
		
		// We start with a dialogID index of 2 which is always true for all short forms
		// then we check for long form and add on the additional length octets if needed
		int dialogIDTagIndex = 2;
		int length = message[1] & 0xFF;
		if (length > 127) {
			// taking bits 7-1, that gives us the additional length octets, add them to the intial offset
			dialogIDTagIndex+= (length & 0x7F);
		}
		
		if (message[dialogIDTagIndex] != (byte)0x80) {
			// the dialogID has to start with 0x80, if not we have a big problem
			logger.error("Start of DialogID (0x80) not found at index " + dialogIDTagIndex);
			return 0;
		}
		
		int dialogIDLength = (int)message[(dialogIDTagIndex+1)];
		int startIndex = dialogIDTagIndex + 2;
		int endIndex = startIndex + dialogIDLength;
		byte[] dialogIDBytes = Arrays.copyOfRange(message, startIndex, endIndex);
		long dialogID = new BigInteger(dialogIDBytes).longValue();
		
		return dialogID;
    }
	
	public static void main(String[] args) throws InterruptedException {
		@SuppressWarnings("rawtypes")
		Enumeration appenders = LogManager.getRootLogger().getAllAppenders();
        if (!appenders.hasMoreElements()) {
        	Logger rootLogger = Logger.getRootLogger();
        	rootLogger.setLevel(Level.DEBUG);
        	
        	PatternLayout layout = new PatternLayout("%d{ISO8601} %5p %c{1}:%L - %m%n");
        	rootLogger.addAppender(new ConsoleAppender(layout));
        	rootLogger.info("Log4J not configured! Setting up default console configuration");
        }
        
		String configFile = args.length > 0 ? args[0] : null;
		try {
			if (configFile != null) {
				SecureConfig config = new SecureConfig(configFile);
				SecurityHelper.loadCertificates(config);
			}
		} catch (Exception e3) {
			e3.printStackTrace();
		}
		
		MessageSubscriber subscriber = new MessageSubscriber();
		subscriber.start();
		File dir = new File(OUTPUT_DIR);
		if (!dir.exists())
			dir.mkdir();
		logger.info("Listening for messages...");
		while (true) {
			Thread.sleep(1000);
		}
	}
}
