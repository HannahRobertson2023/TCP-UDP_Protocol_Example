package UDP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

//Based on
//https://github.com/hmrob/ser321examples/blob/master/Sockets/AdvancedCustomProtocol/build.gradle
	
public class NetworkUtilities {
  // https://mkyong.com/java/java-convert-byte-to-int-and-vice-versa/
  public static byte[] toBytes(final int data) {
    return new byte[] { (byte) ((data >> 24) & 0xff), (byte) ((data >> 16) & 0xff), (byte) ((data >> 8) & 0xff),
        (byte) ((data >> 0) & 0xff), };
  }

// https://mkyong.com/java/java-convert-byte-to-int-and-vice-versa/
  public static int bytesToInt(byte[] bytes) {
    return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | ((bytes[3] & 0xFF) << 0);
  }

  /* packet (1024 max)
   * [ 
   *   totalPackets(4-byte int), 
   *   currentPacket#(4-byte int), 
   *   pl(4-byte int),
   *   payload(byte[])
   * ]
   */
  public static void Send(DatagramSocket sock, InetAddress addr, int port, byte... bytes) throws IOException {
    int maxBufferLength = 1024 - 12;
    int packetsTotal = bytes.length / maxBufferLength + 1;
    
    int offset = 0;
    int packetNum = 0;
    while (offset < bytes.length) {
      int bytesLeftToSend = bytes.length - offset;
      int length = Math.min(maxBufferLength, bytesLeftToSend);
      
      byte[] tb = NetworkUtilities.toBytes(packetsTotal);
      byte[] cp = NetworkUtilities.toBytes(packetNum);
      byte[] lb = NetworkUtilities.toBytes(length);
     
      byte[] buffer = new byte[12 + length];
      System.arraycopy(tb, 0, buffer, 0, 4);
      System.arraycopy(cp, 0, buffer, 4, 4);
      System.arraycopy(lb, 0, buffer, 8, 4);
      System.arraycopy(bytes, offset, buffer, 12, length);
      
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, addr, port);
      sock.send(packet);
      
      packetNum++;
      offset += length;
    }
  }

  static class Packet {
    /* packet (1024 max)
     * [ 
     *   totalPackets(4-byte int), 
     *   currentPacket#(4-byte int), 
     *   pl(4-byte int),
     *   payload(byte[])
     * ]
     */
    public final DatagramPacket Packet;
    public final int Total;
    public final int Current;
    public final int Length;
    public final byte[] Payload;
    
    public Packet(DatagramPacket packet) {
      Packet = packet;
      
      byte[] tb = new byte[4];
      System.arraycopy(packet.getData(), 0, tb, 0, 4);
      Total = NetworkUtilities.bytesToInt(tb);
      
      byte[] cp = new byte[4];
      System.arraycopy(packet.getData(), 4, cp, 0, 4);
      Current = NetworkUtilities.bytesToInt(cp);
      
      byte[] lb = new byte[4];
      System.arraycopy(packet.getData(), 8, lb, 0, 4);
      Length = NetworkUtilities.bytesToInt(lb);
      
      int pl = packet.getLength() - 12;
      Payload = new byte[pl];
      System.arraycopy(packet.getData(), 12, Payload, 0, pl);
    }
  }
  
  static class Tuple {
    public final InetAddress Address;
    public final int Port;
    public final byte[] Payload;
    
    public Tuple(InetAddress address, int port, byte[] payload) {
      Address = address;
      Port = port;
      Payload = payload;
    }
  }
  
  private static Packet Read(DatagramSocket sock, int length) throws IOException {
    byte[] buff = new byte[length];
    DatagramPacket pack = new DatagramPacket(buff, length);
    sock.receive(pack);
    return new Packet(pack);
  }
 
  // reading in all the packets and adding them to "packets"
  // collecting packets as long as the size of the packets is smaller then the total ones we are supposed to receice
  public static Tuple Receive(DatagramSocket sock) throws IOException {
    ArrayList<Packet> packetList = new ArrayList<Packet>();
    do {
      packetList.add(Read(sock, 1024));
    } while (packetList.size() > 0 && packetList.size() < packetList.get(0).Total);
    
    packetList.sort((p1, p2) -> p1.Current - p2.Current); // sorting the packages by package number
    int tbl = packetList.stream().mapToInt((p)->p.Length).sum(); // summing up how long the payload is
    byte[] buffer = new byte[tbl]; // creating big enough buffer
    int offset = 0;
    
    // interating through all packetList, adding the paylod of each package to the buffer, make sure offset is used to not overwrite things
    for(var p : packetList) {
      System.arraycopy(p.Payload, 0, buffer, offset, p.Length);
      offset += p.Length;
    }
    DatagramPacket first = packetList.get(0).Packet;
    return new Tuple(first.getAddress(), first.getPort(), buffer);
  }
}