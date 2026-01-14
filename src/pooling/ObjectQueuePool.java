package pooling;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import mochadoom.Loggers;
import p.mobj_t;

/** A convenient object pooling class, derived from the stock ObjectPool.
 *  
 *  It's about 50% faster than calling new, and MUCH faster than ObjectPool
 *  because it doesn't do that bullshit object cleanup every so often.
 * 
 */
public abstract class ObjectQueuePool<K> {

    private static final Logger LOGGER = Loggers.getLogger(ObjectQueuePool.class.getName());

    private static final boolean D = false;

    public ObjectQueuePool(long expirationTime) {
        locked = new Stack<>();

    }

    protected abstract K create();

    public abstract boolean validate(K obj);

    public abstract void expire(K obj);

    public void drain() {
        locked.clear();
    }

    public K checkOut() {

        K t;
        if (!locked.isEmpty()) {
            return locked.pop();

        }

        t = create();
        return t;
    }

    public void checkIn(K t) {
        if (D) {
            if (t instanceof mobj_t) {
                LOGGER.log(Level.FINE, String.format("Object %s returned to the pool", t.toString()));
            }
        }
        locked.push(t);
    }

    protected Stack<K> locked;
    // private Hashtable<K,Long> unlocked;
}
