package com.zinhao.kikoeru;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

public class AboutActivity extends BaseActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView githubLink = findViewById(R.id.textView4);
        githubLink.setText(Html.fromHtml("<a href ='"+getString(R.string.project_kikoeru_android_url)+"'>Kikoeru-android</a>",
                Html.FROM_HTML_MODE_LEGACY));
    }
}
