package v.graphics;

/**
 * Those guys sure have an obsession with shit...
 * this is supposed to do some lame-ass transpose.
 */
public interface ColumnTranspose {
    default void shittyColumnTranspose(byte[] w, int width, int height) {
        final byte[] dest = new byte[w.length];

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; x++) {
                dest[x * height + y] = w[y * width + x];
                //dest[(1+x)*height+y] = array[y*width+(1+x)];
            }
        }
        System.arraycopy(dest, 0, w, 0, w.length);
    }
    
    default void shittyColumnTranspose(short[] w, int width, int height) {
        final short[] dest = new short[w.length];

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; x++) {
                dest[x * height + y] = w[y * width + x];
                //dest[(1+x)*height+y] = array[y*width+(1+x)];
            }
        }
        System.arraycopy(dest, 0, w, 0, w.length);
    }
    
    default void shittyColumnTranspose(int[] w, int width, int height) {
        final int[] dest = new int[w.length];

        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; x++) {
                dest[x * height + y] = w[y * width + x];
                //dest[(1+x)*height+y] = array[y*width+(1+x)];
            }
        }
        System.arraycopy(dest, 0, w, 0, w.length);
    }
}
