package g;

//

import static java.awt.event.KeyEvent.*;
import java.util.Arrays;


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
    
    public static final char KEY_COMMA       = (0x80+114);
    public static final char KEY_PERIOD      = (0x80+115);
    public static final char KEY_QUOTE       = (0x80+116);
    public static final char KEY_SEMICOLON   = (0x80+117);
    public static final char KEY_BROPEN      = (0x80+118);
    public static final char KEY_BRCLOSE     = (0x80+119);
    public static final char KEY_BSLASH      = (0x80+120);
    public static final char KEY_MULTPLY     = (0x80+121);

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
    
    public static int getFromDosKey(final int source) {
        return DosKeys.v[Math.max(0, Math.min(source, 0x58))].listenKey;
    }
    
    public static int toDosKey(final int source) {
        final int index = Arrays.binarySearch(DosKeys.l, () -> source, KeyCode::compareCode);
        
        if (index > -1) {
            return ((Enum) DosKeys.l[index]).ordinal();
        }
        
        return 0;
    }

    /**
     * Maps DOS keys for whatever crap we use.
     * The order of these is important! Do not move.
     *  - Good Sign 2017/04/19
     */
    enum DosKeys implements KeyCode {
        /*  0 */ NULL(KEY_NULL),
        /*  1 */ SC_ESCAPEE(KEY_ESCAPE),
        /*  2 */ SC_1(0x100 + VK_1),
        /*  3 */ SC_2(0x100 + VK_2),
        /*  4 */ SC_3(0x100 + VK_3),
        /*  5 */ SC_4(0x100 + VK_4),
        /*  6 */ SC_5(0x100 + VK_5),
        /*  7 */ SC_6(0x100 + VK_6),
        /*  8 */ SC_7(0x100 + VK_7),
        /*  9 */ SC_8(0x100 + VK_8),
        /* 10 */ SC_9(0x100 + VK_9),
        /* 11 */ SC_0(0x100 + VK_0),
        /* 12 */ SC_MINUS(KEY_MINUS),
        /* 13 */ SC_EQUALS(KEY_EQUALS),
        /* 14 */ SC_BACKSPACE(KEY_BACKSPACE),
        /* 15 */ SC_TAB(KEY_TAB),
        /* 16 */ SC_Q('q'),
        /* 17 */ SC_W('w'),
        /* 18 */ SC_E('e'),
        /* 19 */ SC_R('r'),
        /* 20 */ SC_T('t'),
        /* 21 */ SC_Y('y'),
        /* 22 */ SC_U('u'),
        /* 23 */ SC_I('i'),
        /* 24 */ SC_O('o'),
        /* 25 */ SC_P('p'),
        /* 26 */ SC_LEFTBRACKET(KEY_BROPEN),
        /* 27 */ SC_RIGHTBRACKET(KEY_BRCLOSE),
        /* 28 */ SC_ENTER(KEY_ENTER),
        /* 29 */ SC_CONTROL(KEY_CTRL),
        /* 30 */ SC_A('a'),
        /* 31 */ SC_S('s'),
        /* 32 */ SC_D('d'),
        /* 33 */ SC_F('f'),
        /* 34 */ SC_G('g'),
        /* 35 */ SC_H('h'),
        /* 36 */ SC_J('j'),
        /* 37 */ SC_K('k'),
        /* 38 */ SC_L('l'),
        /* 39 */ SC_SEMICOLON(KEY_SEMICOLON),
        /* 40 */ SC_QUOTE(KEY_QUOTE),
        /* 41 */ SC_TILDE(KEY_NULL), // Java can't get its code
        /* 42 */ SC_LSHIFT(KEY_NULL), // Currently no logit to detect left or right shift, both work, map to right 54
        /* 43 */ SC_BACKSLASH(KEY_BSLASH),
        /* 44 */ SC_Z('z'),
        /* 45 */ SC_X('x'),
        /* 46 */ SC_C('c'),
        /* 47 */ SC_V('v'),
        /* 48 */ SC_B('b'),
        /* 49 */ SC_N('n'),
        /* 50 */ SC_M('m'),
        /* 51 */ SC_COMMA(KEY_COMMA),
        /* 52 */ SC_PERIOD(KEY_PERIOD),
        /* 53 */ SC_SLASH(KEY_KPADSLASH),
        /* 54 */ SC_RSHIFT(KEY_SHIFT),
        /* 55 */ SC_MULTIPLY(KEY_MULTPLY),
        /* 56 */ SC_ALT(KEY_ALT),
        /* 57 */ SC_SPACE(KEY_SPACE),
        /* 58 */ SC_CAPSLOCK(KEY_CAPSLOCK),
        /* 59 */ SC_F1(KEY_F1),
        /* 60 */ SC_F2(KEY_F2),
        /* 61 */ SC_F3(KEY_F3),
        /* 62 */ SC_F4(KEY_F4),
        /* 63 */ SC_F5(KEY_F5),
        /* 64 */ SC_F6(KEY_F6),
        /* 65 */ SC_F7(KEY_F7),
        /* 66 */ SC_F8(KEY_F8),
        /* 67 */ SC_F9(KEY_F9),
        /* 68 */ SC_F10(KEY_F10),
        /* 69 */ SC_NUMLOCK(KEY_NUMLOCK),
        /* 70 */ SC_SCROLLLOCK(KEY_SCROLLLOCK),
        /* 71 */ SC_HOME(KEY_HOME),
        /* 72 */ SC_UP(KEY_UPARROW),
        /* 73 */ SC_PAGEUP(KEY_PGUP),
        /* 74 */ SC_4A(KEY_NULL),
        /* 75 */ SC_LEFT(KEY_LEFTARROW),
        /* 76 */ SC_4C(KEY_NULL),
        /* 77 */ SC_RIGHT(KEY_RIGHTARROW),
        /* 78 */ SC_PLUS(KEY_PLUSPAD),
        /* 79 */ SC_END(KEY_END),
        /* 80 */ SC_DOWN(KEY_DOWNARROW),
        /* 81 */ SC_PAGEDOWN(KEY_PGDN),
        /* 82 */ SC_INSERT(KEY_INS),
        /* 83 */ SC_DELETE(KEY_DEL),
        /* 84 */ SC_54(KEY_NULL),
        /* 85 */ SC_55(KEY_NULL),
        /* 86 */ SC_56(KEY_NULL),
        /* 87 */ SC_F11(KEY_F11),
        /* 88 */ SC_F12(KEY_F12);

        private final char listenKey;
        private final static DosKeys[] v = values();
        private final static KeyCode[] l = values();

        DosKeys(int listenKey) {
            this.listenKey = (char) listenKey;
        }

        @Override
        public int getKeyCode() {
            return listenKey;
        }
    
        static {
            Arrays.sort(l, KeyCode::compareCode);
        }
    }
    
    @FunctionalInterface
    interface KeyCode {
        int getKeyCode();

        default int compareCode(KeyCode o) {
            return Integer.compare(getKeyCode(), o.getKeyCode());
        }
    }
}