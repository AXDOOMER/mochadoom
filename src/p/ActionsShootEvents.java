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
import static doom.SourceCode.P_Map.PTR_ShootTraverse;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedDiv;
import static m.fixed_t.FixedMul;
import static p.mobj_t.MF_NOBLOOD;
import static p.mobj_t.MF_SHOOTABLE;
import rr.line_t;
import static rr.line_t.ML_TWOSIDED;
import static utils.C2JUtils.eval;

interface ActionsShootEvents extends ActionsFloors, ActionsDoors {
    /**
     * PTR_ShootTraverse
     *
     * 9/5/2011: Accepted _D_'s fix
     */
    @SourceCode.P_Map.C(PTR_ShootTraverse) default boolean ShootTraverse(intercept_t in) {
        final Actions.Registry obs = obs();
        @SourceCode.fixed_t int x, y, z, frac;
        line_t li;
        mobj_t th;
    
        @SourceCode.fixed_t int slope, dist, thingtopslope, thingbottomslope;

        if (in.isaline) {
            li = (line_t) in.d();

            if (li.special != 0) {
                this.ShootSpecialLine(obs.shootthing, li);
            }

            if (!eval(li.flags & ML_TWOSIDED)) {
                return this.gotoHitLine(in, li);
            }

            // crosses a two sided line
            obs.LineOpening(li);

            dist = FixedMul(obs.attackrange, in.frac);

            if (li.frontsector.floorheight != li.backsector.floorheight) {
                slope = FixedDiv(obs.openbottom - obs.shootz, dist);
                if (slope > obs.aimslope) {
                    return this.gotoHitLine(in, li);
                }
            }

            if (li.frontsector.ceilingheight != li.backsector.ceilingheight) {
                slope = FixedDiv(obs.opentop - obs.shootz, dist);
                if (slope < obs.aimslope) {
                    return this.gotoHitLine(in, li);
                }
            }

            // shot continues
            return true;

        }

        // shoot a thing
        th = (mobj_t) in.d();
        if (th == obs.shootthing) {
            return true;        // can't shoot self
        }
        if (!eval(th.flags & MF_SHOOTABLE)) {
            return true;        // corpse or something
        }
        // check angles to see if the thing can be aimed at
        dist = FixedMul(obs.attackrange, in.frac);
        thingtopslope = FixedDiv(th.z + th.height - obs.shootz, dist);

        if (thingtopslope < obs.aimslope) {
            return true;        // shot over the thing
        }
        thingbottomslope = FixedDiv(th.z - obs.shootz, dist);

        if (thingbottomslope > obs.aimslope) {
            return true;        // shot under the thing
        }

        // hit thing
        // position a bit closer
        frac = in.frac - FixedDiv(10 * FRACUNIT, obs.attackrange);

        x = obs.trace.x + FixedMul(obs.trace.dx, frac);
        y = obs.trace.y + FixedMul(obs.trace.dy, frac);
        z = obs.shootz + FixedMul(obs.aimslope, FixedMul(frac, obs.attackrange));

        // Spawn bullet puffs or blod spots,
        // depending on target type.
        if (eval(((mobj_t) in.d()).flags & MF_NOBLOOD)) {
            this.SpawnPuff(x, y, z);
        } else {
            this.SpawnBlood(x, y, z, obs.la_damage);
        }

        if (obs.la_damage != 0) {
            this.DamageMobj(th, obs.shootthing, obs.shootthing, obs.la_damage);
        }

        // don't go any farther
        return false;
    };
    
    /**
     * P_ShootSpecialLine - IMPACT SPECIALS Called when a thing shoots a special line.
     */
    default void ShootSpecialLine(mobj_t thing, line_t line) {
        final Actions.Registry obs = obs();
        boolean ok;

        //  Impacts that other things can activate.
        if (thing.player == null) {
            ok = false;
            switch (line.special) {
                case 46:
                    // OPEN DOOR IMPACT
                    ok = true;
                    break;
            }
            if (!ok) {
                return;
            }
        }

        switch (line.special) {
            case 24:
                // RAISE FLOOR
                this.DoFloor(line, floor_e.raiseFloor);
                obs.SW.ChangeSwitchTexture(line, false);
                break;

            case 46:
                // OPEN DOOR
                this.DoDoor(line, vldoor_e.open);
                obs.SW.ChangeSwitchTexture(line, true);
                break;

            case 47:
                // RAISE FLOOR NEAR AND CHANGE
                obs.PEV.DoPlat(line, plattype_e.raiseToNearestAndChange, 0);
                obs.SW.ChangeSwitchTexture(line, false);
                break;
        }
    }

    //_D_: NOTE: this function was added, because replacing a goto by a boolean flag caused a bug if shooting a single sided line
    default boolean gotoHitLine(intercept_t in, line_t li) {
        final Actions.Registry obs = obs();
        int x, y, z, frac;

        // position a bit closer
        frac = in.frac - FixedDiv(4 * FRACUNIT, obs.attackrange);
        x = obs.trace.x + FixedMul(obs.trace.dx, frac);
        y = obs.trace.y + FixedMul(obs.trace.dy, frac);
        z = obs.shootz + FixedMul(obs.aimslope, FixedMul(frac, obs.attackrange));

        if (li.frontsector.ceilingpic == obs.DOOM.textureManager.getSkyFlatNum()) {
            // don't shoot the sky!
            if (z > li.frontsector.ceilingheight) {
                return false;
            }

            // it's a sky hack wall
            if (li.backsector != null && li.backsector.ceilingpic == obs.DOOM.textureManager.getSkyFlatNum()) {
                return false;
            }
        }

        // Spawn bullet puffs.
        this.SpawnPuff(x, y, z);

        // don't go any farther
        return false;
    }    
}
