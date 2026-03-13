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
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WorkTreeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<WorkTreeItem> data;
    private View.OnClickListener itemClickListener;
    private View.OnClickListener parentDirClickListener;
    private View.OnLongClickListener longClickListener;
    private TagsView.TagClickListener tagClickListener;
    private TagsView.TagClickListener vaClickListener;
    private TagsView.TagClickListener circlesClickListener;
    private List<List<WorkTreeItem>> parentData;
    private List<String> pathList;
    private RelativePathChangeListener pathChangeListener;
    private WorkInfo headerInfo;
    private static final int TYPE_HEADER = 295;
    private static final int TYPE_FILE = 296;
    private static final int TYPE_PARENT_DIR = 297;
    private int unCacheItemBackgroundColor = -1;
    private int cachedItemBackgroundColor = -1;

    public void setHeaderInfo(WorkInfo headerInfo) {
        this.headerInfo = headerInfo;
    }

    public void setCirclesClickListener(TagsView.TagClickListener circlesClickListener) {
        this.circlesClickListener = circlesClickListener;
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

    public void setParentDirClickListener(View.OnClickListener parentDirClickListener) {
        this.parentDirClickListener = parentDirClickListener;
    }

    public List<WorkTreeItem> getData() {
        return data;
    }

    public WorkTreeAdapter(List<WorkTreeItem> data, WorkInfo headerInfo) {
        this.data = data;
        this.headerInfo = headerInfo;
        this.pathList = new ArrayList<>();
        parentData = new ArrayList<>();
        mapFileExistValue();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_HEADER;
        } else if (position == 1) {
            return TYPE_PARENT_DIR;
        }
        return TYPE_FILE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (unCacheItemBackgroundColor == -1 || cachedItemBackgroundColor == -1) {
            Context context = parent.getContext();
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.textAppearanceLarge, typedValue, true);
            int[] attribute = new int[]{R.attr.colorOnPrimary, R.attr.colorOnSecondary};
            TypedArray array = context.obtainStyledAttributes(typedValue.resourceId, attribute);
            unCacheItemBackgroundColor = array.getColor(0, Color.WHITE);
            cachedItemBackgroundColor = array.getColor(1, Color.WHITE);
            array.recycle();
        }
        View v;
        if (viewType == TYPE_FILE) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_tree_1, parent, false);
            return new SimpleViewHolder(v);
        } else if (viewType == TYPE_HEADER) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_header, parent, false);
            return new DetailViewHolder(v);
        } else if (viewType == TYPE_PARENT_DIR) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_tree_2, parent, false);
            return new ParentDirViewHolder(v);
        } else {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_tree_1, parent, false);
        }
        return new SimpleViewHolder(v);
    }

    public void mapFileExistValue() {
        for (WorkTreeItem item : data) {
            try {
                JSONObject jsonItem = item.toJson();
                LocalFileCache.getInstance().mapLocalItemFile(jsonItem, headerInfo.getId(), getRelativePath());
                // 更新 item 的 exists 状态
                item.setExists(jsonItem.optBoolean(JSONConst.WorkTree.EXISTS, false));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof SimpleViewHolder) {
            WorkTreeItem item = data.get(position - 2);
            String itemTitle = item.getTitle();
            
            try {
                JSONObject jsonItem = item.toJson();
                DownloadUtils.Mission mapMission = DownloadUtils.mapMission(jsonItem);
                if (mapMission != null) {
                    mapMission.setStepCallback(() -> {
                        holder.itemView.post(() -> notifyItemChanged(position));
                    });
                    int progress = mapMission.getProgress();
                    ((SimpleViewHolder) holder).pb1.setVisibility(View.VISIBLE);
                    ((SimpleViewHolder) holder).pb1.setProgress(progress);
                    ((SimpleViewHolder) holder).tvCount.setText(progress + "%");
                } else {
                    ((SimpleViewHolder) holder).pb1.setVisibility(View.GONE);
                    ((SimpleViewHolder) holder).tvCount.setText(item.getType());
                }
            } catch (JSONException e) {
                ((SimpleViewHolder) holder).pb1.setVisibility(View.GONE);
                ((SimpleViewHolder) holder).tvCount.setText(item.getType());
            }
            
            ((SimpleViewHolder) holder).tvTitle.setText(itemTitle);
            boolean exists = item.isExists();
            if (exists) {
                ((SimpleViewHolder) holder).ivCover.setBackgroundColor(cachedItemBackgroundColor);
            } else {
                ((SimpleViewHolder) holder).ivCover.setBackgroundColor(unCacheItemBackgroundColor);
            }

            if (item.isFolder()) {
                ((SimpleViewHolder) holder).tvCount.setText(String.format("%d 项", item.getChildrenCount()));
                Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_folder_24).into(((SimpleViewHolder) holder).ivCover);
            } else if (item.isAudio()) {
                if (itemTitle.endsWith(".mp4")) {
                    Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_video_library_24).into(((SimpleViewHolder) holder).ivCover);
                } else {
                    Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_audiotrack_24).into(((SimpleViewHolder) holder).ivCover);
                }
            } else if (item.isImage()) {
                Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_image_24).into(((SimpleViewHolder) holder).ivCover);
            } else if (item.isText()) {
                Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_text_snippet_24).into(((SimpleViewHolder) holder).ivCover);
            } else {
                Glide.with(holder.itemView.getContext()).load(R.drawable.ic_baseline_insert_drive_file_24).into(((SimpleViewHolder) holder).ivCover);
            }

            if (item.isFolder()) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onClick(View v) {
                        v.postDelayed(() -> {
                            changeDir(item.getTitle());
                            data = item.getChildren();
                            if (pathChangeListener != null) {
                                pathChangeListener.onPathChange(getRelativePath());
                            }
                            mapFileExistValue();
                            notifyDataSetChanged();
                        }, 300);
                    }
                });
                holder.itemView.setOnLongClickListener(null);
            } else {
                try {
                    holder.itemView.setTag(item.toJson());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                holder.itemView.setOnClickListener(itemClickListener);
                holder.itemView.setOnLongClickListener(longClickListener);
            }
        } else if (holder instanceof DetailViewHolder) {
            DetailViewHolder girdHolder = (DetailViewHolder) holder;
            
            // 优先设置关键字段，避免后续异常导致这些字段无法显示
            girdHolder.tvRjNumber.setText(String.format("RJ%d", headerInfo.getId()));
            girdHolder.tvTitle.setText(headerInfo.getTitle());
            girdHolder.tvCom.setText(headerInfo.getHost());
            girdHolder.tvDate.setText(headerInfo.getRelease());
            girdHolder.tvPrice.setText(String.format("%d 日元", headerInfo.getPrice()));
            girdHolder.tvSaleCount.setText(String.format("售出：%d", headerInfo.getDlCount()));

            Glide.with(holder.itemView.getContext())
                    .load(App.getInstance().currentUser().getHost() + String.format("/api/cover/%d?token=%s", headerInfo.getId(), Api.token))
                    .apply(App.getInstance().getDefaultPic())
                    .into(girdHolder.ivCover);

            // 设置 VA
            try {
                JSONArray vasJson = new JSONArray();
                for (WorkInfo.VaInfo va : headerInfo.getVas()) {
                    JSONObject vaJson = new JSONObject();
                    vaJson.put("id", va.getId());
                    vaJson.put("name", va.getName());
                    vasJson.put(vaJson);
                }
                girdHolder.tvArt.setTags(vasJson, TagsView.JSON_TEXT_GET.setKey("name"));
                girdHolder.tvArt.setTagClickListener(vaClickListener);
            } catch (JSONException e) {
                girdHolder.tvArt.setTags(new JSONArray(), TagsView.JSON_TEXT_GET.setKey("name"));
            }

            // 设置 Tags
            try {
                JSONArray tagsJson = new JSONArray();
                for (WorkInfo.TagInfo tag : headerInfo.getTags()) {
                    JSONObject tagJson = new JSONObject();
                    tagJson.put("id", tag.getId());
                    tagJson.put("name", tag.getName());
                    tagsJson.put(tagJson);
                }
                girdHolder.tvTags.setTags(tagsJson, TagsView.JSON_TEXT_GET.setKey("name"));
                girdHolder.tvTags.setTagClickListener(tagClickListener);
            } catch (JSONException e) {
                girdHolder.tvTags.setTags(new JSONArray(), TagsView.JSON_TEXT_GET.setKey("name"));
            }

            // 设置 Circles
            try {
                girdHolder.tvCircles.setTags(Collections.singletonList(headerInfo.getName()), TagsView.STRING_TEXT_GET);
                girdHolder.tvCircles.setTagClickListener(circlesClickListener);
            } catch (Exception e) {
                girdHolder.tvCircles.setTags(Collections.emptyList(), TagsView.STRING_TEXT_GET);
            }

            if (headerInfo.getHost() != null && !headerInfo.getHost().isEmpty()) {
                girdHolder.tvHost.setVisibility(View.VISIBLE);
                girdHolder.tvHost.setText(headerInfo.getHost());
            } else {
                girdHolder.tvHost.setVisibility(View.INVISIBLE);
            }
        } else if (holder instanceof ParentDirViewHolder) {
            holder.itemView.setOnClickListener(parentDirClickListener);
        }
    }

    private void changeDir(String title) {
        parentData.add(new ArrayList<>(data));
        pathList.add(title);
    }

    public boolean parentDir() {
        if (parentData == null || parentData.isEmpty()) {
            return true;
        }
        data = parentData.remove(parentData.size() - 1);
        pathList.remove(pathList.size() - 1);
        pathChangeListener.onPathChange(getRelativePath());
        notifyDataSetChanged();
        return false;
    }

    public String getRelativePath() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(File.separator);
        for (int i = 0; i < pathList.size(); i++) {
            stringBuilder.append(pathList.get(i));
            if (i != pathList.size() - 1) {
                stringBuilder.append(File.separator);
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public int getItemCount() {
        return data.size() + 2;
    }

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivCover;
        private TextView tvTitle;
        private TextView tvCount;
        private ProgressBar pb1;


        public SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvCount = itemView.findViewById(R.id.tvCount);
            pb1 = itemView.findViewById(R.id.pb1);
        }
    }

    public static class DetailViewHolder extends RecyclerView.ViewHolder {
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
        private final TagsView<List<String>> tvCircles;

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
            tvCircles = itemView.findViewById(R.id.tvCircles);
        }
    }

    public static class ParentDirViewHolder extends RecyclerView.ViewHolder {

        public ParentDirViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public interface RelativePathChangeListener {
        void onPathChange(String path);
    }
}
