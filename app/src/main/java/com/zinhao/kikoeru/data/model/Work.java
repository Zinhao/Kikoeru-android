package com.zinhao.kikoeru.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 作品/音声模型
 */
public class Work {
    @SerializedName("id")
    private int id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("circle_id")
    private int circleId;
    
    @SerializedName("circle")
    private Circle circle;
    
    @SerializedName("nsfw")
    private boolean nsfw;
    
    @SerializedName("release")
    private String releaseDate;
    
    @SerializedName("series_id")
    private Integer seriesId;
    
    @SerializedName("series")
    private Series series;
    
    @SerializedName("dl_count")
    private int dlCount;
    
    @SerializedName("price")
    private int price;
    
    @SerializedName("review_count")
    private int reviewCount;
    
    @SerializedName("rate_average_2dp")
    private double rateAverage;
    
    @SerializedName("rate_count")
    private int rateCount;
    
    @SerializedName("rank")
    private Rank rank;
    
    @SerializedName("tags")
    private List<Tag> tags;
    
    @SerializedName("vas")
    private List<Va> vas;
    
    // 本地缓存标记
    private boolean isLocalWork;
    
    // 本地作品来源服务器
    private String host;
    
    public Work() {
    }
    
    // Getters and Setters
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
    
    public int getCircleId() {
        return circleId;
    }
    
    public void setCircleId(int circleId) {
        this.circleId = circleId;
    }
    
    public Circle getCircle() {
        return circle;
    }
    
    public void setCircle(Circle circle) {
        this.circle = circle;
    }
    
    public boolean isNsfw() {
        return nsfw;
    }
    
    public void setNsfw(boolean nsfw) {
        this.nsfw = nsfw;
    }
    
    public String getReleaseDate() {
        return releaseDate;
    }
    
    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }
    
    public Integer getSeriesId() {
        return seriesId;
    }
    
    public void setSeriesId(Integer seriesId) {
        this.seriesId = seriesId;
    }
    
    public Series getSeries() {
        return series;
    }
    
    public void setSeries(Series series) {
        this.series = series;
    }
    
    public int getDlCount() {
        return dlCount;
    }
    
    public void setDlCount(int dlCount) {
        this.dlCount = dlCount;
    }
    
    public int getPrice() {
        return price;
    }
    
    public void setPrice(int price) {
        this.price = price;
    }
    
    public int getReviewCount() {
        return reviewCount;
    }
    
    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }
    
    public double getRateAverage() {
        return rateAverage;
    }
    
    public void setRateAverage(double rateAverage) {
        this.rateAverage = rateAverage;
    }
    
    public int getRateCount() {
        return rateCount;
    }
    
    public void setRateCount(int rateCount) {
        this.rateCount = rateCount;
    }
    
    public Rank getRank() {
        return rank;
    }
    
    public void setRank(Rank rank) {
        this.rank = rank;
    }
    
    public List<Tag> getTags() {
        return tags;
    }
    
    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
    
    public List<Va> getVas() {
        return vas;
    }
    
    public void setVas(List<Va> vas) {
        this.vas = vas;
    }
    
    public boolean isLocalWork() {
        return isLocalWork;
    }
    
    public void setLocalWork(boolean localWork) {
        isLocalWork = localWork;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    /**
     * 获取RJ号（从id格式化）
     */
    public String getRjNumber() {
        return String.format("RJ%06d", id);
    }
    
    /**
     * 获取社团名称
     */
    public String getCircleName() {
        return circle != null ? circle.getName() : "";
    }
    
    /**
     * 获取标签名称列表
     */
    public String getTagNames() {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.size() && i < 3; i++) {
            if (i > 0) sb.append(" ");
            sb.append(tags.get(i).getLocalizedName());
        }
        return sb.toString();
    }
    
    /**
     * 获取声优名称列表
     */
    public String getVaNames() {
        if (vas == null || vas.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vas.size() && i < 3; i++) {
            if (i > 0) sb.append(" ");
            sb.append(vas.get(i).getName());
        }
        return sb.toString();
    }
    
    // 嵌套类
    public static class Circle {
        @SerializedName("id")
        private int id;
        
        @SerializedName("name")
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
    
    public static class Series {
        @SerializedName("id")
        private int id;
        
        @SerializedName("name")
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
    
    public static class Rank {
        @SerializedName("category")
        private String category;
        
        @SerializedName("term")
        private String term;
        
        @SerializedName("rank")
        private int rank;
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public String getTerm() {
            return term;
        }
        
        public void setTerm(String term) {
            this.term = term;
        }
        
        public int getRank() {
            return rank;
        }
        
        public void setRank(int rank) {
            this.rank = rank;
        }
    }
}
