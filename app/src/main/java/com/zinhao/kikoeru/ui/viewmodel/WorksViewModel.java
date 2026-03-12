package com.zinhao.kikoeru.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zinhao.kikoeru.Api;
import com.zinhao.kikoeru.data.model.Work;
import com.zinhao.kikoeru.data.model.WorksResponse;
import com.zinhao.kikoeru.data.repository.WorkRepository;
import com.zinhao.kikoeru.utils.SingleLiveEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 作品列表 ViewModel
 */
public class WorksViewModel extends ViewModel {
    
    private final WorkRepository workRepository;
    
    // 筛选类型
    public static final int TYPE_ALL_WORK = 491;
    public static final int TYPE_SELF_LISTENING = 492;
    public static final int TYPE_SELF_LISTENED = 493;
    public static final int TYPE_SELF_MARKED = 494;
    public static final int TYPE_SELF_REPLAY = 495;
    public static final int TYPE_SELF_POSTPONED = 496;
    public static final int TYPE_TAG_WORK = 497;
    public static final int TYPE_LOCAL_WORK = 498;
    public static final int TYPE_VA_WORK = 499;
    public static final int TYPE_CIRCLES_WORK = 500;
    
    // 查询参数
    private final MutableLiveData<Integer> workType = new MutableLiveData<>(TYPE_ALL_WORK);
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(1);
    private final MutableLiveData<String> order = new MutableLiveData<>("id");
    private final MutableLiveData<Integer> tagId = new MutableLiveData<>(-1);
    private final MutableLiveData<String> vaId = new MutableLiveData<>("");
    private final MutableLiveData<Long> circlesId = new MutableLiveData<>(-1L);
    private final MutableLiveData<String> circlesName = new MutableLiveData<>("");
    private final MutableLiveData<String> searchKeyword = new MutableLiveData<>("");
    
    // 数据
    private final MutableLiveData<List<Work>> works = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(true);
    
    // 事件
    private final SingleLiveEvent<Work> navigateToDetail = new SingleLiveEvent<>();
    
    public WorksViewModel() {
        this.workRepository = WorkRepository.getInstance();
    }
    
    // Getters
    public LiveData<List<Work>> getWorks() {
        return works;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Integer> getTotalCount() {
        return totalCount;
    }
    
    public LiveData<Boolean> getHasMore() {
        return hasMore;
    }
    
    public LiveData<Integer> getWorkType() {
        return workType;
    }
    
    public LiveData<Work> getNavigateToDetail() {
        return navigateToDetail;
    }
    
    public LiveData<String> getCirclesName() {
        return circlesName;
    }
    
    // Setters
    public void setWorkType(int type) {
        if (workType.getValue() != null && workType.getValue() == type) {
            return;
        }
        workType.setValue(type);
        resetAndLoad();
    }
    
    public void setTagId(int id) {
        tagId.setValue(id);
        workType.setValue(TYPE_TAG_WORK);
        resetAndLoad();
    }
    
    public void setVaId(String id) {
        vaId.setValue(id);
        workType.setValue(TYPE_VA_WORK);
        resetAndLoad();
    }
    
    public void setCirclesId(long id, String name) {
        circlesId.setValue(id);
        circlesName.setValue(name);
        workType.setValue(TYPE_CIRCLES_WORK);
        resetAndLoad();
    }
    
    public void setSearchKeyword(String keyword) {
        searchKeyword.setValue(keyword);
        resetAndLoad();
    }
    
    public void setOrder(String newOrder) {
        order.setValue(newOrder);
        resetAndLoad();
    }
    
    /**
     * 重置并加载数据
     */
    private void resetAndLoad() {
        currentPage.setValue(1);
        works.setValue(new ArrayList<>());
        totalCount.setValue(0);  // 重置总数
        hasMore.setValue(true);  // 重置为 true，允许加载更多
        android.util.Log.d("WorksViewModel", "resetAndLoad: page=1, hasMore=true");
        loadWorks();
    }
    
    /**
     * 加载作品列表
     */
    public void loadWorks() {
        if (Boolean.TRUE.equals(isLoading.getValue())) {
            return;
        }
        
        int type = workType.getValue() != null ? workType.getValue() : TYPE_ALL_WORK;
        int page = currentPage.getValue() != null ? currentPage.getValue() : 1;
        String sortOrder = makeSort();
        Boolean currentHasMore = hasMore.getValue();
        
        android.util.Log.d("WorksViewModel", "loadWorks START: type=" + type + ", page=" + page + ", hasMore=" + currentHasMore);
        
        // 本地作品不需要分页
        if (type == TYPE_LOCAL_WORK) {
            hasMore.setValue(false);
        }
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        WorkRepository.ResultCallback<WorksResponse> callback = new WorkRepository.ResultCallback<WorksResponse>() {
            @Override
            public void onSuccess(WorksResponse response) {
                android.util.Log.d("WorksViewModel", "onSuccess: " + (response.getWorks() != null ? response.getWorks().size() : 0) + " works");
                isLoading.postValue(false);
                
                if (response != null) {
                    List<Work> currentList = works.getValue();
                    if (currentList == null) {
                        currentList = new ArrayList<>();
                    }
                    
                    // 第一页清空，否则追加
                    int currentPageValue = currentPage.getValue() != null ? currentPage.getValue() : 1;
                    if (currentPageValue == 1) {
                        currentList.clear();
                    }
                    
                    if (response.getWorks() != null) {
                        currentList.addAll(response.getWorks());
                    }
                    works.postValue(currentList);
                    
                    // 更新分页信息
                    if (response.getPagination() != null) {
                        int currentPageNum = response.getPagination().getCurrentPage();
                        int pageSize = response.getPagination().getPageSize() > 0 ? response.getPagination().getPageSize() : 20;
                        int total = response.getPagination().getTotalCount();
                        
                        // 自己计算是否有更多页（不依赖服务器返回的totalPage）
                        boolean more = currentList.size() < total;
                        
                        android.util.Log.d("WorksViewModel", "Pagination: current=" + currentPageNum + 
                            ", loaded=" + currentList.size() + ", total=" + total + 
                            ", pageSize=" + pageSize + ", hasMore=" + more);
                        
                        totalCount.postValue(total);
                        hasMore.postValue(more);
                    } else {
                        // 如果没有分页信息，使用当前列表大小作为总数
                        android.util.Log.d("WorksViewModel", "No pagination, using list size");
                        totalCount.postValue(currentList.size());
                        hasMore.postValue(false);
                    }
                }
            }
            
            @Override
            public void onError(String message, Throwable error) {
                android.util.Log.e("WorksViewModel", "onError: " + message);
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        };
        
        switch (type) {
            case TYPE_TAG_WORK:
                int tid = tagId.getValue() != null ? tagId.getValue() : -1;
                workRepository.getWorksByTag(page, tid, callback);
                break;
            case TYPE_VA_WORK:
                String vid = vaId.getValue() != null ? vaId.getValue() : "";
                workRepository.getWorksByVa(page, vid, callback);
                break;
            case TYPE_CIRCLES_WORK:
                Long cid = circlesId.getValue();
                if (cid != null && cid != -1) {
                    workRepository.getWorksByCircles(page, cid, callback);
                } else {
                    workRepository.getWorks(page, order.getValue(), sortOrder, 1, callback);
                }
                break;
            case TYPE_SELF_LISTENING:
                workRepository.getWorksByProgress(Api.FILTER_LISTENING, page, callback);
                break;
            case TYPE_SELF_LISTENED:
                workRepository.getWorksByProgress(Api.FILTER_LISTENED, page, callback);
                break;
            case TYPE_SELF_MARKED:
                workRepository.getWorksByProgress(Api.FILTER_MARKED, page, callback);
                break;
            case TYPE_SELF_REPLAY:
                workRepository.getWorksByProgress(Api.FILTER_REPLAY, page, callback);
                break;
            case TYPE_SELF_POSTPONED:
                workRepository.getWorksByProgress(Api.FILTER_POSTPONED, page, callback);
                break;
            case TYPE_LOCAL_WORK:
                loadLocalWorks();
                return;
            default:
                workRepository.getWorks(page, order.getValue(), sortOrder, 1, callback);
                break;
        }
    }
    
    /**
     * 加载本地作品
     */
    private void loadLocalWorks() {
        workRepository.getLocalWorks(new WorkRepository.ResultCallback<List<Work>>() {
            @Override
            public void onSuccess(List<Work> result) {
                isLoading.postValue(false);
                works.postValue(result != null ? result : new ArrayList<>());
                totalCount.postValue(result != null ? result.size() : 0);
                hasMore.postValue(false);
            }
            
            @Override
            public void onError(String message, Throwable error) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }
    
    /**
     * 加载下一页
     */
    public void loadNextPage() {
        if (Boolean.TRUE.equals(isLoading.getValue())) {
            android.util.Log.d("WorksViewModel", "loadNextPage: already loading");
            return;
        }
        Boolean more = hasMore.getValue();
        if (more != null && !more) {
            android.util.Log.d("WorksViewModel", "loadNextPage: no more data");
            return;
        }
        int page = currentPage.getValue() != null ? currentPage.getValue() : 1;
        android.util.Log.d("WorksViewModel", "loadNextPage: loading page " + (page + 1));
        currentPage.setValue(page + 1);
        loadWorks();
    }
    
    /**
     * 刷新数据
     */
    public void refresh() {
        resetAndLoad();
    }
    
    /**
     * 点击作品项
     */
    public void onWorkClick(Work work) {
        navigateToDetail.setValue(work);
    }
    
    /**
     * 切换排序
     */
    public void toggleSort() {
        Api.setOrder(order.getValue());
        String newOrder = order.getValue();
        resetAndLoad();
    }
    
    /**
     * 获取当前标题
     */
    public String getCurrentTitle() {
        int type = workType.getValue() != null ? workType.getValue() : TYPE_ALL_WORK;
        switch (type) {
            case TYPE_SELF_LISTENING:
                return "listening";
            case TYPE_SELF_LISTENED:
                return "listened";
            case TYPE_SELF_MARKED:
                return "marked";
            case TYPE_SELF_REPLAY:
                return "replay";
            case TYPE_SELF_POSTPONED:
                return "postponed";
            case TYPE_TAG_WORK:
                return "tag";
            case TYPE_VA_WORK:
                return vaId.getValue() != null ? vaId.getValue() : "";
            case TYPE_CIRCLES_WORK:
                return circlesName.getValue() != null ? circlesName.getValue() : "";
            case TYPE_LOCAL_WORK:
                return "local";
            default:
                return "all";
        }
    }
    
    public int getCurrentPage() {
        return currentPage.getValue() != null ? currentPage.getValue() : 1;
    }
    
    private String makeSort() {
        return Api.makeSort();
    }
}
