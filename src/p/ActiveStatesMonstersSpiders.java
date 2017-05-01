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
import p.ActionSystem.AbstractCommand;

interface ActiveStatesMonstersSpiders<R extends Actions.Registry & AbstractCommand<R>> extends ActiveStatesAi<R>, ActionsMissiles<R> {
    default void A_SpidRefire(mobj_t actor) {
        final Actions.Registry obs = obs();
        // keep firing unless target got out of sight
        A_FaceTarget(actor);

        if (obs.DOOM.random.P_Random() < 10) {
            return;
        }

        if (actor.target == null
            || actor.target.health <= 0
            || !obs.EN.CheckSight(actor, actor.target)) {
            actor.SetMobjState(actor.info.seestate);
        }
    }

    default void A_BspiAttack(mobj_t actor) {
        final Actions.Registry obs = obs();
        if (actor.target == null) {
            return;
        }

        A_FaceTarget(actor);

        // launch a missile
        SpawnMissile(actor, actor.target, mobjtype_t.MT_ARACHPLAZ);
    }
}
