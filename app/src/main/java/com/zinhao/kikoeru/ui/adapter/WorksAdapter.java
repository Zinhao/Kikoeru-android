package com.zinhao.kikoeru.ui.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.zinhao.kikoeru.Api;
import com.zinhao.kikoeru.App;
import com.zinhao.kikoeru.R;
import com.zinhao.kikoeru.TagsView;
import com.zinhao.kikoeru.WorkAdapter;
import com.zinhao.kikoeru.data.model.Tag;
import com.zinhao.kikoeru.data.model.Va;
import com.zinhao.kikoeru.data.model.Work;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 作品列表适配器（MVVM 版本，使用 Work 数据模型）
 */
public class WorksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private List<Work> datas;
    private int layoutType;
    
    private TagsView.TagClickListener<Tag> tagClickListener;
    private TagsView.TagClickListener<Va> vaClickListener;
    private TagsView.TagClickListener<String> circlesClickListener;
    private OnItemClickListener itemClickListener;
    private OnItemLongClickListener itemLongClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(View view, int position, Work work);
    }
    
    public interface OnItemLongClickListener {
        boolean onItemLongClick(View view, int position, Work work);
    }
    
    public WorksAdapter(List<Work> datas, int layoutType) {
        this.datas = datas != null ? datas : new ArrayList<>();
        this.layoutType = layoutType;
    }
    
    public void setTagClickListener(TagsView.TagClickListener<Tag> listener) {
        this.tagClickListener = listener;
    }
    
    public void setVaClickListener(TagsView.TagClickListener<Va> listener) {
        this.vaClickListener = listener;
    }
    
    public void setCirclesClickListener(TagsView.TagClickListener<String> listener) {
        this.circlesClickListener = listener;
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }
    
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.itemLongClickListener = listener;
    }
    
    public void setData(List<Work> data) {
        android.util.Log.d("WorksAdapter", "setData: " + (data != null ? data.size() : 0) + " items");
        this.datas = data != null ? data : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public List<Work> getData() {
        return datas;
    }
    
    public void addData(List<Work> data) {
        if (data != null && !data.isEmpty()) {
            int startPosition = datas.size();
            datas.addAll(data);
            notifyItemRangeInserted(startPosition, data.size());
        }
    }
    
    public void clear() {
        datas.clear();
        notifyDataSetChanged();
    }
    
    public void remove(int position) {
        if (position >= 0 && position < datas.size()) {
            datas.remove(position);
            notifyItemRemoved(position);
        }
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (layoutType == WorkAdapter.LAYOUT_LIST) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_1, parent, false);
            return new SimpleViewHolder(v);
        } else if (layoutType == WorkAdapter.LAYOUT_BIG_GRID) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_2, parent, false);
            return new GirdViewHolder(v);
        } else if (layoutType == WorkAdapter.LAYOUT_STAGGERED) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_2, parent, false);
            return new GirdViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_3, parent, false);
            return new SmallGirdViewHolder(v);
        }
    }
    
    @SuppressLint("DefaultFormatter")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Work item = datas.get(position);
        
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(v, position, item);
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (itemLongClickListener != null) {
                return itemLongClickListener.onItemLongClick(v, position, item);
            }
            return false;
        });
        
        String coverUrl = App.getInstance().currentUser().getHost() + 
                String.format("/api/cover/%d?token=%s", item.getId(), Api.token);
        
        if (holder instanceof SimpleViewHolder) {
            bindSimpleViewHolder((SimpleViewHolder) holder, item, coverUrl);
        } else if (holder instanceof GirdViewHolder) {
            bindGirdViewHolder((GirdViewHolder) holder, item, coverUrl);
        } else if (holder instanceof SmallGirdViewHolder) {
            bindSmallGirdViewHolder((SmallGirdViewHolder) holder, item, coverUrl);
        }
    }
    
    private void bindSimpleViewHolder(SimpleViewHolder holder, Work item, String coverUrl) {
        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
        
        // 设置声优
        List<Va> vas = item.getVas();
        if (vas != null && !vas.isEmpty()) {
            holder.tvComArt.setTags(vas, (TagsView.TextGet<Va>) va -> va.getName());
            holder.tvComArt.setTagClickListener(vaClickListener);
        }
        
        // 设置标签
        List<Tag> tags = item.getTags();
        if (tags != null) {
            holder.tvTags.setTags(tags, (TagsView.TextGet<Tag>) tag -> tag.getName());
            holder.tvTags.setTagClickListener(tagClickListener);
        }
        
        // 设置社团
        if (item.getCircle() != null) {
            List<String> circles = new ArrayList<>();
            circles.add(item.getCircle().getName());
            holder.tvCircles.setTags(circles, (TagsView.TextGet<String>) s -> s);
            holder.tvCircles.setTagClickListener(circlesClickListener);
        }
        
        Glide.with(holder.itemView.getContext())
                .load(coverUrl + "&type=sam")
                .apply(App.getInstance().getDefaultPic())
                .into(holder.ivCover);
    }
    
    private void bindGirdViewHolder(GirdViewHolder holder, Work item, String coverUrl) {
        Glide.with(holder.itemView.getContext())
                .load(coverUrl)
                .apply(App.getInstance().getDefaultPic())
                .into(holder.ivCover);
        
        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
        holder.tvCom.setText(item.getCircle() != null ? item.getCircle().getName() : "");
        
        // 设置声优
        List<Va> vas = item.getVas();
        if (vas != null && !vas.isEmpty()) {
            holder.tvArt.setTags(vas, (TagsView.TextGet<Va>) va -> va.getName());
            holder.tvArt.setTagClickListener(vaClickListener);
        }
        
        // 设置标签
        List<Tag> tags = item.getTags();
        if (tags != null) {
            holder.tvTags.setTags(tags, (TagsView.TextGet<Tag>) tag -> tag.getName());
            holder.tvTags.setTagClickListener(tagClickListener);
        }
        
        // 设置社团
        if (item.getCircle() != null) {
            List<String> circles = new ArrayList<>();
            circles.add(item.getCircle().getName());
            holder.tvCircles.setTags(circles, (TagsView.TextGet<String>) s -> s);
            holder.tvCircles.setTagClickListener(circlesClickListener);
        }
        
        holder.tvRjNumber.setText(String.format("RJ%d", item.getId()));
        holder.tvDate.setText(item.getReleaseDate() != null ? item.getReleaseDate() : "");
        holder.tvPrice.setText(String.format("%d 日元", item.getPrice()));
        holder.tvSaleCount.setText(String.format("售出：%d", item.getDlCount()));
        
        if (item.isLocalWork()) {
            holder.tvHost.setVisibility(View.VISIBLE);
            holder.tvHost.setText(item.getHost());
        } else {
            holder.tvHost.setVisibility(View.INVISIBLE);
        }
    }
    
    private void bindSmallGirdViewHolder(SmallGirdViewHolder holder, Work item, String coverUrl) {
        Glide.with(holder.itemView.getContext())
                .load(coverUrl)
                .apply(App.getInstance().getDefaultPic())
                .into(holder.ivCover);
        
        holder.tvRjNumber.setText(String.format("RJ%d", item.getId()));
        holder.tvDate.setText(item.getReleaseDate() != null ? item.getReleaseDate() : "");
        
        if (item.isLocalWork()) {
            holder.tvHost.setVisibility(View.VISIBLE);
            holder.tvHost.setText(item.getHost());
        } else {
            holder.tvHost.setVisibility(View.INVISIBLE);
        }
    }
    
    @Override
    public int getItemCount() {
        return datas.size();
    }
    
    public void setLayoutType(int layoutType) {
        this.layoutType = layoutType;
        notifyDataSetChanged();
    }
    
    static class SimpleViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        TagsView<List<Va>> tvComArt;
        TagsView<List<Tag>> tvTags;
        TagsView<List<String>> tvCircles;
        
        @SuppressWarnings("unchecked")
        SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvComArt = itemView.findViewById(R.id.tvComArt);
            tvTags = itemView.findViewById(R.id.tvTags);
            tvCircles = itemView.findViewById(R.id.tvCircles);
        }
    }
    
    static class GirdViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        TextView tvCom;
        TagsView<List<Va>> tvArt;
        TagsView<List<Tag>> tvTags;
        TagsView<List<String>> tvCircles;
        TextView tvRjNumber;
        TextView tvDate;
        TextView tvPrice;
        TextView tvSaleCount;
        TextView tvHost;
        
        @SuppressWarnings("unchecked")
        GirdViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCom = itemView.findViewById(R.id.tvCom);
            tvArt = itemView.findViewById(R.id.tvArt);
            tvTags = itemView.findViewById(R.id.tvTags);
            tvCircles = itemView.findViewById(R.id.tvCircles);
            tvRjNumber = itemView.findViewById(R.id.tvRjNumber);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSaleCount = itemView.findViewById(R.id.tvSaleCount);
            tvHost = itemView.findViewById(R.id.tvHost);
        }
    }
    
    static class SmallGirdViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvRjNumber;
        TextView tvDate;
        TextView tvHost;
        
        SmallGirdViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvRjNumber = itemView.findViewById(R.id.tvRjNumber);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvHost = itemView.findViewById(R.id.tvHost);
        }
    }
}
