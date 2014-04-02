package ssui.moblab.asim;

/*
 * A bunch of constants that are needed for the project. Yea i put them in a class!
 */

public class ProjectConstants {
	
	//debug out tag for the project (good for filtering in logcat)
	static final String debugTag = "toolstone";
	
	//commands that are sent to the host for interactions
	static final String cmdZoom 		= "zoom";
	static final String cmdPinch		= "pinch";
	static final String cmdSwipeLeft 	= "swipe_left";
	static final String cmdSwipeRight 	= "swipe_right";
	static final String cmdGrabThree 	= "grab_3";
	static final String cmdGrabFour		= "grab_4";
	static final String cmdGrabOff		= "grab_off";
	static final String cmdFaceDown		= "face_down";
	
	//gravity threshold for accelerometer readings
	static final int gravityThreshold 	= 7;
	
	//messages
	static final String msgHelpInteraction = "Pinch, Zoom, Swipe, Grab... Discover!";
	
	//others
	static final String keyIP = "ip";
	static final String keyPort = "port";
}
