package com.zinhao.kikoeru.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "audio_lrc_bind",
        indices = @Index(value = "audioPath", unique = true))
public class AudioLrcBind {
    @PrimaryKey(autoGenerate = true)
    private Long id;
    private long rjNumber;
    private String audioPath;
    private String lrcPath;

    public AudioLrcBind(long rjNumber, String audioPath, String lrcPath) {
        this.rjNumber = rjNumber;
        this.audioPath = audioPath;
        this.lrcPath = lrcPath;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getRjNumber() {
        return rjNumber;
    }

    public void setRjNumber(long rjNumber) {
        this.rjNumber = rjNumber;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String getLrcPath() {
        return lrcPath;
    }

    public void setLrcPath(String lrcPath) {
        this.lrcPath = lrcPath;
    }
}
