package f;

import static data.Defines.HU_FONTSIZE;
import static data.Defines.HU_FONTSTART;
import static data.Defines.PU_CACHE;
import static data.Defines.PU_LEVEL;
import static data.Defines.FF_FRAMEMASK;
import static data.Limits.MAXPLAYERS;
import static data.info.mobjinfo;
import static data.info.states;
import static doom.englsh.*;
import static utils.C2JUtils.*;
import hu.HU;
import i.DoomStatusAware;

import java.io.IOException;
import java.util.ArrayList;

import rr.ISpriteManager;
import rr.flat_t;
import rr.patch_t;
import rr.spritedef_t;
import rr.spriteframe_t;
import s.IDoomSound;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import v.IVideoScale;
import v.IVideoScaleAware;
import w.IWadLoader;
import data.mobjtype_t;
import data.sounds.musicenum_t;
import data.state_t;
import defines.*;
import data.sounds.sfxenum_t;
import doom.DoomStatus;
import doom.IDoomGame;
import doom.event_t;
import doom.evtype_t;
import doom.gameaction_t;

// Emacs style mode select   -*- C++ -*- 
//-----------------------------------------------------------------------------
//
// $Id: Finale.java,v 1.28 2012/09/24 17:16:23 velktron Exp $
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
//  Game completion, final screen animation.
//
//-----------------------------------------------------------------------------

public class Finale<T> implements DoomStatusAware, IVideoScaleAware {

	public static final String rcsid = "$Id: Finale.java,v 1.28 2012/09/24 17:16:23 velktron Exp $";

	IDoomGame DG;
	DoomStatus<?,?> DS;
	DoomVideoRenderer<T,?> V;
	IDoomSound S;
	HU HU;
	IWadLoader W;
	ISpriteManager SM;

	int finalestage;

	int finalecount;

	private static int TEXTSPEED = 3;
	private static int TEXTWAIT = 250;

	String[] doom_text = { E1TEXT, E2TEXT, E3TEXT, E4TEXT };

	String[] doom2_text = { C1TEXT, C2TEXT, C3TEXT, C4TEXT, C5TEXT, C6TEXT };

	String[] plut_text = { P1TEXT, P2TEXT, P3TEXT, P4TEXT, P5TEXT, P6TEXT };
	String[] tnt_text = { T1TEXT, T2TEXT, T3TEXT, T4TEXT, T5TEXT, T6TEXT };

	String finaletext;
	String finaleflat;

	/**
	 * F_StartFinale
	 */
	public void StartFinale() {
		DG.setGameAction(gameaction_t.ga_nothing);
		DS.gamestate = gamestate_t.GS_FINALE;
		DS.viewactive = false;
		DS.automapactive = false;
		String[] texts = null;

		// Pick proper text.
		switch (DS.getGameMode()) {
		case commercial:
		case pack_xbla:
			texts = doom2_text;
			break;
		case pack_tnt:
			texts = tnt_text;
			break;
		case pack_plut:
			texts = plut_text;
			break;
		case shareware:
		case registered:
		case retail:
			texts = doom_text;
			break;
		}

		// Okay - IWAD dependend stuff.
		// This has been changed severly, and
		// some stuff might have changed in the process.
		switch (DS.getGameMode()) {

		// DOOM 1 - E1, E3 or E4, but each nine missions
		case shareware:
		case registered:
		case retail: {
			S.ChangeMusic(musicenum_t.mus_victor, true);

			switch (DS.gameepisode) {
			case 1:
				finaleflat = "FLOOR4_8";
				finaletext = texts[0];
				break;
			case 2:
				finaleflat = "SFLR6_1";
				finaletext = texts[1];
				break;
			case 3:
				finaleflat = "MFLR8_4";
				finaletext = texts[2];
				break;
			case 4:
				finaleflat = "MFLR8_3";
				finaletext = texts[3];
				break;
			default:
				// Ouch.
				break;
			}
			break;
		}

			// DOOM II and missions packs with E1, M34
		case commercial:
		case pack_xbla:
		case pack_tnt:
		case pack_plut: {
			S.ChangeMusic(musicenum_t.mus_read_m, true);

			switch (DS.gamemap) {
			case 6:
				finaleflat = "SLIME16";
				finaletext = texts[0];
				break;
			case 11:
				finaleflat = "RROCK14";
				finaletext = texts[1];
				break;
			case 20:
				finaleflat = "RROCK07";
				finaleflat = texts[2];
				break;
			case 30:
				finaleflat = "RROCK17";
				finaletext = texts[3];
				break;
			case 15:
				finaleflat = "RROCK13";
				finaletext = texts[4];
				break;
			case 31:
				finaleflat = "RROCK19";
				finaletext = texts[5];
				break;
			default:
				// Ouch.
				break;
			}
			break;
		}

			// Indeterminate.
		default:
			S.ChangeMusic(musicenum_t.mus_read_m, true);
			finaleflat = "F_SKY1"; // Not used anywhere else.
			finaletext = doom2_text[1];
			; // FIXME - other text, music?
			break;
		}

		finalestage = 0;
		finalecount = 0;

	}

	public boolean Responder(event_t event) {
		if (finalestage == 2)
			return CastResponder(event);

		return false;
	}

	/**
	 * F_Ticker
	 */

	public void Ticker() {
		int i;

		// check for skipping
		if ((DS.isCommercial()) && (finalecount > 50)) {
			// go on to the next level
			for (i = 0; i < MAXPLAYERS; i++)
				if (DS.players[i].cmd.buttons != 0)
					break;

			if (i < MAXPLAYERS) {
				if (DS.gamemap == 30)
					StartCast();
				else
					DG.setGameAction(gameaction_t.ga_worlddone);
			}
		}

		// advance animation
		finalecount++;

		if (finalestage == 2) {
			CastTicker();
			return;
		}

		if (DS.isCommercial())
			return;
		// MAES: this is when we can transition to bunny.
		if ((finalestage == 0)
				&& finalecount > finaletext.length() * TEXTSPEED + TEXTWAIT) {
			finalecount = 0;
			finalestage = 1;
			DS.wipegamestate = gamestate_t.GS_MINUS_ONE; // force a wipe
			if (DS.gameepisode == 3)

				S.StartMusic(musicenum_t.mus_bunny);
		}
	}

	//
	// F_TextWrite
	//

	// #include "hu_stuff.h"
	patch_t[] hu_font;

	@SuppressWarnings("unchecked")
    public void TextWrite() {
		T src;
		//V dest;
		int w;
		int count;
		char[] ch;
		int c;
		int cx;
		int cy;

		// erase the entire screen to a tiled background
		src = (T)((flat_t) W.CacheLumpName(finaleflat, PU_CACHE, flat_t.class)).data;
		//dest = V.getScreen(DoomVideoRenderer.SCREEN_FG);

		for (int y = 0; y < SCREENHEIGHT; y+=64) {
			
			int y_maxdraw=Math.min(SCREENHEIGHT-y, 64);
			
			// Draw whole blocks.
			for (int x = 0; x < SCREENWIDTH; x+=64) {
				int x_maxdraw=Math.min(SCREENWIDTH-x, 64);
				V.DrawBlock(x, y, DoomVideoRenderer.SCREEN_FG, x_maxdraw,y_maxdraw,
						src);
			}
		}

		V.MarkRect(0, 0, SCREENWIDTH, SCREENHEIGHT);

		// draw some of the text onto the screen
		cx = 10;
		cy = 10;
		ch = finaletext.toCharArray();

		count = (finalecount - 10) / TEXTSPEED;
		if (count < 0)
			count = 0;

		// _D_: added min between count and ch.length, so that the text is not
		// written all at once
		for (int i = 0; i < Math.min(ch.length, count); i++)

		{
			c = ch[i];
			if (c == 0)
				break;
			if (c == '\n') {
				cx = 10;
				cy += 11;
				continue;
			}

			c = Character.toUpperCase(c) - HU_FONTSTART;
			if (c < 0 || c > HU_FONTSIZE) {
				cx += 4;
				continue;
			}

			w = hu_font[c].width;
			if (cx + w > SCREENWIDTH)
				break;
			V.DrawScaledPatch(cx, cy, 0, vs, hu_font[c]);
			cx += w;
		}

	}

	private final castinfo_t[] castorder;

	int castnum;
	int casttics;
	state_t caststate;
	boolean castdeath;
	int castframes;
	int castonmelee;
	boolean castattacking;

	//
	// F_StartCast
	//
	// extern gamestate_t wipegamestate;

	public void StartCast() {
		DS.wipegamestate = gamestate_t.GS_MINUS_ONE; // force a screen wipe
		castnum = 0;
		caststate = states[mobjinfo[castorder[castnum].type.ordinal()].seestate
				.ordinal()];
		casttics = (int) caststate.tics;
		castdeath = false;
		finalestage = 2;
		castframes = 0;
		castonmelee = 0;
		castattacking = false;
		S.ChangeMusic(musicenum_t.mus_evil, true);
	}

	//
	// F_CastTicker
	//
	public void CastTicker() {
		statenum_t st;
		sfxenum_t sfx;

		if (--casttics > 0)
			return; // not time to change state yet

		if (caststate.tics == -1 || caststate.nextstate == statenum_t.S_NULL
				|| caststate.nextstate == null) {
			// switch from deathstate to next monster
			castnum++;
			castdeath = false;
			if (castorder[castnum].name == null)
				castnum = 0;
			if (mobjinfo[castorder[castnum].type.ordinal()].seesound.ordinal() != 0)
				;
			S.StartSound(null,
					mobjinfo[castorder[castnum].type.ordinal()].seesound);
			caststate = states[mobjinfo[castorder[castnum].type.ordinal()].seestate
					.ordinal()];
			castframes = 0;
		} else {
			// just advance to next state in animation
			if (caststate == states[statenum_t.S_PLAY_ATK1.ordinal()]) {
				stopattack(); // Oh, gross hack!
				afterstopattack();
				return; // bye ...
			}

			st = caststate.nextstate;
			caststate = states[st.ordinal()];
			castframes++;

			// sound hacks....
			switch (st) {
			case S_PLAY_ATK1:
				sfx = sfxenum_t.sfx_dshtgn;
				break;
			case S_POSS_ATK2:
				sfx = sfxenum_t.sfx_pistol;
				break;
			case S_SPOS_ATK2:
				sfx = sfxenum_t.sfx_shotgn;
				break;
			case S_VILE_ATK2:
				sfx = sfxenum_t.sfx_vilatk;
				break;
			case S_SKEL_FIST2:
				sfx = sfxenum_t.sfx_skeswg;
				break;
			case S_SKEL_FIST4:
				sfx = sfxenum_t.sfx_skepch;
				break;
			case S_SKEL_MISS2:
				sfx = sfxenum_t.sfx_skeatk;
				break;
			case S_FATT_ATK8:
			case S_FATT_ATK5:
			case S_FATT_ATK2:
				sfx = sfxenum_t.sfx_firsht;
				break;
			case S_CPOS_ATK2:
			case S_CPOS_ATK3:
			case S_CPOS_ATK4:
				sfx = sfxenum_t.sfx_shotgn;
				break;
			case S_TROO_ATK3:
				sfx = sfxenum_t.sfx_claw;
				break;
			case S_SARG_ATK2:
				sfx = sfxenum_t.sfx_sgtatk;
				break;
			case S_BOSS_ATK2:
			case S_BOS2_ATK2:
			case S_HEAD_ATK2:
				sfx = sfxenum_t.sfx_firsht;
				break;
			case S_SKULL_ATK2:
				sfx = sfxenum_t.sfx_sklatk;
				break;
			case S_SPID_ATK2:
			case S_SPID_ATK3:
				sfx = sfxenum_t.sfx_shotgn;
				break;
			case S_BSPI_ATK2:
				sfx = sfxenum_t.sfx_plasma;
				break;
			case S_CYBER_ATK2:
			case S_CYBER_ATK4:
			case S_CYBER_ATK6:
				sfx = sfxenum_t.sfx_rlaunc;
				break;
			case S_PAIN_ATK3:
				sfx = sfxenum_t.sfx_sklatk;
				break;
			default:
				sfx = null;
				break;
			}

			if (sfx != null) // Fixed mute thanks to _D_ 8/6/2011
				S.StartSound(null, sfx);
		}

		if (castframes == 12) {
			// go into attack frame
			castattacking = true;
			if (castonmelee != 0)
				caststate = states[mobjinfo[castorder[castnum].type.ordinal()].meleestate
						.ordinal()];
			else
				caststate = states[mobjinfo[castorder[castnum].type.ordinal()].missilestate
						.ordinal()];
			castonmelee ^= 1;
			if (caststate == states[statenum_t.S_NULL.ordinal()]) {
				if (castonmelee != 0)
					caststate = states[mobjinfo[castorder[castnum].type
							.ordinal()].meleestate.ordinal()];
				else
					caststate = states[mobjinfo[castorder[castnum].type
							.ordinal()].missilestate.ordinal()];
			}
		}

		if (castattacking) {
			if (castframes == 24
					|| caststate == states[mobjinfo[castorder[castnum].type
							.ordinal()].seestate.ordinal()])

				stopattack();
		}

		afterstopattack();
	}

	protected void stopattack() {
		castattacking = false;
		castframes = 0;
		caststate = states[mobjinfo[castorder[castnum].type.ordinal()].seestate
				.ordinal()];
	}

	protected void afterstopattack() {
		casttics = (int) caststate.tics;
		if (casttics == -1)
			casttics = 15;
	}

	/**
	 * CastResponder
	 */

	public boolean CastResponder(event_t ev) {
		if (ev.type != evtype_t.ev_keydown)
			return false;

		if (castdeath)
			return true; // already in dying frames

		// go into death frame
		castdeath = true;
		caststate = states[mobjinfo[castorder[castnum].type.ordinal()].deathstate
				.ordinal()];
		casttics = (int) caststate.tics;
		castframes = 0;
		castattacking = false;
		if (mobjinfo[castorder[castnum].type.ordinal()].deathsound != null)
			S.StartSound(null,
					mobjinfo[castorder[castnum].type.ordinal()].deathsound);

		return true;
	}

	public void CastPrint(String text) {
		char[] ch;
		int c;
		int cx;
		int w;
		int width;

		// find width
		ch = text.toCharArray();
		width = 0;

		for (int i = 0; i < ch.length; i++) {
			c = ch[i];
			if (c == 0)
				break;
			c = Character.toUpperCase(c) - HU_FONTSTART;
			if (c < 0 || c > HU_FONTSIZE) {
				width += 4;
				continue;
			}

			w = hu_font[c].width;
			width += w;
		}

		// draw it
		cx = 160 - width / 2;
		// ch = text;
		for (int i = 0; i < ch.length; i++) {
			c = ch[i];
			if (c == 0)
				break;
			c = Character.toUpperCase(c) - HU_FONTSTART;
			if (c < 0 || c > HU_FONTSIZE) {
				cx += 4;
				continue;
			}

			w = hu_font[c].width;
			V.DrawScaledPatch(cx, 180, 0, vs, hu_font[c]);
			cx += w;
		}

	}

	/**
	 * F_CastDrawer
	 * 
	 * @throws IOException
	 */

	// public void V_DrawPatchFlipped (int x, int y, int scrn, patch_t patch);

	public void CastDrawer() {
		spritedef_t sprdef;
		spriteframe_t sprframe;
		int lump;
		boolean flip;
		patch_t patch = null;

		// erase the entire screen to a background
		V.DrawPatchSolidScaled(0, 0, SAFE_SCALE, SAFE_SCALE, 0,
				W.CachePatchName("BOSSBACK", PU_CACHE));

		this.CastPrint(castorder[castnum].name);

		// draw the current frame in the middle of the screen
		sprdef = SM.getSprite(caststate.sprite.ordinal());
		sprframe = sprdef.spriteframes[caststate.frame & FF_FRAMEMASK];
		lump = sprframe.lump[0];
		flip = eval(sprframe.flip[0]);
		// flip=false;
		// lump=0;

		patch = W.CachePatchNum(lump + SM.getFirstSpriteLump());

		if (flip)
			V.DrawScaledPatch(160, 170, 0 | DoomVideoRenderer.V_FLIPPEDPATCH,
					vs, patch);
		else
			V.DrawScaledPatch(160, 170, 0, vs, patch);
	}

	protected int laststage;

	/**
	 * F_BunnyScroll
	 */
	public void BunnyScroll() {
		int scrolled;
		int x;
		patch_t p1;
		patch_t p2;
		String name;
		int stage;

		p1 = W.CachePatchName("PFUB2", PU_LEVEL);
		p2 = W.CachePatchName("PFUB1", PU_LEVEL);

		V.MarkRect(0, 0, SCREENWIDTH, SCREENHEIGHT);

		scrolled = 320 - (finalecount - 230) / 2;
		if (scrolled > 320)
			scrolled = 320;
		if (scrolled < 0)
			scrolled = 0;

		for (x = 0; x < 320; x++) {
			if (x + scrolled < 320)
				V.DrawPatchColScaled(x, p1, x + scrolled, vs, 0);
			else
				V.DrawPatchColScaled(x, p2, x + scrolled - 320, vs, 0);
		}

		if (finalecount < 1130)
			return;
		if (finalecount < 1180) {
			V.DrawScaledPatch((320 - 13 * 8) / 2, (320 - 8 * 8) / 2, 0, vs,
					W.CachePatchName("END0", PU_CACHE));
			laststage = 0;
			return;
		}

		stage = (finalecount - 1180) / 5;
		if (stage > 6)
			stage = 6;
		if (stage > laststage) {
			S.StartSound(null, sfxenum_t.sfx_pistol);
			laststage = stage;
		}

		name = ("END" + stage);
		V.DrawScaledPatch((320 - 13 * 8) / 2, (320 - 8 * 8) / 2, 0, vs,
				W.CachePatchName(name, PU_CACHE));
	}

	//
	// F_Drawer
	//
	public void Drawer() {
		if (finalestage == 2) {
			CastDrawer();
			return;
		}

		if (finalestage == 0)
			TextWrite();
		else {
			switch (DS.gameepisode) {
			case 1:
				if (DS.isCommercial() || DS.isRegistered())
					V.DrawPatchSolidScaled(0, 0, this.SAFE_SCALE,
							this.SAFE_SCALE, 0,
							W.CachePatchName("CREDIT", PU_CACHE));
				else
					// Fun fact: Registered/Ultimate Doom has no "HELP2" lump.
					V.DrawPatchSolidScaled(0, 0, this.SAFE_SCALE,
							this.SAFE_SCALE, 0,
							W.CachePatchName("HELP2", PU_CACHE));
				break;
			case 2:
				V.DrawPatchSolidScaled(0, 0, this.SAFE_SCALE, this.SAFE_SCALE,
						0, W.CachePatchName("VICTORY2", PU_CACHE));
				break;
			case 3:
				BunnyScroll();
				break;
			case 4:
				V.DrawPatchSolidScaled(0, 0, this.SAFE_SCALE, this.SAFE_SCALE,
						0, W.CachePatchName("ENDPIC", PU_CACHE));
				break;
			}
		}

	}

	@SuppressWarnings("unchecked")
	public Finale(DoomStatus DC) {
		this.updateStatus(DC);
		hu_font = HU.getHUFonts();

		castinfo_t shit = new castinfo_t(CC_ZOMBIE, mobjtype_t.MT_POSSESSED);

		ArrayList<castinfo_t> crap = new ArrayList<castinfo_t>();
		crap.add(new castinfo_t(CC_ZOMBIE, mobjtype_t.MT_POSSESSED));
		crap.add(new castinfo_t(CC_SHOTGUN, mobjtype_t.MT_SHOTGUY));
		crap.add(new castinfo_t(CC_HEAVY, mobjtype_t.MT_CHAINGUY));
		crap.add(new castinfo_t(CC_IMP, mobjtype_t.MT_TROOP));
		crap.add(new castinfo_t(CC_DEMON, mobjtype_t.MT_SERGEANT));
		crap.add(new castinfo_t(CC_LOST, mobjtype_t.MT_SKULL));
		crap.add(new castinfo_t(CC_CACO, mobjtype_t.MT_HEAD));
		crap.add(new castinfo_t(CC_HELL, mobjtype_t.MT_KNIGHT));
		crap.add(new castinfo_t(CC_BARON, mobjtype_t.MT_BRUISER));
		crap.add(new castinfo_t(CC_ARACH, mobjtype_t.MT_BABY));
		crap.add(new castinfo_t(CC_PAIN, mobjtype_t.MT_PAIN));
		crap.add(new castinfo_t(CC_REVEN, mobjtype_t.MT_UNDEAD));
		crap.add(new castinfo_t(CC_MANCU, mobjtype_t.MT_FATSO));
		crap.add(new castinfo_t(CC_ARCH, mobjtype_t.MT_VILE));
		crap.add(new castinfo_t(CC_SPIDER, mobjtype_t.MT_SPIDER));
		crap.add(new castinfo_t(CC_CYBER, mobjtype_t.MT_CYBORG));
		crap.add(new castinfo_t(CC_HERO, mobjtype_t.MT_PLAYER));
		crap.add(new castinfo_t(null, null));

		castorder = C2JUtils
				.createArrayOfObjects(castinfo_t.class, crap.size());
		crap.toArray(castorder);

	}

	@Override
	public void updateStatus(DoomStatus<?,?> DC) {
		this.DG = DC.DG;
		this.DS = DC.DM;
		V = (DoomVideoRenderer<T,?>) DC.V;
		S = DC.S;
		HU = DC.HU;
		W = DC.W;
		SM = DC.SM;
	}

	// //////////////////////////VIDEO SCALE STUFF
	// ////////////////////////////////

	protected int SCREENWIDTH;
	protected int SCREENHEIGHT;
	protected int SAFE_SCALE;
	protected IVideoScale vs;

	@Override
	public void setVideoScale(IVideoScale vs) {
		this.vs = vs;
	}

	@Override
	public void initScaling() {
		this.SCREENHEIGHT = vs.getScreenHeight();
		this.SCREENWIDTH = vs.getScreenWidth();
		this.SAFE_SCALE = vs.getSafeScaling();

		// Pre-scale stuff.

	}

}

// /$Log