package ssui.moblab.asim;

import java.text.DecimalFormat;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.hardware.*;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

/*********************************************************************************
 * class name : InteractionActivity
 * 
 * This is the second activity which actually allows touch based interaction. The 
 * interactions that are possible:
 * 	- Single finger swipe (left / right)
 *  - Two finger pinch/zoom
 *  - Three finger grab
 *  - Four finger grab
 *  - Face down using tilt sensor
 *********************************************************************************/
public class InteractionActivity extends Activity implements TouchGesturesEvent,SensorListener{

	final String moduleName = "InteractionActivity";
	
	//The UI elements
	FrameLayout container;					//this is the outer layout
	ColoredDot markers;						//this view will draw the touch points and other stuff
	
	//The network related stuff				
	TCPClient client = null;				//this client object will connect to the TCP server on the host
	String ipAddr = "";						//this will store the IP address of the server
	int portNum = -1;						//this will store the port number of the server
	
	//The motion sensing stuff
	SensorManager sensorMgr;				//sensor manager to grab the motion sensor
	boolean isFaceDown = false;				//a flag that tells me if the phone is lying face down
	
	/****************************************************************************
	 * onCreate routine for this activity - whenever this activity is created,
	 * the following things must happen:
	 * 	- it must create the UI
	 *  - it must connect to the host server
	 *  - it must start looking for orientation data from the motion sensor
	 *  
	 ****************************************************************************/
	public void onCreate(Bundle savedInstance){
		super.onCreate(savedInstance);

		//create the UI - the coloredDot class is a view that will handle all touch events
		//it will also detect various touch gestures - pinch, zoom, swipe etc.
		container = new FrameLayout(this);														
		Drawable bgImg = getResources().getDrawable(R.drawable.fractalbg);
		markers = new ColoredDot(this,bgImg,this);
		container.addView(markers);
		this.setContentView(container);
				
		//if the address info is blank, try to extract the address info from the intent
		//sent from the previous activity (Splash Screen). The splash screen keeps listening
		//for UDP broadcasts from the host server. These broadcasts contain the address info
		if(ipAddr.length() == 0 | portNum == -1){	
			
			Intent intent = this.getIntent();					//grab the intent that was sent
			Bundle parcel = intent.getExtras();					//find a parcel inside that intent
			ipAddr = parcel.getString(ProjectConstants.keyIP);	//extract the IP from the parcel
			portNum = parcel.getInt(ProjectConstants.keyPort);	//extract the port number
			boolean connStatus = connectToHost();				//connect to the host
			
			//if connection was successful, which in most cases it usually is, send a dummy message
			if(connStatus) sendCommand("Interaction Activity is Running!");
			
			//initialize the sensor manager, and check if the accelerometer is any good and tie a listener to it
			sensorMgr = (SensorManager) this.getSystemService(SENSOR_SERVICE);
	        boolean isAccelSupported = sensorMgr.registerListener(this,SensorManager.SENSOR_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME);
	        
	        //if accelerometer is not supported, make sure the registered listener is unregistered
	        if(!isAccelSupported){
	        	sensorMgr.unregisterListener(this,SensorManager.SENSOR_ACCELEROMETER);
	        }
		}
		
		Toast.makeText(this, ProjectConstants.msgHelpInteraction, Toast.LENGTH_SHORT).show();
	}
	
	/****************************************************************************
	 * this routine connects to the host over TCP
	 ****************************************************************************/
	public boolean connectToHost(){
		
		//try to create a new client which connects to the host server at 
		//the specified address
		try{
			client = new TCPClient(ipAddr,portNum);
		}
		//if some problem occurred, log this error... honestly, I don't know what a good
		//user output would be to handle this error
		catch(Exception e){
			Log.d(ProjectConstants.debugTag,"Error connecting to server!");
			return false;
		}
		
		//if everything went well, return true
		return true;
	}
	
	/****************************************************************************
	 * this routine will send the command to the host using the TCP Client
	****************************************************************************/
	public boolean sendCommand(String command){
		
		//use the client to try and send a command to the host server
		try{						
			
			//the <data></data> tags are important, as the server will read only strings
			//bounded within these tags
			client.sendToClient("<data>"+command+"</data>");
		} 
		catch(Exception e){
			
			//if it was unable to send, try a reconnection and try to send again
			boolean status = connectToHost();
			if(status) sendCommand(command);
			return status;
		}
		
		return true;
	}
	
	/****************************************************************************
	 * if the activity gets paused for some reason
	 ****************************************************************************/	
	@Override
	public void onPause(){
		super.onPause();
		
		//the acitivity is about to be paused, close the connection if one exists
		try{
			client.closeConn();
		}catch(Exception e){}
	}
	
	/****************************************************************************
	 * Event handlers for gesture events
	****************************************************************************/
	@Override
	public void onZoom() {		
		//zoom event occurred - send the zoom command
		sendCommand(ProjectConstants.cmdZoom);
	}

	@Override
	public void onPinch() {
		//pinch event occurred - send the pinch command
		sendCommand(ProjectConstants.cmdPinch);
	}

	@Override
	public void onSwipeLeft() {
		//swipe left occurred - send the swipe left command
		sendCommand(ProjectConstants.cmdSwipeLeft);
	}

	@Override
	public void onSwipeRight() {
		//swipe right occurred - send the swipe right command
		sendCommand(ProjectConstants.cmdSwipeRight);
	}

	@Override
	public void onGrab(int ptrCount) {		
		
		//the grab event occurred, let's see how many touch points were used to perform the grab
		switch(ptrCount){
			case 3: sendCommand(ProjectConstants.cmdGrabThree); break;	//three touch points - send the grab3 command
			case 4: sendCommand(ProjectConstants.cmdGrabFour); break;  	//four touch points - send the grab4 command
		}
	}

	@Override
	public void onGrabRelease() {
		//the grab was released - send the grab release command
		sendCommand(ProjectConstants.cmdGrabOff);
	}

	/****************************************************************************
	 * These are the motion sensor event handlers
	 ****************************************************************************/
	@Override
	public void onAccuracyChanged(int sensor, int accuracy) {
		
	}
		
	@Override
	public void onSensorChanged(int sensor, float[] values) {
		
		//the accelerometer dumped some orientation data, let's get the x,y,z (rounded and clean)
		int x = (int)roundDecimals(values[SensorManager.DATA_X]);
		int y = (int)roundDecimals(values[SensorManager.DATA_Y]);
		int z = (int)roundDecimals(values[SensorManager.DATA_Z]);
		
		//the accelerometer reads (0,0,9.8) when facing downwards, check if that is the case
		if(x == 0 && y == 0 && z >= ProjectConstants.gravityThreshold){
			
			//yep, the phone is facing downwards (lying on the touchscreen)
			//check if the "faceDown" flag is false (this flag is basically used to reduce multiple 
			//instances of this event from triggering over and over again
			
			if(!isFaceDown){
				sendCommand(ProjectConstants.cmdFaceDown); 	//send the "facedown" command
				isFaceDown = true;							//set the flag to true, disabling repeated events
			}
		}
		//okay, the phone is not lying face down, so lets allow the face down event to occur
		else if(x == 0 && y ==0 && z <= -ProjectConstants.gravityThreshold){
			isFaceDown = false;
		}
		
		/*
		 * Note: the accelerometer is always going to keep pumping data, whenever the phone is lying face down
		 * the flag will allow only one command to be sent for this orientation of the phone (as opposed to
		 * bombarding the server with 'facedown' commands.
		 */
		
	}
	
	/****************************************************************************
	 * routine that rounds off the decimal to .XX place
	 * @param d - original decimal value
	 * @return	- rounded off decimal value
	 ****************************************************************************/
	private double roundDecimals(double d) {
    	DecimalFormat twoDForm = new DecimalFormat("#.##");
    	return Double.valueOf(twoDForm.format(d));
    }
	
	
}
