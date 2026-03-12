package com.zinhao.kikoeru.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.zinhao.kikoeru.Api;
import com.zinhao.kikoeru.data.model.Result;
import com.zinhao.kikoeru.data.model.Track;
import com.zinhao.kikoeru.data.model.Work;
import com.zinhao.kikoeru.data.repository.WorkRepository;
import com.zinhao.kikoeru.utils.SingleLiveEvent;

import java.util.List;

/**
 * 作品详情/目录树 ViewModel
 */
public class WorkTreeViewModel extends ViewModel {
    
    private final WorkRepository workRepository;
    
    // 当前作品
    private final MutableLiveData<Work> currentWork = new MutableLiveData<>();
    
    // 音轨列表
    private final MutableLiveData<List<Track>> tracks = new MutableLiveData<>();
    
    // 状态
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // 事件
    private final SingleLiveEvent<Boolean> markSuccess = new SingleLiveEvent<>();
    private final SingleLiveEvent<Track> playTrack = new SingleLiveEvent<>();
    
    public WorkTreeViewModel() {
        this.workRepository = WorkRepository.getInstance();
    }
    
    // Getters
    public LiveData<Work> getCurrentWork() {
        return currentWork;
    }
    
    public LiveData<List<Track>> getTracks() {
        return tracks;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getMarkSuccess() {
        return markSuccess;
    }
    
    public LiveData<Track> getPlayTrack() {
        return playTrack;
    }
    
    /**
     * 设置当前作品并加载音轨
     */
    public void setWork(Work work) {
        if (work == null) return;
        currentWork.setValue(work);
        loadTracks();
    }
    
    /**
     * 加载音轨列表
     */
    public void loadTracks() {
        Work work = currentWork.getValue();
        if (work == null) return;
        
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        LiveData<Result<List<Track>>> source = workRepository.getWorkTracks(work.getId());
        source.observeForever(new Observer<Result<List<Track>>>() {
            @Override
            public void onChanged(Result<List<Track>> result) {
                source.removeObserver(this);
                isLoading.setValue(false);
                
                if (result.isSuccess()) {
                    tracks.setValue(result.getData());
                } else {
                    errorMessage.setValue(result.getMessage());
                }
            }
        });
    }
    
    /**
     * 标记作品进度
     */
    public void markProgress(@Api.Filter String progress) {
        Work work = currentWork.getValue();
        if (work == null) return;
        
        isLoading.setValue(true);
        
        LiveData<Result<Boolean>> source = workRepository.markWorkProgress(work.getId(), progress);
        source.observeForever(new Observer<Result<Boolean>>() {
            @Override
            public void onChanged(Result<Boolean> result) {
                source.removeObserver(this);
                isLoading.setValue(false);
                
                if (result.isSuccess() && Boolean.TRUE.equals(result.getData())) {
                    markSuccess.setValue(true);
                } else {
                    errorMessage.setValue(result.getMessage());
                }
            }
        });
    }
    
    /**
     * 播放音轨
     */
    public void onTrackClick(Track track) {
        if (track == null) return;
        
        // 如果是目录，不处理（在 UI 层处理展开/折叠）
        if (track.hasChildren()) {
            return;
        }
        
        playTrack.setValue(track);
    }
    
    /**
     * 获取作品 ID
     */
    public int getWorkId() {
        Work work = currentWork.getValue();
        return work != null ? work.getId() : 0;
    }
    
    /**
     * 获取作品标题
     */
    public String getWorkTitle() {
        Work work = currentWork.getValue();
        return work != null ? work.getTitle() : "";
    }
}
