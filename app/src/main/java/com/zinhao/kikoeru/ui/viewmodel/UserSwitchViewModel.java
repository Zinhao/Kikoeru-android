package com.zinhao.kikoeru.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.zinhao.kikoeru.User;
import com.zinhao.kikoeru.data.repository.UserRepository;
import com.zinhao.kikoeru.utils.SingleLiveEvent;

import java.util.List;

/**
 * 用户切换 ViewModel
 */
public class UserSwitchViewModel extends ViewModel {
    
    private final UserRepository userRepository;
    
    private final MutableLiveData<List<User>> users = new MutableLiveData<>();
    private final SingleLiveEvent<Boolean> switchSuccess = new SingleLiveEvent<>();
    
    public UserSwitchViewModel() {
        this.userRepository = UserRepository.getInstance();
        loadUsers();
    }
    
    public LiveData<List<User>> getUsers() {
        return users;
    }
    
    public LiveData<Boolean> getSwitchSuccess() {
        return switchSuccess;
    }
    
    /**
     * 加载用户列表
     */
    public void loadUsers() {
        users.setValue(userRepository.getAllUsers());
    }
    
    /**
     * 切换到指定用户
     */
    public void switchToUser(User user) {
        if (user == null) return;
        userRepository.switchUser(user);
        switchSuccess.setValue(true);
    }
    
    /**
     * 删除用户
     */
    public void deleteUser(User user) {
        if (user == null) return;
        userRepository.deleteUser(user);
        loadUsers(); // 刷新列表
    }
}
