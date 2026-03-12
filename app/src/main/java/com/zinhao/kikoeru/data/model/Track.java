package com.zinhao.kikoeru.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 音轨/文件模型
 */
public class Track {
    @SerializedName("id")
    private int id;
    
    @SerializedName("work_id")
    private int workId;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("file_name")
    private String fileName;
    
    @SerializedName("work_title")
    private String workTitle;
    
    @SerializedName("work")
    private Work work;
    
    @SerializedName("hash")
    private String hash;
    
    @SerializedName("children")
    private List<Track> children;
    
    @SerializedName("is_se")
    private boolean isSE;
    
    @SerializedName("duration")
    private double duration;
    
    @SerializedName("size")
    private long size;
    
    @SerializedName("type")
    private String type;
    
    // 本地文件路径（用于本地缓存）
    private String localPath;
    
    // 当前播放进度（毫秒）
    private long currentPosition;
    
    public Track() {
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getWorkId() {
        return workId;
    }
    
    public void setWorkId(int workId) {
        this.workId = workId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getWorkTitle() {
        return workTitle;
    }
    
    public void setWorkTitle(String workTitle) {
        this.workTitle = workTitle;
    }
    
    public Work getWork() {
        return work;
    }
    
    public void setWork(Work work) {
        this.work = work;
    }
    
    public String getHash() {
        return hash;
    }
    
    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public List<Track> getChildren() {
        return children;
    }
    
    public void setChildren(List<Track> children) {
        this.children = children;
    }
    
    public boolean isSE() {
        return isSE;
    }
    
    public void setSE(boolean SE) {
        isSE = SE;
    }
    
    public double getDuration() {
        return duration;
    }
    
    public void setDuration(double duration) {
        this.duration = duration;
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getLocalPath() {
        return localPath;
    }
    
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }
    
    public long getCurrentPosition() {
        return currentPosition;
    }
    
    public void setCurrentPosition(long currentPosition) {
        this.currentPosition = currentPosition;
    }
    
    /**
     * 是否有子文件（目录）
     */
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
    
    /**
     * 是否是音频文件
     */
    public boolean isAudio() {
        if (type == null) return false;
        return type.startsWith("audio/") || 
               fileName != null && (fileName.endsWith(".mp3") || 
                                    fileName.endsWith(".flac") || 
                                    fileName.endsWith(".wav") ||
                                    fileName.endsWith(".m4a") ||
                                    fileName.endsWith(".ogg"));
    }
    
    /**
     * 是否是视频文件
     */
    public boolean isVideo() {
        if (type == null) return false;
        return type.startsWith("video/") ||
               fileName != null && (fileName.endsWith(".mp4") ||
                                    fileName.endsWith(".webm") ||
                                    fileName.endsWith(".mkv"));
    }
    
    /**
     * 格式化的文件大小
     */
    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024));
        }
    }
    
    /**
     * 格式化的时长
     */
    public String getFormattedDuration() {
        int seconds = (int) duration;
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
}
