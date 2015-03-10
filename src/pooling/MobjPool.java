package pooling;

import doom.think_t;
import p.Actions;
import p.mobj_t;
import s.AudioChunk;

/* The idea is to reuse mobjs...however in practice that 
 * doesn't work out so well, with everything "freezing" after 
 * a while */

public class MobjPool extends ObjectPool<mobj_t>
{

	Actions A;
	
    public MobjPool(Actions A)    
    {
    	// A reasonable time limit for map objects?
    	super(1000L);
    	this.A=A;
    }

    protected mobj_t create()
    {
        /*
        for (int i=0;i<8192;i++){
            locked.push(new mobj_t(A));
        }
        
        return locked.pop();*/
        
        return new mobj_t(A);
    }

    public void expire(mobj_t o)
    {
        o.function=think_t.NOP;
    }

    public boolean validate(mobj_t o)
    {
        return (o.function==think_t.NOP);
    }

}
