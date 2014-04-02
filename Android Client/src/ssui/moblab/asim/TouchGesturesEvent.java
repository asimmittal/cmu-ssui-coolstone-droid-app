package ssui.moblab.asim;

/*
 * Simple interface for all the touch based gestures that I'm going to use
 * This interface is implemented by the activity and used by the ColoredDot
 * view to throw events into the activity
 * 
 * Names are self explanatory
 */
public interface TouchGesturesEvent {
	
	public void onZoom();
	public void onPinch();
	public void onSwipeLeft();
	public void onSwipeRight();
	public void onGrab(int pointCount);
	public void onGrabRelease();
	
}
