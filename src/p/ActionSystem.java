/*
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

import p.Actions.Registry;

/**
 * A tough task was to implement Actions using Observer pattern
 * (observing a Registry - an instance shared state between Actions)
 * and stateful Actions (Actions used in States, and listed in an Enum)
 * using Command pattern to be combined together.
 * 
 * It could be done in Strategy pattern - all ActionStates in one Enum with
 * lambdas in initializer - but too clunky. And no way to use method references
 * with lambdas on Strategy pattern.
 * 
 * So there is the Governor pattern: a Governor is a source of Command
 * from some abstract class, that also Observes his own shared state
 * and is capable to give that state to the concrete Command.
 * 
 * To clarify:
 *  governor = command | observer
 *  - Good Sign 2017/04/30
 */
class ActionSystem {
    
    interface Governor<R extends Registry & Observer<Registry>> extends Command, Observer<Registry> {

        @Override
        public default Registry obs() {
            return (Registry) this;
        }
    }
    
    private interface Command extends
        ActionsAim,
        ActionsAttacks,
        ActionsCeilings,
        ActionsClipping,
        ActionsDoors,
        ActionsFloors,
        ActionsMissiles,
        ActionsMoveEvents,
        ActionsMovement,
        ActionsPathTraverse,
        ActionsPlanes,
        ActionsSectors,
        ActionsShootEvents,
        ActionsSpawn,
        ActiveStatesAi,
        ActiveStatesAttacks,
        ActiveStatesMonstersBosses,
        ActiveStatesMonstersDemonspawns,
        ActiveStatesMonstersHorrendousVisages,
        ActiveStatesMonstersMancubi,
        ActiveStatesMonstersPainsSouls,
        ActiveStatesMonstersSkels,
        ActiveStatesMonstersSpiders,
        ActiveStatesMonstersViles,
        ActiveStatesMonstersZombies,
        ActiveStatesSounds,
        ActiveStatesThinkers,
        ActiveStatesWeapons,
        ActionsTeleportation,
        ActionsThings,
        ActionsThinkers,
        ActionsUseEvents,
        ActionsUtility
    {}
    
    interface Observer<R> {
        R obs();
    }
    
    private ActionSystem() {}
}
