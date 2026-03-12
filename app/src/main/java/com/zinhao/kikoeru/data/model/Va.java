package com.zinhao.kikoeru.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 声优/配音演员模型
 */
public class Va {
    @SerializedName("id")
    private String id;
    
    @SerializedName("name")
    private String name;
    
    public Va() {
    }
    
    public Va(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Va va = (Va) o;
        return id != null && id.equals(va.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
