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
import data.sounds;

interface ActiveStatesMonstersZombies extends ActiveStatesAi, ActionsAttacks, ActionsAim {
    //
    // A_PosAttack
    //
    default void A_PosAttack(mobj_t actor) {
        final Actions.Registry obs = obs();
        int angle;
        int damage;
        int slope;

        if (actor.target == null) {
            return;
        }
        A_FaceTarget(actor);
        angle = (int) actor.angle;
        slope = AimLineAttack(actor, angle, MISSILERANGE);

        obs.DOOM.doomSound.StartSound(actor, sounds.sfxenum_t.sfx_pistol);
        angle += (obs.DOOM.random.P_Random() - obs.DOOM.random.P_Random()) << 20;
        damage = ((obs.DOOM.random.P_Random() % 5) + 1) * 3;
        LineAttack(actor, angle, MISSILERANGE, slope, damage);
    }

    default void A_SPosAttack(mobj_t actor) {
        final Actions.Registry obs = obs();
        int i;
        long angle;
        long bangle;
        int damage;
        int slope;

        if (actor.target == null) {
            return;
        }

        obs.DOOM.doomSound.StartSound(actor, sounds.sfxenum_t.sfx_shotgn);
        A_FaceTarget(actor);
        bangle = actor.angle;
        slope = AimLineAttack(actor, bangle, MISSILERANGE);

        for (i = 0; i < 3; i++) {
            angle = bangle + ((obs.DOOM.random.P_Random() - obs.DOOM.random.P_Random()) << 20);
            damage = ((obs.DOOM.random.P_Random() % 5) + 1) * 3;
            LineAttack(actor, angle, MISSILERANGE, slope, damage);
        }
    }

    default void A_CPosAttack(mobj_t actor) {
        final Actions.Registry obs = obs();
        long angle;
        long bangle;
        int damage;
        int slope;

        if (actor.target == null) {
            return;
        }

        obs.DOOM.doomSound.StartSound(actor, sounds.sfxenum_t.sfx_shotgn);
        A_FaceTarget(actor);
        bangle = actor.angle;
        slope = AimLineAttack(actor, bangle, MISSILERANGE);

        angle = bangle + ((obs.DOOM.random.P_Random() - obs.DOOM.random.P_Random()) << 20);
        damage = ((obs.DOOM.random.P_Random() % 5) + 1) * 3;
        LineAttack(actor, angle, MISSILERANGE, slope, damage);
    }

    default void A_CPosRefire(mobj_t actor) {
        final Actions.Registry obs = obs();
        // keep firing unless target got out of sight
        A_FaceTarget(actor);

        if (obs.DOOM.random.P_Random() < 40) {
            return;
        }

        if (actor.target == null
            || actor.target.health <= 0
            || !obs.EN.CheckSight(actor, actor.target)) {
            actor.SetMobjState(actor.info.seestate);
        }
    }
}
