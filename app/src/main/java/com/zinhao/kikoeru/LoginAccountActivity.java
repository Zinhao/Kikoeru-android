package com.zinhao.kikoeru;

import androidx.annotation.NonNull;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LoginAccountActivity extends BaseActivity {
    private static final String TAG = "LoginAccountActivity";
    private TextInputLayout tilUser;
    private TextInputLayout tilPassword;
    private TextInputLayout tilServer;
    private Button btSignIn;
    private Button btGuest;
    private Button btSignUp;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_account);
        tilUser = findViewById(R.id.textInputLayout);
        tilPassword = findViewById(R.id.textInputLayout2);
        tilServer = findViewById(R.id.textInputLayout3);
        btSignIn = findViewById(R.id.button2);
        btGuest = findViewById(R.id.button4);
        btSignUp = findViewById(R.id.button3);

        user = new User();
        user.setHost(Api.LOCAL_HOST);
        user.setName("guest");
        user.setPassword("guest");
        User currentUser = App.getInstance().currentUser();
        if(currentUser!=null){
            user.setHost(currentUser.getHost());
            user.setName(currentUser.getName());
            user.setPassword(currentUser.getPassword());
        }

        initEdit(user.getName(), user.getPassword());

        btSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btSignIn.setEnabled(false);
                btGuest.setEnabled(false);
                if(signIn()){
                    Toast.makeText(view.getContext(),"sign in with input!",Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(view.getContext(),"please check input!",Toast.LENGTH_SHORT).show();
                    btSignIn.setEnabled(true);
                    btGuest.setEnabled(true);
                }

            }
        });
        btGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btGuest.setEnabled(false);
                btSignIn.setEnabled(false);
                initEdit("guest","guest");
                if(signIn()){
                    Toast.makeText(view.getContext(),"sign in with input!",Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(view.getContext(),"please check input!",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private AsyncHttpClient.JSONObjectCallback signInCallback = new AsyncHttpClient.JSONObjectCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btSignIn.setEnabled(true);
                    btGuest.setEnabled(true);
                }
            });
            if(asyncHttpResponse==null || jsonObject == null){
                Log.d(TAG, "onCompleted: get token err ");
                return;
            }
            if(asyncHttpResponse.code() == 200){
                if(jsonObject.has("token")){
                    try {
                        App app = (App) getApplication();
                        String token = jsonObject.getString("token");
                        user.setToken(token);
                        Api.init(token,user.getHost());
                        App.getInstance().setValue(App.CONFIG_USER_DATABASE_ID,app.insertUser(user));
                        saveAndNext();
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                        alertException(jsonException);
                    }
                }
            }else{
                StringBuilder stringBuilder = new StringBuilder();
                try {
                    if(jsonObject.has("error")){
                        stringBuilder.append(jsonObject.getString("error"));
                    }else if(jsonObject.has("errors")){
                        JSONArray errors = jsonObject.getJSONArray("errors");
                        for (int i = 0; i < errors.length(); i++) {
                            JSONObject error = errors.getJSONObject(i);
                            String errorValue = error.getString("msg");
                            stringBuilder.append(errorValue);
                        }
                    }
                }catch (JSONException e1){
                    alertException(e);
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginAccountActivity.this,String.format("%d:%s",asyncHttpResponse.code(),stringBuilder.toString()),Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };

    private void initEdit(String userName,String password){
        EditText etUser = tilUser.getEditText();
        if(etUser == null)
            return;
        EditText etPassword = tilPassword.getEditText();
        if(etPassword == null)
            return;
        EditText etServer = tilServer.getEditText();
        if(etServer == null)
            return;
        etUser.setText(userName);
        etPassword.setText(password);
        etServer.setText(user.getHost());
    }

    private boolean signIn(){
        EditText etUser = tilUser.getEditText();
        if(etUser == null)
            return false;
        EditText etPassword = tilPassword.getEditText();
        if(etPassword == null)
            return false;
        EditText etServer = tilServer.getEditText();
        if(etServer == null)
            return false;
        String userName = etUser.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String host = etServer.getText().toString().trim();
        if(userName.isEmpty() || password.isEmpty() || host.isEmpty()){
            return false;
        }
        if(!host.startsWith("http")){
            return false;
        }
        user.setName(userName);
        user.setPassword(password);
        user.setHost(host);
        Api.doGetToken(userName,password,host,signInCallback);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem2 =menu.add(0,2,2,"about");
        MenuItem menuItem3 =menu.add(0,3,3,"choose user");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == 2){
            startActivity(new Intent(this,AboutActivity.class));
        }else if(item.getItemId() == 3){
            startActivity(new Intent(this,UserSwitchActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }


    private void saveAndNext(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(LoginAccountActivity.this, WorksActivity.class));
                finish();
            }
        });
    }
}