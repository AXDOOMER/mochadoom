package g;

//
//DOOM keyboard definition.
//This is the stuff configured by Setup.Exe.
//Most key data are simple ascii (uppercased).
//
//NOTE: the codes used here are arbitrary, non-system specificm 
//however they must match what's used in the default.cfg files
//and throughout the code. Once the system input barrier has been
//crossed, Doom only understands these codes. It's the job of
//the system-specific interface to produce the correct mappings.


public class Keys {
	public static final char KEY_NULL        = 0;       // null key, triggers nothing

	public static final char KEY_ESCAPE      = 27;
	public static final char KEY_SPACE       = 32;

	public static final char KEY_NUMLOCK     = (0x80+69);
	public static final char KEY_SCROLLLOCK  = (0x80+70);

	public static final char KEY_MINUS       = 45;
	public static final char KEY_EQUALS      = 61;
	public static final char KEY_BACKSPACE   = 8;
	public static final char KEY_TAB         = 9;
	public static final char KEY_ENTER       = 13;

	//
	//  scancodes 71-83 (non-extended)
	//
	public static final char KEY_KEYPAD7     = (0x80+71);
	public static final char KEY_KEYPAD8     = (0x80+72);
	public static final char KEY_KEYPAD9     = (0x80+73);
	public static final char KEY_MINUSPAD    = (0x80+74);
	public static final char KEY_KEYPAD4     = (0x80+75);
	public static final char KEY_KEYPAD5     = (0x80+76);
	public static final char KEY_KEYPAD6     = (0x80+77);
	public static final char KEY_PLUSPAD     = (0x80+78);
	public static final char KEY_KEYPAD1     = (0x80+79);
	public static final char KEY_KEYPAD2     = (0x80+80);
	public static final char KEY_KEYPAD3     = (0x80+81);
	public static final char KEY_KEYPAD0     = (0x80+82);
	public static final char KEY_KPADDEL     = (0x80+83);

	//  windows95 keys...

	public static final char KEY_LEFTWIN     = (0x80+91);
	public static final char KEY_RIGHTWIN    = (0x80+92);
	public static final char KEY_MENU        = (0x80+93);

	//
	//  scancodes 71-83 EXTENDED are remapped
	//  to these by the keyboard handler (just add 30)
	//
	public static final char KEY_KPADSLASH   = (0x80+100);      //extended scancode 53 '/' remapped

	public static final char KEY_HOME        = (0x80+101);
	public static final char KEY_UPARROW     = (0x80+102);
	public static final char KEY_PGUP        = (0x80+103);
	public static final char KEY_LEFTARROW   = (0x80+105);
	public static final char KEY_RIGHTARROW  = (0x80+107);
	public static final char KEY_END         = (0x80+109);
	public static final char KEY_DOWNARROW   = (0x80+110);
	public static final char KEY_PGDN        = (0x80+111);
	public static final char KEY_INS         = (0x80+112);
	public static final char KEY_DEL         = (0x80+113);


	public static final char KEY_F1          = (0x80+0x3b);
	public static final char KEY_F2          = (0x80+0x3c);
	public static final char KEY_F3          = (0x80+0x3d);
	public static final char KEY_F4          = (0x80+0x3e);
	public static final char KEY_F5          = (0x80+0x3f);
	public static final char KEY_F6          = (0x80+0x40);
	public static final char KEY_F7          = (0x80+0x41);
	public static final char KEY_F8          = (0x80+0x42);
	public static final char KEY_F9          = (0x80+0x43);
	public static final char KEY_F10         = (0x80+0x44);
	public static final char KEY_F11         = (0x80+0x57);
	public static final char KEY_F12         = (0x80+0x58);

	public static final char KEY_PAUSE       = 255;

	// these ones must be non-extended scancodes (rctrl,rshift,lalt)
	public static final char KEY_SHIFT       = (0x80+54);
	public static final char KEY_CTRL        = (0x80+29);
	public static final char KEY_ALT         = (0x80+56);

	public static final char KEY_CAPSLOCK    = (0x80+58);
	public static final char KEY_CONSOLE     = (int)'`';

//	public static final char KEY_OPENBRACKETS
//	public static final char KEY_CLOSEBRACKETS

}