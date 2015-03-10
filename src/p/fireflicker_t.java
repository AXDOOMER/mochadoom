package p;

import m.IRandom;
import rr.SectorAction;

//
// P_LIGHTS
//

public class fireflicker_t extends SectorAction{
	
     private IRandom RND;
    
     public int     count;
     public int     maxlight;
     public int     minlight;
     
     public fireflicker_t(IRandom RND){
         this.RND=RND;
     }
     
     //
     // T_FireFlicker
     //
     public void FireFlicker() {
         int amount;

         if (--count != 0)
             return;

         amount = (RND.P_Random() & 3) * 16;

         if (sector.lightlevel - amount < minlight)
             sector.lightlevel = (short) minlight;
         else
             sector.lightlevel = (short) (maxlight - amount);

         count = 4;
     }
     
 } 
