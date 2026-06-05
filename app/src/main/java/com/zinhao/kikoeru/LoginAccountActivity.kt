package com.zinhao.kikoeru

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout
import com.zinhao.kikoeru.databinding.ActivityLoginAccountBinding
import com.zinhao.kikoeru.viewmodel.LoginViewModel

class LoginAccountActivity : BaseActivity() {
    private var tilUser: TextInputLayout? = null
    private var tilPassword: TextInputLayout? = null
    private var tilServer: TextInputLayout? = null
    private var btSignIn: Button? = null
    private var btGuest: Button? = null
    private var btSignUp: Button? = null

    private var viewModel: LoginViewModel? = null
    private lateinit var viewBinding: ActivityLoginAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityLoginAccountBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel = ViewModelProvider(this).get<LoginViewModel>(LoginViewModel::class.java)
        initViews()
        observeViewModel()
        setupListeners()
        initEdit()
    }

    private fun setupListeners() {
        btSignIn!!.setOnClickListener(View.OnClickListener { v: View? ->
            // 更新 ViewModel 中的值
            updateViewModelInputs()
            viewModel!!.login()
        })

        btGuest!!.setOnClickListener(View.OnClickListener { v: View? ->
            viewModel!!.loginAsGuest()
        })
    }

    private fun observeViewModel() {
        viewModel!!.getIsLoading().observe(this, object : Observer<Boolean?> {
            override fun onChanged(isLoading: Boolean?) {
                isLoading?.let {
                    btSignIn!!.setEnabled(!it)
                    btGuest!!.setEnabled(!it)
                }

            }
        })
        // 观察错误消息
        viewModel!!.getErrorMessage().observe(this, Observer { error: String? ->
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        })

        // 观察登录成功
        viewModel!!.getLoginSuccess().observe(this, Observer { success: Boolean? ->
            success?.let {
                if (it) {
                    navigateToMain()
                }
            }

        })
    }

    private fun initEdit() {
        val etUser = tilUser!!.getEditText()
        val etPassword = tilPassword!!.getEditText()
        val etServer = tilServer!!.getEditText()

        if (etUser == null || etPassword == null || etServer == null) return

        // 设置默认值
        val currentUser = App.getInstance().currentUser()
        if (currentUser != null) {
            etUser.setText(currentUser.getName())
            etPassword.setText(currentUser.getPassword())
            etServer.setText(currentUser.getHost())
        } else {
            etUser.setText("guest")
            etPassword.setText("guest")
            etServer.setText(Api.REMOTE_HOST)
        }
    }

    private fun updateViewModelInputs() {
        val etUser = tilUser!!.getEditText()
        val etPassword = tilPassword!!.getEditText()
        val etServer = tilServer!!.getEditText()

        if (etUser != null) {
            viewModel!!.setUsername(etUser.getText().toString().trim { it <= ' ' })
        }
        if (etPassword != null) {
            viewModel!!.setPassword(etPassword.getText().toString().trim { it <= ' ' })
        }
        if (etServer != null) {
            viewModel!!.setHost(etServer.getText().toString().trim { it <= ' ' })
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this@LoginAccountActivity, WorksActivity::class.java))
        finish()
    }

    private fun initViews() {
        tilUser = viewBinding.textInputLayout
        tilPassword = viewBinding.textInputLayout2
        tilServer = viewBinding.textInputLayout3
        btSignIn = viewBinding.button2
        btGuest = viewBinding.button4
        btSignUp = viewBinding.button3
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val menuItem2 = menu.add(0, 2, 2, "about")
        val menuItem3 = menu.add(0, 3, 3, "choose user")
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == 2) {
            startActivity(Intent(this, AboutActivity::class.java))
        } else if (item.getItemId() == 3) {
            startActivity(Intent(this, UserSwitchActivity::class.java))
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "LoginAccountActivity"
    }
}