package com.zinhao.kikoeru;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends BaseActivity implements TagsView.TagClickListener<JSONObject> {
    private static final String TAG = "SearchActivity";
    private EditText etInput;
    private RecyclerView recyclerView;
    private List<JSONObject> works = new ArrayList<>();
    private WorkAdapter workAdapter;
    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        recyclerView = findViewById(R.id.recyclerView);
        etInput = findViewById(R.id.editTextNumber);
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d(TAG, "beforeTextChanged: "+s);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged: "+s);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(workAdapter!= null){
                    workAdapter.notifyItemRangeRemoved(0,works.size());
                    workAdapter.notifyItemRangeChanged(0,works.size());
                }
                works.clear();
                if(s.toString().length()==6){
                    Api.doGetWork(s.toString(),1,searchWorkCallback);
                }
            }
        });
    }

    private void initLayout(int layoutType) {
        RecyclerView.LayoutManager layoutManager = null;
        if (layoutType == WorkAdapter.LAYOUT_LIST) {
            layoutManager = new LinearLayoutManager(SearchActivity.this);
        } else if (layoutType == WorkAdapter.LAYOUT_SMALL_GRID) {
            layoutManager = new GridLayoutManager(SearchActivity.this, 3);
        } else if (layoutType == WorkAdapter.LAYOUT_BIG_GRID) {
            layoutManager = new GridLayoutManager(SearchActivity.this, 2);
        }
        workAdapter = new WorkAdapter(works, layoutType);
        workAdapter.setTagClickListener(this);
        workAdapter.setVaClickListener(vaClickListener);
        workAdapter.setItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject item = (JSONObject) v.getTag();
                Intent intent = new Intent(v.getContext(), WorkTreeActivity.class);
                intent.putExtra("work_json_str", item.toString());
                ActivityCompat.startActivity(SearchActivity.this, intent,null);
            }
        });
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(workAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        etInput.setFocusable(true);
        etInput.setFocusableInTouchMode(true);
        etInput.requestFocus();
        imm.showSoftInput(etInput,0);
    }

    private final TagsView.TagClickListener<JSONObject> vaClickListener = new TagsView.TagClickListener<JSONObject>() {
        @Override
        public void onTagClick(JSONObject jsonObject) {
            try {
                String vaId = jsonObject.getString("id");
                Log.d(TAG, "onTagClick: "+ vaId);
                String vaName = jsonObject.getString("name");
                setTitle(vaName);
                Intent intent = new Intent(SearchActivity.this,WorksActivity.class);
                intent.putExtra("resultType","va");
                intent.putExtra("id",vaId);
                intent.putExtra("name",vaName);
                startActivity(intent);
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
            }
        }
    };

    private AsyncHttpClient.JSONObjectCallback searchWorkCallback = new AsyncHttpClient.JSONObjectCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
            if(e!=null){
                e.printStackTrace();
                alertException(e);
                return;
            }
            if(asyncHttpResponse == null || asyncHttpResponse.code() !=200){
                return;
            }
            try {
                JSONArray jsonArray = jsonObject.getJSONArray("works");
                int totalCount = jsonObject.getJSONObject("pagination").getInt("totalCount");
                runOnUiThread(new Runnable() {
                    @SuppressLint("DefaultLocale")
                    @Override
                    public void run() {
                        setTitle(String.format("%s(%d)",getString(R.string.app_name),totalCount));
                        for (int i = 0; i < jsonArray.length(); i++) {
                            try {
                                works.add(jsonArray.getJSONObject(i));
                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                                alertException(jsonException);
                            }
                        }
                        initLayout((int) App.getInstance().getValue(App.CONFIG_LAYOUT_TYPE,WorkAdapter.LAYOUT_SMALL_GRID));
                    }
                });
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
                alertException(jsonException);
            }
        }
    };

    @Override
    public void onTagClick(JSONObject jsonObject) {
        try {
            int tagId = jsonObject.getInt("id");
            String tagName = jsonObject.getString("name");
            setTitle(tagName);
            Intent intent = new Intent(SearchActivity.this,WorksActivity.class);
            intent.putExtra("resultType","tag");
            intent.putExtra("id",tagId);
            intent.putExtra("name",tagName);
            startActivity(intent);
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    }
}