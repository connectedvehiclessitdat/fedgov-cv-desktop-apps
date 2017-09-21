package gov.usdot.desktop.apps.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import net.sf.json.JSONObject;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSslConnectionFactory;

import gov.usdot.cv.resources.PrivateResourceLoader;

public class JMSConnectionBuilder {

	private JSONObject json;
	private ActiveMQSslConnectionFactory factory;
	
	public JMSConnectionBuilder(JSONObject json) {
		this.json = json;
	}
	
	public Connection buildConnection() throws JMSException {
		String username = json.getString("brokerUsername");
		String password = json.getString("brokerPassword");
		String url = json.getString("brokerUrl");
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(username, password, url);
		return factory.createConnection();
	}
	
	public Connection buildSecureConnection() throws KeyStoreException, 
		NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, JMSException {
		
		if (factory == null) {
			InputStream keystore;
			String keystoreFilePath = json.getString("keystoreFile");
	    	if(PrivateResourceLoader.isPrivateResource(keystoreFilePath)) {
	    		keystore = PrivateResourceLoader.getFileAsStream(keystoreFilePath);
	    	}
	    	else {
	    		keystore = new FileInputStream(new File(keystoreFilePath));
	    	}
	    	
	    	InputStream truststore;
			String truststoreFilePath = json.getString("truststoreFile");
	    	if(PrivateResourceLoader.isPrivateResource(truststoreFilePath)) {
	    		truststore = PrivateResourceLoader.getFileAsStream(truststoreFilePath);
	    	}
	    	else {
	    		truststore = new FileInputStream(new File(truststoreFilePath));
	    	}
	    	
			String brokerUrl = json.getString("brokerUrl");
			
			factory = new ActiveMQSslConnectionFactory();
			factory.setBrokerURL(brokerUrl);
			
			String store_password;
	    	if(PrivateResourceLoader.isPrivateResource(json.getString("storePassword"))) {
	    		store_password = PrivateResourceLoader.getProperty(json.getString("storePassword"));
	    	}
	    	else {
	    		store_password = json.getString("storePassword");
	    	}
	    	
			TrustManager[] trustManagers = getTrustManagers(truststore, store_password);
			KeyManager[] keyManagers = getKeyManagers(keystore, store_password, "");
			
			SecureRandom secureRandom = new SecureRandom();
			
			factory.setKeyAndTrustManagers(keyManagers, trustManagers, secureRandom);
		}
		
		String username = json.has("brokerUsername") ? json.getString("brokerUsername") : null;
		String password = json.has("brokerPassword") ? json.getString("brokerPassword") : null;
		if (username != null && password != null) {
			return factory.createConnection(username, password);
		} else {
			// Create an anonymous connection to the jms server.
			// The server must be configured to support it.
			return factory.createConnection();
		}
	}
	
	private KeyManager[] getKeyManagers(InputStream keyStoreFile,
			String keyStorePassword, String certAlias)
			throws KeyStoreException, IOException, NoSuchAlgorithmException,
			CertificateException, UnrecoverableKeyException {

		KeyStore keyStore = KeyStore.getInstance("JKS");
		char[] keyStorePwd = (keyStorePassword != null) ? keyStorePassword
				.toCharArray() : null;
		keyStore.load(keyStoreFile, keyStorePwd);

		// if certAlias given then load single cert with given alias
		if (certAlias != null) {
			Enumeration<String> aliases = keyStore.aliases();

			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();

				if (!certAlias.equals(alias)) {
					keyStore.deleteEntry(alias); // remove cert only load
													// certificate with given
													// alias
				}
			}
		}
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
				.getDefaultAlgorithm());
		kmf.init(keyStore, keyStorePwd);

		return kmf.getKeyManagers();

	}

	private TrustManager[] getTrustManagers(InputStream trustStoreFile,
			String trustStorePassword) throws IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException {

		KeyStore trustStore = KeyStore.getInstance("JKS");
		char[] trustStorePwd = (trustStorePassword != null) ? trustStorePassword
				.toCharArray() : null;
		trustStore.load(trustStoreFile, trustStorePwd);
		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);

		return trustManagerFactory.getTrustManagers();

	}
}
