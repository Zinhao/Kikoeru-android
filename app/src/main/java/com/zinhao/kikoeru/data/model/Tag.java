package com.zinhao.kikoeru.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 标签/分类模型
 */
public class Tag {
    @SerializedName("id")
    private int id;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("i18n")
    private TagI18n i18n;
    
    public Tag() {
    }
    
    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
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
    
    public TagI18n getI18n() {
        return i18n;
    }
    
    public void setI18n(TagI18n i18n) {
        this.i18n = i18n;
    }
    
    /**
     * 获取本地化名称（优先简体中文）
     */
    public String getLocalizedName() {
        if (i18n != null && i18n.getZhCn() != null && !i18n.getZhCn().isEmpty()) {
            return i18n.getZhCn();
        }
        return name;
    }
    
    /**
     * 标签国际化信息
     */
    public static class TagI18n {
        @SerializedName("zh-cn")
        private String zhCn;
        
        @SerializedName("ja-jp")
        private String jaJp;
        
        @SerializedName("en-us")
        private String enUs;
        
        public String getZhCn() {
            return zhCn;
        }
        
        public void setZhCn(String zhCn) {
            this.zhCn = zhCn;
        }
        
        public String getJaJp() {
            return jaJp;
        }
        
        public void setJaJp(String jaJp) {
            this.jaJp = jaJp;
        }
        
        public String getEnUs() {
            return enUs;
        }
        
        public void setEnUs(String enUs) {
            this.enUs = enUs;
        }
    }
}
