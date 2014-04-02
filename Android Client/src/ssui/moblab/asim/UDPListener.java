package ssui.moblab.asim;


import java.net.*;
import android.util.Log;


/***********************************************************************************
 * UDPListener
 * -----------
 * This class allows you to create a simple UDP Listener that will pick up any 
 * messages coming in on the specified port over UDP. 
 * 
 * In the application, the server runs on a PC which broadcasts meta data like
 * IP,PORT (for TCP server), device name, device type over the LAN. An object of
 * this class will keep listening to these broadcasts and be able to track changes 
 * in the server
 * 
 * Its needed to create "automatic sync" between the PC and the mobile phone
 ***********************************************************************************/
public class UDPListener implements Runnable{
	
	//this is a tag that will be used to track the module name in the log
	final String moduleTag = this.getClass().getName() + ": ";
	
	final int MAX_SIZE = 1024;								//the maximum number of bytes to read in a UDP Packet
	final int NAP_TIME = 1000;								//the amount of time to sleep for in between packet reads
	
	private int port = 52226;								//the UDP port on which we have to listen
	private byte[] messageBuffer = new byte[MAX_SIZE];		//the buffer which will hold the incoming bytes
	private DatagramPacket packet;							//the data gram packet object	
	private DatagramSocket socket;							//the data gram socket object
	private Thread thrdListener;							//a thread that does the listening							
	private boolean isRunning = false;						//a flag to check the life cycle of the thread
	private String message = "";							//a string that will be used to extract the data from the packet
	
	UDPMessageReceivedEvent msgRecvdEvent;					//a callback to raise an event at the app when packet is received
	
	
	/***********************************************************************************
	 * Class constructor
	 ***********************************************************************************/
	public UDPListener(int portNum,UDPMessageReceivedEvent event) throws SocketException{
		
		//save the port number
		port = portNum;
		
		//initialize the socket related members and event handler
		try{
			packet = new DatagramPacket(messageBuffer,messageBuffer.length);
			socket = new DatagramSocket(port);
			msgRecvdEvent = event;			
		}
		//if a problem occurs during this initialization, throw an exception at the app
		catch(SocketException sockEx){
			Log.d(ProjectConstants.debugTag, moduleTag +"SocketException in UDP Listener");
			Log.d(ProjectConstants.debugTag, moduleTag + sockEx.getMessage());
			throw sockEx;
		}
	}
	
	/***********************************************************************************
	 * starts the UDP listener thread
	 ***********************************************************************************/
	public void startUDPListener(){
		
		thrdListener = new Thread(this);
		isRunning = true;
		thrdListener.start();
	}
	
	/***********************************************************************************
	 * stops the UDP listener thread 
	 ***********************************************************************************/
	public void stopUDPListener(){
		isRunning = false;		
	}
	
	/***********************************************************************************
	 * the actual logic for the listener thread
	 ***********************************************************************************/
	@Override
	public void run() {
		
		//log the fact that the thread is running
		Log.d(ProjectConstants.debugTag, moduleTag + "Running the listener thread...");
		
		//while the thread is running, do this
		while(isRunning){
			
			//try to read a packet from the socket, this is a blocking call btw.
			try {
				socket.receive(packet);													//blocking call to read the socket
				message = new String(messageBuffer, 0, packet.getLength());				//create a string from the bytes that were read
				msgRecvdEvent.messageReceived(message);									//raise an event and pass this string to app
				Thread.sleep(NAP_TIME);													//take a little nap
				
			}
			//if there was a problem with reading the socket, log the error
			catch (Exception ex) {
				Log.d(ProjectConstants.debugTag, moduleTag + "Exception in UDPListener thread");
				Log.d(ProjectConstants.debugTag, moduleTag + ex.getMessage());
			}
		}
		
		Log.d(ProjectConstants.debugTag, moduleTag + "UDP Listener is dying...");
		socket.close();				
	}
	

}
