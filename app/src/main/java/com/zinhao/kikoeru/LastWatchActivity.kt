package com.zinhao.kikoeru

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.koushikdutta.async.http.AsyncHttpClient
import com.koushikdutta.async.http.AsyncHttpResponse
import com.zinhao.kikoeru.Api.fullCoverImageUrl
import com.zinhao.kikoeru.databinding.ActivityLocalHistoryBinding
import com.zinhao.kikoeru.db.LocalWorkHistory
import org.json.JSONException
import org.json.JSONObject

class LastWatchActivity : BaseActivity() {
    private var viewBinding: ActivityLocalHistoryBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityLocalHistoryBinding.inflate(getLayoutInflater())
        setContentView(viewBinding!!.getRoot())
        setSafeArea(getWindow().getDecorView())
        setTitle(R.string.local_history)

        val app = application as App
        val localWorkHistoryList = app.getLocalWorkHistoryList()
        localWorkHistoryList.sortBy { -it.position }
        viewBinding!!.mainRecycler.setAdapter(object : SuperRecyclerAdapter<LocalWorkHistory?>(localWorkHistoryList) {
            override fun bindData(holder: SuperVHolder, position: Int) {
                val item = localWorkHistoryList.get(position)

                val coverUrl = fullCoverImageUrl(item.rjNumber)
                holder.setText(item.title,R.id.tvTitle)
                holder.setText("RJ${item.rjNumber}",R.id.tvRJ)
                holder.setText(DateFormat.format("yyyy-MM-dd HH:mm:ss",item.position).toString(),R.id.tvDate)
                val ivCover = holder.getView(R.id.ivCover)
                if(ivCover is ImageView) {
                    Glide.with(holder.itemView.context).load(coverUrl)
                        .apply(App.getInstance().radius15Pic)
                        .into(ivCover)
                }
                holder.itemView.setOnClickListener { Api.doGetWork(item.rjNumber.toString(), 1, searchWorkCallback) }
            }

            override fun setLayout(viewType: Int): Int {
                return R.layout.item_recent_work
            }
        })


//        val col = max(getResources().getDisplayMetrics().widthPixels / 395, 3)
//        val layoutManager = GridLayoutManager(this@LastWatchActivity, col)
//        viewBinding!!.mainRecycler.setLayoutManager(layoutManager)
        viewBinding!!.mainRecycler.layoutManager = LinearLayoutManager(this)
    }

    private val searchWorkCallback: AsyncHttpClient.JSONObjectCallback = object : AsyncHttpClient.JSONObjectCallback() {
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
                        val intent = Intent(this@LastWatchActivity, WorkTreeActivity::class.java)
                        intent.putExtra("work_json_str", item.toString())
                        this@LastWatchActivity.startActivity(intent, null)
                    }
                } catch (jsonException: JSONException) {
                    jsonException.printStackTrace()
                }
            }
        }
    }
}
