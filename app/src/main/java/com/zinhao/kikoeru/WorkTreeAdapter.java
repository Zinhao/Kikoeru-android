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
import java.util.ArrayList;
import java.util.List;

public class WorkTreeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<JSONObject> data;
    private View.OnClickListener itemClickListener;
    private View.OnLongClickListener longClickListener;
    private TagsView.TagClickListener tagClickListener;
    private TagsView.TagClickListener vaClickListener;
    private List<List<JSONObject>> parentData;
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

    public List<JSONObject> getData() {
        return data;
    }

    public WorkTreeAdapter(List<JSONObject> data) {
        this.data = data;
        this.pathList = new ArrayList<>();
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

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        JSONObject item;
        if(position == 0){
            item = headerInfo;
        }else {
            item= data.get(position-1);
        }

        if(holder instanceof SimpleViewHolder){
            try {
                String itemTitle = item.getString("title");
                ((SimpleViewHolder) holder).tvTitle.setText(itemTitle);
                File mapFile = LocalFileCache.getInstance().mapLocalItemFile(item,headerInfo.getInt("id"),getRelativePath());
                if(mapFile.exists()){
                    item.put("local_file_path",mapFile.getAbsolutePath());
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
                                        parentData.add(data);
                                        JSONArray jsonArray = item.getJSONArray("children");
                                        List<JSONObject> list = new ArrayList<>();
                                        pathList.add(item.getString("title"));
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            list.add(jsonArray.getJSONObject(i));
                                        }
                                        data = list;
                                        if(pathChangeListener != null){
                                            pathChangeListener.onPathChange(getRelativePath());
                                        }
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
                }else if("audio".equals(item.getString("type")) ||
                        "image".equals(item.getString("type")) ||
                        "text".equals(item.getString("type"))){
                    holder.itemView.setTag(item);
                    holder.itemView.setOnClickListener(itemClickListener);
                    holder.itemView.setOnLongClickListener(longClickListener);
                } else {
                    holder.itemView.setOnLongClickListener(null);
                    holder.itemView.setOnClickListener(null);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
        } else if(holder instanceof DetailViewHolder){
            DetailViewHolder girdHolder = (DetailViewHolder) holder;
            try {
                Glide.with(holder.itemView.getContext()).load(Api.HOST+String.format("/api/cover/%d",item.getInt("id"))).apply(App.getInstance().getDefaultPic()).into(girdHolder.ivCover);
                girdHolder.tvTitle.setText(item.getString("title"));
                girdHolder.tvArt.setTags(App.getVasList(item),TagsView.JSON_TEXT_GET.setKey("name"));
                girdHolder.tvArt.setTagClickListener(vaClickListener);
                girdHolder.tvCom.setText(item.getString("name"));
                girdHolder.tvTags.setTags(App.getTagsList(item),TagsView.JSON_TEXT_GET.setKey("name"));
                girdHolder.tvTags.setTagClickListener(tagClickListener);
                girdHolder.tvRjNumber.setText(String.format("RJ%d",item.getInt("id")));
                girdHolder.tvDate.setText(item.getString("release"));
                girdHolder.tvPrice.setText(String.format("%d 日元",item.getInt("price")));
                girdHolder.tvSaleCount.setText(String.format("售出：%d",item.getInt("dl_count")));
            } catch (JSONException e) {
                e.printStackTrace();
                App.getInstance().alertException(e);
            }
        }
    }

    public boolean parentDir(){
        if(parentData == null ||parentData.size() == 0){
            return true;
        }
        data = parentData.remove(parentData.size()-1);
        if(pathList.size()!=0){
            pathList.remove(pathList.size()-1);
            pathChangeListener.onPathChange(getRelativePath());
        }
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
        private TagsView<JSONArray> tvArt;
        private TagsView<JSONArray> tvTags;
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

    public interface RelativePathChangeListener{
        void onPathChange(String path);
    }
}
