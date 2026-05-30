package com.zinhao.kikoeru

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.PlaybackStateCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class AudioPlayerActivity : BaseActivity(), ServiceConnection, MusicChangeListener, LrcRowChangeListener,
    OnSeekBarChangeListener {
    private var ctrlBinder: CtrlBinder? = null
    private var imageView: ImageView? = null
    private lateinit var tvTitle: TextView
    private var ibPrevious: ImageButton? = null
    private var ibPause: ImageButton? = null
    private var ibNext: ImageButton? = null
    private var ibLrc: ImageButton? = null
    private var ibLoop: ImageButton? = null
    private var recyclerView: RecyclerView? = null
    private var timeProgressView: TimeProgressView? = null
    private var needShowLrcWhenDestroy = false
    private var lrcAdapter: LrcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        imageView = findViewById<ImageView>(R.id.ivCover)
        imageView!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                doGetWork(ctrlBinder!!.currentAlbumId.toString(), 1, searchWorkCallback)
            }
        })
        ibPrevious = findViewById<ImageButton>(R.id.ib1)
        ibPause = findViewById<ImageButton>(R.id.ib2)
        ibNext = findViewById<ImageButton>(R.id.ib3)
        ibLrc = findViewById<ImageButton>(R.id.imageButton2)
        ibLoop = findViewById<ImageButton>(R.id.ibLoop)
        tvTitle = findViewById(R.id.textView13)

        timeProgressView = findViewById<TimeProgressView>(R.id.time_view)
        timeProgressView!!.setColor(ContextCompat.getColor(this, R.color.play_control_icon_color))
        ibPause!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (ctrlBinder == null) return
                if (ctrlBinder!!.getController() == null || ctrlBinder!!.getController()
                        .getTransportControls() == null
                ) return
                val playbackStateCompat = ctrlBinder!!.getController().getPlaybackState()
                if (playbackStateCompat != null && playbackStateCompat.getState() == PlaybackStateCompat.STATE_PLAYING) {
                    ctrlBinder!!.getController().getTransportControls().pause()
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

    var lastScrollIDLE = 0L

    private fun setupLrc() {
        recyclerView = findViewById(R.id.mainRecycler)
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
        ibLrc!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                if (ctrlBinder!!.isLrcWindowShow()) {
                    ctrlBinder!!.hideLrcFloatWindow()
                } else {
                    ctrlBinder!!.showLrcFloatWindow()
                }
            }
        })
        timeProgressView!!.setMax(ctrlBinder!!.getExoPlayer().getDuration().toInt())
        if (ctrlBinder != null && ctrlBinder!!.getExoPlayer() != null) {
            val current = ctrlBinder!!.getExoPlayer().getCurrentPosition()
            val buffer = ctrlBinder!!.getExoPlayer().getBufferedPosition()
            timeProgressView!!.setProgress(current.toInt(), buffer.toInt())
        }
        updateSeek()
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
        if(System.currentTimeMillis() - lastScrollIDLE > 5000) {
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
        runOnUiThread {
            setupLrc()
        }
    }

    override fun onAlbumChange(rjNumber: Int) {
        Glide.with(this).asBitmap().load(formatGetUrl(String.format(Locale.US, "/api/cover/%d", rjNumber), true))
            .apply(App.getInstance().radius15Pic).into(object : CustomViewTarget<ImageView?, Bitmap?>(imageView!!) {
                override fun onLoadFailed(drawable: Drawable?) {
                }

                override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap?>?) {
                    imageView!!.setImageBitmap(bitmap)
                    Palette.from(bitmap).generate(object : PaletteAsyncListener {
                        override fun onGenerated(palette: Palette?) {
                            val bg = imageView!!.getParent() as View?
                            if (bg != null && palette != null) {
                                val mainColor = palette.getDarkMutedColor(
                                    ActivityCompat.getColor(
                                        this@AudioPlayerActivity,
                                        R.color.main_color
                                    )
                                )
                                bg.setBackgroundColor(mainColor)
                                window.setNavigationBarColor(mainColor)
                                window.setStatusBarColor(mainColor)
                                val actionBar = getSupportActionBar()
                                if (actionBar != null) actionBar.setBackgroundDrawable(ColorDrawable(mainColor))
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val subMenu = menu.addSubMenu(0, 0, 0, R.string.stop_delay)
        subMenu.add(1, 1, 1, "30 minutes")
        subMenu.add(1, 2, 2, "60 minutes")
        subMenu.add(1, 3, 3, "90 minutes")
        subMenu.add(1, 4, 4, "120 minutes")
        subMenu.add(1, 5, 5, "240 minutes")
        subMenu.getItem().setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
            if (item.getItemId() == 1) {
                ctrlBinder!!.stopAfterMinutes(30)
            } else if (item.getItemId() == 2) {
                ctrlBinder!!.stopAfterMinutes(60)
            } else if (item.getItemId() == 3) {
                ctrlBinder!!.stopAfterMinutes(90)
            } else if (item.getItemId() == 4) {
                ctrlBinder!!.stopAfterMinutes(120)
            } else if (item.getItemId() == 5) {
                ctrlBinder!!.stopAfterMinutes(240)
            }
        } catch (e: Exception) {
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
    }
}
