package v;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import m.BBox;

/** Truecolor (24/32-bit) renderer. Uses indexed palette LUTs for a RGB888
 * display. BufferedImage.TYPE_INT_RGB seems to be the fastest, almost on part 
 * with built-in indexed, while ARGB has nearly half the performace. Go figure. 
 * 
 * @author velktron
 *
 */

public class ParallelTrueColorRenderer extends SoftwareVideoRenderer {
	
static final String rcsid = "$Id: ParallelTrueColorRenderer.java,v 1.2 2011/10/23 20:40:44 velktron Exp $";


/* With a truecolour raster, some things are indeed easier */
protected int[][] palettes;
protected int[] raster;
protected final int nrOfProcessors;
protected Runnable[] paletteThreads;
protected final Executor executor;

public ParallelTrueColorRenderer(){
super();
Runtime runtime = Runtime.getRuntime();
nrOfProcessors = runtime.availableProcessors();
updateBarrier=new CyclicBarrier(nrOfProcessors+1);
paletteThreads=new PaletteThread[nrOfProcessors];
int len=this.getHeight()*this.getWidth();
int chunk=len/nrOfProcessors;
for (int i=0;i<nrOfProcessors;i++){
	paletteThreads[i]=new PaletteThread(i*chunk,(i+1)*chunk);
}
this.executor=Executors.newFixedThreadPool(nrOfProcessors);
}

public ParallelTrueColorRenderer(int w,int h){
    // Defaults
    super(w,h);
    Runtime runtime = Runtime.getRuntime();
    nrOfProcessors = runtime.availableProcessors();
    updateBarrier=new CyclicBarrier(nrOfProcessors+1);
    paletteThreads=new PaletteThread[nrOfProcessors];
    int len=w*h;
    int chunk=len/nrOfProcessors;
    for (int i=0;i<nrOfProcessors;i++){
    	paletteThreads[i]=new PaletteThread(i*chunk,(i+1)*chunk);
    }
    this.executor=Executors.newFixedThreadPool(nrOfProcessors);
}

public void Init () 
{ 
 int		i;
 for (i=0 ; i<screens.length ; i++){
	screens[i] = new byte[this.getHeight()*this.getWidth()];
	}
     dirtybox=new BBox();
     
  // Using ARGB is half the speed, WTF? While RGB is almost as fast as indexed. Go figure.
  this.currentscreen=new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
  this.mapInternalRasterToBufferedImage((BufferedImage) currentscreen);
}

@Override
public void setUsegamma(int gammalevel) {
	this.usegamma=gammalevel%maxgammas;
	
}

@Override
public void setPalette(int palette) {
	this.usepalette=palette%maxpalettes;
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
	  
	  this.palettes=new int[maxgammas*maxpalettes][];
	  
	  for (int z=0;z<maxgammas;z++){
		  
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

}

/** Hotlinks a 32-bit "canvas" (the raster int[] array) to an externally supplied
 *  buffered image. Now whatever we write into raster, will appear in the image as well,
 *  without using drawing primitives. Necessary for fast rendering.
 *  
 * @param b
 */

private void mapInternalRasterToBufferedImage(BufferedImage b){
    raster=((DataBufferInt)(b.getRaster().getDataBuffer())).getData();
    
}

/** Update "canvas" to one of the internal screens.
 *  
 * @param screen
 * @param b
 */

@Override
public  final void update()  {
	
	for (int i=0;i<this.nrOfProcessors;i++){
		executor.execute(paletteThreads[i]);
	}
	try {
		updateBarrier.await();
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (BrokenBarrierException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	/*
    final byte[] scr=this.screens[usescreen];
    final int length=scr.length; 
    final int[] pal=this.palettes[usegamma*maxpalettes+usepalette];
    for (int i=0;i<length;i+=16){
        raster[i]=pal[0xFF&scr[i]];
        raster[i+1]=pal[0xFF&scr[i+1]];
        raster[i+2]=pal[0xFF&scr[i+2]];
        raster[i+3]=pal[0xFF&scr[i+3]];
        raster[i+4]=pal[0xFF&scr[i+4]];
        raster[i+5]=pal[0xFF&scr[i+5]];
        raster[i+6]=pal[0xFF&scr[i+6]];
        raster[i+7]=pal[0xFF&scr[i+7]];
        raster[i+8]=pal[0xFF&scr[i+8]];
        raster[i+9]=pal[0xFF&scr[i+9]];
        raster[i+10]=pal[0xFF&scr[i+10]];
        raster[i+11]=pal[0xFF&scr[i+11]];
        raster[i+12]=pal[0xFF&scr[i+12]];
        raster[i+13]=pal[0xFF&scr[i+13]];
        raster[i+14]=pal[0xFF&scr[i+14]];
        raster[i+15]=pal[0xFF&scr[i+15]];

    } */
}

protected final CyclicBarrier updateBarrier;

private class PaletteThread implements Runnable{

	private final int start;
	private final int stop;
	
	public PaletteThread(int start, int stop){
		this.start=start;
		this.stop=stop;
	}
	
	@Override
	public void run() {
		
    	final byte[] scr=screens[usescreen];
    	final int[] pal=palettes[usegamma*maxpalettes+usepalette];
	    for (int i=start;i<stop;i+=16){
	    	raster[i]=pal[0xFF&scr[i]];
	        raster[i+1]=pal[0xFF&scr[i+1]];
	        raster[i+2]=pal[0xFF&scr[i+2]];
	        raster[i+3]=pal[0xFF&scr[i+3]];
	        raster[i+4]=pal[0xFF&scr[i+4]];
	        raster[i+5]=pal[0xFF&scr[i+5]];
	        raster[i+6]=pal[0xFF&scr[i+6]];
	        raster[i+7]=pal[0xFF&scr[i+7]];
	        raster[i+8]=pal[0xFF&scr[i+8]];
	        raster[i+9]=pal[0xFF&scr[i+9]];
	        raster[i+10]=pal[0xFF&scr[i+10]];
	        raster[i+11]=pal[0xFF&scr[i+11]];
	        raster[i+12]=pal[0xFF&scr[i+12]];
	        raster[i+13]=pal[0xFF&scr[i+13]];
	        raster[i+14]=pal[0xFF&scr[i+14]];
	        raster[i+15]=pal[0xFF&scr[i+15]];

	    }
        try {
			updateBarrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}


}

