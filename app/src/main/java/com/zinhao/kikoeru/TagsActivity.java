package com.zinhao.kikoeru;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TagsActivity extends BaseActivity implements TagsView.TagClickListener<JSONObject> {
    private static final String TAG = "TagsActivity";
    private TagsView<JSONArray> tagsView;
    private EditText etInput;
    private JSONArray allTags;
    private InputMethodManager imm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_tags);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        tagsView = findViewById(R.id.tagsView);
        tagsView.setTagClickListener(this);
        etInput = findViewById(R.id.editText);
        etInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    tagsView.setTags(filterTag(v.getText().toString().trim()), textGet);
                    return true;
                }
                return false;
            }
        });
        Api.doGetAllTags(callback);
    }

    private JSONArray filterTag(@NonNull String text) {
        if (text.isEmpty()) {
            return allTags;
        }
        JSONArray result = new JSONArray();
        for (int i = 0; i < allTags.length(); i++) {
            try {
                JSONObject tag = allTags.getJSONObject(i);
                String tagName = textGet.onGetText(tag);
                if (tagName.contains(text)) {
                    result.put(tag);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                continue;
            }
        }
        return result;
    }

    @Override
    protected void onResume() {
        super.onResume();
        etInput.setFocusable(true);
        etInput.setFocusableInTouchMode(true);
        etInput.requestFocus();
        imm.showSoftInput(etInput, 0);
    }

    private final TagsView.TextGet<JSONObject> textGet = new TagsView.TextGet<JSONObject>() {
        @Override
        public String onGetText(JSONObject jsonObject) {
            try {
                return jsonObject.getString("name") + "(" + jsonObject.getInt("count") + ")";
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
            if (e != null) {
                alertException(e);
                return;
            }
            if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                Log.d(TAG, "onCompleted: err");
                return;
            }
            Log.d(TAG, "onCompleted: " + jsonArray.length());
            allTags = jsonArray;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tagsView.setTags(jsonArray, textGet);
                }
            });

        }
    };

    @Override
    public void onTagClick(JSONObject jsonObject) {
        try {
            int tagId = jsonObject.getInt("id");
            Log.d(TAG, "onTagClick: " + tagId);
            String tagName = jsonObject.getString("name");
            setTitle(tagName);
            Intent intent = new Intent();
            intent.putExtra("resultType", "tag");
            intent.putExtra("id", tagId);
            intent.putExtra("name", tagName);
            setResult(RESULT_OK, intent);
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
