package com.zinhao.kikoeru.db;


import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "local_work_history",
        indices = {@Index(value = {"rjNumber"}, unique = true)})
public class LocalWorkHistory {
    @PrimaryKey(autoGenerate = true)
    private Long id;
    private long rjNumber;
    private long position;
    private String coverUrl;
    private String title;

    public LocalWorkHistory(long rjNumber, long position, String coverUrl, String title) {
        this.rjNumber = rjNumber;
         this.position = position;
        this.coverUrl = coverUrl;
        this.title = title;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRjNumber(long rjNumber) {
        this.rjNumber = rjNumber;
    }

    public long getRjNumber() {
        return rjNumber;
    }
}
