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

/**
 *
 * @author Good Sign
 */
public class Actions<T, V> extends ActionsRegistry<T, V> implements ActionsRegistry.Observer<T, V>,
    ActionsAim<T, V>,
    ActionsAttacks<T, V>,
    ActionsCeilings<T, V>,
    ActionsClipping<T, V>,
    ActionsDoors<T, V>,
    ActionsFloors<T, V>,
    ActionsMissiles<T, V>,
    ActionsMoveEvents<T, V>,
    ActionsMovement<T, V>,
    ActionsPathTraverse<T, V>,
    ActionsPlanes<T, V>,
    ActionsSectors<T, V>,
    ActionsShootEvents<T, V>,
    ActionsSpawn<T, V>,
    ActionsTeleportation<T, V>,
    ActionsThings<T, V>,
    ActionsThinkers<T, V>,
    ActionsUseEvents<T, V>,
    ActionsUtility<T, V>
    {
    
    public Actions(DoomMain<T, V> DOOM) {
        super(DOOM);
    }

    @Override
    public ActionsRegistry<T, V> obs() {
        return this;
    }

    @Override
    public mobj_t CreateMobj() {
        return new mobj_t(this);
    }
}
