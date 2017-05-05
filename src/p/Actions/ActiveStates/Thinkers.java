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
package p.Actions.ActiveStates;

import doom.thinker_t;
import p.Actions.ActionTrait;
import p.ceiling_t;
import p.fireflicker_t;
import p.floormove_t;
import p.glow_t;
import p.lightflash_t;
import p.plat_t;
import p.slidedoor_t;
import p.strobe_t;
import p.vldoor_t;

/**
 * TODO:
 * 
 * rethink necessity and objectness of these...
 * - Good Sign 2017/04/30
 */
public interface Thinkers extends ActionTrait {
    default void T_FireFlicker(thinker_t f) {
        ((fireflicker_t) f).FireFlicker();
    }

    default void T_LightFlash(thinker_t l) {
        ((lightflash_t) l).LightFlash();
    }

    default void T_StrobeFlash(thinker_t s) {
        ((strobe_t) s).StrobeFlash();
    }

    default void T_Glow(thinker_t g) {
        ((glow_t) g).Glow();
    }

    default void T_MoveCeiling(thinker_t c) {
        getThinkers().MoveCeiling((ceiling_t) c);
    }

    default void T_MoveFloor(thinker_t f) {
        getThinkers().MoveFloor((floormove_t) f);
    }

    default void T_VerticalDoor(thinker_t v) {
        getThinkers().VerticalDoor((vldoor_t) v);
    }
    
    default void T_SlidingDoor(thinker_t door) {
        getThinkers().SlidingDoor((slidedoor_t) door);
    }
    
    default void T_PlatRaise(thinker_t p) {
        getThinkers().PlatRaise((plat_t) p);
    }
}
