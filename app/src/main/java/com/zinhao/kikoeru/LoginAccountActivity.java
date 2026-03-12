package com.zinhao.kikoeru;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputLayout;
import com.zinhao.kikoeru.ui.viewmodel.LoginViewModel;

/**
 * 登录页面 - MVVM 重构版
 */
public class LoginAccountActivity extends BaseActivity {
    private static final String TAG = "LoginAccountActivity";
    private TextInputLayout tilUser;
    private TextInputLayout tilPassword;
    private TextInputLayout tilServer;
    private Button btSignIn;
    private Button btGuest;
    private Button btSignUp;

    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_account);

        // 初始化 ViewModel
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // 初始化视图
        initViews();

        // 绑定 ViewModel 观察
        observeViewModel();

        // 设置事件监听
        setupListeners();

        // 初始化输入框
        initEdit();
    }

    private void initViews() {
        tilUser = findViewById(R.id.textInputLayout);
        tilPassword = findViewById(R.id.textInputLayout2);
        tilServer = findViewById(R.id.textInputLayout3);
        btSignIn = findViewById(R.id.button2);
        btGuest = findViewById(R.id.button4);
        btSignUp = findViewById(R.id.button3);
    }

    private void observeViewModel() {
        // 观察加载状态
        viewModel.getIsLoading().observe(this, isLoading -> {
            btSignIn.setEnabled(!isLoading);
            btGuest.setEnabled(!isLoading);
        });

        // 观察错误消息
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // 观察登录成功
        viewModel.getLoginSuccess().observe(this, success -> {
            if (success) {
                navigateToMain();
            }
        });
    }

    private void setupListeners() {
        btSignIn.setOnClickListener(v -> {
            // 更新 ViewModel 中的值
            updateViewModelInputs();
            viewModel.login();
        });

        btGuest.setOnClickListener(v -> {
            viewModel.loginAsGuest();
        });
    }

    private void initEdit() {
        EditText etUser = tilUser.getEditText();
        EditText etPassword = tilPassword.getEditText();
        EditText etServer = tilServer.getEditText();

        if (etUser == null || etPassword == null || etServer == null) return;

        // 设置默认值
        User currentUser = App.getInstance().currentUser();
        if (currentUser != null) {
            etUser.setText(currentUser.getName());
            etPassword.setText(currentUser.getPassword());
            etServer.setText(currentUser.getHost());
        } else {
            etUser.setText("guest");
            etPassword.setText("guest");
            etServer.setText(Api.REMOTE_HOST);
        }
    }

    private void updateViewModelInputs() {
        EditText etUser = tilUser.getEditText();
        EditText etPassword = tilPassword.getEditText();
        EditText etServer = tilServer.getEditText();

        if (etUser != null) {
            viewModel.setUsername(etUser.getText().toString().trim());
        }
        if (etPassword != null) {
            viewModel.setPassword(etPassword.getText().toString().trim());
        }
        if (etServer != null) {
            viewModel.setHost(etServer.getText().toString().trim());
        }
    }

    private void navigateToMain() {
        startActivity(new Intent(this, WorksActivity.class));
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem2 = menu.add(0, 2, 2, "about");
        MenuItem menuItem3 = menu.add(0, 3, 3, "choose user");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 2) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (item.getItemId() == 3) {
            startActivity(new Intent(this, UserSwitchActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
