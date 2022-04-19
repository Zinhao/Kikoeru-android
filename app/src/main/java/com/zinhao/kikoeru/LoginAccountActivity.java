package com.zinhao.kikoeru;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
    private String host;

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

        host = App.getInstance().getValue(App.CONFIG_HOST,Api.REMOTE_HOST);

        long lastGetToken = App.getInstance().getValue(App.CONFIG_UPDATE_TIME,0);
        if(System.currentTimeMillis() - lastGetToken < 24*60*60*1000){
            String token = App.getInstance().getValue(App.CONFIG_TOKEN,"");
            if(!token.isEmpty() && !host.isEmpty()){
                Api.init(token,host);
                next();
                return;
            }
        }

        String userName = App.getInstance().getValue(App.CONFIG_USER_ACCOUNT,"guest");
        String password = App.getInstance().getValue(App.CONFIG_USER_PASSWORD,"guest");

        initEdit(userName,password);

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
                        String token = jsonObject.getString("token");
                        Log.d(TAG, "onCompleted: "+token);
                        Api.init(token,host);
                        App.getInstance().setValue(App.CONFIG_HOST,host);
                        App.getInstance().setValue(App.CONFIG_TOKEN,token);
                        App.getInstance().setValue(App.CONFIG_UPDATE_TIME,System.currentTimeMillis());
                        next();
                    } catch (JSONException jsonException) {
                        jsonException.printStackTrace();
                        alertException(jsonException);
                    }
                }
            }else{
                if(jsonObject.has("error")){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Toast.makeText(LoginAccountActivity.this,String.format("%d:%s",asyncHttpResponse.code(),jsonObject.getString("error")),Toast.LENGTH_SHORT).show();
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                                alertException(jsonException);
                            }
                        }
                    });
                }
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
        etServer.setText(host);
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
        host = etServer.getText().toString().trim();
        if(userName.isEmpty() || password.isEmpty() || host.isEmpty()){
            return false;
        }
        if(!host.startsWith("http")){
            return false;
        }
        App.getInstance().setValue(App.CONFIG_USER_ACCOUNT,userName);
        App.getInstance().setValue(App.CONFIG_USER_PASSWORD,password);
        Api.doGetToken(userName,password,host,signInCallback);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem3 =menu.add(0,2,2,"about");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == 2){
            startActivity(new Intent(this,AboutActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void next(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(LoginAccountActivity.this,MainActivity.class));
                finish();
            }
        });
    }
}