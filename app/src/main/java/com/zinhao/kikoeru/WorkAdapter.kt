package com.zinhao.kikoeru

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zinhao.kikoeru.Api.fullCoverImageUrl
import com.zinhao.kikoeru.TagsView.TagClickListener
import com.zinhao.kikoeru.TagsView.TextGet
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.absoluteValue


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

    var animDuration = 500L
    var isScrollingDown: Boolean = true
    private val showAnimation: Boolean = BuildConfig.DEBUG

    private val bottomAnimPool = mutableListOf<Animation?>()
    private val topAnimPool = mutableListOf<Animation?>()
    private var poolIndex = 0

    fun updateAnimArgs(dx: Int, dy: Int){
       animDuration = (400 - dy.absoluteValue).coerceAtLeast(100).toLong()
       isScrollingDown = dy >= 0
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        if (!showAnimation) return
        if(holder.bindingAdapterPosition >= datas.size) {return}

        val pool = if (isScrollingDown) bottomAnimPool else topAnimPool
        val animRes = if (isScrollingDown) R.anim.from_bottom_slide_in else R.anim.from_top_slide_in

        val anim: Animation?
        if (pool.size < 31) {
            anim = AnimationUtils.loadAnimation(holder.itemView.context, animRes)
            pool.add(anim)
        } else {
            anim = pool[poolIndex]
            anim?.reset() // 关键：重置动画状态，否则不会再次播放
            poolIndex = (poolIndex + 1) % 30
        }
        anim?.duration = animDuration
        holder.itemView.startAnimation(anim)
    }

    fun submitList(newList: List<JSONObject>) {
        val oldSize = datas.size
        datas.clear()
        datas.addAll(newList)

        if (oldSize == 0) {
            // 首次加载
            notifyItemRangeInserted(0, datas.size)
        } else if (newList.size > oldSize) {
            // 有新增数据，只刷新新增部分
            val addedCount = newList.size - oldSize
            notifyItemRangeInserted(oldSize, addedCount)
        } else {
            // 数据变少（比如筛选），全量刷新
            notifyDataSetChanged()
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
                    App.getInstance().alertException(e)
                }
                return ""
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (layoutType == LAYOUT_LIST) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_work_1, parent, false)
            return SimpleViewHolder(v)
        } else if (layoutType == LAYOUT_BIG_GRID) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_work_2, parent, false)
            return GirdViewHolder(v)
        } else if (layoutType == LAYOUT_STAGGERED) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_work_2, parent, false)
            return GirdViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_work_3, parent, false)
            return SmallGirdViewHolder(v)
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
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
                ).apply(App.getInstance().getRadius15Pic()).into(holder.ivCover)
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
                    .apply(App.getInstance().noRadiusPic).into(girdHolder.ivCover)
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

    override fun getItemCount(): Int {
        return datas.size
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
       val ivCover: ImageView = itemView.findViewById<ImageView>(R.id.ivCover)
        val tvRjNumber: TextView = itemView.findViewById<TextView>(R.id.tvRjNumber)
        val tvDate: TextView = itemView.findViewById<TextView>(R.id.tvDate)
        val tvHost: TextView = itemView.findViewById<TextView>(R.id.tvHost)
    }

    companion object {
        private const val TAG = "WorkAdapter"
        const val LAYOUT_LIST: Int = 846
        const val LAYOUT_SMALL_GRID: Int = 847
        const val LAYOUT_BIG_GRID: Int = 848
        const val LAYOUT_STAGGERED: Int = 849
    }
}
