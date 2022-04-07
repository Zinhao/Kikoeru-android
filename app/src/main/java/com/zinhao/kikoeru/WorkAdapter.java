package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.content.Intent;
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
    private List<JSONObject> datas;

    public WorkAdapter(List<JSONObject> datas) {
        this.datas = datas;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_2,parent,false);
        return new GirdViewHolder(v);
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
                Glide.with(holder.itemView.getContext()).load(MainActivity.HOST+String.format("/api/cover/%d?type=sam",jsonObject.getInt("id"))).into(((SimpleViewHolder) holder).ivCover);
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
                Glide.with(holder.itemView.getContext()).load(MainActivity.HOST+String.format("/api/cover/%d",jsonObject.getInt("id"))).into(girdHolder.ivCover);
                girdHolder.tvTitle.setText(jsonObject.getString("title"));
                girdHolder.tvArt.setText(App.getArtStr(jsonObject));
                girdHolder.tvCom.setText(jsonObject.getString("name"));
                girdHolder.tvTags.setText(App.getTagsStr(jsonObject));
                girdHolder.tvRjNumber.setText(String.format("RJ%d",jsonObject.getInt("id")));
                girdHolder.tvDate.setText(jsonObject.getString("release"));
                girdHolder.tvPrice.setText(String.format("%d 日元",jsonObject.getInt("price")));
                girdHolder.tvSaleCount.setText(String.format("售出：%d",jsonObject.getInt("dl_count")));
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
        private TextView tvTags;
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
}
