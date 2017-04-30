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

import static data.Limits.MAXRADIUS;
import doom.SourceCode;
import static doom.SourceCode.P_Map.P_CheckPosition;
import static m.BBox.BOXBOTTOM;
import static m.BBox.BOXLEFT;
import static m.BBox.BOXRIGHT;
import static m.BBox.BOXTOP;
import static p.mobj_t.MF_NOCLIP;
import rr.line_t;
import rr.subsector_t;
import static utils.C2JUtils.eval;

interface ActionsClipping extends ActionsThings, ActionsUtility {
    //
    // MOVEMENT CLIPPING
    //
    /**
     * P_CheckPosition This is purely informative, nothing is modified (except things picked up).
     *
     * in: a mobj_t (can be valid or invalid) a position to be checked (doesn't need to be related to the mobj_t.x,y)
     *
     * during: special things are touched if MF_PICKUP early out on solid lines?
     *
     * out: newsubsec floorz ceilingz tmdropoffz the lowest point contacted (monsters won't move to a dropoff)
     * speciallines[] numspeciallines
     *
     * @param thing
     * @param x fixed_t
     * @param y fixed_t
     */
    @SourceCode.P_Map.C(P_CheckPosition)
    default boolean CheckPosition(mobj_t thing, @SourceCode.fixed_t int x, @SourceCode.fixed_t int y) {
        final Actions.Registry obs = obs();
        int xl;
        int xh;
        int yl;
        int yh;
        int bx;
        int by;
        subsector_t newsubsec;

        obs.tmthing = thing;
        obs.tmflags = thing.flags;

        obs.tmx = x;
        obs.tmy = y;

        obs.tmbbox[BOXTOP] = y + obs.tmthing.radius;
        obs.tmbbox[BOXBOTTOM] = y - obs.tmthing.radius;
        obs.tmbbox[BOXRIGHT] = x + obs.tmthing.radius;
        obs.tmbbox[BOXLEFT] = x - obs.tmthing.radius;

        R_PointInSubsector: {
            newsubsec = obs.DOOM.levelLoader.PointInSubsector(x, y);
        }
        obs.ceilingline = null;

        // The base floor / ceiling is from the subsector
        // that contains the point.
        // Any contacted lines the step closer together
        // will adjust them.
        obs.tmfloorz = obs.tmdropoffz = newsubsec.sector.floorheight;
        obs.tmceilingz = newsubsec.sector.ceilingheight;

        obs.DOOM.sceneRenderer.increaseValidCount(1);
        obs.numspechit = 0;

        if (eval(obs.tmflags & MF_NOCLIP)) {
            return true;
        }

        // Check things first, possibly picking things up.
        // The bounding box is extended by MAXRADIUS
        // because mobj_ts are grouped into mapblocks
        // based on their origin point, and can overlap
        // into adjacent blocks by up to MAXRADIUS units.
        xl = obs.DOOM.levelLoader.getSafeBlockX(obs.tmbbox[BOXLEFT] - obs.DOOM.levelLoader.bmaporgx - MAXRADIUS);
        xh = obs.DOOM.levelLoader.getSafeBlockX(obs.tmbbox[BOXRIGHT] - obs.DOOM.levelLoader.bmaporgx + MAXRADIUS);
        yl = obs.DOOM.levelLoader.getSafeBlockY(obs.tmbbox[BOXBOTTOM] - obs.DOOM.levelLoader.bmaporgy - MAXRADIUS);
        yh = obs.DOOM.levelLoader.getSafeBlockY(obs.tmbbox[BOXTOP] - obs.DOOM.levelLoader.bmaporgy + MAXRADIUS);

        for (bx = xl; bx <= xh; bx++) {
            for (by = yl; by <= yh; by++) {
                P_BlockThingsIterator: {
                    if (!this.BlockThingsIterator(bx, by, this::CheckThing)) {
                        return false;
                    }
                }
            }
        }

        // check lines
        xl = obs.DOOM.levelLoader.getSafeBlockX(obs.tmbbox[BOXLEFT] - obs.DOOM.levelLoader.bmaporgx);
        xh = obs.DOOM.levelLoader.getSafeBlockX(obs.tmbbox[BOXRIGHT] - obs.DOOM.levelLoader.bmaporgx);
        yl = obs.DOOM.levelLoader.getSafeBlockY(obs.tmbbox[BOXBOTTOM] - obs.DOOM.levelLoader.bmaporgy);
        yh = obs.DOOM.levelLoader.getSafeBlockY(obs.tmbbox[BOXTOP] - obs.DOOM.levelLoader.bmaporgy);

        // Maes's quick and dirty blockmap extension hack
        // E.g. for an extension of 511 blocks, max negative is -1.
        // A full 512x512 blockmap doesn't have negative indexes.
        if (xl <= obs.DOOM.levelLoader.blockmapxneg) {
            xl = 0x1FF & xl;         // Broke width boundary
        }
        if (xh <= obs.DOOM.levelLoader.blockmapxneg) {
            xh = 0x1FF & xh;    // Broke width boundary
        }
        if (yl <= obs.DOOM.levelLoader.blockmapyneg) {
            yl = 0x1FF & yl;        // Broke height boundary
        }
        if (yh <= obs.DOOM.levelLoader.blockmapyneg) {
            yh = 0x1FF & yh;   // Broke height boundary     
        }
        for (bx = xl; bx <= xh; bx++) {
            for (by = yl; by <= yh; by++) {
                P_BlockLinesIterator: {
                    if (!this.BlockLinesIterator(bx, by, this::CheckLine)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }
    
    //
    // P_ThingHeightClip
    // Takes a valid thing and adjusts the thing.floorz,
    // thing.ceilingz, and possibly thing.z.
    // This is called for all nearby monsters
    // whenever a sector changes height.
    // If the thing doesn't fit,
    // the z will be set to the lowest value
    // and false will be returned.
    //
    default boolean ThingHeightClip(mobj_t thing) {
        final Actions.Registry obs = obs();
        boolean onfloor;

        onfloor = (thing.z == thing.floorz);

        this.CheckPosition(thing, thing.x, thing.y);
        // what about stranding a monster partially off an edge?

        thing.floorz = obs.tmfloorz;
        thing.ceilingz = obs.tmceilingz;

        if (onfloor) {
            // walking monsters rise and fall with the floor
            thing.z = thing.floorz;
        } else {
            // don't adjust a floating monster unless forced to
            if (thing.z + thing.height > thing.ceilingz) {
                thing.z = thing.ceilingz - thing.height;
            }
        }

        return thing.ceilingz - thing.floorz >= thing.height;
    }
    
    default boolean isblocking(intercept_t in, line_t li) {
        final Actions.Registry obs = obs();
        // the line does block movement,
        // see if it is closer than best so far

        if (in.frac < obs.bestslidefrac) {
            obs.secondslidefrac = obs.bestslidefrac;
            obs.secondslideline = obs.bestslideline;
            obs.bestslidefrac = in.frac;
            obs.bestslideline = li;
        }

        return false;   // stop
    }
}
