package com.zinhao.kikoeru;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;
import java.util.function.Consumer;

public class BaseActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_READ_CODE = 23;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WRITE_READ_CODE) {
            if (activityResultCallBack != null) {
                activityResultCallBack.run();
                activityResultCallBack = null;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_READ_CODE) {
            if (activityResultCallBack != null) {
                activityResultCallBack.run();
                activityResultCallBack = null;
            }
        }
    }

    private Runnable activityResultCallBack;

    /**
     * 请求读写权限
     *
     * @param callback 对话框被关闭时回调 或者 获取权限成功回调
     * @return
     */
    protected boolean requestReadWriteExternalPermission(Runnable callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.tip);
                builder.setMessage(R.string.external_storage_tip);
                builder.setNegativeButton("去授予", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivityForResult(intent, REQUEST_WRITE_READ_CODE);
                        dialog.dismiss();
                        activityResultCallBack = callback;
                    }
                });
                builder.setPositiveButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (callback != null)
                            callback.run();
                    }
                });
                builder.create().show();
                return false;
            } else {
                return true;
            }
        } else {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_READ_CODE);
                activityResultCallBack = callback;
                return false;
            } else {
                return true;
            }

        }
    }

    protected void alertException(Exception e) {
        if (!App.getInstance().isAppDebug() || isDestroyed()) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
                builder.setTitle(e.getClass().getSimpleName());
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage())).append('\n');
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

    protected void alertMessage(AppMessage e) {
        if (!App.getInstance().isAppDebug() || isDestroyed()) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
                builder.setTitle(e.getTitle());
                builder.setMessage(String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage()) + '\n');
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
    }
}
