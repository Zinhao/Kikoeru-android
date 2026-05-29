package com.zinhao.kikoeru.ui.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.zinhao.kikoeru.App;
import com.zinhao.kikoeru.data.model.LoginResponse;
import com.zinhao.kikoeru.data.model.Result;
import com.zinhao.kikoeru.data.repository.UserRepository;
import com.zinhao.kikoeru.utils.SingleLiveEvent;

/**
 * 登录页面 ViewModel
 */
public class LoginViewModel extends ViewModel {

    private final UserRepository userRepository;

    // 输入字段
    private final MutableLiveData<String> username = new MutableLiveData<>("");
    private final MutableLiveData<String> password = new MutableLiveData<>("");
    private final MutableLiveData<String> host = new MutableLiveData<>("https://api.asmr.one");

    // 登录状态
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final SingleLiveEvent<Boolean> loginSuccess = new SingleLiveEvent<>();

    public LoginViewModel() {
        this.userRepository = UserRepository.getInstance();
    }

    // Getters for input fields
    public LiveData<String> getUsername() {
        return username;
    }

    public LiveData<String> getPassword() {
        return password;
    }

    public LiveData<String> getHost() {
        return host;
    }

    // Getters for status
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getLoginSuccess() {
        return loginSuccess;
    }

    // Setters for input fields
    public void setUsername(String value) {
        username.setValue(value);
    }

    public void setPassword(String value) {
        password.setValue(value);
    }

    public void setHost(String value) {
        host.setValue(value);
    }

    /**
     * 执行登录
     */
    public void login() {
        String user = username.getValue();
        String pwd = password.getValue();
        String server = host.getValue();

        if (user == null || user.trim().isEmpty()) {
            errorMessage.setValue("请输入用户名");
            return;
        }
        if (pwd == null || pwd.trim().isEmpty()) {
            errorMessage.setValue("请输入密码");
            return;
        }
        if (server == null || server.trim().isEmpty()) {
            errorMessage.setValue("请输入服务器地址");
            return;
        }

        // 确保 host 有协议前缀
        String hostUrl = server.trim();
        if (!hostUrl.startsWith("http://") && !hostUrl.startsWith("https://")) {
            hostUrl = "http://" + hostUrl;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        LiveData<Result<LoginResponse>> result = userRepository.login(user.trim(), pwd, hostUrl);
        result.observeForever(new Observer<Result<LoginResponse>>() {
            @Override
            public void onChanged(Result<LoginResponse> loginResult) {
                Log.d("LoginViewModel", "Login result status: " + loginResult.getStatus());

                // 处理 Loading 状态
                if (loginResult.isLoading()) {
                    isLoading.setValue(true);
                    return;
                }

                // 处理完成，移除观察者
                result.removeObserver(this);
                isLoading.setValue(false);

                // 打印详细日志
                if (loginResult.getData() != null) {
                    LoginResponse data = loginResult.getData();
                    Log.d("LoginViewModel", "Token: " + (data.getToken() != null ? "***" : "null"));
                    Log.d("LoginViewModel", "Error: " + data.getError());
                    Log.d("LoginViewModel", "ErrorMessage: " + data.getErrorMessage());
                }

                if (loginResult.isSuccess()) {
                    Log.i("LoginViewModel", "Login success!");
                    loginSuccess.setValue(true);
                } else {
                    String msg = loginResult.getMessage();
                    Log.w("LoginViewModel", "Login failed: " + msg);
                    if (msg == null || msg.isEmpty() || "null".equals(msg)) {
                        msg = "登录失败，请检查网络或服务器地址";
                    }
                    errorMessage.setValue(msg);
                }
            }
        });
    }

    /**
     * 使用访客登录
     */
    public void loginAsGuest() {
        setUsername("guest");
        setPassword("guest");
        login();
    }

    /**
     * 检查是否已有登录用户
     */
    public boolean hasExistingUser() {
        return userRepository.hasUsers();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
