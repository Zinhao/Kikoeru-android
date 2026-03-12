package com.zinhao.kikoeru.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.zinhao.kikoeru.Api;
import com.zinhao.kikoeru.data.model.Result;
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
    
    // 查询参数
    private final MutableLiveData<Integer> workType = new MutableLiveData<>(TYPE_ALL_WORK);
    private final MutableLiveData<Integer> currentPage = new MutableLiveData<>(1);
    private final MutableLiveData<String> order = new MutableLiveData<>("id");
    private final MutableLiveData<Integer> tagId = new MutableLiveData<>(-1);
    private final MutableLiveData<String> vaId = new MutableLiveData<>("");
    private final MutableLiveData<String> searchKeyword = new MutableLiveData<>("");
    
    // 数据
    private final MutableLiveData<List<Work>> works = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(false);
    
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
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        LiveData<Result<WorksResponse>> source;
        
        switch (type) {
            case TYPE_TAG_WORK:
                int tid = tagId.getValue() != null ? tagId.getValue() : -1;
                source = workRepository.getWorksByTag(page, tid);
                break;
            case TYPE_VA_WORK:
                String vid = vaId.getValue() != null ? vaId.getValue() : "";
                source = workRepository.getWorksByVa(page, vid);
                break;
            case TYPE_SELF_LISTENING:
                source = workRepository.getWorksByProgress(Api.FILTER_LISTENING, page);
                break;
            case TYPE_SELF_LISTENED:
                source = workRepository.getWorksByProgress(Api.FILTER_LISTENED, page);
                break;
            case TYPE_SELF_MARKED:
                source = workRepository.getWorksByProgress(Api.FILTER_MARKED, page);
                break;
            case TYPE_SELF_REPLAY:
                source = workRepository.getWorksByProgress(Api.FILTER_REPLAY, page);
                break;
            case TYPE_SELF_POSTPONED:
                source = workRepository.getWorksByProgress(Api.FILTER_POSTPONED, page);
                break;
            case TYPE_LOCAL_WORK:
                loadLocalWorks();
                return;
            default:
                source = workRepository.getWorks(page, order.getValue(), sortOrder, 1);
                break;
        }
        
        observeWorksResult(source);
    }
    
    /**
     * 加载本地作品
     */
    private void loadLocalWorks() {
        LiveData<Result<List<Work>>> source = workRepository.getLocalWorks();
        source.observeForever(new Observer<Result<List<Work>>>() {
            @Override
            public void onChanged(Result<List<Work>> result) {
                source.removeObserver(this);
                isLoading.setValue(false);
                
                if (result.isSuccess()) {
                    works.setValue(result.getData());
                    totalCount.setValue(result.getData() != null ? result.getData().size() : 0);
                    hasMore.setValue(false);
                } else {
                    errorMessage.setValue(result.getMessage());
                }
            }
        });
    }
    
    /**
     * 观察作品列表结果
     */
    private void observeWorksResult(LiveData<Result<WorksResponse>> source) {
        source.observeForever(new Observer<Result<WorksResponse>>() {
            @Override
            public void onChanged(Result<WorksResponse> result) {
                source.removeObserver(this);
                isLoading.setValue(false);
                
                if (result.isSuccess()) {
                    WorksResponse response = result.getData();
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
                        works.setValue(currentList);
                        
                        // 更新分页信息
                        if (response.getPagination() != null) {
                            totalCount.setValue(response.getPagination().getTotalCount());
                            hasMore.setValue(response.getPagination().hasNextPage());
                        }
                    }
                } else {
                    errorMessage.setValue(result.getMessage());
                }
            }
        });
    }
    
    /**
     * 加载下一页
     */
    public void loadNextPage() {
        if (Boolean.TRUE.equals(isLoading.getValue())) {
            return;
        }
        if (Boolean.FALSE.equals(hasMore.getValue())) {
            return;
        }
        int page = currentPage.getValue() != null ? currentPage.getValue() : 1;
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
    
    private String makeSort() {
        return Api.makeSort();
    }
}
