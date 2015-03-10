package rr.drawfuns;

import i.IDoomSystem;

/**
	 * Draws the actual span.
	 * 
	 * ds_frac, ds_yfrac, ds_x2, ds_x1, ds_xstep and ds_ystep must be set.
	 * 
	 */

	public final class R_DrawSpan extends DoomSpanFunction<byte[],short[]> {

        public R_DrawSpan(int sCREENWIDTH, int sCREENHEIGHT, int[] ylookup,
            int[] columnofs, SpanVars<byte[],short[]> dsvars,short[] screen,
            IDoomSystem I) {
        super(sCREENWIDTH, sCREENHEIGHT, ylookup, columnofs, dsvars, screen, I);
        }

        public void invoke() {

			int f_xfrac; // fixed_t
			int f_yfrac; // fixed_t
			int dest,count,spot;
			final short[] ds_colormap=dsvars.ds_colormap;
			final byte[] ds_source=dsvars.ds_source;

			// System.out.println("R_DrawSpan: "+ds_x1+" to "+ds_x2+" at "+
			// ds_y);

			if (RANGECHECK) {
			    doRangeCheck();
				// dscount++;
			}

			f_xfrac = dsvars.ds_xfrac;
			f_yfrac = dsvars.ds_yfrac;

			dest = ylookup[dsvars.ds_y] + columnofs[dsvars.ds_x1];

			// We do not check for zero spans here?
			count = dsvars.ds_x2 - dsvars.ds_x1;

			do {
				// Current texture index in u,v.
				spot = ((f_yfrac >> (16 - 6)) & (63 * 64))
						+ ((f_xfrac >> 16) & 63);

				// Lookup pixel from flat texture tile,
				// re-index using light/colormap.
				screen[dest++] = ds_colormap[0x00FF & ds_source[spot]];

				// Next step in u,v.
				f_xfrac += dsvars.ds_xstep;
				f_yfrac += dsvars.ds_ystep;

			} while (count-- > 0);
		}
	}