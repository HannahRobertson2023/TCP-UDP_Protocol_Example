package UDP;

import java.net.*;
import java.util.Base64;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.*;

class Client {

	static Socket sock;
	static DataInputStream clientFromServer;
	static DataOutputStream clientToServer;
	static boolean wait = true;
	static int port = 9000;
	static String host = "localhost";

	/**
	 * The class for all of the GUI stuff. Entire buckets of fun.
	 */

	public static void main(String args[]) {
		Scanner scan = new Scanner(System.in);
		DatagramSocket sock;
		try {
			// parses input into port and host
			if (args.length > 0) {
				String[] input = args[0].split("=");
				port = Integer.parseInt(input[1]);
				if (args.length > 1) {
					input = args[1].split("=");
					host = input[1];
				}
			}
		} catch (NumberFormatException e) {
			System.out.println("Sorry, not a valid socket number.");
			System.exit(0);
		}

		while (true) {
			try {
			      InetAddress address = InetAddress.getByName(host);
			      sock = new DatagramSocket();

			    JSONObject serverObj = makeResponse("hello");
				NetworkUtilities.Send(sock, address, port, JSONUtilities.dataToByteArray(serverObj));

				System.out.println("Please be sure to type all answers without spaces before or after the words.\n"
						+ "Type only one space between two-word answers. Answers are not case sensitive.\n");
				
				NetworkUtilities.Tuple responseTuple = NetworkUtilities.Receive(sock);
				serverObj = JSONUtilities.dataFromByteArray(responseTuple.Payload);
				System.out.println(serverObj.getString("question"));
				
				String a = scan.nextLine();
				serverObj = makeResponse(a);
				NetworkUtilities.Send(sock, address, port, JSONUtilities.dataToByteArray(serverObj));

				// Receives next
				responseTuple = NetworkUtilities.Receive(sock);
				serverObj = JSONUtilities.dataFromByteArray(responseTuple.Payload);

				// Receives and sends all other questions
				while (serverObj.has("question")) {
					System.out.println(serverObj.getString("question"));
					
					a = scan.nextLine();
					serverObj = makeResponse(a);
					NetworkUtilities.Send(sock, address, port, JSONUtilities.dataToByteArray(serverObj));

					responseTuple = NetworkUtilities.Receive(sock);
					serverObj = JSONUtilities.dataFromByteArray(responseTuple.Payload);
				}
				
				//errorcatch
				if (serverObj.has("error")) {
					System.out.println(
							"An error has occurred, message: \n" + serverObj.getString("error") + "nShutting down.");
					System.exit(0);
				}

				// end of program stuff- last message and image
				// partially cribbed from
				// https://github.com/amehlhase316/ser321examples/blob/master/Sockets/AdvancedCustomProtocol/src/main/java/fauxSolution/tcp/Client.java#L113
				else if (serverObj.has("done")) {
					System.out.println(serverObj.getString("done"));

					responseTuple = NetworkUtilities.Receive(sock);
					serverObj = JSONUtilities.dataFromByteArray(responseTuple.Payload);
	
					Base64.Decoder decoder = Base64.getDecoder();
					String temp = serverObj.getString("image");
					
					byte[] bytes = decoder.decode(temp);
					ImageIcon icon = null;
					try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
						BufferedImage image = ImageIO.read(bais);
						icon = new ImageIcon(image);
					}

					if (icon != null) {
						JFrame frame = new JFrame();
						JLabel label = new JLabel();
						label.setIcon(icon);
						frame.add(label); // I know getIconWidth is supposed to
						// set the size of hte image, but it doesnt do the trick
						frame.setSize(icon.getIconWidth() + 30, icon.getIconHeight() + 30);
						frame.setVisible(true);
					}

				}
				sock.close();
				//error catching.
			} catch (ConnectException e) {
				System.out.println("No Server Found. Start up server and try again.");
				System.exit(0);
			} catch (SocketException e) {
				System.out.println("Server unexpectedly shut down. Exiting program.");
				System.exit(0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// the method to make a reply to the server
	public static JSONObject makeResponse(String s) {
		JSONObject json = new JSONObject();
		json.put("answer", s);
		return json;
	}
}