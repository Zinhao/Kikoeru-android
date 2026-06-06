package com.zinhao.kikoeru;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        //tvVer 设置版本号 版本code 格式 "ver +version字符串 + (CODE)"
        TextView tvVer = findViewById(R.id.tvVer); // 确保布局中有 id 为 tvVer 的 TextView

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;

            String versionText = "ver " + versionName + " (" + versionCode + ")";
            tvVer.setText(versionText);
        } catch (PackageManager.NameNotFoundException e) {
            alertException(new Exception("get package info failed!"+e.getMessage()));
            tvVer.setText("ver unknown");
        }
    }
}
