package com.zinhao.kikoeru;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 作品信息（Work详情）
 */
public class WorkInfo {
    private int id;
    private String title;
    private String name;
    private String host;
    private String release;
    private int price;
    private int dlCount;
    private List<TagInfo> tags;
    private List<VaInfo> vas;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public int getDlCount() {
        return dlCount;
    }

    public void setDlCount(int dlCount) {
        this.dlCount = dlCount;
    }

    public List<TagInfo> getTags() {
        return tags != null ? tags : Collections.emptyList();
    }

    public void setTags(List<TagInfo> tags) {
        this.tags = tags;
    }

    public List<VaInfo> getVas() {
        return vas != null ? vas : Collections.emptyList();
    }

    public void setVas(List<VaInfo> vas) {
        this.vas = vas;
    }

    /**
     * 从 org.json.JSONObject 转换为 WorkInfo
     */
    public static WorkInfo fromJson(JSONObject json) throws JSONException {
        WorkInfo info = new WorkInfo();
        info.setId(json.optInt("id", 0));
        info.setTitle(json.optString("title"));
        info.setName(json.optString("name"));
        info.setHost(json.optString(JSONConst.Work.HOST));
        info.setRelease(json.optString("release"));
        info.setPrice(json.optInt("price", 0));
        info.setDlCount(json.optInt("dl_count", 0));
        
        // 解析 tags
        List<TagInfo> tagsList = new ArrayList<>();
        if (json.has("tags")) {
            JSONArray tagsJson = json.getJSONArray("tags");
            for (int i = 0; i < tagsJson.length(); i++) {
                JSONObject tagJson = tagsJson.getJSONObject(i);
                TagInfo tag = new TagInfo();
                tag.setId(tagJson.optInt("id", 0));
                tag.setName(tagJson.optString("name"));
                tagsList.add(tag);
            }
        }
        info.setTags(tagsList);
        
        // 解析 vas
        List<VaInfo> vasList = new ArrayList<>();
        if (json.has("vas")) {
            JSONArray vasJson = json.getJSONArray("vas");
            for (int i = 0; i < vasJson.length(); i++) {
                JSONObject vaJson = vasJson.getJSONObject(i);
                VaInfo va = new VaInfo();
                va.setId(vaJson.optInt("id", 0));
                va.setName(vaJson.optString("name"));
                vasList.add(va);
            }
        }
        info.setVas(vasList);
        
        return info;
    }

    /**
     * Tag 信息
     */
    public static class TagInfo {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * VA (Voice Actor) 信息
     */
    public static class VaInfo {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
