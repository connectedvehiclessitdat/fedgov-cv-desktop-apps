package gov.usdot.desktop.apps.gui.subscription;

import gov.usdot.asn1.j2735.CVTypeHelper;
import gov.usdot.cv.common.dialog.DPCSubscription;
import gov.usdot.cv.common.dialog.DPCSubscriptionException;
import gov.usdot.cv.common.util.UnitTestHelper;
import gov.usdot.cv.security.SecureConfig;
import gov.usdot.cv.security.SecurityHelper;
import gov.usdot.cv.security.cert.CertificateException;
import gov.usdot.cv.security.crypto.CryptoException;
import gov.usdot.desktop.apps.gui.controls.NumericTextField;
import gov.usdot.desktop.apps.gui.controls.TextAreaOutputStream;
import gov.usdot.desktop.apps.provider.InitializationException;
import gov.usdot.desktop.apps.provider.JMSGeoPointProvider;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.PrintStream;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.SoftBevelBorder;

import net.sf.json.JSONObject;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;

public class SubscriptionForm extends JFrame{
	
	private static final long serialVersionUID = 1L;
	
	/*****************************************************
	 * Declare Variables
	 *****************************************************/
	
	private static String[] vsmStrings = { "fund", "vehstat", "weather","env", "elveh"}; //order must match VSM method
	
	private JLabel lblReturnIpAddress = new JLabel("Return Address");
	private JLabel lblWHIpAddress = new JLabel("Warehouse IP");
	private JLabel lblRequestID = new JLabel("Request ID");
	private JLabel lblSeCorner = new JLabel("SE Corner");
	private JLabel lblNeCorner = new JLabel("NW Corner");
	private JLabel lblLatitude = new JLabel("Latitude");
	private JLabel labelLongitude = new JLabel("Longitude");
	private JLabel lblPort = new JLabel("Port");
	private JLabel lblRtnPort = new JLabel("Port");
	private JLabel lblSubID = new JLabel("Sub ID");
	private JLabel lblVsmType = new JLabel("VSM Type");
	private JLabel lblRequestTimeout = new JLabel("Subscription Life");
	private JLabel lblMinutes = new JLabel("minutes");
	private static NumericTextField txtRequestID = new NumericTextField();
	private static JRadioButton rdbtnRequest = new JRadioButton("Request");
	private static JRadioButton rdbtnCancel = new JRadioButton("Cancel");
	private static JTextField txtIp = new JTextField();
	private static JTextField txtWHIp = new JTextField();
	private static NumericTextField textPort = new NumericTextField();
	private static NumericTextField txtRtnPort = new NumericTextField();
	private static NumericTextField txtMinutes = new NumericTextField();
	private static NumericTextField txtSubID = new NumericTextField();
	private static JList listVSM = new JList(vsmStrings);
	private static NumericTextField txtNWLat = new NumericTextField();
	private static NumericTextField txtNWLong = new NumericTextField();
	private static NumericTextField txtSELat = new NumericTextField();
	private static NumericTextField txtSELong = new NumericTextField();

	private static JButton btnSendReq = new JButton("Send");
	private static JButton btnSendCancel = new JButton("Send");
	private static JButton btnClear = new JButton("Clear");
	private static JButton btnTest = new JButton("Test");
	private static JButton btnStopTest = new JButton("Stop Test");
	private static JButton btnSave = new JButton("Save");
	private static JButton btnUploadLastSave = new JButton("Load");
	
	private static JTextArea txtResponse = new JTextArea();
	private static JScrollPane scroll = new JScrollPane();
	
	private static int cancelCounter = 1;
	private static int reqCounter = 1;
	private static JCheckBox chckbxServiceRegion = new JCheckBox("Service Region");
	private final JLayeredPane requestPane = new JLayeredPane();
	private final JLayeredPane regionPane = new JLayeredPane();
	private final JLayeredPane cancelPane = new JLayeredPane();
	private final JLayeredPane top = new JLayeredPane();
	private final JLayeredPane bottom = new JLayeredPane();
	private final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
	
	private String brokerHost = "10.114.237.115";
	
	private static SecureConfig config;
	
	/*****************************************************
	 * Form View
	 *****************************************************/
	
	private final String welcomeText = "<html><body><font size=4 color=#084B8A>This application allows creating, testing, and, cancelling subscriptions for the Vehicle Situation Data messages<br>to the Connected Vehicles Situation Data Clearinghouse.</font></body></html>";
	
	public SubscriptionForm(String configFile) {
		
		try {
			if (configFile != null) {
				config = new SecureConfig(configFile);
				SecurityHelper.loadCertificates(config);
			}
		} catch (Exception e3) {
			e3.printStackTrace();
		}
		
		//Set up standard view
		getContentPane().setLayout(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Manage Subscriptions");
		this.setSize(441,752);
		this.setResizable(false);
		
		JLabel lblWelcome = new JLabel(welcomeText);

		splitPane.setOpaque(false);
		splitPane.setDividerLocation(548);
		splitPane.setOneTouchExpandable(true);
		splitPane.setContinuousLayout(true);
		splitPane.setResizeWeight(0.5);
		
		txtWHIp.setText("192.168.0.0");
		textPort.setText("80");
		txtIp.setText("192.168.0.1");
		txtRtnPort.setText("46751");
		
		lblWHIpAddress.setHorizontalAlignment(SwingConstants.RIGHT);
		lblPort.setHorizontalAlignment(SwingConstants.RIGHT);
		
		requestPane.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		regionPane.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		lblLatitude.setHorizontalAlignment(SwingConstants.CENTER);
		labelLongitude.setHorizontalAlignment(SwingConstants.CENTER);
		chckbxServiceRegion.setToolTipText("Optional Field");
		lblMinutes.setHorizontalAlignment(SwingConstants.CENTER);
		
		listVSM.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		listVSM.setToolTipText("Press ctrl for multiple selections");
		
		cancelPane.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		txtRequestID.setText("0000");
		txtSubID.setText("0000");
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setViewportView(txtResponse);
		
		//Radio button grouping - one selection at a time
		ButtonGroup reqCancel = new ButtonGroup();
		reqCancel.add(rdbtnCancel);
		reqCancel.add(rdbtnRequest);
		
		//Add items to form
		getContentPane().add(splitPane);
		top.add(btnClear);
		top.add(lblWelcome);
		top.add(rdbtnCancel);
		top.add(lblWHIpAddress);
		top.add(txtWHIp);
		top.add(lblPort);
		top.add(textPort);
		top.add(btnSendReq);
		top.add(btnSendCancel);
		top.add(cancelPane);
		top.add(rdbtnRequest);
		top.add(requestPane);
		top.add(btnTest);
		top.add(btnSave);
		top.add(btnUploadLastSave);
		top.add(lblReturnIpAddress);
		top.add(lblRtnPort);
		top.add(txtIp);
		top.add(txtRtnPort);
		bottom.add(scroll);
		
		//Add content to cancel pane
		cancelPane.add(lblRequestID);
		cancelPane.add(txtRequestID);
		cancelPane.add(lblSubID);
		cancelPane.add(txtSubID);
		//Add to request pane
		requestPane.add(lblRequestTimeout);
		requestPane.add(txtMinutes);
		requestPane.add(lblMinutes);
		requestPane.add(lblVsmType);
		requestPane.add(listVSM);
		requestPane.add(regionPane);
			//Add to region Pane
			regionPane.add(chckbxServiceRegion);
			regionPane.add(lblLatitude);
			regionPane.add(labelLongitude);
			regionPane.add(lblNeCorner);
			regionPane.add(lblSeCorner);
			regionPane.add(txtNWLat);
			regionPane.add(txtSELat);	
			regionPane.add(txtNWLong);	
			regionPane.add(txtSELong);	

		//Set bounds
		lblWelcome.setBounds(20, 14, 420, 56);
		splitPane.setBounds(0, 0, 435, 719);
		rdbtnCancel.setBounds(26, 398, 67, 25);
		chckbxServiceRegion.setBounds(8, 9, 113, 25);
		regionPane.setBounds(12, 20, 387, 94);
		lblLatitude.setBounds(148, 13, 76, 16);
		labelLongitude.setBounds(256, 13, 76, 16);
		lblNeCorner.setBounds(46, 43, 75, 16);
		lblSeCorner.setBounds(46, 65, 76, 16);
		txtNWLat.setBounds(148, 43, 76, 16);
		txtSELat.setBounds(148, 65, 76, 16);
		txtNWLong.setBounds(256, 43, 76, 16);
		txtSELong.setBounds(256, 65, 76, 16);
		lblVsmType.setBounds(12, 162, 58, 16);
		lblRequestTimeout.setBounds(177, 162, 97, 16);
		txtMinutes.setBounds(279, 160, 60, 23);
		lblMinutes.setBounds(339, 162, 60, 16);
		cancelPane.setBounds(12, 409, 411, 97);
		listVSM.setBounds(82, 130, 70, 94);
		lblRequestID.setBounds(12, 27, 91, 16);
		txtRequestID.setBounds(98, 24, 226, 22);
		lblSubID.setBounds(12, 59, 61, 16);
		txtSubID.setBounds(98, 56, 226, 22);
		scroll.setBounds(12, 6, 412, 149);
		
		// buttons
		btnSave.setBounds(12, 515, 70, 25);
		btnUploadLastSave.setBounds(90, 515, 70, 25);
		btnSendCancel.setBounds(181, 515, 70, 25);
		btnSendReq.setBounds(181, 515, 70, 25);
		btnTest.setBounds(271, 515, 70, 25);
		btnClear.setBounds(350, 515, 70, 25);
		
		lblPort.setBounds(236, 84, 42, 16);
		textPort.setBounds(290, 81, 58, 22);
		textPort.setColumns(6);
		txtIp.setColumns(14);
		lblWHIpAddress.setBounds(26, 84, 90, 16);
		txtWHIp.setBounds(126, 81, 116, 22);
		requestPane.setBounds(12, 151, 411, 240);
		rdbtnRequest.setBounds(26, 140, 75, 25);

		lblReturnIpAddress.setBounds(26, 116, 90, 16);
		lblReturnIpAddress.setHorizontalAlignment(SwingConstants.RIGHT);
		txtIp.setBounds(126, 114, 116, 22);
		lblRtnPort.setBounds(254, 116, 42, 16);
		txtRtnPort.setBounds(290, 114, 58, 22);
		txtRtnPort.setColumns(6);
		
		txtResponse.setBounds(12, 6, 411, 149);
		
		Dimension tsize = new Dimension(441,548);
		top.setMinimumSize(tsize);
			
		//set visibility
		rdbtnRequest.setSelected(true);
		textPort.setEnabled(true);
		txtRequestID.setEnabled(false);
		txtSubID.setEnabled(false);
		txtResponse.setText("");
		txtResponse.setEditable(false);
		txtResponse.setLineWrap(true);
		txtResponse.setWrapStyleWord(true);
		btnSendCancel.setVisible(false);
		txtNWLat.setEnabled(false);
		txtNWLong.setEnabled(false);
		txtSELat.setEnabled(false);
		txtSELong.setEnabled(false);
		btnStopTest.setVisible(false);
		
				
		//Set pane for cancel view
		rdbtnCancel.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				
			//clear all extra elements
			txtResponse.setText("");
			listVSM.setEnabled(false);
			txtMinutes.setEnabled(false);
			txtNWLat.setEnabled(false);
			txtNWLong.setEnabled(false);
			txtSELat.setEnabled(false);
			txtSELong.setEnabled(false);
			btnSendReq.setEnabled(false);
			btnSendReq.setVisible(false);
			chckbxServiceRegion.setEnabled(false);
			
			txtRequestID.setEnabled(true);
			txtSubID.setEnabled(true);
			btnSendCancel.setVisible(true);
			}
		});

		//Set pane for request view
		rdbtnRequest.addActionListener(new ActionListener(){
			public void actionPerformed (ActionEvent e){
				
			//clear all elements
			txtResponse.setText("");
			txtRequestID.setEnabled(false);
			txtSubID.setEnabled(false);
			btnSendCancel.setVisible(false);
			listVSM.setEnabled(true);
			txtMinutes.setEnabled(true);
			chckbxServiceRegion.setEnabled(true);
			btnSendReq.setEnabled(true);
			btnSendReq.setVisible(true);
			
			if (chckbxServiceRegion.isSelected()){
				txtNWLat.setEnabled(true);
				txtNWLong.setEnabled(true);
				txtSELat.setEnabled(true);
				txtSELong.setEnabled(true);
			}
			
			}
		});
	
	   //Other actions
		
		chckbxServiceRegion.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e){
				if (e.getStateChange() == ItemEvent.SELECTED){
					txtNWLat.setEnabled(true);
					txtNWLong.setEnabled(true);
					txtSELat.setEnabled(true);
					txtSELong.setEnabled(true);
				} else {
					txtNWLat.setEnabled(false);
					txtNWLong.setEnabled(false);
					txtSELat.setEnabled(false);
					txtSELong.setEnabled(false);
				}
			}
		});
		
	   splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener(){
		   public void propertyChange(PropertyChangeEvent pce){
			   int d = splitPane.getDividerLocation();
			   d = getContentPane().getHeight() - d - 20;
			   txtResponse.setSize(412,d);
			   scroll.setSize(412,d);
		   }
	   });
	   
	   btnClear.addActionListener(new ActionListener(){
		   public void actionPerformed(ActionEvent e){
			    txtResponse.setText("");
				txtWHIp.setText("");
				textPort.setText("");
				txtRtnPort.setText("");
				listVSM.clearSelection();
				txtMinutes.setText("");
				txtNWLat.setText("");
				txtNWLong.setText("");
				txtSELat.setText("");
				txtSELong.setText("");
				txtIp.setText("");
				chckbxServiceRegion.setSelected(false);
				txtRequestID.setText("");
				txtSubID.setText("");
		   }
	   });
	   
	   btnSave.addActionListener(new ActionListener(){
		   public void actionPerformed(ActionEvent e){
			   Preferences obj = Preferences.userNodeForPackage(getClass());
			   try {
				obj.clear();
				int vsmSelection[] = listVSM.getSelectedIndices();
				   for(int i = 0; i < vsmSelection.length; i++){
						if(vsmSelection[i]==0){
							obj.putInt("vsmFund", 0);
						}
						if(vsmSelection[i]==1){
							obj.putInt("vsmVehStat", 1);
						}
						if(vsmSelection[i]==2){
							obj.putInt("vsmWeather", 2);
						}
						if(vsmSelection[i]==3){
							obj.putInt("vsmEnv", 3);
						}
						if(vsmSelection[i]==4){
							obj.putInt("vsmElveh", 4);
						}
					}
				    obj.put("warehouseIP",txtWHIp.getText());
				    obj.put("port",textPort.getText());
				    obj.put("rtnPort",txtRtnPort.getText());
				    obj.put("ip",txtIp.getText());
				    obj.put("subLifeMinutes",txtMinutes.getText());
				    obj.put("nwLat",txtNWLat.getText());
				    obj.put("nwLong",txtNWLong.getText());
				    obj.put("seLat",txtSELat.getText());
				    obj.put("seLong",txtSELong.getText());
				    obj.put("requestID",txtRequestID.getText());
				    obj.put("subscriptionID",txtSubID.getText());
				    obj.putBoolean("geoCheck", chckbxServiceRegion.isSelected());
				    obj.put("brokerHost", brokerHost);
			} catch (BackingStoreException e1) {
				txtResponse.append("Save Failed.");
			}
		   }
	   });
	   
	   btnUploadLastSave.addActionListener(new ActionListener(){
		   public void actionPerformed(ActionEvent e){
			   loadLastSaved();
		   }
		   
	   });
	   
	   btnTest.addActionListener(new ActionListener(){
		  public void actionPerformed(ActionEvent e) {
			  testSubscription();
		   }
	   });
		
	   btnSendReq.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			try {
				try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					txtResponse.append("Request Send " + reqCounter + "\n");
					txtResponse.append("Initializing data feed to LCSDW ...\n");
					reqCounter++;
					int subscriptionID = buildMessageReq().request();
					txtResponse.append("   Subscription ID " + subscriptionID + "\n");
					txtSubID.setText(Integer.toString(subscriptionID));
					String message = String.format("Successfully created subscription with ID %d!", subscriptionID);
					txtResponse.append("\n" + message + "\n\n");
					JOptionPane.showMessageDialog(getContentPane(), message, "Subscription Create Succsess", JOptionPane.INFORMATION_MESSAGE);
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			} catch (NumberFormatException e1) {
				txtResponse.append("\nNumber Input Error. Check geo coordinates, minutes, or port.\n\n");
			} catch (DPCSubscriptionException e2){
				String message = String.format("Couldn't create subscription.\nReason: %s", getReason(e2)); 
				txtResponse.append("\nERROR: " + message + "\n\n");
				JOptionPane.showMessageDialog(getContentPane(),message,"Subscription Create Failure",JOptionPane.ERROR_MESSAGE);
			} 
		  }
	   });
  
	   btnSendCancel.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e) {
			String subID = txtSubID.getText();
			try {
				try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					txtResponse.append("Request Send " + cancelCounter + "\n");
					txtResponse.append("Initializing data feed to LCSDW ...\n");
					cancelCounter++;
					buildMessageCancel().cancel(setID(subID));
					String message = String.format("Successfully cancelled subscription with ID %s!", subID);
					txtResponse.append("\n" + message + "\n\n");
					JOptionPane.showMessageDialog(getContentPane(), message, "Subscription Cancel Succsess", JOptionPane.INFORMATION_MESSAGE);
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			} catch (DPCSubscriptionException e1){
				String message = String.format("Couldn't cancel subscription with ID %s.\nReason: %s", subID, getReason(e1));
				txtResponse.append("\nERROR: " + message + "\n\n");
				JOptionPane.showMessageDialog(getContentPane(), message,"Subscription Cancel Failure",JOptionPane.ERROR_MESSAGE);
			}
		  }
	   });
	   
       PrintStream con=new PrintStream(new TextAreaOutputStream(txtResponse));
       System.setOut(con);
       System.setErr(con);
	}
	
	public void loadLastSaved() {
		Preferences obj = Preferences.userNodeForPackage(getClass());
		int[] vsmSelections = { obj.getInt("vsmFund", 100),
				obj.getInt("vsmVehStat", 100), obj.getInt("vsmWeather", 100),
				obj.getInt("vsmEnv", 100), obj.getInt("vsmElveh", 100) };
		listVSM.setSelectedIndices(vsmSelections);
		txtWHIp.setText(obj.get("warehouseIP", "192.168.0.0"));
		textPort.setText(obj.get("port", "46751"));
		txtRtnPort.setText(obj.get("rtnPort", "46751"));
		txtIp.setText(obj.get("ip", "192.168.0.1"));
		txtMinutes.setText(obj.get("subLifeMinutes", "12"));
		txtNWLat.setText(obj.get("nwLat", "48.374353"));
		txtNWLong.setText(obj.get("nwLong", "-131.643968"));
		txtSELat.setText(obj.get("seLat", "24.156250"));
		txtSELong.setText(obj.get("seLong", "-72.347240"));
		txtRequestID.setText(obj.get("requestID", "0000"));
		txtSubID.setText(obj.get("subscriptionID", "10000000"));
		chckbxServiceRegion.setSelected(obj.getBoolean("geoCheck", false));
		brokerHost = obj.get("brokerHost", "10.80.106.157");
	}
	
	private void testSubscription() {
		String subId = txtSubID.getText();
		
        TextField brokerHostTextField = new TextField(brokerHost);
        TextField subIdTextField = new TextField(subId);
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(2, 2, 8, 8));
        centerPanel.add(new JLabel("Subscription Host"));
        centerPanel.add(brokerHostTextField);
        centerPanel.add(new JLabel("Subscription ID"));
        centerPanel.add(subIdTextField);

        int selection = JOptionPane.showOptionDialog(this, 
        		centerPanel, 
        		"Test Subscription Connection", 
                JOptionPane.OK_CANCEL_OPTION, 
                JOptionPane.PLAIN_MESSAGE, 
                null, 
                new String[]{"Connect", "Close"},
                "Test");
        if (selection != 0)
            return;
		
        brokerHost = brokerHostTextField.getText();
        subId = subIdTextField.getText();

		String brokerUrl = String.format("ssl://%s:61616", brokerHost);
		
		String baseMsg = String.format("Subscription Connection to URL %s with Sub ID %s", brokerUrl, subId);
		
		txtResponse.append(String.format("Testing %s ...\n", baseMsg));

		JSONObject config = new JSONObject();
		config.put("brokerUrl", brokerUrl);
		config.put("brokerTopic", subId);
		config.put("keystoreFile", "@keystores/desktop-apps-keystore@");
		config.put("truststoreFile", "@truststores/desktop-apps-truststore@");
		config.put("storePassword", "@desktop-apps/store.password@");
		config.put("replayDelayMillis", new Integer(2000));
		config.put("printData", new Boolean(true));

		String message = "";
		try {
			JMSGeoPointProvider provider = new JMSGeoPointProvider(null, config);
			try {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				provider.init();
				provider.start();
				message = String.format("%s was successful.", baseMsg);
			} finally {
				provider.stop();
				provider.dispose();
				setCursor(Cursor.getDefaultCursor());
			}
			JOptionPane.showMessageDialog(this, message, "Subscription Connection Succsess", JOptionPane.INFORMATION_MESSAGE);
		} catch (InitializationException ex) {
			message = String.format("%s failed.\nReason: %s", baseMsg, getReason(ex));
			JOptionPane.showMessageDialog(this, message, "Subscription Connection Failure", JOptionPane.ERROR_MESSAGE);
		}
		
		txtResponse.append("\n" + message + "\n\n");
	}
	
	private String getReason(Exception ex) {
		String reason = ex.getMessage();
		if ( StringUtils.isBlank(reason) ) {
			Throwable t = ex.getCause();
			if ( t != null )
				reason = t.getMessage();
		}
		if ( StringUtils.isBlank(reason) )
			reason = "Unknown";
		return reason;
	}
	
	public static DPCSubscription buildMessageReq() throws DPCSubscriptionException {
		DPCSubscription dsreq = new DPCSubscription();
		
		if (config != null && config.secure.enable) {
			try {
				dsreq.setSecureEnabled(true);
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CryptoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			dsreq.setPsid(config.secure.psid);
		}
		
		int requestID = dsreq.getRequestID();
		txtResponse.append("   Request ID: " + requestID + "\n");
		txtRequestID.setText(Integer.toString(requestID));
		dsreq.setSendToHost(txtWHIp.getText());
		txtResponse.append("   Warehouse IP: " + dsreq.getSendToHost() + "\n");
		dsreq.setSendToPort(getPortNumber(textPort));
		txtResponse.append("   Warehouse Port: " + dsreq.getSendToPort() + "\n");
		dsreq.setReplyToHost(txtIp.getText());
		txtResponse.append("   Return IP: " + dsreq.getReplyToHost() + "\n");
		dsreq.setReplyToPort(getPortNumber(txtRtnPort));
		txtResponse.append("   Return Port: " + dsreq.getReplyToPort() + "\n");
		dsreq.setEndInMinutes(getEndTime());
		txtResponse.append("   Timeout: " + txtMinutes.getText() + "\n");
			
		if (chckbxServiceRegion.isSelected()){
			String NWLat = txtNWLat.getText();
			String NWLong = txtNWLong.getText();
			String SELat = txtSELat.getText();
			String SELong = txtSELong.getText();
			dsreq.setServiceRegion(setLatLong(NWLat), setLatLong(NWLong), setLatLong(SELat), setLatLong(SELong));
			txtResponse.append("   Service Region: " + NWLat + ", " + NWLong + ", " + SELat + ", " + SELong + "\n");
		}
		
		int vsmSelection[] = listVSM.getSelectedIndices();
		int vsmNumber = getVSMType(vsmSelection);
		dsreq.setVsmTypeValue(vsmNumber);
		txtResponse.append("   VSM Type: " + vsmNumber + "\n");
		
		return dsreq;
	}
	
	public static DPCSubscription buildMessageCancel() throws DPCSubscriptionException {
		DPCSubscription dsc = new DPCSubscription();
		
		if (config != null && config.secure.enable) {
			try {
				dsc.setSecureEnabled(true);
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CryptoException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			dsc.setPsid(config.secure.psid);
		}
		
		int cRequestID = setID(txtRequestID.getText());
		dsc.setRequestID(cRequestID);
		txtResponse.append("   Request ID: " + cRequestID + "\n");
		dsc.setSendToHost(txtWHIp.getText());
		txtResponse.append("   Warehouse IP: " + txtWHIp.getText() + "\n");
		dsc.setReplyToHost(txtIp.getText());
		txtResponse.append("   Return IP: " + dsc.getReplyToHost() + "\n");
		dsc.setReplyToPort(getPortNumber(txtRtnPort));
		txtResponse.append("   Return Port: " + dsc.getReplyToPort() + "\n");
		txtResponse.append("   Subscription ID " + txtSubID.getText() + "\n");
		dsc.setSendToPort(getPortNumber(textPort));
		
		return dsc;
	}
	
	private static int getEndTime(){
		return Integer.parseInt(txtMinutes.getText());
	}
	
	private static int getPortNumber(JTextField txtPort){
		int port = 0;
		String s = txtPort.getText();
		try{port = new Integer(s);}
		catch (NumberFormatException e1){
			txtResponse.append("\nPort Number Invalid\n\n");
			e1.printStackTrace();
		}
		return port;
	}
	
	private static double setLatLong(String latLong) throws NumberFormatException{
		return Double.parseDouble(latLong);
	}
	
	private static int setID(String id) throws NumberFormatException{
		return Integer.parseInt(id);
	}
	
	private static int getVSMType(int[] vsmSelections){
		
		int vsmFinal = 0;
		
		for(int i = 0; i < vsmSelections.length; i++){
			if(vsmSelections[i]==0){
				vsmFinal |= CVTypeHelper.VsmType.FUND.intValue();
			}
			if(vsmSelections[i]==1){
				vsmFinal |= CVTypeHelper.VsmType.VEHSTAT.intValue();
			}
			if(vsmSelections[i]==2){
				vsmFinal |= CVTypeHelper.VsmType.WEATHER.intValue();
			}
			if(vsmSelections[i]==3){
				vsmFinal |= CVTypeHelper.VsmType.ENV.intValue();
			}
			if(vsmSelections[i]==4){
				vsmFinal |= CVTypeHelper.VsmType.ELVEH.intValue();
			}
		}
		return vsmFinal;
	}

	public static void main(String[] args){
		UnitTestHelper.initLog4j(Level.WARN);
		final String configFile = args.length > 0 ? args[0] : null;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				SubscriptionForm subS = new SubscriptionForm(configFile);
				subS.setLocationRelativeTo(null);
				subS.loadLastSaved();
				subS.setVisible(true);
		}
	});
}
}




