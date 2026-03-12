package com.zinhao.kikoeru.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 作品列表 API 响应
 */
public class WorksResponse {
    @SerializedName("works")
    private List<Work> works;
    
    @SerializedName("pagination")
    private Pagination pagination;
    
    public WorksResponse() {
    }
    
    public List<Work> getWorks() {
        return works;
    }
    
    public void setWorks(List<Work> works) {
        this.works = works;
    }
    
    public Pagination getPagination() {
        return pagination;
    }
    
    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
