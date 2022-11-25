package com.zinhao.kikoeru;

import android.content.Intent;
import android.os.Bundle;

public class LauncherActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user = App.getInstance().currentUser();
        if (user == null) {
            startActivity(new Intent(LauncherActivity.this, UserSwitchActivity.class));
        } else {
            Api.init(user.getToken(), user.getHost());
            startActivity(new Intent(LauncherActivity.this, WorksActivity.class));
        }
        finish();
    }
}
