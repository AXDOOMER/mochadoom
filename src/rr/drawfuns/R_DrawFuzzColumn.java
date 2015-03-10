package rr.drawfuns;

import i.IDoomSystem;

/**
	 * Framebuffer postprocessing. Creates a fuzzy image by copying pixels from
	 * adjacent ones to left and right. Used with an all black colormap, this
	 * could create the SHADOW effect, i.e. spectres and invisible players.
	 */

	public abstract class R_DrawFuzzColumn<T,V> extends DoomColumnFunction<T,V>  {
		
	    public R_DrawFuzzColumn(int SCREENWIDTH, int SCREENHEIGHT,
                int[] ylookup, int[] columnofs, ColVars<T,V> dcvars,
                V screen, IDoomSystem I,T BLURRY_MAP) {
	    	this(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I);
	    	this.BLURRY_MAP=BLURRY_MAP;
	    }
		
	    public R_DrawFuzzColumn(int SCREENWIDTH, int SCREENHEIGHT,
                int[] ylookup, int[] columnofs, ColVars<T,V> dcvars,
                V screen, IDoomSystem I) {
            super(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I);
        
            this.flags=DcFlags.FUZZY;
        
        FUZZOFF = SCREENWIDTH;
        
        // Recompute fuzz table
        
        fuzzoffset= new int[]{ FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF,
                FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF,
                FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF,
                -FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF,
                FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF,
                FUZZOFF, -FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF, -FUZZOFF,
                -FUZZOFF, -FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, FUZZOFF, -FUZZOFF,
                FUZZOFF, FUZZOFF, -FUZZOFF, FUZZOFF };
        
        FUZZTABLE=fuzzoffset.length;
		}
		
		protected int fuzzpos;
		
		protected final int FUZZTABLE;
		

        //
        // Spectre/Invisibility.
        //

		protected final int FUZZOFF;

		protected final int[] fuzzoffset;

        public static final class HiColor extends R_DrawFuzzColumn<byte[],short[]>{
        	
        	public HiColor(int SCREENWIDTH, int SCREENHEIGHT, int[] ylookup,
					int[] columnofs, ColVars<byte[], short[]> dcvars,
					short[] screen, IDoomSystem I) {
				super(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I);
				// TODO Auto-generated constructor stub
			}

			public void invoke() {
    			int count;
    			int dest;

    			// Adjust borders. Low...
    			if (dcvars.dc_yl == 0)
    			    dcvars.dc_yl = 1;

    			// .. and high.
    			if (dcvars.dc_yh == dcvars.viewheight - 1)
    			    dcvars.dc_yh = dcvars.viewheight - 2;

    			count = dcvars.dc_yh - dcvars.dc_yl;

    			// Zero length.
    			if (count < 0)
    				return;

    			if (RANGECHECK) {
    				    super.performRangeCheck();
    			}

    			// Does not work with blocky mode.
    			dest = computeScreenDest();

    			// Looks like an attempt at dithering,
    			// using the colormap #6 (of 0-31, a bit
    			// brighter than average).
    			if (count>4) // MAES: unroll by 4
    			do {
    				// Lookup framebuffer, and retrieve
    				// a pixel that is either one column
    				// left or right of the current one.
    				// Add index from colormap to index.

    				screen[dest] = fuzzMix(screen[dest
    										+ fuzzoffset[fuzzpos]]);

    				// Clamp table lookup index.
    				if (++fuzzpos == FUZZTABLE)
    					fuzzpos = 0;

    				dest += SCREENWIDTH;				
                    
    				screen[dest] = fuzzMix(screen[dest
    										+ fuzzoffset[fuzzpos]]);
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
                    dest += SCREENWIDTH;

    				screen[dest] = fuzzMix(screen[dest
    										+ fuzzoffset[fuzzpos]]);
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
                    dest += SCREENWIDTH;

    				screen[dest] = fuzzMix(screen[dest
    										+ fuzzoffset[fuzzpos]]);
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
                    dest += SCREENWIDTH;
    				
    			} while ((count-=4) > 4);
    			
    			if (count>0)
    	         do {
    	                screen[dest] = fuzzMix(screen[dest
    	                                              + fuzzoffset[fuzzpos]]);

    	                // Clamp table lookup index.
    	                if (++fuzzpos == FUZZTABLE)
    	                    fuzzpos = 0;

    	                dest += SCREENWIDTH;
    	            } while (count-- > 0);
    			
    		}
            
            private final short fuzzMix(short rgb){
                // super-fast half-brite trick
                // 3DEF and >> 1: ok hue, but too dark
                // 7BDE, no shift:  good compromise
                // 739C, no shift: results in too obvious tinting.         
                return (short) (rgb&0x7BDE);        	
            }
        }
        
        public static final class Indexed extends R_DrawFuzzColumn<byte[],byte[]>{
        
        public Indexed(int SCREENWIDTH, int SCREENHEIGHT, int[] ylookup,
					int[] columnofs, ColVars<byte[], byte[]> dcvars,
					byte[] screen, IDoomSystem I,byte[] BLURRY_MAP) {
				super(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I,BLURRY_MAP);
			}

		public void invoke() {
			int count;
			int dest;

			// Adjust borders. Low...
			if (dcvars.dc_yl == 0)
			    dcvars.dc_yl = 1;

			// .. and high.
			if (dcvars.dc_yh == dcvars.viewheight - 1)
			    dcvars.dc_yh = dcvars.viewheight - 2;

			count = dcvars.dc_yh - dcvars.dc_yl;

			// Zero length.
			if (count < 0)
				return;

			if (RANGECHECK) {
				    super.performRangeCheck();
			}

			// Does not work with blocky mode.
			dest = computeScreenDest();

			// Looks like an attempt at dithering,
			// using the colormap #6 (of 0-31, a bit
			// brighter than average).
			if (count>4) // MAES: unroll by 4
			do {
				// Lookup framebuffer, and retrieve
				// a pixel that is either one column
				// left or right of the current one.
				// Add index from colormap to index.
				screen[dest] = BLURRY_MAP[0x00FF & screen[dest
						+ fuzzoffset[fuzzpos]]];

				// Clamp table lookup index.
				if (++fuzzpos == FUZZTABLE)
					fuzzpos = 0;

				dest += SCREENWIDTH;				
                
				screen[dest] = 
                        BLURRY_MAP[0x00FF & screen[dest+ fuzzoffset[fuzzpos]]];
                if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
                dest += SCREENWIDTH;
                
                screen[dest] = 
                        BLURRY_MAP[0x00FF & screen[dest+ fuzzoffset[fuzzpos]]];
                if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
                dest += SCREENWIDTH;
                
                screen[dest] = 
                        BLURRY_MAP[0x00FF & screen[dest+ fuzzoffset[fuzzpos]]];
                if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
                dest += SCREENWIDTH;
				
			} while ((count-=4) > 4);
			
			if (count>0)
	         do {
	                // Lookup framebuffer, and retrieve
	                // a pixel that is either one column
	                // left or right of the current one.
	                // Add index from colormap to index.
	                screen[dest] = BLURRY_MAP[0x00FF & screen[dest
	                        + fuzzoffset[fuzzpos]]];

	                // Clamp table lookup index.
	                if (++fuzzpos == FUZZTABLE)
	                    fuzzpos = 0;

	                dest += SCREENWIDTH;
	            } while (count-- > 0);
			
		}
        }

public static final class TrueColor extends R_DrawFuzzColumn<byte[],int[]>{
            
            public TrueColor(int SCREENWIDTH, int SCREENHEIGHT, int[] ylookup,
                    int[] columnofs, ColVars<byte[], int[]> dcvars,
                    int[] screen, IDoomSystem I) {
                super(SCREENWIDTH, SCREENHEIGHT, ylookup, columnofs, dcvars, screen, I);
            }

            public void invoke() {
                int count;
                int dest;

                // Adjust borders. Low...
                if (dcvars.dc_yl == 0)
                    dcvars.dc_yl = 1;

                // .. and high.
                if (dcvars.dc_yh == dcvars.viewheight - 1)
                    dcvars.dc_yh = dcvars.viewheight - 2;

                count = dcvars.dc_yh - dcvars.dc_yl;

                // Zero length.
                if (count < 0)
                    return;

                if (RANGECHECK) {
                        super.performRangeCheck();
                }

                // Does not work with blocky mode.
                dest = computeScreenDest();

                // Looks like an attempt at dithering,
                // using the colormap #6 (of 0-31, a bit
                // brighter than average).
                if (count>4) // MAES: unroll by 4
                do {
                    // Lookup framebuffer, and retrieve
                    // a pixel that is either one column
                    // left or right of the current one.
                    // Add index from colormap to index.

                    screen[dest] = fuzzMix(screen[dest
                                            + fuzzoffset[fuzzpos]]);

                    // Clamp table lookup index.
                    if (++fuzzpos == FUZZTABLE)
                        fuzzpos = 0;

                    dest += SCREENWIDTH;                
                    
                    screen[dest] = fuzzMix(screen[dest
                                            + fuzzoffset[fuzzpos]]);
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
                    dest += SCREENWIDTH;

                    screen[dest] = fuzzMix(screen[dest
                                            + fuzzoffset[fuzzpos]]);
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
                    dest += SCREENWIDTH;

                    screen[dest] = fuzzMix(screen[dest
                                            + fuzzoffset[fuzzpos]]);
                    if (++fuzzpos == FUZZTABLE) fuzzpos = 0;
                    dest += SCREENWIDTH;
                    
                } while ((count-=4) > 4);
                
                if (count>0)
                 do {
                        screen[dest] = fuzzMix(screen[dest
                                                      + fuzzoffset[fuzzpos]]);

                        // Clamp table lookup index.
                        if (++fuzzpos == FUZZTABLE)
                            fuzzpos = 0;

                        dest += SCREENWIDTH;
                    } while (count-- > 0);
                
            }
            
            private final int fuzzMix(int rgb){
                // Proper half-brite alpha!
                return rgb&0x10FFFFFF;
            }
        }
        
	}