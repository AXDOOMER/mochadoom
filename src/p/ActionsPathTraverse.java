package p;

import static data.Defines.MAPBLOCKSHIFT;
import static data.Defines.MAPBLOCKSIZE;
import static data.Defines.MAPBTOFRAC;
import static data.Defines.PT_ADDLINES;
import static data.Defines.PT_ADDTHINGS;
import static data.Defines.PT_EARLYOUT;
import static data.Limits.MAXINT;
import doom.SourceCode;
import static doom.SourceCode.P_MapUtl.P_PathTraverse;
import java.util.function.Predicate;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedDiv;
import static m.fixed_t.FixedMul;
import static p.MapUtils.InterceptVector;
import rr.line_t;
import static utils.C2JUtils.eval;

public interface ActionsPathTraverse<T, V> extends ActionsUtility<T, V> {
    /**
     * P_PathTraverse Traces a line from x1,y1 to x2,y2, calling the traverser function for each. Returns true if the
     * traverser function returns true for all lines.
     */
    @SourceCode.P_MapUtl.C(P_PathTraverse) default boolean PathTraverse(int x1, int y1, int x2, int y2, int flags, Predicate<intercept_t> trav) {
        final ActionsRegistry<T, V> obs = obs();
        // System.out.println("Pathtraverse "+x1+" , " +y1+" to "+x2 +" , "
        // +y2);
        final int xt1, yt1;
        final int xt2, yt2;
        final long _x1, _x2, _y1, _y2;
        final int mapx1, mapy1;
        final int xstep, ystep;

        int partial;

        int xintercept, yintercept;

        int mapx;
        int mapy;

        int mapxstep;
        int mapystep;

        int count;

        obs.earlyout = eval(flags & PT_EARLYOUT);

        obs.DOOM.sceneRenderer.increaseValidCount(1);
        obs.intercept_p = 0;

        if (((x1 - obs.DOOM.levelLoader.bmaporgx) & (MAPBLOCKSIZE - 1)) == 0) {
            x1 += FRACUNIT; // don't side exactly on a line
        }
        if (((y1 - obs.DOOM.levelLoader.bmaporgy) & (MAPBLOCKSIZE - 1)) == 0) {
            y1 += FRACUNIT; // don't side exactly on a line
        }
        obs.trace.x = x1;
        obs.trace.y = y1;
        obs.trace.dx = x2 - x1;
        obs.trace.dy = y2 - y1;

        // Code developed in common with entryway
        // for prBoom+
        _x1 = (long) x1 - obs.DOOM.levelLoader.bmaporgx;
        _y1 = (long) y1 - obs.DOOM.levelLoader.bmaporgy;
        xt1 = (int) (_x1 >> MAPBLOCKSHIFT);
        yt1 = (int) (_y1 >> MAPBLOCKSHIFT);

        mapx1 = (int) (_x1 >> MAPBTOFRAC);
        mapy1 = (int) (_y1 >> MAPBTOFRAC);

        _x2 = (long) x2 - obs.DOOM.levelLoader.bmaporgx;
        _y2 = (long) y2 - obs.DOOM.levelLoader.bmaporgy;
        xt2 = (int) (_x2 >> MAPBLOCKSHIFT);
        yt2 = (int) (_y2 >> MAPBLOCKSHIFT);

        x1 -= obs.DOOM.levelLoader.bmaporgx;
        y1 -= obs.DOOM.levelLoader.bmaporgy;
        x2 -= obs.DOOM.levelLoader.bmaporgx;
        y2 -= obs.DOOM.levelLoader.bmaporgy;

        if (xt2 > xt1) {
            mapxstep = 1;
            partial = FRACUNIT - (mapx1 & (FRACUNIT - 1));
            ystep = FixedDiv(y2 - y1, Math.abs(x2 - x1));
        } else if (xt2 < xt1) {
            mapxstep = -1;
            partial = mapx1 & (FRACUNIT - 1);
            ystep = FixedDiv(y2 - y1, Math.abs(x2 - x1));
        } else {
            mapxstep = 0;
            partial = FRACUNIT;
            ystep = 256 * FRACUNIT;
        }

        yintercept = mapy1 + FixedMul(partial, ystep);

        if (yt2 > yt1) {
            mapystep = 1;
            partial = FRACUNIT - (mapy1 & (FRACUNIT - 1));
            xstep = FixedDiv(x2 - x1, Math.abs(y2 - y1));
        } else if (yt2 < yt1) {
            mapystep = -1;
            partial = mapy1 & (FRACUNIT - 1);
            xstep = FixedDiv(x2 - x1, Math.abs(y2 - y1));
        } else {
            mapystep = 0;
            partial = FRACUNIT;
            xstep = 256 * FRACUNIT;
        }
        xintercept = mapx1 + FixedMul(partial, xstep);

        // Step through map blocks.
        // Count is present to prevent a round off error
        // from skipping the break.
        mapx = xt1;
        mapy = yt1;

        for (count = 0; count < 64; count++) {
            if (eval(flags & PT_ADDLINES)) {
                if (!this.BlockLinesIterator(mapx, mapy, this::AddLineIntercepts)) {
                    return false;   // early out
                }
            }

            if (eval(flags & PT_ADDTHINGS)) {
                if (!this.BlockThingsIterator(mapx, mapy, this::AddThingIntercepts)) {
                    return false;   // early out
                }
            }

            if (mapx == xt2
                    && mapy == yt2) {
                break;
            }

            boolean changeX = (yintercept >> FRACBITS) == mapy;
            boolean changeY = (xintercept >> FRACBITS) == mapx;
            if (changeX) {
                yintercept += ystep;
                mapx += mapxstep;
            } else //[MAES]: this fixed sync issues. Lookup linuxdoom
            if (changeY) {
                xintercept += xstep;
                mapy += mapystep;
            }

        }
        // go through the sorted list
        //System.out.println("Some intercepts found");
        return TraverseIntercept(trav, FRACUNIT);
    } // end method

    default boolean AddLineIntercepts(line_t ld) {
        final ActionsRegistry<T, V> obs = obs();

        boolean s1;
        boolean s2;
        @SourceCode.fixed_t int frac;
        
        // avoid precision problems with two routines
        if (obs.trace.dx > FRACUNIT * 16 || obs.trace.dy > FRACUNIT * 16
            || obs.trace.dx < -FRACUNIT * 16 || obs.trace.dy < -FRACUNIT * 16) {
            s1 = obs.trace.PointOnDivlineSide(ld.v1x, ld.v1y);
            s2 = obs.trace.PointOnDivlineSide(ld.v2x, ld.v2y);
            //s1 = obs.trace.DivlineSide(ld.v1x, ld.v1.y);
            //s2 = obs.trace.DivlineSide(ld.v2x, ld.v2y);
        } else {
            s1 = ld.PointOnLineSide(obs.trace.x, obs.trace.y);
            s2 = ld.PointOnLineSide(obs.trace.x + obs.trace.dx, obs.trace.y + obs.trace.dy);
            //s1 = new divline_t(ld).DivlineSide(obs.trace.x, obs.trace.y);
            //s2 = new divline_t(ld).DivlineSide(obs.trace.x + obs.trace.dx, obs.trace.y + obs.trace.dy);
        }

        if (s1 == s2) {
            return true; // line isn't crossed
        }
        // hit the line
        obs.addLineDivLine.MakeDivline(ld);
        frac = InterceptVector(obs.trace, obs.addLineDivLine);

        if (frac < 0) {
            return true; // behind source
        }
        // try to early out the check
        if (obs.earlyout && frac < FRACUNIT && ld.backsector == null) {
            return false; // stop checking
        }

        // "create" a new intercept in the static intercept pool.
        if (obs.intercept_p >= obs.intercepts.length) {
            obs.ResizeIntercepts();
        }

        obs.intercepts[obs.intercept_p].frac = frac;
        obs.intercepts[obs.intercept_p].isaline = true;
        obs.intercepts[obs.intercept_p].line = ld;
        obs.intercept_p++;

        return true; // continue
    };

    default boolean AddThingIntercepts(mobj_t thing) {
        final ActionsRegistry<T, V> obs = obs();
        @SourceCode.fixed_t int x1, y1, x2, y2;
        boolean s1, s2;
        boolean tracepositive;
        @SourceCode.fixed_t int frac;

        tracepositive = (obs.trace.dx ^ obs.trace.dy) > 0;

        // check a corner to corner crossection for hit
        if (tracepositive) {
            x1 = thing.x - thing.radius;
            y1 = thing.y + thing.radius;

            x2 = thing.x + thing.radius;
            y2 = thing.y - thing.radius;
        } else {
            x1 = thing.x - thing.radius;
            y1 = thing.y - thing.radius;

            x2 = thing.x + thing.radius;
            y2 = thing.y + thing.radius;
        }

        s1 = obs.trace.PointOnDivlineSide(x1, y1);
        s2 = obs.trace.PointOnDivlineSide(x2, y2);

        if (s1 == s2) {
            return true; // line isn't crossed
        }
        
        obs.thingInterceptDivLine.x = x1;
        obs.thingInterceptDivLine.y = y1;
        obs.thingInterceptDivLine.dx = x2 - x1;
        obs.thingInterceptDivLine.dy = y2 - y1;

        frac = InterceptVector(obs.trace, obs.thingInterceptDivLine);

        if (frac < 0) {
            return true; // behind source
        }
        
        // "create" a new intercept in the static intercept pool.
        if (obs.intercept_p >= obs.intercepts.length) {
            obs.ResizeIntercepts();
        }
        
        obs.intercepts[obs.intercept_p].frac = frac;
        obs.intercepts[obs.intercept_p].isaline = false;
        obs.intercepts[obs.intercept_p].thing = thing;
        obs.intercept_p++;

        return true; // keep going
    };

    //
    //P_TraverseIntercepts
    //Returns true if the traverser function returns true
    //for all lines.
    //
    default boolean TraverseIntercept(Predicate<intercept_t> func, int maxfrac) {
        final ActionsRegistry<T, V> obs = obs();
        int count;
        @SourceCode.fixed_t int dist;
        intercept_t in = null;  // shut up compiler warning

        count = obs.intercept_p;

        while (count-- > 0) {
            dist = MAXINT;
            for (int scan = 0; scan < obs.intercept_p; scan++) {
                if (obs.intercepts[scan].frac < dist) {
                    dist = obs.intercepts[scan].frac;
                    in = obs.intercepts[scan];
                }
            }

            if (dist > maxfrac) {
                return true;    // checked everything in range      
            }
            /*  // UNUSED
            {
            // don't check these yet, there may be others inserted
            in = scan = intercepts;
            for ( scan = intercepts ; scan<intercept_p ; scan++)
                if (scan.frac > maxfrac)
                *in++ = *scan;
            intercept_p = in;
            return false;
            }
             */

            if (!func.test(in)) {
                return false;   // don't bother going farther
            }
            in.frac = MAXINT;
        }

        return true;        // everything was traversed
    }
}
