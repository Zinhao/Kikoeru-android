package com.zinhao.kikoeru;

import android.content.Intent;
import android.os.Bundle;

public class LauncherActivity extends BaseActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String token = App.getInstance().getValue(App.CONFIG_TOKEN,"");
        String host = App.getInstance().getValue(App.CONFIG_HOST,Api.REMOTE_HOST);
        if(!token.isEmpty() && !host.isEmpty()){
            Api.init(token,host);
            startActivity(new Intent(LauncherActivity.this, WorksActivity.class));
        }else {
            startActivity(new Intent(LauncherActivity.this, LoginAccountActivity.class));
        }
        finish();
    }
}
