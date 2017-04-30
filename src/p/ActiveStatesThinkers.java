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
import static rr.line_t.ML_BLOCKING;

/**
 * TODO:
 * 
 * rethink necessity and objectness of these...
 * - Good Sign 2017/04/30
 */
interface ActiveStatesThinkers extends ActionsCeilings, ActionsFloors, ActionsDoors {
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
    
    default void T_SlidingDoor(thinker_t doort) {
        final slidedoor_t door = (slidedoor_t) doort;
        final Actions.Registry obs = obs();
        switch (door.status) {
            case sd_opening:
                if (door.timer-- == 0) {
                    if (++door.frame == SlideDoor.SNUMFRAMES) {
                        // IF DOOR IS DONE OPENING...
                        obs.DOOM.levelLoader.sides[door.line.sidenum[0]].midtexture = 0;
                        obs.DOOM.levelLoader.sides[door.line.sidenum[1]].midtexture = 0;
                        door.line.flags &= ML_BLOCKING ^ 0xff;

                        if (door.type == sdt_e.sdt_openOnly) {
                            door.frontsector.specialdata = null;
                            obs.RemoveThinker(door);
                            break;
                        }

                        door.timer = SlideDoor.SDOORWAIT;
                        door.status = sd_e.sd_waiting;
                    } else {
                        // IF DOOR NEEDS TO ANIMATE TO NEXT FRAME...
                        door.timer = SlideDoor.SWAITTICS;

                        obs.DOOM.levelLoader.sides[door.line.sidenum[0]].midtexture = (short) obs.SL.slideFrames[door.whichDoorIndex].frontFrames[door.frame];
                        obs.DOOM.levelLoader.sides[door.line.sidenum[1]].midtexture = (short) obs.SL.slideFrames[door.whichDoorIndex].backFrames[door.frame];
                    }
                }
                break;

            case sd_waiting:
                // IF DOOR IS DONE WAITING...
                if (door.timer-- == 0) {
                    // CAN DOOR CLOSE?
                    if (door.frontsector.thinglist != null
                        || door.backsector.thinglist != null) {
                        door.timer = SlideDoor.SDOORWAIT;
                        break;
                    }

                    // door.frame = SNUMFRAMES-1;
                    door.status = sd_e.sd_closing;
                    door.timer = SlideDoor.SWAITTICS;
                }
                break;

            case sd_closing:
                if (door.timer-- == 0) {
                    if (--door.frame < 0) {
                        // IF DOOR IS DONE CLOSING...
                        door.line.flags |= ML_BLOCKING;
                        door.frontsector.specialdata = null;
                        obs.RemoveThinker(door);
                        break;
                    } else {
                        // IF DOOR NEEDS TO ANIMATE TO NEXT FRAME...
                        door.timer = SlideDoor.SWAITTICS;

                        obs.DOOM.levelLoader.sides[door.line.sidenum[0]].midtexture = (short) obs.SL.slideFrames[door.whichDoorIndex].frontFrames[door.frame];
                        obs.DOOM.levelLoader.sides[door.line.sidenum[1]].midtexture = (short) obs.SL.slideFrames[door.whichDoorIndex].backFrames[door.frame];
                    }
                }
                break;
        }
    }
    
    default void T_PlatRaise(thinker_t p) {
        PlatRaise((plat_t) p);
    }
}
