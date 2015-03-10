package f;

public interface IWiper {

    boolean ScreenWipe(int wipeno, int x, int y, int width, int height,
            int ticks);

    boolean EndScreen(int x, int y, int width, int height);

    boolean StartScreen(int x, int y, int width, int height);

}
