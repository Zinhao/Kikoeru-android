package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WorkAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "WorkAdapter";
    private List<JSONObject> datas;
    private int layoutType;
    private TagsView.TextGet<JSONObject> textGet;
    private TagsView.TagClickListener tagClickListener;
    private TagsView.TagClickListener vaClickListener;
    private TagsView.TagClickListener circlesClickListener;
    private View.OnClickListener itemClickListener;
    private View.OnLongClickListener itemLongClickListener;

    public static final int LAYOUT_LIST = 846;
    public static final int LAYOUT_SMALL_GRID = 847;
    public static final int LAYOUT_BIG_GRID = 848;
    public static final int LAYOUT_STAGGERED = 849;

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_LOADING = 1;

    private boolean isLoading = false; // 是否正在加载

    // 辅助方法：显示/隐藏加载动画
    public void setLoading(boolean loading) {
        if (this.isLoading != loading) {
            this.isLoading = loading;
            if (loading) {
                notifyItemInserted(datas.size());
            } else {
                notifyItemRemoved(datas.size());
            }
        }
    }

    public void setTagClickListener(TagsView.TagClickListener<?> tagClickListener) {
        this.tagClickListener = tagClickListener;
    }

    public void setItemClickListener(View.OnClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setCirclesClickListener(TagsView.TagClickListener circlesClickListener) {
        this.circlesClickListener = circlesClickListener;
    }

    public void setItemLongClickListener(View.OnLongClickListener itemLongClickListener) {
        this.itemLongClickListener = itemLongClickListener;
    }

    public void setVaClickListener(TagsView.TagClickListener<?> vaClickListener) {
        this.vaClickListener = vaClickListener;
    }

    public WorkAdapter(List<JSONObject> datas) {
        this(datas, LAYOUT_SMALL_GRID);
    }

    public WorkAdapter(List<JSONObject> datas, int layoutType) {
        this.datas = datas;
        this.layoutType = layoutType;
        textGet = new TagsView.TextGet<JSONObject>() {
            @Override
            public String onGetText(JSONObject t) {
                try {
                    return t.getString("name");
                } catch (JSONException e) {

                }
                return "";
            }
        };
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_LOADING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
            return new LoadingViewHolder(view);
        }
        if (layoutType == LAYOUT_LIST) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_1, parent, false);
            return new SimpleViewHolder(v);
        } else if (layoutType == LAYOUT_BIG_GRID) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_2, parent, false);
            return new GirdViewHolder(v);
        } else if (layoutType == LAYOUT_STAGGERED) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_2, parent, false);
            return new GirdViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_3, parent, false);
            return new SmallGirdViewHolder(v);
        }

    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(position == datas.size()){
            return;
        }
        JSONObject item = datas.get(position);
        holder.itemView.setTag(item);
        holder.itemView.setOnClickListener(itemClickListener);
        holder.itemView.setOnLongClickListener(itemLongClickListener);
        if (holder instanceof SimpleViewHolder) {
            try {
                ((SimpleViewHolder) holder).tvTitle.setText(item.getString("title"));
                ((SimpleViewHolder) holder).tvComArt.setTags(App.getVasList(item), TagsView.JSON_TEXT_GET.setKey("name"));
                ((SimpleViewHolder) holder).tvComArt.setTagClickListener(vaClickListener);
                ((SimpleViewHolder) holder).tvTags.setTags(App.getTagsList(item), textGet);
                ((SimpleViewHolder) holder).tvCircles.setTags(Collections.singletonList(item.getString("name")),TagsView.STRING_TEXT_GET);
                ((SimpleViewHolder) holder).tvCircles.setTagClickListener(circlesClickListener);
                ((SimpleViewHolder) holder).tvTags.setTagClickListener(tagClickListener);
                Glide.with(holder.itemView.getContext()).load(App.getInstance().currentUser().getHost() + String.format("/api/cover/%d?type=sam&token=%s", item.getInt("id"), Api.token))
                        .apply(App.getInstance().getDefaultPic()).into(((SimpleViewHolder) holder).ivCover);
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
        }
        if (holder instanceof GirdViewHolder) {
            GirdViewHolder girdHolder = (GirdViewHolder) holder;
            try {
                Glide.with(holder.itemView.getContext()).load(App.getInstance().currentUser().getHost() + String.format("/api/cover/%d?token=%s", item.getInt("id"), Api.token))
                        .apply(App.getInstance().getDefaultPic()).into(girdHolder.ivCover);
                girdHolder.tvTitle.setText(item.getString("title"));
                girdHolder.tvArt.setTags(App.getVasList(item), TagsView.JSON_TEXT_GET.setKey("name"));
                girdHolder.tvArt.setTagClickListener(vaClickListener);
                girdHolder.tvCom.setText(item.getString("name"));
                girdHolder.tvTags.setTags(App.getTagsList(item), textGet);
                girdHolder.tvCircles.setTags(Collections.singletonList(item.getString("name")),TagsView.STRING_TEXT_GET);
                girdHolder.tvCircles.setTagClickListener(circlesClickListener);
                girdHolder.tvTags.setTagClickListener(tagClickListener);
                girdHolder.tvRjNumber.setText(String.format("RJ%d", item.getInt("id")));

                String dateStr = item.optString("release");
                if(dateStr.isEmpty()){
                    girdHolder.tvDate.setVisibility(View.GONE);
                }else{
                    girdHolder.tvDate.setVisibility(View.VISIBLE);
                    girdHolder.tvDate.setText(dateStr);
                }

                girdHolder.tvPrice.setText(String.format("%d 日元", item.getInt("price")));
                girdHolder.tvSaleCount.setText(String.format("售出：%d", item.getInt("dl_count")));
                if (item.has(JSONConst.Work.HOST)) {
                    girdHolder.tvHost.setVisibility(View.VISIBLE);
                    girdHolder.tvHost.setText(item.getString(JSONConst.Work.HOST));
                } else {
                    girdHolder.tvHost.setVisibility(View.INVISIBLE);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
        }

        if (holder instanceof SmallGirdViewHolder) {
            SmallGirdViewHolder girdHolder = (SmallGirdViewHolder) holder;
            try {
                Glide.with(holder.itemView.getContext()).load(App.getInstance().currentUser().getHost() + String.format("/api/cover/%d?token=%s", item.getInt("id"), Api.token))
                        .apply(App.getInstance().getDefaultPic()).into(girdHolder.ivCover);
                girdHolder.tvRjNumber.setText(String.format("RJ%d", item.getInt("id")));
                girdHolder.tvDate.setText(item.getString("release"));
                if (item.has(JSONConst.Work.HOST)) {
                    girdHolder.tvHost.setVisibility(View.VISIBLE);
                    girdHolder.tvHost.setText(item.getString(JSONConst.Work.HOST));
                } else {
                    girdHolder.tvHost.setVisibility(View.INVISIBLE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }

        }
    }

    @Override
    public int getItemViewType(int position) {
        // 如果位置是最后一位且处于加载状态，返回加载布局类型
        if (position == datas.size()) {
            return TYPE_LOADING;
        }
        return TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return datas.size() + (isLoading ? 1 : 0);
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivCover;
        private final TextView tvTitle;
        private final TagsView<JSONArray> tvComArt;
        private final TagsView<JSONArray> tvTags;
        private final TagsView<List<String>> tvCircles;


        public SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvComArt = itemView.findViewById(R.id.tvComArt);
            tvTags = itemView.findViewById(R.id.tvTags);
            tvCircles = itemView.findViewById(R.id.tvCircles);
        }
    }

    public static class GirdViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivCover;
        private final TextView tvTitle;
        private final TextView tvCom;
        private final TagsView<JSONArray> tvArt;
        private final TagsView<JSONArray> tvTags;
        private final TagsView<List<String>> tvCircles;
        private final TextView tvRjNumber;
        private final TextView tvDate;
        private final TextView tvPrice;
        private final TextView tvSaleCount;
        private final TextView tvHost;

        public GirdViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCom = itemView.findViewById(R.id.tvCom);
            tvArt = itemView.findViewById(R.id.tvArt);
            tvTags = itemView.findViewById(R.id.tvTags);
            tvRjNumber = itemView.findViewById(R.id.tvRjNumber);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSaleCount = itemView.findViewById(R.id.tvSaleCount);
            tvHost = itemView.findViewById(R.id.tvHost);
            tvCircles = itemView.findViewById(R.id.tvCircles);
        }
    }

    public static class SmallGirdViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivCover;
        private TextView tvRjNumber;
        private TextView tvDate;
        private final TextView tvHost;

        public SmallGirdViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvRjNumber = itemView.findViewById(R.id.tvRjNumber);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvHost = itemView.findViewById(R.id.tvHost);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        public LoadingViewHolder(View itemView) { super(itemView); }
    }
}
