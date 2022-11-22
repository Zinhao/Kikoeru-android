package com.zinhao.kikoeru;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.TypedValue;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class WorkTreeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private JSONArray data;
    private View.OnClickListener itemClickListener;
    private View.OnLongClickListener longClickListener;
    private TagsView.TagClickListener tagClickListener;
    private TagsView.TagClickListener vaClickListener;
    private List<JSONArray> parentData;
    private List<String> pathList;
    private RelativePathChangeListener pathChangeListener;
    private JSONObject headerInfo;
    private static final int TYPE_HEADER = 295;
    private static final int TYPE_FILE = 296;
    private int unCacheItemBackgroundColor = -1;
    private int cachedItemBackgroundColor = -1;

    public void setHeaderInfo(JSONObject headerInfo) {
        this.headerInfo = headerInfo;
    }

    public void setPathChangeListener(RelativePathChangeListener pathChangeListener) {
        this.pathChangeListener = pathChangeListener;
    }

    public void setItemClickListener(View.OnClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    public void setTagClickListener(TagsView.TagClickListener<?> tagClickListener) {
        this.tagClickListener = tagClickListener;
    }

    public void setVaClickListener(TagsView.TagClickListener<?> vaClickListener) {
        this.vaClickListener = vaClickListener;
    }

    public void setItemLongClickListener(View.OnLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public JSONArray getData() {
        return data;
    }

    public WorkTreeAdapter(JSONArray data,JSONObject headerInfo) {
        this.data = data;
        this.headerInfo = headerInfo;
        this.pathList = new ArrayList<>();
        parentData =new ArrayList<>();
        notifyWorkDataSetChanged();
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
        if(unCacheItemBackgroundColor == -1 || cachedItemBackgroundColor == -1){
            Context context = parent.getContext();
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.textAppearanceLarge, typedValue, true);
            int[] attribute = new int[] { R.attr.colorOnPrimary,R.attr.colorOnSecondary};
            TypedArray array = context.obtainStyledAttributes(typedValue.resourceId, attribute);
            unCacheItemBackgroundColor = array.getColor(0 , Color.WHITE);
            cachedItemBackgroundColor = array.getColor(1 , Color.WHITE);
            array.recycle();
        }
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

    public void notifyWorkDataSetChanged(){
        for (int i = 0; i < data.length(); i++) {
            JSONObject item = null;
            try {
                item = data.getJSONObject(i);
                LocalFileCache.getInstance().mapLocalItemFile(item,headerInfo.getInt("id"),getRelativePath());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof SimpleViewHolder){
            try {
                JSONObject item= data.getJSONObject(position-1);
                String itemTitle = item.getString("title");
                ((SimpleViewHolder) holder).tvTitle.setText(itemTitle);
                boolean exists = item.getBoolean(JSONConst.WorkTree.EXISTS);
                if(exists){
                    ((SimpleViewHolder) holder).ivCover.setBackgroundColor(cachedItemBackgroundColor);
                }else {
                    ((SimpleViewHolder) holder).ivCover.setBackgroundColor(unCacheItemBackgroundColor);
                }
                ((SimpleViewHolder) holder).tvCount.setText(item.getString("type"));
                if("folder".equals(item.getString("type"))){
                    JSONArray jsonArray = item.getJSONArray("children");
                    ((SimpleViewHolder) holder).tvCount.setText(String.format("%d 项",jsonArray.length()));
                    Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_folder_24).into(((SimpleViewHolder) holder).ivCover);
                }else if("audio".equals(item.getString("type"))){
                    if(itemTitle.endsWith(".mp4")){
                        Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_video_library_24).into(((SimpleViewHolder) holder).ivCover);
                    }else {
                        Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_audiotrack_24).into(((SimpleViewHolder) holder).ivCover);
                    }
                }else if("image".equals(item.getString("type"))){
                    Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_image_24).into(((SimpleViewHolder) holder).ivCover);
                }else if("text".equals(item.getString("type"))){
                    Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_text_snippet_24).into(((SimpleViewHolder) holder).ivCover);
                }else {
                    Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_insert_drive_file_24).into(((SimpleViewHolder) holder).ivCover);
                }

                if("folder".equals(item.getString("type"))){
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @SuppressLint("NotifyDataSetChanged")
                        @Override
                        public void onClick(View v) {
                            v.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        changeDir(item.getString("title"));
                                        data = item.getJSONArray("children");
                                        if(pathChangeListener != null){
                                            pathChangeListener.onPathChange(getRelativePath());
                                        }
                                        notifyWorkDataSetChanged();
                                        notifyDataSetChanged();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        App.getInstance().alertException(e);
                                    }
                                }
                            },300);
                        }
                    });
                    holder.itemView.setOnLongClickListener(null);
                }else{
                    holder.itemView.setTag(item);
                    holder.itemView.setOnClickListener(itemClickListener);
                    holder.itemView.setOnLongClickListener(longClickListener);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
        } else if(holder instanceof DetailViewHolder){
            DetailViewHolder girdHolder = (DetailViewHolder) holder;
            try {
                Glide.with(holder.itemView.getContext()).load(Api.HOST+String.format("/api/cover/%d?token=%s",headerInfo.getInt("id"),Api.token)).apply(App.getInstance().getDefaultPic()).into(girdHolder.ivCover);
                girdHolder.tvTitle.setText(headerInfo.getString("title"));
                girdHolder.tvArt.setTags(App.getVasList(headerInfo),TagsView.JSON_TEXT_GET.setKey("name"));
                girdHolder.tvArt.setTagClickListener(vaClickListener);
                girdHolder.tvCom.setText(headerInfo.getString("name"));
                girdHolder.tvTags.setTags(App.getTagsList(headerInfo),TagsView.JSON_TEXT_GET.setKey("name"));
                girdHolder.tvTags.setTagClickListener(tagClickListener);
                girdHolder.tvRjNumber.setText(String.format("RJ%d",headerInfo.getInt("id")));
                girdHolder.tvDate.setText(headerInfo.getString("release"));
                girdHolder.tvPrice.setText(String.format("%d 日元",headerInfo.getInt("price")));
                girdHolder.tvSaleCount.setText(String.format("售出：%d",headerInfo.getInt("dl_count")));
                if(headerInfo.has(JSONConst.Work.HOST)){
                    girdHolder.tvHost.setVisibility(View.VISIBLE);
                    girdHolder.tvHost.setText(headerInfo.getString(JSONConst.Work.HOST));
                }else {
                    girdHolder.tvHost.setVisibility(View.INVISIBLE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
        }
    }

    private void changeDir(String title){
        parentData.add(data);
        pathList.add(title);
    }

    public boolean parentDir(){
        if(parentData == null ||parentData.size() == 0){
            return true;
        }
        data = parentData.remove(parentData.size()-1);
        pathList.remove(pathList.size()-1);
        pathChangeListener.onPathChange(getRelativePath());
        notifyDataSetChanged();
        return false;
    }

    public String getRelativePath(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(File.separator);
        for (int i = 0; i < pathList.size(); i++) {
            stringBuilder.append(pathList.get(i));
            if(i != pathList.size() - 1){
                stringBuilder.append(File.separator);
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public int getItemCount() {
        return data.length() + 1;
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
        private TagsView<JSONArray> tvArt;
        private TagsView<JSONArray> tvTags;
        private TextView tvRjNumber;
        private TextView tvDate;
        private TextView tvPrice;
        private TextView tvSaleCount;
        private final TextView tvHost;

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
            tvHost = itemView.findViewById(R.id.tvHost);
        }
    }

    public interface RelativePathChangeListener{
        void onPathChange(String path);
    }
}
