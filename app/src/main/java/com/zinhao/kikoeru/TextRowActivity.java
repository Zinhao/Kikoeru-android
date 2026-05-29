package com.zinhao.kikoeru;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class TextRowActivity extends BaseActivity implements ServiceConnection {
    private static final String TAG = "TextRowActivity";
    private AudioService.CtrlBinder ctrlBinder;
    public static void start(Context context, String jsonStr) {
        Intent starter = new Intent(context, TextRowActivity.class);
        starter.putExtra("jsonText", jsonStr);
        context.startActivity(starter);
    }

    private final AsyncHttpClient.StringCallback textCallback = new AsyncHttpClient.StringCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, String s) {
            if (e != null) {
                alertException(e);
                return;
            }
            if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                Log.d(TAG, "onCompleted: lrcTextCallback err!");
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    init(s);
                }
            });
        }
    };

    private RecyclerView mRecyclerView;
    private Text mText;
    private TextAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lrc_show);
        mRecyclerView = findViewById(R.id.recyclerView);
        String text = getIntent().getStringExtra("jsonText");
        bindService(new Intent(this, AudioService.class), this, BIND_AUTO_CREATE);
        if (text == null) {
            finish();
            return;
        }
        try {
            JSONObject item = new JSONObject(text);
            setTitle(item.getString(JSONConst.WorkTree.TITLE));
            if (item.has(JSONConst.WorkTree.EXISTS)) {
                boolean exists = item.getBoolean(JSONConst.WorkTree.EXISTS);
                if (exists) {
                    File mapFile = new File(item.getString(JSONConst.WorkTree.MAP_FILE_PATH));
                    LocalFileCache.getInstance().readText(mapFile, textCallback);
                } else {
                    String hash = item.getString(JSONConst.WorkTree.HASH);
                    Api.doGetMediaString(hash, textCallback);
                }
            }
        } catch (JSONException e) {
            init(text);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "load as lrc");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == 0){
            if(ctrlBinder!=null){
                ctrlBinder.setmLrc(mText.getText());
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void init(String s) {
        mText = new Text(s);
        adapter = new TextAdapter(mText);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        ctrlBinder = (AudioService.CtrlBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
