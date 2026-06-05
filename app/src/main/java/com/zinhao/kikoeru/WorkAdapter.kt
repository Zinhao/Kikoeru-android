package com.zinhao.kikoeru

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zinhao.kikoeru.Api.fullCoverImageUrl
import com.zinhao.kikoeru.TagsView.TagClickListener
import com.zinhao.kikoeru.TagsView.TextGet
import com.zinhao.kikoeru.model.toWork
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class WorkAdapter(
    private val datas: MutableList<JSONObject>,
    private val layoutType: Int = LAYOUT_SMALL_GRID
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val textGet: TextGet<JSONObject?>?
    private var tagClickListener: TagClickListener<*>? = null
    private var vaClickListener: TagClickListener<*>? = null
    private var circlesClickListener: TagClickListener<*>? = null
    private var itemClickListener: View.OnClickListener? = null
    private var itemLongClickListener: OnLongClickListener? = null

    private var isLoading = false // 是否正在加载
    fun isLoading(): Boolean {
        return isLoading
    }
    // 辅助方法：显示/隐藏加载动画
    fun setLoading(loading: Boolean) {
        if (this.isLoading != loading) {
            this.isLoading = loading
            if (loading) {
                notifyItemInserted(datas.size)
            } else {
                notifyItemRemoved(datas.size)
            }
        }
    }

    fun setTagClickListener(tagClickListener: TagClickListener<*>?) {
        this.tagClickListener = tagClickListener
    }

    fun setItemClickListener(itemClickListener: View.OnClickListener?) {
        this.itemClickListener = itemClickListener
    }

    fun setCirclesClickListener(circlesClickListener: TagClickListener<*>?) {
        this.circlesClickListener = circlesClickListener
    }

    fun setItemLongClickListener(itemLongClickListener: OnLongClickListener?) {
        this.itemLongClickListener = itemLongClickListener
    }

    fun setVaClickListener(vaClickListener: TagClickListener<*>?) {
        this.vaClickListener = vaClickListener
    }

    init {
        textGet = object : TextGet<JSONObject?> {
            override fun onGetText(t: JSONObject?): String {
                try {
                    return t?.optString("name")?:""
                } catch (e: JSONException) {
                }
                return ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == TYPE_LOADING) {
            val view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false)
            return LoadingViewHolder(view)
        }
        if (layoutType == LAYOUT_LIST) {
            val v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_1, parent, false)
            return SimpleViewHolder(v)
        } else if (layoutType == LAYOUT_BIG_GRID) {
            val v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_2, parent, false)
            return GirdViewHolder(v)
        } else if (layoutType == LAYOUT_STAGGERED) {
            val v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_2, parent, false)
            return GirdViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_3, parent, false)
            return SmallGirdViewHolder(v)
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position == datas.size) {
            return
        }
        val item = datas.get(position)
        holder.itemView.setTag(item)
        holder.itemView.setOnClickListener(itemClickListener)
        holder.itemView.setOnLongClickListener(itemLongClickListener)
        if (holder is SimpleViewHolder) {
            try {
                holder.tvTitle.setText(item.getString("title"))
                holder.tvComArt.setTags(App.getVasList(item), TagsView.JSON_TEXT_GET.setKey("name"))
                holder.tvComArt.setTagClickListener(vaClickListener)
                holder.tvTags.setTags(App.getTagsList(item), textGet)
                holder.tvCircles.setTags(mutableListOf<String?>(item.getString("name")), TagsView.STRING_TEXT_GET)
                holder.tvCircles.setTagClickListener(circlesClickListener)
                holder.tvTags.setTagClickListener(tagClickListener)
                Glide.with(holder.itemView.getContext()).load(
                    App.getInstance().currentUser().getHost() + String.format(
                        "/api/cover/%d?type=sam&token=%s",
                        item.getInt("id"),
                        Api.token
                    )
                )
                    .apply(App.getInstance().getRadius15Pic()).into(holder.ivCover)
            } catch (e: JSONException) {
                e.printStackTrace()
                App.getInstance().alertException(e)
            }
        }
        if (holder is GirdViewHolder) {
            val girdHolder = holder
            try {
                Glide.with(holder.itemView.getContext()).load(fullCoverImageUrl(item.optInt("id").toLong()))
                    .apply(App.getInstance().getRadius15Pic()).into(girdHolder.ivCover)
                girdHolder.tvTitle.setText(item.getString("title"))
                girdHolder.tvArt.setTags(App.getVasList(item), TagsView.JSON_TEXT_GET.setKey("name"))
                girdHolder.tvArt.setTagClickListener(vaClickListener)
                girdHolder.tvTags.setTags(App.getTagsList(item), textGet)
                girdHolder.tvCircles.setTags(mutableListOf<String?>(item.getString("name")), TagsView.STRING_TEXT_GET)
                girdHolder.tvCircles.setTagClickListener(circlesClickListener)
                girdHolder.tvTags.setTagClickListener(tagClickListener)
                girdHolder.tvRjNumber.setText(String.format("RJ%d", item.getInt("id")))

                val dateStr = item.optString("release")
                if (dateStr.isEmpty()) {
                    girdHolder.tvDate.setVisibility(View.GONE)
                } else {
                    girdHolder.tvDate.setVisibility(View.VISIBLE)
                    girdHolder.tvDate.setText(dateStr)
                }

                girdHolder.tvPrice.setText(String.format("%d 日元", item.getInt("price")))
                girdHolder.tvSaleCount.setText(String.format("售出：%d", item.getInt("dl_count")))
                if (item.has(JSONConst.Work.HOST)) {
                    girdHolder.tvHost.setVisibility(View.VISIBLE)
                    girdHolder.tvHost.setText(item.getString(JSONConst.Work.HOST))
                } else {
                    girdHolder.tvHost.setVisibility(View.INVISIBLE)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                App.getInstance().alertException(e)
            }
        }

        if (holder is SmallGirdViewHolder) {
            val girdHolder = holder
            try {
                Glide.with(holder.itemView.getContext()).load(fullCoverImageUrl(item.optInt("id").toLong()))
                    .apply(App.getInstance().getRadius5Pic()).into(girdHolder.ivCover)
                girdHolder.tvRjNumber.setText(String.format("RJ%d", item.getInt("id")))
                girdHolder.tvDate.setText(item.getString("release"))
                if (item.has(JSONConst.Work.HOST)) {
                    girdHolder.tvHost.setVisibility(View.VISIBLE)
                    girdHolder.tvHost.setText(item.getString(JSONConst.Work.HOST))
                } else {
                    girdHolder.tvHost.setVisibility(View.INVISIBLE)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                App.getInstance().alertException(e)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        // 如果位置是最后一位且处于加载状态，返回加载布局类型
        if (position == datas.size) {
            return TYPE_LOADING
        }
        return TYPE_ITEM
    }

    override fun getItemCount(): Int {
        return datas.size + (if(isLoading) 1 else 0)
    }

    class SimpleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView
        val tvTitle: TextView
        val tvComArt: TagsView<JSONArray?>
        val tvTags: TagsView<JSONArray?>
        val tvCircles: TagsView<MutableList<String?>?>


        init {
            ivCover = itemView.findViewById<ImageView>(R.id.ivCover)
            tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
            tvComArt = itemView.findViewById<TagsView<JSONArray?>>(R.id.tvComArt)
            tvTags = itemView.findViewById<TagsView<JSONArray?>>(R.id.tvTags)
            tvCircles = itemView.findViewById<TagsView<MutableList<String?>?>>(R.id.tvCircles)
        }
    }

    class GirdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView
        val tvTitle: TextView
        val tvArt: TagsView<JSONArray?>
        val tvTags: TagsView<JSONArray?>
        val tvCircles: TagsView<MutableList<String?>?>
        val tvRjNumber: TextView
        val tvDate: TextView
        val tvPrice: TextView
        val tvSaleCount: TextView
        val tvHost: TextView

        init {
            ivCover = itemView.findViewById<ImageView>(R.id.ivCover)
            tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
            tvArt = itemView.findViewById<TagsView<JSONArray?>>(R.id.tvArt)
            tvTags = itemView.findViewById<TagsView<JSONArray?>>(R.id.tvTags)
            tvRjNumber = itemView.findViewById<TextView>(R.id.tvRjNumber)
            tvDate = itemView.findViewById<TextView>(R.id.tvDate)
            tvPrice = itemView.findViewById<TextView>(R.id.tvPrice)
            tvSaleCount = itemView.findViewById<TextView>(R.id.tvSaleCount)
            tvHost = itemView.findViewById<TextView>(R.id.tvHost)
            tvCircles = itemView.findViewById<TagsView<MutableList<String?>?>>(R.id.tvCircles)
        }
    }

    class SmallGirdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
       val ivCover: ImageView
       val tvRjNumber: TextView
       val tvDate: TextView
       val tvHost: TextView

        init {
            ivCover = itemView.findViewById<ImageView>(R.id.ivCover)
            tvRjNumber = itemView.findViewById<TextView>(R.id.tvRjNumber)
            tvDate = itemView.findViewById<TextView>(R.id.tvDate)
            tvHost = itemView.findViewById<TextView>(R.id.tvHost)
        }
    }

    internal class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    companion object {
        private const val TAG = "WorkAdapter"
        const val LAYOUT_LIST: Int = 846
        const val LAYOUT_SMALL_GRID: Int = 847
        const val LAYOUT_BIG_GRID: Int = 848
        const val LAYOUT_STAGGERED: Int = 849

        private const val TYPE_ITEM = 0
        private const val TYPE_LOADING = 1
    }
}
