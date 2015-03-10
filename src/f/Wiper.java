package f;

import v.DoomVideoRenderer;
import doom.DoomMain;

public abstract class Wiper<T,V> extends AbstractWiper<T,V> {

    static final String rcsid = "$Id: Wiper.java,v 1.18 2012/09/24 17:16:23 velktron Exp $";
    
    protected wipefun[] wipes;
    
    public Wiper(DoomMain<T,V> DC){
        this.updateStatus(DC);
    }
    
    public static final class HiColor extends Wiper<byte[],short[]>{

        public HiColor(DoomMain<byte[], short[]> DC) {
            super(DC);
            wipes=new wipefun[]{
                    new wipe_initColorXForm(), new wipe_doColorXForm(), new wipe_exitColorXForm(),
                    new wipe_initMelt(), new wipe_doMelt(), new wipe_exitMelt()
                    };
        }
        
        /** Those guys sure have an obsession with shit...this is supposed to do some
         * lame-ass transpose.
         * 
         * @param array
         * @param width
         * @param height
         */
        
        protected final void
        shittyColMajorXform
        ( short[]    array,
          int       width,
          int       height )
        {
            int     x;
            int     y;
            short[]  dest;

            dest = new short[width*height];

            for(y=0;y<height;y++)
            for(x=0;x<width;x++){
                dest[x*height+y] = array[y*width+x];
                //dest[(1+x)*height+y] = array[y*width+(1+x)];
            }
            System.arraycopy(dest, 0, array, 0, width*height);

            //Z_Free(dest);

        }
        
        class wipe_doColorXForm implements wipefun{
            
            public boolean
        invoke
        ( int   width,
          int   height,
          int   ticks )
        {
            boolean changed;
            short[]   w=wipe_scr;
            short[]   e=wipe_scr_end;
            int     newval;

            changed = false;
            int pw =0;// wipe_scr;
            int pe = 0; //wipe_scr_end;
            
            while (pw!=width*height)
            {
            if (w[pw] != e[pe])
            {
                if (w[pw] > e[pe])
                {
                newval = w[pw] - ticks;
                if (newval < e[pe])
                    w[pw] = e[pe];
                else
                    w[pw] = (byte) newval;
                changed = true;
                }
                else if (w[pw] < e[pe])
                {
                newval = w[pw] + ticks;
                if (newval > e[pe])
                    w[pw] = e[pe];
                else
                    w[pw] = (byte) newval;
                changed = true;
                }
            }
            pw++;
            pe++;
            }

            return !changed;

        }
        }
    
        class wipe_doMelt implements wipefun{
            public boolean
            invoke
        ( int   width,
          int   height,
          int   ticks )
        {
                
            //   int w2=2*width;
            //    int w3=3*width;
            //    int w4=4*width;

            int     dy;
            int     idx;
            
            // Indexex to short* ?! WTF... 
            int  ps;
            int  pd;
            
            short[] s;//=wipe_scr_end;
            short[] d=wipe_scr;
            
            boolean done = true;

            //width=2;

            while (ticks-->0)
            {
            for (int i=0;i<width;i++)
            {
                // Column won't start yet.
                if (y[i]<0)
                {
                y[i]++; done = false;
                }
                else if (y[i] < height)
                {
                    
                    
                dy = (y[i] < 16*Y_SCALE) ? y[i]+Y_SCALE : 8*Y_SCALE;
                if (y[i]+dy >= height) dy = height - y[i];
                ps = i*height+y[i];// &((short *)wipe_scr_end)[i*height+y[i]];
                pd = y[i]*width+i;//&((short *)wipe_scr)[y[i]*width+i];
                idx = 0;

                s=wipe_scr_end;
                

                
                // MAES: this part should draw the END SCREEN "behind" the melt.
                for (int j=dy;j>0;j--)
                {
                    d[pd+idx] = s[ps++];
                    idx += width;
                }
                y[i] += dy;
                s=wipe_scr_start;
                ps = i*height; //&((short *)wipe_scr_start)[i*height];
                pd = y[i]*width+i; //&((short *)wipe_scr)[y[i]*width+i];
                idx = 0;

                // This draws a column shifted by y[i]
                
                for (int j=height-y[i];j>0;j--)
                {
                    d[pd+idx] = s[ps++];
                    idx+=width;
                }
                done = false;
                }
            }
            }

            return done;

        }
        }
    }
    
    public static final class Indexed extends Wiper<byte[],byte[]>{

        public Indexed(DoomMain<byte[], byte[]> DC) {
            super(DC);
            wipes=new wipefun[]{
                    new wipe_initColorXForm(), new wipe_doColorXForm(), new wipe_exitColorXForm(),
                    new wipe_initMelt(), new wipe_doMelt(), new wipe_exitMelt()
                    };
        }
        
        /** Those guys sure have an obsession with shit...this is supposed to do some
         * lame-ass transpose.
         * 
         * @param array
         * @param width
         * @param height
         */
        
        protected final void
        shittyColMajorXform
        ( byte[]    array,
          int       width,
          int       height )
        {
            int     x;
            int     y;
            byte[]  dest;

            dest = new byte[width*height];

            for(y=0;y<height;y++)
            for(x=0;x<width;x++){
                dest[x*height+y] = array[y*width+x];
                //dest[(1+x)*height+y] = array[y*width+(1+x)];
            }
            System.arraycopy(dest, 0, array, 0, width*height);

            //Z_Free(dest);

        }
        
        class wipe_doColorXForm implements wipefun{
            
            public boolean
        invoke
        ( int   width,
          int   height,
          int   ticks )
        {
            boolean changed;
            byte[]   w=wipe_scr;
            byte[]   e=wipe_scr_end;
            int     newval;

            changed = false;
            int pw =0;// wipe_scr;
            int pe = 0; //wipe_scr_end;
            
            while (pw!=width*height)
            {
            if (w[pw] != e[pe])
            {
                if (w[pw] > e[pe])
                {
                newval = w[pw] - ticks;
                if (newval < e[pe])
                    w[pw] = e[pe];
                else
                    w[pw] = (byte) newval;
                changed = true;
                }
                else if (w[pw] < e[pe])
                {
                newval = w[pw] + ticks;
                if (newval > e[pe])
                    w[pw] = e[pe];
                else
                    w[pw] = (byte) newval;
                changed = true;
                }
            }
            pw++;
            pe++;
            }

            return !changed;

        }
        }
    
        class wipe_doMelt implements wipefun{
            public boolean
            invoke
        ( int   width,
          int   height,
          int   ticks )
        {
                
            //   int w2=2*width;
            //    int w3=3*width;
            //    int w4=4*width;

            int     dy;
            int     idx;
            
            // Indexex to short* ?! WTF... 
            int  ps;
            int  pd;
            
            byte[] s;//=wipe_scr_end;
            byte[] d=wipe_scr;
            
            boolean done = true;

            //width=2;

            while (ticks-->0)
            {
            for (int i=0;i<width;i++)
            {
                // Column won't start yet.
                if (y[i]<0)
                {
                y[i]++; done = false;
                }
                else if (y[i] < height)
                {
                    
                    
                dy = (y[i] < 16*Y_SCALE) ? y[i]+Y_SCALE : 8*Y_SCALE;
                if (y[i]+dy >= height) dy = height - y[i];
                ps = i*height+y[i];// &((short *)wipe_scr_end)[i*height+y[i]];
                pd = y[i]*width+i;//&((short *)wipe_scr)[y[i]*width+i];
                idx = 0;

                s=wipe_scr_end;
                

                
                // MAES: this part should draw the END SCREEN "behind" the melt.
                for (int j=dy;j>0;j--)
                {
                    d[pd+idx] = s[ps++];
                    idx += width;
                }
                y[i] += dy;
                s=wipe_scr_start;
                ps = i*height; //&((short *)wipe_scr_start)[i*height];
                pd = y[i]*width+i; //&((short *)wipe_scr)[y[i]*width+i];
                idx = 0;

                // This draws a column shifted by y[i]
                
                for (int j=height-y[i];j>0;j--)
                {
                    d[pd+idx] = s[ps++];
                    idx+=width;
                }
                done = false;
                }
            }
            }

            return done;

        }
        }
    }
    
    public static final class TrueColor extends Wiper<byte[],int[]>{

        public TrueColor(DoomMain<byte[], int[]> DC) {
            super(DC);
            wipes=new wipefun[]{
                    new wipe_initColorXForm(), new wipe_doColorXForm(), new wipe_exitColorXForm(),
                    new wipe_initMelt(), new wipe_doMelt(), new wipe_exitMelt()
                    };
        }
        
        /** Those guys sure have an obsession with shit...this is supposed to do some
         * lame-ass transpose.
         * 
         * @param array
         * @param width
         * @param height
         */
        
        protected final void
        shittyColMajorXform
        ( int[]    array,
          int       width,
          int       height )
        {
            int     x;
            int     y;
            int[]  dest;

            dest = new int[width*height];

            for(y=0;y<height;y++)
            for(x=0;x<width;x++){
                dest[x*height+y] = array[y*width+x];
                //dest[(1+x)*height+y] = array[y*width+(1+x)];
            }
            System.arraycopy(dest, 0, array, 0, width*height);

            //Z_Free(dest);

        }
        
        class wipe_doColorXForm implements wipefun{
            
            public boolean
        invoke
        ( int   width,
          int   height,
          int   ticks )
        {
            boolean changed;
            int[]   w=wipe_scr;
            int[]   e=wipe_scr_end;
            int     newval;

            changed = false;
            int pw =0;// wipe_scr;
            int pe = 0; //wipe_scr_end;
            
            while (pw!=width*height)
            {
            if (w[pw] != e[pe])
            {
                if (w[pw] > e[pe])
                {
                newval = w[pw] - ticks;
                if (newval < e[pe])
                    w[pw] = e[pe];
                else
                    w[pw] = (byte) newval;
                changed = true;
                }
                else if (w[pw] < e[pe])
                {
                newval = w[pw] + ticks;
                if (newval > e[pe])
                    w[pw] = e[pe];
                else
                    w[pw] = (byte) newval;
                changed = true;
                }
            }
            pw++;
            pe++;
            }

            return !changed;

        }
        }
    
        class wipe_doMelt implements wipefun{
            public boolean
            invoke
        ( int   width,
          int   height,
          int   ticks )
        {
                
            //   int w2=2*width;
            //    int w3=3*width;
            //    int w4=4*width;

            int     dy;
            int     idx;
            
            // Indexex to short* ?! WTF... 
            int  ps;
            int  pd;
            
            int[] s;//=wipe_scr_end;
            int[] d=wipe_scr;
            
            boolean done = true;

            //width=2;

            while (ticks-->0)
            {
            for (int i=0;i<width;i++)
            {
                // Column won't start yet.
                if (y[i]<0)
                {
                y[i]++; done = false;
                }
                else if (y[i] < height)
                {
                    
                    
                dy = (y[i] < 16*Y_SCALE) ? y[i]+Y_SCALE : 8*Y_SCALE;
                if (y[i]+dy >= height) dy = height - y[i];
                ps = i*height+y[i];// &((short *)wipe_scr_end)[i*height+y[i]];
                pd = y[i]*width+i;//&((short *)wipe_scr)[y[i]*width+i];
                idx = 0;

                s=wipe_scr_end;
                

                
                // MAES: this part should draw the END SCREEN "behind" the melt.
                for (int j=dy;j>0;j--)
                {
                    d[pd+idx] = s[ps++];
                    idx += width;
                }
                y[i] += dy;
                s=wipe_scr_start;
                ps = i*height; //&((short *)wipe_scr_start)[i*height];
                pd = y[i]*width+i; //&((short *)wipe_scr)[y[i]*width+i];
                idx = 0;

                // This draws a column shifted by y[i]
                
                for (int j=height-y[i];j>0;j--)
                {
                    d[pd+idx] = s[ps++];
                    idx+=width;
                }
                done = false;
                }
            }
            }

            return done;

        }
        }
    }
    
    protected abstract void shittyColMajorXform( V array, int       width,  int       height );
    
    class wipe_initColorXForm implements wipefun{
        public boolean
        invoke
    ( int   width,
      int   height,
      int   ticks )
    {
        System.arraycopy(wipe_scr_start,0 ,wipe_scr, 0,width*height);
        return false;
    }
    
    }
   
    
    
    class wipe_exitColorXForm implements wipefun{
    public boolean
    invoke
    ( int   width,
      int   height,
      int   ticks )
    {
        return false;
    }
    }


    protected int[] y;

    class wipe_initMelt implements wipefun{
        public boolean
        invoke
    ( int   width,
      int   height,
      int   ticks )
    {
        int i, r;
        
        // copy start screen to main screen
        System.arraycopy(wipe_scr_start, 0,wipe_scr, 0,width*height);
        
        // makes this wipe faster (in theory)
        // to have stuff in column-major format
        shittyColMajorXform(wipe_scr_start, width, height);
        shittyColMajorXform(wipe_scr_end, width, height);
        
        // setup initial column positions
        // (y<0 => not ready to scroll yet)
        y = new int[width];
        
        y[0] = -(RND.M_Random()%16);
        
        for (int j=1;j<Y_SCALE;j++){
        	y[j]=y[j-1];
        }
        
        for (i=Y_SCALE;i<width;i+=Y_SCALE)
        {
            r = (RND.M_Random()%3) - 1;
            y[i] = y[i-1] + r;
            if (y[i] > 0) y[i] = 0;
            else if (y[i] == -16) y[i] = -15;
            
        	for (int j=1;j<Y_SCALE;j++){
        		y[i+j]=y[i];
        	}
        
        }

        return false;
    }
    }

    
    
    class wipe_exitMelt implements wipefun{
        public boolean
        invoke
    ( int   width,
      int   height,
      int   ticks )

    {
        y=null; //Z_Free(y);
        return false;
    }
    }

    /** Sets "from" screen and stores it in "screen 2"*/
    
    @Override
    public boolean
    StartScreen
    ( int   x,
      int   y,
      int   width,
      int   height )
    {
        wipe_scr_start = V.getScreen(DoomVideoRenderer.SCREEN_WS);
        //  byte[] screen_zero=V.getScreen(0);
        VI.ReadScreen(wipe_scr_start);
        
        //System.arraycopy(screen_zero,0,wipe_scr_start, 0, SCREENWIDTH*SCREENHEIGHT);
        return false;
    }

    /** Sets "to" screen and stores it to "screen 3" */

    @Override
    public boolean
    EndScreen
    ( int   x,
      int   y,
      int   width,
      int   height )
    {
        // Set end screen to "screen 3" and copy visible screen to it.
        wipe_scr_end = V.getScreen(DoomVideoRenderer.SCREEN_WE);
        VI.ReadScreen(wipe_scr_end);
        
        // Restore starting screen.
        V screen_zero= V.getScreen(DoomVideoRenderer.SCREEN_FG);        
        System.arraycopy(wipe_scr_start,0,screen_zero, 0, SCREENWIDTH*SCREENHEIGHT);
        return false;
    }
    @Override
    public boolean
    ScreenWipe
    ( int   wipeno,
      int   x,
      int   y,
      int   width,
      int   height,
      int   ticks )
    {
        boolean rc;
  
        //System.out.println("Ticks do "+ticks);

        // initial stuff
        if (!go)
        {
        go = true;
        //wipe_scr = new byte[width*height]; // DEBUG
        wipe_scr = V.getScreen(DoomVideoRenderer.SCREEN_FG);
        // HOW'S THAT FOR A FUNCTION POINTER, BIATCH?!
        (wipes[wipeno*3]).invoke(width, height, ticks);
        }

        // do a piece of wipe-in
        V.MarkRect(0, 0, width, height);
        rc = (wipes[wipeno*3+1]).invoke(width, height, ticks);
        // V.DrawBlock(x, y, 0, width, height, wipe_scr); // DEBUG

        // final stuff
        if (rc)
        {
        go = false;
        (wipes[wipeno*3+2]).invoke(width, height, ticks);
        }

        return !go;

    }

    
    /** Interface for ASS-WIPING functions */
    
    interface wipefun{
        public boolean invoke(int   width,
                int   height,
                int   ticks );
    }
    
}
