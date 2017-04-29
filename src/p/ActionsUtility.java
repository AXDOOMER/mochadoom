package p;

import doom.SourceCode;
import static doom.SourceCode.P_Map.PIT_CheckLine;
import static doom.SourceCode.P_MapUtl.P_BlockLinesIterator;
import static doom.SourceCode.P_MapUtl.P_BlockThingsIterator;
import java.util.function.Predicate;
import static m.BBox.BOXBOTTOM;
import static m.BBox.BOXLEFT;
import static m.BBox.BOXRIGHT;
import static m.BBox.BOXTOP;
import static p.mobj_t.MF_MISSILE;
import rr.line_t;
import static rr.line_t.ML_BLOCKING;
import static rr.line_t.ML_BLOCKMONSTERS;
import utils.C2JUtils;
import static utils.C2JUtils.eval;

public interface ActionsUtility<T, V> extends ActionsRegistry.Observer<T, V> {
    //
    //P_BlockThingsIterator
    //
    @SourceCode.P_MapUtl.C(P_BlockThingsIterator) default boolean BlockThingsIterator(int x, int y, Predicate<mobj_t> func) {
        final ActionsRegistry<T, V> obs = obs();
        mobj_t mobj;

        if (x < 0 || y < 0 || x >= obs.DOOM.levelLoader.bmapwidth || y >= obs.DOOM.levelLoader.bmapheight) {
            return true;
        }

        for (mobj = obs.DOOM.levelLoader.blocklinks[y * obs.DOOM.levelLoader.bmapwidth + x]; mobj != null;
                mobj = (mobj_t) mobj.bnext) {
            if (!func.test(mobj)) {
                return false;
            }
        }
        return true;
    }

    //
    // SECTOR HEIGHT CHANGING
    // After modifying a sectors floor or ceiling height,
    // call this routine to adjust the positions
    // of all things that touch the sector.
    //
    // If anything doesn't fit anymore, true will be returned.
    // If crunch is true, they will take damage
    //  as they are being crushed.
    // If Crunch is false, you should set the sector height back
    //  the way it was and call P_ChangeSector again
    //  to undo the changes.
    //

    /**
     * P_BlockLinesIterator The validcount flags are used to avoid checking lines that are marked in multiple mapblocks,
     * so increment validcount before the first call to P_BlockLinesIterator, then make one or more calls to it.
     */
    @SourceCode.P_MapUtl.C(P_BlockLinesIterator) default boolean BlockLinesIterator(int x, int y, Predicate<line_t> func) {
        final ActionsRegistry<T, V> obs = obs();
        int offset;
        int lineinblock;
        line_t ld;

        if (x < 0
                || y < 0
                || x >= obs.DOOM.levelLoader.bmapwidth
                || y >= obs.DOOM.levelLoader.bmapheight) {
            return true;
        }

        // This gives us the index to look up (in blockmap)
        offset = y * obs.DOOM.levelLoader.bmapwidth + x;

        // The index contains yet another offset, but this time 
        offset = obs.DOOM.levelLoader.blockmap[offset];

        // MAES: blockmap terminating marker is always -1
        final int validcount = obs.DOOM.sceneRenderer.getValidCount();

        // [SYNC ISSUE]: don't skip offset+1 :-/
        for (int list = offset; (lineinblock = obs.DOOM.levelLoader.blockmap[list]) != -1; list++) {
            ld = obs.DOOM.levelLoader.lines[lineinblock];
            //System.out.println(ld);
            if (ld.validcount == validcount) {
                continue;   // line has already been checked
            }
            ld.validcount = validcount;
            if (!func.test(ld)) {
                return false;
            }
        }
        return true;    // everything was checked
    }

    // keep track of the line that lowers the ceiling,
    // so missiles don't explode against sky hack walls

    default void ResizeSpechits() {
        final ActionsRegistry<T, V> obs = obs();
        obs.spechit = C2JUtils.resize(obs.spechit[0], obs.spechit, obs.spechit.length * 2);
    }
    
    /**
     * PIT_CheckLine Adjusts tmfloorz and tmceilingz as lines are contacted
     *
     */
    @SourceCode.P_Map.C(PIT_CheckLine) default boolean CheckLine(line_t ld) {
        final ActionsRegistry<T, V> obs = obs();
        if (obs.tmbbox[BOXRIGHT] <= ld.bbox[BOXLEFT]
                || obs.tmbbox[BOXLEFT] >= ld.bbox[BOXRIGHT]
                || obs.tmbbox[BOXTOP] <= ld.bbox[BOXBOTTOM]
                || obs.tmbbox[BOXBOTTOM] >= ld.bbox[BOXTOP]) {
            return true;
        }

        if (ld.BoxOnLineSide(obs.tmbbox) != -1) {
            return true;
        }

        // A line has been hit
        // The moving thing's destination position will cross
        // the given line.
        // If this should not be allowed, return false.
        // If the line is special, keep track of it
        // to process later if the move is proven ok.
        // NOTE: specials are NOT sorted by order,
        // so two special lines that are only 8 pixels apart
        // could be crossed in either order.
        if (ld.backsector == null) {
            return false;       // one sided line
        }
        if (!eval(obs.tmthing.flags & MF_MISSILE)) {
            if (eval(ld.flags & ML_BLOCKING)) {
                return false;   // explicitly blocking everything
            }
            if ((obs.tmthing.player == null) && eval(ld.flags & ML_BLOCKMONSTERS)) {
                return false;   // block monsters only
            }
        }

        // set openrange, opentop, openbottom
        obs.LineOpening(ld);

        // adjust floor / ceiling heights
        if (obs.opentop < obs.tmceilingz) {
            obs.tmceilingz = obs.opentop;
            obs.ceilingline = ld;
        }

        if (obs.openbottom > obs.tmfloorz) {
            obs.tmfloorz = obs.openbottom;
        }

        if (obs.lowfloor < obs.tmdropoffz) {
            obs.tmdropoffz = obs.lowfloor;
        }

        // if contacted a special line, add it to the list
        if (ld.special != 0) {
            obs.spechit[obs.numspechit] = ld;
            obs.numspechit++;
            // Let's be proactive about this.
            if (obs.numspechit >= obs.spechit.length) {
                this.ResizeSpechits();
            }
        }

        return true;
    };

}
