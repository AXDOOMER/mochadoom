package v;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;

import m.BBox;

public class BufferedRenderer extends SoftwareVideoRenderer8 {
	
static final String rcsid = "$Id: BufferedRenderer.java,v 1.18 2012/09/24 17:16:23 velktron Exp $";

/** Buffered Renderer has a bunch of images "pegged" to the underlying arrays */

private BufferedImage[] screenbuffer;

/** Indexed renderers keep separate color models for each colormap (intended as gamma levels) and 
 * palette levels */
private IndexColorModel[][] cmaps;

public BufferedRenderer(int w, int h, IndexColorModel icm) {
    super(w,h);
    this.setIcm(icm);
}

/** Normally, you only have the palettes available ONLY after you read the palette from disk.
 *  So use the super contructor, and then this when the palettes are available.
 *  
 * @param icm2
 */

public void setIcm(IndexColorModel icm2) {
    this.icm=icm2;
    
}

public BufferedRenderer(int w, int h) {
    super(w,h);
}

// Used only for internal status.
private IndexColorModel icm;

@Override
public final void Init () 
{ 
 int		i;
 for (i=0 ; i<4 ; i++){
	//screens[i] = new byte[this.getHeight()*this.getWidth()];
     this.setScreen(i, this.width, this.height);     
	}
     dirtybox=new BBox();
}

/** This actually creates a raster with a fixed underlying array, but NOT the images
 *  themselves. So it's possible to have "imageless" rasters (unless you specifically
 *  request to make them visible, of course).
 *  
 */

@Override
public final void setScreen(int index, int width, int height){
	
    // We must FIRST initialize the image, so that the (immutable) color model will be set.
    if (this.icm==null){
    	final byte[] dummy=new byte[256];
    	for (int i=0;i<dummy.length;i++)
    		dummy[i]=(byte) i;    	
    	icm=new IndexColorModel(8,256,dummy,dummy,dummy);
    }
    
    r[index]=icm.createCompatibleWritableRaster(width,height);
    
    // Only create non-visible data, pegged to the raster. Create visible images
    // only on-demand.
    
    screens[index]=((DataBufferByte)r[index].getDataBuffer()).getData();

}


/** We only call this once we have a stable WritableRaster, and we only want
 *  a different colormodel (e.g. after changing gamma). It's slower than keepings
 *  severerl BufferedImages ready, so it's only used when changing gamma. The
 *  backing screen, array etc. should not have changed at this moment.
 * 
 * @param index
 * @param r
 */

private final BufferedImage createScreen(int index,IndexColorModel icm, WritableRaster r){
    return new BufferedImage(icm,r,false,null);
}

/*
public BufferedImage mapBufferedImageToScreen(int screen, IndexColorModel icm){
    // Map databuffer to one of the screens.
    DataBufferByte dbb=new DataBufferByte(screens[screen],screens[screen].length);
    BufferedImage b=new BufferedImage(this.getWidth(),this.getHeight(),BufferedImage.TYPE_BYTE_INDEXED,icm);
    WritableRaster r=WritableRaster.createPackedRaster(dbb,b.getWidth(), b.getHeight(), 8,
        new Point(0,0));
    b.setData(r);
    
    return b;
    
} */

/*
public BufferedImage cloneScreen(int screen, IndexColorModel icm){
    BufferedImage b=new BufferedImage(this.getWidth(),this.getHeight(),BufferedImage.TYPE_BYTE_INDEXED,icm);
    b.setData(screenbuffer[0].getRaster());
    return b;
    
} */




public final void changePalette(int pal){
    this.usepalette=(pal<<8);//+0x00FF;
    //this.usepalette=/*(pal<<8)+*/0xFF;
    
}

/** Get a bunch of BufferedImages "pegged" on the same output screen of this
 *  Doom Video Renderer, but with different palettes, defined in icms[].
 *  This is VERY speed efficient assuming that an IndexedColorModel will be used,
 *  rather than a 32-bit canvas, and memory overhead is minimal. Call this ONLY
 *  ONCE when initializing the video renderer, else it will invalidate pretty much
 *  everything in an ongoing game.
 * 
 *  NOTE: this will actually CREATE a new byte array for the screen, so it's important
 *  that this is called BEFORE anything else taps into it.
 * 
 * @param screen
 * @param icms
 * @return
 */

private BufferedImage[] createScreenForPalettes(int screen,IndexColorModel[] icms) {
        
		// These screens represent a complete range of palettes for a specific gamma
		// and specific screen
        BufferedImage[] b=new BufferedImage[icms.length];
        
        
        // MEGA hack: all images share the same raster data as screenbuffer[screen]
        // If this is the first time we called this method, the actually backing array
        // will be actually created. If not...

        // Create the first of the screens.
        this.icm=icms[0];
        
        if (r[screen]==null){
        	// This will create the first buffered image (and its data array)/
            // as screenbuffer[0]	   	
        	setScreen(screen,this.getWidth(),this.getHeight());
       		}
        
        // This is the base image for this set of palettes (usually index 0).
       
       
        // Create the rest of the screens (with different palettes) on the same raster.
        for (int i=0;i<icms.length;i++){
        	 b[i]=createScreen(screen,icms[i],r[screen]);
        }
        
        return b;
        
    }

protected final void specificPaletteCreation(byte[] paldata,
		short[][] gammadata, 
		final int palettes, 
		final int colors,
		final int stride,
		final int gammalevels){

	  System.out.printf("Enough data for %d palettes",maxpalettes);
	  System.out.printf("Enough data for %d gamma levels",maxgammas);
	  
	  // Create as gamma levels as specified.
	  cmaps=new IndexColorModel[maxgammas][];
	  
	  // First set of palettes, normal gamma.
	  cmaps[0]=new IndexColorModel[maxpalettes];

	  // Now we have our palettes.
	  for (int i=0;i<maxpalettes;i++){
		  cmaps[0][i]=new IndexColorModel(8, colors,paldata, i*stride*colors, false);
	  		}
  
  // Wire the others according to the gamma table.
	  byte[] tmpcmap=new byte[colors*stride];
	  
	  // For each gamma value...
	  for (int j=1;j<maxgammas;j++){
		  
		  cmaps[j]=new IndexColorModel[maxpalettes];
		  
		  // For each palette
		  for (int i=0;i<maxpalettes;i++){
			  
			  for (int k=1;k<256;k++){
				  tmpcmap[3*k]=(byte) gammadata[j][0x00FF&paldata[i*colors*stride+stride*k]]; // R
				  tmpcmap[3*k+1]=(byte) gammadata[j][0x00FF&paldata[1+i*colors*stride+stride*k]]; // G
				  tmpcmap[3*k+2]=(byte) gammadata[j][0x00FF&paldata[2+i*colors*stride+stride*k]]; // B
			  	}

			  cmaps[j][i]=new IndexColorModel(8, 256,tmpcmap, 0, false);
	  		}
	  }

}

private WritableRaster[] r=new WritableRaster[5];

public void setPalette(int palette){
	this.currentpal=palette%maxpalettes;
	this.currentscreen=this.screenbuffer[currentpal];
}

@Override
public void setUsegamma(int gamma) {
	this.usegamma=gamma%maxgammas;
	// Changing gamma also "fixes" the screens!
	this.setCurrentScreen(0);
}

public void setCurrentScreen(int screen){
	  super.setCurrentScreen(screen);
	  this.screenbuffer=this.createScreenForPalettes(usescreen, cmaps[usegamma]);
	  this.currentscreen=this.screenbuffer[currentpal];
}

public IndexColorModel getPalette(){
    return cmaps[0][this.usepalette];
    }

}

//$Log: BufferedRenderer.java,v $
//Revision 1.18  2012/09/24 17:16:23  velktron
//Massive merge between HiColor and HEAD. There's no difference from now on, and development continues on HEAD.
//
//Revision 1.17.2.3  2012/09/24 16:56:06  velktron
//New hierarchy, less code repetition.
//
