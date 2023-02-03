package UDP;
import org.json.*;

public class JSONUtilities {
	//https://github.com/hmrob/ser321examples/blob/master/Sockets/AdvancedCustomProtocol/src/main/java/fauxSolution/tcp/JsonUtils.java#L7
	public static JSONObject dataFromByteArray(byte[] biteys) {
		String hotPotato = new String(biteys);
		JSONObject j = new JSONObject(hotPotato);
		return j;
	}
	
	//https://github.com/hmrob/ser321examples/blob/master/Sockets/AdvancedCustomProtocol/src/main/java/fauxSolution/tcp/JsonUtils.java#L7
	public static byte[] dataToByteArray(JSONObject hotPotato) {
		return hotPotato.toString().getBytes();
	}
}