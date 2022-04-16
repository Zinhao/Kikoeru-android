package com.zinhao.kikoeru;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TagsActivity extends AppCompatActivity implements TagsView.TagClickListener<JSONObject>{
    private static final String TAG = "TagsActivity";
    private TagsView<JSONArray> tagsView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        tagsView = new TagsView<JSONArray>(this);
        setContentView(R.layout.layout_tags);
        tagsView = findViewById(R.id.tagsView);
        tagsView.setTagClickListener(this);
        Api.doGetAllTags(callback);

    }

    private final TagsView.TextGet<JSONObject> textGet = new TagsView.TextGet<JSONObject>() {
        @Override
        public String onGetText(JSONObject jsonObject) {
            try {
                return jsonObject.getString("name")+ "("+jsonObject.getInt("count")+")";
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return "";
        }
    };

    private AsyncHttpClient.JSONArrayCallback callback = new AsyncHttpClient.JSONArrayCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONArray jsonArray) {
            if(asyncHttpResponse == null || asyncHttpResponse.code() !=200){
                Log.d(TAG, "onCompleted: err");
                return;
            }
            Log.d(TAG, "onCompleted: "+jsonArray.length());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tagsView.setTags(jsonArray,textGet);
                    if(!tagsView.isInLayout()){
                        tagsView.requestLayout();
                    }else {
                        tagsView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(!tagsView.isInLayout()){
                                    tagsView.requestLayout();
                                }
                            }
                        },500);
                    }

                }
            });

        }
    };

    @Override
    public void onTagClick(JSONObject jsonObject) {
        try {
            int tagId = jsonObject.getInt("id");
            Log.d(TAG, "onTagClick: "+ tagId);
            String tagName = jsonObject.getString("name");
            setTitle(tagName);
            Intent intent = new Intent();
            intent.putExtra("id",tagId);
            intent.putExtra("name",tagName);
            setResult(RESULT_OK,intent);
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
    }
}
