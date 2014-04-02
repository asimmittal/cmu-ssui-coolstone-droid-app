package ssui.moblab.asim;

import android.os.SystemClock;

public class Stopwatch {

	private long ticks;
	
	public Stopwatch(){
		ticks = 0;
	}
	
	public void start(){
		ticks = SystemClock.elapsedRealtime();
	}
	
	public long getTicks(){
		ticks = SystemClock.elapsedRealtime() - ticks;
		return ticks;
	}
	
	public void reset(){
		ticks = 0;
	}
}
