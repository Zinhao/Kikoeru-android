package com.zinhao.kikoeru;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.jil.swipeback.SlideOutActivity;

import java.util.Arrays;
import java.util.function.Consumer;

public class BaseActivity extends SlideOutActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestPermissions(new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE},23);
            }else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},23);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 23){
            for (int i = 0; i < grantResults.length; i++) {
                if(grantResults[i] == PackageManager.PERMISSION_DENIED){
                    return;
                }
            }
            Toast.makeText(this,"获取读写权限成功",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected boolean enableSlide() {
        return true;
    }

    protected void alertException(Exception e){
        if(!App.getInstance().isAppDebug()){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
                builder.setTitle(e.getClass().getSimpleName());
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

    protected void alertMessage(AppMessage e){
        if(!App.getInstance().isAppDebug()){
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
                builder.setTitle(e.getTitle());
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
                builder.setPositiveButton(e.getActionName(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        e.getAction().run();
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
    }
}
