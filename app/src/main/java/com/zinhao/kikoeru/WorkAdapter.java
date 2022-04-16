package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class WorkAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "WorkAdapter";
    private List<JSONObject> datas;
    private int layoutType;
    private TagsView.TextGet<JSONObject> textGet;
    private TagsView.TagClickListener tagClickListener;
    public static final int LAYOUT_LIST = 846;
    public static final int LAYOUT_SMALL_GRID = 847;
    public static final int LAYOUT_BIG_GRID = 848;

    public void setTagClickListener(TagsView.TagClickListener<?> tagClickListener) {
        this.tagClickListener = tagClickListener;
    }

    public WorkAdapter(List<JSONObject> datas) {
        this(datas,LAYOUT_SMALL_GRID);
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
        if(layoutType == LAYOUT_LIST){
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_1,parent,false);
            return new SimpleViewHolder(v);
        }else if(layoutType == LAYOUT_BIG_GRID){
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_2,parent,false);
            return new GirdViewHolder(v);
        }else{
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_3,parent,false);
            return new SmallGirdViewHolder(v);
        }

    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        JSONObject jsonObject = datas.get(position);
        if(holder instanceof SimpleViewHolder){
            try {
                ((SimpleViewHolder) holder).tvTitle.setText(jsonObject.getString("title"));
                ((SimpleViewHolder) holder).tvComArt.setText(jsonObject.getString("name"));
                ((SimpleViewHolder) holder).tvTags.setText(App.getTagsStr(jsonObject));
                Glide.with(holder.itemView.getContext()).load(Api.HOST+String.format("/api/cover/%d?type=sam",jsonObject.getInt("id"))).into(((SimpleViewHolder) holder).ivCover);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(v.getContext(),WorkActivity.class);
                        intent.putExtra("work_json_str",jsonObject.toString());
                        ActivityCompat.startActivity(v.getContext(),intent,null);
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(holder instanceof GirdViewHolder){
            GirdViewHolder girdHolder = (GirdViewHolder) holder;
            try {
                Glide.with(holder.itemView.getContext()).load(Api.HOST+String.format("/api/cover/%d",jsonObject.getInt("id"))).apply(App.getInstance().getDefaultPic()).into(girdHolder.ivCover);
                girdHolder.tvTitle.setText(jsonObject.getString("title"));
                girdHolder.tvArt.setText(App.getArtStr(jsonObject));
                girdHolder.tvCom.setText(jsonObject.getString("name"));
                girdHolder.tvTags.setTags(App.getTagsList(jsonObject),textGet);
                girdHolder.tvTags.setTagClickListener(tagClickListener);
                girdHolder.tvRjNumber.setText(String.format("RJ%d",jsonObject.getInt("id")));
                girdHolder.tvDate.setText(jsonObject.getString("release"));
                girdHolder.tvPrice.setText(String.format("%d 日元",jsonObject.getInt("price")));
                girdHolder.tvSaleCount.setText(String.format("售出：%d",jsonObject.getInt("dl_count")));

            } catch (JSONException e) {
                e.printStackTrace();
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(),WorkActivity.class);
                    intent.putExtra("work_json_str",jsonObject.toString());
                    ActivityCompat.startActivity(v.getContext(),intent,null);
                }
            });
        }

        if(holder instanceof SmallGirdViewHolder){
            SmallGirdViewHolder girdHolder = (SmallGirdViewHolder) holder;
            try {
                Glide.with(holder.itemView.getContext()).load(Api.HOST+String.format("/api/cover/%d",jsonObject.getInt("id"))).apply(App.getInstance().getDefaultPic()).into(girdHolder.ivCover);
                girdHolder.tvRjNumber.setText(String.format("RJ%d",jsonObject.getInt("id")));
                girdHolder.tvDate.setText(jsonObject.getString("release"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(),WorkActivity.class);
                    intent.putExtra("work_json_str",jsonObject.toString());
                    ActivityCompat.startActivity(v.getContext(),intent,null);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder{
        private ImageView ivCover;
        private TextView tvTitle;
        private TextView tvComArt;
        private TextView tvTags;


        public SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvComArt = itemView.findViewById(R.id.tvComArt);
            tvTags = itemView.findViewById(R.id.tvTags);
        }
    }

    public static class GirdViewHolder extends RecyclerView.ViewHolder{
        private ImageView ivCover;
        private TextView tvTitle;
        private TextView tvCom;
        private TextView tvArt;
        private TagsView<JSONArray> tvTags;
        private TextView tvRjNumber;
        private TextView tvDate;
        private TextView tvPrice;
        private TextView tvSaleCount;

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
        }
    }

    public static class SmallGirdViewHolder extends RecyclerView.ViewHolder{
        private ImageView ivCover;
        private TextView tvRjNumber;
        private TextView tvDate;

        public SmallGirdViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvRjNumber = itemView.findViewById(R.id.tvRjNumber);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}
