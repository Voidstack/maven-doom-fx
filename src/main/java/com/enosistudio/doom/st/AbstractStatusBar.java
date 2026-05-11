package com.enosistudio.doom.st;

import com.enosistudio.doom.doom.DoomMain;

public abstract class AbstractStatusBar implements IDoomStatusBar {
    protected final DoomMain<?, ?> DOOM;

    public AbstractStatusBar(DoomMain<?, ?> DOOM) {
        this.DOOM = DOOM;
    }
}
