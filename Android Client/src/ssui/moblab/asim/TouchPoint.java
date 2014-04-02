package ssui.moblab.asim;

/****************************************************************************
 * class : TouchPoint
 * 
 * This class defines a touch point. The touch point is represented by 
 * coordinates, pointer id and index and a validity flag
 ****************************************************************************/
public class TouchPoint {
	
	//these members are public, so you can reference them directly using an object
	
	int x,y;				//coords of the touch point
	boolean isValid;		//controls validity of this touch point
	int ptrId, ptrIndex;	//pointer id, pointer index
	long time;				//timestamp
	
	/****************************************************************************
	 * class constructor
	 ****************************************************************************/
	public TouchPoint(){
		isValid = false;
		time = 0;
	}
	
	public TouchPoint(int id,int index,int x,int y, boolean valid){
		
		this.x = x;
		this.y = y;
		this.ptrId = id;
		this.ptrIndex = index;
		isValid = valid;
		time = 0;
	}
}
