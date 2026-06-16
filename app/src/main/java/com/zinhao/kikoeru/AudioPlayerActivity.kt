package com.zinhao.kikoeru

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.PlaybackStateCompat
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.PaletteAsyncListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.Player
import com.koushikdutta.async.http.AsyncHttpClient.JSONObjectCallback
import com.koushikdutta.async.http.AsyncHttpResponse
import com.zinhao.kikoeru.Api.doGetWork
import com.zinhao.kikoeru.Api.formatGetUrl
import com.zinhao.kikoeru.AudioService.CtrlBinder
import com.zinhao.kikoeru.Lrc.LrcRow
import com.zinhao.kikoeru.databinding.ActivityPlayerBinding
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class AudioPlayerActivity : BaseActivity(), ServiceConnection, MusicChangeListener, LrcRowChangeListener,
    OnSeekBarChangeListener {
    private var ctrlBinder: CtrlBinder? = null
    private var imageView: ImageView? = null
    private lateinit var rootView: View
    private lateinit var tvTitle: TextView
    private var ibPrevious: ImageButton? = null
    private var ibPause: ImageButton? = null
    private var ibNext: ImageButton? = null
    private var ibSleep: ImageButton? = null
    private var ibLoop: ImageButton? = null
    private var recyclerView: RecyclerView? = null
    private var timeProgressView: TimeProgressView? = null
    private var needShowLrcWhenDestroy = false
    private var lrcAdapter: LrcAdapter? = null
    private lateinit var viewBinding: ActivityPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setSafeArea(viewBinding.root)
        imageView = viewBinding.ivCover
        imageView!!.setOnClickListener { doGetWork(ctrlBinder!!.currentAlbumId.toString(), 1, searchWorkCallback) }
        ibPrevious =viewBinding.ib1
        ibPause = viewBinding.ib2
        ibNext = viewBinding.ib3
        ibSleep = viewBinding.imageButton2
        ibLoop = viewBinding.ibLoop
        tvTitle = viewBinding.textView13
        setupSleepMenu()
        timeProgressView = viewBinding.timeView
        timeProgressView!!.setColor(ContextCompat.getColor(this, R.color.play_control_icon_color))
        ibPause!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (ctrlBinder == null) return
                if (ctrlBinder!!.getController() == null || ctrlBinder!!.getController()
                        .getTransportControls() == null
                ) return
                val playbackStateCompat = ctrlBinder!!.controller.getPlaybackState()
                if (playbackStateCompat != null && playbackStateCompat.getState() == PlaybackStateCompat.STATE_PLAYING) {
                    ctrlBinder!!.controller.getTransportControls().pause()
                } else {
                    ctrlBinder!!.getController().getTransportControls().play()
                }
            }
        })
        ibPrevious!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (ctrlBinder == null) return
                if (ctrlBinder!!.getController() == null || ctrlBinder!!.getController()
                        .getTransportControls() == null
                ) return
                ctrlBinder!!.getController().getTransportControls().skipToPrevious()
            }
        })
        ibNext!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (ctrlBinder == null) return
                if (ctrlBinder!!.getController() == null || ctrlBinder!!.getController()
                        .getTransportControls() == null
                ) return
                ctrlBinder!!.getController().getTransportControls().skipToNext()
            }
        })
        ibLoop!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (ctrlBinder == null) return
                if (ctrlBinder!!.getController() == null || ctrlBinder!!.getController()
                        .getTransportControls() == null
                ) return
                if (ctrlBinder!!.reapMode == Player.REPEAT_MODE_ONE) {
                    ctrlBinder!!.setReapAll()
                    Toast.makeText(this@AudioPlayerActivity, getString(R.string.all_repeat), Toast.LENGTH_SHORT).show()
                } else if (ctrlBinder!!.reapMode == Player.REPEAT_MODE_ALL) {
                    ctrlBinder!!.setReapOff()
                    Toast.makeText(this@AudioPlayerActivity, getString(R.string.repeat_off), Toast.LENGTH_SHORT).show()
                } else if (ctrlBinder!!.reapMode == Player.REPEAT_MODE_OFF) {
                    ctrlBinder!!.setReapOne()
                    Toast.makeText(this@AudioPlayerActivity, getString(R.string.repeat_one), Toast.LENGTH_SHORT).show()
                }
                updateLoopIcon()
            }
        })
        timeProgressView!!.setOnSeekBarChangeListener(this)

        bindService(Intent(this, AudioService::class.java), this, BIND_AUTO_CREATE)
    }

    private var sleepMenu: ListPopupWindow? = null
    private fun setupSleepMenu() {
        sleepMenu = ListPopupWindow(this)
        sleepMenu!!.setAdapter(
            ArrayAdapter<String?>(
                this, android.R.layout.simple_list_item_1,
                listOf<String?>(
                    "30 minutes", "1 hours", "90 minutes", "2 hours", "150 minutes", "3 hours",
                )
            )
        )
        sleepMenu!!.setModal(true)
        sleepMenu!!.setAnchorView( ibSleep )
        sleepMenu?.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 130f, resources.displayMetrics).toInt()
        sleepMenu!!.setOnItemClickListener(AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            sleepMenu!!.dismiss()
            val minutes= 30*(position+1)
            if(BuildConfig.DEBUG){
                ctrlBinder?.stopAfterMinutes(minutes/30)
            }else{
                ctrlBinder?.stopAfterMinutes(minutes)
            }
            Toast.makeText(this, "will stop after ${minutes} minutes", Toast.LENGTH_LONG).show()
        })
    }

    var lastScrollIDLE = 0L

    private fun setupLrc() {
        recyclerView = viewBinding.mainRecycler
        ctrlBinder?.lrc?.let {
            lrcAdapter = LrcAdapter(it)
            lrcAdapter?.setOnToHereClickListener { v->
                v?.tag?.let { tag->
                    if(tag is LrcRow){
                        ctrlBinder?.controller?.transportControls?.seekTo(tag.time)
                        lrcAdapter?.notifyDataSetChanged()
                    }
                }
            }
            recyclerView!!.layoutManager = LinearLayoutManager(this)
            recyclerView!!.adapter = lrcAdapter
            scrollToLrcPosition()

            recyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if(newState == RecyclerView.SCROLL_STATE_IDLE) {
                        lastScrollIDLE = System.currentTimeMillis()
                    }
                }
            })
        }
    }

    private val searchWorkCallback: JSONObjectCallback = object : JSONObjectCallback() {
        override fun onCompleted(e: Exception?, asyncHttpResponse: AsyncHttpResponse, jsonObject: JSONObject) {
            if (e != null) {
                alertException(e)
                return
            }
            if (asyncHttpResponse.code() == 200) {
                try {
                    val totalCount = jsonObject.getJSONObject("pagination").getInt("totalCount")
                    if (totalCount < 1) return
                    val works = jsonObject.getJSONArray("works")
                    if (works.length() != 0) {
                        val item = works.getJSONObject(0)
                        val intent = Intent(this@AudioPlayerActivity, WorkTreeActivity::class.java)
                        intent.putExtra("work_json_str", item.toString())
                        this@AudioPlayerActivity.startActivity(intent, null)
                    }
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace()
                }
            }
        }
    }

    private fun updateLoopIcon() {
        if (ctrlBinder == null) return
        if (ctrlBinder!!.getReapMode() == Player.REPEAT_MODE_ONE) {
            ibLoop!!.setImageResource(R.drawable.ic_baseline_flip_camera_android_24)
        } else if (ctrlBinder!!.getReapMode() == Player.REPEAT_MODE_ALL) {
            ibLoop!!.setImageResource(R.drawable.ic_baseline_loop_24)
        } else if (ctrlBinder!!.getReapMode() == Player.REPEAT_MODE_OFF) {
            ibLoop!!.setImageResource(R.drawable.ic_baseline_close_24)
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        ctrlBinder = service as CtrlBinder?
        ctrlBinder!!.addMusicChangeListener(this)
        ctrlBinder!!.addLrcChangeListener(this)
        if (ctrlBinder!!.isLrcWindowShow()) {
            needShowLrcWhenDestroy = true
            ctrlBinder!!.hideLrcFloatWindow()
        }
        updateLoopIcon()
        ibSleep!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                sleepMenu?.show()
            }
        })
        timeProgressView!!.setMax(ctrlBinder!!.getExoPlayer().getDuration().toInt())
        if (ctrlBinder != null && ctrlBinder!!.getExoPlayer() != null) {
            val current = ctrlBinder!!.getExoPlayer().getCurrentPosition()
            val buffer = ctrlBinder!!.getExoPlayer().getBufferedPosition()
            timeProgressView!!.setProgress(current.toInt(), buffer.toInt())
        }
        updateSeek()
        if(ctrlBinder?.lrc== Lrc.NONE){
            runOnUiThread { imageView?.alpha = 1f }
        }else{
            runOnUiThread { imageView?.alpha = 0.5f }
        }
        setupLrc()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onSeekChange(lrcRow: LrcRow) {
        runOnUiThread {
            lrcAdapter?.update()
            scrollToLrcPosition()
        }
    }

    private fun scrollToLrcPosition(){
        if(System.currentTimeMillis() - lastScrollIDLE > 3000) {
            val manger = recyclerView?.layoutManager
            if(manger is LinearLayoutManager) {
                val index = manger.findLastCompletelyVisibleItemPosition()
                ctrlBinder?.let {
                    if(it.lrc.currentIndex != index) {
                        manger.scrollToPosition(it.lrc.currentIndex)
                    }
                }
            }
        }
    }

    override fun onLrcChange(lrc: Lrc?) {
        if(ctrlBinder?.lrc== Lrc.NONE){
            runOnUiThread { imageView?.alpha = 1f }
        }else{
            runOnUiThread { imageView?.alpha = 0.5f }
        }
        runOnUiThread {
            setupLrc()
        }
    }

    override fun onAlbumChange(rjNumber: Long) {
        Glide.with(this).asBitmap().load(formatGetUrl(String.format(Locale.US, "/api/cover/%d", rjNumber), true))
            .apply(App.getInstance().radius15Pic).into(object : CustomViewTarget<ImageView?, Bitmap?>(imageView!!) {
                override fun onLoadFailed(drawable: Drawable?) {
                }

                override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap?>?) {
                    imageView!!.setImageBitmap(bitmap)
                    Palette.from(bitmap).generate(object : PaletteAsyncListener {
                        override fun onGenerated(palette: Palette?) {
                            if (palette != null) {
                                val mainColor = palette.getDarkMutedColor(
                                    ActivityCompat.getColor(
                                        this@AudioPlayerActivity,
                                        R.color.main_color
                                    )
                                )
                                rootView.setBackgroundColor(mainColor)
                                window.setNavigationBarColor(mainColor)
                                window.setStatusBarColor(mainColor)
                                val actionBar = getSupportActionBar()
                                actionBar?.setBackgroundDrawable(mainColor.toDrawable())
                            }
                        }
                    })
                }

                override fun onResourceCleared(drawable: Drawable?) {
                }
            })
    }

    override fun onAudioChange(audio: JSONObject?) {
        runOnUiThread(object : Runnable {
            override fun run() {
                try {
                    timeProgressView!!.setMax(ctrlBinder!!.getExoPlayer().getDuration().toInt())
                    setTitle(ctrlBinder!!.getCurrentTitle())
                } catch (e: JSONException) {
                    e.printStackTrace()
                    alertException(e)
                }
            }
        })
    }

    override fun setTitle(title: CharSequence?) {
        tvTitle.text = title
    }

    override fun onStatusChange(status: Int) {
        timeProgressView!!.setMax(ctrlBinder!!.getExoPlayer().getDuration().toInt())
        if (status == 0) {
            ibPause!!.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        } else {
            ibPause!!.setImageResource(R.drawable.ic_baseline_pause_24)
        }
    }

    private fun updateSeek() {
        timeProgressView!!.postDelayed(object : Runnable {
            override fun run() {
                if (ctrlBinder != null && ctrlBinder!!.getExoPlayer() != null && ctrlBinder!!.getExoPlayer()
                        .isPlaying()
                ) {
                    val current = ctrlBinder!!.getExoPlayer().getCurrentPosition()
                    val buffer = ctrlBinder!!.getExoPlayer().getBufferedPosition()
                    timeProgressView!!.setProgress(current.toInt(), buffer.toInt())
                }
                if (!isDestroyed()) updateSeek()
            }
        }, 500)
    }

    override fun onDestroy() {
        if (ctrlBinder != null) {
            ctrlBinder!!.removeLrcChangeListener(this)
            ctrlBinder!!.removeMusicChangeListener(this)
            unbindService(this)
            if (!ctrlBinder!!.isLrcWindowShow() && needShowLrcWhenDestroy) ctrlBinder!!.showLrcFloatWindow()
        }
        super.onDestroy()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            ctrlBinder!!.getController().getTransportControls().seekTo(progress.toLong())
        }
    }
    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }
}
