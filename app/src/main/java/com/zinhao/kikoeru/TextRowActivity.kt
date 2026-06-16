package com.zinhao.kikoeru

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.koushikdutta.async.http.AsyncHttpClient
import com.koushikdutta.async.http.AsyncHttpResponse
import com.zinhao.kikoeru.Api.doGetMediaString
import com.zinhao.kikoeru.AudioService.CtrlBinder
import com.zinhao.kikoeru.databinding.ActivityLrcShowBinding
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class TextRowActivity : BaseActivity(), ServiceConnection {
    private var ctrlBinder: CtrlBinder? = null
    private val textCallback: AsyncHttpClient.StringCallback = object : AsyncHttpClient.StringCallback() {
        override fun onCompleted(e: Exception?, asyncHttpResponse: AsyncHttpResponse?, s: String?) {
            if (e != null) {
                alertException(e)
                return
            }
            s?.let {
                if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                    runOnUiThread { init(it) }
                    return
                }
                runOnUiThread { init(it) }
            }

        }
    }

    private var mRecyclerView: RecyclerView? = null
    private var mText: Text? = null
    private var adapter: TextAdapter? = null

    private lateinit var fileItem: JSONObject
    private lateinit var viewBinding: ActivityLrcShowBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding  = ActivityLrcShowBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setSafeArea(viewBinding.appBarLayout, null)
        mRecyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val text = intent.getStringExtra("jsonText")
        bindService(Intent(this, AudioService::class.java), this, BIND_AUTO_CREATE)
        if (text == null) {
            finish()
            return
        }
        try {
            fileItem = JSONObject(text)
            setTitle(fileItem.optString(JSONConst.WorkTree.TITLE))
            if (fileItem.has(JSONConst.WorkTree.EXISTS)) {
                val exists = fileItem.getBoolean(JSONConst.WorkTree.EXISTS)
                if (exists) {
                    val mapFile = File(fileItem.getString(JSONConst.WorkTree.MAP_FILE_PATH))
                    LocalFileCache.getInstance().readText(mapFile, textCallback)
                } else {
                    val hash = fileItem.getString(JSONConst.WorkTree.HASH)
                    doGetMediaString(hash, textCallback)
                }
            }
        } catch (e: JSONException) {
            init(e.message.toString())
            alertException(e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val subMenu = menu.addSubMenu(0, 0, 0, "load as lrc")
        subMenu.setIcon(R.drawable.ic_baseline_text_fields_24)
        subMenu.item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 0 && mText != null) {
            ctrlBinder?.let { ctrlBinder->
                ctrlBinder.setLrc(mText!!.text)
                Toast.makeText(this, "load as lrc success", Toast.LENGTH_SHORT).show()
                ctrlBinder.insertLrcBind(fileItem.optString(JSONConst.WorkTree.HASH))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun init(s: String) {
        mText = Text(s)
        adapter = TextAdapter(mText)
        mRecyclerView?.setAdapter(adapter)
        mRecyclerView?.setLayoutManager(LinearLayoutManager(this))
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        ctrlBinder = service as CtrlBinder?
    }

    override fun onServiceDisconnected(name: ComponentName?) {}

    companion object {
        private const val TAG = "TextRowActivity"
        fun start(context: Context, jsonStr: String?) {
            val starter = Intent(context, TextRowActivity::class.java)
            starter.putExtra("jsonText", jsonStr)
            context.startActivity(starter)
        }
    }
}
