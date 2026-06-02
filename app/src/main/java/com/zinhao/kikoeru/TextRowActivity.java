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
import android.view.SubMenu;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
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
                runOnUiThread(() -> init(s));
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

    private JSONObject textItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lrc_show);
        View root = findViewById(R.id.root);
        setSafeArea(root,null);
        mRecyclerView = findViewById(R.id.recyclerView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        String text = getIntent().getStringExtra("jsonText");
        bindService(new Intent(this, AudioService.class), this, BIND_AUTO_CREATE);
        if (text == null) {
            finish();
            return;
        }
        try {
            textItem = new JSONObject(text);
            setTitle(textItem.getString(JSONConst.WorkTree.TITLE));
            if (textItem.has(JSONConst.WorkTree.EXISTS)) {
                boolean exists = textItem.getBoolean(JSONConst.WorkTree.EXISTS);
                if (exists) {
                    File mapFile = new File(textItem.getString(JSONConst.WorkTree.MAP_FILE_PATH));
                    LocalFileCache.getInstance().readText(mapFile, textCallback);
                } else {
                    String hash = textItem.getString(JSONConst.WorkTree.HASH);
                    Api.doGetMediaString(hash, textCallback);
                }
            }
        } catch (JSONException e) {
            init(text);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu subMenu = menu.addSubMenu(0, 0, 0, "load as lrc");
        subMenu.setIcon(R.drawable.ic_baseline_text_fields_24);
        subMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == 0){
            if(ctrlBinder!=null && mText != null){
                ctrlBinder.setLrc(mText.getText());
                Toast.makeText(this,"load as lrc success",Toast.LENGTH_SHORT).show();
                ctrlBinder.insertLrcBind(textItem.optString(JSONConst.WorkTree.HASH));
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
