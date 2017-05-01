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

import static data.Defines.MISSILERANGE;
import static data.Defines.PT_ADDLINES;
import static data.Defines.PT_ADDTHINGS;
import static data.Limits.MAXRADIUS;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import static data.info.mobjinfo;
import data.mobjtype_t;
import defines.statenum_t;
import doom.SourceCode;
import static doom.SourceCode.P_Enemy.PIT_VileCheck;
import static doom.SourceCode.P_Map.PIT_RadiusAttack;
import doom.SourceCode.fixed_t;
import p.ActionSystem.AbstractCommand;

import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static p.mobj_t.MF_CORPSE;
import static p.mobj_t.MF_SHOOTABLE;
import static utils.C2JUtils.eval;

interface ActionsAttacks<R extends Actions.Registry & AbstractCommand<R>> extends ActionsShootEvents<R>, ActionsPathTraverse<R> {
    //
    // P_GunShot
    //
    default void P_GunShot(mobj_t mo, boolean accurate) {
        final Actions.Registry obs = obs();
        long angle;
        int damage;

        damage = 5 * (obs.DOOM.random.P_Random() % 3 + 1);
        angle = mo.angle;

        if (!accurate) {
            angle += (obs.DOOM.random.P_Random() - obs.DOOM.random.P_Random()) << 18;
        }

        this.LineAttack(mo, angle, MISSILERANGE, obs.bulletslope, damage);
    }
    //
    // P_LineAttack
    //    //
    // P_LineAttack
    //
    /**
     * who got hit (or NULL)
     */

    /**
     * P_LineAttack If damage == 0, it is just a test trace that will leave linetarget set.
     *
     * @param t1
     * @param angle angle_t
     * @param distance fixed_t
     * @param slope fixed_t
     * @param damage
     */
    default void LineAttack(mobj_t t1, @SourceCode.angle_t long angle, @fixed_t int distance, @fixed_t int slope, int damage) {
        final Actions.Registry obs = obs();
        int x2, y2;
        
        obs.shootthing = t1;
        obs.la_damage = damage;
        x2 = t1.x + (distance >> FRACBITS) * finecosine(angle);
        y2 = t1.y + (distance >> FRACBITS) * finesine(angle);
        obs.shootz = t1.z + (t1.height >> 1) + 8 * FRACUNIT;
        obs.attackrange = distance;
        obs.aimslope = slope;

        this.PathTraverse(t1.x, t1.y, x2, y2, PT_ADDLINES | PT_ADDTHINGS, this::ShootTraverse);
    }

    //
    // RADIUS ATTACK
    //


    /**
     * P_RadiusAttack Source is the creature that caused the explosion at spot.
     */
    default void RadiusAttack(mobj_t spot, mobj_t source, int damage) {
        final Actions.Registry obs = obs();
        int x;
        int y;

        int xl;
        int xh;
        int yl;
        int yh;

        @SourceCode.fixed_t int dist;

        dist = (damage + MAXRADIUS) << FRACBITS;
        yh = obs.DOOM.levelLoader.getSafeBlockY(spot.y + dist - obs.DOOM.levelLoader.bmaporgy);
        yl = obs.DOOM.levelLoader.getSafeBlockY(spot.y - dist - obs.DOOM.levelLoader.bmaporgy);
        xh = obs.DOOM.levelLoader.getSafeBlockX(spot.x + dist - obs.DOOM.levelLoader.bmaporgx);
        xl = obs.DOOM.levelLoader.getSafeBlockX(spot.x - dist - obs.DOOM.levelLoader.bmaporgx);
        obs.bombspot = spot;
        obs.bombsource = source;
        obs.bombdamage = damage;

        for (y = yl; y <= yh; y++) {
            for (x = xl; x <= xh; x++) {
                this.BlockThingsIterator(x, y, this::RadiusAttack);
            }
        }
    }
    ///////////////////// PIT AND PTR FUNCTIONS //////////////////
    /**
     * PIT_VileCheck Detect a corpse that could be raised.
     */
    
    @SourceCode.P_Enemy.C(PIT_VileCheck) default boolean VileCheck(mobj_t thing) {
        final Actions.Registry obs = obs();
        int maxdist;
        boolean check;

        if (!eval(thing.flags & MF_CORPSE)) {
            return true;    // not a monster
        }
        if (thing.mobj_tics != -1) {
            return true;    // not lying still yet
        }
        if (thing.info.raisestate == statenum_t.S_NULL) {
            return true;    // monster doesn't have a raise state
        }
        maxdist = thing.info.radius + mobjinfo[mobjtype_t.MT_VILE.ordinal()].radius;

        if (Math.abs(thing.x - obs.vileTryX) > maxdist
                || Math.abs(thing.y - obs.vileTryY) > maxdist) {
            return true;        // not actually touching
        }
        obs.vileCorpseHit = thing;
        obs.vileCorpseHit.momx = obs.vileCorpseHit.momy = 0;
        obs.vileCorpseHit.height <<= 2;
        check = this.CheckPosition(obs.vileCorpseHit, obs.vileCorpseHit.x, obs.vileCorpseHit.y);
        obs.vileCorpseHit.height >>= 2;

        // check it doesn't fit here, or stop checking
        return !check;
    };

    /**
     * PIT_RadiusAttack "bombsource" is the creature that caused the explosion at "bombspot".
     */
    @SourceCode.P_Map.C(PIT_RadiusAttack) default boolean RadiusAttack(mobj_t thing) {
        final Actions.Registry obs = obs();
        int dx, dy, dist;
        fixed_t: {
            dx: dy: dist:;
        }

        if (!eval(thing.flags & MF_SHOOTABLE)) {
            return true;
        }

        // Boss spider and cyborg
        // take no damage from concussion.
        if (thing.type == mobjtype_t.MT_CYBORG
                || thing.type == mobjtype_t.MT_SPIDER) {
            return true;
        }

        dx = Math.abs(thing.x - obs.bombspot.x);
        dy = Math.abs(thing.y - obs.bombspot.y);

        dist = dx > dy ? dx : dy;
        dist = (dist - thing.radius) >> FRACBITS;

        if (dist < 0) {
            dist = 0;
        }

        if (dist >= obs.bombdamage) {
            return true;    // out of range
        }
        if (obs.EN.CheckSight(thing, obs.bombspot)) {
            // must be in direct path
            this.DamageMobj(thing, obs.bombspot, obs.bombsource, obs.bombdamage - dist);
        }

        return true;
    };

}
