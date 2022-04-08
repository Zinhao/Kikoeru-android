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

import java.util.ArrayList;
import java.util.List;

public class WorkTreeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<JSONObject> data;
    private View.OnClickListener itemClickListener;
    private List<List<JSONObject>> parentData;
    private JSONObject headerInfo;
    private static final int TYPE_HEADER = 295;
    private static final int TYPE_FILE = 296;

    public void setHeaderInfo(JSONObject headerInfo) {
        this.headerInfo = headerInfo;
    }

    public void setItemClickListener(View.OnClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public List<JSONObject> getData() {
        return data;
    }

    public WorkTreeAdapter(List<JSONObject> data) {
        this.data = data;
        parentData =new ArrayList<>();
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0){
            return TYPE_HEADER;
        }
        return TYPE_FILE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        if(viewType == TYPE_FILE){
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_tree_1,parent,false);
            return new SimpleViewHolder(v);
        }else if(viewType == TYPE_HEADER){
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_2,parent,false);
            return new DetailViewHolder(v);
        }else {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_tree_1,parent,false);
        }
        return new SimpleViewHolder(v);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        JSONObject jsonObject;
        if(position == 0){
            jsonObject = headerInfo;
        }else {
            jsonObject= data.get(position-1);
        }
        if(holder instanceof SimpleViewHolder){
            try {
                ((SimpleViewHolder) holder).tvTitle.setText(jsonObject.getString("title"));
                ((SimpleViewHolder) holder).tvCount.setText(jsonObject.getString("type"));
                if("folder".equals(jsonObject.getString("type"))){
                    JSONArray jsonArray = jsonObject.getJSONArray("children");
                    ((SimpleViewHolder) holder).tvCount.setText(String.format("%d 项",jsonArray.length()));
                    Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_folder_24).into(((SimpleViewHolder) holder).ivCover);
                }else if("audio".equals(jsonObject.getString("type"))){
                    Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_audiotrack_24).into(((SimpleViewHolder) holder).ivCover);
                }else if("image".equals(jsonObject.getString("type"))){
                    Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_image_24).into(((SimpleViewHolder) holder).ivCover);
                }else if("text".equals(jsonObject.getString("type"))){
                    Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_text_snippet_24).into(((SimpleViewHolder) holder).ivCover);
                }else {
                    Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_insert_drive_file_24).into(((SimpleViewHolder) holder).ivCover);
                }

                if("folder".equals(jsonObject.getString("type"))){
                    JSONArray jsonArray = jsonObject.getJSONArray("children");
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            parentData.add(data);
                            List<JSONObject> list = new ArrayList<>();
                            for (int i = 0; i < jsonArray.length(); i++) {
                                try {
                                    list.add(jsonArray.getJSONObject(i));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            data = list;
//                            notifyItemRangeChanged(1,list.size()-1);
                            notifyDataSetChanged();
                        }
                    });
                }else if("audio".equals(jsonObject.getString("type")) ||
                        "image".equals(jsonObject.getString("type")) ||
                        "text".equals(jsonObject.getString("type"))){
                    holder.itemView.setTag(jsonObject);
                    holder.itemView.setOnClickListener(itemClickListener);
                } else {
                    holder.itemView.setOnClickListener(null);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if(holder instanceof DetailViewHolder){
            DetailViewHolder girdHolder = (DetailViewHolder) holder;
            try {
                Glide.with(holder.itemView.getContext()).load(Api.HOST+String.format("/api/cover/%d",jsonObject.getInt("id"))).into(girdHolder.ivCover);
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

    public boolean parentDir(){
        if(parentData == null ||parentData.size() == 0){
            return true;
        }
        data = parentData.remove(parentData.size()-1);
        notifyDataSetChanged();
        return false;
    }

    @Override
    public int getItemCount() {
        return data.size() + 1;
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder{
        private ImageView ivCover;
        private TextView tvTitle;
        private TextView tvCount;


        public SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCount = itemView.findViewById(R.id.tvCount);
        }
    }

    public static class DetailViewHolder extends RecyclerView.ViewHolder{
        private ImageView ivCover;
        private TextView tvTitle;
        private TextView tvCom;
        private TextView tvArt;
        private TextView tvTags;
        private TextView tvRjNumber;
        private TextView tvDate;
        private TextView tvPrice;
        private TextView tvSaleCount;

        public DetailViewHolder(@NonNull View itemView) {
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
