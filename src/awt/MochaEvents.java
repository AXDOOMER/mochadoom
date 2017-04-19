package awt;

import static awt.MochaDoomInputEvent.*;
import java.awt.KeyEventDispatcher;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.util.LinkedList;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * An alternate events class, more suited for handling the complex situations that might arise during daily use, window
 * moving, etc. Use instead of the old AWTEvents, which is present but deprecated.
 *
 * @author vekltron
 *
 */
public class MochaEvents extends Spliterators.AbstractSpliterator<MochaDoomInputEvent> implements WindowListener,
    ComponentListener, KeyEventDispatcher, KeyListener, MouseListener, MouseMotionListener, WindowFocusListener
{
    // modifications of eventQueue must be thread safe!
    private final LinkedList<MochaDoomInputEvent> eventQueue = new LinkedList<>();
    private static final boolean D = false;
    volatile boolean we_are_moving = false;
    volatile boolean ignorebutton;

    MochaEvents() {
        super(Long.MAX_VALUE, ORDERED|CONCURRENT);
    }

    /////////////////////    WINDOW STUFF //////////////////////
    @Override
    public void windowActivated(WindowEvent windowevent) {
        if (D) {
            System.err.println("Window activated");
        }
        eventQueue.add(new MochaDoomInputEvent(CONFIGURE_NOTIFY, null));
    }

    @Override public void windowClosed(WindowEvent windowevent) {}
    @Override public void windowClosing(WindowEvent windowevent) {}

    @Override
    public void windowDeactivated(WindowEvent windowevent) {
        // Clear the queue if focus is lost.
        eventQueue.clear();
    }

    @Override
    public void windowDeiconified(WindowEvent windowevent) {
        eventQueue.add(new MochaDoomInputEvent(CONFIGURE_NOTIFY, null));
    }

    @Override
    public void windowIconified(WindowEvent windowevent) {
        eventQueue.clear();
    }

    @Override
    public void windowOpened(WindowEvent windowevent) {
        eventQueue.add(new MochaDoomInputEvent(CREATE_NOTIFY, null));
    }

    public synchronized void addEvent(MochaDoomInputEvent ev) {
        synchronized (eventQueue) {
            eventQueue.addLast(ev);
        }
    }
    
    ////////////LISTENERS //////////
    ///////////////////////// KEYBOARD EVENTS ///////////////////////////////////
    // UNUSED used commented out because it doesn't appear to work
    // I think that there is no bulletproof method to poll the state of
    // the CAPS LOCK key, and forcing a call to getLockingKeyState()
    // results in an UnsupportedOperationException
    // For now, the best course of action seems to be intercepting toggling only,
    // rather than state. It also looks like a compicated thing to
    // run at every key update.
    /*private void updateLockingKeys() {
        Toolkit toolkit = canvas.getToolkit();
        for (Iterator<Map.Entry<Integer, Boolean>> it =
                 lockingKeyStates.entrySet().iterator();
             it.hasNext();
             ) {
            Map.Entry<Integer, Boolean> entry = it.next(); 
            Integer keyCode = entry.getKey();
            Boolean oldState = entry.getValue();
            try {
            	if (D) System.err.println("Trying");
                boolean newState = toolkit.getLockingKeyState(keyCode);
                if (! Boolean.valueOf(newState).equals(oldState)) {
                	if (D) System.out.println("New event");
                    int eventType =
                        newState ? LOCK_ON
                                 : LOCK_OFF;
                    addEvent(new MochaDoomInputEvent(eventType,null));
                    entry.setValue(newState);
                }
            } catch (UnsupportedOperationException ex) {
                // Key not present
                it.remove();
            }
        }
    }*/
    public void keyPressed(KeyEvent e) {
        // //  updateLockingKeys();
        // if (! lockingKeyStates.containsKey(e.getKeyCode())) {
        if (e.getKeyCode() <= KeyEvent.VK_F12) {
            addEvent(new MochaDoomInputEvent(KEY_PRESS, e));
        }

        e.consume();
        // }
    }

    public void keyReleased(KeyEvent e) {
        // updateLockingKeys();
        // if (! lockingKeyStates.containsKey(e.getKeyCode())) {

        //if ((e.getModifiersEx() & UNACCEPTABLE_MODIFIERS) ==0) {
        if (e.getKeyCode() <= KeyEvent.VK_F12) {
            addEvent(new MochaDoomInputEvent(KEY_RELEASE, e));
        }
        e.consume();
        // }
    }

    public void keyTyped(KeyEvent e) {
        //updateLockingKeys();
        //if (! lockingKeyStates.containsKey(e.getKeyCode())) {
        if (e.getKeyCode() <= KeyEvent.VK_F12) {
            addEvent(new MochaDoomInputEvent(KEY_TYPE, e));
        }

        e.consume();
        //}
    }

    //////////////////////////// MOUSE EVENTS   ////////////////////////////
    @Override
    public void mouseClicked(MouseEvent mouseevent) {
        //System.out.println("Mouse clicked");
        addEvent(new MochaDoomInputEvent(MOUSE_CLICKED, null));
    }

    @Override
    public void mouseEntered(MouseEvent mouseevent) {
        //System.out.println("Mouse entered");
        addEvent(new MochaDoomInputEvent(MOUSE_ENTERED, null));
    }

    @Override
    public void mouseExited(MouseEvent mouseevent) {
        //System.out.println("Mouse exited");
        addEvent(new MochaDoomInputEvent(MOUSE_EXITED, null));
    }

    @Override
    public void mousePressed(MouseEvent mouseevent) {
        if (!we_are_moving) // Don't let presses go through when moving.
        {
            addEvent(new MochaDoomInputEvent(BUTTON_PRESS, mouseevent));
        }
    }

    @Override
    public void mouseReleased(MouseEvent mouseevent) {
        if (!we_are_moving) // Don't let presses go through when moving.
        {
            addEvent(new MochaDoomInputEvent(BUTTON_RELEASE, mouseevent));
        }
    }

    @Override
    public void mouseDragged(MouseEvent mouseevent) {
        addEvent(new MochaDoomInputEvent(DRAG_NOTIFY, mouseevent));
    }

    @Override
    public void mouseMoved(MouseEvent mouseevent) {
        addEvent(new MochaDoomInputEvent(MOTION_NOTIFY, mouseevent));
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        // Do what, here? Pausing would be a good idea.
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        eventQueue.add(new MochaDoomInputEvent(WINDOW_MOVING, null));
    }

    @Override
    public void componentResized(ComponentEvent e) {
        eventQueue.add(new MochaDoomInputEvent(CONFIGURE_NOTIFY, null));
    }

    @Override
    public void componentShown(ComponentEvent e) {
        eventQueue.add(new MochaDoomInputEvent(CREATE_NOTIFY, null));
    }

    @Override
    public void windowGainedFocus(WindowEvent arg0) {
        eventQueue.add(new MochaDoomInputEvent(FOCUS_GAINED, null));
    }

    @Override
    public void windowLostFocus(WindowEvent arg0) {
        eventQueue.add(new MochaDoomInputEvent(FOCUS_LOST, null));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        //if (e.getKeyCode() == KeyEvent.VK_TAB)
        //	eventQueue.add(new MochaDoomInputEvent(KeyRelease, e));
        return false;
    }

    @Override
    public boolean tryAdvance(Consumer<? super MochaDoomInputEvent> action) {
        synchronized (eventQueue) {
            if (!eventQueue.isEmpty()) {
                action.accept(eventQueue.removeFirst());
                return true;
            }
        }
        
        return false;
    }
}
