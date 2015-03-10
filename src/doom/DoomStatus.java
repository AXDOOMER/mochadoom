package doom;

import static data.Defines.*;
import static g.Keys.*;
import static data.Limits.*;
import java.io.OutputStreamWriter;

import automap.IAutoMap;
import m.IUseVariables;
import m.IVariablesManager;
import m.Settings;
import p.mobj_t;
import rr.Renderer;
import utils.C2JUtils;
import v.DoomVideoRenderer;
import data.mapthing_t;
import defines.*;
import demo.IDoomDemo;
import f.Finale;
import f.Wiper;

/**
 * We need globally shared data structures, for defining the global state
 * variables. MAES: in pure OO style, this should be a global "Doom state"
 * object to be passed along various modules. No ugly globals here!!! Now, some
 * of the variables that appear here were actually defined in separate modules.
 * Pretty much, whatever needs to be shared with other modules was placed here,
 * either as a local definition, or as an extern share. The very least, I'll
 * document where everything is supposed to come from/reside.
 */

public abstract class DoomStatus<T,V> extends DoomContext<T,V> implements IUseVariables {

	public static final int	BGCOLOR=		7;
	public static final int	FGCOLOR		=8;
	public static int   RESENDCOUNT =10;
	public static int   PL_DRONE    =0x80;  // bit flag in doomdata->player

	public String[]		wadfiles=new String[MAXWADFILES];

	boolean         drone;
	
    /** Command line parametersm, actually defined in d_main.c */
    public boolean nomonsters; // checkparm of -nomonsters

    public boolean respawnparm; // checkparm of -respawn

    public boolean fastparm; // checkparm of -fast

    public boolean devparm; // DEBUG: launched with -devparm

 // MAES: declared as "extern", shared with Menu.java
    public  boolean	inhelpscreens;

    boolean		advancedemo;
    
    /////////// Local to doomstat.c ////////////
    // TODO: hide those behind getters
    
    /** Game Mode - identify IWAD as shareware, retail etc. 
     *  This is now hidden behind getters so some cases like plutonia
     *  etc. can be handled more cleanly.
     * */

    private GameMode_t gamemode;
    
    public void setGameMode(GameMode_t mode){
    	this.gamemode=mode;
    }
    
    public GameMode_t getGameMode(){
    	return gamemode;
    }
    
    public boolean isShareware(){
    	return (gamemode== GameMode_t.shareware);
    }    
    
    /** Commercial means Doom 2, Plutonia, TNT, and possibly others like XBLA.
     * 
     * @return
     */
    public boolean isCommercial(){
    	return (gamemode== GameMode_t.commercial ||
    			gamemode== GameMode_t.pack_plut ||
    			gamemode== GameMode_t.pack_tnt ||
    			gamemode== GameMode_t.pack_xbla);
    }
    
    /** Retail means Ultimate.
     * 
     * @return
     */
    public boolean isRetail(){
    	return (gamemode== GameMode_t.retail );
    }
    
    /** Registered is a subset of Ultimate 
     * 
     * @return
     */

    public boolean isRegistered(){
    	return (gamemode== GameMode_t.registered || gamemode== GameMode_t.retail );
    }
    
    public GameMission_t gamemission;

    /** Set if homebrew PWAD stuff has been added. */
    public boolean modifiedgame;

    /** Language. */
    public Language_t language;

    // /////////// Normally found in d_main.c ///////////////

    // Selected skill type, map etc.

    /** Defaults for menu, methinks. */
    public skill_t startskill;

    public int startepisode;

    public int startmap;

    public boolean autostart;

    /** Selected by user */
    public skill_t gameskill;

    public int gameepisode;

    public int gamemap;

    /** Nightmare mode flag, single player. */
    public boolean respawnmonsters;

    /** Netgame? Only true if >1 player. */
    public boolean netgame;

    /**
     * Flag: true only if started as net deathmatch. An enum might handle
     * altdeath/cooperative better. Use altdeath for the "2" value
     */
    public boolean deathmatch;

    /** Use this instead of "deathmatch=2" which is bullshit. */
    public boolean altdeath;
    
    //////////// STUFF SHARED WITH THE RENDERER ///////////////
    
    // -------------------------
    // Status flags for refresh.
    //

    public boolean nodrawers;

    public boolean noblit;
    
    public boolean viewactive;
    
    // Player taking events, and displaying.
    public int consoleplayer;

    public int displayplayer;
    
    // Depending on view size - no status bar?
    // Note that there is no way to disable the
    // status bar explicitely.
    public boolean statusbaractive;

    public boolean automapactive; // In AutoMap mode?

    public boolean menuactive; // Menu overlayed?

    public boolean paused; // Game Pause?

    // -------------------------
    // Internal parameters for sound rendering.
    // These have been taken from the DOS version,
    // but are not (yet) supported with Linux
    // (e.g. no sound volume adjustment with menu.

    // These are not used, but should be (menu).
    // From m_menu.c:
    // Sound FX volume has default, 0 - 15
    // Music volume has default, 0 - 15
    // These are multiplied by 8.
    /** maximum volume for sound */
    public int snd_SfxVolume;

    /** maximum volume for music */
    public int snd_MusicVolume;

    /** Maximum number of sound channels */
    public int numChannels;

    // Current music/sfx card - index useless
    // w/o a reference LUT in a sound module.
    // Ideally, this would use indices found
    // in: /usr/include/linux/soundcard.h
    public int snd_MusicDevice;

    public int snd_SfxDevice;

    // Config file? Same disclaimer as above.
    public int snd_DesiredMusicDevice;

    public int snd_DesiredSfxDevice;

    
    // -------------------------------------
    // Scores, rating.
    // Statistics on a given map, for intermission.
    //
    public int totalkills;

    public int totalitems;

    public int totalsecret;

    /** TNTHOM "cheat" for flashing HOM-detecting BG */
	public boolean flashing_hom;
    
    // Added for prBoom+ code
    public int totallive;
    
    // Timer, for scores.
    public int levelstarttic; // gametic at level start

    public int leveltime; // tics in game play for par

    // --------------------------------------
    // DEMO playback/recording related stuff.
    // No demo, there is a human player in charge?
    // Disable save/end game?
    public boolean usergame;

    // ?
    public boolean demoplayback;

    public boolean demorecording;

    // Quit after playing a demo from cmdline.
    public boolean singledemo;

    /** Set this to GS_DEMOSCREEN upon init, else it will be null*/
    public gamestate_t gamestate=gamestate_t.GS_DEMOSCREEN;

    // -----------------------------
    // Internal parameters, fixed.
    // These are set by the engine, and not changed
    // according to user inputs. Partly load from
    // WAD, partly set at startup time.

    public int gametic;

    // Bookkeeping on players - state.
    public player_t[] players;

    // Alive? Disconnected?
    public boolean[] playeringame = new boolean[MAXPLAYERS];

    public mapthing_t[] deathmatchstarts = new mapthing_t[MAX_DM_STARTS];

    /** pointer into deathmatchstarts */
    public int deathmatch_p;

    /** Player spawn spots. */
    public mapthing_t[] playerstarts = new mapthing_t[MAXPLAYERS];

    /** Intermission stats.
      Parameters for world map / intermission. */
    public wbstartstruct_t wminfo;

    /** LUT of ammunition limits for each kind.
        This doubles with BackPack powerup item.
        NOTE: this "maxammo" is treated like a global.
         */
    public final static int[] maxammo =  {200, 50, 300, 50};

    // -----------------------------------------
    // Internal parameters, used for engine.
    //

    // File handling stuff.
    
    public OutputStreamWriter debugfile;

    // if true, load all graphics at level load
    public boolean precache;

    // wipegamestate can be set to -1
    // to force a wipe on the next draw
    // wipegamestate can be set to -1 to force a wipe on the next draw
    public gamestate_t     wipegamestate = gamestate_t.GS_DEMOSCREEN;
    
    public int mouseSensitivity=15;
    
    // debug flag to cancel adaptiveness
    // Set to true during timedemos.
    public boolean singletics=false;
    
    /* A "fastdemo" is a demo with a clock that tics as
     * fast as possible, yet it maintains adaptiveness and doesn't
     * try to render everything at all costs.
     */
    protected boolean fastdemo;
    protected boolean normaldemo;
    
    protected String loaddemo;
    
    public int bodyqueslot;

    // Needed to store the number of the dummy sky flat.
    // Used for rendering,
    // as well as tracking projectiles etc.
    //public int skyflatnum;

    // TODO: Netgame stuff (buffers and pointers, i.e. indices).

    // TODO: This is ???
    public doomcom_t doomcom;

    // TODO: This points inside doomcom.
    public doomdata_t netbuffer;

    public ticcmd_t[] localcmds = new ticcmd_t[BACKUPTICS];

    public int rndindex;

    public int maketic;

    public int[] nettics = new int[MAXNETNODES];

    public ticcmd_t[][] netcmds;// [MAXPLAYERS][BACKUPTICS];
      
    /** MAES: this WAS NOT in the original. 
     *  Remember to call it!
     */
    protected void initNetGameStuff() {
        //this.netbuffer = new doomdata_t();
        this.doomcom = new doomcom_t();
        this.netcmds = new ticcmd_t[MAXPLAYERS][BACKUPTICS];

        C2JUtils.initArrayOfObjects(localcmds);
        for (int i=0;i<MAXPLAYERS;i++){
        C2JUtils.initArrayOfObjects(netcmds[i]);
        }
        
    }
    
    public abstract void Init();

    // Fields used for selecting variable BPP implementations.
    
    protected abstract Finale<T> selectFinale();
    
    protected abstract DoomVideoRenderer<T,V> selectVideoRenderer();
    
    protected abstract Renderer<T,V> selectRenderer();
    
    protected abstract Wiper<T,V> selectWiper();
    
    protected abstract IAutoMap<T,V> selectAutoMap();
    
    // MAES: Fields specific to DoomGame. A lot of them were
    // duplicated/externalized
    // in d_game.c and d_game.h, so it makes sense adopting a more unified
    // approach.
    protected gameaction_t gameaction=gameaction_t.ga_nothing;

    public boolean sendpause; // send a pause event next tic

    protected boolean sendsave; // send a save event next tic

    protected int starttime;

    protected boolean timingdemo; // if true, exit with report on completion

    public boolean getPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    // ////////// DEMO SPECIFIC STUFF/////////////

    protected String demoname;

    protected boolean netdemo;

    //protected IDemoTicCmd[] demobuffer;

    protected IDoomDemo demobuffer;
    /** pointers */
    // USELESS protected int demo_p;

   // USELESS protected int demoend;

    protected short[][] consistancy = new short[MAXPLAYERS][BACKUPTICS];

    protected byte[] savebuffer;

    /* TODO Proper reconfigurable controls. Defaults hardcoded for now. T3h h4x, d00d. */

    public int key_right=KEY_RIGHTARROW;

    public int key_left=KEY_LEFTARROW;

    public int key_up='w';

    public int key_down='a';

    public int key_strafeleft='s';

    public int key_straferight='d';

    public int key_fire=KEY_CTRL;

    public int key_use=' ';
    
    public int key_strafe=KEY_ALT;

    public int key_speed=KEY_SHIFT;
    
    // Heretic stuff
    public int		key_lookup=KEY_PGUP;
    public int		key_lookdown=KEY_PGDN;
    public int      key_lookcenter=KEY_END;

    public int mousebfire=0;

    public int mousebstrafe=1;

    public int mousebforward=2;

    public int joybfire;

    public int joybstrafe;

    public int joybuse;

    public int joybspeed;
    
    /** Cancel vertical mouse movement by default */
    protected boolean novert=true;

    protected int MAXPLMOVE() {
        return forwardmove[1];
    }

    protected static final int TURBOTHRESHOLD = 0x32;

    /** fixed_t */
    protected final int[] forwardmove = { 0x19, 0x32 }; // + slow turn

    protected final int[] sidemove = { 0x18, 0x28 };

    protected final int[] angleturn = { 640, 1280, 320 };

    protected static final int SLOWTURNTICS = 6;

    protected static final int NUMKEYS = 256;

    protected boolean[] gamekeydown = new boolean[NUMKEYS];

    protected boolean keysCleared;
    
    public boolean alwaysrun;

    protected int turnheld; // for accelerative turning
    protected int lookheld; // for accelerative looking?

    protected boolean[] mousearray = new boolean[4];

    /** This is an alias for mousearray [1+i] */
    protected boolean mousebuttons(int i) {
        return mousearray[1 + i]; // allow [-1]
    }

    protected void mousebuttons(int i, boolean value) {
        mousearray[1 + i] = value; // allow [-1]
    }

    protected void mousebuttons(int i, int value) {
        mousearray[1 + i] = value != 0; // allow [-1]
    }

    /** mouse values are used once */
    protected int mousex, mousey;

    protected int dclicktime;

    protected int dclickstate;

    protected int dclicks;

    protected int dclicktime2, dclickstate2, dclicks2;

    /** joystick values are repeated */
    protected int joyxmove, joyymove;

    protected boolean[] joyarray = new boolean[5];

    protected boolean joybuttons(int i) {
        return joyarray[1 + i]; // allow [-1]
    }

    protected void joybuttons(int i, boolean value) {
        joyarray[1 + i] = value; // allow [-1]
    }

    protected void joybuttons(int i, int value) {
        joyarray[1 + i] = value != 0; // allow [-1]
    }

    protected int savegameslot;

    protected String savedescription;

    protected static final int BODYQUESIZE = 32;

    protected mobj_t[] bodyque = new mobj_t[BODYQUESIZE];

    public String statcopy; // for statistics driver

    
    
    /** Not documented/used in linuxdoom. I supposed it could be used to
     *  ignore mouse input?
     */
    
    public boolean use_mouse,use_joystick;
    
    
    /** More prBoom+ stuff. Used mostly for code uhm..reuse, rather
     *  than to actually change the way stuff works.
     *   
     */
    
    public static int compatibility_level;
    
    public DoomStatus(){
    	players = new player_t[MAXPLAYERS];
    	C2JUtils.initArrayOfObjects(players);

    	this.wminfo=new wbstartstruct_t();
    	initNetGameStuff();
    }

    @Override
    public void registerVariableManager(IVariablesManager manager) {
        this.VM=manager;        
    }

    @Override
    public void update() {

           this.snd_SfxVolume=VM.getSetting(Settings.sfx_volume).getInteger();
           this.snd_MusicVolume=VM.getSetting(Settings.music_volume).getInteger();
           this.alwaysrun=VM.getSetting(Settings.alwaysrun).getBoolean();
                     
          
           // Keys...
           this.key_right=VM.getSetting(Settings.key_right).getChar();
           this.key_left=VM.getSetting(Settings.key_left).getChar();
           this.key_up=VM.getSetting(Settings.key_up).getChar();
           this.key_down=VM.getSetting(Settings.key_down).getChar();
           this.key_strafeleft=VM.getSetting(Settings.key_strafeleft).getChar();
           this.key_straferight=VM.getSetting(Settings.key_straferight).getChar();
           this.key_fire=VM.getSetting(Settings.key_fire).getChar();
           this.key_use=VM.getSetting(Settings.key_use).getChar();
           this.key_strafe=VM.getSetting(Settings.key_strafe).getChar();
           this.key_speed=VM.getSetting(Settings.key_speed).getChar();
           

           // Mouse buttons
           this.use_mouse=VM.getSetting(Settings.use_mouse).getBoolean();
           this.mousebfire=VM.getSetting(Settings.mouseb_fire).getInteger();
           this.mousebstrafe=VM.getSetting(Settings.mouseb_strafe).getInteger();
           this.mousebforward=VM.getSetting(Settings.mouseb_forward).getInteger();
           
           // Joystick

           this.use_joystick=VM.getSetting(Settings.use_joystick).getBoolean();
           this.joybfire=VM.getSetting(Settings.joyb_fire).getInteger();
           this.joybstrafe=VM.getSetting(Settings.joyb_strafe).getInteger();
           this.joybuse=VM.getSetting(Settings.joyb_use).getInteger();
           this.joybspeed=VM.getSetting(Settings.joyb_speed).getInteger();

           // Sound
           this.numChannels=VM.getSetting(Settings.snd_channels).getInteger();

           // Video...so you should wait until video renderer is active.           
           this.V.setUsegamma(VM.getSetting(Settings.usegamma).getInteger());
           
           // These should really be handled by the menu.
           this.M.setShowMessages(VM.getSetting(Settings.show_messages).getBoolean());
           this.M.setScreenBlocks(VM.getSetting(Settings.screenblocks).getInteger());

           // These should be handled by the HU

           for (int i=0;i<=9;i++){
               
           String chatmacro=String.format("chatmacro%d",i);
           this.HU.setChatMacro(i,VM.getSetting(chatmacro).toString());
           }
        }

    @Override
    public void commit() {
        VM.putSetting(Settings.sfx_volume,this.snd_SfxVolume);
        VM.putSetting(Settings.music_volume,this.snd_MusicVolume);
        VM.putSetting(Settings.alwaysrun, this.alwaysrun);
       
       
        // Keys...
        VM.putSetting(Settings.key_right,this.key_right);
        VM.putSetting(Settings.key_left,this.key_left);
        VM.putSetting(Settings.key_up,this.key_up);
        VM.putSetting(Settings.key_down,this.key_down);
        VM.putSetting(Settings.key_strafeleft,this.key_strafeleft);
        VM.putSetting(Settings.key_straferight,this.key_straferight);
        VM.putSetting(Settings.key_fire,this.key_fire);
        VM.putSetting(Settings.key_use,this.key_use);
        VM.putSetting(Settings.key_strafe,this.key_strafe);
        VM.putSetting(Settings.key_speed,this.key_speed);
        

        // Mouse buttons
        VM.putSetting(Settings.use_mouse,this.use_mouse);
        VM.putSetting(Settings.mouseb_fire,this.mousebfire);
        VM.putSetting(Settings.mouseb_strafe,this.mousebstrafe);
        VM.putSetting(Settings.mouseb_forward,this.mousebforward);
        
        // Joystick

        VM.putSetting(Settings.use_joystick,this.use_joystick);
        VM.putSetting(Settings.joyb_fire,this.joybfire);
        VM.putSetting(Settings.joyb_strafe,this.joybstrafe);
        VM.putSetting(Settings.joyb_use,this.joybuse);
        VM.putSetting(Settings.joyb_speed,this.joybspeed);

        // Sound
        VM.putSetting(Settings.snd_channels,this.numChannels);
        
        // Video...         
        VM.putSetting(Settings.usegamma,V.getUsegamma());
        
        // These should really be handled by the menu.
        VM.putSetting(Settings.show_messages,this.M.getShowMessages());
        VM.putSetting(Settings.screenblocks,this.M.getScreenBlocks());
        
        // These should be handled by the HU

        for (int i=0;i<=9;i++){
            
        String chatmacro=String.format("chatmacro%d",i);
        VM.putSetting(chatmacro,this.HU.chat_macros[i]);
        }
        
    }
    

    

}

// $Log: DoomStatus.java,v $
// Revision 1.36  2012/11/06 16:04:58  velktron
// Variables manager less tightly integrated.
//
// Revision 1.35  2012/09/24 17:16:22  velktron
// Massive merge between HiColor and HEAD. There's no difference from now on, and development continues on HEAD.
//
// Revision 1.34.2.3  2012/09/24 16:58:06  velktron
// TrueColor, Generics.
//
// Revision 1.34.2.2  2012/09/20 14:25:13  velktron
// Unified DOOM!!!
//
// Revision 1.34.2.1  2012/09/17 16:06:52  velktron
// Now handling updates of all variables, though those specific to some subsystems should probably be moved???
//
// Revision 1.34  2011/11/01 23:48:10  velktron
// Added tnthom stuff.
//
// Revision 1.33  2011/10/24 02:11:27  velktron
// Stream compliancy
//
// Revision 1.32  2011/10/07 16:01:16  velktron
// Added freelook stuff, using Keys.
//
// Revision 1.31  2011/09/27 16:01:41  velktron
// -complevel_t
//
// Revision 1.30  2011/09/27 15:54:51  velktron
// Added some more prBoom+ stuff.
//
// Revision 1.29  2011/07/28 17:07:04  velktron
// Added always run hack.
//
// Revision 1.28  2011/07/16 10:57:50  velktron
// Merged finnw's changes for enabling polling of ?_LOCK keys.
//
// Revision 1.27  2011/06/14 20:59:47  velktron
// Channel settings now read from default.cfg. Changes in sound creation order.
//
// Revision 1.26  2011/06/04 11:04:25  velktron
// Fixed registered/ultimate identification.
//
// Revision 1.25  2011/06/01 17:35:56  velktron
// Techdemo v1.4a level. Default novert and experimental mochaevents interface.
//
// Revision 1.24  2011/06/01 00:37:58  velktron
// Changed default keys to WASD.
//
// Revision 1.23  2011/05/31 21:45:51  velktron
// Added XBLA version as explicitly supported.
//
// Revision 1.22  2011/05/30 15:50:42  velktron
// Changed to work with new Abstract classes
//
// Revision 1.21  2011/05/26 17:52:11  velktron
// Now using ICommandLineManager
//
// Revision 1.20  2011/05/26 13:39:52  velktron
// Now using ICommandLineManager
//
// Revision 1.19  2011/05/25 17:56:52  velktron
// Introduced some fixes for mousebuttons etc.
//
// Revision 1.18  2011/05/24 17:44:37  velktron
// usemouse added for defaults
//