package st;

// Emacs style mode select -*- C++ -*-
// -----------------------------------------------------------------------------
//
// $Id: StatusBar.java,v 1.47 2011/11/01 23:46:37 velktron Exp $
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// DESCRIPTION:
// Status bar code.
// Does the face/direction indicator animatin.
// Does palette indicators as well (red pain/berserk, bright pickup)
//
// -----------------------------------------------------------------------------

import defines.*;
import static data.Limits.MAXPLAYERS;
import static g.Keys.*;
import static data.Defines.*;
import static doom.englsh.*;
import static automap.IAutoMap.*;
import static doom.player_t.*;
import static doom.items.*;
import static data.Tables.*;
import p.mobj_t;
import m.cheatseq_t;
import data.sounds.musicenum_t;
import doom.DoomStatus;
import doom.event_t;
import doom.evtype_t;
import doom.player_t;
import doom.weapontype_t;
import rr.patch_t;
import v.IVideoScale;
import static v.DoomVideoRenderer.*;

public class StatusBar extends AbstractStatusBar   {
    public static final String rcsid =
        "$Id: StatusBar.java,v 1.47 2011/11/01 23:46:37 velktron Exp $";

   
    
    // Size of statusbar.
    // Now sensitive for scaling.

    //
    // STATUS BAR DATA
    //

    // Palette indices.
    // For damage/bonus red-/gold-shifts
    private static int STARTREDPALS = 1;

    private static int STARTBONUSPALS = 9;

    private static int NUMREDPALS = 8;

    private static int NUMBONUSPALS = 4;

    // Radiation suit, green shift.
    private static int RADIATIONPAL = 13;

    // N/256*100% probability
    // that the normal face state will change
    private static int ST_FACEPROBABILITY = 96;

    // For Responder
    private static int ST_TOGGLECHAT = KEY_ENTER;

    // Location of status bar
    private int ST_X = 0;
    private int ST_X2;
    private int ST_FX;
    private int ST_FY;

    // Should be set to patch width
    // for tall numbers later on
    // TODO: private static int ST_TALLNUMWIDTH = (tallnum[0].width);

    // Number of status faces.
    private static int ST_NUMPAINFACES = 5;

    private static int ST_NUMSTRAIGHTFACES = 3;

    private static int ST_NUMTURNFACES = 2;

    private static int ST_NUMSPECIALFACES = 3;

    private static int ST_FACESTRIDE =
        (ST_NUMSTRAIGHTFACES + ST_NUMTURNFACES + ST_NUMSPECIALFACES);

    private static int ST_NUMEXTRAFACES = 2;

    private static int ST_NUMFACES =
        (ST_FACESTRIDE * ST_NUMPAINFACES + ST_NUMEXTRAFACES);

    private static int ST_TURNOFFSET = (ST_NUMSTRAIGHTFACES);

    private static int ST_OUCHOFFSET = (ST_TURNOFFSET + ST_NUMTURNFACES);

    private static int ST_EVILGRINOFFSET = (ST_OUCHOFFSET + 1);

    private static int ST_RAMPAGEOFFSET = (ST_EVILGRINOFFSET + 1);

    private static int ST_GODFACE = (ST_NUMPAINFACES * ST_FACESTRIDE);

    private static int ST_DEADFACE = (ST_GODFACE + 1);

    private int ST_FACESX;

    private int ST_FACESY;

    private static int ST_EVILGRINCOUNT = (2 * TICRATE);

    private static int ST_STRAIGHTFACECOUNT = (TICRATE / 2);

    private static int ST_TURNCOUNT = (1 * TICRATE);

    private static int ST_OUCHCOUNT = (1 * TICRATE);

    private static int ST_RAMPAGEDELAY = (2 * TICRATE);

    private static int ST_MUCHPAIN = 20;

    // Location and size of statistics,
    // justified according to widget type.
    // Problem is, within which space? STbar? Screen?
    // Note: this could be read in by a lump.
    // Problem is, is the stuff rendered
    // into a buffer,
    // or into the frame buffer?

    // AMMO number pos.
    private int ST_AMMOWIDTH;
    private int ST_AMMOX;
    private int ST_AMMOY;

    // HEALTH number pos.
    private int ST_HEALTHWIDTH=3;
    private int ST_HEALTHX;
    private int ST_HEALTHY;

    // Weapon pos.
    private int ST_ARMSX;
    private int ST_ARMSY;
    private int ST_ARMSBGX;
    private int ST_ARMSBGY;
    private int ST_ARMSXSPACE;
    private int ST_ARMSYSPACE;

    // Frags pos.
    private int ST_FRAGSX;
    private int ST_FRAGSY;
    private int ST_FRAGSWIDTH;

    // ARMOR number pos.
    private int ST_ARMORWIDTH = 3;

    private int ST_ARMORX;

    private int ST_ARMORY;

    // Key icon positions.
    private int ST_KEY0WIDTH;
    private int ST_KEY0HEIGHT;
    private int ST_KEY0X;
    private int ST_KEY0Y;

    private int ST_KEY1WIDTH;
    private int ST_KEY1X;
    private int ST_KEY1Y;

    private int ST_KEY2WIDTH;
    private int ST_KEY2X;
    private int ST_KEY2Y;

    // Ammunition counter.
    private int ST_AMMO0WIDTH;
    private int ST_AMMO0HEIGHT;

    private int ST_AMMO0X;
    private int ST_AMMO0Y;

    private int ST_AMMO1WIDTH;

    private int ST_AMMO1X;

    private int ST_AMMO1Y;

    private int ST_AMMO2WIDTH;

    private int ST_AMMO2X;

    private int ST_AMMO2Y;

    private int ST_AMMO3WIDTH;

    private int ST_AMMO3X;

    private int ST_AMMO3Y;

    // Indicate maximum ammunition.
    // Only needed because backpack exists.
    private int ST_MAXAMMO0WIDTH;

    private int ST_MAXAMMO0HEIGHT;

    private int ST_MAXAMMO0X;

    private int ST_MAXAMMO0Y;

    private int ST_MAXAMMO1WIDTH;

    private int ST_MAXAMMO1X;

    private int ST_MAXAMMO1Y;

    private int ST_MAXAMMO2WIDTH;

    private int ST_MAXAMMO2X;

    private int ST_MAXAMMO2Y;

    private int ST_MAXAMMO3WIDTH;

    private int ST_MAXAMMO3X;
    private int ST_MAXAMMO3Y;
    
    // pistol
    private int ST_WEAPON0X;
    private int ST_WEAPON0Y;

    // shotgun
    private int ST_WEAPON1X;
    private int ST_WEAPON1Y;

    // chain gun
    private int ST_WEAPON2X;
    private int ST_WEAPON2Y;

    // missile launcher
    private int ST_WEAPON3X;
    private int ST_WEAPON3Y;

    // plasma gun
    private int ST_WEAPON4X;
    private int ST_WEAPON4Y;

    // bfg
    private int ST_WEAPON5X;
    private int ST_WEAPON5Y;

    // WPNS title
    private int ST_WPNSX;
    private int ST_WPNSY;

    // DETH title
    private int ST_DETHX;
    private int ST_DETHY;

    // Incoming messages window location
    // UNUSED
    // #define ST_MSGTEXTX (viewwindowx)
    // #define ST_MSGTEXTY (viewwindowy+viewheight-18)
    private static int ST_MSGTEXTX = 0;

    private static int ST_MSGTEXTY = 0;

    // Dimensions given in characters.
    private static int ST_MSGWIDTH = 52;

    // Or shall I say, in lines?
    private static int ST_MSGHEIGHT = 1;

    private static int ST_OUTTEXTX = 0;

    private static int ST_OUTTEXTY = 6;

    // Width, in characters again.
    private static int ST_OUTWIDTH = 52;

    // Height, in lines.
    private static int ST_OUTHEIGHT = 1;

    // TODO private static int ST_MAPWIDTH =
    // (mapnames[(gameepisode-1)*9+(gamemap-1)].length));

    // TODO private static int ST_MAPTITLEX = (SCREENWIDTH - ST_MAPWIDTH *
    // ST_CHATFONTWIDTH);

    private static int ST_MAPTITLEY = 0;

    private static int ST_MAPHEIGHT = 1;

    // main player in game
    private player_t plyr;

    // ST_Start() has just been called, OR we want to force an redraw anyway.
    private boolean st_firsttime;
    
    @Override
    public void forceRefresh(){
        st_firsttime=true;
    }

    // used to execute ST_Init() only once
    private int veryfirsttime = 1;

    // lump number for PLAYPAL
    private int lu_palette;

    // used for timing (unsigned int .. maybe long !)
    private long st_clock;

    // used for making messages go away
    int st_msgcounter = 0;

    // used when in chat
    private st_chatstateenum_t st_chatstate;

    // whether in automap or first-person
    private st_stateenum_t st_gamestate;

    /** whether left-side main status bar is active. This fugly hax
     *  (and others like it) are necessary in order to have something
     *  close to a pointer.
     */
    private boolean[] st_statusbaron={false};

    // whether status bar chat is active
    private boolean st_chat;

    // value of st_chat before message popped up
    private boolean st_oldchat;

    // whether chat window has the cursor on
    private boolean[] st_cursoron={false};

    /** !deathmatch */
    private boolean[] st_notdeathmatch={true};

    /** !deathmatch && st_statusbaron */
    private boolean[] st_armson={true};

    /** !deathmatch */
    private boolean[] st_fragson={false};

    // main bar left
    private patch_t sbar;

    // 0-9, tall numbers
    private patch_t[] tallnum = new patch_t[10];

    // tall % sign
    private patch_t tallpercent;

    // 0-9, short, yellow (,different!) numbers
    private patch_t[] shortnum = new patch_t[10];

    // 3 key-cards, 3 skulls
    private patch_t[] keys = new patch_t[NUMCARDS];

    // face status patches
    private patch_t[] faces = new patch_t[ST_NUMFACES];

    // face background
    private patch_t faceback;

    // main bar right
    private patch_t armsbg;

    // weapon ownership patches
    private patch_t[][] arms = new patch_t[6][2];
   
    // // WIDGETS /////

    // ready-weapon widget
    private st_number_t w_ready;

    // in deathmatch only, summary of frags stats
    private st_number_t w_frags;

    // health widget
    private st_percent_t w_health;

    // arms background
    private st_binicon_t w_armsbg;

    // weapon ownership widgets
    private st_multicon_t[] w_arms = new st_multicon_t[6];

    // face status widget
    private st_multicon_t w_faces;

    // keycard widgets
    private st_multicon_t[] w_keyboxes = new st_multicon_t[3];

    // armor widget
    private st_percent_t w_armor;

    // ammo widgets
    private st_number_t[] w_ammo = new st_number_t[4];

    // max ammo widgets
    private st_number_t[] w_maxammo = new st_number_t[4];

    // / END WIDGETS ////

    // number of frags so far in deathmatch
    private int[] st_fragscount={0};

    // used to use appopriately pained face
    private int st_oldhealth = -1;

    // used for evil grin
    private boolean[] oldweaponsowned = new boolean[NUMWEAPONS];

    // count until face changes
    private int st_facecount = 0;

    // current face index, used by w_faces
    private int[] st_faceindex = new int[1];

    // holds key-type for each key box on bar
    private int[] keyboxes = new int[3];

    // a random number per tick
    private int st_randomnumber;
    
    // idmypos toggle mode
    private boolean st_idmypos=false;

    // Massive bunches of cheat shit
    // to keep it from being easy to figure them out.
    // Yeah, right...
    private char cheat_mus_seq[] =
        { 0xb2, 0x26, 0xb6, 0xae, 0xea, 1, 0, 0, 0xff };

    private char cheat_choppers_seq[] =
        { 0xb2, 0x26, 0xe2, 0x32, 0xf6, 0x2a, 0x2a, 0xa6, 0x6a, 0xea, 0xff // id...
        };

    private char cheat_god_seq[] = { 0xb2, 0x26, 0x26, 0xaa, 0x26, 0xff // iddqd
        };

    private char cheat_ammo_seq[] = { 0xb2, 0x26, 0xf2, 0x66, 0xa2, 0xff // idkfa
        };

    private char cheat_ammonokey_seq[] = { 0xb2, 0x26, 0x66, 0xa2, 0xff // idfa
        };

    // Smashing Pumpkins Into Samml Piles Of Putried Debris.
    private char cheat_noclip_seq[] = { 0xb2, 0x26, 0xea, 0x2a, 0xb2, // idspispopd
            0xea, 0x2a, 0xf6, 0x2a, 0x26, 0xff };

    //
    private char cheat_commercial_noclip_seq[] =
        { 0xb2, 0x26, 0xe2, 0x36, 0xb2, 0x2a, 0xff // idclip
        };

    private char cheat_powerup_seq[][] =
        { { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0x6e, 0xff }, // beholdv
                { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0xea, 0xff }, // beholds
                { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0xb2, 0xff }, // beholdi
                { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0x6a, 0xff }, // beholdr
                { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0xa2, 0xff }, // beholda
                { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0x36, 0xff }, // beholdl
                { 0xb2, 0x26, 0x62, 0xa6, 0x32, 0xf6, 0x36, 0x26, 0xff } // behold
        };

    private char cheat_clev_seq[] =
        { 0xb2, 0x26, 0xe2, 0x36, 0xa6, 0x6e, 1, 0, 0, 0xff // idclev
        };

    // my position cheat
    private char cheat_mypos_seq[] =
        { 0xb2, 0x26, 0xb6, 0xba, 0x2a, 0xf6, 0xea, 0xff // idmypos
        };

    // Now what?
    cheatseq_t cheat_mus = new cheatseq_t(cheat_mus_seq, 0);

    cheatseq_t cheat_god = new cheatseq_t(cheat_god_seq, 0);

    cheatseq_t cheat_ammo = new cheatseq_t(cheat_ammo_seq, 0);

    cheatseq_t cheat_ammonokey = new cheatseq_t(cheat_ammonokey_seq, 0);

    cheatseq_t cheat_noclip = new cheatseq_t(cheat_noclip_seq, 0);

    cheatseq_t cheat_commercial_noclip =
        new cheatseq_t(cheat_commercial_noclip_seq, 0);

    cheatseq_t[] cheat_powerup =
        { new cheatseq_t(cheat_powerup_seq[0], 0),
                new cheatseq_t(cheat_powerup_seq[1], 0),
                new cheatseq_t(cheat_powerup_seq[2], 0),
                new cheatseq_t(cheat_powerup_seq[3], 0),
                new cheatseq_t(cheat_powerup_seq[4], 0),
                new cheatseq_t(cheat_powerup_seq[5], 0),
                new cheatseq_t(cheat_powerup_seq[6], 0) };

    cheatseq_t cheat_choppers = new cheatseq_t(cheat_choppers_seq, 0);

    cheatseq_t cheat_clev = new cheatseq_t(cheat_clev_seq, 0);

    cheatseq_t cheat_mypos = new cheatseq_t(cheat_mypos_seq, 0);
    
    cheatseq_t cheat_tnthom= new cheatseq_t("tnthom",false);

    // 
    String[] mapnames;

    //
    // STATUS BAR CODE
    //

    public StatusBar(DoomStatus DC) {
    	this.updateStatus(DC);
    	//this.plyr=DM.players[DM.]
    }

    public void refreshBackground() {

        if (st_statusbaron[0]) {
            V.DrawPatchSolidScaled(ST_X, 0, SAFE_SCALE, SAFE_SCALE, BG, sbar);
            //V.DrawPatch(ST_X, 0, BG, sbar);

            if (DM.netgame)
                V.DrawScaledPatch(ST_FX, 0, BG,vs, faceback);
                //V.DrawPatch(ST_FX, 0, BG, faceback);

            // Buffers the background.
            V.CopyRect(ST_X, 0, BG, ST_WIDTH, ST_HEIGHT, ST_X, ST_Y, FG);
        }

    }

    public void Init() {
        veryfirsttime = 0;
        loadData();
        // MAES: screen(4) of the Video Renderer is actually reserved for the status bar.
        // The "clean" status bar is cached in there, and redrawn only as required.
        
        this.V.setScreen(BG,ST_WIDTH,ST_HEIGHT);
    }

    protected boolean st_stopped = true;

    public void Start() {

        if (!st_stopped)
            Stop();

        initData();
        createWidgets();
        st_stopped = false;

    }

    public void Stop() {
        if (st_stopped)
            return;
        // Reset palette.
        VI.SetPalette (0);

        st_stopped = true;
    }

    public void loadData() {
        lu_palette = W.GetNumForName("PLAYPAL");
        loadGraphics();
    }

    // Respond to keyboard input events,
    // intercept cheats.

    public boolean Responder(event_t ev) {
        int i;

        // Filter automap on/off.
        if (ev.type == evtype_t.ev_keyup
                && ((ev.data1 & 0xffff0000) == AM_MSGHEADER)) {
            switch (ev.data1) {
            case AM_MSGENTERED:
                st_gamestate = st_stateenum_t.AutomapState;
                st_firsttime = true;
                break;

            case AM_MSGEXITED:
                // fprintf(stderr, "AM exited\n");
                st_gamestate = st_stateenum_t.FirstPersonState;
                break;
            }
        }

        // if a user keypress...
        else if (ev.type == evtype_t.ev_keydown) {
            if (!DM.netgame) {
                // b. - enabled for more debug fun.
                // if (gameskill != sk_nightmare) {

                // 'dqd' cheat for toggleable god mode
                if (cheat_god.CheckCheat((char) ev.data1)) {
                    plyr.cheats ^= CF_GODMODE;
                    if ((plyr.cheats & CF_GODMODE) != 0) {
                        if (plyr.mo != null)
                            plyr.mo.health = 100;

                        plyr.health[0] = 100;
                        plyr.message = STSTR_DQDON;
                    } else
                        plyr.message = STSTR_DQDOFF;
                }
                // 'fa' cheat for killer fucking arsenal
                else if (cheat_ammonokey.CheckCheat((char) ev.data1)) {
                    plyr.armorpoints[0] = 200;
                    plyr.armortype = 2;

                    for (i = 0; i < NUMWEAPONS; i++)
                        plyr.weaponowned[i] = true; // true

                    for (i = 0; i < NUMAMMO; i++)
                        plyr.ammo[i] = plyr.maxammo[i];

                    plyr.message = STSTR_FAADDED;
                }
                // 'kfa' cheat for key full ammo
                else if (cheat_ammo.CheckCheat((char) ev.data1)) {
                    plyr.armorpoints[0] = 200;
                    plyr.armortype = 2;

                    for (i = 0; i < NUMWEAPONS; i++)
                        plyr.weaponowned[i] = true; // true

                    for (i = 0; i < NUMAMMO; i++)
                        plyr.ammo[i] = plyr.maxammo[i];

                    for (i = 0; i < NUMCARDS; i++)
                        plyr.cards[i] = true;

                    plyr.message = STSTR_KFAADDED;
                }
                // 'mus' cheat for changing music
                else if (cheat_mus.CheckCheat((char) ev.data1)) {

                    char[] buf = new char[3];
                    int musnum;

                    plyr.message = STSTR_MUS;
                    cheat_mus.GetParam(buf);

                    if (DM.isCommercial()) {
                        musnum =
                            musicenum_t.mus_runnin.ordinal() + (buf[0] - '0')
                                    * 10 + buf[1] - '0' - 1;

                        if (((buf[0] - '0') * 10 + buf[1] - '0') > 35)
                            plyr.message = STSTR_NOMUS;
                        else
                        S.ChangeMusic(musnum, true);
                    } else {
                        musnum =
                            musicenum_t.mus_e1m1.ordinal() + (buf[0] - '1') * 9
                                    + (buf[1] - '1');

                        if (((buf[0] - '1') * 9 + buf[1] - '1') > 31)
                            plyr.message = STSTR_NOMUS;
                        else
                       S.ChangeMusic(musnum, true);
                    }
                }
                // Simplified, accepting both "noclip" and "idspispopd".
                // no clipping mode cheat
                else if (cheat_noclip.CheckCheat((char) ev.data1)
                        || cheat_commercial_noclip.CheckCheat((char) ev.data1)) {
                    plyr.cheats ^= CF_NOCLIP;

                    if ((plyr.cheats & CF_NOCLIP) != 0)
                        plyr.message = STSTR_NCON;
                    else
                        plyr.message = STSTR_NCOFF;
                }
                // 'behold?' power-up cheats
                for (i = 0; i < 6; i++) {
                    if (cheat_powerup[i].CheckCheat((char) ev.data1)) {
                        if (plyr.powers[i] == 0)
                           plyr.GivePower(i);
                        else if (i != pw_strength)
                            plyr.powers[i] = 1;
                        else
                            plyr.powers[i] = 0;

                        plyr.message = STSTR_BEHOLDX;
                    }
                }

                // 'behold' power-up menu
                if (cheat_powerup[6].CheckCheat((char) ev.data1)) {
                    plyr.message = STSTR_BEHOLD;
                }
                // 'choppers' invulnerability & chainsaw
                else if (cheat_choppers.CheckCheat((char) ev.data1)) {
                    plyr.weaponowned[weapontype_t.wp_chainsaw.ordinal()] = true;
                    plyr.powers[pw_invulnerability] = 1; // true
                    plyr.message = STSTR_CHOPPERS;
                }
                // 'mypos' for player position
                else if (cheat_mypos.CheckCheat((char) ev.data1)) {
                    // MAES: made into a toggleable cheat.
                   this.st_idmypos=!st_idmypos;
                }
                else if (cheat_tnthom.CheckCheat((char) ev.data1)) {
                    // MAES: made into a toggleable cheat.
                	plyr.message = (DM.flashing_hom = !DM.flashing_hom) ? "HOM Detection On" :
                	    "HOM Detection Off";
                }
            }

            // 'clev' change-level cheat
            if (cheat_clev.CheckCheat((char) ev.data1)) {
                char[] buf = new char[3];
                int epsd;
                int map;

                cheat_clev.GetParam(buf);

                // This applies to Doom II, Plutonia and TNT.
                if (DM.isCommercial())
                		{
                    epsd = 0;
                    map = (buf[0] - '0') * 10 + buf[1] - '0';
                } else {
                    epsd = buf[0] - '0';
                    map = buf[1] - '0';
                }

                // Catch invalid maps.
                if (epsd < 1 && (!DM.isCommercial()))
                    return false;

                if (map < 1)
                    return false;

                // Ohmygod - this is not going to work.
                if (DM.isRetail()
                        && ((epsd > 4) || (map > 9)))
                    return false;

                // MAES: If it's doom.wad but not ultimate
                if (DM.isRegistered()&& !DM.isRetail()
                        && ((epsd > 3) || (map > 9)))
                    return false;

                if (DM.isShareware()
                        && ((epsd > 1) || (map > 9)))
                    return false;

                if (DM.isCommercial()
                        && ((epsd > 1) || (map > 34)))
                    return false;

                // So be it.
                plyr.message = STSTR_CLEV;
                DM.DeferedInitNew(DM.gameskill, epsd, map);
            }
        }
        return false;
    }

    protected int lastcalc;

    protected int oldhealth = -1;

    public int calcPainOffset() {
        int health = 0;

        health = plyr.health[0] > 100 ? 100 : plyr.health[0];

        if (health != oldhealth) {
            lastcalc =
                ST_FACESTRIDE * (((100 - health) * ST_NUMPAINFACES) / 101);
            oldhealth = health;
        }
        return lastcalc;
    }

    protected int lastattackdown = -1;

    protected int priority = 0;

    /**
     * This is a not-very-pretty routine which handles the face states and their
     * timing. the precedence of expressions is: dead > evil grin > turned head
     * > straight ahead
     */
    public void updateFaceWidget() {
        long badguyangle; // angle_t
        long diffang;

        boolean doevilgrin;

        if (priority < 10) {
            // dead
            if (plyr.health[0] == 0) {
                priority = 9;
                st_faceindex[0] = ST_DEADFACE;
                st_facecount = 1;
            }
        }

        if (priority < 9) {
            if (plyr.bonuscount != 0) {
                // picking up bonus
                doevilgrin = false;

                for (int i = 0; i < NUMWEAPONS; i++) {
                    if (oldweaponsowned[i] != plyr.weaponowned[i]) {
                        doevilgrin = true;
                        oldweaponsowned[i] = plyr.weaponowned[i];
                    }
                }
                if (doevilgrin) {
                    // evil grin if just picked up weapon
                    priority = 8;
                    st_facecount = ST_EVILGRINCOUNT;
                    st_faceindex[0] = calcPainOffset() + ST_EVILGRINOFFSET;
                }
            }

        }

        if (priority < 8) {
            if ((plyr.damagecount != 0) && (plyr.attacker != null)
                    && (plyr.attacker != plyr.mo)) {
                // being attacked
                priority = 7;

                if (plyr.health[0] - st_oldhealth > ST_MUCHPAIN) {
                    st_facecount = ST_TURNCOUNT;
                    st_faceindex[0] = calcPainOffset() + ST_OUCHOFFSET;
                } else {
                    badguyangle =
                        R.PointToAngle2(plyr.mo.x, plyr.mo.y, plyr.attacker.x,
                            plyr.attacker.y);
                    boolean obtuse; // that's another "i"

                    if (badguyangle > plyr.mo.angle) {
                        // whether right or left
                        diffang = badguyangle - plyr.mo.angle;
                        obtuse = diffang > ANG180;
                    } else {
                        // whether left or right
                        diffang = plyr.mo.angle - badguyangle;
                        obtuse = diffang <= ANG180;
                    } // confusing, aint it?

                    st_facecount = ST_TURNCOUNT;
                    st_faceindex[0] = calcPainOffset();

                    if (diffang < ANG45) {
                        // head-on
                        st_faceindex[0] += ST_RAMPAGEOFFSET;
                    } else if (obtuse) {
                        // turn face right
                        st_faceindex[0] += ST_TURNOFFSET;
                    } else {
                        // turn face left
                        st_faceindex[0] += ST_TURNOFFSET + 1;
                    }
                }
            }
        }

        if (priority < 7) {
            // getting hurt because of your own damn stupidity
            if (plyr.damagecount != 0) {
                if (plyr.health[0] - st_oldhealth > ST_MUCHPAIN) {
                    priority = 7;
                    st_facecount = ST_TURNCOUNT;
                    st_faceindex[0] = calcPainOffset() + ST_OUCHOFFSET;
                } else {
                    priority = 6;
                    st_facecount = ST_TURNCOUNT;
                    st_faceindex[0] = calcPainOffset() + ST_RAMPAGEOFFSET;
                }

            }

        }

        if (priority < 6) {
            // rapid firing
            if (plyr.attackdown) {
                if (lastattackdown == -1)
                    lastattackdown = ST_RAMPAGEDELAY;
                else if (--lastattackdown == 0) {
                    priority = 5;
                    st_faceindex[0] = calcPainOffset() + ST_RAMPAGEOFFSET;
                    st_facecount = 1;
                    lastattackdown = 1;
                }
            } else
                lastattackdown = -1;

        }

        if (priority < 5) {
            // invulnerability
            if (((plyr.cheats & CF_GODMODE) != 0)
                    || (plyr.powers[pw_invulnerability] != 0)) {
                priority = 4;

                st_faceindex[0] = ST_GODFACE;
                st_facecount = 1;

            }

        }

        // look left or look right if the facecount has timed out
        if (st_facecount == 0) {
            st_faceindex[0] = calcPainOffset() + (st_randomnumber % 3);
            st_facecount = ST_STRAIGHTFACECOUNT;
            priority = 0;
        }

        st_facecount--;

    }

    protected int largeammo = 1994; // means "n/a"

    /**
     * MAES: this code updated the widgets. Now, due to the way they are
     * constructed, they originally were "hooked" to actual variables using
     * pointers so that they could tap into them directly and self-update.
     * Clearly we can't do that in Java unless said variables are inside an
     * array and we provide both the array AND an index. For other cases, we
     * must simply build ad-hoc hacks.
     * 
     * In any case, only "status" updates are performed here. Actual visual
     * updates are performed by the Drawer.
     * 
     */

    public void updateWidgets() {

        int i;

        // MAES: sticky idmypos cheat that is actually useful
        // TODO: this spams the player message queue at every tic.
        // A direct overlay with a widget would be more useful.
        
        if (this.st_idmypos){
            mobj_t mo = DM.players[DM.consoleplayer].mo;
            plyr.message = String.format("ang= 0x%x; x,y= (%x, %x)",
                        (int)mo.angle,mo.x,mo.y);

        }
        

        // must redirect the pointer if the ready weapon has changed.
        // if (w_ready.data != plyr.readyweapon)
        // {
        if (weaponinfo[plyr.readyweapon.ordinal()].ammo == ammotype_t.am_noammo)
            w_ready.numindex = largeammo;
        else
            w_ready.numindex =
                weaponinfo[plyr.readyweapon.ordinal()].ammo.ordinal();

        w_ready.data = plyr.readyweapon.ordinal();

        // if (*w_ready.on)
        // STlib_updateNum(&w_ready, true);
        // refresh weapon change
        // }

        // update keycard multiple widgets
        for (i = 0; i < 3; i++) {
            keyboxes[i] = plyr.cards[i] ? i : -1;

            if (plyr.cards[i + 3])
                keyboxes[i] = i + 3;
        }

        // refresh everything if this is him coming back to life
        updateFaceWidget();

        // used by the w_armsbg widget
        st_notdeathmatch[0] = !DM.deathmatch;

        // used by w_arms[] widgets
        st_armson[0] = st_statusbaron[0] && !(DM.altdeath||DM.deathmatch);

        // used by w_frags widget
        st_fragson[0] = (DM.altdeath||DM.deathmatch) && st_statusbaron[0];
        st_fragscount[0] = 0;

        for (i = 0; i < MAXPLAYERS; i++) {
            if (i != DM.consoleplayer)
                st_fragscount[0] += plyr.frags[i];
            else
                st_fragscount[0] -= plyr.frags[i];
        }

        // get rid of chat window if up because of message
        if (--st_msgcounter == 0)
            st_chat = st_oldchat;

    }

    public void Ticker() {

        st_clock++;
        st_randomnumber = RND.M_Random();
        updateWidgets();
        st_oldhealth = plyr.health[0];

    }

    static int st_palette = 0;

    public void doPaletteStuff() {

        int palette;
        //byte[] pal;
        int cnt;
        int bzc;

        cnt = plyr.damagecount;

        if (plyr.powers[pw_strength] != 0) {
            // slowly fade the berzerk out
            bzc = 12 - (plyr.powers[pw_strength] >> 6);

            if (bzc > cnt)
                cnt = bzc;
        }

        if (cnt != 0) {
            palette = (cnt + 7) >> 3;

            if (palette >= NUMREDPALS)
                palette = NUMREDPALS - 1;

            palette += STARTREDPALS;
        }

        else if (plyr.bonuscount != 0) {
            palette = (plyr.bonuscount + 7) >> 3;

            if (palette >= NUMBONUSPALS)
                palette = NUMBONUSPALS - 1;

            palette += STARTBONUSPALS;
        }

        else if (plyr.powers[pw_ironfeet] > 4 * 32
                || (plyr.powers[pw_ironfeet] & 8) != 0)
            palette = RADIATIONPAL;
        else
            palette = 0;

        if (palette != st_palette) {
            st_palette = palette;
            VI.SetPalette (palette);
        }

    }

    public void drawWidgets(boolean refresh) {
        int i;

        // used by w_arms[] widgets
        st_armson[0] = st_statusbaron[0] && !(DM.altdeath||DM.deathmatch);

        // used by w_frags widget
        st_fragson[0] = DM.deathmatch && st_statusbaron[0];

        w_ready.update(refresh);

        for (i = 0; i < 4; i++) {
            w_ammo[i].update(refresh);
            w_maxammo[i].update(refresh);
        }

        w_health.update(refresh);
        w_armor.update(refresh);

        w_armsbg.update(refresh);

        for (i = 0; i < 6; i++)
            w_arms[i].update(refresh);

        w_faces.update(refresh);

        for (i = 0; i < 3; i++)
            w_keyboxes[i].update(refresh);

        w_frags.update(refresh);

    }

    public void doRefresh() {

        st_firsttime = false;

        // draw status bar background to off-screen buff
        refreshBackground();

        // and refresh all widgets
        drawWidgets(true);

    }

    public void diffDraw() {
        // update all widgets
        drawWidgets(false);
    }

    public void Drawer(boolean fullscreen, boolean refresh) {

        st_statusbaron[0] = (!fullscreen) || DM.automapactive;
        st_firsttime = st_firsttime || refresh;

        // Do red-/gold-shifts from damage/items
        doPaletteStuff();

        // If just after ST_Start(), refresh all
        if (st_firsttime)
            doRefresh();
        // Otherwise, update as little as possible
        else
            diffDraw();

    }

    public void loadGraphics() {

        int i;
        int j;
        int facenum;

        String namebuf;

        // Load the numbers, tall and short
        for (i = 0; i < 10; i++) {
            namebuf = ("STTNUM" + i);
            tallnum[i] = W.CachePatchName(namebuf, PU_STATIC);

            namebuf = ("STYSNUM" + i);
            shortnum[i] = W.CachePatchName(namebuf, PU_STATIC);

        }

        // Load percent key.
        // Note: why not load STMINUS here, too?
        tallpercent = W.CachePatchName("STTPRCNT", PU_STATIC);
        // MAES: in fact, I do this for sanity. Fuck them. Seriously.
        sttminus= W.CachePatchName("STTMINUS");

        // key cards
        for (i = 0; i < NUMCARDS; i++) {
            namebuf = ("STKEYS" + i);
            keys[i] = W.CachePatchName(namebuf, PU_STATIC);
        }

        // arms background
        armsbg = W.CachePatchName("STARMS", PU_STATIC);

        // arms ownership widgets
        for (i = 0; i < 6; i++) {
            namebuf = ("STGNUM" + (i + 2));

            // gray #
            arms[i][0] = W.CachePatchName(namebuf, PU_STATIC);

            // yellow #
            arms[i][1] = shortnum[i + 2];
        }

        // face backgrounds for different color players
        namebuf = ("STFB" + DM.consoleplayer);
        faceback = W.CachePatchName(namebuf, PU_STATIC);

        // status bar background bits
        sbar = W.CachePatchName("STBAR", PU_STATIC);

        // face states
        facenum = 0;
        for (i = 0; i < ST_NUMPAINFACES; i++) {
            for (j = 0; j < ST_NUMSTRAIGHTFACES; j++) {
                namebuf = ("STFST" + (i) + (j));
                faces[facenum++] = W.CachePatchName(namebuf, PU_STATIC);
            }
            namebuf = "STFTR" + i + "0"; // turn right
            faces[facenum++] = W.CachePatchName(namebuf, PU_STATIC);
            namebuf = "STFTL" + i + "0"; // turn left
            faces[facenum++] = W.CachePatchName(namebuf, PU_STATIC);
            namebuf = "STFOUCH" + i; // ouch!
            faces[facenum++] = W.CachePatchName(namebuf, PU_STATIC);
            namebuf = "STFEVL" + i; // evil grin ;)
            faces[facenum++] = W.CachePatchName(namebuf, PU_STATIC);
            namebuf = "STFKILL" + i; // pissed off
            faces[facenum++] = W.CachePatchName(namebuf, PU_STATIC);
        }
        faces[facenum++] = W.CachePatchName("STFGOD0", PU_STATIC);
        faces[facenum++] = W.CachePatchName("STFDEAD0", PU_STATIC);

    }

    public void unloadGraphics() {
    	
          int i; // unload the numbers, tall and short 
          for (i=0;i<10;i++) {
        	  W.UnlockLumpNum(tallnum[i]);
        	  tallnum[i]=null;
        	  W.UnlockLumpNum(shortnum[i]);
        	  shortnum[i]=null;
          }
        
       // unload tall percent
          W.UnlockLumpNum(tallpercent);
          tallpercent=null;
          	
        	  
         // unload arms background          
          W.UnlockLumpNum(armsbg);
          armsbg=null;
         // unload gray #'s          
          for (i=0;i<6;i++) { 
        	  W.UnlockLumpNum(arms[i][0]);
        	  arms[i][0]=null;
        	  W.UnlockLumpNum(arms[i][1]);
        	  arms[i][1]=null;

          }
          
          // unload the key cards for (i=0;i<NUMCARDS;i++)
          
          for (i=0;i<6;i++) { 
        	  W.UnlockLumpNum(keys[i]);
        	  keys[i]=null;
          }
          
          W.UnlockLumpNum(sbar);
          sbar=null;
          
          W.UnlockLumpNum(faceback);
          faceback=null;
          
           for (i=0;i<ST_NUMFACES;i++){
        	   W.UnlockLumpNum(faces[i]);
        	   faces[i]=null;
           	}
         

        // Note: nobody ain't seen no unloading
        // of stminus yet. Dude.

    }

    public void unloadData() {
        unloadGraphics();
    }

    public void initData() {

        int i;

        st_firsttime = true;
        plyr = DM.players[DM.consoleplayer];

        st_clock = 0;
        st_chatstate = st_chatstateenum_t.StartChatState;
        st_gamestate = st_stateenum_t.FirstPersonState;

        st_statusbaron[0] = true;
        st_oldchat = st_chat = false;
        st_cursoron[0] = false;

        st_faceindex[0] = 0;
        st_palette = -1;

        st_oldhealth = -1;

        for (i = 0; i < NUMWEAPONS; i++)
            oldweaponsowned[i] = plyr.weaponowned[i];

        for (i = 0; i < 3; i++)
            keyboxes[i] = -1;

        Init();

    }

    /**
     * Widgets are created here. Be careful, because their "constructors" used
     * reference to boolean or int variables so that they could be auto-updated
     * by the global refresh functions. We can only do this with some
     * limitations in Java (e.g. passing an array AND an index).
     */

    public void createWidgets() {

        int i;

        // ready weapon ammo

        w_ready =
            new st_number_t(ST_AMMOX, ST_AMMOY, tallnum, plyr.ammo,
                    weaponinfo[plyr.readyweapon.ordinal()].ammo.ordinal(),
                    st_statusbaron, 0,ST_AMMOWIDTH);

        // the last weapon type
        w_ready.data = plyr.readyweapon.ordinal();

        // health percentage
        w_health =
            new st_percent_t(ST_HEALTHX, ST_HEALTHY, tallnum, plyr.health,
                    0, st_statusbaron,0, tallpercent);

        // arms background
        w_armsbg =
            new st_binicon_t(ST_ARMSBGX, ST_ARMSBGY, armsbg, st_notdeathmatch,0,
                    st_statusbaron,0);

        // weapons owned
        for (i = 0; i < 6; i++) {
            w_arms[i] =
                new st_multicon_t(ST_ARMSX + (i % 3) * ST_ARMSXSPACE, ST_ARMSY
                        + (i / 3) * ST_ARMSYSPACE, arms[i], plyr.weaponowned,
                        i + 1, st_armson,0);
        }

        // frags sum
        w_frags =
            new st_number_t(ST_FRAGSX, ST_FRAGSY, tallnum, st_fragscount, 0, // dummy,
                                                                             // we're
                                                                             // passing
                                                                             // an
                                                                             // integer.
                    st_fragson,0, ST_FRAGSWIDTH);

        // faces
        w_faces =
            new st_multicon_t(ST_FACESX, ST_FACESY, faces, st_faceindex, 0,
                    st_statusbaron,0);

        // armor percentage - should be colored later
        w_armor =
            new st_percent_t(ST_ARMORX, ST_ARMORY, tallnum, plyr.armorpoints,
                    0, st_statusbaron, 0,tallpercent);

        // keyboxes 0-2
        w_keyboxes[0] =
            new st_multicon_t(ST_KEY0X, ST_KEY0Y, keys, keyboxes, 0,
                    st_statusbaron,0);

        w_keyboxes[1] =
            new st_multicon_t(ST_KEY1X, ST_KEY1Y, keys, keyboxes, 1,
                    st_statusbaron,0);

        w_keyboxes[2] =
            new st_multicon_t(ST_KEY2X, ST_KEY2Y, keys, keyboxes, 2,
                    st_statusbaron,0);

        // ammo count (all four kinds)

        w_ammo[0] =
            new st_number_t(ST_AMMO0X, ST_AMMO0Y, shortnum, plyr.ammo, 0,
                    st_statusbaron,0, ST_AMMO0WIDTH);

        w_ammo[1] =
            new st_number_t(ST_AMMO1X, ST_AMMO1Y, shortnum, plyr.ammo, 1,
                    st_statusbaron,0, ST_AMMO1WIDTH);

        w_ammo[2] =
            new st_number_t(ST_AMMO2X, ST_AMMO2Y, shortnum, plyr.ammo, 2,
                    st_statusbaron,0, ST_AMMO2WIDTH);

        w_ammo[3] =
            new st_number_t(ST_AMMO3X, ST_AMMO3Y, shortnum, plyr.ammo, 3,
                    st_statusbaron,0, ST_AMMO3WIDTH);

        // max ammo count (all four kinds)
        w_maxammo[0] =
            new st_number_t(ST_MAXAMMO0X, ST_MAXAMMO0Y, shortnum, plyr.maxammo,
                    0, st_statusbaron,0, ST_MAXAMMO0WIDTH);

        w_maxammo[1] =
            new st_number_t(ST_MAXAMMO1X, ST_MAXAMMO1Y, shortnum, plyr.maxammo,
                    1, st_statusbaron,0, ST_MAXAMMO1WIDTH);

        w_maxammo[2] =
            new st_number_t(ST_MAXAMMO2X, ST_MAXAMMO2Y, shortnum, plyr.maxammo,
                    2, st_statusbaron,0, ST_MAXAMMO2WIDTH);

        w_maxammo[3] =
            new st_number_t(ST_MAXAMMO3X, ST_MAXAMMO3Y, shortnum, plyr.maxammo,
                    3, st_statusbaron,0, ST_MAXAMMO3WIDTH);

    }

    /** Binary Icon widget 
     *  This is used for stuff such as keys or weapons, which you either have
     *  or you don't.
     * 
     * */

    class st_binicon_t
            implements StatusBarWidget {

        // center-justified location of icon
        int x;

        int y;

        // last icon value
        boolean oldval;

        /** pointer to current icon status */
        boolean[] val;
        int valindex;

        /** pointer to boolean
            stating whether to update icon */
        boolean[] on;
        int onindex;
        
        patch_t p; // icon

        int data; // user data

        // Binary Icon widget routines

        public st_binicon_t(int x, int y, patch_t i, boolean[] val, int valindex, boolean[] on, int onindex) {
            this.x = x;
            this.y = y;
            this.oldval = false;
            this.val = val;
            this.valindex=valindex;
            this.on = on;
            this.onindex=onindex;
            this.p = i;
            this.val[valindex]=false;;
        }

        @Override
        public void update(boolean refresh) {
            st_binicon_t bi = this;
            int x;
            int y;
            int w;
            int h;

            if (bi.on[onindex] && ((bi.oldval != bi.val[valindex]) || refresh)) {
                x = bi.x - bi.p.leftoffset;
                y = bi.y - bi.p.topoffset;
                w = bi.p.width;
                h = bi.p.height;

                if (y - ST_Y < 0)
                    I.Error("updateBinIcon: y - ST_Y < 0");                    
                if (bi.val[valindex])
                    V.DrawScaledPatch(bi.x, bi.y, V_PREDIVIDE|FG,vs, bi.p);
                else
                    V.CopyRect(x/vs.getScalingX(), y/vs.getScalingY() - ST_Y, BG, w*BEST_X_SCALE, h*BEST_Y_SCALE, x, y, FG);

                bi.oldval = bi.val[valindex];
            }

        }

    }

    /** Icon widget */

    class st_multicon_t
            implements StatusBarWidget {

        // center-justified location of icons
        int x;

        int y;

        // last icon number
        int oldinum;

        /** pointer to current icon, if not an array type. */
        int[] iarray;

        int inum;

        // pointer to boolean stating
        // whether to update icon
        boolean[] on;
        int onindex;

        // list of icons
        patch_t[] p;

        // user data
        int data;

        /** special status 0=boolean[] 1=integer[] -1= unspecified */
        int status = -1;

        protected boolean[] asboolean;

        protected int[] asint;

        public st_multicon_t(int x, int y, patch_t[] il, Object iarray,
                int inum, boolean []on,int onindex) {
            this.x = x;
            this.y = y;
            this.oldinum = -1;
            this.inum = inum;
            this.on = on;
            this.p = il;
            if (iarray instanceof boolean[]) {
                status = 0;
                asboolean = (boolean[]) iarray;
            } else 
                if (iarray instanceof int[]){
                    status = 1;
                asint = (int[]) iarray;               
            }  
        }

        @Override
        public void update(boolean refresh) {

            int w;
            int h;
            int x;
            int y;

            // Actual value to be considered. Poor man's generics!
            int thevalue = -1;
            switch (status) {
            case 0:
                thevalue = asboolean[inum] ? 1 : 0;
                break;
            case 1:
                thevalue = asint[inum];
                break;            
            }

            // Unified treatment of boolean and integer references
            // So the widget will update iff:
            // a) It's on AND
            // b) The new value is different than the old one
            // c) Neither of them is -1
            // d) We actually asked for a refresh.
            if (this.on[onindex] && ((this.oldinum != thevalue) || refresh)
                    && (thevalue != -1)) {
            	// Previous value must not have been -1.
                if (this.oldinum != -1) { 
                    x = this.x - this.p[this.oldinum].leftoffset*BEST_X_SCALE;
                    y = this.y - this.p[this.oldinum].topoffset*BEST_Y_SCALE;
                    w = this.p[this.oldinum].width*BEST_X_SCALE;
                    h = this.p[this.oldinum].height*BEST_Y_SCALE;

                    if (y - ST_Y < 0)
                        I.Error("updateMultIcon: y - ST_Y < 0");
                    //System.out.printf("Restoring at x y %d %d w h %d %d\n",x, y - ST_Y,w,h);
                    V.CopyRect(x, y - ST_Y, BG, w, h, x, y, FG);
                    //V.FillRect(x, y - ST_Y, w, h, FG);
                }
                
                //System.out.printf("Drawing at x y %d %d w h %d %d\n",this.x,this.y,p[thevalue].width,p[thevalue].height);
                V.DrawScaledPatch(this.x,this.y, V_SCALEOFFSET|V_NOSCALESTART|FG, vs,this.p[thevalue]);
                
                this.oldinum = thevalue;
            }
        }
    }

    protected patch_t sttminus;

    /** Number widget */

    class st_number_t
            implements StatusBarWidget {

        /** upper right-hand corner of the number (right-justified) */
        int x, y;

        /** max # of digits in number */
        int width;

        /** last number value */
        int oldnum;

        /**
         * Array in which to point with num. 
         * 
         * Fun fact: initially I tried to use Integer and Boolean, but those are
         * immutable -_-. Fuck that, Java.
         * 
         */
        int[] numarray;

        /** pointer to current value. Of course makes sense only for arrays. */
        int numindex;

        /** pointer to boolean stating whether to update number */
        boolean[] on;
        int onindex;

        /** list of patches for 0-9 */
        patch_t[] p;

        /** user data */
        int data;

        // Number widget routines

        public st_number_t(int x, int y, patch_t[] pl, int[] numarray,
                int numindex, boolean[] on,int onindex, int width) {
                init(x, y, pl, numarray, numindex, on,onindex, width);
                    }

        public void init(int x, int y, patch_t[] pl, int[] numarray,int numindex,
                boolean[] on, int onindex, int width) {
            this.x = x;
            this.y = y;
            this.oldnum = 0;
            this.width = width;
            this.numarray = numarray;
            this.on = on;
            this.onindex=onindex;
            this.p = pl;
            this.numindex=numindex; // _D_ fixed this bug
        }

        // 
        // A fairly efficient way to draw a number
        // based on differences from the old number.
        // Note: worth the trouble?
        //
        public void drawNum(boolean refresh) {

            if (this.numindex==largeammo) return;
            
            //st_number_t n = this;
            int numdigits = this.width; // HELL NO. This only worked while the width happened
            							// to be 3.
            int num = ((int[]) this.numarray)[this.numindex];

            int w = this.p[0].width*BEST_X_SCALE;
            int h = this.p[0].height*BEST_Y_SCALE;
            int x = this.x;

            boolean neg;

            // In this way, num and oldnum are exactly the same. Maybe this
            // should go in the end?
            this.oldnum = num;

            neg = num < 0;

            if (neg) {
                if (numdigits == 2 && num < -9)
                    num = -9;
                else if (numdigits == 3 && num < -99)
                    num = -99;

                num = -num;
            }

            // clear the area
            x = this.x - numdigits * w;

            if (this.y - ST_Y < 0) {
                I.Error("drawNum: n.y - ST_Y < 0");
            }

            // Restore BG from buffer
            //V.FillRect(x+(numdigits-3) * w, y, w*3 , h, FG);
            V.CopyRect(x+(numdigits-3)*w, y- ST_Y, BG, w * 3, h, x+(numdigits-3)*w, y, FG);

            // if non-number, do not draw it
            if (num == 1994)
                return;

            x = this.x;

            // in the special case of 0, you draw 0
            if (num == 0)
                //V.DrawPatch(x - w, n.y, FG, n.p[0]);
                V.DrawScaledPatch(x - w, this.y, FG|V_NOSCALESTART|V_TRANSLUCENTPATCH, vs,p[0]);
                
                
            // draw the new number
            while (((num != 0) && (numdigits-- != 0))) {
                x -= w;
                //V.DrawPatch(x, n.y, FG, n.p[num % 10]);
                V.DrawScaledPatch(x, this.y, FG|V_NOSCALESTART|V_TRANSLUCENTPATCH,vs, p[num % 10]);
                num /= 10;
            }

            // draw a minus sign if necessary
            if (neg)
                V.DrawScaledPatch/*DrawPatch*/(x - 8*BEST_X_SCALE, this.y, FG|V_NOSCALESTART|V_TRANSLUCENTPATCH,vs, sttminus);
                //V.DrawPatch(x - sttminus.width*vs.getScalingX(), n.y, FG, sttminus);
        }

        @Override
        public void update(boolean refresh) {
            if (this.on[onindex])
                drawNum(refresh);
        }

    }

    class st_percent_t
            implements StatusBarWidget {

        // Percent widget ("child" of number widget,
        // or, more precisely, contains a number widget.)
        // number information
        st_number_t n;

        // percent sign graphic
        patch_t p;

        public st_percent_t(int x, int y, patch_t[] pl, int[] numarray,
                int numindex, boolean[] on, int onindex, patch_t percent) {
            n = new st_number_t(x, y, pl, numarray, numindex, on,onindex, 3);
            p = percent;
        }

        @Override
        public void update(boolean refresh) {
            if (refresh && this.n.on[0])
                V.DrawScaledPatch(n.x, n.y, V_PREDIVIDE|FG,vs, p);

            n.update(refresh);
        }

    }

    interface StatusBarWidget {
        public void update(boolean refresh);
    }
	
	// Size of statusbar.
	// Now sensitive for scaling.
	public int ST_HEIGHT;
	public int ST_WIDTH;
	public int ST_Y;

////////////////////////////VIDEO SCALE STUFF ////////////////////////////////

	protected int SCREENWIDTH;
	protected int SCREENHEIGHT;
	protected int SAFE_SCALE;
	protected int BEST_X_SCALE;
	protected int BEST_Y_SCALE;
	protected IVideoScale vs;


	@Override
	public void setVideoScale(IVideoScale vs) {
	    this.vs=vs;
	}

	@Override
	public void initScaling() {
	    SCREENHEIGHT=vs.getScreenHeight();
	    SCREENWIDTH=vs.getScreenWidth();
	    SAFE_SCALE=vs.getSafeScaling();
	    BEST_X_SCALE=vs.getScalingX();
	    BEST_Y_SCALE=vs.getScalingY();
	    
	    // Pre-scale stuff.
	    ST_HEIGHT =32*SAFE_SCALE;
	    ST_WIDTH  =SCREENWIDTH;
	    ST_Y      =(SCREENHEIGHT - ST_HEIGHT);
	    ST_X2 = (int) (104*SAFE_SCALE);
	    ST_FX = (int) (143*SAFE_SCALE);
	    ST_FY = (int) (169*SAFE_SCALE);
	    ST_FACESX = (int) (143*SAFE_SCALE);

	    ST_FACESY = (int) (168*SAFE_SCALE);
	    
	     // AMMO number pos.
	     ST_AMMOWIDTH= 3;	    
	     ST_AMMOX = (int) (44*SAFE_SCALE);
	     ST_AMMOY = (int) (171*SAFE_SCALE);

	     // HEALTH number pos
	     ST_HEALTHWIDTH= 3;
	     ST_HEALTHX = (int) (90*SAFE_SCALE);
	     ST_HEALTHY = (int) (171*SAFE_SCALE);

	    // Weapon pos.
	     ST_ARMSX = (int) (111*SAFE_SCALE);
	     ST_ARMSY = (int) (172*SAFE_SCALE);
	     ST_ARMSBGX = (int) (104*SAFE_SCALE);
	     ST_ARMSBGY = (int) (168*SAFE_SCALE);
	     ST_ARMSXSPACE = 12*SAFE_SCALE;;
	     ST_ARMSYSPACE = 10*SAFE_SCALE;;
	     
	     // Frags pos.
	     ST_FRAGSX = (int) (138*SAFE_SCALE);
	     ST_FRAGSY = (int) (171*SAFE_SCALE);
	     ST_FRAGSWIDTH=2;
	     
	     //
	     

	     
	     ST_ARMORX = (int) (221*SAFE_SCALE);

	     ST_ARMORY = (int) (171*SAFE_SCALE);

	     // Key icon positions.
	     ST_KEY0WIDTH = 8*SAFE_SCALE;;
	     ST_KEY0HEIGHT = 5*SAFE_SCALE;;
	     
	     ST_KEY0X = (int) (239*SAFE_SCALE);
	     ST_KEY0Y = (int) (171*SAFE_SCALE);

	     ST_KEY1WIDTH = ST_KEY0WIDTH;
	     ST_KEY1X = (int) (239*SAFE_SCALE);
	     ST_KEY1Y = (int) (181*SAFE_SCALE);

	     ST_KEY2WIDTH = ST_KEY0WIDTH;
	     ST_KEY2X = (int) (239*SAFE_SCALE);
	     ST_KEY2Y = (int) (191*SAFE_SCALE);

	    // Ammunition counter.
	    ST_AMMO0WIDTH = 3*SAFE_SCALE;
	    ST_AMMO0HEIGHT = 6*SAFE_SCALE;

	     ST_AMMO0X = (int) (288*SAFE_SCALE);

	     ST_AMMO0Y = (int) (173*SAFE_SCALE);

	    ST_AMMO1WIDTH = ST_AMMO0WIDTH;

	     ST_AMMO1X = (int) (288*SAFE_SCALE);

	     ST_AMMO1Y = (int) (179*SAFE_SCALE);

	    ST_AMMO2WIDTH = ST_AMMO0WIDTH;

	     ST_AMMO2X = (int) (288*SAFE_SCALE);

	     ST_AMMO2Y = (int) (191*SAFE_SCALE);

	    ST_AMMO3WIDTH = ST_AMMO0WIDTH;

	     ST_AMMO3X = (int) (288*SAFE_SCALE);

	     ST_AMMO3Y = (int) (185*SAFE_SCALE);

	    // Indicate maximum ammunition.
	    // Only needed because backpack exists.
	    ST_MAXAMMO0WIDTH = 3*SAFE_SCALE;
	    ST_MAXAMMO0HEIGHT = 5*SAFE_SCALE;

	     ST_MAXAMMO0X = (int) (314*SAFE_SCALE);
	     ST_MAXAMMO0Y = (int) (173*SAFE_SCALE);

	    ST_MAXAMMO1WIDTH = ST_MAXAMMO0WIDTH;
	    ST_MAXAMMO1X = 314*SAFE_SCALE;
	     ST_MAXAMMO1Y = (int) (179*SAFE_SCALE);

	    ST_MAXAMMO2WIDTH = ST_MAXAMMO0WIDTH;
	     ST_MAXAMMO2X = (int) (314*SAFE_SCALE);
	     ST_MAXAMMO2Y = (int) (191*SAFE_SCALE);

	    ST_MAXAMMO3WIDTH = ST_MAXAMMO0WIDTH;
	     ST_MAXAMMO3X = (int) (314*SAFE_SCALE);
	     ST_MAXAMMO3Y = (int) (185*SAFE_SCALE);

	    // pistol
	     ST_WEAPON0X = (int) (110*SAFE_SCALE);
	     ST_WEAPON0Y = (int) (172*SAFE_SCALE);

	    // shotgun
	     ST_WEAPON1X = (int) (122*SAFE_SCALE);
	     ST_WEAPON1Y = (int) (172*SAFE_SCALE);

	    // chain gun
	     ST_WEAPON2X = (int) (134*SAFE_SCALE);

	     ST_WEAPON2Y = (int) (172*SAFE_SCALE);

	    // missile launcher
	     ST_WEAPON3X = (int) (110*SAFE_SCALE);

	     ST_WEAPON3Y = (int) (181*SAFE_SCALE);

	    // plasma gun
	     ST_WEAPON4X = (int) (122*SAFE_SCALE);

	     ST_WEAPON4Y = (int) (181*SAFE_SCALE);

	    // bfg
	     ST_WEAPON5X = (int) (134*SAFE_SCALE);

	     ST_WEAPON5Y = (int) (181*SAFE_SCALE);

	    // WPNS title
	     ST_WPNSX = (int) (109*SAFE_SCALE);

	     ST_WPNSY = (int) (191*SAFE_SCALE);

	    // DETH title
	     ST_DETHX = (int) (109*SAFE_SCALE);

	     ST_DETHY = (int) (191*SAFE_SCALE);

	    
	}

    @Override
    public int getHeight() {
        return this.ST_HEIGHT;
    }

	
}

//$Log: StatusBar.java,v $
//Revision 1.47  2011/11/01 23:46:37  velktron
//Added TNTHOM cheat.
//
//Revision 1.46  2011/10/23 15:57:08  velktron
//BG reference
//
//Revision 1.45  2011/10/07 16:07:14  velktron
//Now using g.Keys for key input stuff.
//
//Revision 1.44  2011/08/23 16:15:30  velktron
//Got rid of Z remnants.
//
//Revision 1.43  2011/07/28 10:29:46  velktron
//Added hack for forcing full redraw of status bar after help screen drawing.
//
//Revision 1.42  2011/07/22 15:37:16  velktron
//Sticky idmypos cheat
//
//Revision 1.41  2011/06/23 17:17:04  velktron
//Using BG constant.
//
//Revision 1.40  2011/06/13 21:03:48  velktron
//Fixed Ultimate Doom clev bug
//
//Revision 1.39  2011/06/02 14:20:45  velktron
//Implemented unloading code....kind of pointless, really.
//
//Revision 1.38  2011/06/01 18:13:37  velktron
//Fixed idmypos crash.
//
//Revision 1.37  2011/06/01 00:07:26  velktron
//Moved status, fixed soundinterface.
//
//Revision 1.36  2011/05/31 13:32:03  velktron
//Added V_SCALEOFFSET flag to doomguy's mug.
//
//Revision 1.35  2011/05/31 12:39:31  velktron
//Face centering fixed.
//
//Revision 1.34  2011/05/30 15:47:34  velktron
//AbstractStatusBar introduced.
//
//Revision 1.33  2011/05/30 10:34:20  velktron
//Fixed binicon refresh bug...
//
//Revision 1.32  2011/05/30 02:21:08  velktron
//Fixed number widget diffdraw
//
//Revision 1.31  2011/05/29 22:15:32  velktron
//Introduced IRandom interface.
//
//Revision 1.30  2011/05/29 20:54:43  velktron
//Fixed status bar scaling
//
//Revision 1.29  2011/05/24 13:42:22  velktron
//Fidgeting around with the STBar refresh
//
//Revision 1.28  2011/05/24 11:31:23  velktron
//Got rid of a whole bunch of useless interfaces.
//
//Revision 1.27  2011/05/23 16:59:02  velktron
//Migrated to VideoScaleInfo.
//
//Revision 1.26  2011/05/21 15:00:14  velktron
//Adapted to use new gamemode system.
//
//Revision 1.25  2011/05/18 16:57:21  velktron
//Changed to DoomStatus
