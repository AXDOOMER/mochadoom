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

import doom.DoomMain;
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
	static class GoverningRegistry extends Registry implements Governor<GoverningRegistry> {
		GoverningRegistry(DoomMain<?, ?> DOOM) {
			super(DOOM);
		}
	}
	
	interface Governor<R extends GoverningRegistry> extends Command {
		@Override
		default GoverningRegistry obs() {
			return (GoverningRegistry) this;
		}
	}
	
	interface Command extends AbstractCommand<GoverningRegistry> {}
	
    interface AbstractCommand<R extends Registry & AbstractCommand<R>> extends
        ActionsAim<R>,
        ActionsAttacks<R>,
        ActionsCeilings<R>,
        ActionsClipping<R>,
        ActionsDoors<R>,
        ActionsFloors<R>,
        ActionsMissiles<R>,
        ActionsMoveEvents<R>,
        ActionsMovement<R>,
        ActionsPathTraverse<R>,
        ActionsPlanes<R>,
        ActionsSectors<R>,
        ActionsShootEvents<R>,
        ActionsSpawn<R>,
        ActiveStatesAi<R>,
        ActiveStatesAttacks<R>,
        ActiveStatesMonstersBosses<R>,
        ActiveStatesMonstersDemonspawns<R>,
        ActiveStatesMonstersHorrendousVisages<R>,
        ActiveStatesMonstersMancubi<R>,
        ActiveStatesMonstersPainsSouls<R>,
        ActiveStatesMonstersSkels<R>,
        ActiveStatesMonstersSpiders<R>,
        ActiveStatesMonstersViles<R>,
        ActiveStatesMonstersZombies<R>,
        ActiveStatesSounds<R>,
        ActiveStatesThinkers<R>,
        ActiveStatesWeapons<R>,
        ActionsTeleportation<R>,
        ActionsThings<R>,
        ActionsThinkers<R>,
        ActionsUseEvents<R>,
        ActionsUtility<R>
    {}
    
    interface Observer<R> {
        R obs();
    }
    
    private ActionSystem() {}
}
