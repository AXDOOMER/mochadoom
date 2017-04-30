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
import data.Tables;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import data.mobjtype_t;
import data.sounds;
import doom.SourceCode;
import doom.thinker_t;
import static m.BBox.BOXBOTTOM;
import static m.BBox.BOXLEFT;
import static m.BBox.BOXRIGHT;
import static m.BBox.BOXTOP;
import static p.mobj_t.MF_MISSILE;
import rr.line_t;
import rr.sector_t;
import rr.subsector_t;

interface ActionsTeleportation extends ActionsThings, ActionsUtility {
    //
    // TELEPORTATION
    //
    default int Teleport(line_t line, int side, mobj_t thing) {
        final Actions.Registry obs = obs();
        int i;
        int tag;
        mobj_t m;
        mobj_t fog;
        int an;
        thinker_t thinker;
        sector_t sector;
        @SourceCode.fixed_t int oldx, oldy, oldz;

        // don't teleport missiles
        if ((thing.flags & MF_MISSILE) != 0) {
            return 0;
        }

        // Don't teleport if hit back of line,
        //  so you can get out of teleporter.
        if (side == 1) {
            return 0;
        }

        tag = line.tag;
        for (i = 0; i < obs.DOOM.levelLoader.numsectors; i++) {
            if (obs.DOOM.levelLoader.sectors[i].tag == tag) {
                //thinker = thinkercap.next;
                for (thinker = obs.thinkercap.next; thinker != obs.thinkercap; thinker = thinker.next) {
                    // not a mobj
                    if (thinker.thinkerFunction != ActiveStates.P_MobjThinker) {
                        continue;
                    }

                    m = (mobj_t) thinker;

                    // not a teleportman
                    if (m.type != mobjtype_t.MT_TELEPORTMAN) {
                        continue;
                    }

                    sector = m.subsector.sector;
                    // wrong sector
                    if (sector.id != i) {
                        continue;
                    }

                    oldx = thing.x;
                    oldy = thing.y;
                    oldz = thing.z;

                    if (!this.TeleportMove(thing, m.x, m.y)) {
                        return 0;
                    }

                    thing.z = thing.floorz;  //fixme: not needed?
                    if (thing.player != null) {
                        thing.player.viewz = thing.z + thing.player.viewheight;
                        thing.player.lookdir = 0; // Reset lookdir
                    }

                    // spawn teleport fog at source and destination
                    fog = this.SpawnMobj(oldx, oldy, oldz, mobjtype_t.MT_TFOG);
                    obs.DOOM.doomSound.StartSound(fog, sounds.sfxenum_t.sfx_telept);
                    an = Tables.toBAMIndex(m.angle);
                    fog = this.SpawnMobj(m.x + 20 * finecosine[an], m.y + 20 * finesine[an],
                             thing.z, mobjtype_t.MT_TFOG);

                    // emit sound, where?
                    obs.DOOM.doomSound.StartSound(fog, sounds.sfxenum_t.sfx_telept);

                    // don't move for a bit
                    if (thing.player != null) {
                        thing.reactiontime = 18;
                    }

                    thing.angle = m.angle;
                    thing.momx = thing.momy = thing.momz = 0;
                    return 1;
                }
            }
        }
        return 0;
    }

    //
    // TELEPORT MOVE
    // 
    //
    // P_TeleportMove
    //
    default boolean TeleportMove(mobj_t thing, int x, /*fixed*/ int y) {
        final Actions.Registry obs = obs();
        int xl;
        int xh;
        int yl;
        int yh;
        int bx;
        int by;

        subsector_t newsubsec;

        // kill anything occupying the position
        obs.tmthing = thing;
        obs.tmflags = thing.flags;

        obs.tmx = x;
        obs.tmy = y;

        obs.tmbbox[BOXTOP] = y + obs.tmthing.radius;
        obs.tmbbox[BOXBOTTOM] = y - obs.tmthing.radius;
        obs.tmbbox[BOXRIGHT] = x + obs.tmthing.radius;
        obs.tmbbox[BOXLEFT] = x - obs.tmthing.radius;

        newsubsec = obs.DOOM.levelLoader.PointInSubsector(x, y);
        obs.ceilingline = null;

        // The base floor/ceiling is from the subsector
        // that contains the point.
        // Any contacted lines the step closer together
        // will adjust them.
        obs.tmfloorz = obs.tmdropoffz = newsubsec.sector.floorheight;
        obs.tmceilingz = newsubsec.sector.ceilingheight;

        obs.DOOM.sceneRenderer.increaseValidCount(1); // This is r_main's ?
        obs.numspechit = 0;

        // stomp on any things contacted
        xl = obs.DOOM.levelLoader.getSafeBlockX(obs.tmbbox[BOXLEFT] - obs.DOOM.levelLoader.bmaporgx - MAXRADIUS);
        xh = obs.DOOM.levelLoader.getSafeBlockX(obs.tmbbox[BOXRIGHT] - obs.DOOM.levelLoader.bmaporgx + MAXRADIUS);
        yl = obs.DOOM.levelLoader.getSafeBlockY(obs.tmbbox[BOXBOTTOM] - obs.DOOM.levelLoader.bmaporgy - MAXRADIUS);
        yh = obs.DOOM.levelLoader.getSafeBlockY(obs.tmbbox[BOXTOP] - obs.DOOM.levelLoader.bmaporgy + MAXRADIUS);

        for (bx = xl; bx <= xh; bx++) {
            for (by = yl; by <= yh; by++) {
                if (!this.BlockThingsIterator(bx, by, this::StompThing)) {
                    return false;
                }
            }
        }

        // the move is ok,
        // so link the thing into its new position
        obs.UnsetThingPosition(thing);

        thing.floorz = obs.tmfloorz;
        thing.ceilingz = obs.tmceilingz;
        thing.x = x;
        thing.y = y;

        obs.DOOM.levelLoader.SetThingPosition(thing);

        return true;
    }

}
