package awt;

import java.awt.AWTEvent;

/**
 * The older system was just a miserable pile of fuck. This system clearly codifies everything we need to be aware of,
 * namely keypresses, mousebuttonpresses, window state changes, etc.
 *
 * Based on Jake2's JakeInputEvent.
 *
 * @author velktron
 *
 */
public class MochaDoomInputEvent {

    static final int KEY_PRESS = 0;
    static final int KEY_RELEASE = 1;
    static final int KEY_TYPE = 2;
    static final int MOTION_NOTIFY = 3;
    static final int DRAG_NOTIFY = 4;
    static final int BUTTON_PRESS = 5;
    static final int BUTTON_RELEASE = 6;
    static final int CREATE_NOTIFY = 7;
    static final int CONFIGURE_NOTIFY = 8;
    static final int WHEEL_MOVED = 9;
    static final int MOUSE_EXITED = 10;
    static final int MOUSE_ENTERED = 11;
    static final int MOUSE_CLICKED = 17;
    static final int WINDOW_MOVING = 12;
    static final int FOCUS_GAINED = 13;
    static final int FOCUS_LOST = 14;
    static final int LOCK_ON = 15;
    static final int LOCK_OFF = 16;
    static final int KEY_MASK = 0X100; // Extract info from lower bits for this

    final int type;
    final AWTEvent ev;

    MochaDoomInputEvent(int type, AWTEvent ev) {
        this.type = type;
        this.ev = ev;
    }

    /**
     * Just a friendly way to remind the child component to position and initialize itself correctly for the first use.
     */
    static MochaDoomInputEvent GET_YOUR_ASS_OFF = new MochaDoomInputEvent(CONFIGURE_NOTIFY, null);
}
