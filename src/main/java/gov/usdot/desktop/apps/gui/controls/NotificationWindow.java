/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.usdot.desktop.apps.gui.controls;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

public class NotificationWindow  extends JFrame {
    
	private static final long serialVersionUID = 1L;

	protected NotificationWindow( Component parent, String message ) {
        super("NotificationWindow");
        setLayout(new GridBagLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel label = new JLabel(message);
        label.setBorder(BorderFactory.createLineBorder(Color.black));
        add(label);
        
        ColorUIResource toolTipColor = (ColorUIResource) UIManager.get("ToolTip.background");
        Color backgroundColor = new Color(
                toolTipColor.getRed(),
                toolTipColor.getGreen(),
                toolTipColor.getBlue(),
                toolTipColor.getAlpha());
        getContentPane().setBackground(backgroundColor);
        
        setUndecorated(true);
        pack();
        setLocationRelativeTo(parent);
    }
    
    private static void closeAfter(NotificationWindow nw, final long timeout) {
        Timer timer = new Timer(true);
        class CloseWindowTimerTask extends TimerTask {

            NotificationWindow nw;

            CloseWindowTimerTask(NotificationWindow nw) {
                super();
                this.nw = nw;
            }

            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        closeWindow();
                    }
                });
            }
            
            private void closeWindow() {
                nw.setVisible(false);
                nw.dispose();
                nw = null;
                this.cancel();
            }
        }
        TimerTask task = new CloseWindowTimerTask(nw);
        timer.schedule(task, timeout);     
    }
    
    public static void showNotificationWindow( final Component parent, final String message, final long timeout ) {
        final NotificationWindow nw = new NotificationWindow(parent,message);
        nw.setVisible(true);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                closeAfter(nw, timeout);
            }
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ex) {
                }
                NotificationWindow.showNotificationWindow(
                    null, 
                    "<html><h3><font color=\"blue\">&nbsp;&nbsp;Successfully established trust with LCSDW!&nbsp;&nbsp;</font></h3></html>", 
                    4000
                );
            }
        });
    }
}
