package p;

import doom.think_t;
import rr.SectorAction;
import rr.line_t;
import rr.sector_t;

public class slidedoor_t extends SectorAction {
	    sdt_e	type;
	    line_t	line;
	    int		frame;
	    int		whichDoorIndex;
	    int		timer;
	    sector_t	frontsector;
	    sector_t	backsector;
	    sd_e	 status;
	    
	    public slidedoor_t(){
	    	type=sdt_e.sdt_closeOnly;
	    	status=sd_e.sd_closing;
	    	function=think_t.T_SlidingDoor;
	    }

	}