package com.enosistudio.doom.savegame;

import com.enosistudio.doom.p.ThinkerList;

public interface ILoadSaveGame {
    void setThinkerList(ThinkerList li);
    void doSave(ThinkerList li);
    void doLoad(ThinkerList li);
}
