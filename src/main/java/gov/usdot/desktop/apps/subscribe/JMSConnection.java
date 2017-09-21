package gov.usdot.desktop.apps.subscribe;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

/**
 * Utility class for encapsulating the work of creating a JMS Connection and adding a topic subscriber.
 */
public class JMSConnection {

	private static Logger logger = Logger.getLogger(JMSConnection.class);
	
	private Connection connection;
	private Session session;
	private MessageConsumer consumer;
	
	private JMSParameters jmsParams;
	
	public JMSConnection(JMSParameters jmsParams) {
		this.jmsParams = jmsParams;
	}
	
	public void openConnection() throws JMSException, 
		KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, URISyntaxException {
		
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(jmsParams.getUserName(), 
				jmsParams.getPassword(), jmsParams.getBrokerURL());
		
		if (jmsParams.getKeystoreFile() != null && jmsParams.getTruststoreFile() != null) {
			ActiveMQSslTransportFactory transportFactory = new ActiveMQSslTransportFactory();
			transportFactory.initialize(jmsParams);
		}
		connection = factory.createConnection();
		
		logger.info("JMS connection opened to " + jmsParams.getBrokerURL());
	}
	
	public void closeConnection() {
		try {consumer.close();} catch (Exception ignore) { }
		try {session.close();} catch (Exception ignore) { }
		try {connection.close();} catch (Exception ignore) { }
		consumer = null;
		connection = null;
		session = null;
		logger.info("JMS connection closed to " + jmsParams.getBrokerURL());
	}
	
	public void addTopicListener(String topic, MessageListener listener) throws JMSException {
		if (connection != null) {
			if (session == null) {
				session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			}
			Destination dest = session.createTopic(topic);
			consumer = session.createConsumer(dest, null, false);
			consumer.setMessageListener(listener);
			logger.info("Listener added to topic " + jmsParams.getTopic());
		}
	}
	
	public void startConnection() throws JMSException {
		if (connection != null) {
			connection.start();
			logger.info("JMS connection started");
		}
	}
}
