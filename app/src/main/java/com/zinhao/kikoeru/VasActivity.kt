package com.zinhao.kikoeru;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class VasActivity extends BaseActivity implements TagsView.TagClickListener<JSONObject> {
    private static final String TAG = "VasActivity";
    private TagsView<JSONArray> vasView;
    private EditText etInput;
    private JSONArray allVas;
    private InputMethodManager imm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_tags);
        setSafeArea(getWindow().getDecorView(),null);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        vasView = findViewById(R.id.tagsView);
        vasView.setTagClickListener(this);
        vasView.setTagBackgroundResource(R.drawable.card_bg_va);
        etInput = findViewById(R.id.editText);
        etInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    vasView.setTags(filterTag(v.getText().toString().trim()), textGet);
                    return true;
                }
                return false;
            }
        });
        etInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                vasView.setTags(filterTag(s.toString().trim()), textGet);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });
        Api.doGetAllVas(callback);
    }

    private JSONArray filterTag(@NonNull String text) {
        if (text.isEmpty()) {
            return allVas;
        }
        JSONArray result = new JSONArray();
        for (int i = 0; i < allVas.length(); i++) {
            try {
                JSONObject tag = allVas.getJSONObject(i);
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

    private final TagsView.TextGet<JSONObject> textGet = new TagsView.TextGet<JSONObject>() {
        @Override
        public String onGetText(JSONObject jsonObject) {
            return jsonObject.optString("name") + "(" + jsonObject.optInt("count") + ")";
        }
    };

    private final AsyncHttpClient.JSONArrayCallback callback = new AsyncHttpClient.JSONArrayCallback() {
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
            allVas = jsonArray;
            Log.d(TAG, "onCompleted: " + jsonArray.length());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    vasView.setTags(jsonArray, textGet);
                    if (!vasView.isInLayout()) {
                        vasView.requestLayout();
                    } else {
                        vasView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!vasView.isInLayout()) {
                                    vasView.requestLayout();
                                }
                            }
                        }, 500);
                    }

                }
            });

        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        etInput.setFocusable(true);
        etInput.setFocusableInTouchMode(true);
        etInput.requestFocus();
        imm.showSoftInput(etInput, 0);
    }

    @Override
    public void onTagClick(JSONObject jsonObject) {
        try {
            String vaId = jsonObject.getString("id");
            Log.d(TAG, "onTagClick: " + vaId);
            String vaName = jsonObject.getString("name");
            setTitle(vaName);
            Intent intent = new Intent();
            intent.putExtra("resultType", "va");
            intent.putExtra("id", vaId);
            intent.putExtra("name", vaName);
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
