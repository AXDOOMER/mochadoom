package v;

import static data.Defines.RANGECHECK;

import i.DoomStatusAware;
import i.IDoomSystem;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;

import doom.DoomStatus;

import m.BBox;
import rr.LightsAndColors;
import rr.patch_t;

public abstract class SoftwareVideoRenderer<T,V>
        implements DoomVideoRenderer<T,V>, IVideoScaleAware, DoomStatusAware{
    
    /** Light levels. Binded to the colormap subsystem */
    public static final int NUMLIGHTS=LightsAndColors.LIGHTLEVELS;
    
    /** Colormap used for static graphics (menu etc.) */
    public static final int CMAP_FIXED=0;

    protected IDoomSystem I;
    protected Image currentscreen;
    
    public Image getCurrentScreen(){
    	return currentscreen;
    }
    
    public final void updateStatus(DoomStatus<?,?> DM){
    	this.I=DM.I;
    }
    
    protected int width;
    protected int height;

    /** Each screen is [SCREENWIDTH*SCREENHEIGHT]; 
     *  This is what the various modules (menu, automap,
     *  renderer etc.) get to manipulate at the pixel
     *  level. To go beyond 8 bit displays, these must be extended */ 
    protected V[]         screens;  
     
    //MAES: maybe this should be a bbox?

    public BBox             dirtybox=new BBox();
    
    /** Colormaps are now part of the base software renderer. This 
     *  allows some flexibility over manipulating them.
     *  
     *  Use base as immutable, use work for applying effects.
     *  
     */
    
    protected V[] cmap_base,cmap_work;
    
    /** PLAYPAL-read palettes, used to build dynamic color maps 
     *  Use [z*maxpalettes+y] form, where z=gamme, y=palette
     * */
    
    protected int[][] palettes;
    
    
    public SoftwareVideoRenderer(){
        // Defaults
        width=SCREENWIDTH;
        height=SCREENHEIGHT;
    }

    public SoftwareVideoRenderer(int w,int h){
        // Defaults
        width=w;
        height=h;
    }
    
    protected int  usegamma=0;
    protected int  usepalette=0;
    protected int maxpalettes;
    protected int maxgammas;
    protected int currentpal;
    protected int currentgamma;
    protected int usescreen=0;
    
    public final int getUsegamma() {
        return usegamma;
    }
    
    /** V_Markrect:
     *  Apparently, video.c had its own "dirtybox" bbox, and this was a crude method for 
     *  expanding it.
     * 
     */
    
    public final void MarkRect ( int      x,  int     y,  int     width,  int     height ) 
    { 
        dirtybox.AddToBox(x, y); 
        dirtybox.AddToBox(x+width-1, y+height-1); 
    } 
    
  /**
   *  V_CopyRect 
   */

  public final void CopyRect (int       srcx,
          int     srcy,
          int     srcscrn,
          int     width,
          int     height,
          int     destx,
          int     desty,
          int     destscrn ) 
  { 
      // These are pointers inside an array.
      final V  src=screens[srcscrn];
      final V  dest=screens[destscrn]; 

      if  (RANGECHECK) {
          if (srcx<0
                  ||srcx+width >this.width
                  || srcy<0
                  || srcy+height>SCREENHEIGHT 
                  ||destx<0||destx+width >this.width
                  || desty<0
                  || desty+height>SCREENHEIGHT 
                  || srcscrn>4
                  || destscrn>4)
          {
              I.Error ("Bad V_CopyRect");
          }
      } 
      this.MarkRect (destx, desty, width, height); 


      // MAES: these were pointers to a specific position inside the screen.
      int srcPos = this.width*srcy+srcx; 
      int destPos = this.width*desty+destx; 

      for ( ; height>0 ; height--) 
      { 
          System.arraycopy(src,srcPos, dest, destPos, width);
          //memcpy (dest, src, width); 
          srcPos += this.width; 
          destPos += this.width; 
      }

  }
   
  protected final boolean doRangeCheck(int x, int y,patch_t patch, int scrn){
    return      (x<0
                  ||x+patch.width >this.width
                  || y<0
                  || y+patch.height>this.height 
                  || scrn>4);
  }

  protected final boolean doRangeCheck(int x, int y, int scrn){
      return      (x<0
                    ||x>this.width
                    || y<0
                    || y>this.height 
                    || scrn>4);
    }

  public void DrawPatchSolidScaled ( int x, int y,  int scrn, IVideoScale vs, patch_t patch ){
      this.DrawPatchSolidScaled(x, y, vs.getScalingX(), vs.getScalingY(),scrn, patch);
  }
  
  public final int getHeight() {
      return this.height;
  } 
  public final int getWidth() {
      return this.width;
  } 
  
  public final void DrawPatchDirect(int x, int y, int scrn, patch_t patch) {
      this.DrawPatch(x, y, scrn, patch);
      
  }

  public final V getScreen(int index) {
     return screens[index];
  }
  /*
  public final boolean isRasterNull(int screen){
      for (int i=0;i<screens[screen].length;i++){
          if (screens[screen][i]!=0) return false;
      }
      return true;
  } */
  
  public void setCurrentScreen(int screen){
	  this.usescreen=screen;
  }
  
  public void update(){
	// Override only if there's something else to be done, e.g. map palette to truecolor buffer  
  }
  
  public void report(BufferedImage[] b){
	    System.out.println("Main video buffer "+screens[0]);
	    for (int i=0;i<b.length;i++){
	    System.out.println(((Object)b[i].getRaster()).toString()+" "+b[i].getRaster().hashCode()+" "+((DataBufferByte)(b[i].getRaster().getDataBuffer())).getData());
	    }
	}
  
////////////////////////////VIDEO SCALE STUFF ////////////////////////////////

  protected int SCREENWIDTH=320;
  protected int SCREENHEIGHT=200;
  protected IVideoScale vs;


  @Override
  public void setVideoScale(IVideoScale vs) {
      this.vs=vs;
  }

  @Override
  public void initScaling() {
      this.SCREENHEIGHT=vs.getScreenHeight();
      this.SCREENWIDTH=vs.getScreenWidth();
  }

  /** Built-in method for recovering from palette disasters.
   * Uses PaletteGenerator class to generate Doom's palettes with only the data of
   * the first palette.
   * 
   */
protected final void paletteRecovery() {
	createPalettes(PaletteGenerator.generatePalette(PaletteGenerator.playpal, 256,ColorTint.tints), GammaTables.gammatables, 14, 256, 3, 5);
	
}

/** Internal method for setting up palettes (and gamma tables)
 * 
 */

public void createPalettes(byte[] paldata, short[][] gammadata, final int palettes, final int colors, final int stride,final int gammalevels){
	
	// Sanity check on supplied data length. If there is not enough data to create the specified palettes,
	// their number will be limited.
	
	if (paldata!=null) 	// As many as are likely contained
		maxpalettes=paldata.length/(colors*stride);
	else
		maxpalettes=0; // Do some default action on null palette.

	if (gammadata!=null) 	// As many as are likely contained
		maxgammas=gammadata.length;
	else
		maxgammas=0; // Do some default action on null gamma tables.
	
	if (maxgammas==0){
		gammadata=GammaTables.gammatables;
		maxgammas=GammaTables.gammatables.length;
	}
	

	// Enough data for all palettes. 
	// Enough data for all palettes. 
	if (maxpalettes>0 && maxgammas>0)
			specificPaletteCreation(paldata,gammadata,palettes,colors,stride,gammalevels);
		 else 
			 paletteRecovery();
    	  
      }
    
/** Override this in extending classes to perform specific actions depending on the
 *  type of renderer. It's better not to assign a default action, nor make assumptions
 *  on the underlying types of actual palettes
 * 
 * @param paldata
 * @param gammadata
 * @param palettes
 * @param colors
 * @param stride
 * @param gammalevels
 */

protected abstract void specificPaletteCreation(byte[] paldata,
		short[][] gammadata, 
		final int palettes, 
		final int colors,
		final int stride,
		final int gammalevels);

protected int lastcolor=-1;
protected byte[] scanline;

public int getBaseColor(int color){
    return color;
}

/** Override if there's something special to do for colormap caches, e.g. during
 *  palette or gamma changes. Not required for indexed.
 */

public void clearCaches(){
    // Does nothing for indexed.
}

/** Should return colormaps, if you ever move their management in here.
 * 
 */

public V[] getColorMaps(){
    return null;
}

public void setColorMaps(V[] colormaps,int num){
    // Dummy
}

public IndexColorModel getPalette(){
    return null;
    }
}
