package com.zinhao.kikoeru;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;

import com.jil.swipeback.SlideOutActivity;

import java.util.Arrays;
import java.util.function.Consumer;

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
                builder.setTitle("发生错误");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.format("%s: %s",e.getClass().getSimpleName(),e.getMessage())).append('\n');
                Arrays.stream(e.getStackTrace()).forEach(new Consumer<StackTraceElement>() {
                    @Override
                    public void accept(StackTraceElement stackTraceElement) {
                        stringBuilder.append(stackTraceElement.getClassName()).append('.')
                                .append(stackTraceElement.getMethodName()).append(':')
                                .append(stackTraceElement.getLineNumber()).append('\n');
                    }
                });
                builder.setMessage(stringBuilder.toString());
                builder.create().show();
            }
        });
    }
}
