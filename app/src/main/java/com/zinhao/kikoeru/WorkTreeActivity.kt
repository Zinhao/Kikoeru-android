package com.zinhao.kikoeru

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnLongClickListener
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.Insets
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.bumptech.glide.Glide
import com.koushikdutta.async.http.AsyncHttpClient.JSONArrayCallback
import com.koushikdutta.async.http.AsyncHttpClient.JSONObjectCallback
import com.koushikdutta.async.http.AsyncHttpResponse
import com.zinhao.kikoeru.Api.doGetDocTree
import com.zinhao.kikoeru.Api.doGetWorkByCircles
import com.zinhao.kikoeru.Api.doGetWorkByVa
import com.zinhao.kikoeru.Api.doPutReview
import com.zinhao.kikoeru.Api.formatGetUrl
import com.zinhao.kikoeru.Api.minCoverImageUrl
import com.zinhao.kikoeru.AudioService.CtrlBinder
import com.zinhao.kikoeru.DownloadUtils.Mission
import com.zinhao.kikoeru.TagsView.TagClickListener
import com.zinhao.kikoeru.WorkTreeAdapter.RelativePathChangeListener
import com.zinhao.kikoeru.databinding.ActivityWorkBinding
import com.zinhao.kikoeru.db.LocalWorkHistory
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import kotlin.math.max
import kotlin.math.min

class WorkTreeActivity : BaseActivity(), View.OnClickListener, MusicChangeListener, ServiceConnection,
    OnLongClickListener, TagClickListener<JSONObject?>, RelativePathChangeListener {
    private lateinit var recyclerView: RecyclerView
    private var workTreeAdapter: WorkTreeAdapter? = null
    private var ctrlBinder: CtrlBinder? = null

    private lateinit var work: JSONObject
    private var jsonWorkTrees: JSONArray? = null

    private lateinit var bottomLayout: View
    private lateinit var ivCover: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvWorkTitle: TextView
    private lateinit var ibStatus: ImageButton
    private lateinit var ibFloatLrc: ImageButton

    private lateinit var headerViewCompat: HeaderViewCompat
    private lateinit var viewBinding: ActivityWorkBinding

    private class HeaderViewCompat(itemView: View) {
        val ivCover: ImageView
        val tvTitle: TextView
        val tvArt: TagsView<JSONArray?>
        val tvTags: TagsView<JSONArray?>
        val tvDate: TextView
        val tvPrice: TextView
        val tvSaleCount: TextView
        val tvHost: TextView
        val tvCircles: TagsView<MutableList<String?>?>

        fun setCirclesClickListener(circlesClickListener: TagClickListener<*>?) {
            tvCircles.setTagClickListener(circlesClickListener)
        }

        fun setTagClickListener(tagClickListener: TagClickListener<*>?) {
            tvTags.setTagClickListener(tagClickListener)
        }

        fun setVaClickListener(vaClickListener: TagClickListener<*>?) {
            tvArt.setTagClickListener(vaClickListener)
        }

        init {
            ivCover = itemView.findViewById<ImageView>(R.id.ivCover)
            tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
            tvArt = itemView.findViewById<TagsView<JSONArray?>>(R.id.tvArt)
            tvTags = itemView.findViewById<TagsView<JSONArray?>>(R.id.tvTags)
            tvDate = itemView.findViewById<TextView>(R.id.tvDate)
            tvPrice = itemView.findViewById<TextView>(R.id.tvPrice)
            tvSaleCount = itemView.findViewById<TextView>(R.id.tvSaleCount)
            tvHost = itemView.findViewById<TextView>(R.id.tvHost)
            tvCircles = itemView.findViewById<TagsView<MutableList<String?>?>>(R.id.tvCircles)
        }
    }

    private val docTreeCallback: JSONArrayCallback = object : JSONArrayCallback() {
        override fun onCompleted(e: Exception?, asyncHttpResponse: AsyncHttpResponse?, jsonArray: JSONArray?) {
            runOnUiThread { viewBinding.swipe.isRefreshing = false }
            if (e != null) {
                alertException(e)
                return
            }
            if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                return
            }
            jsonWorkTrees = jsonArray
            // TODO 来自不同服务器的同一个作品（RJ号码相同），当用户执行下载操作时，目录树不一致。
            runOnUiThread(Runnable {
                workTreeAdapter = WorkTreeAdapter(jsonWorkTrees, work.optInt("id"))
                workTreeAdapter?.setItemClickListener(this@WorkTreeActivity)
                workTreeAdapter?.setParentDirClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        if (workTreeAdapter != null) {
                            val r = workTreeAdapter!!.parentDir()
                            if (r) {
                                finish()
                            }
                        }
                    }
                })

                workTreeAdapter?.setItemLongClickListener(this@WorkTreeActivity)
                workTreeAdapter?.setPathChangeListener(this@WorkTreeActivity)
                val itemDecoration: ItemDecoration =
                    DividerItemDecoration(this@WorkTreeActivity, DividerItemDecoration.VERTICAL)
                recyclerView.addItemDecoration(itemDecoration)
                recyclerView.setItemAnimator(null)
                recyclerView.setLayoutManager(LinearLayoutManager(this@WorkTreeActivity))
                recyclerView.setAdapter(workTreeAdapter)
            })
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityWorkBinding.inflate(getLayoutInflater())
        setContentView(viewBinding.getRoot())
        setSafeArea(viewBinding.appBarLayout, object : InsetReady {
            override fun onInsetReady(insets: Insets) {
                viewBinding.bottomLayout.getRoot().setPadding(
                    insets.left, 0, insets.right, insets.bottom
                )
            }
        })
        setSupportActionBar(viewBinding.toolbar)
        onBackPressedDispatcher.addCallback(this,true){
            if (workAdapter!=null && recyclerView.adapter == workAdapter) {
                val itemDecoration: ItemDecoration =
                    DividerItemDecoration(this@WorkTreeActivity, DividerItemDecoration.VERTICAL)
                recyclerView.addItemDecoration(itemDecoration)
                recyclerView.setLayoutManager(LinearLayoutManager(this@WorkTreeActivity))
                recyclerView.setAdapter(workTreeAdapter)
                workAdapter = null
            }else{
                if(workTreeAdapter == null){
                    finish()
                }else{
                    if(workTreeAdapter!!.parentDir()){
                        finish()
                    }
                }

            }
        }
        val workStr = intent.getStringExtra("work_json_str")
        if (!workStr.isNullOrEmpty()) {
            try {
                work = JSONObject(workStr)
            } catch (e: JSONException) {
                e.printStackTrace()
                alertException(e)
                finish()
                return
            }
        }

        setupView()

        val header: View = viewBinding.headerInfo.getRoot()
        headerViewCompat = HeaderViewCompat(header)
        headerViewCompat.setTagClickListener(this@WorkTreeActivity)
        headerViewCompat.setVaClickListener(vaClickListener)
        headerViewCompat.setCirclesClickListener(circlesClickListener)
        initHeader()

        bindService(Intent(this, AudioService::class.java), this, BIND_AUTO_CREATE)
        loadTree()
        saveLocalHis()
    }

    private fun setupView(){
        inAnim = AnimationUtils.loadAnimation(this, R.anim.move_bottom_in)
        recyclerView = viewBinding.recyclerView
        bottomLayout = viewBinding.bottomLayout.getRoot()
        ivCover = viewBinding.bottomLayout.imageView
        tvTitle = viewBinding.bottomLayout.textView
        tvWorkTitle = viewBinding.bottomLayout.textView2
        ibStatus = viewBinding.bottomLayout.button
        ibFloatLrc = viewBinding.bottomLayout.imageButton
    }

    private fun saveLocalHis(){
        val app = getApplication() as App
        try {
            val localWorkHistory = LocalWorkHistory(
                work.getInt("id").toLong(),
                System.currentTimeMillis(), "",
                work.getString("title")
            )
            app.insertLocalHis(localWorkHistory, object : Runnable {
                override fun run() {
                }
            })
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }
    }

    private fun initHeader() {
        try {
            Glide.with(this).load(
                App.getInstance().currentUser().host + "/api/cover/${work.getInt("id")}?token=${Api.token}"
            )
                .apply(App.getInstance().getRadius15Pic())
                .into(headerViewCompat.ivCover)
            headerViewCompat.tvTitle.setText(work.getString("title"))
            headerViewCompat.tvArt.setTags(App.getVasList(work), TagsView.JSON_TEXT_GET.setKey("name"))
            headerViewCompat.tvTags.setTags(App.getTagsList(work), TagsView.JSON_TEXT_GET.setKey("name"))
            headerViewCompat.tvCircles.setTags(
                mutableListOf<String?>(work.getString("name")),
                TagsView.STRING_TEXT_GET
            )
            val dateStr = work.optString("release")
            if (dateStr.isEmpty()) {
                headerViewCompat.tvDate.setVisibility(View.GONE)
            } else {
                headerViewCompat.tvDate.setVisibility(View.VISIBLE)
                headerViewCompat.tvDate.setText(dateStr)
            }
            headerViewCompat.tvPrice.setText(String.format("%d 日元", work.getInt("price")))
            headerViewCompat.tvSaleCount.setText(String.format("售出：%d", work.getInt("dl_count")))
            if (work.has(JSONConst.Work.HOST)) {
                headerViewCompat.tvHost.setVisibility(View.VISIBLE)
                headerViewCompat.tvHost.setText(work.getString(JSONConst.Work.HOST))
            } else {
                headerViewCompat.tvHost.setVisibility(View.INVISIBLE)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            App.getInstance().alertException(e)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val workStr = intent.getStringExtra("work_json_str")
        Log.d(TAG, "onNewIntent: " + workStr)
        if (workStr != null && !workStr.isEmpty()) {
            try {
                work = JSONObject(workStr)
                loadTree()
            } catch (e: JSONException) {
                e.printStackTrace()
                alertException(e)
            }
        }
    }

    private fun loadTree() {
        Log.d(TAG, "init: work tree init")
        viewBinding.swipe.isRefreshing = true
        try {
            if (work.has(JSONConst.Work.IS_LOCAL_WORK)) {
                val isLocalWork = work.getBoolean(JSONConst.Work.IS_LOCAL_WORK)
                if (isLocalWork) {
                    LocalFileCache.getInstance().readLocalWorkTree(this, work.getInt("id"), docTreeCallback)
                    return
                }
            }
            doGetDocTree(work.getInt("id"), docTreeCallback)
        } catch (e: JSONException) {
            e.printStackTrace()
            alertException(e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val subMenu = menu.addSubMenu(0, 0, 0, R.string.mark_action)
        subMenu.setIcon(R.drawable.ic_baseline_work_24)
        subMenu.add(1, 1, 1, R.string.marked)
        subMenu.add(1, 2, 2, R.string.listening)
        subMenu.add(1, 3, 3, R.string.listened)
        subMenu.add(1, 4, 4, R.string.replay)
        subMenu.add(1, 5, 5, R.string.postponed)
        subMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
            if (item.getItemId() == 1) {
                doPutReview(work.getInt("id").toLong(), Api.FILTER_MARKED, actionCallBack)
            } else if (item.getItemId() == 2) {
                doPutReview(work.getInt("id").toLong(), Api.FILTER_LISTENING, actionCallBack)
            } else if (item.getItemId() == 3) {
                doPutReview(work.getInt("id").toLong(), Api.FILTER_LISTENED, actionCallBack)
            } else if (item.getItemId() == 4) {
                doPutReview(work.getInt("id").toLong(), Api.FILTER_REPLAY, actionCallBack)
            } else if (item.getItemId() == 5) {
                doPutReview(work.getInt("id").toLong(), Api.FILTER_POSTPONED, actionCallBack)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            alertException(e)
        }
        return super.onOptionsItemSelected(item)
    }

    private val actionCallBack: JSONObjectCallback = object : JSONObjectCallback() {
        override fun onCompleted(e: Exception?, asyncHttpResponse: AsyncHttpResponse?, jsonObject: JSONObject) {
            if (e != null) {
                alertException(e)
                return
            }
            if (asyncHttpResponse == null) return
            if (asyncHttpResponse.code() == 200) {
                try {
                    val message = jsonObject.getString("message")
                    runOnUiThread(Runnable {
                        Toast.makeText(this@WorkTreeActivity, message, Toast.LENGTH_SHORT).show()
                    })
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace()
                    alertException(jsonException)
                }
            }
        }
    }

    override fun onClick(v: View) {
        val item = v.getTag() as JSONObject
        try {
            val itemType = item.getString("type")
            if ("image" == itemType) {
                openImage(item)
            } else if ("audio" == itemType) {
                openAudioOrVideo(item)
            } else if ("text" == itemType) {
                openText(item)
            } else {
                openOther(item)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
            alertException(e)
        }
    }

    @Throws(JSONException::class)
    private fun openImage(item: JSONObject) {
        val imageList: MutableList<String?> = ArrayList<String?>()
        var index = 0
        workTreeAdapter?.let {
            for (i in 0..<it.data.length()) {
                val _item = it.data.getJSONObject(i)
                if (_item.getString("type") == "image") {
                    var url: String?
                    try {
                        url = _item.getString(JSONConst.WorkTree.MAP_FILE_PATH)
                        if (!File(url).exists()) {
                            url = _item.getString(JSONConst.WorkTree.MEDIA_STREAM_URL)
                            url = formatGetUrl(url, true)
                        }
                        imageList.add(url)
                        if (_item.getString(JSONConst.WorkTree.HASH) == item.getString(JSONConst.WorkTree.HASH)) {
                            index = imageList.size - 1
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        alertException(e)
                    }
                }
            }
        }
        ImageBrowserActivity.start(this@WorkTreeActivity, imageList, index)
    }

    private fun openText(item: JSONObject) {
        TextRowActivity.start(this, item.toString())
    }

    @Throws(JSONException::class)
    private fun openOther(item: JSONObject) {
        var url = item.getString(JSONConst.WorkTree.MEDIA_STREAM_URL)
        val intent = Intent(Intent.ACTION_VIEW)
        if (!url.startsWith("http")) {
            url = String.format("%s%s", App.getInstance().currentUser().getHost(), url)
        }
        intent.setData(Uri.parse(url))
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            alertException(e)
        }
    }

    @Throws(JSONException::class)
    private fun openAudioOrVideo(item: JSONObject) {
        val itemHash = item.getString(JSONConst.WorkTree.HASH)
        val itemTitle = item.getString("title")
        val itemMediaStreamUrl = item.getString(JSONConst.WorkTree.MEDIA_STREAM_URL)
        val musicArray: MutableList<JSONObject> = ArrayList<JSONObject>()
        val lrcArray: MutableList<JSONObject> = ArrayList<JSONObject>()
        var clickItemIndex = 0
        workTreeAdapter?.let {
            for (i in 0..<it.data.length()) {
                val seekItem = it.data.getJSONObject(i)
                val fileType = seekItem.getString("type")
                if ("audio" == fileType) {
                    musicArray.add(seekItem)
                    if (seekItem.getString(JSONConst.WorkTree.HASH) == itemHash) {
                        clickItemIndex = musicArray.size - 1
                    }
                }
                if ("text" == fileType) {
                    lrcArray.add(seekItem)
                }
            }
        }

        if (!item.has(JSONConst.WorkTree.LRC_INFO)) {
            try {
                findLrcInfo(musicArray, lrcArray)
            } catch (e: JSONException) {
                e.printStackTrace(System.err)
                alertException(e)
            }
        }
        ctrlBinder?.let {
            if (it.current != null && it.current.getString(JSONConst.WorkTree.MEDIA_STREAM_URL) == itemMediaStreamUrl) {
                if (itemTitle.lowercase().endsWith(".mp4")) {
                    startActivity(Intent(this@WorkTreeActivity, VideoPlayerActivity::class.java))
                } else {
                    startActivity(Intent(this@WorkTreeActivity, AudioPlayerActivity::class.java))
                }
            } else {
                it.play(musicArray, clickItemIndex)
            }
        }

    }

    @Throws(JSONException::class)
    fun findLrcInfo(musicArray: MutableList<JSONObject>, lrcArray: MutableList<JSONObject>) {
        for (audioItem in musicArray) {
            for (lrcItem in lrcArray) {
                val audioTitle = audioItem.optString("title")
                val lrcTitle = lrcItem.optString("title")
                if (audioTitle.isEmpty() || lrcTitle.isEmpty()) {
                    break
                }
                if (lrcTitle.contains(audioTitle)) {
                    // audio_title.mp3 -> audio_title.mp3.lrc
                    audioItem.put("lrc_info", lrcItem)
                    break
                } else {
                    // audio_title.mp3 -> audio_title.lrc
                    if (audioTitle.contains(".") && lrcTitle.contains(".")) {
                        val lastPoint = audioTitle.lastIndexOf(".")
                        val audioTitleContent = audioTitle.substring(0, lastPoint)
                        if (lrcTitle.contains(audioTitleContent)) {
                            audioItem.put("lrc_info", lrcItem)
                            break
                        }
                    }
                }
            }
        }
    }

    override fun onAlbumChange(rjNumber: Long) {
        Glide.with(this).load(minCoverImageUrl(rjNumber))
            .apply(App.getInstance().getRadius5Pic()).into(ivCover)
    }

    override fun onAudioChange(audio: JSONObject) {
        try {
            tvTitle.setText(audio.getString("title"))
            tvWorkTitle.setText(audio.getString("workTitle"))
        } catch (e: JSONException) {
            e.printStackTrace()
            alertException(e)
        }
    }

    override fun onStatusChange(status: Int) {
        if (status == 0) {
            ibStatus.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        } else {
            if (bottomLayout.getVisibility() == View.GONE) {
                showBottomLayout()
            }
            ibStatus.setImageResource(R.drawable.ic_baseline_pause_24)
        }
    }

    private var shouldShowAnim = true
    private var inAnim: Animation? = null
    private fun showBottomLayout() {
        if (shouldShowAnim) {
            shouldShowAnim = false
            bottomLayout.setVisibility(View.VISIBLE)
            bottomLayout.startAnimation(inAnim)
            bottomLayout.postDelayed(object : Runnable {
                override fun run() {
                    shouldShowAnim = true
                }
            }, inAnim!!.getDuration())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ctrlBinder!!.removeMusicChangeListener(this)
        unbindService(this)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        ctrlBinder = service as CtrlBinder
        if (ctrlBinder!!.getController().playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
            bottomLayout.setVisibility(View.VISIBLE)
        }
        ibStatus.setOnClickListener(View.OnClickListener { v: View? ->
            if (ctrlBinder!!.controller.getPlaybackState() == null) return@OnClickListener
            if (ctrlBinder!!.controller.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                ctrlBinder!!.controller.getTransportControls().pause()
            } else {
                ctrlBinder!!.controller.getTransportControls().play()
            }
        })
        ibFloatLrc.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (ctrlBinder!!.isLrcWindowShow()) {
                    ctrlBinder!!.hideLrcFloatWindow()
                } else {
                    ctrlBinder!!.showLrcFloatWindow()
                }
            }
        })
        bottomLayout.setOnClickListener(View.OnClickListener { v: View? ->
            try {
                if (ctrlBinder!!.getCurrentTitle().endsWith("mp4")) {
                    startActivity(Intent(this@WorkTreeActivity, VideoPlayerActivity::class.java))
                } else {
                    val intent = Intent(v!!.getContext(), AudioPlayerActivity::class.java)
                    val view = v.findViewById<View>(R.id.imageView)
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this@WorkTreeActivity, view, "hero_bottom" // 这里的字符串必须匹配 transitionName
                    )
                    startActivity(intent, options.toBundle())
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                alertException(e)
            }
        })
        ctrlBinder!!.addMusicChangeListener(this)
    }

    override fun onServiceDisconnected(name: ComponentName?) {}

    override fun onLongClick(v: View): Boolean {
        val item = v.getTag() as JSONObject?
        if (item == null) return false
        if (work == null) return false
        try {
            val itemType = item.getString("type")
            if (!item.has(JSONConst.WorkTree.MAP_FILE_PATH)) {
                return false
            }
            val itemFile = File(item.getString(JSONConst.WorkTree.MAP_FILE_PATH))
            val builder = AlertDialog.Builder(this)
            builder.setMessage(itemFile.getAbsolutePath())
            if (itemFile.exists()) {
                val mapMission = DownloadUtils.mapMission(item)
                if (mapMission != null) {
                    builder.setTitle(R.string.downloading)
                    builder.setNegativeButton(
                        R.string.cancel_download,
                        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> mapMission.stop() })
                    builder.setPositiveButton(
                        R.string.check_mission,
                        DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
                            startActivity(Intent(this@WorkTreeActivity, DownLoadMissionActivity::class.java))
                            dialogInterface!!.dismiss()
                        })
                } else {
                    builder.setNegativeButton(
                        R.string.open,
                        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                            try {
                                if (itemType == "audio") {
                                    openAudioOrVideo(item)
                                } else if (itemType == "text") {
                                    openText(item)
                                } else if (itemType == "image") {
                                    openImage(item)
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        })
                    builder.setPositiveButton(
                        "open with",
                        DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                            val intent = Intent(Intent.ACTION_VIEW)
                            val uri = FileProvider.getUriForFile(
                                this@WorkTreeActivity,
                                getPackageName() + ".fileProvider",
                                itemFile
                            )
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            intent.setDataAndType(uri, String.format("%s/*", itemType))
                            try {
                                startActivity(intent)
                            } catch (e: ActivityNotFoundException) {
                                alertException(e)
                            }
                        })
                }
            } else {
                builder.setTitle(getString(R.string.not_download))
                builder.setMessage(itemFile.getAbsolutePath())
                builder.setNegativeButton(
                    R.string.download,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        if (work.has(JSONConst.Work.IS_LOCAL_WORK)) {
                            // 从本地目录树开始下载
                            if (!work.has(JSONConst.Work.HOST)) {
                                return@OnClickListener
                            }
                            try {
                                val workHost = work.getString(JSONConst.Work.HOST)
                                if (App.getInstance().currentUser().getHost() != workHost) {
                                    Toast.makeText(
                                        this@WorkTreeActivity,
                                        "switch host user then start download!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                   return@OnClickListener
                                }
                            } catch (e: JSONException) {
                                throw RuntimeException(e)
                            }
                        }
                        val havePermission: Boolean
                        val downLoadMission = Mission(item)
                        downLoadMission.setSuccessCallback(Runnable {
                            runOnUiThread(Runnable {
                                if (!isDestroyed()) {
                                    workTreeAdapter!!.mapFileExistValue()
                                }
                            })
                        })
                        if (App.getInstance().isSaveExternal()) {
                            havePermission = requestReadWriteExternalPermission(Runnable {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    if (!Environment.isExternalStorageManager()) {
                                        return@Runnable
                                    }
                                } else {
                                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                                       return@Runnable
                                    }
                                }
                                saveWorkWithTree()
                                downLoadMission.start()
                            })
                        } else {
                            havePermission = true
                        }
                        if (havePermission) {
                            saveWorkWithTree()
                            downLoadMission.start()
                            runOnUiThread(Runnable {
                                workTreeAdapter?.notifyDataSetChanged()
                            })
                        }
                        dialog!!.dismiss()
                    })
                val itemStreamUrl = item.getString(JSONConst.WorkTree.MEDIA_STREAM_URL)
                builder.setPositiveButton(
                    "open in browser",
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        val intent = Intent(Intent.ACTION_VIEW)
                        val readableUrl: String?
                        if (!itemStreamUrl.startsWith("http")) {
                            readableUrl = String.format(
                                "%s%s?token=%s",
                                App.getInstance().currentUser().getHost(),
                                itemStreamUrl,
                                Api.token
                            )
                        } else {
                            readableUrl = String.format("%s?token=%s", itemStreamUrl, Api.token)
                        }
                        intent.setData(Uri.parse(readableUrl))
                        try {
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            alertException(e)
                        }
                    })
            }
            builder.create().show()
        } catch (e: JSONException) {
            e.printStackTrace()
            alertException(e)
        }
        return true
    }

    private fun saveWorkWithTree() {
        if (jsonWorkTrees != null) {
            try {
                work.put(JSONConst.Work.HOST, App.getInstance().currentUser().getHost())
                LocalFileCache.getInstance().saveWork(work, jsonWorkTrees)
            } catch (jsonException: JSONException) {
                jsonException.printStackTrace()
            }
        }
    }

    override fun onTagClick(jsonObject: JSONObject?) {
        try {
            val tagId = jsonObject?.getInt("id")
            val tagName = jsonObject?.getString("name")
            setTitle(tagName)
            val intent = Intent(this@WorkTreeActivity, WorksActivity::class.java)
            intent.putExtra("resultType", "tag")
            intent.putExtra("id", tagId)
            intent.putExtra("name", tagName)
            startActivity(intent)
        } catch (e: JSONException) {
            e.printStackTrace()
            alertException(e)
        }
    }

    private var workAdapter: WorkAdapter? = null
    private var works: MutableList<JSONObject>? = null
    private var page = 1

    private val apisCallback: JSONObjectCallback = object : JSONObjectCallback() {
        override fun onCompleted(e: Exception?, asyncHttpResponse: AsyncHttpResponse?, jsonObject: JSONObject) {
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
                page = jsonObject.getJSONObject("pagination").getInt("currentPage") + 1

                if (jsonArray.length() != 0) {
                    page = min(page, totalCount / jsonArray.length() + 1)
                }
                runOnUiThread(object : Runnable {
                    @SuppressLint("DefaultLocale")
                    override fun run() {
                        for (i in 0..<jsonArray.length()) {
                            try {
                                if (works == null) {
                                    works = ArrayList()
                                }
                                works!!.add(jsonArray.getJSONObject(i))
                            } catch (jsonException: JSONException) {
                                jsonException.printStackTrace()
                                alertException(jsonException)
                            }
                        }
                        initLayout()
                        workAdapter!!.notifyItemRangeInserted(
                            max(0, works!!.size - jsonArray.length()),
                            jsonArray.length()
                        )
                        workAdapter!!.notifyItemRangeChanged(
                            max(0, works!!.size - jsonArray.length()),
                            jsonArray.length()
                        )
                    }
                })
            } catch (jsonException: JSONException) {
                jsonException.printStackTrace()
                alertException(jsonException)
            }
        }
    }

    private val vaClickListener = TagClickListener { jsonObject: JSONObject? ->
        try {
            val vaId = jsonObject!!.getString("id")
            Log.d(TAG, "onTagClick: " + vaId)
            val vaName = jsonObject.getString("name")
            setTitle(vaName)
            doGetWorkByVa(page, vaId, apisCallback)
        } catch (e: JSONException) {
            e.printStackTrace()
            alertException(e)
        }
    }

    private val circlesClickListener = TagClickListener { circlesName: String? ->
        //todo
        //http://localhost:8980/api/circles/
        //http://localhost:8980/api/circles/54978/works?order=release&sort=desc&page=1&seed=59
        val circlesId = App.getInstance().mapCirclesId(circlesName)
        if (circlesId != -1L) {
            doGetWorkByCircles(page, circlesId, apisCallback)
        }
        setTitle(circlesName)
        Log.d(TAG, "onTagClick: " + circlesName)
    }

    private fun initLayout() {
        var layoutManager: RecyclerView.LayoutManager? = null
        val col = max(getResources().getDisplayMetrics().widthPixels / 395, 3)
        layoutManager = GridLayoutManager(this, col)
        works?.let {
            workAdapter = WorkAdapter(it, WorkAdapter.LAYOUT_SMALL_GRID)
            workAdapter!!.setItemClickListener { v ->
                val item = v.getTag() as JSONObject
                val intent = Intent(v.getContext(), WorkTreeActivity::class.java)
                intent.putExtra("work_json_str", item.toString())
                ActivityCompat.startActivity(v.getContext(), intent, null)
            }
            recyclerView.setLayoutManager(layoutManager)
            recyclerView.setAdapter(workAdapter)
        }

    }

    override fun onPathChange(path: String) {
        var path = path
        if (path.length > 15) {
            path = "..." + path.substring(path.length - 12)
        }
        setTitle(path)
    }

    companion object {
        /**
         * [...](http://localhost:8888/api/tracks/357844)
         */
        private const val TAG = "WorkActivity"
    }
}