package ssui.moblab.asim;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPClient {
	
	private String ipAddr;
	private int portNum;
	private Socket clientSock;
	private DataOutputStream outToServer = null;
	
	public TCPClient(String ip, int port) throws UnknownHostException, IOException{
		
		ipAddr = ip; portNum = port;
		clientSock = new Socket(ipAddr,portNum);
		outToServer = new DataOutputStream(clientSock.getOutputStream());		
	}
	
	public void sendToClient(String toSend) throws IOException{
		
		if(outToServer != null){
			outToServer.writeBytes(toSend);
		}
	}
	
	public void closeConn(){
		try {
			outToServer.close();
		} catch (IOException e) {}
	}
	
}
