package i;

public final class Strings {

    public static final String MOCHA_DOOM_TITLE = "Mocha Doom Alpha 1.6";

    public static final String MODIFIED_GAME_TITLE = "Modified game alert";

    public static final String MODIFIED_GAME_DIALOG
            = ("<html><center>"
            + "===========================================================================<br>"
            + "ATTENTION:  This version of DOOM has been modified.  If you would like to<br>"
            + "get a copy of the original game, call 1-800-IDGAMES or see the readme file.<br>"
            + "        You will not receive technical support for modified games.<br>"
            + "                      press OK to continue<br>"
            + "===========================================================================<br>"
            + "</center></html>");

    public static final String LEVEL_FAILURE_TITLE = "Level loading failure";

    public static final String LEVEL_FAILURE_CAUSE
            = ("<html><center>"
            + "Level loading failed!<br>"
            + "Press OK to end this game without exiting, or cancel to quit Doom."
            + "</center></html>");

    public static final String NO_WAD_FILE_FOUND_TITLE = "Cannot find a game IWAD";

    public static final String NO_WAD_FILE_FOUND_NOTE = "<html>"
            + "===========================================================================<br>"
            + "Execution could not continue:<br>"
            + "Cannot find a game IWAD (doom.wad, doom2.wad, etc.).<br>"
            + "<br>"
            + "You can do either of the following:<br>"
            + "- Place one or more of these WADs in the same directory as Mocha Doom.<br>"
            + "- Start Mocha Doom with '-iwad' parameter e.g. 'mochadoom -iwad wads/doom.wad'<br>"
            + "- Define 'DOOMWADDIR' environment variable which points to a directory of WAD files.<br>"
            + "===========================================================================<br>"
            + "</html>";

}
