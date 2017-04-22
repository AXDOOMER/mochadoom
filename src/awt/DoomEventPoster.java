/*
 * Copyright (C) 2017 Good Sign
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package awt;

import doom.DoomMain;
import doom.event_t;
import doom.evtype_t;
import g.Signals;
import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is catching events thrown at him by someone, and sends them to underlying DOOM engine.
 * As a way to preserver vanilla until full understand the code, everything inside of underlying engine
 * is still considered black box and changes to it are minimal.
 * But I've tried to make high-level API on top level effective.
 * 
 * For example, we do not need to create some MochaDoomInputEvent for unique combination of AWTEvent and
 * its type - it can be easily switched by ID value from AWTEvent's ow information method. Also, we can
 * certainly know which ScanCodes we will get and what are their minimal and max values, because
 * of using Enum for them (my favorite type of data structure, huh!) 
 * And if we know that ScanCodes can only be something very limited, and every KeyEvent (an AWTEvent for keys)
 * will be translated into one of them, we only have to pre-create two copies of DOOM's event structure
 * for each entry of ScanCode Enum: one for press state, one for release.
 * 
 * Also note: Caps Lock key is handled in a very special way. We do not send him to underlying engine at all.
 * We only save state of it.
 * Also note: it seems there is no way to handle Num Lock itself (however, we can handle keys modified by it)
 * Also note: SysRq / Print Screen key only sends release state, so it has to send press to underlying engine
 * on release or it will be ignored.
 *  - Good Sign 2017/04/21
 * 
 * @author Good Sign
 * @author vekltron
 */
public class DoomEventPoster {
    /**
     * This event here is used as a static scratch copy. When sending out
     * messages, its contents are to be actually copied (struct-like).
     * This avoids the continuous object creation/destruction overhead,
     * And it also allows creating "sticky" status.
     * 
     * As we have now parallel processing of events, one event would eventually
     * overwrite another. So we have to keep thread-local copies of them.
     */
    private final ThreadLocal<event_t.mouseevent_t> mouse_event = new ThreadLocal<event_t.mouseevent_t>() {
        @Override
        protected event_t.mouseevent_t initialValue() {
            return new event_t.mouseevent_t(evtype_t.ev_mouse, 0, 0, 0);
        }
    };
    
    private final DoomMain DM;
    private final Component content;
    private final Point offset = new Point();
    private final Cursor hidden;
    private final Cursor normal;
    private final Robot robby;

    //////// CURRENT MOVEMENT AND INPUT STATUS //////////////
    private volatile int mousedx;
    private volatile int mousedy;
    private volatile int prevmousebuttons;
    private volatile int win_w2, win_h2;

    // Nasty hack for CAPS LOCK. Apparently, there's no RELIABLE way
    // to get the caps lock state programmatically, so we have to make 
    // do with simply toggling
    private volatile boolean capstoggle = false;
    private volatile boolean ignorebutton = false;

    public DoomEventPoster(DoomMain DM, Component content) {
        this.DM = DM;
        this.content = content;

        // AWT: create cursors.
        this.normal = content.getCursor();
        this.hidden = createInvisibleCursor();

        // Create AWT Robot for forcing mouse
        Robot robot;
        try {
            /**
             * In my opinion, its better turn off mouse at all, then without Robot.
             * But it will certainly stay there for someone's edge cases.
             * - Good Sign 2017/04/22
             */
            robot = new Robot();
        } catch (AWTException e) {
            Logger.getLogger(DoomEventPoster.class.getName()).log(Level.SEVERE, e.getMessage(), e);
            System.err.println("AWT Robot could not be created, mouse input focus will be loose!");
            robot = null;
        }
        this.robby = robot;
    }

    /**
     * NASTY hack to hide the cursor.
     * 
     * Create a 'hidden' cursor by using a transparent image
     * ...return the invisible cursor
     */
    private Cursor createInvisibleCursor() {
        Dimension bestCursorDim = Toolkit.getDefaultToolkit().getBestCursorSize(2, 2);
        BufferedImage transparentImage = new BufferedImage(bestCursorDim.width, bestCursorDim.height, BufferedImage.TYPE_INT_ARGB);
        Cursor hiddenCursor = Toolkit.getDefaultToolkit().createCustomCursor(transparentImage, new Point(1, 1), "HiddenCursor");
        return hiddenCursor;
    }
    
    private volatile boolean mouseLoose = false;
    
    void setMouseLoose() {
        mouseLoose = true;
        content.setCursor(normal);
    }
    
    void setMouseCaptured() {
        mouseLoose = false;
        forceKeyboardReaction();
        forceMouseAutoCapture();
        autoPositionMouse();
        autoCaptureMouse();
    }

	/**
     * Update relative position offset, and force mouse there.
	 */
    void reposition() {
        if (!content.isShowing() || mouseLoose) {
            return;
        }
        
        offset.x = content.getLocationOnScreen().x;
        offset.y = content.getLocationOnScreen().y;
        // Shamelessly ripped from Jake 2. Maybe it works better?
        win_w2 = content.getWidth() / 2;
        win_h2 = content.getHeight() / 2;

        if (robby != null) {
            robby.mouseMove(offset.x + win_w2, offset.y + win_h2);
        }

        content.getInputContext().selectInputMethod(java.util.Locale.US);
        content.setCursor(hidden);
    }
    
    private volatile boolean alt_enter_fullscreen = false;
    
    private void keyRelease(final Signals.ScanCode sc, final event_t.mouseevent_t mouseEvent) {
        if(sc == Signals.ScanCode.SC_ENTER && alt_enter_fullscreen) {
            alt_enter_fullscreen = false;
            DM.videoInterface.switchFullscreen();
            return;
        }
        alt_enter_fullscreen = false;
        
        if (sc == Signals.ScanCode.SC_PRTSCRN) {
            DM.PostEvent(sc.doomEventDown);
        }
        
        if ((sc != Signals.ScanCode.SC_CAPSLK) || ((sc == Signals.ScanCode.SC_CAPSLK) && capstoggle)) {
            DM.PostEvent(sc.doomEventUp);
        }
        
        capstoggle = false;
        
        if (prevmousebuttons != 0) {
            // Allow combined mouse/keyboard events.
            mouseEvent.buttons = prevmousebuttons;
            DM.PostEvent(mouseEvent);
        }
    }

    private void keyPress(final Signals.ScanCode sc, final event_t.mouseevent_t mouseEvent) {
        if (sc == Signals.ScanCode.SC_LALT) {
            alt_enter_fullscreen = true;
            System.out.println("ALT+");
        }
        
        // Toggle, but don't it go through.
        if (sc == Signals.ScanCode.SC_CAPSLK) {
            capstoggle = true;
        }
        
        if (sc != Signals.ScanCode.SC_CAPSLK) {
            DM.PostEvent(sc.doomEventDown);
        }
        
        if (prevmousebuttons != 0) {
            // Allow combined mouse/keyboard events.
            mouseEvent.buttons = prevmousebuttons;
            DM.PostEvent(mouseEvent);
        }
    }
    
    public void sendEvent(Signals.ScanCode sc, int eventType) {
        final event_t.mouseevent_t mouseEvent = mouse_event.get();
        if (eventType == KeyEvent.KEY_PRESSED) {
            keyPress(sc, mouseEvent);
        } else if (eventType == KeyEvent.KEY_RELEASED) {
            keyRelease(sc, mouseEvent);
        }
    }
    
    public void sendEvent(AWTEvent X_event) {
        if (enableAutoCapture && autoCaptureTimeout < System.currentTimeMillis()) {
            enableAutoCapture = false;
            forceKeyboardReaction();
            forceMouseAutoCapture();
            autoPositionMouse();
        }
        
        final event_t.mouseevent_t mouseEvent = mouse_event.get();
        // Keyboard events get priority vs mouse events.
        // In the case of combined input, however, we need
        if (!ignorebutton) {
            switch (X_event.getID()) {
                case KeyEvent.KEY_PRESSED: {
                    keyPress(Signals.getScanCode((KeyEvent) X_event), mouseEvent);
                    break;
                } case KeyEvent.KEY_RELEASED: {
                    keyRelease(Signals.getScanCode((KeyEvent) X_event), mouseEvent);
                    break;
                } case KeyEvent.KEY_TYPED: {
                    DM.PostEvent(Signals.getScanCode((KeyEvent) X_event).doomEventUp);

                    if (prevmousebuttons != 0) {
                        // Allow combined mouse/keyboard events.
                        mouseEvent.buttons = prevmousebuttons;
                        DM.PostEvent(mouseEvent);
                    }
                    break;
                }
            }
        }

        // Mouse events are also handled, but with secondary priority.
        // Ignore ALL mouse events if we are moving the window.
        if (!weAreMoving) {
            ProcessMouse(X_event, mouseEvent);
        }
        
        // Now for window events. This includes the mouse breaking the border.
        ProcessWindow(X_event); 
    }

    private void ProcessMouse(AWTEvent X_event, final event_t.mouseevent_t mouseEvent) {
        final MouseEvent MEV;
        final Point tmp;
        switch (X_event.getID()) {
            // ButtonPress
            case MouseEvent.MOUSE_PRESSED:
                MEV = (MouseEvent) X_event;
                mouseEvent.type = evtype_t.ev_mouse;
                mouseEvent.buttons = prevmousebuttons
                        = (MEV.getButton() == MouseEvent.BUTTON1 ? event_t.MOUSE_LEFT : 0)
                        | (MEV.getButton() == MouseEvent.BUTTON2 ? event_t.MOUSE_RIGHT : 0)
                        | (MEV.getButton() == MouseEvent.BUTTON3 ? event_t.MOUSE_MID : 0);
                mouseEvent.x = mouseEvent.y = 0;
                DM.PostEvent(mouseEvent);
                break;
            // ButtonRelease
            // This must send out an amended event.
            case MouseEvent.MOUSE_RELEASED:
                MEV = (MouseEvent) X_event;
                mouseEvent.type = evtype_t.ev_mouse;
                mouseEvent.buttons = prevmousebuttons
                        ^= (MEV.getButton() == MouseEvent.BUTTON1 ? event_t.MOUSE_LEFT : 0)
                        | (MEV.getButton() == MouseEvent.BUTTON2 ? event_t.MOUSE_RIGHT : 0)
                        | (MEV.getButton() == MouseEvent.BUTTON3 ? event_t.MOUSE_MID : 0);
                // A PURE mouse up event has no movement.
                mouseEvent.x = mouseEvent.y = 0;
                DM.PostEvent(mouseEvent);
                break;
            case MouseEvent.MOUSE_MOVED:
                MEV = (MouseEvent) X_event;
                tmp = MEV.getPoint();
                //this.AddPoint(tmp,center);
                mouseEvent.type = evtype_t.ev_mouse;
                this.mousedx = (tmp.x - win_w2);
                this.mousedy = (win_h2 - tmp.y);

                // A pure move has no buttons.
                mouseEvent.buttons = prevmousebuttons = 0;
                mouseEvent.x = (mousedx) << 2;
                mouseEvent.y = (mousedy) << 2;

                if ((mouseEvent.x | mouseEvent.y) != 0) {
                    DM.PostEvent(mouseEvent);
                }
                break;

            case MouseEvent.MOUSE_DRAGGED:
                MEV = (MouseEvent) X_event;
                tmp = MEV.getPoint();
                this.mousedx = (tmp.x - win_w2);
                this.mousedy = (win_h2 - tmp.y);
                mouseEvent.type = evtype_t.ev_mouse;

                // A drag means no change in button state.
                mouseEvent.buttons = prevmousebuttons;
                mouseEvent.x = (mousedx) << 2;
                mouseEvent.y = (mousedy) << 2;

                if ((mouseEvent.x | mouseEvent.y) != 0) {
                    DM.PostEvent(mouseEvent);
                }
                break;
        }
    }

    protected volatile boolean weAreMoving = false;
    protected volatile boolean enableAutoCapture = false;
    protected volatile long autoCaptureTimeout = 0L;

    /**
     * Ehrwww.. This amount of gain-lost-exit-click-boom-puff-splesk-drink scenarios are so fucked up!
     * I hope I've fixed most of them - say 85% of what a player will usually encounter.
     * 
     * However, I am sure there are still cases when it will break a bit.
     *  - Good Sign 2017/04/22
     */
    private void ProcessWindow(AWTEvent X_event) {
        switch (X_event.getID()) {
            /**
             * All events that have to do with the window being changed,
             * moved etc. should go here. The most often result
             * in focus being lost and position being changed, so we
             * need to take charge.
             */
            case WindowEvent.WINDOW_ACTIVATED:
            case WindowEvent.WINDOW_DEICONIFIED:
            case ComponentEvent.COMPONENT_RESIZED:
                content.requestFocusInWindow();
                break;

            /**
             * This set of events is for passive focus gain. The focus will be automatically
             * gain and keys and mouse captured as fast as the game will start.
             * Also they will be captured normally when the mouse moves into the window,
             * for example, after switching tasks.
             * 
             * The window opened event also repeats if you switch full screen, and muse entered
             * event will not happen if you have dragged the window, until you click into it
             * or switch to another window, then back.
             */
            case MouseEvent.MOUSE_ENTERED:
                autoPositionMouse();
                break;
            case WindowEvent.WINDOW_OPENED:
                forceMouseAutoCapture();
                forceKeyboardReaction();
                autoPositionMouse();
                
            /**
             * The next set of rules is for active focus gain. It could be done in two ways:
             * natural, when window become visible topmost window with active borders,
             * and when you click with mouse into the window.
             * 
             * Focus gain *must not* capture the mouse and keys immediately, or it will
             * start to process undesirable events, i.e. start firing your weapon or
             * navigating menus.
             */
            case MouseEvent.MOUSE_CLICKED:
            case WindowEvent.WINDOW_GAINED_FOCUS:
                // Re-elable acquiring mouse and keyboard focus after 300 millisecond delay
                delayedAutoCapture(300);
                break;
                
            /**
             * This set of rules are for ultimately releasing any capture on mouse and keyboard
             * When mouse exits the window, it no longer is listened by the game.
             * When the mouse returns to the window, it should be captured back.
             * 
             * When the window additionally loses focus, keyboard reactions are disabled and events already
             * sent to the underlying engine are cleared, or when you drag the window by its border,
             * the mouse will not be captured back upon return to the window. You will have to
             * click inside the window or make window inactive, then active again.
             */
            case WindowEvent.WINDOW_LOST_FOCUS:
                forciblyClearEvents();
                disableKeyboardReaction();
            case ComponentEvent.COMPONENT_MOVED:
                disableMouseAutoCapture();
            case MouseEvent.MOUSE_EXITED:
                // If we happen to barely slide mouse through window, disable the timeout to capture it
                enableAutoCapture = false;
                break;
        }
        
        /**
         * If all the conditions are set up properly, capture the mouse
         * and move its real cursor using Robot
         */
        autoCaptureMouse();
        mousedx = mousedy = 0; // don't spaz.
    }

    /**
     * Will hide the cursor and set internal component positions tracking it,
     * but not touching the cursor's real position. After 300 ms will
     * enable processing of mouse clicks and keyboard strokes
     */
    private void delayedAutoCapture(int delayMs) {
        if (!mouseLoose) {
            content.setCursor(hidden);
        }
        
        autoCaptureTimeout= System.currentTimeMillis() + delayMs;
        enableAutoCapture = true;
    }

    /**
     * As the window gains focus, keyboard input should be processed
     */
    private void forceKeyboardReaction() {
        ignorebutton = false;
    }

    /**
     * If the mouse moved, don't wait until it managed to get out of the
     * window to bring it back.
     */
    private void autoCaptureMouse() {
        if (mouseLoose) {
            return;
        }
        
        if ((!weAreMoving || enableAutoCapture) && (mousedx != 0 || mousedy != 0)) {
            // move the mouse to the window center again
            if (robby != null) {
                robby.mouseMove(offset.x + win_w2, offset.y + win_h2);
            }
        }
    }

    /**
     * As the window gains focus, mouse capture should be enabled again
     */
    private void forceMouseAutoCapture() {
        weAreMoving = false;
    }

    /**
     * Repositions the mouse if auto-capture is enabled
     */
    private void autoPositionMouse() {
        if (!weAreMoving || enableAutoCapture) {
            reposition();
        }
    }

    /**
     * If the window is not in focus, do not capture the mouse
     */
    private void disableMouseAutoCapture() {
        weAreMoving = true;
    }
    
    /**
     * Do not react on user keyboard key presses in the game
     */
    private void disableKeyboardReaction() {
        ignorebutton = true;
    }

    /**
     * Forcibly clear mouse and key events in the underlying engine
     * Discard cursor modifications
     */
    private void forciblyClearEvents() {
        DM.PostEvent(event_t.CANCEL_MOUSE);
        DM.PostEvent(event_t.CANCEL_KEYS);
        content.setCursor(normal);
    }
}
