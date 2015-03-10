package defines;


/** Game mode handling - identify IWAD version
  to handle IWAD dependend animations etc.
  */
public enum GameMode_t 
{
shareware,    // DOOM 1 shareware, E1, M9
registered,   // DOOM 1 registered, E3, M27
commercial,   // DOOM 2 retail, E1 M34
// DOOM 2 german edition not handled
retail,   // DOOM 1 retail, E4, M36
pack_tnt, // TNT mission pack
pack_plut,    // Plutonia pack
pack_xbla,    // XBLA Doom. How you got hold of it, I don't care :-p
indetermined  // Well, no IWAD found.  
} ;
