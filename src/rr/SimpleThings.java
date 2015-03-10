package rr;

/**
 * A very "simple" things class which just does serial rendering and uses all
 * the base methods from AbstractThings.
 * 
 * @author velktron
 * @param <T>
 * @param <V>
 */


public final class SimpleThings<T,V>
        extends AbstractThings<T,V> {

    public SimpleThings(Renderer<T, V> R) {
        super(R);
    }

    @Override
    public void completeColumn() {
        colfunc.invoke();
        }
}
