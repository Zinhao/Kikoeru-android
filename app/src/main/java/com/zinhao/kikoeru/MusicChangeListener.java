package com.zinhao.kikoeru;

import org.json.JSONObject;

public interface MusicChangeListener {
    void onAlbumChange(int rjNumber);
    void onAudioChange(JSONObject audio);
    void onStatusChange(int status);
}
