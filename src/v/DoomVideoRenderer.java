package v;

import java.awt.Image;
import java.awt.image.IndexColorModel;
import java.io.IOException;

import rr.patch_t;

/** DoomVideoSystem is now an interface, that all "video drivers" (wheter do screen, disk, etc.)
 *  must implement. 
 *  
 *  23/10/2011: Made into a generic type, which affects the underlying raw screen data
 *  type. This should make -in theory- true colour or super-indexed (>8 bits) video modes
 *  possible. The catch is that everything directly meddling with the renderer must also
 *  be aware of the underlying implementation. E.g. the various screen arrays will not be
 *  necessarily byte[].
 * 
 * @author Maes
 *
 */

public interface DoomVideoRenderer<T,V> extends IVideoScaleAware {
    
    //flags hacked in scrn (not supported by all functions (see src))
    // Added by _D_. Unsure if I should use VSI objects instead, as they
    // already carry scaling information which doesn't need to be repacked...
    public static final int V_NOSCALESTART =      0x010000;   // dont scale x,y, start coords
    public static final int V_SCALESTART        = 0x020000;   // scale x,y, start coords
    public static final int V_SCALEPATCH        = 0x040000;   // scale patch
    public static final int V_NOSCALEPATCH      = 0x080000;   // don't scale patch
    public static final int V_WHITEMAP          = 0x100000;   // draw white (for v_drawstring)    
    public static final int V_FLIPPEDPATCH      = 0x200000;   // flipped in y
    public static final int V_TRANSLUCENTPATCH  = 0x400000;   // draw patch translucent    
    public static final int V_PREDIVIDE  = 0x800000;   // pre-divide by best x/y scale.    
    public static final int V_SCALEOFFSET = 0x1000000; // Scale the patch offset
    
    public static final int SCREEN_FG = 0; // Foreground screen
    public static final int SCREEN_BG= 1; // Used for endlevel/finale BG
    public static final int SCREEN_WS = 2; // Wipe start screen, also used for screenshots
    public static final int SCREEN_WE = 3; // Wipe end screen
    public static final int SCREEN_SB = 4; // Used for status bar
    
    // Allocates buffer screens, call before R_Init.
    public void Init();

    public void CopyRect(int srcx, int srcy, int srcscrn, int width,
            int height, int destx, int desty, int destscrn);
    
    public void FillRect(int srcx, int srcy, int width,
            int height,int destscrn);

    public void DrawPatch(int x, int y, int scrn, patch_t patch);
    
    public void DrawPatchFlipped ( int      x,   int        y,    int       scrn,  patch_t  patch );

    public void DrawPatchDirect(int x, int y, int scrn, patch_t patch);

    /** V_DrawPatch
     * Draws a SOLID (non-masked) patch to the screen with integer scaling
     * m and n.
     * Useful for stuff such as help screens, titlepic and status bar. Not 
     * very useful for menus, though.
     * desttop, dest and source were byte
     */ 

    public void DrawPatchSolidScaled ( int x, int y,int m, int n, int scrn, patch_t patch );
    
    public void DrawPatchSolidScaled ( int x, int y,  int scrn, IVideoScale vs, patch_t patch );
    
    // Draw a linear block of pixels into the view buffer.
    public void DrawBlock(int x, int y, int scrn, int width, int height,
            T src);

    // Draw a linear block of pixels into the view buffer.
    public void DrawBlock(int x, int y, int scrn, int width, int height,
            T src,int offset);
    
    // Reads a linear block of pixels into the view buffer.
    public void GetBlock(int x, int y, int scrn, int width, int height,
            V dest);

    public void MarkRect(int x, int y, int width, int height);

    public V getScreen(int index);
    
    public void setScreen(int index, int width, int height);
    
    public int getUsegamma();    
    
    public void takeScreenShot(int screen, String imagefile, IndexColorModel icm) throws IOException;
    
    public int getWidth();
    
    public int getHeight();
    
    /** Shamelessly ripped from Doom Legacy (for menus, etc) by _D_ ;-)
     * It uses FLAGS (see above) hacked into the scrn parameter, to be
     * parsed afterwards.
     */
    public void DrawScaledPatch(int x, int y, int scrn, IVideoScale VSI,  // hacked flags in it...
            patch_t patch);

    void DrawPatchColScaled(int x,  patch_t patch,int col, IVideoScale vs,
            int screen);
    
    /** Perform any action necessary so that palettes get modified according to specified gamma.
     *  Consider this a TIME CONSUMING operation, so don't call it unless really necessary.
     * 
     * @param gammalevel
     * 
     */
    void setUsegamma(int gammalevel);
    
    /** Perform any action necessary so that the screen output uses the specified palette
     * Consider this a TIME CONSUMING operation, so don't call it unless really necessary.
     * 
     * @param palette
     */
    
    void setPalette(int palette);
    
    
    /** Perform any action necessary so that palettes and gamma tables are created, e.g. by reading
     * from on-disk resources or from somewhere else.
     * 
     */
    void createPalettes(byte[] paldata, short[][] gammadata, final int palettes, final int colors, final int stride,final int gammalevels);
    
    
    /** No matter how complex/weird/arcane palette manipulations you do internally, the AWT module
     *  must always be able to "tap" into what's the current, "correct" screen after all manipulation and
     *  color juju was applied. Call after a palette/gamma change.
     * 
     */
    Image getCurrentScreen();
    
    /** Final call before updating a particular screen. 
     *  In case that e.g. manual palette application or additonal
     *  rendering must be performed on a screen.
     * 
     */
    
    void update();
    
    
    /** Which of the internal screens you want to display next time you call getCurrentScreen
     * 
     * @param screen
     */
    void setCurrentScreen(int screen);

    void FillRect(int color, int screen, int x, int y, int width, int height);
    
    V[] getColorMaps();

    /** Get the value corresponding to a base color (0-255).
     *  Depending on the implementation this might be indexed,
     *  RGB etc. Use whenever you need "absolute" colors.
     * 
     * @return
     */
    int getBaseColor(int color);
    
    /** Clear any byte-to-short or byte-to-int post or flat caches generated 
     *  during e.g. extended color blits or OpenGL acceleration.
     *  
     *  Good moments to call this function include:
     *  *After starting a new level.
     *  *After Menu, Finale, Endlevel screens or Wipers have been deactivated.
     *  *In general, after anything that might use fixed graphics has completed.
     *  
     *  This is necessary because the cache keeps references to 
     */
    
	void clearCaches();
	
	/** Return current palette. Only works for 8-bit renderer, others return null */	
	
	IndexColorModel getPalette();

    
}
