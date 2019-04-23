

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ImagingOpException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class ImageSender {
	
	
	public static int HEADER_SIZE = 8;
	public static int MAX_PACKETS = 255;
	public static int SESSION_START = 128;
	public static int SESSION_END = 64;
	public static int DATAGRAM_MAX_SIZE = 65507 - HEADER_SIZE;
	public static int MAX_SESSION_NUMBER = 255;

	/*
	 * Max IP paketi boyutu 65535 eksi 20 byte IP i�in eksi 8 byteda UDP header i�in Datagram�m�z 65507 olur
	 */
	public static String OUTPUT_FORMAT = "jpg";

	public static int COLOUR_OUTPUT = BufferedImage.TYPE_INT_RGB;

	
	public static double SCALING = 0.5; //g�rsel boyutu k���ltme katsay�s�
	public static int SLEEP_MILLIS = 2000; //bekleme s�remiz
	public static String IP_ADDRESS =  "225.4.3.1";
	public static int PORT = 4444;
	

	/* Screen shot alan kodumuz*/
	public static BufferedImage getScreenshot() throws AWTException,
			ImagingOpException, IOException {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Dimension screenSize = toolkit.getScreenSize();
		Rectangle screenRect = new Rectangle(screenSize);

		Robot robot = new Robot();
		BufferedImage image = robot.createScreenCapture(screenRect);

		return image;
	}





	public static byte[] imageyibyteyap(BufferedImage image, String format) throws IOException { //image'i byte arraye �eviriyoruz
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); //koyulacak olan array olusturluyor
		ImageIO.write(image, format, baos);
		return baos.toByteArray();
	}


	public static BufferedImage olcek(BufferedImage source, int w, int h) {
		Image image = source
				.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING);
		BufferedImage result = new BufferedImage(w, h, COLOUR_OUTPUT);
		Graphics2D g = result.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return result;
	}

	
	public static BufferedImage kucult(BufferedImage source, double factor) {
		int w = (int) (source.getWidth() * factor);
		int h = (int) (source.getHeight() * factor);
		return olcek(source, w, h);
	}

	

	
	private boolean gonder(byte[] imageData, String multiAddress,
			int port) {
		InetAddress ia;

		boolean ret = false;
		int ttl = 2; //time to live her routerda 1 d��er javada orjinali 1

		try {
			ia = InetAddress.getByName(multiAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return ret;
		}

		MulticastSocket ms = null;

		try {
			ms = new MulticastSocket();
			ms.joinGroup(ia);
			ms.setTimeToLive(ttl);
			DatagramPacket dp = new DatagramPacket(imageData, imageData.length,
					ia, port);
			ms.send(dp);
			ret = true;
		} catch (IOException e) {
			e.printStackTrace();
			ret = false;
		} finally {
			if (ms != null) {
					ms.close();
			}
		}

		return ret;
	}

	
	public static void main(String[] args) {  //start of main
		ImageSender sender = new ImageSender(); //Obje olusturuyor
		int sessionNumber = 0; // ka��nc�
		
		
		
		// Frame
		JFrame frame = new JFrame("Verici");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel label = new JLabel();
		frame.getContentPane().add(label);
		frame.setVisible(true);


		
			label.setText("Ekran Goruntuleri aliniyor");
		
		
		frame.pack();
		
		try {
			/* devaml� g�nderecez */
			while (true) {
				BufferedImage image;

				/* image varsa i�inden rastgele yoksa screenshot */
				
					image = getScreenshot();	
					
				

				/* Scale image */
				image = kucult(image, SCALING); //g�r�nt�y� k���lt�yoruz
				byte[] imageByteArray = imageyibyteyap(image, OUTPUT_FORMAT); //jpg olarak byte arraye koyuyoruz.
				int packets = (int) Math.ceil(imageByteArray.length / (float)DATAGRAM_MAX_SIZE); // byte arrayimizin boyutunu hesapl�yoruz  byte arreayden ka�tane datagram ��kar?

				/* If image has more than MAX_PACKETS slices -> error */
				if(packets > MAX_PACKETS) { //paketlerin say�s� 255den b�y�kse
					System.out.println("Image is too large");
					continue;
				}

			
				for(int i = 0; i <= packets; i++) {
					int flags = 0;
					flags = i == 0 ? flags | SESSION_START: flags; // E�er yeni ba�lad�ysak flag�n start�n� ilk paketteyiz flag 128
					flags = (i + 1) * DATAGRAM_MAX_SIZE > imageByteArray.length ? flags | SESSION_END : flags; //son pakete geldiysek flag+64

					int size = (flags & SESSION_END) != SESSION_END ? DATAGRAM_MAX_SIZE : imageByteArray.length - i * DATAGRAM_MAX_SIZE; //size� g�ncelliyoruz

					/* Header�m�z�n i�i */
					byte[] data = new byte[HEADER_SIZE + size]; //8 bitlik header + size�m�z datagram paketimizi olu�turuyor.
					data[0] = (byte)flags; // Session ba�� veya sonu olan flaglerin bilgisni i�eriyor
					data[1] = (byte)sessionNumber; // Ka��nc� paket sessionu oldu�u bilgisini i�eriyor
					data[2] = (byte)packets; // Toplam paket bilgisini i�eriyor
					data[3] = (byte)(DATAGRAM_MAX_SIZE >> 8); //datagram sizez� tam ��km�yor
					data[4] = (byte)DATAGRAM_MAX_SIZE; // 1 Datagram�n toplam sizez�
					data[5] = (byte)i; //counter� tutyoruz
					data[6] = (byte)(size >> 8); // size tam ��km�yor
					data[7] = (byte)size;

					/* G�r�nt�y� kopyal�yourz */
					System.arraycopy(imageByteArray, i * DATAGRAM_MAX_SIZE, data, HEADER_SIZE, size); //image bilgsinin i�aretlenen b�l�m�n� data arryimizin i�ine koyuyoruz
					/* Paketimizi g�nderiyoruz */
					sender.gonder(data, IP_ADDRESS, PORT);

					/* Leave loop if last slice has been sent */
					if((flags & SESSION_END) == SESSION_END) break; //sona geldiyse paket g�nderilmesi tamamlanm��t�r.
				}
				/* Sleep */
				Thread.sleep(SLEEP_MILLIS);
				
				/* sesion number� art�r art�r */
				sessionNumber = sessionNumber < MAX_SESSION_NUMBER ? ++sessionNumber : 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	} //end of main

}



