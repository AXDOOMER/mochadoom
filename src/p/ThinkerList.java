package p;

import doom.SourceCode.P_Tick;
import static doom.SourceCode.P_Tick.*;
import doom.thinker_t;

public interface ThinkerList {

    @P_Tick.C(P_AddThinker)
    void AddThinker(thinker_t thinker);
    @P_Tick.C(P_RemoveThinker)
    void RemoveThinker(thinker_t thinker);
    @P_Tick.C(P_InitThinkers)
    void InitThinkers();
    
    thinker_t getRandomThinker();
    thinker_t getThinkerCap();
}
