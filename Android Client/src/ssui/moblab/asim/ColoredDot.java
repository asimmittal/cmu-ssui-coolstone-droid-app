package ssui.moblab.asim;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/*******************************************************************************
 * class : ColoredDot
 * 
 * This class will create a view that draws the touch points. Any touch point is
 * accurately mapped using a hash table. Whenever a touch event occurs, the position
 * of that point is updated in the hash table.
 * 
 * Whenever a point "moves" (move event), we'll perform a few calculations based
 * on its current position (given by the event), and its earlier position (stored
 * in the hash table).
 * 
 * Based on a few calculations we'll be able to predict if the motion of the points
 * represents any of the following gestures:
 *  - 2 finger Pinch / Zoom
 *  - 1 finger Swipe (left or right)
 *  - 3 finger grab
 *  - 4 finger grab
 *  
 * Whenever any of these gestures is identified, an event is raised at the activity
 * which will then act accordingly
 * 
 * The drawing of this view includes the following features:
 *  - a nice white box enclosing it
 *  - a background image (which can be zoomed or pinched)
 *  - circles for each of the touch points
 *  - axes for each of the points (so it looks really really cool and geeky)
 *  - text showing the coordinates of each point
 *  
 *******************************************************************************/

@SuppressWarnings("deprecation")
public class ColoredDot extends View{

	// a few constants that will be needed for calculating certain gestures
	final float delta = 0.45f;
	final int moveThresh = 20;
	final int swipeThresh = 40;
	final int velocityThresh = 2;
	
	//other members
	boolean isGrabbed = false;								//this flag is used to prevent servicing of recurring grab events
	float sx=1,sy=1;										//these are scaling factors for the bitmap that is drawn as the background
	int size = 85;											//this is the radius of the circle
	Hashtable<Integer,TouchPoint> mapToDraw = null;			//this hashtable will store all the points against their unique pointer IDs
	Bitmap img;
	
	//a stopwatch - this is needed to timestamp the touch points
	Stopwatch timer;
	
	//a delegate that will raise the events
	TouchGesturesEvent gesturesEvent;
	
	/*******************************************************************************
	 * class constructor
	 *******************************************************************************/
	public ColoredDot(Context context, Drawable bgImage, TouchGesturesEvent event) {
		super(context);
		mapToDraw = new Hashtable<Integer,TouchPoint>();
		gesturesEvent = event;
		timer = new Stopwatch();
		timer.start();	
		
		if(bgImage != null){
			img = ((BitmapDrawable)bgImage).getBitmap();
		}		
	}
	
	/*******************************************************************************
	 * this routine can be used to set the hashtable for drawing
	 *******************************************************************************/
	public void setMapToDraw(Hashtable<Integer,TouchPoint> map){
		mapToDraw = map;
		this.invalidate();
	}
	
	/*******************************************************************************
	 * the routine that actually draws the touch point. called from this view's 
	 * onDraw() method
	 *******************************************************************************/
	private void drawTouchPoint(Canvas canv, TouchPoint tp){
		
		//the touch point will be drawn by using two components - a circle that marks the point
		//and secondly, two lines passing through the center of the circle spanning the total
		//width and height of the screen
		
		Paint painter = new Paint();	
				
		//this is what draws the circle
		painter.setColor(Color.GREEN);
		painter.setAlpha(250);
		painter.setStyle(Style.STROKE);
		painter.setStrokeWidth(15);		
		canv.drawCircle(tp.x, tp.y, size, painter);
	
		//this is where the lines are drawn
		painter.setColor(Color.YELLOW);
		painter.setAlpha(150);
		painter.setStrokeWidth(3);
		canv.drawLine(tp.x, 0, tp.x, this.getHeight(), painter);
		canv.drawLine(0, tp.y, this.getWidth(), tp.y, painter);
		
		//text representing coords
		painter.setAlpha(190);
		painter.setTextSize(25);
		painter.setTypeface(Typeface.MONOSPACE);
		String coord = "("+tp.x+","+tp.y+")";
		canv.drawText(coord, tp.x, tp.y - size, painter);
		
	}
	
	/*******************************************************************************
	 * this is the onDraw routine which will pull the data from the hashtable and
	 * draw them out on the view
	 *******************************************************************************/
	public void onDraw(Canvas canvas){
		
		if(mapToDraw != null){
		
			//the bounding rectangle
			RectF rect = new RectF(5,5,this.getWidth()-5,this.getHeight()-5);
						
			//draw the background image
			Paint painter = new Paint();	
			painter.setAlpha(50);
			canvas.save();
			canvas.scale(sx, sy, this.getWidth()/2, this.getHeight()/2);
			canvas.drawBitmap(img, 0, 0, painter);
			canvas.clipRect(rect);
			canvas.restore();
			
			//this is where the border gets drawn
			painter.setColor(Color.WHITE);
			painter.setStyle(Style.STROKE);
			painter.setStrokeWidth(6);
			canvas.drawRoundRect(rect, 10, 10, painter);			
			
			//the points from the map are converted into an array
			Collection<TouchPoint> points = mapToDraw.values();
			int countPoints = points.size();
			Object[] arrPoints = points.toArray();
						
			//this loop runs through the array and draws each point
			for(int i=0; i<countPoints; i++){
				
				TouchPoint tp = (TouchPoint)arrPoints[i];
				if(tp.isValid){
					drawTouchPoint(canvas,tp);
				}
			}			
			
		}
	}
	
	/******************************************************************************
	 * the onTouchEvent handler for this view. Detects multiple points of contact
	 * and draws them out as circles on the view
	 ******************************************************************************/
	public boolean onTouchEvent(MotionEvent event){
		
		int action = event.getAction();
		int count = event.getPointerCount();
		int ptrIndex = 0, ptrId;
		int actionResolved;
		
		//resolve the action as a basic type (up, down, move)
		actionResolved = action & MotionEvent.ACTION_MASK;
		if(actionResolved < 7 && actionResolved > 4) actionResolved -= 5;
		
		//if multiple pointers exist, try to get the correct pointer id for this action
		//first get the pointer index and then use that to get the pointer ID
		if(count > 1) ptrIndex = (action & MotionEvent.ACTION_POINTER_ID_MASK) >>> MotionEvent.ACTION_POINTER_ID_SHIFT;
		ptrId = event.getPointerId(ptrIndex);
		
		//now that the pointer Id and index are both known, lets check what kind of action occurred
		switch(actionResolved)
		{
		
			//if the current touch point went down on the screen, we'll simply get its coordinates and update them in the map
			case MotionEvent.ACTION_DOWN:			
				
				//get the coordinates of the point that caused the event
				float xDn = event.getX(ptrId);
				float yDn = event.getY(ptrId);
				
				//update this in the map against this pointer ID. Also timestamp the point
				TouchPoint newTp = new TouchPoint(ptrId, ptrIndex, (int)xDn, (int)yDn, true);
				newTp.time = timer.getTicks();
				mapToDraw.put(ptrId, newTp);
			
				//redraw this view to show the updated position
				this.invalidate();
				break;
			
			//if the pointer has changed its position, let's check if any gestures were performed, and update the positions
			//of all the touch points in the map
			case MotionEvent.ACTION_MOVE:
				
				//something moved, check if any gestures were performed
				checkForGestures(event);
				
				//this loop basically takes all the points present in the event and updates
				//their info in the map. Timestamp the points too.
				for(int i=0; i<event.getPointerCount(); i++){
					int id = event.getPointerId(i);
					int xCur = (int)event.getX(id);
					int yCur = (int)event.getY(id);
					TouchPoint curTp = new TouchPoint(id,i,xCur,yCur,true);
					curTp.time = timer.getTicks();
					mapToDraw.put(id, curTp);
				}
				
				//redraw the view
				this.invalidate();
				break;
			
			//if the current touch point went up off the screen, we'll simply get its coordinates and update them in the map
			case MotionEvent.ACTION_UP:
				
				//some finger went off the screen, update this info into the hashtable
				float xUp = event.getX(ptrIndex);
				float yUp = event.getY(ptrIndex);						
				TouchPoint tp = new TouchPoint(ptrId,ptrIndex,(int)xUp,(int)yUp, false);
				tp.time = timer.getTicks();
				mapToDraw.put(ptrId, tp);				
				
				//a finger went off the screen, check if a grab was the last gesture performed
				//if it was, then its time to generate the "grab released" event
				if(isGrabbed && event.getPointerCount() <= 1){ 
					isGrabbed = false;
					gesturesEvent.onGrabRelease();
				}
				
				//as always, redraw this view to show the updated positions of the points
				this.invalidate();
				break;
		}
		
		return true;		
		
		/* Note:
		 * -----
		 * Since the action_down and action_up events give us very exact info about the pointer ids of the touch point that raised
		 * these events, all we need to do is update these values in the hash table. Most of the gesture recognition is done
		 * when touch points are "moved".
		 * 
		 * for that we first check the number of points available to us from the event (basically the number of fingers touching
		 * the screen right now), and based on that number, we'll decide what algorithm to apply. The algos are pretty basic
		 * and usually compare the displacement of each point from its preceding position. Some basic velocity or distance calc. is
		 * done and thresholded against the constant empirical values (scroll up).
		 */
	}
	
	/******************************************************************************	
	 * This routine will check for various gestures - pinch, zoom, swipe (left/right)
	 * @param 
	 ******************************************************************************/
	private void checkForGestures(MotionEvent event){
		
		int ptrCount = event.getPointerCount();
		
		//GESTURE 1 : Swipe using a single finger
		if(ptrCount == 1){
			
			
			int ptrId = event.getPointerId(0);															//get the pointer id
			int x = (int)event.getX(ptrId);																//for this id, get the x coord
			int y = (int)event.getY(ptrId);																//and the y coord
			TouchPoint newPoint = new TouchPoint(ptrId,0,x,y,true);										//this is the current touch point
			newPoint.time = timer.getTicks();															//timestamp this touch point
			TouchPoint oldPoint = mapToDraw.get(ptrId);													//get the earlier value for this point from the map
			
			double distance  = getDistanceBetweenPoints(oldPoint,newPoint);								//see how much further the current point is from its older position
			double timeTaken = newPoint.time - oldPoint.time;											//see how much time it took to travel this distance
			double velocity  = (distance/timeTaken) * 1000 * 1000;										//calculate the velocity. Usually the value would be 1/1000000. So multiply accordingly
			boolean isDirectionLeft = (newPoint.x > oldPoint.x)?false:true;								//current point is on which side of the older position -> gets the direction of swipe
						
			if(distance > swipeThresh && Math.abs(velocity) >= velocityThresh){							//filter these using empirical thresholds
				if(isDirectionLeft)	gesturesEvent.onSwipeLeft();										//check the direction, and raise the appropriate event at the app
				else gesturesEvent.onSwipeRight();
			}
		}		
		
		//GESTURE 2 : Pinch / Zoom using two fingers
		else if(ptrCount == 2){
			
			TouchPoint[] oldPoints = new TouchPoint[2];													//so for each finger, there will be older positions
			TouchPoint[] newPoints = new TouchPoint[2];													//and for each finger, there will also be current positions
			
			for(int i=0; i<ptrCount; i++){																//this loop will check all the touch pointers
				
				int ptrId = event.getPointerId(i);														//get the pointer ID
				int x = (int)event.getX(ptrId);															//get the x coord
				int y = (int)event.getY(ptrId);															//get the y coord
				
				oldPoints[i] = mapToDraw.get(ptrId);													//get the earlier position of this pointer from the map
				newPoints[i] = new TouchPoint(ptrId,i,x,y,true);										//store the current position 
			}
			
			double oldLength = getDistanceBetweenPoints(oldPoints[0],oldPoints[1]);						//check the line length between the earlier positions of the two touch points
			double curLength = getDistanceBetweenPoints(newPoints[0],newPoints[1]);						//check the line length between the current positions of the two points	
			double diff = curLength - oldLength;														//get the difference between the current and older lengths
			
			if(diff > moveThresh) {																		//if this line length is greater than a certain threshold
				sx += delta; sy += delta;																//the zoom in gesture was performed
				gesturesEvent.onZoom();				
			}
			else if(diff < -moveThresh) {																//else the pinch gesture was performed
				sx = (float) ((sx - delta < 1)?1:(sx-delta));
				sy = (float) ((sy - delta < 1)?1:(sy-delta));
				gesturesEvent.onPinch();
			}
			
		}
		//GESTURE 3 : it must be a grab with 3 or more fingers, just raise the damn event
		else {			
			
			//this flag is used to prevent multiple recurrences of this event from triggering  
			//the "grab" event at the app
			if(!isGrabbed){
				isGrabbed = true;
				gesturesEvent.onGrab(ptrCount);
			}
		}
				
	}
	
	
	/****************************************************************************
	 * Returns the distance between two TouchPoints
	 * @param a = first touch point 
	 * @param b = second touch point
	 * @return the distance between a and b
	 ****************************************************************************/
	private double getDistanceBetweenPoints(TouchPoint a, TouchPoint b){
		return Math.sqrt(Math.pow((b.y - a.y),2) + Math.pow((b.x - a.x),2));
	}
	
}
