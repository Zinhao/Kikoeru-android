package com.zinhao.kikoeru;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class MoreActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener{
    private CheckBox cbOnlyLrcWork;
    private View vLicense;
    private View vAbout;
    private CheckBox cbDebug;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);

        cbOnlyLrcWork = findViewById(R.id.checkBox);
        long onlyLrcFlag = App.getInstance().getValue(App.CONFIG_ONLY_DISPLAY_LRC,1);
        cbOnlyLrcWork.setChecked(onlyLrcFlag == 1);
        cbOnlyLrcWork.setOnCheckedChangeListener(this);

        vLicense= findViewById(R.id.license);
        vLicense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MoreActivity.this,LicenseActivity.class));
            }
        });

        vAbout = findViewById(R.id.about);
        vAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MoreActivity.this,AboutActivity.class));
            }
        });

        cbDebug = findViewById(R.id.checkBox1);
        cbDebug.setChecked(App.getInstance().isAppDebug());
        cbDebug.setOnCheckedChangeListener(this);

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if(compoundButton == cbOnlyLrcWork){
            long value = b ? 1:0;
            App.getInstance().setValue(App.CONFIG_ONLY_DISPLAY_LRC,value);
            Api.setSubtitle((int) value);
        }

        if(compoundButton == cbDebug){
            App.getInstance().setAppDebug(b);
        }
    }
}