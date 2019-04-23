

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JWindow;



public class ImageReceiver implements KeyListener,Runnable {
	
	public static int HEADER_SIZE = 8;

	public static int SESSION_START = 128;

	public static int SESSION_END = 64;


	private static int DATAGRAM_MAX_SIZE = 65507;

	
	public static String IP_ADDRESS = "225.4.3.1";

	public static int PORT = 4444;


	static boolean run=false;

	boolean fullscreen = false;

	static JWindow fullscreenWindow = null;
	
	static JFrame frame = new JFrame("Alici");

	private void receiveImages(String multicastAddress, int port) {
		boolean debug = true; //debug

		InetAddress ia = null;
		MulticastSocket ms = null;

		/* frame */
		JLabel labelImage = new JLabel();
		JLabel windowImage = new JLabel();

		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(labelImage);
		frame.setSize(300, 10);
		frame.setVisible(true);
		
		frame.addKeyListener(this);
		
		
		
		
		
		
		fullscreenWindow = new JWindow();
		fullscreenWindow.getContentPane().add(windowImage);
		fullscreenWindow.addKeyListener(this);

		try {
			/* Adresi al */
			ia = InetAddress.getByName(multicastAddress);

			/* soket aç gruba katýl */
			ms = new MulticastSocket(port);
			ms.joinGroup(ia);

			int currentSession = -1; //henüz gelen yok ilk gelen 0.
			int slicesStored = 0; //gelen dilimler
			int[] slicesCol = null;
			byte[] imageData = null;
			boolean sessionAvailable = false;

			/* Byte array olusturuyoruz */
			byte[] buffer = new byte[DATAGRAM_MAX_SIZE];

			//dinliyoruz...
			
			while (true) {
				/*  UDP paketini yakalýyoruz */
				DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
				ms.receive(dp);
				byte[] data = dp.getData();

				/* Headerý okuyourz */
				short session = (short) (data[1] & 0xff); //kaçýncýdayýz
				short slices = (short) (data[2] & 0xff); //parça sayýmýz paket sayýmýz
				int maxPacketSize = (int) ((data[3] & 0xff) << 8 | (data[4] & 0xff)); // burda orladým
				int data3 = (int) ((data[3] & 0xff) << 8); // mask
				int data4 = (int) (data[4] & 0xff); // mask
				
				// bit
				short slice = (short) (data[5] & 0xff);
				int size = (int) ((data[6] & 0xff) << 8 | (data[7] & 0xff)); // mask
			
				// bit

				if (debug) {
					System.out.println("------------- PACKET -------------");
					System.out.println("SESSION_START = "
							+ ((data[0] & SESSION_START) == SESSION_START));
					System.out.println("SSESSION_END = "
							+ ((data[0] & SESSION_END) == SESSION_END));
					System.out.println("SESSION = " + session);
					System.out.println("SLICES = " + slices);
					System.out.println("data[3] = " + data3);
					System.out.println("data[4] = " + data4);
					System.out.println("MAX PACKET SIZE = " + maxPacketSize);
					System.out.println("SLICE  = " + slice);
					System.out.println("SIZE = " + size);
					System.out.println("------------- PACKET -------------\n");
				}

				/* Eger SESSION_START falg ise , ilk verileri yüklüyoruz */
				if ((data[0] & SESSION_START) == SESSION_START) {
					if (session != currentSession) {
						currentSession = session;
						slicesStored = 0;
						/* Uzun boyutlu bayt dizisi olusturotum */
						imageData = new byte[slices * maxPacketSize];
						slicesCol = new int[slices];
						sessionAvailable = true;
					}
				}

				/* Eger sezonun ilk paketi degilse */
				if (sessionAvailable && session == currentSession) {
					if (slicesCol != null && slicesCol[slice] == 0) {
						slicesCol[slice] = 1;
						System.arraycopy(data, HEADER_SIZE, imageData, slice
								* maxPacketSize, size);
						slicesStored++;
					}
				}

				/* Tamamsa oynatýyoruz */
				if (slicesStored == slices) {
					ByteArrayInputStream bis = new ByteArrayInputStream(
							imageData);
					BufferedImage image = ImageIO.read(bis);
					labelImage.setIcon(new ImageIcon(image));
					windowImage.setIcon(new ImageIcon(image));

					frame.pack();
				}

				if (debug) {
					System.out.println("STORED SLICES: " + slicesStored);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (ms != null) {
				try {
					/* Leave group and close socket */
					ms.leaveGroup(ia);
					ms.close();
				} catch (IOException e) {
				}
			}
		}
	}

	
	
	public static void main(String[] args) { //main

		
		
		JFrame f = new JFrame("Control");
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setSize(300, 300);
		JPanel panel = new JPanel();
		f.add(panel);
		JButton b1 = new JButton("Baslat");
		JButton b2 = new JButton("Durdur");
		JButton b3 = new JButton("Hakkýnda");
		panel.add(b1);
		panel.add(b2);
		panel.add(b3);
		b1.addActionListener(new Action()); 
		b2.addActionListener(new Action2()); 
		b3.addActionListener(new Action3()); 
		//ImageReceiver receiver = new ImageReceiver();
		//receiver.receiveImages(IP_ADDRESS, PORT);
		
		
		
	}
	
	
	
	private static void startThread() {
	    new Thread() {
	        public void run() {
	        	
	        	
	        	ImageReceiver receiver = new ImageReceiver();
	    		receiver.receiveImages(IP_ADDRESS, PORT);
	        	
	        	
	        }
	    }.start();

	}
	
	

	
	   
	

	
	 
	 
	 
	static class Action implements ActionListener{
		
		public void actionPerformed (ActionEvent e)
		{
			frame.setVisible(true);
			startThread();
			
		}
	}
static class Action2 implements ActionListener{
		
		public void actionPerformed (ActionEvent e)
		{
			
			frame.setVisible(false);
			
		}

		
	}
static class Action3 implements ActionListener{
	
	public void actionPerformed (ActionEvent e)
	{
		
		JOptionPane.showMessageDialog(null, 
                "Bu Uygulama Barýþ Güngör Tarafýndan Geliþtirilmiþtir.", 
                "Hakkýnda", 
                JOptionPane.WARNING_MESSAGE);
		
	}

	
}
	
	public void keyPressed(KeyEvent keyevent) {
		GraphicsDevice device = GraphicsEnvironment
				.getLocalGraphicsEnvironment().getDefaultScreenDevice();

		/* Toggle full screen mode on key press */
		if (fullscreen) {
			device.setFullScreenWindow(null);
			fullscreenWindow.setVisible(false);
			fullscreen = false;
		} else {
			device.setFullScreenWindow(fullscreenWindow);
			fullscreenWindow.setVisible(true);
			fullscreen = true;
		}

	}

	public void keyReleased(KeyEvent keyevent) {
	}

	public void keyTyped(KeyEvent keyevent) {
	}



	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}



	
	

}


