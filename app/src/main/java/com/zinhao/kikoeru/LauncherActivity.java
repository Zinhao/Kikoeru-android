package com.zinhao.kikoeru;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.core.splashscreen.SplashScreen;
import com.zinhao.kikoeru.db.User;
import com.zinhao.kikoeru.ui.WorkPageActivity;

import java.util.List;

public class LauncherActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.S){
            splashScreen.setKeepOnScreenCondition(() -> true );
        }
        App.getInstance().getAllUsersAsync(new App.DatabaseResultCallback() {
            @Override
            public void onResult(Object result) {
                if(result instanceof List){
                    next();
                }
            }
        });
    }

    private void next(){
        runOnUiThread(()->{
            User user = App.getInstance().currentUser();
            if (user == null) {
                startActivity(new Intent(LauncherActivity.this, UserSwitchActivity.class));
            } else {
                Api.init(user.getToken(), user.getHost());
                if(App.getInstance().isUseNewLayout()){
                    startActivity(new Intent(LauncherActivity.this, WorkPageActivity.class));
                }else{
                    startActivity(new Intent(LauncherActivity.this, WorksActivity.class));
                }

            }
            finish();
        });
    }
}
