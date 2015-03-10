package rr.parallel;

import static data.Tables.finetangent;
import static m.fixed_t.*;
import static rr.LightsAndColors.*;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import rr.IDetailAware;
import rr.IGetColumn;
import rr.TextureManager;
import rr.visplane_t;
import rr.drawfuns.ColVars;
import rr.drawfuns.DoomColumnFunction;
import rr.drawfuns.R_DrawColumnBoomOpt;
import rr.drawfuns.R_DrawColumnBoomOptLow;

import data.Tables;

import v.IVideoScale;
import v.IVideoScaleAware;

/** This is what actual executes the RenderSegInstructions.
 *   *  
 *  Each thread actually operates on a FIXED PORTION OF THE SCREEN
 *  (e.g. half-width, third-width etc.) and only renders the portions
 *  of the RenderSegInstructions that are completely contained
 *  within its own screen area. For this reason, all threads
 *  check out all RenderSegInstructions of the list, and render any 
 *  and all portions that are within their responsability domain, so
 *  to speak.
 *  
 *  FIXME there's a complex data dependency with ceilingclip/floorclip 
 *  I was not quite able to fix yet. Practically, in the serial renderer,
 *  calls to RenderSegLoop are done in a correct, non-overlapping order,
 *  and certain parts are drawn before others in order to set current
 *  floor/ceiling markers and visibility e.g. think of a wall visible
 *  through windows.
 *  
 *  FIXME 7/6/2011 Data dependencies and per-thread clipping are now 
 *  fixed, however there is still visible "jitter" or "noise" on some 
 *  of the walls, probably related to column offsets.
 * 
 * @author velktron
 *
 */

public abstract class RenderSegExecutor<T,V> implements Runnable, IVideoScaleAware,IDetailAware {

	// This needs to be set by the partitioner.
    protected int rw_start, rw_end,rsiend;
	// These need to be set on creation, and are unchangeable.
	protected final TextureManager<T> TexMan;
	protected final CyclicBarrier barrier;
	protected RenderSegInstruction<V>[] RSI;
	protected final long[] xtoviewangle;
	protected final short[] ceilingclip, floorclip;

	// Each thread should do its own ceiling/floor blanking
	protected final short[] BLANKFLOORCLIP;
	protected final short[] BLANKCEILINGCLIP;

    protected static final int HEIGHTBITS   =   12;
	protected static final int HEIGHTUNIT   =   (1<<HEIGHTBITS);
	protected final int id;
	
	protected DoomColumnFunction<T,V> colfunchi,colfunclow;
	protected DoomColumnFunction<T,V> colfunc;
	protected ColVars<T,V> dcvars;
	
	public RenderSegExecutor(int SCREENWIDTH, int SCREENHEIGHT,int id,V screen, 
			TextureManager<T> texman,
			RenderSegInstruction<V>[] RSI,
			short[] BLANKCEILINGCLIP,
			short[] BLANKFLOORCLIP,
			short[] ceilingclip,
			short[] floorclip,
			int[] columnofs,
			long[] xtoviewangle,
			int[] ylookup,
			visplane_t[] visplanes,
			CyclicBarrier barrier){
		this.id=id;
		this.TexMan=texman;
		this.RSI=RSI;
		this.barrier=barrier;		
		this.ceilingclip=ceilingclip;
		this.floorclip=floorclip;
		this.xtoviewangle=xtoviewangle;
		this.BLANKCEILINGCLIP=BLANKCEILINGCLIP;
		this.BLANKFLOORCLIP=BLANKFLOORCLIP;
        
	}

	 protected final void ProcessRSI(RenderSegInstruction<V> rsi, int startx,int endx,boolean contained){
         int     angle; // angle_t
         int     index;
         int     yl; // low
         int     yh; // hight
         int     mid;
         int pixlow,pixhigh,pixhighstep,pixlowstep;
         int rw_scale,topfrac,bottomfrac,bottomstep;
         // These are going to be modified A LOT, so we cache them here.
         pixhighstep=rsi.pixhighstep;
         pixlowstep=rsi.pixlowstep;
         bottomstep=rsi.bottomstep;
         //  We must re-scale it.
         int rw_scalestep= rsi.rw_scalestep; 
         int topstep=rsi.topstep;
         int     texturecolumn=0; // fixed_t
         final int bias;
         // Well is entirely contained in our screen zone 
         // (or the very least it starts in it).
         if (contained) bias=0; 
             // We are continuing a wall that started in another 
             // screen zone.
             else bias=(startx-rsi.rw_x);
         // PROBLEM: these must be pre-biased when multithreading.
             rw_scale=rsi.rw_scale+bias*rw_scalestep;
             topfrac = rsi.topfrac+bias*topstep;
             bottomfrac = rsi.bottomfrac+ bias*bottomstep;
             pixlow=rsi.pixlow+bias*pixlowstep;
             pixhigh=rsi.pixhigh+bias*pixhighstep;

         {
            
             for ( int rw_x=startx; rw_x < endx ; rw_x++)
             {
             // mark floor / ceiling areas
             yl = (topfrac+HEIGHTUNIT-1)>>HEIGHTBITS;

             // no space above wall?
             if (yl < ceilingclip[rw_x]+1)
                 yl = ceilingclip[rw_x]+1;
                 
             yh = bottomfrac>>HEIGHTBITS;

             if (yh >= floorclip[rw_x])
                 yh = floorclip[rw_x]-1;
             
           //  System.out.printf("Thread: rw %d yl %d yh %d\n",rw_x,yl,yh);

             // A particular seg has been identified as a floor marker.
             
             
             // texturecolumn and lighting are independent of wall tiers
             if (rsi.segtextured)
             {
                 // calculate texture offset
                
                 
               // CAREFUL: a VERY anomalous point in the code. Their sum is supposed
               // to give an angle not exceeding 45 degrees (or 0x0FFF after shifting).
               // If added with pure unsigned rules, this doesn't hold anymore,
               // not even if accounting for overflow.
                 angle = Tables.toBAMIndex(rsi.rw_centerangle + (int)xtoviewangle[rw_x]);
               //angle = (int) (((rw_centerangle + xtoviewangle[rw_x])&BITS31)>>>ANGLETOFINESHIFT);
               //angle&=0x1FFF;
                 
               // FIXME: We are accessing finetangent here, the code seems pretty confident
               // in that angle won't exceed 4K no matter what. But xtoviewangle
               // alone can yield 8K when shifted.
               // This usually only overflows if we idclip and look at certain directions 
               // (probably angles get fucked up), however it seems rare enough to just 
               // "swallow" the exception. You can eliminate it by anding with 0x1FFF
               // if you're so inclined. 
               
               texturecolumn = rsi.rw_offset-FixedMul(finetangent[angle],rsi.rw_distance);
                texturecolumn >>= FRACBITS;
               // calculate lighting
               index = rw_scale>>LIGHTSCALESHIFT;
       

                 if (index >=  MAXLIGHTSCALE )
                 index = MAXLIGHTSCALE-1;

                 dcvars.dc_colormap = rsi.walllights[index];
                 dcvars.dc_x = rw_x;
                 dcvars.dc_iscale = (int) (0xffffffffL / rw_scale);
             }
             
             // draw the wall tiers
             if (rsi.midtexture!=0)
             {
                 // single sided line
                 dcvars.dc_yl = yl;
                 dcvars.dc_yh = yh;
                 dcvars.dc_texheight = TexMan.getTextureheight(rsi.midtexture)>>FRACBITS; // killough
                 dcvars.dc_texturemid = rsi.rw_midtexturemid;    
                 dcvars.dc_source = TexMan.GetCachedColumn(rsi.midtexture,texturecolumn);
                 dcvars.dc_source_ofs=0;
                 colfunc.invoke();
                 ceilingclip[rw_x] = (short) rsi.viewheight;
                 floorclip[rw_x] = -1;
             }
             else
             {
                 // two sided line
                 if (rsi.toptexture!=0)
                 {
                     // top wall
                     mid = pixhigh>>HEIGHTBITS;
                     pixhigh += pixhighstep;

                     if (mid >= floorclip[rw_x])
                         mid = floorclip[rw_x]-1;

                 if (mid >= yl)
                 {
                     dcvars.dc_yl = yl;
                     dcvars.dc_yh = mid;
                     dcvars.dc_texturemid = rsi.rw_toptexturemid;
                     dcvars.dc_texheight=TexMan.getTextureheight(rsi.toptexture)>>FRACBITS;
                     dcvars.dc_source = TexMan.GetCachedColumn(rsi.toptexture,texturecolumn);
                     //dc_source_ofs=0;
                     colfunc.invoke();
                     ceilingclip[rw_x] = (short) mid;
                 }
                 else
                     ceilingclip[rw_x] = (short) (yl-1);
                 }  // if toptexture
                 else
                 {
                     // no top wall
                     if (rsi.markceiling)
                         ceilingclip[rw_x] = (short) (yl-1);
                 } 
                     
                 if (rsi.bottomtexture!=0)
                 {
                 // bottom wall
                 mid = (pixlow+HEIGHTUNIT-1)>>HEIGHTBITS;
                 pixlow += pixlowstep;

                 // no space above wall?
                 if (mid <= ceilingclip[rw_x])
                     mid = ceilingclip[rw_x]+1;
                 
                 if (mid <= yh)
                 {
                     dcvars.dc_yl = mid;
                     dcvars.dc_yh = yh;
                     dcvars.dc_texturemid = rsi.rw_bottomtexturemid;
                     dcvars.dc_texheight=TexMan.getTextureheight(rsi.bottomtexture)>>FRACBITS;
                     dcvars.dc_source = TexMan.GetCachedColumn(rsi.bottomtexture,texturecolumn);
                     // dc_source_ofs=0;
                     colfunc.invoke();
                     floorclip[rw_x] = (short) mid;
                 }
                 else
                      floorclip[rw_x] = (short) (yh+1);

             } // end-bottomtexture
             else
             {
                 // no bottom wall
                 if (rsi.markfloor)
                     floorclip[rw_x] = (short) (yh+1);
             }
                 
            } // end-else (two-sided line)
                 rw_scale += rw_scalestep;
                 topfrac += topstep;
                 bottomfrac += bottomstep;
             } // end-rw 
         } // end-block
     }
	
	@Override
    public void setDetail(int detailshift) {
        if (detailshift == 0)
            colfunc = colfunchi;
        else
            colfunc = colfunclow;
    }
	
	/** Only called once per screen width change */
	public void setScreenRange(int rwstart, int rwend){
		this.rw_end=rwend;
		this.rw_start=rwstart;
	}

	
	/** How many instructions TOTAL are there to wade through.
	 *  Not all will be executed on one thread, except in some rare
	 *  circumstances. 
	 *  
	 * @param rsiend
	 */
	public void setRSIEnd(int rsiend){
		this.rsiend=rsiend;
	}

	public void run()
	{

		RenderSegInstruction<V> rsi;

		// Each worker blanks its own portion of the floor/ceiling clippers.
		System.arraycopy(BLANKFLOORCLIP,rw_start,floorclip, rw_start,rw_end-rw_start);
		System.arraycopy(BLANKCEILINGCLIP,rw_start,ceilingclip, rw_start,rw_end-rw_start);

		// For each "SegDraw" instruction...
		for (int i=0;i<rsiend;i++){
			rsi=RSI[i];
			dcvars.centery=RSI[i].centery;
			int startx,endx;
			// Does a wall actually start in our screen zone?
			// If yes, we need no bias, since it was meant for it.
			// If the wall started BEFORE our zone, then we
			// will need to add a bias to it (see ProcessRSI).
			// If its entirely non-contained, ProcessRSI won't be
			// called anyway, so we don't need to check for the end.
			
			boolean contained=(rsi.rw_x>=rw_start);
			// Keep to your part of the screen. It's possible that several
			// threads will process the same RSI, but different parts of it.
			
				// Trim stuff that starts before our rw_start position.
				startx=Math.max(rsi.rw_x,rw_start);
				// Similarly, trim stuff after our rw_end position.
				endx=Math.min(rsi.rw_stopx,rw_end);
				// Is there anything to actually draw?
				if ((endx-startx)>0) {
					ProcessRSI(rsi,startx,endx,contained);
					}
		} // end-instruction
	
		try {
			barrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	//protected abstract void ProcessRSI(RenderSegInstruction<V> rsi, int startx,int endx,boolean contained);

	

	////////////////////////////VIDEO SCALE STUFF ////////////////////////////////

	protected int SCREENWIDTH;
	protected int SCREENHEIGHT;
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

	public void updateRSI(RenderSegInstruction<V>[] rsi) {
		this.RSI=rsi;
		}
	
	public static final class HiColor extends RenderSegExecutor<byte[],short[]>{

        public HiColor(int SCREENWIDTH, int SCREENHEIGHT, int id,
                short[] screen, TextureManager<byte[]> texman,
                RenderSegInstruction<short[]>[] RSI, short[] BLANKCEILINGCLIP,
                short[] BLANKFLOORCLIP, short[] ceilingclip, short[] floorclip,
                int[] columnofs, long[] xtoviewangle, int[] ylookup,
                visplane_t[] visplanes, CyclicBarrier barrier) {
            super(SCREENWIDTH, SCREENHEIGHT, id, screen, texman, RSI, BLANKCEILINGCLIP,
                    BLANKFLOORCLIP, ceilingclip, floorclip, columnofs, xtoviewangle,
                    ylookup, visplanes, barrier);
            dcvars=new ColVars<byte[],short[]>();
            colfunc=colfunchi=new R_DrawColumnBoomOpt.HiColor(SCREENWIDTH, SCREENHEIGHT,ylookup,columnofs,dcvars,screen,null );
            colfunclow=new R_DrawColumnBoomOptLow.HiColor(SCREENWIDTH, SCREENHEIGHT,ylookup,columnofs,dcvars,screen,null );
        }
	}
	
	public static final class Indexed extends RenderSegExecutor<byte[],byte[]>{

        public Indexed(int SCREENWIDTH, int SCREENHEIGHT, int id,
                byte[] screen, IGetColumn<byte[]> gc, TextureManager<byte[]> texman,
                RenderSegInstruction<byte[]>[] RSI, short[] BLANKCEILINGCLIP,
                short[] BLANKFLOORCLIP, short[] ceilingclip, short[] floorclip,
                int[] columnofs, long[] xtoviewangle, int[] ylookup,
                visplane_t[] visplanes, CyclicBarrier barrier) {
            super(SCREENWIDTH, SCREENHEIGHT, id, screen, texman, RSI, BLANKCEILINGCLIP,
                    BLANKFLOORCLIP, ceilingclip, floorclip, columnofs, xtoviewangle,
                    ylookup, visplanes, barrier);
            dcvars=new ColVars<byte[],byte[]>();
            colfunc=colfunchi=new R_DrawColumnBoomOpt.Indexed(SCREENWIDTH, SCREENHEIGHT,ylookup,columnofs,dcvars,screen,null );
            colfunclow=new R_DrawColumnBoomOptLow.Indexed(SCREENWIDTH, SCREENHEIGHT,ylookup,columnofs,dcvars,screen,null );
        }
    }
	
}

// $Log: RenderSegExecutor.java,v $
// Revision 1.2  2012/09/24 17:16:22  velktron
// Massive merge between HiColor and HEAD. There's no difference from now on, and development continues on HEAD.
//
// Revision 1.1.2.2  2012/09/21 16:18:29  velktron
// More progress...but no cigar, yet.
//
// Revision 1.1.2.1  2012/09/20 14:20:10  velktron
// Parallel stuff in their own package (STILL BROKEN)
//
// Revision 1.12.2.4  2012/09/19 21:45:41  velktron
// More extensions...
//
// Revision 1.12.2.3  2012/09/18 16:11:50  velktron
// Started new "all in one" approach for unifying Indexed, HiColor and (future) TrueColor branches.
//
// Revision 1.12.2.2  2012/06/15 14:44:09  velktron
// Doesn't need walllights?
//
// Revision 1.12.2.1  2011/11/14 00:27:11  velktron
// A barely functional HiColor branch. Most stuff broken. DO NOT USE
//
// Revision 1.12  2011/11/01 19:04:06  velktron
// Cleaned up, using IDetailAware for subsystems.
//
// Revision 1.11  2011/10/31 18:33:56  velktron
// Much cleaner implementation using the new rendering model. Now it's quite faster, too.
//
// Revision 1.10  2011/07/25 11:39:10  velktron
// Optimized to work without dc_source_ofs (uses only cached, solid textures)
//
// Revision 1.9  2011/07/12 16:29:35  velktron
// Now using GetCachedColumn
//
// Revision 1.8  2011/07/12 16:25:02  velktron
// Removed dependency on per-thread column pointers.
//
// Revision 1.7  2011/06/07 21:21:15  velktron
// Definitively fixed jitter bug, which was due to dc_offset_contention. Now the alternative parallel renderer is just as good as the original one.
//
// Revision 1.6  2011/06/07 13:35:38  velktron
// Definitively fixed drawing priority/zones. Now to solve the jitter :-/
//
// Revision 1.5  2011/06/07 01:32:32  velktron
// Parallel Renderer 2 still buggy :-(
//
// Revision 1.4  2011/06/07 00:50:47  velktron
// Alternate Parallel Renderer fixed.
//
// Revision 1.3  2011/06/07 00:11:11  velktron
// Fixed alternate parallel renderer (seg based). No longer deprecated.
//