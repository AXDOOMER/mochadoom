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

import static data.Tables.BITS32;
import static data.Tables.finecosine;
import static data.Tables.finesine;
import static data.info.mobjinfo;
import data.mobjtype_t;
import doom.SourceCode.angle_t;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FRACUNIT;
import static m.fixed_t.FixedMul;
import static p.MapUtils.AproxDistance;
import static p.mobj_t.MF_MISSILE;
import static p.mobj_t.MF_SHADOW;
import static utils.C2JUtils.eval;

interface ActionsMissiles extends ActionsSpawn {
    void CheckMissileSpawn(mobj_t th);
    int AimLineAttack(mobj_t source, long an, int i);

    /**
     * P_SpawnMissile
     */
    default mobj_t SpawnMissile(mobj_t source, mobj_t dest, mobjtype_t type) {
        mobj_t th;
        @angle_t long an;
        int dist;

        th = SpawnMobj(source.x, source.y, source.z + 4 * 8 * FRACUNIT, type);

        if (th.info.seesound != null) {
            StartSound(th, th.info.seesound);
        }

        th.target = source;    // where it came from
        an = sceneRenderer().PointToAngle2(source.x, source.y, dest.x, dest.y) & BITS32;

        // fuzzy player
        if (eval(dest.flags & MF_SHADOW)) {
            an += (P_Random() - P_Random()) << 20;
        }

        th.angle = an & BITS32;
        //an >>= ANGLETOFINESHIFT;
        th.momx = FixedMul(th.info.speed, finecosine(an));
        th.momy = FixedMul(th.info.speed, finesine(an));

        dist = AproxDistance(dest.x - source.x, dest.y - source.y);
        dist /= th.info.speed;

        if (dist < 1) {
            dist = 1;
        }

        th.momz = (dest.z - source.z) / dist;
        CheckMissileSpawn(th);

        return th;
    }

    /**
     * P_SpawnPlayerMissile Tries to aim at a nearby monster
     */
    default void SpawnPlayerMissile(mobj_t source, mobjtype_t type) {
        final Spawn targ = contextRequire(KEY_SPAWN);
        
        mobj_t th;
        @angle_t long an;
        int x, y, z, slope; // ActionFunction

        // see which target is to be aimed at
        an = source.angle;
        slope = AimLineAttack(source, an, 16 * 64 * FRACUNIT);

        if (targ.linetarget == null) {
            an += 1 << 26;
            an &= BITS32;
            slope = AimLineAttack(source, an, 16 * 64 * FRACUNIT);

            if (targ.linetarget == null) {
                an -= 2 << 26;
                an &= BITS32;
                slope = AimLineAttack(source, an, 16 * 64 * FRACUNIT);
            }

            if (targ.linetarget == null) {
                an = source.angle & BITS32;
                // angle should be "sane"..right?
                // Just this line allows freelook.
                slope = ((source.player.lookdir) << FRACBITS) / 173;
            }
        }

        x = source.x;
        y = source.y;
        z = source.z + 4 * 8 * FRACUNIT + slope;

        th = this.SpawnMobj(x, y, z, type);

        if (th.info.seesound != null) {
            StartSound(th, th.info.seesound);
        }

        th.target = source;
        th.angle = an;
        th.momx = FixedMul(th.info.speed, finecosine(an));
        th.momy = FixedMul(th.info.speed, finesine(an));
        th.momz = FixedMul(th.info.speed, slope);

        CheckMissileSpawn(th);
    }

    /**
     * P_ExplodeMissile
     */
    default void ExplodeMissile(mobj_t mo) {
        mo.momx = mo.momy = mo.momz = 0;

        // MAES 9/5/2011: using mobj code for that.
        mo.SetMobjState(mobjinfo[mo.type.ordinal()].deathstate);

        mo.mobj_tics -= P_Random() & 3;

        if (mo.mobj_tics < 1) {
            mo.mobj_tics = 1;
        }

        mo.flags &= ~MF_MISSILE;

        if (mo.info.deathsound != null) {
            StartSound(mo, mo.info.deathsound);
        }
    }
}
