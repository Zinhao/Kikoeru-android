package com.zinhao.kikoeru;

import org.json.JSONObject;

public interface MusicChangeListener {
    void onAlbumChange(long rjNumber);

    void onAudioChange(JSONObject audio);

    void onStatusChange(int status);
}
