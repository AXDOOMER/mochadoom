package st;
// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: IDoomStatusBar.java,v 1.4 2012/09/24 17:16:23 velktron Exp $
//
// Copyright (C) 1993-1996 by id Software, Inc.
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// DESCRIPTION:
//	Status bar code.
//	Does the face/direction indicator animatin.
//	Does palette indicators as well (red pain/berserk, bright pickup)
//
//-----------------------------------------------------------------------------

import i.DoomStatusAware;
import v.IVideoScaleAware;
import doom.event_t;

public interface IDoomStatusBar extends IVideoScaleAware,DoomStatusAware{

    
    
    /** Points to "screen 4" which is treated as a buffer */
    static final int BG =4;

    /** Points to "screen 0" which is what you actually see */
     static final int FG =0;

    //
    // STATUS BAR
    //

    /** Called by main loop. */
    public boolean Responder (event_t ev);

    /** Called by main loop. */
    public void Ticker ();

    /** Called by main loop.*/
    public void Drawer (boolean fullscreen, boolean refresh);

    /** Called when the console player is spawned on each level. */
    public void Start ();

    /** Called by startup code. */
    public void Init ();

    /** Used externally to determine window scaling. 
     *  This means that drawing transparent status bars is possible, but
     *  it will look fugly because of the solid windowing (and possibly
     *  HOMS).
     */
    public int getHeight();
    
    /** Forces a full refresh for reasons not handled otherwise, e.g. after full-page
     *  draws of help screens, which normally don't trigger a complete redraw even if
     *  they should, really.
     */
    
    void forceRefresh();

}