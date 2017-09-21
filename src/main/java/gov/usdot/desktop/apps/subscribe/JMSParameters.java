package gov.usdot.desktop.apps.subscribe;

import java.util.Properties;

/**
 * Parameters for connecting to a JMS Server.
 */
public class JMSParameters {

	private String userName;
	private String password;
	private String brokerURL;
	private String keystoreFile;
	private String truststoreFile;
	private String storePassword;
	private String topic;
	private boolean secure;
	
	public JMSParameters(String userName, String password, String brokerURL) {
		super();
		this.userName = userName;
		this.password = password;
		this.brokerURL = brokerURL;
	}
	
	public JMSParameters(String userName, String password, String brokerURL, 
			String keystoreFile, String truststoreFile, String storePassword, String topic) {
		super();
		this.userName = userName;
		this.password = password;
		this.brokerURL = brokerURL;
		this.keystoreFile = keystoreFile;
		this.truststoreFile = truststoreFile;
		this.storePassword = storePassword;
		this.topic = topic;
	}
	
	public JMSParameters(Properties props) {
		super();
		this.userName = props.getProperty("userName");
		this.password = props.getProperty("password");
		this.brokerURL = props.getProperty("brokerURL");
		this.keystoreFile = props.getProperty("keystoreFile");
		this.truststoreFile = props.getProperty("truststoreFile");
		this.storePassword = props.getProperty("storePassword");
		this.topic = props.getProperty("topic");
		this.secure = Boolean.valueOf(props.getProperty("secure"));
	}
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getBrokerURL() {
		return brokerURL;
	}
	public void setBrokerURL(String brokerURL) {
		this.brokerURL = brokerURL;
	}
	public String getKeystoreFile() {
		return keystoreFile;
	}
	public void setKeystoreFile(String keystoreFile) {
		this.keystoreFile = keystoreFile;
	}
	public String getTruststoreFile() {
		return truststoreFile;
	}
	public void setTruststoreFile(String truststoreFile) {
		this.truststoreFile = truststoreFile;
	}
	public String getStorePassword() {
		return storePassword;
	}
	public void setStorePassword(String storePassword) {
		this.storePassword = storePassword;
	}
	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public boolean isSecure() {
		return secure;
	}
	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	@Override
	public String toString() {
		return "JMSConnectionParameters [userName=" + userName + ", password=" + password + ", brokerURL=" + brokerURL
				+ ", keystoreFile=" + keystoreFile + ", truststoreFile=" + truststoreFile + ", storePassword="
				+ storePassword + ", secure=" + secure + "]";
	}
	
}
