package hu;

import rr.patch_t;
import doom.event_t;

public interface IHeadsUp {

	void Ticker();

	void Erase();

	void Drawer();

	boolean Responder(event_t ev);

	patch_t[] getHUFonts();

	char dequeueChatChar();

	void Init();

	void setChatMacro(int i, String s);

	void Start();

	void Stop();

}
