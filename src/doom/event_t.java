package doom;

// Event structure.

public class event_t {
	
	public event_t(){
	}

	   public event_t(event_t t){
	        this.type = t.type;
	        this.data1 = t.data1;
	        this.data2 = t.data2;
	        this.data3 = t.data3;
	    }

	
    public event_t(evtype_t type, int data) {
        this.type = type;
        this.data1 = data;
    }

    public event_t(char c) {
        this.type = evtype_t.ev_keydown;
        this.data1 = c;
    }

    public event_t(evtype_t type, int data1, int data2, int data3) {
        this.type = type;
        this.data1 = data1;
        this.data2 = data2;
        this.data3 = data3;
    }
    
    public void setFrom(event_t t){
        this.type = t.type;
        this.data1 = t.data1;
        this.data2 = t.data2;
        this.data3 = t.data3;
    }

    public evtype_t type;

    /** keys / mouse/joystick buttons, or wheel rotation amount */
    public int data1;

    /** mouse/joystick x move */
    public int data2;

    /** mouse/joystick y move */
    public int data3;

};
