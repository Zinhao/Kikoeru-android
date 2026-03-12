package com.zinhao.kikoeru.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * 分页信息模型
 */
public class Pagination {
    @SerializedName("currentPage")
    private int currentPage;
    
    @SerializedName("pageSize")
    private int pageSize;
    
    @SerializedName("totalCount")
    private int totalCount;
    
    @SerializedName("totalPage")
    private int totalPage;
    
    public Pagination() {
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public int getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
    
    public int getTotalPage() {
        return totalPage;
    }
    
    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }
    
    /**
     * 是否有下一页
     */
    public boolean hasNextPage() {
        return currentPage < totalPage;
    }
    
    /**
     * 是否有上一页
     */
    public boolean hasPreviousPage() {
        return currentPage > 1;
    }
    
    /**
     * 获取下一页页码
     */
    public int getNextPage() {
        return hasNextPage() ? currentPage + 1 : currentPage;
    }
}
