package com.enosistudio.doom.hu;

import com.enosistudio.doom.doom.SourceCode.HU_Stuff;
import static com.enosistudio.doom.doom.SourceCode.HU_Stuff.HU_Responder;
import com.enosistudio.doom.doom.event_t;
import com.enosistudio.doom.rr.patch_t;

public interface IHeadsUp {

	void Ticker();

	void Erase();

	void Drawer();

    @HU_Stuff.C(HU_Responder)
	boolean Responder(event_t ev);

	patch_t[] getHUFonts();

	char dequeueChatChar();

	void Init();

	void setChatMacro(int i, String s);

	void Start();

	void Stop();

}
