package com.zinhao.kikoeru.ui.adapter

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
import com.zinhao.kikoeru.App
import com.zinhao.kikoeru.R
import com.zinhao.kikoeru.TagsView
import com.zinhao.kikoeru.TagsView.TagClickListener
import com.zinhao.kikoeru.TagsView.TextGet
import com.zinhao.kikoeru.model.Circle
import com.zinhao.kikoeru.model.Tag
import com.zinhao.kikoeru.model.Va
import com.zinhao.kikoeru.model.Work
import org.json.JSONException

class WorksAdapter: ListAdapter<Work, RecyclerView.ViewHolder>(WorkDiffCallback)  {

    private var tagClickListener: TagClickListener<*>? = null
    private var vaClickListener: TagClickListener<*>? = null
    private var circlesClickListener: TagClickListener<*>? = null
    private var itemClickListener: View.OnClickListener? = null
    private var itemLongClickListener: OnLongClickListener? = null

    private val tagGet: TextGet<Tag> = TextGet { t -> t.name }
    private val circleGet: TextGet<Circle> = TextGet { t -> t.name }
    private val vaGet: TextGet<Va> = TextGet { t -> t.name }

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_2, parent, false)
        return GirdViewHolder(v)
    }

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setTag(item)
        holder.itemView.setOnClickListener(itemClickListener)
        holder.itemView.setOnLongClickListener(itemLongClickListener)
        if (holder is GirdViewHolder) {
            val girdHolder = holder
            try {
                Glide.with(holder.itemView.getContext()).load(fullCoverImageUrl(item.id.toLong()))
                    .apply(App.getInstance().getRadius15Pic()).into(girdHolder.ivCover)
                girdHolder.tvTitle.setText(item.title)
                girdHolder.tvArt.setTags(item.vas, vaGet)
                girdHolder.tvArt.setTagClickListener(vaClickListener)
                girdHolder.tvTags.setTags(item.tags, tagGet)
                girdHolder.tvCircles.setTags(listOf<Circle>(item.circle), circleGet)
                girdHolder.tvCircles.setTagClickListener(circlesClickListener)
                girdHolder.tvTags.setTagClickListener(tagClickListener)
                girdHolder.tvRjNumber.setText(String.format("RJ%d", item.id))

                val dateStr = item.release
                if (dateStr.isEmpty()) {
                    girdHolder.tvDate.setVisibility(View.GONE)
                } else {
                    girdHolder.tvDate.setVisibility(View.VISIBLE)
                    girdHolder.tvDate.setText(dateStr)
                }

                girdHolder.tvPrice.setText(String.format("%d 日元",item.price))
                girdHolder.tvSaleCount.setText(String.format("售出：%d", item.dl_count))

                if (item.host != null) {
                    girdHolder.tvHost.setVisibility(View.VISIBLE)
                    girdHolder.tvHost.setText(item.host)
                } else {
                    girdHolder.tvHost.setVisibility(View.INVISIBLE)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                App.getInstance().alertException(e)
            }
        }
    }

    class GirdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView
        val tvTitle: TextView
        val tvArt: TagsView<List<Va>>
        val tvTags: TagsView<List<Tag>>
        val tvCircles: TagsView<List<Circle>>
        val tvRjNumber: TextView
        val tvDate: TextView
        val tvPrice: TextView
        val tvSaleCount: TextView
        val tvHost: TextView

        init {
            ivCover = itemView.findViewById<ImageView>(R.id.ivCover)
            tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
            tvArt = itemView.findViewById(R.id.tvArt)
            tvTags = itemView.findViewById(R.id.tvTags)
            tvRjNumber = itemView.findViewById<TextView>(R.id.tvRjNumber)
            tvDate = itemView.findViewById<TextView>(R.id.tvDate)
            tvPrice = itemView.findViewById<TextView>(R.id.tvPrice)
            tvSaleCount = itemView.findViewById<TextView>(R.id.tvSaleCount)
            tvHost = itemView.findViewById<TextView>(R.id.tvHost)
            tvCircles = itemView.findViewById(R.id.tvCircles)
        }
    }

    companion object {
        private const val TAG = "WorkAdapter"
        const val LAYOUT_LIST: Int = 846
        const val LAYOUT_SMALL_GRID: Int = 847
        const val LAYOUT_BIG_GRID: Int = 848
        const val LAYOUT_STAGGERED: Int = 849

        private const val TYPE_ITEM = 0

        private val WorkDiffCallback =
            object : DiffUtil.ItemCallback<Work>() {

                override fun areItemsTheSame(
                    oldItem: Work,
                    newItem: Work
                ): Boolean = oldItem.id == newItem.id

                override fun areContentsTheSame(
                    oldItem: Work,
                    newItem: Work
                ): Boolean = oldItem == newItem
            }
    }
}
