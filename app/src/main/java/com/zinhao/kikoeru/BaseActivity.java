package com.zinhao.kikoeru;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.jil.swipeback.SlideOutActivity;

public class BaseActivity extends SlideOutActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean enableSlide() {
        return true;
    }

    protected void alertException(Exception e){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
                builder.setTitle("发生错误").setMessage(String.format("%s: %s",e.getClass().getSimpleName(),e.getMessage()));
                builder.create().show();
            }
        });
    }
}
