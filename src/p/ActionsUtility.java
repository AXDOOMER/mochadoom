/*
 * Copyright (C) 1993-1996 by id Software, Inc.
 * Copyright (C) 2017 Good Sign
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package p;

import doom.SourceCode;
import static doom.SourceCode.P_Map.PIT_CheckLine;
import doom.SourceCode.P_MapUtl;
import static doom.SourceCode.P_MapUtl.P_BlockLinesIterator;
import static doom.SourceCode.P_MapUtl.P_BlockThingsIterator;
import java.util.function.Predicate;
import static m.BBox.BOXBOTTOM;
import static m.BBox.BOXLEFT;
import static m.BBox.BOXRIGHT;
import static m.BBox.BOXTOP;

import p.ActionSystem.AbstractCommand;
import p.ActionSystem.Observer;
import static p.mobj_t.MF_MISSILE;
import rr.SceneRenderer;
import rr.line_t;
import static rr.line_t.ML_BLOCKING;
import static rr.line_t.ML_BLOCKMONSTERS;
import utils.C2JUtils;
import static utils.C2JUtils.eval;

interface ActionsUtility<R extends Actions.Registry & AbstractCommand<R>> extends Observer<R> {
    //
    //P_BlockThingsIterator
    //
    @SourceCode.Exact
    @P_MapUtl.C(P_BlockThingsIterator)
    default boolean BlockThingsIterator(int x, int y, Predicate<mobj_t> func) {
        final AbstractLevelLoader ll = obs().DOOM.levelLoader;
        mobj_t mobj;

        if (x < 0 || y < 0 || x >= ll.bmapwidth || y >= ll.bmapheight) {
            return true;
        }

        for (mobj = ll.blocklinks[y * ll.bmapwidth + x]; mobj != null; mobj = (mobj_t) mobj.bnext) {
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
    @P_MapUtl.C(P_BlockLinesIterator)
    default boolean BlockLinesIterator(int x, int y, Predicate<line_t> func) {
        final AbstractLevelLoader ll = obs().DOOM.levelLoader;
        final SceneRenderer<?, ?> sr = obs().DOOM.sceneRenderer;
        int offset;
        int lineinblock;
        line_t ld;

        if (x < 0 || y < 0 || x >= ll.bmapwidth || y >= ll.bmapheight) {
            return true;
        }

        // This gives us the index to look up (in blockmap)
        offset = y * ll.bmapwidth + x;

        // The index contains yet another offset, but this time 
        offset = ll.blockmap[offset];

        // MAES: blockmap terminating marker is always -1
        @SourceCode.Compatible("validcount")
        final int validcount = sr.getValidCount();

        // [SYNC ISSUE]: don't skip offset+1 :-/
        for (
            @SourceCode.Compatible("list = blockmaplump+offset ; *list != -1 ; list++")
            int list = offset; (lineinblock = ll.blockmap[list]) != -1; list++
        ) {
            ld = ll.lines[lineinblock];
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
        final Actions.Registry obs = obs();
        obs.spechit = C2JUtils.resize(obs.spechit[0], obs.spechit, obs.spechit.length * 2);
    }
    
    /**
     * PIT_CheckLine Adjusts tmfloorz and tmceilingz as lines are contacted
     *
     */
    @SourceCode.P_Map.C(PIT_CheckLine) default boolean CheckLine(line_t ld) {
        final Actions.Registry obs = obs();
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
