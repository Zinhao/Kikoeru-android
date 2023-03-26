package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;

import java.util.Locale;

public class LrcFloatWindow extends BaseActivity implements ServiceConnection{
    private AudioService.CtrlBinder ctrlBinder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("还没有显示悬浮窗口的权限！")
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    }).setPositiveButton("去开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse(String.format(Locale.US, "package:%s", getPackageName()))), 1);
                        }
                    }).setCancelable(false);
            AlertDialog askDrawOverlaysDialog = builder.create();
            askDrawOverlaysDialog.show();
        }
        bindService(new Intent(this, AudioService.class), this, BIND_AUTO_CREATE);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ctrlBinder = (AudioService.CtrlBinder) service;
        if (ctrlBinder.getLrcFloatView() == null) {
            TextView view = (TextView) LayoutInflater.from(this).inflate(R.layout.lrc_layout, null, false);
            view.setOnTouchListener(new View.OnTouchListener() {
                private float downX, downY;
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // 歌词手势
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        downX = event.getRawX();
                        downY = event.getRawY();
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (ctrlBinder != null)
                            App.getInstance().savePosition(ctrlBinder.getLrcWindowParams().x, ctrlBinder.getLrcWindowParams().y);
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {

                        float nowX = event.getRawX();
                        float nowY = event.getRawY();
                        float moveX = nowX - downX;
                        float moveY = nowY - downY;
                        if (ctrlBinder != null) {
                            ctrlBinder.getLrcWindowParams().x += moveX;
                            ctrlBinder.getLrcWindowParams().y += moveY;
                            getWindowManager().updateViewLayout(ctrlBinder.getLrcFloatView(), ctrlBinder.getLrcWindowParams());
                        }
                        downX = nowX;
                        downY = nowY;
                    }
                    return true;
                }
            });
            ctrlBinder.setLrcView(view);
        }
        if (Settings.canDrawOverlays(this)) {
            // 有权限
            ctrlBinder.showLrcFloatWindow();
            finishAndRemoveTask();
        }

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Settings.canDrawOverlays(this)) {
            ctrlBinder.showLrcFloatWindow();
        }
        finishAndRemoveTask();
    }
}
