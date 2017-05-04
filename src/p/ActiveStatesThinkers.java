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

import doom.thinker_t;

/**
 * TODO:
 * 
 * rethink necessity and objectness of these...
 * - Good Sign 2017/04/30
 */
interface ActiveStatesThinkers extends ActionTrait {
    void MoveCeiling(ceiling_t ceiling_t);
    void MoveFloor(floormove_t floormove_t);
    void VerticalDoor(vldoor_t vldoor_t);
    void SlidingDoor(slidedoor_t slidedoor_t);
    void RemoveThinker(slidedoor_t door);
    void PlatRaise(plat_t plat_t);

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
        MoveCeiling((ceiling_t) c);
    }

    default void T_MoveFloor(thinker_t f) {
        MoveFloor((floormove_t) f);
    }

    default void T_VerticalDoor(thinker_t v) {
        VerticalDoor((vldoor_t) v);
    }
    
    default void T_SlidingDoor(thinker_t door) {
        SlidingDoor((slidedoor_t) door);
    }
    
    default void T_PlatRaise(thinker_t p) {
        PlatRaise((plat_t) p);
    }
}
