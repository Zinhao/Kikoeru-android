package com.zinhao.kikoeru

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.addCallback
import com.koushikdutta.async.http.AsyncHttpClient.JSONArrayCallback
import com.koushikdutta.async.http.AsyncHttpResponse
import com.zinhao.kikoeru.Api.doGetCirclesList
import com.zinhao.kikoeru.TagsView.TagClickListener
import com.zinhao.kikoeru.TagsView.TextGet
import com.zinhao.kikoeru.databinding.LayoutTagsBinding
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class CirclesActivity : BaseActivity(), TagClickListener<JSONObject?> {
    private lateinit var circlesView: TagsView<Any?>
    private lateinit var etInput: EditText
    private var allCircles: JSONArray? = null
    private var imm: InputMethodManager? = null
    private lateinit var viewBinding: LayoutTagsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = LayoutTagsBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setSafeArea(viewBinding.root, null)
        onBackPressedDispatcher.addCallback(this,true){
            setResult(RESULT_CANCELED)
            finish()
        }
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        circlesView = viewBinding.tagsView
        circlesView.setTagClickListener(this)
        circlesView.setTagBackgroundResource(R.drawable.card_bg_circles)
        etInput = viewBinding.editText
        etInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                circlesView.setTags(filterTag(s.toString().trim { it <= ' ' }), textGet)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
        viewBinding.swipe.setOnRefreshListener {doGetCirclesList(callback)}
        viewBinding.swipe.isRefreshing = true
        doGetCirclesList(callback)
    }

    private fun filterTag(text: String): JSONArray? {
        if (text.isEmpty()) {
            return allCircles
        }
        val result = JSONArray()
        allCircles?.let {
            for (i in 0..<it.length()) {
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

    override fun onResume() {
        super.onResume()
        etInput.setFocusable(true)
        etInput.setFocusableInTouchMode(true)
        etInput.requestFocus()
        imm?.showSoftInput(etInput, 0)
    }

    private val textGet: TextGet<JSONObject?> = object : TextGet<JSONObject?> {
        override fun onGetText(jsonObject: JSONObject?): String {
            return jsonObject?.opt("name").toString() + "(" + jsonObject?.optInt("count") + ")"
        }
    }

    private val callback: JSONArrayCallback = object : JSONArrayCallback() {
        override fun onCompleted(e: Exception?, asyncHttpResponse: AsyncHttpResponse?, jsonArray: JSONArray) {
            runOnUiThread {viewBinding.swipe.isRefreshing = false}
            if (e != null) {
                alertException(e)
                return
            }
            if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                Log.d(TAG, "onCompleted: err")
                return
            }
            Log.d(TAG, "onCompleted: " + jsonArray.length())
            allCircles = jsonArray
            runOnUiThread(object : Runnable {
                override fun run() {
                    circlesView.setTags(jsonArray, textGet)
                }
            })
        }
    }

    override fun onTagClick(jsonObject: JSONObject?) {
        try {
            val tagId = jsonObject?.getInt("id")?.toLong()
            Log.d(TAG, "onTagClick: " + tagId)
            val tagName = jsonObject?.getString("name")
            setTitle(tagName)
            val intent = Intent()
            intent.putExtra("resultType", "circles")
            intent.putExtra("id", tagId)
            intent.putExtra("name", tagName)
            setResult(RESULT_OK, intent)
            finish()
        } catch (e: JSONException) {
            e.printStackTrace()
            alertException(e)
        }
    }

    companion object {
        private const val TAG = "CirclesActivity"
    }
}
