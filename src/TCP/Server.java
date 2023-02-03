package TCP;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.swing.Timer;

import org.json.JSONObject;

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
	
	//TODO: track down persisten JSON errors
	
	static int port = 8080;
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
			file = new File("img/win.png");

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

	public static void main(String args[]) {
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
		// sets up the infinite loop and resets global
		while (true) {
			try {
				ServerSocket serverSock = null;
				gameOn = true;
				i = 0;

				// The hook is baited, let's try for fish
				serverSock = new ServerSocket(port);

				// Player took the bait
				sock = serverSock.accept();

				// input and output streams
				OutputStream toClient = sock.getOutputStream();
				InputStream fromClient = sock.getInputStream();

				// gets player name and sets it to the global variable
				JSONObject gmObj = makeQuestion("What is your name?");
				NetworkUtilities.Send(toClient, gmObj);
				gmObj = NetworkUtilities.Receive(fromClient);
				name = gmObj.getString("answer");

				// gets question amt and sets it to global
				gmObj = makeQuestion("How many questions do you think you can answer, " + name + "?");
				NetworkUtilities.Send(toClient, gmObj);
				gmObj = NetworkUtilities.Receive(fromClient);
				String t = gmObj.getString("answer");
				try {
					timer = Integer.parseInt(t);
					gmObj = makeQuestion("Okay, " + name + " send anything to start your " + timer + " questions.");
					NetworkUtilities.Send(toClient, gmObj);
					gmObj = NetworkUtilities.Receive(fromClient);
				} catch (NumberFormatException e) {
					timer = 1;
					NetworkUtilities.Send(toClient, gmObj);
					gmObj = NetworkUtilities.Receive(fromClient);
				}

				// loop args: two for packet shuffling, two for right and wrong answers, one
				// iter
				int r = 0;
				int w = 0;

				ActionListener taskPerformer = new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
						gameOn = false;
					}
				};
				Timer tmr = new Timer(timer * 5000, taskPerformer);
				tmr.setRepeats(false);
				tmr.start();

				String ans; 
				
				// the game monstrosity
				while (gameOn && i < timer) {
					// increments i
					i++;
					// gets and sends the question
					gmObj = makeQuestion(Questions.getQuestion());
					
					NetworkUtilities.Send(toClient, gmObj);

					System.out.println("The answer to the question is: " + Questions.getAnswer());

					// recieves and tests the answer
					gmObj = NetworkUtilities.Receive(fromClient);
					System.out.println(gmObj);
					ans = gmObj.getString("answer");
					
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
						gmObj = makeQuestion(
								"The answer was incorrect. Would you like to try again? To skip, type 'next'.");
						
						NetworkUtilities.Send(toClient, gmObj);

						gmObj = NetworkUtilities.Receive(fromClient);
						ans = gmObj.getString("answer");

						// skip clause
						if (ans.equalsIgnoreCase("next")) {
							ans = Questions.getAnswer();
							w++;
						}
					}
				}
				if (gameComplete && (w / i) < .25) {
					// test
					gmObj = makeDone("You won! You got " + r + " out of " + (r + w) + " correct!");
					
					NetworkUtilities.Send(toClient, gmObj);

					// image sending
					gmObj = makeImage(1);
					NetworkUtilities.Send(toClient, gmObj
							);
				} else if (gameComplete && (w / i) > .25) {
					// test
					gmObj = makeDone("You beat the timer, but you only got " + r + " out of " + timer
							+ " correct. Better luck next time!");
					
					NetworkUtilities.Send(toClient, gmObj);

					// image sending
					gmObj = makeImage(0);
					
					NetworkUtilities.Send(toClient, gmObj);
				} else {
					// test
					gmObj = makeDone("Time's up, better luck next time!");
					
					NetworkUtilities.Send(toClient, gmObj);

					// image sending
					gmObj = makeImage(0);
					NetworkUtilities.Send(toClient, gmObj);
				}
				tmr.stop();
				sock.close();
				gameComplete = false;
				serverSock.close();
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