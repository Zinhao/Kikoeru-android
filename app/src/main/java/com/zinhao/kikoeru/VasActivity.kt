package com.zinhao.kikoeru

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.activity.addCallback
import com.koushikdutta.async.http.AsyncHttpClient.JSONArrayCallback
import com.koushikdutta.async.http.AsyncHttpResponse
import com.zinhao.kikoeru.Api.doGetAllVas
import com.zinhao.kikoeru.TagsView.TagClickListener
import com.zinhao.kikoeru.TagsView.TextGet
import com.zinhao.kikoeru.databinding.LayoutTagsBinding
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class VasActivity : BaseActivity(), TagClickListener<JSONObject?> {
    private lateinit var vasView: TagsView<Any?>
    private lateinit var etInput: EditText
    private var allVas: JSONArray? = null
    private var imm: InputMethodManager? = null
    private lateinit var viewBinding: LayoutTagsBinding
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutTagsBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setSafeArea(getWindow().getDecorView(), null)
        onBackPressedDispatcher.addCallback(this,true){
            setResult(RESULT_CANCELED)
            finish()
        }
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        vasView = viewBinding.tagsView
        vasView.setTagClickListener(this)
        vasView.setTagBackgroundResource(R.drawable.card_bg_va)
        etInput = viewBinding.editText
        etInput.setOnEditorActionListener(object : OnEditorActionListener {
            override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    vasView.setTags(filterTag(v.getText().toString().trim { it <= ' ' }), textGet)
                    return true
                }
                return false
            }
        })
        etInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                vasView.setTags(filterTag(s.toString().trim { it <= ' ' }), textGet)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        viewBinding.swipe.setOnRefreshListener {doGetAllVas(callback)}
        viewBinding.swipe.isRefreshing = true
        doGetAllVas(callback)
    }

    private fun filterTag(text: String): JSONArray {
        if (text.isEmpty()) {
            return allVas!!
        }
        val result = JSONArray()
        allVas?.let {
            for (i in 0..< it.length()) {
                try {
                    val tag = it.getJSONObject(i)
                    val tagName = textGet.onGetText(tag)
                    if (tagName.contains(text)) {
                        result.put(tag)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    continue
                }
            }
        }
        return result
    }

    private val textGet: TextGet<JSONObject?> = object : TextGet<JSONObject?> {
        override fun onGetText(jsonObject: JSONObject?): String {
            return jsonObject?.optString("name") + "(" + jsonObject?.optInt("count") + ")"
        }
    }

    private val callback: JSONArrayCallback = object : JSONArrayCallback() {
        override fun onCompleted(e: Exception?, asyncHttpResponse: AsyncHttpResponse?, jsonArray: JSONArray) {
            runOnUiThread { viewBinding.swipe.isRefreshing = false }
            if (e != null) {
                alertException(e)
                return
            }
            if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                Log.d(TAG, "onCompleted: err")
                return
            }
            allVas = jsonArray
            Log.d(TAG, "onCompleted: " + jsonArray.length())
            runOnUiThread(object : Runnable {
                override fun run() {
                    vasView!!.setTags(jsonArray, textGet)
                    if (!vasView!!.isInLayout()) {
                        vasView!!.requestLayout()
                    } else {
                        vasView!!.postDelayed(object : Runnable {
                            override fun run() {
                                if (!vasView!!.isInLayout()) {
                                    vasView!!.requestLayout()
                                }
                            }
                        }, 500)
                    }
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        etInput!!.setFocusable(true)
        etInput!!.setFocusableInTouchMode(true)
        etInput!!.requestFocus()
        imm!!.showSoftInput(etInput, 0)
    }

    override fun onTagClick(jsonObject: JSONObject?) {
        try {
            val vaId = jsonObject?.getString("id")
            Log.d(TAG, "onTagClick: " + vaId)
            val vaName = jsonObject?.getString("name")
            setTitle(vaName)
            val intent = Intent()
            intent.putExtra("resultType", "va")
            intent.putExtra("id", vaId)
            intent.putExtra("name", vaName)
            setResult(RESULT_OK, intent)
            finish()
        } catch (e: JSONException) {
            e.printStackTrace()
            alertException(e)
        }
    }

    companion object {
        private const val TAG = "VasActivity"
    }
}
