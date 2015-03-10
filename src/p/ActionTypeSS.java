package p;

import doom.thinker_t;

/** Generic single-thinker argument action function. 
 *  Useful for handling stuff such as sector actions, doors, etc.
 *  or special thinker objects that don't qualify as mobj_t's.
 * 
 * @author velktron
 *
 * @param <T>
 */

public interface ActionTypeSS<T extends thinker_t>{
	public void invoke (T a);
}