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

import static data.Limits.MAXPLAYERS;
import doom.SourceCode;
import static doom.SourceCode.P_Tick.P_RemoveThinker;
import doom.thinker_t;
import static p.ActiveStates.NOP;

interface ActionsThinkers extends ActionsSpawn {
    
    //
    // P_RemoveThinker
    // Deallocation is lazy -- it will not actually be freed
    // until its thinking turn comes up.
    //
    //
    // killough 4/25/98:
    //
    // Instead of marking the function with -1 value cast to a function pointer,
    // set the function to P_RemoveThinkerDelayed(), so that later, it will be
    // removed automatically as part of the thinker process.
    //
    
    @Override
    @SourceCode.Compatible("thinker->function.acv = (actionf_v)(-1)")
    @SourceCode.P_Tick.C(P_RemoveThinker)
    default void RemoveThinker(thinker_t thinker) {
        thinker.thinkerFunction = NOP;
    }

    //
    // P_AllocateThinker
    // Allocates memory and adds a new thinker at the end of the list.
    //
    //public void AllocateThinker(thinker_t thinker) {;
        // UNUSED
    //}

    //
    // P_RunThinkers
    //
    default void RunThinkers() {
        thinker_t thinker = getThinkerCap().next;
        while (thinker != getThinkerCap()) {
            if (thinker.thinkerFunction == ActiveStates.NOP) {
                // time to remove it
                thinker.next.prev = thinker.prev;
                thinker.prev.next = thinker.next;
                // Z_Free (currentthinker);
            } else {
                CallThinkerFunction(thinker.thinkerFunction, thinker);
            }
            thinker = thinker.next;
        }
    }
    
    //
    //P_Ticker
    //
    default void Ticker() {
        // run the tic
        if (IsPaused()) {
            return;
        }

        // pause if in menu and at least one tic has been run
        if (!IsNetGame() && IsMenuActive() && !IsDemoPlayback() && getPlayer(ConsolePlayerNumber()).viewz != 1) {
            return;
        }

        for (int i = 0; i < MAXPLAYERS; i++) {
            if (PlayerInGame(i)) {
                getPlayer(i).PlayerThink();
            }
        }

        RunThinkers();
        getSpecials().UpdateSpecials(); // In specials. Merge?
        RespawnSpecials();

        // for par times
        DOOM().leveltime++;
    }
}
