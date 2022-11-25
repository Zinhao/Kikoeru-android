package com.zinhao.kikoeru;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import android.widget.EditText;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VasActivity extends BaseActivity implements TagsView.TagClickListener<JSONObject>{
    private static final String TAG = "VasActivity";
    private TagsView<JSONArray> VasView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_tags);
        VasView = findViewById(R.id.tagsView);
        VasView.setTagClickListener(this);
        VasView.setTagBackgroundResource(R.drawable.card_bg_va);
        EditText input = findViewById(R.id.editText);
        input.setHint(R.string.va_voicer);
        Api.doGetAllVas(callback);
    }

    private final TagsView.TextGet<JSONObject> textGet = new TagsView.TextGet<JSONObject>() {
        @Override
        public String onGetText(JSONObject jsonObject) {
            try {
                return jsonObject.getString("name")+ "("+jsonObject.getInt("count")+")";
            } catch (JSONException e) {
                e.printStackTrace();
                alertException(e);
            }
            return "";
        }
    };

    private AsyncHttpClient.JSONArrayCallback callback = new AsyncHttpClient.JSONArrayCallback() {
        @Override
        public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONArray jsonArray) {
            if(e!=null){
                alertException(e);
                return;
            }
            if(asyncHttpResponse == null || asyncHttpResponse.code() !=200){
                Log.d(TAG, "onCompleted: err");
                return;
            }
            Log.d(TAG, "onCompleted: "+jsonArray.length());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    VasView.setTags(jsonArray,textGet);
                    if(!VasView.isInLayout()){
                        VasView.requestLayout();
                    }else {
                        VasView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(!VasView.isInLayout()){
                                    VasView.requestLayout();
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
            String vaId = jsonObject.getString("id");
            Log.d(TAG, "onTagClick: "+ vaId);
            String vaName = jsonObject.getString("name");
            setTitle(vaName);
            Intent intent = new Intent();
            intent.putExtra("resultType","va");
            intent.putExtra("id",vaId);
            intent.putExtra("name",vaName);
            setResult(RESULT_OK,intent);
            finish();
        } catch (JSONException e) {
            e.printStackTrace();
            alertException(e);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
    }
}
