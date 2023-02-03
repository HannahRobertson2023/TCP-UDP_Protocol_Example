package TCP;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.json.*;

//based on
//https://github.com/hmrob/ser321examples/blob/master/Sockets/AdvancedCustomProtocol/src/main/java/fauxSolution/tcp/NetworkUtils.java

public class NetworkUtilities {
	// https://mkyong.com/java/java-convert-byte-to-int-and-vice-versa/
	public static byte[] intToBytes(final int data) {
		return new byte[] { (byte) ((data >> 24) & 0xff), (byte) ((data >> 16) & 0xff), (byte) ((data >> 8) & 0xff),
				(byte) ((data >> 0) & 0xff), };
	}

	// https://mkyong.com/java/java-convert-byte-to-int-and-vice-versa/
	public static int bytesToInt(byte[] bytes) {
		return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | ((bytes[3] & 0xFF) << 0);
	}

	//https://github.com/hmrob/ser321examples/blob/master/Sockets/AdvancedCustomProtocol/src/main/java/fauxSolution/tcp/NetworkUtils.java
	//TODO: finish
	public static void Send(OutputStream o, JSONObject obj) throws IOException {
		byte[] bytes = JSONUtilities.dataToByteArray(obj);
		o.write(intToBytes(bytes.length));
		o.write(bytes);
		o.flush();
	}

	//https://github.com/hmrob/ser321examples/blob/master/Sockets/AdvancedCustomProtocol/src/main/java/fauxSolution/tcp/NetworkUtils.java
	//read the bytes on the stream
	private static byte[] Read(InputStream in, int length) throws IOException {
		byte[] bytes = new byte[length];
		int bR = 0;
		try {
			bR = in.read(bytes, 0, length);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if (bR != length) {
			return null;
		}

		return bytes;
	}
	//https://github.com/hmrob/ser321examples/blob/master/Sockets/AdvancedCustomProtocol/src/main/java/fauxSolution/tcp/NetworkUtils.java
	// first 4 bytes we read give us the length of the message we are about to receive
	// next we call read again with the length of the actual bytes in the data we are interested in 
	public static JSONObject Receive(InputStream in) throws IOException {
		byte[] lB = Read(in, 4);
		if (lB == null)
			return JSONUtilities.dataFromByteArray(new byte[0]);
		int length = NetworkUtilities.bytesToInt(lB);
		System.out.println(length);
		byte[] msg = Read(in, length);
		if (msg == null)
			return JSONUtilities.dataFromByteArray(new byte[0]);
		return JSONUtilities.dataFromByteArray(msg);
	}

	public static String obString(byte[] wire) {
		JSONObject irm = JSONUtilities.dataFromByteArray(wire);
		return irm.getString("String");
	}
}