package com.zinhao.kikoeru

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.koushikdutta.async.http.AsyncHttpClient.JSONObjectCallback
import com.koushikdutta.async.http.AsyncHttpResponse
import com.zinhao.kikoeru.Api.doGetWork
import com.zinhao.kikoeru.TagsView.TagClickListener
import com.zinhao.kikoeru.databinding.ActivitySearchBinding
import org.json.JSONException
import org.json.JSONObject

class SearchActivity : BaseActivity(), TagClickListener<JSONObject?> {
    private var etInput: EditText? = null
    private var recyclerView: RecyclerView? = null
    private val works: MutableList<JSONObject> = ArrayList()
    private var workAdapter: WorkAdapter? = null
    private var imm: InputMethodManager? = null
    private lateinit var viewBinding: ActivitySearchBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setSafeArea(getWindow().getDecorView(), null)
        imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        recyclerView = viewBinding.recyclerView
        etInput = viewBinding.editTextNumber
        etInput!!.setOnEditorActionListener(object : OnEditorActionListener {
            override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    viewBinding.swipe.isRefreshing = true
                    doGetWork(v.getText().toString().trim { it <= ' ' }, 1, searchWorkCallback)
                    return true
                }
                return false
            }
        })
        etInput!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                if (s.toString().length >= 6) {
                    viewBinding.swipe.isRefreshing = true
                    doGetWork(s.toString(), 1, searchWorkCallback)
                }
            }
        })
        viewBinding.swipe.setOnRefreshListener {
            viewBinding.editTextNumber.text.toString().trim().let {
                if(it.isNotBlank()){
                    doGetWork(it, 1, searchWorkCallback)
                }
            }
        }
        initLayout(WorkAdapter.LAYOUT_BIG_GRID)
    }

    private fun initLayout(layoutType: Int) {
        var layoutManager: RecyclerView.LayoutManager? = null
        if (layoutType == WorkAdapter.LAYOUT_LIST) {
            layoutManager = LinearLayoutManager(this@SearchActivity)
        } else if (layoutType == WorkAdapter.LAYOUT_SMALL_GRID) {
            layoutManager = GridLayoutManager(this@SearchActivity, 3)
        } else if (layoutType == WorkAdapter.LAYOUT_BIG_GRID) {
            layoutManager = GridLayoutManager(this@SearchActivity, 2)
        }
        workAdapter = WorkAdapter(works, layoutType)
        workAdapter!!.setTagClickListener(this)
        workAdapter!!.setVaClickListener(vaClickListener)
        workAdapter!!.setItemClickListener { v ->
            val item = v.getTag() as JSONObject
            val intent = Intent(v.getContext(), WorkTreeActivity::class.java)
            intent.putExtra("work_json_str", item.toString())
            ActivityCompat.startActivity(this@SearchActivity, intent, null)
        }
        recyclerView!!.setLayoutManager(layoutManager)
        recyclerView!!.setAdapter(workAdapter)
    }

    override fun onResume() {
        super.onResume()
        etInput!!.setFocusable(true)
        etInput!!.setFocusableInTouchMode(true)
        etInput!!.requestFocus()
        imm!!.showSoftInput(etInput, 0)
    }

    private val vaClickListener: TagClickListener<JSONObject?> = TagClickListener<JSONObject?> { jsonObject ->
        try {
            val vaId = jsonObject?.getString("id")
            Log.d(TAG, "onTagClick: " + vaId)
            val vaName = jsonObject?.getString("name")
            setTitle(vaName)
            val intent = Intent(this@SearchActivity, WorksActivity::class.java)
            intent.putExtra("resultType", "va")
            intent.putExtra("id", vaId)
            intent.putExtra("name", vaName)
            startActivity(intent)
        } catch (e: JSONException) {
            e.printStackTrace()
            alertException(e)
        }
    }


    private val searchWorkCallback: JSONObjectCallback = object : JSONObjectCallback() {
        override fun onCompleted(e: Exception?, asyncHttpResponse: AsyncHttpResponse?, jsonObject: JSONObject) {
            runOnUiThread {viewBinding.swipe.isRefreshing = false}
            if (e != null) {
                e.printStackTrace()
                alertException(e)
                return
            }
            if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                return
            }
            try {
                val jsonArray = jsonObject.getJSONArray("works")
                val totalCount = jsonObject.getJSONObject("pagination").getInt("totalCount")
                runOnUiThread(object : Runnable {
                    @SuppressLint("DefaultLocale")
                    override fun run() {
                        works.clear()
                        setTitle(String.format("%s(%d)", getString(R.string.app_name), totalCount))
                        for (i in 0..<jsonArray.length()) {
                            try {
                                works.add(jsonArray.getJSONObject(i))
                            } catch (jsonException: JSONException) {
                                jsonException.printStackTrace()
                                alertException(jsonException)
                            }
                        }
                        workAdapter!!.notifyDataSetChanged()

                    }
                })
            } catch (jsonException: JSONException) {
                jsonException.printStackTrace()
                alertException(jsonException)
            }
        }
    }

    override fun onTagClick(jsonObject: JSONObject?) {
        try {
            val tagId = jsonObject?.getInt("id")
            val tagName = jsonObject?.getString("name")
            setTitle(tagName)
            val intent = Intent(this@SearchActivity, WorksActivity::class.java)
            intent.putExtra("resultType", "tag")
            intent.putExtra("id", tagId)
            intent.putExtra("name", tagName)
            startActivity(intent)
        } catch (e: JSONException) {
            e.printStackTrace()
            alertException(e)
        }
    }

    companion object {
        private const val TAG = "SearchActivity"
    }
}