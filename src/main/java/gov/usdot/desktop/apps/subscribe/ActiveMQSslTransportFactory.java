package gov.usdot.desktop.apps.subscribe;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.activemq.broker.SslContext;
import org.apache.activemq.transport.TransportFactory;
import org.apache.activemq.transport.tcp.SslTransportFactory;
import org.apache.activemq.util.IOExceptionSupport;

import gov.usdot.cv.resources.PrivateResourceLoader;

/**
 * Assists in setting up the SSL connection to ActiveMQ. 
 *
 */
public class ActiveMQSslTransportFactory extends SslTransportFactory {

    private SslContext sslContext = null;
    
    public ActiveMQSslTransportFactory() {
    	super();
    }
    
    public void initialize(JMSParameters jmsParams) throws URISyntaxException, KeyStoreException, 
    	NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException  {
    	InputStream keystore = null;
    	if(PrivateResourceLoader.isPrivateResource(jmsParams.getKeystoreFile())) {
    		keystore = PrivateResourceLoader.getFileAsStream(jmsParams.getKeystoreFile());
    	}
    	else {
    		keystore = new FileInputStream(jmsParams.getKeystoreFile());
    	}
    	
    	InputStream truststore = null;
    	if(PrivateResourceLoader.isPrivateResource(jmsParams.getTruststoreFile())) {
    		truststore = PrivateResourceLoader.getFileAsStream(jmsParams.getTruststoreFile());
    	}
    	else {
    		truststore = new FileInputStream(jmsParams.getTruststoreFile());
    	}
    	
		String store_password = null; 
    	if(PrivateResourceLoader.isPrivateResource(jmsParams.getStorePassword())) {
    		store_password = PrivateResourceLoader.getProperty(jmsParams.getStorePassword());
    	}
    	else {
    		store_password = jmsParams.getStorePassword();
    	};
    	
		TrustManager[] trustManagers = getTrustManagers(truststore, store_password);
		KeyManager[] keyManagers = getKeyManagers(keystore, store_password, "");
		SecureRandom secureRandom = new SecureRandom();
		
		sslContext = new SslContext(keyManagers, trustManagers, secureRandom);
        TransportFactory.registerTransportFactory("ssl", this);
    }
    
    @Override
    protected ServerSocketFactory createServerSocketFactory() throws IOException {
    	
        if (null != sslContext) {
            try {
                return sslContext.getSSLContext().getServerSocketFactory();
            } catch (Exception e) {
                throw IOExceptionSupport.create(e);
            }
        }
        
        return super.createServerSocketFactory();
        
    }

    @Override
    protected SocketFactory createSocketFactory() throws IOException {
    	
        if (null != sslContext) {
            try {
                return sslContext.getSSLContext().getSocketFactory();
            } catch (Exception e) {
                throw IOExceptionSupport.create(e);
            }
        }
        
        return super.createSocketFactory();
        
    }

    private KeyManager[] getKeyManagers(InputStream keyStoreStream,
			String keyStorePassword, String certAlias)
			throws KeyStoreException, IOException, NoSuchAlgorithmException,
			CertificateException, UnrecoverableKeyException {

		KeyStore keyStore = KeyStore.getInstance("JKS");
		char[] keyStorePwd = (keyStorePassword != null) ? keyStorePassword
				.toCharArray() : null;
		keyStore.load(keyStoreStream, keyStorePwd);

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

	private TrustManager[] getTrustManagers(InputStream trustStoreStream,
			String trustStorePassword) throws IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException {

		KeyStore trustStore = KeyStore.getInstance("JKS");
		char[] trustStorePwd = (trustStorePassword != null) ? trustStorePassword
				.toCharArray() : null;
		trustStore.load(trustStoreStream, trustStorePwd);
		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustStore);

		return trustManagerFactory.getTrustManagers();

	}
}