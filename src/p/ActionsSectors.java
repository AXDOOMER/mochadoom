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

import data.mobjtype_t;
import defines.statenum_t;
import doom.SourceCode;
import p.ActionSystem.AbstractCommand;

import static doom.SourceCode.P_Map.PIT_ChangeSector;
import static m.BBox.BOXBOTTOM;
import static m.BBox.BOXLEFT;
import static m.BBox.BOXRIGHT;
import static m.BBox.BOXTOP;
import static p.mobj_t.MF_DROPPED;
import static p.mobj_t.MF_SHOOTABLE;
import static p.mobj_t.MF_SOLID;
import rr.sector_t;
import static utils.C2JUtils.eval;

interface ActionsSectors<R extends Actions.Registry & AbstractCommand<R>> extends ActionsClipping<R> {
    //
    // P_ChangeSector
    //
    default boolean ChangeSector(sector_t sector, boolean crunch) {
        final p.Actions.Registry obs = obs();
        int x;
        int y;

        obs.nofit = false;
        obs.crushchange = crunch;

        // re-check heights for all things near the moving sector
        for (x = sector.blockbox[BOXLEFT]; x <= sector.blockbox[BOXRIGHT]; x++) {
            for (y = sector.blockbox[BOXBOTTOM]; y <= sector.blockbox[BOXTOP]; y++) {
                this.BlockThingsIterator(x, y, this::ChangeSector);
            }
        }

        return obs.nofit;
    }

    /**
     * PIT_ChangeSector
     */
    @SourceCode.P_Map.C(PIT_ChangeSector) default boolean ChangeSector(mobj_t thing) {
        final p.Actions.Registry obs = obs();
        mobj_t mo;

        if (this.ThingHeightClip(thing)) {
            // keep checking
            return true;
        }

        // crunch bodies to giblets
        if (thing.health <= 0) {
            thing.SetMobjState(statenum_t.S_GIBS);

            thing.flags &= ~MF_SOLID;
            thing.height = 0;
            thing.radius = 0;

            // keep checking
            return true;
        }

        // crunch dropped items
        if (eval(thing.flags & MF_DROPPED)) {
            obs.RemoveMobj(thing);

            // keep checking
            return true;
        }

        if (!eval(thing.flags & MF_SHOOTABLE)) {
            // assume it is bloody gibs or something
            return true;
        }

        obs.nofit = true;

        if (obs.crushchange && !eval(obs.DOOM.leveltime & 3)) {
            this.DamageMobj(thing, null, null, 10);

            // spray blood in a random direction
            mo = this.SpawnMobj(thing.x, thing.y, thing.z + thing.height / 2, mobjtype_t.MT_BLOOD);

            mo.momx = (obs.DOOM.random.P_Random() - obs.DOOM.random.P_Random()) << 12;
            mo.momy = (obs.DOOM.random.P_Random() - obs.DOOM.random.P_Random()) << 12;
        }

        // keep checking (crush other things)   
        return true;
    };

    
}
