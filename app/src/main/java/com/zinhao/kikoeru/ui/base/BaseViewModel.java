package com.zinhao.kikoeru.ui.base;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zinhao.kikoeru.utils.SingleLiveEvent;

/**
 * ViewModel 基类
 */
public abstract class BaseViewModel extends ViewModel {
    
    // 加载状态
    protected final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    
    // 错误消息（单次事件）
    protected final SingleLiveEvent<String> errorMessage = new SingleLiveEvent<>();
    
    // 通用消息（单次事件）
    protected final SingleLiveEvent<String> message = new SingleLiveEvent<>();
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<String> getMessage() {
        return message;
    }
    
    protected void setLoading(boolean loading) {
        isLoading.postValue(loading);
    }
    
    protected void setError(String msg) {
        errorMessage.postValue(msg);
    }
    
    protected void setMessage(String msg) {
        message.postValue(msg);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // 清理资源
    }
}
