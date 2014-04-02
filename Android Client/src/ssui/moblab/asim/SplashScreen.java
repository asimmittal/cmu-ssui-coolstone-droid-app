package ssui.moblab.asim;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;

import android.os.*;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.*;

public class SplashScreen extends Activity implements UDPMessageReceivedEvent,AnimationListener{

	final String moduleTag = this.getClass().getName() + ": ";
	final String UDP_DELIM = ";";
	final int UDP_PORT = 52226;	
	
	private UDPListener datagramService;
	private TCPClient client;
	private Handler udpHandler;
	private String ipAddress = "";
	private int portNumber= -1;
	private String hostName = "";
	
	private LinearLayout statusContainer;
	private TextView statusBox;
	private String[] statusMsg = { "Waiting for sync with PC...", "Connecting to " };
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	this.setContentView(R.layout.main);
    	
    	statusContainer = (LinearLayout)findViewById(R.id.container);
    	statusBox = (TextView)findViewById(R.id.statusText);
    	statusBox.setText(statusMsg[0]);
    	
    	//start a UDP listener that will grab any incoming UDP messages from 
    	//the broadcaster on the PC
    	try{
			datagramService = new UDPListener(UDP_PORT,this);
			datagramService.startUDPListener();
			udpHandler = new Handler();
	    } 
    	catch (SocketException e) {
        	Log.d(ProjectConstants.debugTag,moduleTag+"error initializing datagram service");
		}
    	
    	//Display a progress dialog until the IP and Port of the TCP Server are known
    	//pDlg = ProgressDialog.show(this, "Syncing with PC", "Waiting for Sync...", true);
        
    	
    }

    /******************************************************************************************
     * filterUDPMessage
     * 
     * @this routine will filter any incoming messages on UDP. Messages that are being broadcasted
     * by the host will be in a specific format <ip address>;<port>;<nickname>;<random>
     * 
     * this routine extracts the relevant fields and compares this information against the 
     * address and port number saved with us. If the info is different, we'll save this new info
     * or else, we'll just forget about it (return false)
     *      
     * @param message - the string message received over UDP
     * @return - returns true if a new and unique message was sent, else false is returned
     * ******************************************************************************************/
    private boolean filterUDPMessage(String message){
    	
    	//temporary variables that will be used to store the meta data for current message
    	String ip = "";
    	String nickname = "";
    	int port = -1;
    	
    	//we'll assume that the message has nothing new and unique
    	boolean toReturn = false;
    	
    	//look for the first delimiter - marks the end of the IP Address
    	int endIP = message.indexOf(UDP_DELIM);
    	if(endIP > -1){
    		
    		//if the delimiter was found, extract the IP address
    		ip = message.substring(0, endIP);
    		
    		//the port number starts after the ip address and stops at the next delimiter
    		int endPort = message.indexOf(UDP_DELIM, endIP+1);
    		if(endPort > -1){
    			
    			//the port number exists, so lets extract it as well
    			port = Integer.parseInt(message.substring(endIP+1, endPort));
    			
    			//similarly, extract the hostname
    			int endNickname = message.indexOf(UDP_DELIM,endPort+1);
    			if(endNickname > -1){
    				nickname = message.substring(endPort+1,endNickname);
    				toReturn = true;
    			}
    		}    		  
    	}
    	
    	//toReturn is true --> all the meta data was indeed found, let's check its its unique
    	if(toReturn == true){
    		
    		//check if the ip address received in this packet is the same as the one saved.
    		//do the same for hostname
    		int sameAddr = ipAddress.compareTo(ip);
    		int sameName = hostName.compareTo(nickname);
    		
    		//if the ip, host name and port are the same, return false
    		if(sameAddr == 0 && sameName == 0 && port == portNumber)
    			return false;
    		
    		//they're not the same, then let's save this new set of values
    		ipAddress = ip; 
    		portNumber = port; 
    		hostName = nickname;
    	}
    	
    	//return the result of the parsing
    	return toReturn;    	
    }
    
    /******************************************************************************************
     * Handles incoming UDP packets. The UDP listener calls this event handler whenever
     * a new packet arrives.
     * 
     * @param message - string representing the UDP message that just arrived
     *****************************************************************************************/
	@Override
	public void messageReceived(final String message) {		
		
		//Since this event is invoked by a thread (UDP Listener), we need to use a handler
		//to make any updates to the UI. So I'll create a new Runnable and post the message
		//to that runnable using this handler
		udpHandler.post(new Runnable(){
			@Override
			
			//this is where the handling will take place
			public void run() {
				//try to filter the UDP message. Filteration usually involves extracting
				//the important pieces of information from the message and storing it in 
				//class variables. The status will indicate if any significant info was present
				boolean status = filterUDPMessage(message);
				
				//if the information in the packet was significant, then do this.
				if(status)
				{
					try
					{
						//try to establish a connection to the TCP server on the host machine
						client = new TCPClient(ipAddress,portNumber);
						statusBox.setText(statusMsg[1] + hostName);
						animHideLayout();
						client.closeConn();
						datagramService.stopUDPListener();
					}
					catch(Exception clientConnEx)
					{
						
					}
				}
			}
		});
	}
	
	/***************************************************************************
	 * Event handler for pressing the back button on the phone 
	 ***************************************************************************/
	@Override
	 public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if(keyCode == KeyEvent.KEYCODE_BACK){
			
			//if the back button is pressed during the course of this activity, close the 
			//datagram listener, the tcp connection if one exists
			try
			{
				Log.d(ProjectConstants.debugTag, moduleTag + "back button pressed");
				datagramService.stopUDPListener();
				client.closeConn();
			}
			catch(Exception e){}
			
			//end the application
			this.finish();
		}
		
				
		return super.onKeyDown(keyCode, event);
	}
	
	/***************************************************************************
	 * this routine is used to create the animation for the progress bar and
	 * status label to smoothly slide out of view. The animation also has a 
	 * listener attached to it. The listener will trigger the next activity 
	 * after the animation ends 
	 ***************************************************************************/
	private void animHideLayout(){		
		TranslateAnimation anim = new TranslateAnimation(0,0,0,statusContainer.getHeight()+10);
		anim.setAnimationListener(this);
		anim.setFillAfter(true);
		anim.setStartOffset(2300);
		anim.setDuration(1000);
		statusContainer.startAnimation(anim);		
	}
	
	@Override
	public void onAnimationEnd(Animation animation) {

		Intent activityChangeIntent = new Intent(this, InteractionActivity.class);
		activityChangeIntent.putExtra(ProjectConstants.keyIP, ipAddress);
		activityChangeIntent.putExtra(ProjectConstants.keyPort,portNumber);
		this.startActivity(activityChangeIntent);		
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		
	}

	@Override
	public void onAnimationStart(Animation animation) {
	
	}


}