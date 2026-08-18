package com.zinhao.kikoeru;

public interface LrcRowChangeListener {
    void onSeekChange(Lrc.LrcRow currentRow);
    void onLrcChange(Lrc lrc);
}
