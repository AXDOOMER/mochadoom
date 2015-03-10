package p;

import data.mapthing_t;
import doom.thinker_t;

public interface ThinkerList {

    void AddThinker(thinker_t thinker);
    void RemoveThinker(thinker_t thinker);
    void InitThinkers();
    thinker_t getRandomThinker();
    thinker_t getThinkerCap();
}
