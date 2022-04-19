package com.zinhao.kikoeru;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

public class SettingActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener{
    CheckBox cbOnlyLrcWork;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        cbOnlyLrcWork = findViewById(R.id.checkBox);
        long onlyLrcFlag = App.getInstance().getValue(App.CONFIG_ONLY_DISPLAY_LRC,1);
        cbOnlyLrcWork.setChecked(onlyLrcFlag == 1);

        cbOnlyLrcWork.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if(compoundButton == cbOnlyLrcWork){
            long value = b ? 1:0;
            App.getInstance().setValue(App.CONFIG_ONLY_DISPLAY_LRC,value);
            Api.setSubtitle((int) value);
        }
    }
}