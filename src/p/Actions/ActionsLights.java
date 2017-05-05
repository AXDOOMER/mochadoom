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
package p.Actions;

import p.AbstractLevelLoader;
import static p.DoorDefines.SLOWDARK;
import rr.line_t;
import rr.sector_t;

public interface ActionsLights extends ActionsMoveEvents, ActionsUseEvents {

    int FindSectorFromLineTag(line_t line, int secnum);

    //
    // Start strobing lights (usually from a trigger)
    //
    @Override
    default void StartLightStrobing(line_t line) {
        final AbstractLevelLoader ll = levelLoader();

        int secnum;
        sector_t sec;

        secnum = -1;
        while ((secnum = FindSectorFromLineTag(line, secnum)) >= 0) {
            sec = ll.sectors[secnum];
            if (sec.specialdata != null) {
                continue;
            }

            sec.SpawnStrobeFlash(SLOWDARK, 0);
        }
    }

    //
    // TURN LINE'S TAG LIGHTS OFF
    //
    @Override
    default void TurnTagLightsOff(line_t line) {
        final AbstractLevelLoader ll = levelLoader();

        int i;
        int min;
        sector_t sector;
        sector_t tsec;
        line_t templine;

        for (int j = 0; j < ll.numsectors; j++) {
            sector = ll.sectors[j];
            if (sector.tag == line.tag) {

                min = sector.lightlevel;
                for (i = 0; i < sector.linecount; i++) {
                    templine = sector.lines[i];
                    tsec = templine.getNextSector(sector);
                    if (tsec == null) {
                        continue;
                    }
                    if (tsec.lightlevel < min) {
                        min = tsec.lightlevel;
                    }
                }
                sector.lightlevel = (short) min;
            }
        }
    }

    //
    // TURN LINE'S TAG LIGHTS ON
    //
    @Override
    default void LightTurnOn(line_t line, int bright) {
        final AbstractLevelLoader ll = levelLoader();

        sector_t sector;
        sector_t temp;
        line_t templine;

        for (int i = 0; i < ll.numsectors; i++) {
            sector = ll.sectors[i];
            if (sector.tag == line.tag) {
                // bright = 0 means to search
                // for highest light level
                // surrounding sector
                if (bright == 0) {
                    for (int j = 0; j < sector.linecount; j++) {
                        templine = sector.lines[j];
                        temp = templine.getNextSector(sector);

                        if (temp == null) {
                            continue;
                        }

                        if (temp.lightlevel > bright) {
                            bright = temp.lightlevel;
                        }
                    }
                }
                sector.lightlevel = (short) bright;
            }
        }
    }
}
