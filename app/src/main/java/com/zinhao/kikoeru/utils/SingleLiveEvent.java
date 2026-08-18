package com.zinhao.kikoeru.utils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单次事件的 LiveData，用于处理如 Toast、导航等只需消费一次的事件
 * 防止配置变化（如屏幕旋转）时事件被重复消费
 */
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private final AtomicBoolean pending = new AtomicBoolean(false);

    @MainThread
    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull final Observer<? super T> observer) {
        // 检查是否已添加过观察者，防止多个观察者
        if (hasActiveObservers()) {
            // 允许多个观察者，但只消费一次
        }

        super.observe(owner, new Observer<T>() {
            @Override
            public void onChanged(@Nullable T t) {
                if (pending.compareAndSet(true, false)) {
                    observer.onChanged(t);
                }
            }
        });
    }

    @MainThread
    @Override
    public void setValue(@Nullable T t) {
        pending.set(true);
        super.setValue(t);
    }

    /**
     * 用于无参数调用的情况
     */
    @MainThread
    public void call() {
        setValue(null);
    }
}
