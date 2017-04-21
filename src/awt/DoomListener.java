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

package awt;

import java.awt.KeyEventDispatcher;
import java.awt.event.ComponentListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;

/**
 * An interface to combine all needed listeners plus provide method to stop answering other mouse events
 * when it is moving and a function to process all queued events
 * 
 * @author Good Sign
 */
public interface DoomListener extends
        WindowListener,
        ComponentListener,
        KeyListener,
        MouseListener,
        MouseMotionListener,
        WindowFocusListener,
        KeyEventDispatcher
{
}