package awt;

import java.awt.AWTEvent;

/** The older system was just a miserable pile of fuck.
 *  This system clearly codifies everything we need to be aware
 *  of, namely keypresses, mousebuttonpresses, window state
 *  changes, etc.
 *  
 *  Based on Jake2's JakeInputEvent.
 *   
 * @author velktron
 *
 */

public class MochaDoomInputEvent {
		static final int KeyPress = 0;
		static final int KeyRelease = 1;
		static final int KeyType = 2;
		static final int MotionNotify = 3;
		static final int DragNotify = 4;
		static final int ButtonPress = 5;
		static final int ButtonRelease = 6;
		static final int CreateNotify = 7;
		static final int ConfigureNotify = 8;
		static final int WheelMoved = 9;
		static final int MouseExited =10;
		static final int MouseEntered=11;
		static final int WindowMoving=12;
		static final int FocusGained=13;
		static final int FocusLost=14;
		static final int LockOn=15;
		static final int LockOff=16;
		static final int KEY_MASK=0X100; // Extract info from lower bits for this
		
		int type;
		int value;
		AWTEvent ev;
		
		MochaDoomInputEvent(int type, AWTEvent ev) {
			this.type = type;
			this.ev = ev;
		}
		
		MochaDoomInputEvent(int type, int value) {
			this.type=type;
			this.value=value;
			}

		/** Just a friendly way to remind the child component to
		 *  position and initialize itself correctly for the first
		 *  use.
		 */
		
		static MochaDoomInputEvent GET_YOUR_ASS_OFF = new
		MochaDoomInputEvent(ConfigureNotify, null);
	}
