package com.zinhao.kikoeru;

import java.util.ArrayList;
import java.util.List;

/**
 * 作品目录树中的文件或文件夹项
 */
public class WorkTreeItem {
    private String title;
    private String type;
    private String hash;
    private String mediaStreamUrl;
    private boolean exists;
    private int workId;
    private String localFilePath;
    private List<WorkTreeItem> children;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getMediaStreamUrl() {
        return mediaStreamUrl;
    }

    public void setMediaStreamUrl(String mediaStreamUrl) {
        this.mediaStreamUrl = mediaStreamUrl;
    }

    public boolean isExists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public int getWorkId() {
        return workId;
    }

    public void setWorkId(int workId) {
        this.workId = workId;
    }

    public String getLocalFilePath() {
        return localFilePath;
    }

    public void setLocalFilePath(String localFilePath) {
        this.localFilePath = localFilePath;
    }

    public List<WorkTreeItem> getChildren() {
        return children;
    }

    public void setChildren(List<WorkTreeItem> children) {
        this.children = children;
    }

    public boolean isFolder() {
        return "folder".equals(type);
    }

    public boolean isAudio() {
        return "audio".equals(type);
    }

    public boolean isImage() {
        return "image".equals(type);
    }

    public boolean isText() {
        return "text".equals(type);
    }

    public int getChildrenCount() {
        return children != null ? children.size() : 0;
    }

    /**
     * 从 org.json.JSONObject 转换为 WorkTreeItem
     */
    public static WorkTreeItem fromJson(org.json.JSONObject json) throws org.json.JSONException {
        WorkTreeItem item = new WorkTreeItem();
        item.setTitle(json.optString("title"));
        item.setType(json.optString("type"));
        item.setHash(json.optString("hash"));
        item.setMediaStreamUrl(json.optString(JSONConst.WorkTree.MEDIA_STREAM_URL));
        item.setExists(json.optBoolean(JSONConst.WorkTree.EXISTS, false));
        item.setWorkId(json.optInt(JSONConst.WorkTree.WORK_ID, 0));
        item.setLocalFilePath(json.optString(JSONConst.WorkTree.MAP_FILE_PATH));
        
        if (json.has("children")) {
            org.json.JSONArray childrenJson = json.getJSONArray("children");
            List<WorkTreeItem> childrenList = new ArrayList<>();
            for (int i = 0; i < childrenJson.length(); i++) {
                childrenList.add(fromJson(childrenJson.getJSONObject(i)));
            }
            item.setChildren(childrenList);
        }
        
        return item;
    }

    /**
     * 从 org.json.JSONArray 转换为 List<WorkTreeItem>
     */
    public static List<WorkTreeItem> listFromJson(org.json.JSONArray jsonArray) throws org.json.JSONException {
        List<WorkTreeItem> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            list.add(fromJson(jsonArray.getJSONObject(i)));
        }
        return list;
    }

    /**
     * 转换为 org.json.JSONObject（用于兼容旧代码）
     */
    public org.json.JSONObject toJson() throws org.json.JSONException {
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("title", title);
        json.put("type", type);
        json.put("hash", hash);
        json.put(JSONConst.WorkTree.MEDIA_STREAM_URL, mediaStreamUrl);
        json.put(JSONConst.WorkTree.EXISTS, exists);
        json.put(JSONConst.WorkTree.WORK_ID, workId);
        json.put(JSONConst.WorkTree.MAP_FILE_PATH, localFilePath);
        
        if (children != null) {
            org.json.JSONArray childrenJson = new org.json.JSONArray();
            for (WorkTreeItem child : children) {
                childrenJson.put(child.toJson());
            }
            json.put("children", childrenJson);
        }
        
        return json;
    }
}
