package v;

public class ColorTint {
    public ColorTint(int r, int g, int b, float tint) {
        super();
        this.r = r;
        this.g = g;
        this.b = b;
        this.tint = tint;
    }

    public int r, g, b;

    public float tint;

    public static final ColorTint[] tints = { new ColorTint(0, 0, 0, .0f), // 0
                                                                           // Normal
            new ColorTint(255, 2, 3, 0.11f), // 1 Unused. 11% red tint of
                                             // RGB(252, 2, 3).
            new ColorTint(255, 0, 0, 0.22f), // 2
            new ColorTint(255, 0, 0, 0.33f), // 3
            new ColorTint(255, 0, 0, 0.44f), // 4
            new ColorTint(255, 0, 0, 0.55f), // 5
            new ColorTint(255, 0, 0, 0.66f), // 6
            new ColorTint(255, 0, 0, 0.77f), // 7
            new ColorTint(255, 0, 0, 0.88f), // 8
            new ColorTint(215, 185, 68, 0.12f), // 9
            new ColorTint(215, 185, 68, 0.25f), // 10
            new ColorTint(215, 185, 68, 0.375f), // 11
            new ColorTint(215, 185, 68, 0.50f), // 12
            new ColorTint(3, 253, 3, 0.125f) // 13

        };
}