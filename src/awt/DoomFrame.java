package awt;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.StringTokenizer;

import javax.swing.JFrame;

import i.DoomVideoInterface;
import i.IDoomSystem;
import i.InputListener;
import i.Strings;
import timing.ITicker;
import v.DoomVideoRenderer;
import doom.DoomMain;
import doom.ICommandLineManager;
import doom.event_t;

/** Common code for Doom's video frames */

public abstract class DoomFrame<V> extends JFrame implements DoomVideoInterface<V> {

    protected V RAWSCREEN;
    // Normally only used in fullscreen mode
    
    /** This might differ from the raster's width & height attribute for number of reasons */
    protected Dimension size;
    protected DisplayMode oldDisplayMode;
    protected DisplayMode currentDisplayMode;
    protected GraphicsDevice device;
    protected int X_OFF;
    protected int Y_OFF;    
    
    public DoomFrame(DoomMain<?,V> DM,DoomVideoRenderer<?,V> V) {
        GraphicsEnvironment env = GraphicsEnvironment.
        getLocalGraphicsEnvironment();
        
        GraphicsDevice[] devices = env.getScreenDevices();
        
        // Get device 0, because we're lazy.
        if (devices!=null)
            if (devices.length>0)
                device=devices[0];
        
    	this.DM=DM;
    	this.CM=DM.CM;
    	this.TICK=DM.TICK;
    	this.I=DM.I;
    	this.V= V;
    	
        this.width=V.getWidth();
        this.height=V.getHeight();

    	
    	// Set those here. If fullscreen isn't used, then they won't change.
    	// They are necessary for normal initialization, though.
    	setDefaultDimension(width,height);

	}
    
    /** Default window size and center spot. These might change 
     * upon entering full screen, so don't consider them absolute. 
     * Due to letterboxing and screen doubling, stretching etc. 
     * they might be different that the screen buffer (typically,
     * larger).
     * 
     * @param width
     * @param height
     */
    
    private void setDefaultDimension(int width, int height){
        this.size=new Dimension(width*multiply,height*multiply);
        this.center=new Point (X_OFF+size.width/2, Y_OFF+size.height/2);
    }
    
	/**
	 * 
	 */
	private static final long serialVersionUID = -4130528877723831825L;

	

	protected static final boolean D=false;
	
	// Must be aware of "Doom" so we can pass it event messages inside a crude queue.
	public DoomMain<?,V> DM;            // Must be aware of general status.
	public ICommandLineManager CM; // Must be aware of command line interface.
	protected IDoomSystem I;         // Must be aware of some other shit like event handler
	protected DoomVideoRenderer<?,V> V;    // Must have a video renderer....
	protected ITicker TICK;          // Must be aware of the ticker/
	protected MochaEvents eventhandler; // Separate event handler a la _D_.
	                               // However I won't make it fully "eternity like" yet
	                               // also because it works quite flakey on Linux.
	

  	protected Robot robby;
	protected Canvas drawhere;
	protected Canvas gelatine;
	/** This is the actual screen */
    protected Image screen;
    protected int palette=0;
    //InputListener in;
    protected Graphics2D g2d;
    protected Graphics2D gel2d;
    
    protected Point center;
        
    /** Dimensions of the screen buffers. The display however, might differ due 
     * to e.g. letterboxing */
    protected int width,height;
    protected int multiply=1;
    
    // This stuff should NOT get through in keyboard events.
    protected final int UNACCEPTABLE_MODIFIERS=(int) (InputEvent.ALT_GRAPH_DOWN_MASK+
    										 InputEvent.META_DOWN_MASK+
    										 InputEvent.META_MASK+
    										 InputEvent.WINDOW_EVENT_MASK+
    										 InputEvent.WINDOW_FOCUS_EVENT_MASK);
    
    public String processEvents(){
        StringBuffer tmp=new StringBuffer();
        event_t event;
        while ( (event=InputListener.nextEvent()) != null ) {
            tmp.append(event.type.ordinal()+"\n");
        }
        return tmp.toString();
    }
    

    /**
     * I_SetPalette
     * 
     * Any bit-depth specific palette manipulation is performed by 
     * the VideoRenderer. It can range from simple (paintjob) to
     * complex (multiple BufferedImages with locked data bits...ugh!
     * 
     *@param palette index (normally between 0-14).
     */
	
	@Override
	public void SetPalette (int palette)
	{
		V.setPalette(palette);
		this.screen=V.getCurrentScreen();      
	}
	
	
	/** Call this before attempting to draw anything.
	 * This will create the window, the canvas and everything.
	 * Unlike a simple JFrame, this is not automatic because of the order
	 * Doom does things.
	 * 
	 */
	
	@Override
	public void InitGraphics()
	{

	  String		displayname;
	  String		d;
	  int			n;
	  int			pnum;
	  int			x=0;
	  int			y=0;
	  
	  // warning: char format, different type arg
	  int		xsign=' ';
	  int		ysign=' ';
	  
	  boolean			oktodraw;
	  long	attribmask;

	  // Try setting the locale the US, otherwise there will be problems
	  // with non-US keyboards.
	  if (this.getInputContext()==null || !this.getInputContext().selectInputMethod(java.util.Locale.US)){
		  System.err.println("Could not set the input context to US! Keyboard input will be glitchy!");
	  } else {
		  System.err.println("Input context successfully set to US.");
	  }

	  // check for command-line display name
	  if ( (pnum=CM.CheckParm("-disp"))!=0 ) // suggest parentheses around assignment
		displayname = CM.getArgv(pnum+1);
	  else
		displayname = null;

	  // check for command-line geometry
	  if ( (pnum=CM.CheckParm("-geom"))!=0 ) // suggest parentheses around assignment
	  {
		  try{
		  String eval=CM.getArgv(pnum+1).trim();
		// warning: char format, different type arg 3,5
		//n = sscanf(myargv[pnum+1], "%c%d%c%d", &xsign, &x, &ysign, &y);
		// OK, so we have to read a string that may contain
		// ' '/'+'/'-' and a number. Twice.
		StringTokenizer tk=new StringTokenizer(eval,"-+ ");
		// Signs. Consider positive.
		xsign=1;ysign=1;
		for (int i=0;i<eval.length();i++){
			if (eval.charAt(i)=='-'){
				// First '-' on trimmed string: negagive
				if (i==0)
					xsign=-1;
				else 
					ysign=-1;
				}
			}
		
		//this should parse two numbers.
		if (tk.countTokens()==2){
			x=xsign*Integer.parseInt(tk.nextToken());
			y=ysign*Integer.parseInt(tk.nextToken());
		}
		  

		  } catch (NumberFormatException e){
		    I.Error("bad -geom parameter");
		  }
	  }

	  // open the display
	  // AWT: create the canvas.
	  try{
      //drawhere=new Canvas();
      // MAES: this method works even on "stubborn" Linux distros that 
      // fuck up the window size.
	  setCanvasSize(size);
      
      this.eventhandler=new MochaEvents(DM,drawhere);
      
      // AWT: Add listeners to CANVAS element.
      // Maybe it should go to the gelatine component?
      drawhere.addKeyListener(eventhandler);
      drawhere.addMouseListener(eventhandler);
      drawhere.addMouseMotionListener(eventhandler);
      addComponentListener(eventhandler);
      addWindowFocusListener(eventhandler);
      addWindowListener(eventhandler);
      
      if (DM.VM.getSetting("fullscreen").getBoolean())
          switchToFullScreen();
      
      
	  } catch (Exception e){
		  I.Error("Error creating AWTDoom frame. Exiting. Reason: %s",e.getMessage());
	  }
	 
	  // AWT: tab is a special case :-/
	  // We need to "peg" it to the JFrame, rather than the canvas,
	  // and the handler itself cannot auto-assign it.
	  
      final Component me=drawhere;
	  
      KeyboardFocusManager.
      getCurrentKeyboardFocusManager().
      addKeyEventDispatcher(new KeyEventDispatcher() {
    	  
    	  boolean press=false;
          public boolean dispatchKeyEvent(KeyEvent e) {    
            if (e.getKeyCode() == KeyEvent.VK_TAB) {
            	// MAES: simulating a key type.
            	if (press)
                eventhandler.keyPressed(
                		new KeyEvent(me, e.getID(), System.nanoTime(),0 , KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED));
            	else
                    eventhandler.keyReleased(
                    		new KeyEvent(me, e.getID(), System.nanoTime(),0 , KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED));
            	press=!press;
            }  
            return false;
          }
      });
	  
	  this.add(drawhere);
	 // this.add(gelatine);
	  this.getContentPane().setPreferredSize(drawhere.getPreferredSize());
	  
	  // JFrame's size is auto-set here.
	  this.pack();
	  this.setVisible(true);
      this.setResizable(false);
	  this.setTitle(Strings.MOCHA_DOOM_TITLE);
	  this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	  // Gently tell the eventhandler to wake up and set itself.	  
	  this.requestFocus();
	  this.eventhandler.addEvent(MochaDoomInputEvent.GET_YOUR_ASS_OFF);
	  SetGamma(0); 
	  
	}
	
    private void setCanvasSize(Dimension size) {
        
        drawhere.setPreferredSize(size);
        drawhere.setBounds(0, 0, drawhere.getWidth()-1,drawhere.getHeight()-1);
        drawhere.setBackground(Color.black);

        gelatine.setPreferredSize(size);
        gelatine.setBounds(0, 0, drawhere.getWidth()-1,drawhere.getHeight()-1);
        gelatine.setBackground(Color.black);
        
    }


    /** FULLSCREEN SWITCH CODE
     * TODO: it's not enough to do this without also switching the screen's resolution.
     * Unfortunately, Java only has a handful of options which depend on the OS, driver,
     * display, JVM etc. and it's not possible to switch to arbitrary resolutions. 
     * 
     * Therefore, a "best fit" strategy with centering is used.
     * 
     */
	
	private void switchToFullScreen() {
	    boolean isFullScreen = device.isFullScreenSupported();
	      setUndecorated(isFullScreen);
	      setResizable(!isFullScreen);
	      
	      // In case we need to revert.
	      oldDisplayMode 
	      = device.getDisplayMode();
	      
	      DisplayModePicker dmp=new DisplayModePicker(device);
	      
	      // TODO: what if bit depths are too small?
	      DisplayMode dm=dmp.pickClosest(width,height);
	      
	      int[] xy=dmp.getCentering(width,height, dm);
	      this.X_OFF=xy[0];
	      this.Y_OFF=xy[1];
	      
	      device.getDisplayModes(); 
	      
	      if (isFullScreen) {
	          // Full-screen mode
	          device.setFullScreenWindow(this);
	          if (device.isDisplayChangeSupported())
	              device.setDisplayMode(dm);
	          validate();
	          
	          Dimension newsize=new Dimension(dm.getWidth(),dm.getHeight());
	          this.setDefaultDimension(dm.getWidth(),dm.getHeight());
	          setCanvasSize(newsize);
	          
	      } else {
	          // Windowed mode
	          pack();
	          setVisible(true);
	      }
        
    }


    @Override
	public void StartFrame() {
		// Dummy. Nothing special to do...yet.
		
	}

	@Override
	public void StartTic() {

		  if (!this.isActive()) return;

		//  System.out.println("Getting events...");
		  while (eventhandler.hasMoreEvents())
			eventhandler.GetEvent();
		      
		    //eventhandler.grabMouse();

		
	}

	protected int lasttic;
	protected int frames;

    @Override
    public void UpdateNoBlit() {
        // Quite pointless, no?
        
    }

    // Convenience, for testers.
	public void GetEvent() {
		this.eventhandler.GetEvent();		
	}

	@Override
	public void ShutdownGraphics() {
		this.dispose();
		((JFrame)this).dispose();
		
	}

	   
    public void SetGamma(int level){
        if (D) System.err.println("Setting gamma "+level);
        V.setUsegamma(level);
        screen=V.getCurrentScreen(); // Refresh screen after change.
        RAWSCREEN=V.getScreen(DoomVideoRenderer.SCREEN_FG);
    }
    
    
    /** Modified update method: no context needs to passed.
     *  Will render only internal screens. Common between AWT 
     *  and Swing
     * 
     */
    public void paint(Graphics g) {
       // Techdemo v1.3: Mac OSX fix, compatible with Windows and Linux.
       // Should probably run just once. Overhead is minimal
       // compared to actually DRAWING the stuff.
       if (g2d==null) g2d = (Graphics2D)drawhere.getGraphics();
       //if (gel2d==null) gel2d= (Graphics2D)gelatine.getGraphics();
       V.update();
       //voli.getGraphics().drawImage(bi,0,0,null);
       g2d.drawImage(screen,X_OFF,Y_OFF,this);
       //gel2d.setColor(new Color(.3f, .4f, .5f, .1f));
       //gel2d.fillRect(0, 0, this.getWidth(), this.getHeight());
       
    }

}
