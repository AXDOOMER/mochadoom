package v;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferUShort;
import static rr.LightsAndColors.LIGHTLEVELS;
import m.BBox;

/* Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: BufferedRenderer16.java,v 1.4 2012/11/06 16:07:00 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
//
// This source is available for distribution and/or modification
// only under the terms of the DOOM Source Code License as
// published by id Software. All rights reserved.
//
// The source is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// FITNESS FOR A PARTICULAR PURPOSE. See the DOOM Source Code License
// for more details.
//
// $Log: BufferedRenderer16.java,v $
// Revision 1.4  2012/11/06 16:07:00  velktron
// Corrected palette & color generation.
//
// Revision 1.3  2012/09/24 17:16:23  velktron
// Massive merge between HiColor and HEAD. There's no difference from now on, and development continues on HEAD.
//
// Revision 1.2.2.5  2012/09/24 16:56:06  velktron
// New hierarchy, less code repetition.
//
// Revision 1.2.2.4  2011/11/29 12:45:29  velktron
// Restored palette and gamma effects. They do work, but display hysteresis.
//
// Revision 1.2.2.3  2011/11/27 18:19:58  velktron
// Added cache clearing to keep memory down.
//
// Revision 1.2.2.2  2011/11/18 21:36:55  velktron
// More 16-bit goodness.
//
// Revision 1.2.2.1  2011/11/14 00:27:11  velktron
// A barely functional HiColor branch. Most stuff broken. DO NOT USE
//
// DESCRIPTION:
//	Gamma correction LUT stuff.
//	Functions to draw patches (by post) directly to screen.
//	Functions to blit a block to the screen.
//
//-----------------------------------------------------------------------------
 * 
 */

public class BufferedRenderer16 extends SoftwareVideoRenderer16 {
	
static final String rcsid = "$Id: BufferedRenderer16.java,v 1.4 2012/11/06 16:07:00 velktron Exp $";

/** Buffered Renderer has a bunch of images "pegged" to the underlying arrays */

public BufferedImage[] screenbuffer=new BufferedImage[5];

public BufferedRenderer16(int w, int h) {
    super(w,h);
}

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

/** This implementation will "tie" a bufferedimage to the underlying byte raster.
 * 
 * NOTE: this relies on the ability to "tap" into a BufferedImage's backing array,
 * in order to have fast writes without setpixel/getpixel. If that is not possible,
 * then we'll need to use a special renderer.
 * 
 */
@Override
public final void setScreen(int index, int width, int height){

	if (screens[index]==null){
		screenbuffer[index]=new BufferedImage(width,height,BufferedImage.TYPE_USHORT_555_RGB);
    
    	screens[index]=((DataBufferUShort)screenbuffer[index].getRaster().getDataBuffer()).getData();
		}
}

public void setPalette(int palette){
    this.usepalette=palette%maxpalettes;

    // Invalidate cached graphics, otherwise older colormaps
    // will persist.
    this.clearCaches();

    
    // Tint the current set of colormaps.
    getCachedCmap(palette);

    
	//this.currentpal=palette%maxpalettes;
	this.currentscreen=this.screenbuffer[0];
	
}

@Override
public void setUsegamma(int gamma) {
	this.usegamma=gamma%maxgammas;

   // Invalidate palette cache.
    super.clearPalettes();

	
	// Re-synthesize current palette.
	setPalette(usepalette);
}

public void setCurrentScreen(int screen){
	  super.setCurrentScreen(screen);
	  this.currentscreen=this.screenbuffer[0];
}

@Override
protected final void specificPaletteCreation(byte[] paldata,
        short[][] gammadata, 
        final int palettes, 
        final int colors,
        final int stride,
        final int gammalevels){

      System.out.printf("Enough data for %d palettes",maxpalettes);
      System.out.printf("Enough data for %d gamma levels",maxgammas);
      
      //this.palettes=new int[maxpalettes*maxgammas][];
      this.palettes=new int[maxpalettes][];
      
      // Apply gammas a-posteriori, not a-priori.
      // Initial palette can be neutral or based upon "gamma 0",
      // which is actually a bit biased and distorted
      
      for (int z=0;z<1;z++){
          
          // For each palette
          for (int y=0;y<maxpalettes;y++){
              this.palettes[z*maxpalettes+y]=new int[colors];
              
              for (int x=0;x<colors;x++){
                  int r=gammadata[z][0xFF&paldata[y*colors*stride+stride*x]]; // R
                  int g=gammadata[z][0xFF&paldata[1+y*colors*stride+stride*x]]; // G
                  int b=gammadata[z][0xFF&paldata[2+y*colors*stride+stride*x]]; // B
                  int color=0xFF000000|r<<16|g<<8|b;
                  this.palettes[z*maxpalettes+y][x]=color;
                }
            }
      }
      
      // Set base colormap
      
      cmap_base=PaletteGenerator.RF_BuildLights15(this.palettes[0], NUMLIGHTS);
      cmap_work=PaletteGenerator.RF_BuildLights15(this.palettes[0], NUMLIGHTS);

}

public int getBaseColor(int color){
	return cmap_work[0][color];
}

}
