package p;

import data.sounds;
import defines.card_t;
import static doom.englsh.PD_BLUEK;
import static doom.englsh.PD_BLUEO;
import static doom.englsh.PD_REDK;
import static doom.englsh.PD_REDO;
import static doom.englsh.PD_YELLOWK;
import static doom.englsh.PD_YELLOWO;
import doom.player_t;
import static m.fixed_t.FRACUNIT;
import static p.DoorDefines.VDOORSPEED;
import static p.DoorDefines.VDOORWAIT;
import rr.line_t;
import rr.sector_t;
import static utils.C2JUtils.eval;

public interface ActionsDoors<T, V> extends ActionsPlanes<T, V> {
    //
    // VERTICAL DOORS
    //
    /**
     * T_VerticalDoor
     */
    default void VerticalDoor(vldoor_t door) {
        final ActionsRegistry<T, V> obs = obs();
        switch (door.direction) {
            case 0:
                // WAITING
                if (!eval(--door.topcountdown)) {
                    switch (door.type) {
                        case blazeRaise:
                            door.direction = -1; // time to go back down
                            obs.DOOM.doomSound.StartSound(door.sector.soundorg, sounds.sfxenum_t.sfx_bdcls);
                            break;
                        case normal:
                            door.direction = -1; // time to go back down
                            obs.DOOM.doomSound.StartSound(door.sector.soundorg, sounds.sfxenum_t.sfx_dorcls);
                            break;
                        case close30ThenOpen:
                            door.direction = 1;
                            obs.DOOM.doomSound.StartSound(door.sector.soundorg, sounds.sfxenum_t.sfx_doropn);
                            break;
                    }
                }
                break;

            case 2:
                //  INITIAL WAIT
                if (!eval(--door.topcountdown)) {
                    switch (door.type) {
                        case raiseIn5Mins:
                            door.direction = 1;
                            door.type = vldoor_e.normal;
                            obs.DOOM.doomSound.StartSound(door.sector.soundorg, sounds.sfxenum_t.sfx_doropn);
                            break;
                    }
                }
                break;

            case -1: {
                // DOWN
                final result_e res = this.MovePlane(door.sector, door.speed, door.sector.floorheight, false, 1, door.direction);
                if (res == result_e.pastdest) {
                    switch (door.type) {
                        case blazeRaise:
                        case blazeClose:
                            door.sector.specialdata = null;
                            obs.RemoveThinker(door);  // unlink and free
                            obs.DOOM.doomSound.StartSound(door.sector.soundorg, sounds.sfxenum_t.sfx_bdcls);
                            break;
                        case normal:
                        case close:
                            door.sector.specialdata = null;
                            obs.RemoveThinker(door);  // unlink and free
                            break;
                        case close30ThenOpen:
                            door.direction = 0;
                            door.topcountdown = 35 * 30;
                            break;
                    }
                } else if (res == result_e.crushed) {
                    switch (door.type) {
                        case blazeClose:
                        case close:       // DO NOT GO BACK UP!
                            break;
                        default:
                            door.direction = 1;
                            obs.DOOM.doomSound.StartSound(door.sector.soundorg, sounds.sfxenum_t.sfx_doropn);
                    }
                }
                break;
            } case 1: {
                // UP
                final result_e res = this.MovePlane(door.sector, door.speed, door.topheight, false, 1, door.direction);

                if (res == result_e.pastdest) {
                    switch (door.type) {
                        case blazeRaise:
                        case normal:
                            door.direction = 0; // wait at top
                            door.topcountdown = door.topwait;
                            break;
                        case close30ThenOpen:
                        case blazeOpen:
                        case open:
                            door.sector.specialdata = null;
                            obs.RemoveThinker(door);  // unlink and free
                            break;
                    }
                }
                break;
            }
        }
    }

    /**
     * EV_DoLockedDoor Move a locked door up/down
     */
    default boolean DoLockedDoor(line_t line, vldoor_e type, mobj_t thing) {
        final ActionsRegistry<T, V> obs = obs();
        player_t p;

        p = thing.player;

        if (p == null) {
            return false;
        }

        switch (line.special) {
            case 99: // Blue Lock
            case 133:
                /*         if ( p==null )
             return false; */
                if (!p.cards[card_t.it_bluecard.ordinal()] && !p.cards[card_t.it_blueskull.ordinal()]) {
                    p.message = PD_BLUEO;
                    obs.DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_oof);
                    return false;
                }
                break;

            case 134: // Red Lock
            case 135:
                /*        if ( p==null )
             return false; */
                if (!p.cards[card_t.it_redcard.ordinal()] && !p.cards[card_t.it_redskull.ordinal()]) {
                    p.message = PD_REDO;
                    obs.DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_oof);
                    return false;
                }
                break;

            case 136:    // Yellow Lock
            case 137:
                /*        if ( p==null )
             return false; */
                if (!p.cards[card_t.it_yellowcard.ordinal()]
                        && !p.cards[card_t.it_yellowskull.ordinal()]) {
                    p.message = PD_YELLOWO;
                    obs.DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_oof);
                    return false;
                }
                break;
        }

        return this.DoDoor(line, type);
    }

    default boolean DoDoor(line_t line, vldoor_e type) {
        final ActionsRegistry<T, V> obs = obs();
        int secnum;
        boolean rtn = false;
        sector_t sec;
        vldoor_t door;

        secnum = -1;

        while ((secnum = obs.FindSectorFromLineTag(line, secnum)) >= 0) {
            sec = obs.DOOM.levelLoader.sectors[secnum];
            if (sec.specialdata != null) {
                continue;
            }

            // new door thinker
            rtn = true;
            door = new vldoor_t();
            sec.specialdata = door;
            door.thinkerFunction = ActionFunction.T_VerticalDoor;
            obs.AddThinker(door);
            door.sector = sec;
            door.type = type;
            door.topwait = VDOORWAIT;
            door.speed = VDOORSPEED;

            switch (type) {
                case blazeClose:
                    door.topheight = sec.FindLowestCeilingSurrounding();
                    door.topheight -= 4 * FRACUNIT;
                    door.direction = -1;
                    door.speed = VDOORSPEED * 4;
                    obs.DOOM.doomSound.StartSound(door.sector.soundorg, sounds.sfxenum_t.sfx_bdcls);
                    break;
                case close:
                    door.topheight = sec.FindLowestCeilingSurrounding();
                    door.topheight -= 4 * FRACUNIT;
                    door.direction = -1;
                    obs.DOOM.doomSound.StartSound(door.sector.soundorg, sounds.sfxenum_t.sfx_dorcls);
                    break;
                case close30ThenOpen:
                    door.topheight = sec.ceilingheight;
                    door.direction = -1;
                    obs.DOOM.doomSound.StartSound(door.sector.soundorg, sounds.sfxenum_t.sfx_dorcls);
                    break;
                case blazeRaise:
                case blazeOpen:
                    door.direction = 1;
                    door.topheight = sec.FindLowestCeilingSurrounding();
                    door.topheight -= 4 * FRACUNIT;
                    door.speed = VDOORSPEED * 4;
                    if (door.topheight != sec.ceilingheight) {
                        obs.DOOM.doomSound.StartSound(door.sector.soundorg, sounds.sfxenum_t.sfx_bdopn);
                    }
                    break;
                case normal:
                case open:
                    door.direction = 1;
                    door.topheight = sec.FindLowestCeilingSurrounding();
                    door.topheight -= 4 * FRACUNIT;
                    if (door.topheight != sec.ceilingheight) {
                        obs.DOOM.doomSound.StartSound(door.sector.soundorg, sounds.sfxenum_t.sfx_doropn);
                    }
            }

        }
        return rtn;
    }

    /**
     * EV_VerticalDoor : open a door manually, no tag value
     */
    default void VerticalDoor(line_t line, mobj_t thing) {
        final ActionsRegistry<T, V> obs = obs();
        player_t player;
        //int      secnum;
        sector_t sec;
        vldoor_t door;
        int side;

        side = 0;  // only front sides can be used

        // Check for locks
        player = thing.player;

        switch (line.special) {
            case 26: // Blue Lock
            case 32:
                if (player == null) {
                    return;
                }

                if (!player.cards[card_t.it_bluecard.ordinal()] && !player.cards[card_t.it_blueskull.ordinal()]) {
                    player.message = PD_BLUEK;
                    obs.DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_oof);
                    return;
                }
                break;

            case 27: // Yellow Lock
            case 34:
                if (player == null) {
                    return;
                }

                if (!player.cards[card_t.it_yellowcard.ordinal()] && !player.cards[card_t.it_yellowskull.ordinal()]) {
                    player.message = PD_YELLOWK;
                    obs.DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_oof);
                    return;
                }
                break;

            case 28: // Red Lock
            case 33:
                if (player == null) {
                    return;
                }

                if (!player.cards[card_t.it_redcard.ordinal()] && !player.cards[card_t.it_redskull.ordinal()]) {
                    player.message = PD_REDK;
                    obs.DOOM.doomSound.StartSound(null, sounds.sfxenum_t.sfx_oof);
                    return;
                }
                break;
        }

        // if the sector has an active thinker, use it
        sec = obs.DOOM.levelLoader.sides[line.sidenum[side ^ 1]].sector;
        // secnum = sec.id;

        if (sec.specialdata != null) {
            if (sec.specialdata instanceof plat_t) {
                /**
                 * [MAES]: demo sync for e1nm0646: emulates active plat_t interpreted
                 * as door. TODO: add our own overflow handling class.
                 */
                door = ((plat_t) sec.specialdata).asVlDoor(obs.DOOM.levelLoader.sectors);
            } else {
                door = (vldoor_t) sec.specialdata;
            }
            switch (line.special) {
                case 1: // ONLY FOR "RAISE" DOORS, NOT "OPEN"s
                case 26:
                case 27:
                case 28:
                case 117:
                    if (door.direction == -1) {
                        door.direction = 1; // go back up
                    } else {
                        if (thing.player == null) {
                            return;     // JDC: bad guys never close doors
                        }
                        door.direction = -1;    // start going down immediately
                    }
                    return;
            }
        }

        // for proper sound
        switch (line.special) {
            case 117:    // BLAZING DOOR RAISE
            case 118:    // BLAZING DOOR OPEN
                obs.DOOM.doomSound.StartSound(sec.soundorg, sounds.sfxenum_t.sfx_bdopn);
                break;

            case 1:  // NORMAL DOOR SOUND
            case 31:
                obs.DOOM.doomSound.StartSound(sec.soundorg, sounds.sfxenum_t.sfx_doropn);
                break;

            default: // LOCKED DOOR SOUND
                obs.DOOM.doomSound.StartSound(sec.soundorg, sounds.sfxenum_t.sfx_doropn);
                break;
        }

        // new door thinker
        door = new vldoor_t();
        sec.specialdata = door;
        door.thinkerFunction = ActionFunction.T_VerticalDoor;
        obs.AddThinker(door);
        door.sector = sec;
        door.direction = 1;
        door.speed = VDOORSPEED;
        door.topwait = VDOORWAIT;

        switch (line.special) {
            case 1:
            case 26:
            case 27:
            case 28:
                door.type = vldoor_e.normal;
                break;
            case 31:
            case 32:
            case 33:
            case 34:
                door.type = vldoor_e.open;
                line.special = 0;
                break;
            case 117:    // blazing door raise
                door.type = vldoor_e.blazeRaise;
                door.speed = VDOORSPEED * 4;
                break;
            case 118:    // blazing door open
                door.type = vldoor_e.blazeOpen;
                line.special = 0;
                door.speed = VDOORSPEED * 4;
        }

        // find the top and bottom of the movement range
        door.topheight = sec.FindLowestCeilingSurrounding();
        door.topheight -= 4 * FRACUNIT;
    }
}
