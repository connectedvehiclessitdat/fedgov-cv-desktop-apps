package gov.usdot.desktop.apps.provider;

import gov.usdot.asn1.generated.j2735.J2735;
import gov.usdot.asn1.j2735.J2735Util;
import gov.usdot.desktop.apps.data.GeoPoint;
import gov.usdot.desktop.apps.renderer.GeoPointRenderer;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import com.oss.asn1.AbstractData;
import com.oss.asn1.Coder;

public class JMSGeoPointProvider implements GeoPointProvider {

	protected GeoPointRenderer renderer;
	protected JSONObject json;
	private MessageConsumer consumer;
	private Connection connection;
	private Session session;
	private Coder coder;
	private long consumerTimeout = 0;
	
	private BlockingQueue<AbstractData> vehicleQueue = new LinkedBlockingQueue<AbstractData>();
	private boolean stopped = false;

	public JMSGeoPointProvider(GeoPointRenderer renderer, JSONObject json) {
		this.renderer = renderer;
		this.json = json;
		if ( json != null )
			consumerTimeout = json.optInt("consumerTimeout", 0);
	}

	public void init() throws InitializationException {
		if (connection == null) {
			try {
				JMSConnectionBuilder builder = new JMSConnectionBuilder(json);
				connection = builder.buildSecureConnection();
				session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				String topic = json.getString("brokerTopic");
				Destination dest = session.createTopic(topic);
				createConsumer(dest);
				
				J2735.initialize();
				coder = J2735.getPERUnalignedCoder();
				consumer.setMessageListener(new VehicleMessageListener(renderer, coder));
			} catch(Exception e) {
				dispose();
				throw new InitializationException("", e);
			}
		}
	}
	
	private void createConsumer(Destination dest) throws JMSException, InitializationException {
		final long FIVE_SECONDS = 5*1000;
		long startedTime = System.currentTimeMillis();
		long timeoutTime = startedTime + consumerTimeout;
		do {
			try {
				consumer = session.createConsumer(dest, null, false);
				return;
			} catch ( Exception ex ) {
				String msg = ex.getLocalizedMessage();
				if ( msg == null || !msg.startsWith("User anonymous is not authorized to create: topic") )
					throw new InitializationException("", ex);
			}
			System.out.println("Waiting for subscrition to become active\n");
			renderer.render("Waiting for subscrition to become active\n");
			try {
				Thread.sleep(FIVE_SECONDS);
			} catch (InterruptedException e) {
			}
		} while( System.currentTimeMillis() < timeoutTime );
		throw new InitializationException(String.format("Couldn't start provider after %d sec of waiting", consumerTimeout/1000));
	}
	
	public void dispose() {
		try {J2735.deinitialize();} catch (Exception ignore) { }
		try {consumer.close();} catch (Exception ignore) { }
		try {session.close();} catch (Exception ignore) { }
		try {connection.close();} catch (Exception ignore) { }
		consumer = null;
		connection = null;
		session = null;
	}

	public void start() throws InitializationException {
		stopped = false;
		Thread t = new Thread(new VehicleQueueConsumer(this.renderer));
		t.start();
		
		if (connection != null) {
			try {
				connection.start();
			} catch (JMSException e) {
				throw new InitializationException("", e);
			}
		}
	}
	
	public void stop() throws InitializationException {
		stopped = true;
		if (connection != null) {
			try {
				connection.stop();
			} catch (JMSException e) {
				throw new InitializationException("", e);
			}
		}
		if (renderer != null) {
			renderer.render(GeoPoint.endOfData);
		}
	}
	
	public void pause() throws InitializationException {
		// TODO Auto-generated method stub
		
	}

	public void resume() throws InitializationException {
		// TODO Auto-generated method stub
		
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
	
	private static final byte[] readyMessage = "This topic is now active.".getBytes();

	private class VehicleMessageListener implements MessageListener {
		
		private Coder coder;
		
		public VehicleMessageListener(GeoPointRenderer renderer, Coder coder) {
			this.coder = coder;
		}
		
		public void onMessage(Message message) {
			if ( message == null )
				return;
			try {
				byte[] bytes = getMessageBytes(message);
				if ( Arrays.equals(bytes, readyMessage) ) {
					String msg = new String(readyMessage);
					renderer.render(msg);
					System.out.println(msg);
					return;
				}
				AbstractData vehicleMessage = J2735Util.decode(coder, bytes);
				if ( vehicleMessage != null )
					vehicleQueue.put(vehicleMessage);
				else
					System.out.printf("Couldn't decode message from bytes (hex encoded): %s\n", bytes != null ? Hex.encodeHexString(bytes) : "null");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		
		private byte[] getMessageBytes(Message message) throws JMSException {
			byte[] bytes = null;
			if (message instanceof BytesMessage) {
				BytesMessage bytesMessage = (BytesMessage)message;
				bytes = new byte[(int)bytesMessage.getBodyLength()];
				bytesMessage.readBytes(bytes);
			} else if (message instanceof TextMessage) {
				TextMessage textMessage = (TextMessage)message;
				bytes = Base64.decodeBase64(textMessage.getText());
			}
			// TODO 16902 processing
			return bytes;
		}
	}
	
	private class VehicleQueueConsumer implements Runnable {

		private GeoPointRenderer renderer;
		private int msgCount = 0;
		
		public VehicleQueueConsumer(GeoPointRenderer renderer) {
			this.renderer = renderer;
		}
		
		public void run() {
			int delay = json.optInt("replayDelayMillis", 1000);
			if (delay <= 0) {
				delay = 1000;
			}
			if ( json.has("brokerTopic") ) {
				String subscriberID = json.optString("brokerTopic", "undefined");
				String message = "Connected with Subscriber ID: " + subscriberID;
				System.out.printf("Subscriber ID: %s\n", subscriberID);
				if ( renderer != null )
					renderer.render(message);
			}
			while (!stopped) {
				AbstractData vehicleMessage = vehicleQueue.poll();
				if (vehicleMessage != null) {
					msgCount++;
					if (json.getBoolean("printData")) {
						System.out.println(vehicleMessage);
					}
					if (renderer != null) {
						renderer.render(new GeoPoint(vehicleMessage));
					}
				}
				try { Thread.sleep(delay); } catch (InterruptedException ignore) {}
			}
			System.out.printf("Received %d VSDM messages.\n", msgCount);
		}
		
	}

}
