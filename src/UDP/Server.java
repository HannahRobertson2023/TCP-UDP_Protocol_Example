package UDP;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.swing.Timer;

import org.json.*;

import TCP.Questions;

//

class Server {
	/*
	 * format for JSON is fairly simple
	 * 
	 * request: { "question": <string containing question> }
	 * 
	 * answer: { "answer": <string containing answer> }
	 * 
	 * done: { "done": <win or lost> }
	 * 
	 * image: { "image": <bitarray with image> }
	 * 
	 * error answer: {"error": <error string> }
	 */
	static int port = 9000;
	static Socket sock;
	static String name;
	static int timer;
	static boolean gameOn;
	static boolean gameComplete;
	static int i;

	// makes the default message to the client
	public static JSONObject makeQuestion(String s) {
		JSONObject json = new JSONObject();
		json.put("question", s);
		return json;
	}

	// makes an error object
	public static JSONObject makeError(String s) {
		JSONObject json = new JSONObject();
		json.put("error", s);
		return json;
	}

	// this makes a special message whose tag lets the client know the game is
	// ending
	public static JSONObject makeDone(String s) {
		JSONObject json = new JSONObject();
		json.put("done", s);
		return json;
	}

	// encapsulates an image in json. entire buckets of fun.
	public static JSONObject makeImage(int i) throws IOException {
		// open file
		JSONObject json = new JSONObject();

		File file = null;
		if (i == 0)
			file = new File("img/lose.png");

		if (i == 1)
			file = new File("img/won.png");

		if (!file.exists()) {
			System.err.println("Cannot find file: " + file.getAbsolutePath());
			System.exit(-1);
		}

		// read imgae
		BufferedImage img = ImageIO.read(file);

		byte[] bytes;

		// write image to byte array
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ImageIO.write(img, "png", out);
			bytes = out.toByteArray();
		}

		// encodes image returns the JSON
		if (bytes != null) {
			Base64.Encoder encoder = Base64.getEncoder();
			String temp = encoder.encodeToString(bytes);
			json.put("image", temp);
			return json;
		}
		json.put("error", "error");
		return json;
	}

	//TODO: fix timer
	public static void main(String args[]) {
		DatagramSocket sock = null;
		// gets the port numbers from the command line
		try {
			if (args.length > 0) {
				String[] input = args[0].split("=");
				port = Integer.parseInt(input[1]);
			}
		} catch (NumberFormatException e) {
			System.out.print("Sorry, not a valid socket number.");
			System.exit(0);
		}
		//connect the sock outside the loop so it doesn't give errors on second round
				try {
					sock = new DatagramSocket(port);
				} catch (SocketException e1) {
					System.out.println("Socket Error, cannot connect.");
					e1.printStackTrace();
				}
		// sets up the infinite loop and resets global
		while (true) {
			try {
				gameOn = true;
				i = 0;

				// gets player name and sets it to the global variable
				NetworkUtilities.Tuple messageTuple = NetworkUtilities.Receive(sock);
				JSONObject serverObj = JSONUtilities.dataFromByteArray(messageTuple.Payload);

				serverObj = makeQuestion("Hello, what is your name?");
				byte[] serverBytes = JSONUtilities.dataToByteArray(serverObj);
				NetworkUtilities.Send(sock, messageTuple.Address, messageTuple.Port, serverBytes);

				messageTuple = NetworkUtilities.Receive(sock);
				serverObj = JSONUtilities.dataFromByteArray(messageTuple.Payload);
				
				name = serverObj.getString("answer");

				// gets question amt and sets it to global
				serverObj = makeQuestion("How many questions do you think you can answer, " + name + "?");
				serverBytes = JSONUtilities.dataToByteArray(serverObj);
				NetworkUtilities.Send(sock, messageTuple.Address, messageTuple.Port, serverBytes);

				messageTuple = NetworkUtilities.Receive(sock);
				serverObj = JSONUtilities.dataFromByteArray(messageTuple.Payload);

				String t = serverObj.getString("answer");
				try {
					timer = Integer.parseInt(t);
					serverObj = makeQuestion("Okay, " + name + " send anything to start your " + timer + " questions.");
					serverBytes = JSONUtilities.dataToByteArray(serverObj);
					NetworkUtilities.Send(sock, messageTuple.Address, messageTuple.Port, serverBytes);

				} catch (NumberFormatException e) {
					timer = 1;
					serverObj = makeQuestion("ERROR: No integer entered, questions set to 1. Send anything to start the game.");
					serverBytes = JSONUtilities.dataToByteArray(serverObj);
					NetworkUtilities.Send(sock, messageTuple.Address, messageTuple.Port, serverBytes);
				}

				messageTuple = NetworkUtilities.Receive(sock);
				serverObj = JSONUtilities.dataFromByteArray(messageTuple.Payload);

				// loop args: two for packet shuffling, two for right and wrong answers, one
				// iter
				int r = 0;
				int w = 0;
				String ans;

				// Timer was giving me trouble, so I had to do it weird.
				ActionListener taskPerformer = new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						gameOn = false;
					}
				};
				Timer tmr = new Timer( timer * 5000, taskPerformer);
				tmr.setRepeats(false);
				tmr.start();

				// the game monstrosity
				while (gameOn && !gameComplete) {		
					// increments i
					i++;
					
					// sends the first question
					serverObj = makeQuestion(Questions.getQuestion());
					serverBytes = JSONUtilities.dataToByteArray(serverObj);
					NetworkUtilities.Send(sock, messageTuple.Address, messageTuple.Port, serverBytes);
					
					// prints answer for graders
					System.out.println("The answer to the question is: " + Questions.getAnswer());

					// recieves the answer
					messageTuple = NetworkUtilities.Receive(sock);
					serverObj = JSONUtilities.dataFromByteArray(messageTuple.Payload);
					System.out.println(serverObj);
					ans = serverObj.getString("answer");

					// increments 'right answer' count
					if (ans.equalsIgnoreCase(Questions.getAnswer())) {
						r++;
						//also checks for game completion
						if (gameOn && i == timer)
							gameComplete = true;
					}

					while (!ans.equalsIgnoreCase(Questions.getAnswer())) {
						// increments 'wrong answer' count
						w++;
						// Restates question
						serverObj = makeQuestion(
								"The answer was incorrect. Would you like to try again? To skip, type 'next'.");
						serverBytes = JSONUtilities.dataToByteArray(serverObj);
						NetworkUtilities.Send(sock, messageTuple.Address, messageTuple.Port, serverBytes);

						System.out.println("The answer to the question is: " + Questions.getAnswer());

						messageTuple = NetworkUtilities.Receive(sock);
						serverObj = JSONUtilities.dataFromByteArray(messageTuple.Payload);

						ans = serverObj.getString("answer");

						// skip clause
						if (ans.equalsIgnoreCase("next")) {
							ans = Questions.getAnswer();
							w++;
						}
					}
				}
				if (gameComplete && (w / i) < .25) {
					// test
					serverObj = makeDone("You won! You got " + r + " out of " + (r + w) + " correct!");
					serverBytes = JSONUtilities.dataToByteArray(serverObj);
					NetworkUtilities.Send(sock, messageTuple.Address, messageTuple.Port, serverBytes);
					// image sending
					serverObj = makeImage(1);
					serverBytes = JSONUtilities.dataToByteArray(serverObj);
					NetworkUtilities.Send(sock, messageTuple.Address, messageTuple.Port, serverBytes);
				} else if (gameComplete) {
					// test
					serverObj = makeDone("You beat the timer, but you only got " + r + " out of " + timer
							+ " correct. Better luck next time!");
					serverBytes = JSONUtilities.dataToByteArray(serverObj);
					NetworkUtilities.Send(sock, messageTuple.Address, messageTuple.Port, serverBytes);

					// image sending
					serverObj = makeImage(0);
					serverBytes = JSONUtilities.dataToByteArray(serverObj);
					NetworkUtilities.Send(sock, messageTuple.Address, messageTuple.Port, serverBytes);
				} else {
					// test
					serverObj = makeDone("Time's up, better luck next time!");
					serverBytes = JSONUtilities.dataToByteArray(serverObj);
					NetworkUtilities.Send(sock, messageTuple.Address, messageTuple.Port, serverBytes);

					// image sending
					serverObj = makeImage(0);
					serverBytes = JSONUtilities.dataToByteArray(serverObj);
					NetworkUtilities.Send(sock, messageTuple.Address, messageTuple.Port, serverBytes);
				}
				tmr.stop();
			} catch (BindException e) {
				e.printStackTrace();
				System.exit(0);
			} catch (SocketException e) {
				System.out.println("Client unexpectedly shut down.");
				System.exit(0);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}