package m;

import defines.*;
import static data.Defines.HU_FONTSIZE;
import static data.Defines.HU_FONTSTART;
import static g.Keys.*;
import static data.Defines.PU_CACHE;
import static data.Defines.SAVESTRINGSIZE;
import static data.dstrings.NUM_QUITMESSAGES;
import static data.dstrings.SAVEGAMENAME;
import static data.dstrings.endmsg;
import static doom.englsh.DOSY;
import static doom.englsh.EMPTYSTRING;
import static doom.englsh.ENDGAME;
import static doom.englsh.LOADNET;
import static doom.englsh.MSGOFF;
import static doom.englsh.MSGON;
import static doom.englsh.NETEND;
import static doom.englsh.NEWGAME;
import static doom.englsh.NIGHTMARE;
import static doom.englsh.QLOADNET;
import static doom.englsh.QLPROMPT;
import static doom.englsh.QSAVESPOT;
import static doom.englsh.QSPROMPT;
import static doom.englsh.SAVEDEAD;
import static doom.englsh.SWSTRING;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

import rr.patch_t;
import utils.C2JUtils;
import v.IVideoScale;
import w.DoomIO;
import data.sounds.sfxenum_t;
import doom.DoomStatus;
import doom.englsh;
import doom.event_t;
import doom.evtype_t;

public class Menu extends AbstractDoomMenu {

	////////////////// CONSTRUCTOR ////////////////
    
    public Menu(DoomStatus DS){
    	this.updateStatus(DS);
    }

    /** The fonts  ... must "peg" them to those from HU */
    patch_t[] hu_font = new patch_t[HU_FONTSIZE];

    /** WTF?! */

    boolean message_dontfuckwithme;
    
    // int mouseSensitivity; // has default

    /** Show messages has default, 0 = off, 1 = on */

    private boolean showMessages=false;

    /**
     * showMessages can be read outside of Menu, but not modified. Menu has the
     * actual C definition (not declaration)
     */
    
    @Override
    public boolean getShowMessages() {
        return showMessages;
    }

    @Override
    public void setShowMessages(boolean val) {
        this.showMessages=val;
    }
    
    /** Blocky mode, has default, 0 = high, 1 = normal */
    int detailLevel;

    int screenblocks=10; // has default

    /** temp for screenblocks (0-9) */
    int screenSize;

    /** -1 = no quicksave slot picked! */
    int quickSaveSlot;

    /** 1 = message to be printed */
    boolean messageToPrint;

    /** ...and here is the message string! */
    String messageString;

    /** message x & y */
    int messx, messy;

    boolean messageLastMenuActive;

    /** timed message = no input from user */
    boolean messageNeedsInput;

    /** Probably I need some MessageRoutine interface at this point? */
    public MenuRoutine messageRoutine;


    /** we are going to be entering a savegame string */
    boolean saveStringEnter;

    int saveSlot; // which slot to save in

    int saveCharIndex; // which char we're editing

    /** old save description before edit */
    char[] saveOldString = new char[SAVESTRINGSIZE];

    boolean inhelpscreens;

    //int menuactive;

    protected static final int SKULLXOFF = -32;

    protected static final int LINEHEIGHT = 16;

    char[][] savegamestrings = new char[10][SAVESTRINGSIZE];

    String endstring = new String();

    //
    // MENU TYPEDEFS
    //

    /** menu item skull is on */
    short itemOn;

    /** skull animation counter */
    short skullAnimCounter;

    /** which skull to draw */
    short whichSkull;

    /**
     * graphic name of skulls warning: initializer-string for array of chars is
     * too long
     */
    private static String[] skullName = { "M_SKULL1", "M_SKULL2" };

    /** current menudef */
    // MAES: pointer? array?
    menu_t currentMenu;

    //
    // DOOM MENU
    //

    // MAES: was an enum called "main_e" used purely as numerals. No need for
    // strong typing.

    /**
     * MenuRoutine class definitions, replacing "function pointers".
     */
    MenuRoutine ChangeDetail, ChangeMessages, ChangeSensitivity, ChooseSkill,
            EndGame, EndGameResponse, Episode, FinishReadThis, LoadGame,
            LoadSelect, MusicVol, NewGame, Options, VerifyNightmare,
            SaveSelect, SfxVol, SizeDisplay, SaveGame, Sound, QuitDOOM,
            QuitResponse, QuickLoadResponse, QuickSaveResponse, ReadThis, ReadThis2;

    /** DrawRoutine class definitions, replacing "function pointers". */

    DrawRoutine DrawEpisode, DrawLoad,DrawMainMenu,DrawNewGame,DrawOptions,
    			DrawReadThis1, DrawReadThis2, DrawSave, DrawSound;

    /** Initialize menu routines first */
    
    private void initMenuRoutines() {




        ChangeMessages = new M_ChangeMessages();
        ChangeDetail = new M_ChangeDetail();
        ChangeSensitivity = new M_ChangeSensitivity();
        ChooseSkill = new M_ChooseSkill();
        EndGame = new M_EndGame();
        EndGameResponse = new M_EndGameResponse();
        Episode = new M_Episode();
        FinishReadThis=new M_FinishReadThis();
        LoadGame=new M_LoadGame();
        LoadSelect=new M_LoadSelect();
        MusicVol=new M_MusicVol();
        NewGame = new M_NewGame();
        Options = new M_Options();


        QuitDOOM = new M_QuitDOOM();
        QuickLoadResponse = new M_QuickLoadResponse();
        QuickSaveResponse= new M_QuickSaveResponse();
        QuitResponse = new M_QuitResponse();
        
        ReadThis = new M_ReadThis();
        ReadThis2 = new M_ReadThis2();
         
        SaveGame=new M_SaveGame();
        SaveSelect= new M_SaveSelect();
        SfxVol=new M_SfxVol();
        SizeDisplay = new M_SizeDisplay();
        Sound = new M_Sound();
        VerifyNightmare = new M_VerifyNightmare();
    }

    /** Then drawroutines */
    
    private void initDrawRoutines() {
        DrawEpisode = new M_DrawEpisode();
        DrawNewGame = new M_DrawNewGame();
        DrawReadThis1 = new M_DrawReadThis1();
        DrawReadThis2 = new M_DrawReadThis2();
        DrawOptions = new M_DrawOptions();
        DrawLoad = new M_DrawLoad();
        DrawSave = new M_DrawSave();
        DrawSound=new M_DrawSound();
        DrawMainMenu = new M_DrawMainMenu();
    }

    /** Menuitem definitions. A "menu" can consist of multiple menuitems */
    menuitem_t[] MainMenu,EpisodeMenu,NewGameMenu, OptionsMenu,ReadMenu1,ReadMenu2,SoundMenu,LoadMenu,SaveMenu;
    
    /** Actual menus. Each can point to an array of menuitems */
    menu_t MainDef, EpiDef,NewDef,OptionsDef,ReadDef1, ReadDef2,SoundDef,LoadDef,SaveDef;
    
    /** First initialize those */
    
    private void initMenuItems(){
    
    MainMenu = new menuitem_t[]
        { new menuitem_t( 1, "M_NGAME", NewGame, 'n'),
                new menuitem_t(1, "M_OPTION", Options, 'o'),
                new menuitem_t(1, "M_LOADG", LoadGame, 'l'),
                new menuitem_t(1, "M_SAVEG", SaveGame, 's'),
                // Another hickup with Special edition.
                new menuitem_t(1, "M_RDTHIS", ReadThis, 'r'),
                new menuitem_t(1, "M_QUITG", QuitDOOM, 'q') };

    MainDef =
        new menu_t(main_end, null, MainMenu, DrawMainMenu, 97, 64, 0);

    //
    // EPISODE SELECT
    //

    EpisodeMenu = new menuitem_t[]
        { new menuitem_t(1, "M_EPI1", Episode, 'k'),
                new menuitem_t(1, "M_EPI2", Episode, 't'),
                new menuitem_t(1, "M_EPI3", Episode, 'i'),
                new menuitem_t(1, "M_EPI4", Episode, 't') };

    EpiDef = new menu_t(ep_end, // # of menu items
            MainDef, // previous menu
            EpisodeMenu, // menuitem_t ->
            DrawEpisode, // drawing routine ->
            48, 63, // x,y
            ep1 // lastOn
        );

    //
    // NEW GAME
    //


     NewGameMenu  = new menuitem_t[]
        { new menuitem_t(1, "M_JKILL", ChooseSkill, 'i'),
                new menuitem_t(1, "M_ROUGH", ChooseSkill, 'h'),
                new menuitem_t(1, "M_HURT", ChooseSkill, 'h'),
                new menuitem_t(1, "M_ULTRA", ChooseSkill, 'u'),
                new menuitem_t(1, "M_NMARE", ChooseSkill, 'n') };

     NewDef = new menu_t(newg_end, // # of menu items
            EpiDef, // previous menu
            NewGameMenu, // menuitem_t ->
            DrawNewGame, // drawing routine ->
            48, 63, // x,y
            hurtme // lastOn
        );

    //
    // OPTIONS MENU
    //

    OptionsMenu = new  menuitem_t[]
        { new menuitem_t(1, "M_ENDGAM", EndGame, 'e'),
                new menuitem_t(1, "M_MESSG", ChangeMessages, 'm'),
                new menuitem_t(1, "M_DETAIL", ChangeDetail, 'g'),
                new menuitem_t(2, "M_SCRNSZ", SizeDisplay, 's'),
                new menuitem_t(-1, "", null),
                new menuitem_t(2, "M_MSENS", ChangeSensitivity, 'm'),
                new menuitem_t(-1, "", null),
                new menuitem_t(1, "M_SVOL", Sound, 's') };

    OptionsDef =
        new menu_t(opt_end, this.MainDef, OptionsMenu, DrawOptions, 60, 37, 0);

    // Read This! MENU 1 

    ReadMenu1 = new menuitem_t[] { new menuitem_t(1, "", ReadThis2, (char) 0) };

    ReadDef1 =
        new menu_t(read1_end, MainDef, ReadMenu1, DrawReadThis1, 280, 185, 0);

    // Read This! MENU 2

    ReadMenu2 = new menuitem_t[] 
        { new menuitem_t(1, "", FinishReadThis, (char) 0) };

    ReadDef2 =
        new menu_t(read2_end, ReadDef1, ReadMenu2, DrawReadThis2, 330, 175, 0);

    //
    // SOUND VOLUME MENU
    //



    SoundMenu = new menuitem_t[]
        { new menuitem_t(2, "M_SFXVOL", SfxVol, 's'),
                new menuitem_t(-1, "", null),
                new menuitem_t(2, "M_MUSVOL", MusicVol, 'm'),
                new menuitem_t(-1, "", null) };

    SoundDef =
        new menu_t(sound_end, OptionsDef, SoundMenu, DrawSound, 80, 64, 0);

    //
    // LOAD GAME MENU
    //

    LoadMenu =new menuitem_t[]
        { new menuitem_t(1, "", LoadSelect, '1'),
                new menuitem_t(1, "", LoadSelect, '2'),
                new menuitem_t(1, "", LoadSelect, '3'),
                new menuitem_t(1, "", LoadSelect, '4'),
                new menuitem_t(1, "", LoadSelect, '5'),
                new menuitem_t(1, "", LoadSelect, '6') };

    LoadDef =
        new menu_t(load_end, MainDef, LoadMenu, DrawLoad, 80, 54, 0);

    //
    // SAVE GAME MENU
    //
    SaveMenu = new menuitem_t[]
        { new menuitem_t(1, "", SaveSelect, '1'),
                new menuitem_t(1, "", SaveSelect, '2'),
                new menuitem_t(1, "", SaveSelect, '3'),
                new menuitem_t(1, "", SaveSelect, '4'),
                new menuitem_t(1, "", SaveSelect, '5'),
                new menuitem_t(1, "", SaveSelect, '6') };

    SaveDef =
        new menu_t(load_end, MainDef, SaveMenu, DrawSave, 80, 54, 0);
    }
    
    /**
     * M_ReadSaveStrings
     * read the strings from the savegame files
     */
    
    public void ReadSaveStrings() {
        DataInputStream handle;
        int count;
        int i;
        String name;

        for (i = 0; i < load_end; i++) {
            if (DM.CM.CheckParm("-cdrom") != 0)
                name = "c:\\doomdata\\" + SAVEGAMENAME + (i) + ".dsg";
            else
                name = SAVEGAMENAME + (i) + ".dsg";

            try {
                handle = new DataInputStream(new BufferedInputStream(new FileInputStream(name)));
                savegamestrings[i] =
                    DoomIO.readString(handle,SAVESTRINGSIZE).toCharArray();
                handle.close();
                LoadMenu[i].status = 1;
            } catch (IOException e) {
                savegamestrings[i][0] = 0x00;
                LoadMenu[i].status = 0;
                continue;
            }

        }
    }

    /**
     * Draw border for the savegame description. This is special in that it's
     * not "invokable" like the other drawroutines, but standalone.
     */
    private void DrawSaveLoadBorder(int x, int y) {
        int i;

        V.DrawScaledPatch(x - 8, y + 7, 0, vs,W.CachePatchName("M_LSLEFT"));

        for (i = 0; i < 24; i++) {
            V.DrawScaledPatch(x, y + 7, 0, vs,W.CachePatchName("M_LSCNTR"));
            x += 8;
        }

        V.DrawScaledPatch(x, y + 7, 0, vs,W.CachePatchName("M_LSRGHT"));
    }

    /** Draws slider rail of a specified width (each notch is 8 base units wide)
     *  and with a slider selector at position thermDot.
     * 
     * @param x
     * @param y
     * @param thermWidth
     * @param thermDot
     */
    
    public void DrawThermo(int x, int y, int thermWidth, int thermDot) {
        int xx;
        int i;

        xx = x;
        V.DrawScaledPatch(xx, y, 0, vs,W.CachePatchName("M_THERML"));
        xx += 8;
        for (i = 0; i < thermWidth; i++) {
            V.DrawScaledPatch(xx, y, 0, vs,W.CachePatchName("M_THERMM"));
            xx += 8;
        }
        V.DrawScaledPatch(xx, y, 0,vs, W.CachePatchName("M_THERMR"));

        V.DrawScaledPatch((x + 8) + thermDot * 8, y, 0,vs, W
                .CachePatchName("M_THERMO"));
    }

    public void DrawEmptyCell(menu_t menu, int item) {
        V.DrawScaledPatch(menu.x - 10, menu.y + item * LINEHEIGHT - 1, 0,vs,
            (patch_t) W.CacheLumpName("M_CELL1", PU_CACHE, patch_t.class));
    }

    public void DrawSelCell(menu_t menu, int item) {
        V.DrawScaledPatch(menu.x - 10, menu.y + item * LINEHEIGHT - 1, 0,vs,
            (patch_t) W.CacheLumpName("M_CELL2", PU_CACHE, patch_t.class));
    }

    //
    // M_SaveGame & Cie.
    //
    public class M_DrawSave implements DrawRoutine {
    	@Override
    	public void invoke(){
        int i;
        V.DrawScaledPatch(72, 28, 0, vs,W.CachePatchName("M_SAVEG"));
        for (i = 0; i < load_end; i++) {
            DrawSaveLoadBorder(LoadDef.x, LoadDef.y + LINEHEIGHT * i);
            WriteText(LoadDef.x, LoadDef.y + LINEHEIGHT * i, savegamestrings[i]);
        }

        if (saveStringEnter) {
            i = StringWidth(savegamestrings[saveSlot]);
            WriteText(LoadDef.x + i, LoadDef.y + LINEHEIGHT * saveSlot, "_");
        }
    	}
    }

    /**
     * M_Responder calls this when user is finished
     * 
     * @param slot
     */

    public void DoSave(int slot) {
        DG.SaveGame(slot, new String(savegamestrings[slot]));
        ClearMenus();

        // PICK QUICKSAVE SLOT YET?
        if (quickSaveSlot == -2)
            quickSaveSlot = slot;
    }

    /**
     * User wants to save. Start string input for M_Responder
     */

    class M_SaveSelect
            implements MenuRoutine {
        @Override
        public void invoke(int choice) {
            // we are going to be intercepting all chars
        	//System.out.println("ACCEPTING typing input");
            saveStringEnter = true;

            saveSlot = choice;
            C2JUtils.strcpy(saveOldString, savegamestrings[choice]);
            if (C2JUtils.strcmp(savegamestrings[choice], EMPTYSTRING))
                savegamestrings[choice][0] = 0;
            saveCharIndex = C2JUtils.strlen(savegamestrings[choice]);
        }
    }

    /**
     * Selected from DOOM menu
     */
    class M_SaveGame
            implements MenuRoutine {
        @Override
        public void invoke(int choice) {
            if (!DM.usergame) {
                StartMessage(SAVEDEAD, null, false);
                return;
            }

            if (DM.gamestate != gamestate_t.GS_LEVEL)
                return;

            SetupNextMenu(SaveDef);
            ReadSaveStrings();
        }
    }

    //
    // M_QuickSave
    //
    private String tempstring;

    class M_QuickSaveResponse
            implements MenuRoutine {
        @Override
        public void invoke(int ch) {
            if (ch == 'y') {
                DoSave(quickSaveSlot);
                S.StartSound(null, sfxenum_t.sfx_swtchx);
            }
        }
    }

    private void QuickSave() {
        if (!DM.usergame) {
            S.StartSound(null, sfxenum_t.sfx_oof);
            return;
        }

        if (DM.gamestate != gamestate_t.GS_LEVEL)
            return;

        if (quickSaveSlot < 0) {
            StartControlPanel();
            ReadSaveStrings();
            SetupNextMenu(SaveDef);
            quickSaveSlot = -2; // means to pick a slot now
            return;
        }
        tempstring = String.format(QSPROMPT,C2JUtils.nullTerminatedString(savegamestrings[quickSaveSlot]));
        StartMessage(tempstring,this.QuickSaveResponse,true);
    }

    //
    // M_QuickLoad
    //
    class M_QuickLoadResponse
            implements MenuRoutine {
        @Override
        public void invoke(int ch) {
            if (ch == 'y') {
                LoadSelect.invoke(quickSaveSlot);
                S.StartSound(null, sfxenum_t.sfx_swtchx);
            }
        }
    }

    class M_QuitResponse
            implements MenuRoutine {
        @Override
        public void invoke(int ch) {
            if (ch != 'y')
                return;
            if (!DM.netgame) {
                if (DM.isCommercial())
                    S.StartSound(null, quitsounds2[(DM.gametic >> 2) & 7]);
                else
                    S.StartSound(null, quitsounds[(DM.gametic >> 2) & 7]);
                // TI.WaitVBL(105);
            }
            I.Quit();
        }
    }

    public void QuickLoad() {
        if (DM.netgame) {
            StartMessage(QLOADNET, null, false);
            return;
        }

        if (quickSaveSlot < 0) {
            StartMessage(QSAVESPOT, null, false);
            return;
        }
        tempstring = String.format(QLPROMPT, C2JUtils.nullTerminatedString(savegamestrings[quickSaveSlot]));
        StartMessage(tempstring, QuickLoadResponse, true);
    }

    class M_Sound
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {

            SetupNextMenu(SoundDef);
        }
    }

    class M_SfxVol
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            switch (choice) {
            case 0:
                if (DM.snd_SfxVolume != 0)
                    DM.snd_SfxVolume--;
                break;
            case 1:
                if (DM.snd_SfxVolume < 15)
                    DM.snd_SfxVolume++;
                break;
            }

           S.SetSfxVolume(DM.snd_SfxVolume *8);
        }
    }

    class M_MusicVol
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            switch (choice) {
            case 0:
                if (DM.snd_MusicVolume != 0)
                    DM.snd_MusicVolume--;
                break;
            case 1:
                if (DM.snd_MusicVolume < 15)
                    DM.snd_MusicVolume++;
                break;
            }

            S.SetMusicVolume(DM.snd_MusicVolume*8);
        }
    }

    //
    // M_Episode
    //
    private int epi;

    class M_VerifyNightmare
            implements MenuRoutine {

        @Override
        public void invoke(int ch) {
            if (ch != 'y')
                return;

            DG.DeferedInitNew(skill_t.sk_nightmare, epi + 1, 1);
            ClearMenus();
        }
    }

    /**
     * M_ReadThis
     */

    class M_ReadThis
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            choice = 0;
            SetupNextMenu(ReadDef1);
        }
    }

    class M_ReadThis2
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            choice = 0;
            SetupNextMenu(ReadDef2);
        }
    }

    class M_FinishReadThis
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            choice = 0;
            SetupNextMenu(MainDef);
        }
    }

    //
    // M_QuitDOOM
    //

    class M_QuitDOOM
            implements MenuRoutine {
        @Override
        public void invoke(int choice) {
            // We pick index 0 which is language sensitive,
            // or one at random, between 1 and maximum number.
            if (DM.language != Language_t.english)
                endstring = endmsg[0] + "\n\n" + DOSY;
            else
                endstring =
                    endmsg[(DM.gametic % (NUM_QUITMESSAGES - 2)) + 1] + "\n\n"
                            + DOSY;
            StartMessage(endstring, QuitResponse, true);
        }
    }

    class M_QuitGame
            implements MenuRoutine {

        @Override
        public void invoke(int ch) {
            if (ch != 'y')
                return;
            if (!DM.netgame) {
                if (DM.isCommercial())
                S.StartSound(null,quitsounds2[(DM.gametic>>2)&7]);
                else
                S.StartSound(null,quitsounds[(DM.gametic>>2)&7]);
                I.WaitVBL(105);
            }
           I.Quit ();
        }
    }

    class M_SizeDisplay
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            switch (choice) {
            case 0:
                if (screenSize > 0) {
                    screenblocks--;
                    screenSize--;
                }
                break;
            case 1:
                if (screenSize < 8) {
                    screenblocks++;
                    screenSize++;
                }
                break;
            }

            R.SetViewSize (screenblocks, detailLevel);
        }

    }

    class M_Options
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            SetupNextMenu(OptionsDef);
        }

    }

    class M_NewGame
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            if (DM.netgame && !DM.demoplayback) {
                StartMessage(NEWGAME, null, false);
                return;
            }

            if (DM.isCommercial())
                SetupNextMenu(NewDef);
            else
                SetupNextMenu(EpiDef);
        }

    }

    public void StartMessage(String string, MenuRoutine routine, boolean input) {
        messageLastMenuActive = DM.menuactive;
        messageToPrint = true;
        messageString = new String(string);
        messageRoutine = routine;
        messageNeedsInput = input;
        DM.menuactive = true; // "true"
        return;
    }

    public void StopMessage() {
        DM.menuactive = messageLastMenuActive;
        messageToPrint = false;
    }

    /**
     * Find string width from hu_font chars
     */
    public int StringWidth(char[] string) {
        int i;
        int w = 0;
        int c;

        for (i = 0; i < C2JUtils.strlen(string); i++) {
            c = Character.toUpperCase(string[i]) - HU_FONTSTART;
            if (c < 0 || c >= HU_FONTSIZE)
                w += 4;
            else
                w += hu_font[c].width;
        }

        return w;
    }

    /**
     * Find string height from hu_font chars.
     * 
     * Actually it just counts occurences of 'n' and adds height to height.
     */
    private int StringHeight(char[] string) {
        int i;
        int h;
        int height = hu_font[0].height;

        h = height;
        for (i = 0; i < string.length; i++)
            if (string[i] == '\n')
                h += height;

        return h;
    }

    /**
     * Find string height from hu_font chars
     */
    private int StringHeight(String string) {
        return this.StringHeight(string.toCharArray());
    }

    /**
     * Write a string using the hu_font
     */

    private void WriteText(int x, int y, char[] string) {
        int w;
        char[] ch;
        int c;
        int cx;
        int cy;

        ch = string;
        int chptr = 0;
        cx = x;
        cy = y;

        while (chptr<ch.length) {
            c = ch[chptr];
            chptr++;
            if (c == 0)
                break;
            if (c == '\n') {
                cx = x;
                cy += 12;
                continue;
            }
            
            c = Character.toUpperCase(c) - HU_FONTSTART;
            if (c < 0 || c >= HU_FONTSIZE) {
                cx += 4;
                continue;
            }

            w = hu_font[c].width;
            if (cx + w > SCREENWIDTH)
                break;
            
            V.DrawScaledPatch(cx, cy, 0,vs, hu_font[c]);
            cx += w;
        }

    }

    private void WriteText(int x, int y, String string) {
        if (string == null || string.length() == 0)
            return;

        int w;
        int cx;
        int cy;

        int chptr = 0;
        char c;

        cx = x;
        cy = y;

        while (chptr<string.length()) {
            c = string.charAt(chptr++);
            if (c == 0)
                break;
            if (c == '\n') {
                cx = x;
                cy += 12;
                continue;
            }

            c = (char) (Character.toUpperCase(c) - HU_FONTSTART);
            if (c < 0 || c >= HU_FONTSIZE) {
                cx += 4;
                continue;
            }

            w = hu_font[c].width;
            if (cx + w > SCREENWIDTH)
                break;
            V.DrawScaledPatch(cx, cy, 0,vs, hu_font[c]);
            cx += w;
        }

    }

    // These belong to the responder.
    
    private int joywait = 0;

    private int mousewait = 0;

    private int mousey = 0;

    private int lasty = 0;

    private int mousex = 0;

    private int lastx = 0;

    public boolean Responder(event_t ev) {

        char ch;
        int i;
        ch = 0xFFFF;

        //System.out.println("Processing keyevent:" +(ev.type==evtype_t.ev_keydown || ev.type==evtype_t.ev_keyup)+ " value = "+(char)ev.data1);
        
        // Joystick input
        
        if (ev.type == evtype_t.ev_joystick && joywait < TICK.GetTime()) {
            if (ev.data3 == -1) {
                ch = KEY_UPARROW;
                joywait = TICK.GetTime() + 5;
            } else if (ev.data3 == 1) {
                ch = KEY_DOWNARROW;
                joywait = TICK.GetTime() + 5;
            }

            if (ev.data2 == -1) {
                ch = KEY_LEFTARROW;
                joywait = TICK.GetTime() + 2;
            } else if (ev.data2 == 1) {
                ch = KEY_RIGHTARROW;
                joywait = TICK.GetTime() + 2;
            }

            if ((ev.data1 & 1) != 0) {
                ch = KEY_ENTER;
                joywait = TICK.GetTime() + 5;
            }
            if ((ev.data1 & 2) != 0) {
                ch = KEY_BACKSPACE;
                joywait = TICK.GetTime() + 5;
            }
        } else 
        // Mouse input 
        {
            if (ev.type == evtype_t.ev_mouse && mousewait < TICK.GetTime()) {
                mousey += ev.data3;
                if (mousey < lasty - 30) {
                    ch = KEY_DOWNARROW;
                    mousewait = TICK.GetTime() + 5;
                    mousey = lasty -= 30;
                } else if (mousey > lasty + 30) {
                    ch = KEY_UPARROW;
                    mousewait = TICK.GetTime() + 5;
                    mousey = lasty += 30;
                }

                mousex += ev.data2;
                if (mousex < lastx - 30) {
                    ch = KEY_LEFTARROW;
                    mousewait = TICK.GetTime() + 5;
                    mousex = lastx -= 30;
                } else if (mousex > lastx + 30) {
                    ch = KEY_RIGHTARROW;
                    mousewait = TICK.GetTime() + 5;
                    mousex = lastx += 30;
                }

                if ((ev.data1 & 1) != 0) {
                    ch = KEY_ENTER;
                    mousewait = TICK.GetTime() + 15;
                }

                if ((ev.data1 & 2) != 0) {
                    ch = KEY_BACKSPACE;
                    mousewait = TICK.GetTime() + 15;
                }
            } else if (ev.type == evtype_t.ev_keydown) {
                ch = (char) ev.data1;
            }
        }

        if (ch == 0xFFFF)
            return false;

        // Save Game string input
        if (saveStringEnter) {

            switch (ch) {
            case KEY_BACKSPACE:
                if (saveCharIndex > 0) {
                    saveCharIndex--;
                    savegamestrings[saveSlot][saveCharIndex] = 0;
                }
                break;

            case KEY_ESCAPE:
                saveStringEnter = false;
                C2JUtils.strcpy(savegamestrings[saveSlot], saveOldString);
                break;

            case KEY_ENTER:            	
                saveStringEnter = false;
                if (savegamestrings[saveSlot][0] != 0)
                    DoSave(saveSlot);
                break;

            default:
                ch = Character.toUpperCase(ch);
            	if (ch != 32)
                    if (ch - HU_FONTSTART < 0
                            || ch - HU_FONTSTART >= HU_FONTSIZE)
                        break;
                if (ch >= 32
                        && ch <= 127
                        && saveCharIndex < SAVESTRINGSIZE - 1
                        && StringWidth(savegamestrings[saveSlot]) < (SAVESTRINGSIZE - 2) * 8) {
                    savegamestrings[saveSlot][saveCharIndex++] = ch;
                    savegamestrings[saveSlot][saveCharIndex] = 0;
                }
                break;
            }
            return true;
        }

        // Take care of any messages that need input
        if (messageToPrint) {
            if (messageNeedsInput == true
                    && !(ch == ' ' || ch == 'n' || ch == 'y' || ch == KEY_ESCAPE))
                return false;

            DM.menuactive = messageLastMenuActive;
            messageToPrint = false;
            if (messageRoutine != null)
                messageRoutine.invoke(ch);

            DM.menuactive = false; // "false"
            S.StartSound(null, sfxenum_t.sfx_swtchx);
            return true;
        }

        if (DM.devparm && ch == KEY_F1) {
            DG.ScreenShot();
            return true;
        }

        // F-Keys
        if (!DM.menuactive){
            switch (ch) {
            case KEY_MINUS: // Screen size down
                if (DM.automapactive || HU.chat_on[0])
                    return false;
                SizeDisplay.invoke(0);
                S.StartSound(null, sfxenum_t.sfx_stnmov);
                return true;

            case KEY_EQUALS: // Screen size up
                if (DM.automapactive || HU.chat_on[0])
                    return false;
                SizeDisplay.invoke(1);
                S.StartSound(null, sfxenum_t.sfx_stnmov);
                return true;

            case KEY_F1: // Help key
                StartControlPanel();

                if (DM.isRetail() || currentMenu==ReadDef1)
                    currentMenu = ReadDef2;
                else
                    currentMenu = ReadDef1;
                itemOn = 0;
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                return true;

            case KEY_F2: // Save
                StartControlPanel();
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                SaveGame.invoke(0);
                return true;

            case KEY_F3: // Load
                StartControlPanel();
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                LoadGame.invoke(0);
                return true;

            case KEY_F4: // Sound Volume
                StartControlPanel();
                currentMenu = SoundDef;
                itemOn = (short) sfx_vol;
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                return true;

            case KEY_F5: // Detail toggle
                ChangeDetail.invoke(0);
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                return true;

            case KEY_F6: // Quicksave
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                QuickSave();
                return true;

            case KEY_F7: // End game
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                EndGame.invoke(0);
                return true;

            case KEY_F8: // Toggle messages
                ChangeMessages.invoke(0);
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                return true;

            case KEY_F9: // Quickload
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                QuickLoad();
                return true;

            case KEY_F10: // Quit DOOM
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                QuitDOOM.invoke(0);
                return true;

            case KEY_F11: // gamma toggle
                int usegamma = V.getUsegamma();
                usegamma++;
                if (usegamma > 4)
                    usegamma = 0;
                DM.players[DM.consoleplayer].message = gammamsg[usegamma];
                // FIXME: it's pointless to reload the same palette.
                //I.SetPalette (W.CacheLumpName ("PLAYPAL",PU_CACHE));
                DM.VI.SetPalette(0);
                DM.VI.SetGamma(usegamma);
                return true;

            }
    }
        // Pop-up menu?
        if (!DM.menuactive) {
            if (ch == KEY_ESCAPE) {
                StartControlPanel();
                S.StartSound(null, sfxenum_t.sfx_swtchn);
                return true;
            }
            return false;
        }

        // Keys usable within menu
        switch (ch) {
        case KEY_DOWNARROW:
            do {
                if (itemOn + 1 > currentMenu.numitems - 1)
                    itemOn = 0;
                else
                    itemOn++;
                S.StartSound(null, sfxenum_t.sfx_pstop);
            } while (currentMenu.menuitems[itemOn].status == -1);
            return true;

        case KEY_UPARROW:
            do {
                if (itemOn == 0)
                    itemOn = (short) (currentMenu.numitems - 1);
                else
                    itemOn--;
              S.StartSound(null, sfxenum_t.sfx_pstop);
            } while (currentMenu.menuitems[itemOn].status == -1);
            return true;

        case KEY_LEFTARROW:
            if ((currentMenu.menuitems[itemOn].routine != null)
                    && (currentMenu.menuitems[itemOn].status == 2)) {
                S.StartSound(null, sfxenum_t.sfx_stnmov);
                currentMenu.menuitems[itemOn].routine.invoke(0);
            }
            return true;

        case KEY_RIGHTARROW:
            if ((currentMenu.menuitems[itemOn].routine != null)
                    && (currentMenu.menuitems[itemOn].status == 2)) {
            	S.StartSound(null, sfxenum_t.sfx_stnmov);
                currentMenu.menuitems[itemOn].routine.invoke(1);
            }
            return true;

        case KEY_ENTER: {
            if ((currentMenu.menuitems[itemOn].routine != null)
                    && currentMenu.menuitems[itemOn].status != 0) {
                currentMenu.lastOn = itemOn;
                if (currentMenu.menuitems[itemOn].status == 2) {
                    currentMenu.menuitems[itemOn].routine.invoke(1); // right
                    // arrow
                    S.StartSound(null, sfxenum_t.sfx_stnmov);
                } else {
                    currentMenu.menuitems[itemOn].routine.invoke(itemOn);
                    S.StartSound(null, sfxenum_t.sfx_pistol);
                }
            }
        }
            return true;

        case KEY_ESCAPE:
            currentMenu.lastOn = itemOn;
            ClearMenus();
            S.StartSound(null, sfxenum_t.sfx_swtchx);
            return true;

        case KEY_BACKSPACE:
            currentMenu.lastOn = itemOn;
            if (currentMenu.prevMenu != null) {
                currentMenu = currentMenu.prevMenu;
                itemOn = (short) currentMenu.lastOn;
                 S.StartSound(null, sfxenum_t.sfx_swtchn);
            }
            return true;

        default:
            for (i = itemOn + 1; i < currentMenu.numitems; i++)
                if (currentMenu.menuitems[i].alphaKey == ch) {
                    itemOn = (short) i;
                    S.StartSound(null, sfxenum_t.sfx_pstop);
                    return true;
                }
            for (i = 0; i <= itemOn; i++)
                if (currentMenu.menuitems[i].alphaKey == ch) {
                    itemOn = (short) i;
                    S.StartSound(null, sfxenum_t.sfx_pstop);
                    return true;
                }
            break;

        }

        return false;
    }

    /**
     * M_StartControlPanel
     */
    public void StartControlPanel() {
        // intro might call this repeatedly
        if (DM.menuactive)
            return;

        DM.menuactive = true;
        currentMenu = MainDef; // JDC
        itemOn = (short) currentMenu.lastOn; // JDC
    }

    /**
     * M_Drawer Called after the view has been rendered, but before it has been
     * blitted.
     */
    public void Drawer() {

        int x;
        int y;
        int max;
        char[] string = new char[40];
        char[] msstring;
        int start;
        inhelpscreens = false; // Horiz. & Vertically center string and print
        // it.
        if (messageToPrint) {
            start = 0;
            y = 100 - this.StringHeight(messageString) / 2;
            msstring = messageString.toCharArray();
            while(start<messageString.length()) {
                int i=0;
                for ( i = 0; i < messageString.length() - start; i++) 
                    if (msstring[start + i] == '\n') {
                        C2JUtils.memset(string, (char) 0, 40);
                        C2JUtils.strcpy(string, msstring, start, i);
                        start += i + 1;
                        break;
                    }
                
                    if (i == (messageString.length() - start)) {
                        C2JUtils.strcpy(string, msstring,start);
                        start += i;
                    }
                    x = 160 - this.StringWidth(string) / 2;
                    this.WriteText(x, y, string);
                    y += hu_font[0].height;
                }
                return;
            }
            if (!DM.menuactive)
                return;
            if (currentMenu.routine != null){
                currentMenu.routine.invoke(); // call Draw routine
            }
            // DRAW MENU
            x = currentMenu.x;
            y = currentMenu.y;
            max = currentMenu.numitems;
            for (int i = 0; i < max; i++) {
                if (currentMenu.menuitems[i].name != null && currentMenu.menuitems[i].name!="")
                    V.DrawScaledPatch(x, y, 0,vs, W.CachePatchName(
                        currentMenu.menuitems[i].name, PU_CACHE));
                y += LINEHEIGHT;
            }

            // DRAW SKULL
            V.DrawScaledPatch(x + SKULLXOFF, currentMenu.y - 5 + itemOn
                    * LINEHEIGHT, 0, vs,W.CachePatchName(skullName[whichSkull],
                PU_CACHE));
    }

    //
    // M_ClearMenus
    //
    public void ClearMenus() {
        DM.menuactive = false;
        V.clearCaches();
        // MAES: was commented out :-/
        //if (!DM.netgame && DM.usergame && DM.paused)
        //    DM.setPaused(true);
    }

    /**
     * M_SetupNextMenu
     */
    public void SetupNextMenu(menu_t menudef) {
        currentMenu = menudef;
        itemOn = (short) currentMenu.lastOn;
    }

    /**
     * M_Ticker
     */
    public void Ticker() {
        if (--skullAnimCounter <= 0) {
            whichSkull ^= 1;
            skullAnimCounter = 8;
        }
    }

    /**
     * M_Init
     */
    public void Init() {
        
        // Init menus.
        this.initMenuRoutines();
        this.initDrawRoutines();
        this.initMenuItems();
        this.hu_font=HU.getHUFonts();

        currentMenu = MainDef;
        DM.menuactive = false;
        itemOn = (short) currentMenu.lastOn;
        whichSkull = 0;
        skullAnimCounter = 10;
        screenSize = screenblocks - 3;
        messageToPrint = false;
        messageString = null;
        messageLastMenuActive = DM.menuactive;
        quickSaveSlot = -1;

        // Here we could catch other version dependencies,
        // like HELP1/2, and four episodes.

        switch (DM.getGameMode()) {
        case commercial:
        case pack_plut:
        case pack_tnt:
            // This is used because DOOM 2 had only one HELP
            // page. I use CREDIT as second page now, but
            // kept this hack for educational purposes.
            MainMenu[readthis] = MainMenu[quitdoom];
            MainDef.numitems--;
            MainDef.y += 8;
            NewDef.prevMenu = MainDef;
            ReadDef1.routine = DrawReadThis1;
            ReadDef1.x = 330;
            ReadDef1.y = 165;
            ReadMenu1[0].routine = FinishReadThis;
            break;
        case shareware:
            // Episode 2 and 3 are handled,
            // branching to an ad screen.
        case registered:
            // We need to remove the fourth episode.
            EpiDef.numitems--;
            break;
        case retail:
            // We are fine.
        default:
            break;
        }

    }

    

    /**
     * M_DrawText Returns the final X coordinate HU_Init must have been called
     * to init the font. Unused?
     * 
     * @param x
     * @param y
     * @param direct
     * @param string
     * @return
     */

    public int DrawText(int x, int y, boolean direct, String string) {
        int c;
        int w;
        int ptr = 0;
        
        while ((c=string.charAt(ptr)) > 0) {
            c = Character.toUpperCase(c) - HU_FONTSTART;
            ptr++;
            if (c < 0 || c > HU_FONTSIZE) {
                x += 4;
                continue;
            }

            w = hu_font[c].width;
            if (x + w > SCREENWIDTH)
                break;
            if (direct)
                V.DrawScaledPatch(x, y, 0,vs, hu_font[c]);
            else
                V.DrawScaledPatch(x, y, 0,vs, hu_font[c]);
            x += w;
        }

        return x;
    }

    

    // ////////////////////////// DRAWROUTINES
    // //////////////////////////////////

    class M_DrawEpisode
            implements DrawRoutine {

        @Override
        public void invoke() {
            V.DrawScaledPatch(54, 38, 0,vs, W.CachePatchName("M_EPISOD"));
        }

    }

    /**
     * M_LoadGame & Cie.
     */

    class M_DrawLoad
            implements DrawRoutine {
        @Override
        public void invoke() {
            int i;

            V.DrawScaledPatch(72, 28, 0,vs, W.CachePatchName("M_LOADG"));
            for (i = 0; i < load_end; i++) {
                DrawSaveLoadBorder(LoadDef.x, LoadDef.y + LINEHEIGHT * i);
                WriteText(LoadDef.x, LoadDef.y + LINEHEIGHT * i,
                    savegamestrings[i]);
            }

        }
    }

    class M_DrawMainMenu
            implements DrawRoutine {
        @Override
        public void invoke() {
            V.DrawScaledPatch(94, 2, 0,vs, (patch_t) (W.CachePatchName("M_DOOM")));
        }
    }

    class M_DrawNewGame
            implements DrawRoutine {

        @Override
        public void invoke() {
            V.DrawScaledPatch(96, 14, 0, vs,(patch_t) W.CachePatchName("M_NEWG"));
            V.DrawScaledPatch(54, 38, 0, vs,(patch_t) W.CachePatchName("M_SKILL"));
        }
    }

    class M_DrawOptions
            implements DrawRoutine {

        private String detailNames[] = { "M_GDHIGH", "M_GDLOW" };

        private String msgNames[] = { "M_MSGOFF", "M_MSGON" };

        @Override
        public void invoke() {
            V.DrawScaledPatch(108, 15, 0, vs,W.CachePatchName("M_OPTTTL"));

            V.DrawScaledPatch(OptionsDef.x + 175, OptionsDef.y + LINEHEIGHT
                    * detail, 0,vs, W.CachePatchName(detailNames[detailLevel]));

            V.DrawScaledPatch(OptionsDef.x + 120, OptionsDef.y + LINEHEIGHT
                    * messages, 0,vs, W.CachePatchName(msgNames[showMessages?1:0]));

            DrawThermo(OptionsDef.x, OptionsDef.y + LINEHEIGHT
                    * (mousesens + 1), 10, DM.mouseSensitivity);

            DrawThermo(OptionsDef.x,
                OptionsDef.y + LINEHEIGHT * (scrnsize + 1), 9, screenSize);

        }

    }

    /**
     * Read This Menus Had a "quick hack to fix romero bug"
     */

    class M_DrawReadThis1
            implements DrawRoutine {

        @Override
        public void invoke() {
            inhelpscreens = true;
            switch (DM.getGameMode()) {
            case commercial:
                V.DrawPatchSolidScaled(0, 0, 0, vs,W.CachePatchName("HELP"));
                break;
            case shareware:
            case registered:
            case retail:
                V.DrawPatchSolidScaled(0, 0, 0,vs, W.CachePatchName("HELP1"));
                break;
            default:
                break;
            }
            
            // Maes: we need to do this here, otherwide the status bar appears "dirty"
            DM.ST.forceRefresh();
            return;
        }
    }

    /**
     * Read This Menus - optional second page.
     */
    class M_DrawReadThis2
            implements DrawRoutine {

        @Override
        public void invoke() {
            inhelpscreens = true;
            switch (DM.getGameMode()) {
            case retail:
            case commercial:
                // This hack keeps us from having to change menus.
                V.DrawPatchSolidScaled(0, 0, 0, vs,W.CachePatchName("CREDIT"));
                break;
            case shareware:
            case registered:
                V.DrawPatchSolidScaled(0, 0, 0, vs,W.CachePatchName("HELP2"));
                break;
            default:
                break;
            }
            
            // Maes: we need to do this here, otherwide the status bar appears "dirty"
            DM.ST.forceRefresh();
            return;
        }
    }

    /**
     * Change Sfx & Music volumes
     */
    class M_DrawSound
            implements DrawRoutine {

        public void invoke() {
            V.DrawScaledPatch(60, 38, 0,vs, (patch_t) W.CacheLumpName("M_SVOL",
                PU_CACHE, patch_t.class));

            DrawThermo(SoundDef.x, SoundDef.y + LINEHEIGHT * (sfx_vol + 1), 16,
                DM.snd_SfxVolume);

            DrawThermo(SoundDef.x, SoundDef.y + LINEHEIGHT * (music_vol + 1),
                16, DM.snd_MusicVolume);
        }
    }

    // /////////////////////////// MENU ROUTINES
    // ///////////////////////////////////

    class M_ChangeDetail
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            choice = 0;
            detailLevel = 1 - detailLevel;

            // FIXME - does not work. Remove anyway?
            //System.err.print("M_ChangeDetail: low detail mode n.a.\n");

            //return;

            R.SetViewSize (screenblocks, detailLevel); 
            if (detailLevel==0) DM.players[DM.consoleplayer].message = englsh.DETAILHI;
             else DM.players[DM.consoleplayer].message = englsh.DETAILLO;
             

        }
    }

    /**
     * Toggle messages on/off
     */
    class M_ChangeMessages
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            // warning: unused parameter `int choice'
            //choice = 0;
            showMessages = !showMessages;

            if (!showMessages)
                DM.players[DM.consoleplayer].message = MSGOFF;
            else
                DM.players[DM.consoleplayer].message = MSGON;

            message_dontfuckwithme = true;
        }
    }

    class M_ChangeSensitivity
            implements MenuRoutine {
        @Override
        public void invoke(int choice) {
            switch (choice) {
            case 0:
                if (DM.mouseSensitivity != 0)
                	DM.mouseSensitivity--;
                break;
            case 1:
                if (DM.mouseSensitivity < 9)
                	DM.mouseSensitivity++;
                break;
            }
        }
    }

    class M_ChooseSkill
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            if (choice == nightmare) {
                StartMessage(NIGHTMARE, VerifyNightmare, true);
                return;
            }

            DG.DeferedInitNew(skill_t.values()[choice], epi + 1, 1);
            ClearMenus();
        }

    }

    /**
     * M_EndGame
     */

    class M_EndGame
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {
            choice = 0;
            if (!DM.usergame) {
                 S.StartSound(null, sfxenum_t.sfx_oof);
                return;
            }

            if (DM.netgame) {
                StartMessage(NETEND, null, false);
                return;
            }

            StartMessage(ENDGAME, EndGameResponse, true);
        }
    }

    class M_EndGameResponse
            implements MenuRoutine {

        @Override
        public void invoke(int ch) {
            if (ch != 'y')
                return;

            currentMenu.lastOn = itemOn;
            ClearMenus();
            DG.StartTitle();
        }
    }

    class M_Episode
            implements MenuRoutine {

        @Override
        public void invoke(int choice) {

            if (DM.isShareware() && (choice != 0)) {
                StartMessage(SWSTRING, null, false);
                SetupNextMenu(ReadDef2);
                return;
            }

            // Yet another hack...
            if (!DM.isRetail() && (choice > 2)) {
                System.err
                        .print("M_Episode: 4th episode requires UltimateDOOM\n");
                choice = 0;
            }

            epi = choice;
            SetupNextMenu(NewDef);
        }

    }

    /**
     * User wants to load this game
     */
    class M_LoadSelect
            implements MenuRoutine {
        @Override
        public void invoke(int choice) {
            String name;

            if (DM.CM.CheckParm("-cdrom") != 0)
                name = ("c:\\doomdata\\" + SAVEGAMENAME + (choice) + ".dsg");
            else
                name = (SAVEGAMENAME + (choice) + ".dsg");
            DG.LoadGame(name);
            ClearMenus();
        }
    }

    /**
     * Selected from DOOM menu
     */
    class M_LoadGame
            implements MenuRoutine {
        @Override
        public void invoke(int choice) {

            if (DM.netgame) {
                StartMessage(LOADNET, null, false);
                return;
            }

            SetupNextMenu(LoadDef);
            ReadSaveStrings();
        }
    }

    // ////////////////////// VARIOUS CONSTS //////////////////////

    private static final sfxenum_t[] quitsounds =
        { sfxenum_t.sfx_pldeth, sfxenum_t.sfx_dmpain, sfxenum_t.sfx_popain,
                sfxenum_t.sfx_slop, sfxenum_t.sfx_telept, sfxenum_t.sfx_posit1,
                sfxenum_t.sfx_posit3, sfxenum_t.sfx_sgtatk };

    private static final sfxenum_t[] quitsounds2 =
        { sfxenum_t.sfx_vilact, sfxenum_t.sfx_getpow, sfxenum_t.sfx_boscub,
                sfxenum_t.sfx_slop, sfxenum_t.sfx_skeswg, sfxenum_t.sfx_kntdth,
                sfxenum_t.sfx_bspact, sfxenum_t.sfx_sgtatk };

    /** episodes_e enum */
    private static final int ep1 = 0, ep2 = 1, ep3 = 2, ep4 = 3, ep_end = 4;

    /** load_e enum */
    private static final int load1 = 0, load2 = 1, load3 = 2, load4 = 3, load5 = 4,
            load6 = 5, load_end = 6;

    /** options_e enum; */

    private static final int endgame = 0, messages = 1, detail = 2, scrnsize = 3,
            option_empty1 = 4, mousesens = 5, option_empty2 = 6, soundvol = 7,
            opt_end = 8;

    /** main_e enum; */
    private static final int  newgame = 0, options = 1, loadgam = 2, savegame = 3,
            readthis = 4, quitdoom = 5, main_end = 6;

    /** read_e enum */
    private static final int rdthsempty1 = 0, read1_end = 1;

    /** read_2 enum */
    private static final int rdthsempty2 = 0, read2_end = 1;

    /**  newgame_e enum;*/
    public static final int killthings = 0, toorough = 1, hurtme = 2, violence = 3,
            nightmare = 4, newg_end = 5;
    
    private static final String[] gammamsg = { "GAMMALVL0",

        "GAMMALVL1", "GAMMALVL2", "GAMMALVL3", "GAMMALVL4" };

    /** sound_e enum */
    static final int sfx_vol = 0, sfx_empty1 = 1, music_vol = 2, sfx_empty2 = 3,
            sound_end = 4;

    @Override
    public void setScreenBlocks(int val) {
        this.screenblocks=val;
    }
    
	@Override
	public int getScreenBlocks() {
		return this.screenblocks;
	}

	@Override
	public int getDetailLevel() {
		return this.detailLevel;
	}
	
////////////////////////////VIDEO SCALE STUFF ////////////////////////////////

	protected int SCREENWIDTH;
	protected int SCREENHEIGHT;
	protected IVideoScale vs;


	@Override
	public void setVideoScale(IVideoScale vs) {
	    this.vs=vs;
	}

	@Override
	public void initScaling() {
	    this.SCREENHEIGHT=vs.getScreenHeight();
	    this.SCREENWIDTH=vs.getScreenWidth();
	}




}