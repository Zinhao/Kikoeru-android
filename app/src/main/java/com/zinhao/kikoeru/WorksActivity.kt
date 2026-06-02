package com.zinhao.kikoeru

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
import android.widget.AdapterView.OnItemClickListener
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.bumptech.glide.Glide
import com.koushikdutta.async.http.AsyncHttpClient.JSONArrayCallback
import com.koushikdutta.async.http.AsyncHttpClient.JSONObjectCallback
import com.koushikdutta.async.http.AsyncHttpResponse
import com.zinhao.kikoeru.Api.doGetCirclesList
import com.zinhao.kikoeru.Api.doGetReview
import com.zinhao.kikoeru.Api.doGetWorkByCircles
import com.zinhao.kikoeru.Api.doGetWorkByVa
import com.zinhao.kikoeru.Api.doGetWorks
import com.zinhao.kikoeru.Api.doGetWorksByTag
import com.zinhao.kikoeru.Api.minCoverImageUrl
import com.zinhao.kikoeru.Api.setOrder
import com.zinhao.kikoeru.AudioService.CtrlBinder
import com.zinhao.kikoeru.TagsView.TagClickListener
import com.zinhao.kikoeru.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.math.max
import kotlin.math.min

class WorksActivity : BaseActivity(), MusicChangeListener, ServiceConnection, TagClickListener<JSONObject?> {
    private var recyclerView: RecyclerView? = null
    private var workAdapter: WorkAdapter? = null
    private var works: MutableList<JSONObject?>? = null
    private var scrollListener: RecyclerView.OnScrollListener? = null
    private var page = 1
    private var currentPage = 0
    private var totalCount = 0

    private var bottomLayout: View? = null
    private var outAnim: Animation? = null
    private var inAnim: Animation? = null
    private var shouldShowAnim = true
    private var ivCover: ImageView? = null
    private var tvTitle: TextView? = null
    private var tvWorkTitle: TextView? = null
    private var ibStatus: ImageButton? = null
    private var ibFloatLrcWindow: ImageButton? = null
    private var tagId = -1
    private var tagStr: String? = ""
    private var vaId = ""
    private var vaName: String? = ""
    private var circlesName: String? = ""
    private var circlesId: Long = -1

    private var type: Int = TYPE_ALL_WORK
    private var ctrlBinder: CtrlBinder? = null
    private var progressMenu: ListPopupWindow? = null
    private var moreMenu: ListPopupWindow? = null
    private var itemDecoration: ItemDecoration? = null
    private lateinit var viewBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            enableEdgeToEdge()
        }
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setSupportActionBar(viewBinding.toolbar)
        setContentView(viewBinding.root)
        // 统一应用状态栏和导航栏的系统边距
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
        // 统一应用状态栏和导航栏的系统边距
        recyclerView = viewBinding.recyclerView
        bottomLayout = viewBinding.bottomLayout.root
        ivCover = bottomLayout!!.findViewById<ImageView>(R.id.imageView)
        tvTitle = bottomLayout!!.findViewById<TextView>(R.id.textView)
        tvWorkTitle = bottomLayout!!.findViewById<TextView>(R.id.textView2)
        ibStatus = bottomLayout!!.findViewById<ImageButton>(R.id.button)
        ibFloatLrcWindow = bottomLayout!!.findViewById<ImageButton>(R.id.imageButton)
        startForegroundService(Intent(this, AudioService::class.java))
        bindService(Intent(this, AudioService::class.java), this, BIND_AUTO_CREATE)
        itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        outAnim = AnimationUtils.loadAnimation(this, R.anim.move_bottom_out)
        inAnim = AnimationUtils.loadAnimation(this, R.anim.move_bottom_in)
        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (works!!.size >= totalCount) {
                    workAdapter!!.setLoading(false)
                    return
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (!recyclerView.canScrollVertically(1)) {
                        Log.i(TAG, "work size:" + works!!.size + ", total:" + totalCount)
                        reloadRecycleView()
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }
        }
        works = ArrayList<JSONObject?>()
        type = App.getInstance().getValue(CONFIG_TYPE, TYPE_ALL_WORK.toLong()).toInt()
        page = App.getInstance().getValue(CONFIG_PAGE, 1).toInt()
        vaId = App.getInstance().getValue(CONFIG_PARAM_STR, "")
        tagId = App.getInstance().getValue(CONFIG_PARAM_INT, -1).toInt()

        viewBinding.bt1.setOnClickListener(View.OnClickListener { v: View? ->
            type = TYPE_ALL_WORK
            clearWork()
            reloadRecycleView()
        })
        progressMenu = ListPopupWindow(this)
        progressMenu!!.setAdapter(
            ArrayAdapter<String?>(
                this, android.R.layout.simple_list_item_1,
                Arrays.asList<String?>(
                    getString(R.string.marked),
                    getString(R.string.listening),
                    getString(R.string.listened),
                    getString(R.string.replay),
                    getString(R.string.postponed)
                )
            )
        )
        progressMenu!!.setModal(true)
        progressMenu!!.setAnchorView( viewBinding.bt2)
        progressMenu!!.setOnItemClickListener(OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            progressMenu!!.dismiss()
            when (position) {
                0 -> type = TYPE_SELF_MARKED
                1 -> type = TYPE_SELF_LISTENING
                2 -> type = TYPE_SELF_LISTENED
                3 -> type = TYPE_SELF_REPLAY
                4 -> type = TYPE_SELF_POSTPONED
            }
            clearWork()
            reloadRecycleView()
        })
        viewBinding.bt2.setOnClickListener(View.OnClickListener { v: View? -> progressMenu!!.show() })

        moreMenu = ListPopupWindow(this)
        moreMenu?.setAdapter(
            ArrayAdapter<String?>(
                this, android.R.layout.simple_list_item_1,
                Arrays.asList<String?>(
                    getString(R.string.va_voicer),
                    getString(R.string.tag), "Circles",
                    getString(R.string.local_works)
                )
            )
        )
        moreMenu?.setModal(true)
        moreMenu?.setAnchorView( viewBinding.bt3)
        moreMenu?.setOnItemClickListener(OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            moreMenu!!.dismiss()
            when (position) {
                0 -> startActivityForResult(Intent(view!!.getContext(), VasActivity::class.java), VA_SELECT_RESULT)
                1 -> startActivityForResult(Intent(view!!.getContext(), TagsActivity::class.java), TAG_SELECT_RESULT)
                2 -> startActivityForResult(
                    Intent(view!!.getContext(), CirclesActivity::class.java),
                    CIRCLES_SELECT_RESULT
                )

                3 -> {
                    type = TYPE_LOCAL_WORK
                    clearWork()
                    reloadRecycleView()
                }
            }
        })
        viewBinding.bt3.setOnClickListener(View.OnClickListener { v: View? -> moreMenu?.show() })
        ibFloatLrcWindow!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (ctrlBinder!!.isLrcWindowShow()) {
                    ctrlBinder!!.hideLrcFloatWindow()
                } else {
                    ctrlBinder!!.showLrcFloatWindow()
                }
            }
        })
        doGetCirclesList(object : JSONArrayCallback() {
            override fun onCompleted(e: Exception?, asyncHttpResponse: AsyncHttpResponse?, jsonArray: JSONArray) {
                if (e != null) {
                    App.getInstance().alertException(e)
                    return
                }
                try {
                    App.getInstance().initCirclesIdMap(jsonArray)
                } catch (ex: JSONException) {
                    throw RuntimeException(ex)
                }
            }
        })
        reloadRecycleView()
    }

    private fun toggleBottom() {
        if (shouldShowAnim && bottomLayout!!.getVisibility() == View.VISIBLE) {
            shouldShowAnim = false
            bottomLayout!!.startAnimation(outAnim)
            bottomLayout!!.postDelayed(object : Runnable {
                override fun run() {
                    bottomLayout!!.setVisibility(View.GONE)
                    shouldShowAnim = true
                }
            }, outAnim!!.getDuration())
        } else if (shouldShowAnim && bottomLayout!!.getVisibility() == View.GONE) {
            shouldShowAnim = false
            bottomLayout!!.setVisibility(View.VISIBLE)
            bottomLayout!!.startAnimation(inAnim)
            bottomLayout!!.postDelayed(object : Runnable {
                override fun run() {
                    shouldShowAnim = true
                }
            }, inAnim!!.getDuration())
        }
    }

    fun reloadRecycleView() {
        if (workAdapter != null) {
            workAdapter!!.setLoading(true)
        }
        if (type == TYPE_ALL_WORK) {
            setTitle(getString(R.string.app_name))
            doGetWorks(page, apisCallback)
        } else if (type == TYPE_SELF_LISTENING) {
            setTitle(R.string.listening)
            doGetReview(Api.FILTER_LISTENING, page, apisCallback)
        } else if (type == TYPE_SELF_LISTENED) {
            setTitle(R.string.listened)
            doGetReview(Api.FILTER_LISTENED, page, apisCallback)
        } else if (type == TYPE_SELF_MARKED) {
            setTitle(R.string.marked)
            doGetReview(Api.FILTER_MARKED, page, apisCallback)
        } else if (type == TYPE_SELF_REPLAY) {
            setTitle(R.string.replay)
            doGetReview(Api.FILTER_REPLAY, page, apisCallback)
        } else if (type == TYPE_SELF_POSTPONED) {
            setTitle(R.string.postponed)
            doGetReview(Api.FILTER_POSTPONED, page, apisCallback)
        } else if (type == TYPE_TAG_WORK) {
            setTitle(tagStr)
            doGetWorksByTag(page, tagId, apisCallback)
        } else if (type == TYPE_VA_WORK) {
            setTitle(vaName)
            doGetWorkByVa(page, vaId, apisCallback)
        } else if (type == TYPE_CIRCLES_WORK) {
            setTitle(circlesName)
            doGetWorkByCircles(page, circlesId, apisCallback)
        } else if (type == TYPE_LOCAL_WORK) {
            setTitle(String.format("%s", if (App.getInstance().isSaveExternal()) "外部公共目录" else "内部私有目录"))
            try {
                LocalFileCache.getInstance().readLocalWorks(this, apisCallback)
            } catch (e: JSONException) {
                e.printStackTrace()
                alertException(e)
            }
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onAlbumChange(rjNumber: Long) {
        if (rjNumber != 0L && bottomLayout!!.getVisibility() == View.GONE) {
            toggleBottom()
        }
        Glide.with(this).load(minCoverImageUrl(rjNumber)).apply(App.getInstance().getRadius5Pic()).into(ivCover!!)
    }

    override fun onAudioChange(audio: JSONObject) {
        try {
            tvTitle!!.setText(audio.getString("title"))
            tvWorkTitle!!.setText(audio.getString("workTitle"))
        } catch (e: JSONException) {
            e.printStackTrace()
            alertException(e)
        }
    }

    override fun onStatusChange(status: Int) {
        if (status == 0) {
            ibStatus!!.setImageResource(R.drawable.ic_baseline_play_arrow_white_24)
        } else {
            ibStatus!!.setImageResource(R.drawable.ic_baseline_pause_white_24)
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        ctrlBinder = service as CtrlBinder
        ibStatus!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (ctrlBinder!!.getController().getPlaybackState() == null) {
                    ctrlBinder!!.getController().getTransportControls().play()
                    return
                }
                if (ctrlBinder!!.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                    ctrlBinder!!.getController().getTransportControls().pause()
                } else {
                    ctrlBinder!!.getController().getTransportControls().play()
                }
            }
        })
        bottomLayout!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                try {
                    if (ctrlBinder!!.getCurrentTitle().endsWith("mp4")) {
                        startActivity(Intent(this@WorksActivity, VideoPlayerActivity::class.java))
                    } else {
                        val intent = Intent(this@WorksActivity, AudioPlayerActivity::class.java)
                        val view = v.findViewById<View>(R.id.imageView)
                        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this@WorksActivity, view, "hero_bottom" // 这里的字符串必须匹配 transitionName
                        )
                        startActivity(intent, options.toBundle())
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                    alertException(e)
                }
            }
        })
        ctrlBinder!!.addMusicChangeListener(this)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 0, 0, "切换账号")

        val layoutMenu = menu.addSubMenu(0, 9, 9, R.string.works_layout)
        layoutMenu.setIcon(R.drawable.ic_baseline_view_column_24)
        layoutMenu.add(2, 10, 10, R.string.list_layout)
        layoutMenu.add(2, 11, 11, R.string.cover_layout)
        layoutMenu.add(2, 12, 12, R.string.detail_layout)
        layoutMenu.add(2, 13, 13, R.string.staggered)
        layoutMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)


        val sortMenu = menu.addSubMenu(0, 16, 16, R.string.sort)
        sortMenu.add(3, 17, 17, R.string.release_date)
        sortMenu.add(3, 18, 18, R.string.rj_number)
        sortMenu.add(3, 19, 19, R.string.prize)
        sortMenu.add(3, 20, 20, R.string.last_in_lib)

        menu.add(0, 22, 22, R.string.download_mission)
        menu.add(0, 24, 24, R.string.local_history)
        menu.add(0, 15, 99, R.string.more)

        val searchMenu = menu.add(0, 23, 23, R.string.search)

        searchMenu.setIcon(R.drawable.ic_baseline_search_24)
        searchMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getGroupId() == 2) {
            var layoutType = WorkAdapter.LAYOUT_SMALL_GRID
            if (item.getItemId() == 10) {
                layoutType = WorkAdapter.LAYOUT_LIST
            } else if (item.getItemId() == 12) {
                layoutType = WorkAdapter.LAYOUT_BIG_GRID
            } else if (item.getItemId() == 13) {
                layoutType = WorkAdapter.LAYOUT_STAGGERED
            }
            App.getInstance().setValue(App.CONFIG_LAYOUT_TYPE, layoutType.toLong())
            initLayout(layoutType)
            return super.onOptionsItemSelected(item)
        }

        if (item.getGroupId() == 3) {
            var update = false
            if (item.getItemId() == 17) {
                setOrder("release")
                update = true
            } else if (item.getItemId() == 18) {
                setOrder("id")
                update = true
            } else if (item.getItemId() == 19) {
                setOrder("price")
                update = true
            } else if (item.getItemId() == 20) {
                setOrder("create_date")
                update = true
            }
            if (update) {
                clearWork()
                reloadRecycleView()
            }
            return true
        }

        if (item.getItemId() == 0) {
            App.getInstance().setValue(App.CONFIG_UPDATE_TIME, 0)
            startActivity(Intent(this, UserSwitchActivity::class.java))
        } else if (item.getItemId() == 1) {
        } else if (item.getItemId() == 15) {
            startActivity(Intent(this, MoreActivity::class.java))
        } else if (item.getItemId() == 21) {
            startActivityForResult(Intent(this, VasActivity::class.java), VA_SELECT_RESULT)
        } else if (item.getItemId() == 22) {
            startActivity(Intent(this, DownLoadMissionActivity::class.java))
        } else if (item.getItemId() == 23) {
            startActivity(Intent(this, SearchActivity::class.java))
        } else if (item.getItemId() == 24) {
            startActivity(Intent(this, LastWatchActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TAG_SELECT_RESULT || requestCode == VA_SELECT_RESULT) {
            if (resultCode == RESULT_OK && data != null) {
                val resultType = data.getStringExtra("resultType")
                if (resultType == null) {
                    return
                }
                if (resultType == "va") {
                    val vaId: String = data.getStringExtra("id")!!
                    type = TYPE_VA_WORK
                    if (vaId != this.vaId) {
                        vaName = data.getStringExtra("name")
                        clearWork()
                        this.vaId = vaId
                    }
                } else if (resultType == "tag") {
                    val tagId = data.getIntExtra("id", -1)
                    type = TYPE_TAG_WORK
                    if (tagId != this.tagId) {
                        tagStr = data.getStringExtra("name")
                        clearWork()
                        this.tagId = tagId
                    }
                }
                reloadRecycleView()
            }
        } else if (requestCode == CIRCLES_SELECT_RESULT) {
            if (resultCode == RESULT_OK && data != null) {
                val resultType = data.getStringExtra("resultType")
                if (resultType == null) {
                    return
                }
                if (resultType == "circles") {
                    val circlesId = data.getLongExtra("id", -1)
                    if (this.circlesId != circlesId && circlesId != -1L) {
                        type = TYPE_CIRCLES_WORK
                        circlesName = data.getStringExtra("name")
                        clearWork()
                        this.circlesId = circlesId
                        reloadRecycleView()
                    }
                }
            }
        }
    }

    private fun initLayout(layoutType: Int) {
        var layoutManager: RecyclerView.LayoutManager? = null
        recyclerView!!.removeItemDecoration(itemDecoration!!)
        val col: Int
        if (layoutType == WorkAdapter.LAYOUT_LIST) {
            layoutManager = LinearLayoutManager(this@WorksActivity)
            col = 1
        } else if (layoutType == WorkAdapter.LAYOUT_SMALL_GRID) {
            col = max(getResources().getDisplayMetrics().widthPixels / 395, 3)
            layoutManager = GridLayoutManager(this@WorksActivity, col)
        } else if (layoutType == WorkAdapter.LAYOUT_BIG_GRID) {
            col = max(getResources().getDisplayMetrics().widthPixels / 395, 2)
            layoutManager = GridLayoutManager(this@WorksActivity, col)
        } else if (layoutType == WorkAdapter.LAYOUT_STAGGERED) {
            col = max(getResources().getDisplayMetrics().widthPixels / 395, 2)
            layoutManager = StaggeredGridLayoutManager(col, StaggeredGridLayoutManager.VERTICAL)
        } else {
            col = 1
        }
        if (layoutManager is GridLayoutManager) {
            layoutManager.setSpanSizeLookup(object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    if (position == works!!.size) {
                        return col
                    }
                    return 1
                }
            })
        }
        workAdapter = WorkAdapter(works, layoutType)
        workAdapter!!.setTagClickListener(this)
        workAdapter!!.setVaClickListener(vaClickListener)
        workAdapter!!.setCirclesClickListener(circlesClickListener)
        workAdapter!!.setItemClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                val item = v.getTag() as JSONObject
                val intent = Intent(v.getContext(), WorkTreeActivity::class.java)
                intent.putExtra("work_json_str", item.toString())
                val heroView = v.findViewById<View>(R.id.ivCover)
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this@WorksActivity, heroView, "hero_image" // 这里的字符串必须匹配 transitionName
                )
                startActivity(intent, options.toBundle())
            }
        })
        workAdapter!!.setItemLongClickListener(object : OnLongClickListener {
            override fun onLongClick(v: View): Boolean {
                if (type != TYPE_LOCAL_WORK) {
                    return true
                }
                val listPopupWindow = ListPopupWindow(v.getContext())
                listPopupWindow.setModal(true)
                listPopupWindow.setAnchorView(v)
                val _str = getString(R.string.delete_cache)
                listPopupWindow.setAdapter(
                    ArrayAdapter<String?>(
                        v.getContext(),
                        android.R.layout.simple_list_item_1,
                        mutableListOf<String?>(_str)
                    )
                )
                listPopupWindow.setOnItemClickListener(object : OnItemClickListener {
                    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val item = v.getTag() as JSONObject
                        try {
                            LocalFileCache.getInstance().removeWork(item.getInt("id"))
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            alertException(e)
                        }

                        val index = works!!.indexOf(item)
                        if (index != -1) {
                            works!!.removeAt(index)
                            workAdapter!!.notifyItemRemoved(index)
                        }
                        listPopupWindow.dismiss()
                    }
                })
                listPopupWindow.show()
                return true
            }
        })
        recyclerView!!.setLayoutManager(layoutManager)
        recyclerView!!.setAdapter(workAdapter)
    }

    private fun clearWork() {
        page = 1
        if (workAdapter == null) return
        workAdapter!!.notifyItemRangeRemoved(0, works!!.size)
        workAdapter!!.notifyItemRangeChanged(0, works!!.size)
        works!!.clear()
    }

    override fun onDestroy() {
        ctrlBinder!!.removeMusicChangeListener(this)

        val playbackStateCompat = ctrlBinder!!.getController().getPlaybackState()
        if (playbackStateCompat == null) {
            stopService(Intent(this, AudioService::class.java))
        } else {
            val state = ctrlBinder!!.controller.playbackState.state
            if (state == PlaybackStateCompat.STATE_STOPPED || state == PlaybackStateCompat.STATE_PAUSED) {
                stopService(Intent(this, AudioService::class.java))
            }
        }
        unbindService(this)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val resultType: String = intent.getStringExtra("resultType")!!
        if ("va" == resultType) {
            val vaId: String = intent.getStringExtra("id")!!
            if (vaId != this.vaId || type != TYPE_VA_WORK) {
                vaName = intent.getStringExtra("name")
                clearWork()
                this.vaId = vaId
            }
            type = TYPE_VA_WORK
        } else if ("tag" == resultType) {
            val tagId = intent.getIntExtra("id", -1)
            if (tagId != this.tagId || type != TYPE_TAG_WORK) {
                tagStr = intent.getStringExtra("name")
                clearWork()
                this.tagId = tagId
            }
            type = TYPE_TAG_WORK
        } else {
            type = TYPE_ALL_WORK
            clearWork()
        }
        reloadRecycleView()
    }

    override fun onTagClick(jsonObject: JSONObject?) {
        jsonObject?.let {
            try {
                val tagId = it.getInt("id")
                Log.d(TAG, "onTagClick: " + tagId)
                if (tagId != this.tagId || type != TYPE_TAG_WORK) {
                    tagStr = it.getString("name")
                    clearWork()
                    this.tagId = tagId
                }
                type = TYPE_TAG_WORK
                reloadRecycleView()
            } catch (e: JSONException) {
                e.printStackTrace()
                alertException(e)
            }
        }

    }

    private val vaClickListener: TagClickListener<JSONObject?> = object : TagClickListener<JSONObject?> {
        override fun onTagClick(jsonObject: JSONObject?) {
            jsonObject?.let {
                try {
                    val vaId = it.getString("id")
                    if (vaId != this@WorksActivity.vaId || type != TYPE_VA_WORK) {
                        vaName = it.getString("name")
                        clearWork()
                        this@WorksActivity.vaId = vaId
                    }
                    type = TYPE_VA_WORK
                    reloadRecycleView()
                } catch (e: JSONException) {
                    e.printStackTrace()
                    alertException(e)
                }
            }

        }
    }

    private val circlesClickListener = TagClickListener { circlesName: String? ->
        //todo
        //http://localhost:8980/api/circles/
        //http://localhost:8980/api/circles/54978/works?order=release&sort=desc&page=1&seed=59
        val circlesId = App.getInstance().mapCirclesId(circlesName)
        if (circlesId != -1L) {
            clearWork()
            this@WorksActivity.circlesName = circlesName
            this@WorksActivity.circlesId = circlesId
            type = TYPE_CIRCLES_WORK
            reloadRecycleView()
        }
        Log.d(TAG, "onTagClick: " + circlesName)
    }

    private val apisCallback: JSONObjectCallback = object : JSONObjectCallback() {
        override fun onCompleted(e: Exception?, asyncHttpResponse: AsyncHttpResponse?, jsonObject: JSONObject?) {
            if (e != null) {
                e.printStackTrace()
                alertException(e)
                /**
                 * why?
                 * javax.net.ssl.SSLHandshakeException: Read error: ssl=0x798b43bc58: Failure in SSL library, usually a protocol error
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err: error:10000065:SSL routines:OPENSSL_internal:ATTEMPT_TO_REUSE_SESSION_IN_DIFFERENT_CONTEXT (external/boringssl/src/ssl/tls13_client.cc:385 0x78de42cc60:0x00000000)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.android.org.conscrypt.SSLUtils.toSSLHandshakeException(SSLUtils.java:363)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.android.org.conscrypt.ConscryptEngine.convertException(ConscryptEngine.java:1134)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.android.org.conscrypt.ConscryptEngine.unwrap(ConscryptEngine.java:919)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.android.org.conscrypt.ConscryptEngine.unwrap(ConscryptEngine.java:747)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.android.org.conscrypt.ConscryptEngine.unwrap(ConscryptEngine.java:712)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.android.org.conscrypt.Java8EngineWrapper.unwrap(Java8EngineWrapper.java:237)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.AsyncSSLSocketWrapper$6.onDataAvailable(AsyncSSLSocketWrapper.java:296)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.Util.emitAllData(Util.java:23)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.AsyncNetworkSocket.onReadable(AsyncNetworkSocket.java:160)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.AsyncServer.runLoop(AsyncServer.java:878)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.AsyncServer.run(AsyncServer.java:726)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.AsyncServer.access$800(AsyncServer.java:46)
                 * 2022-04-22 00:32:30.323 15775-15802/com.zinhao.kikoeru W/System.err:     at com.koushikdutta.async.AsyncServer$8.run(AsyncServer.java:680)
                 */
                return
            }
            if (asyncHttpResponse == null || asyncHttpResponse.code() != 200) {
                if (jsonObject != null && jsonObject.has("works")) {
                    Log.d(TAG, "onCompleted: load local cache!")
                } else {
                    Log.d(TAG, String.format("onCompleted:failed! "))
                    if (!isDestroyed()) {
                        ivCover!!.postDelayed(object : Runnable {
                            override fun run() {
                                reloadRecycleView()
                            }
                        }, 3000)
                    }
                    return
                }
            }
            try {
                val jsonArray = jsonObject!!.getJSONArray("works")
                totalCount = jsonObject.getJSONObject("pagination").getInt("totalCount")
                currentPage = page
                page = jsonObject.getJSONObject("pagination").getInt("currentPage") + 1

                if (jsonArray.length() != 0) {
                    page = min(page, totalCount / jsonArray.length() + 1)
                }
                runOnUiThread(object : Runnable {
                    @SuppressLint("DefaultLocale")
                    override fun run() {
                        setTitle(String.format("%s (%d)", currentTitle, totalCount))
                        for (i in 0..<jsonArray.length()) {
                            try {
                                works!!.add(jsonArray.getJSONObject(i))
                            } catch (jsonException: JSONException) {
                                jsonException.printStackTrace()
                                alertException(jsonException)
                            }
                        }
                        if (workAdapter == null) {
                            initLayout(
                                App.getInstance()
                                    .getValue(App.CONFIG_LAYOUT_TYPE, WorkAdapter.LAYOUT_STAGGERED.toLong()).toInt()
                            )
                            recyclerView!!.addOnScrollListener(scrollListener!!)
                        } else {
                            workAdapter!!.notifyItemRangeInserted(
                                max(0, works!!.size - jsonArray.length()),
                                jsonArray.length()
                            )
                            workAdapter!!.notifyItemRangeChanged(
                                max(0, works!!.size - jsonArray.length()),
                                jsonArray.length()
                            )
                            if (works!!.size == totalCount) {
                                workAdapter!!.setLoading(false)
                            }
                        }
                    }
                })
            } catch (jsonException: JSONException) {
                jsonException.printStackTrace()
                alertException(jsonException)
            }
        }
    }

    private val currentTitle: String?
        get() {
            if (type == TYPE_ALL_WORK) {
                return getString(R.string.app_name)
            } else if (type == TYPE_SELF_LISTENING) {
                return getString(R.string.listening)
            } else if (type == TYPE_SELF_LISTENED) {
                return getString(R.string.listened)
            } else if (type == TYPE_SELF_MARKED) {
                return getString(R.string.marked)
            } else if (type == TYPE_SELF_REPLAY) {
                return getString(R.string.replay)
            } else if (type == TYPE_SELF_POSTPONED) {
                return getString(R.string.postponed)
            } else if (type == TYPE_TAG_WORK) {
                return tagStr
            } else if (type == TYPE_VA_WORK) {
                return vaName
            } else if (type == TYPE_CIRCLES_WORK) {
                return circlesName
            } else if (type == TYPE_LOCAL_WORK) {
                return String.format(
                    "%s",
                    if (App.getInstance()
                            .isSaveExternal()
                    ) getString(R.string.extra_path) else getString(R.string.private_path)
                )
            }
            return "--"
        }

    override fun onBackPressed() {
        super.onBackPressed()
        try {
            App.getInstance().setValue(CONFIG_TYPE, type.toLong())
            App.getInstance().setValue(CONFIG_PAGE, currentPage.toLong())
            if (type == TYPE_TAG_WORK) {
                App.getInstance().setValue(CONFIG_PARAM_INT, tagId.toLong())
            } else if (type == TYPE_VA_WORK) {
                App.getInstance().setValue(CONFIG_PARAM_STR, vaId)
            }

            DownloadUtils.getInstance().close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "WorksActivity"
        private const val CONFIG_TYPE = "last_type"
        private const val CONFIG_PAGE = "last_page"
        private const val CONFIG_PARAM_INT = "last_param_int"
        private const val CONFIG_PARAM_STR = "last_param_str"
        private const val TYPE_ALL_WORK = 491
        private const val TYPE_SELF_LISTENING = 492
        private const val TYPE_SELF_LISTENED = 493
        private const val TYPE_SELF_MARKED = 494
        private const val TYPE_SELF_REPLAY = 495
        private const val TYPE_SELF_POSTPONED = 496
        private const val TYPE_TAG_WORK = 497
        private const val TYPE_LOCAL_WORK = 498
        private const val TYPE_VA_WORK = 499
        private const val TYPE_CIRCLES_WORK = 500

        private const val TAG_SELECT_RESULT = 14
        private const val VA_SELECT_RESULT = 15
        private const val CIRCLES_SELECT_RESULT = 16
    }
}