package com.zinhao.kikoeru;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MissionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SimpleViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mission_progress,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DownloadUtils.Mission item = DownloadUtils.missionList.get(position);
        if(holder instanceof SimpleViewHolder){
            SimpleViewHolder simpleViewHolder = (SimpleViewHolder) holder;
            simpleViewHolder.tvTitle.setText(item.getTitle());
            simpleViewHolder.ivCover.setImageResource(item.getTypeCover());
            simpleViewHolder.pbProgress.setMax(100);
            simpleViewHolder.pbProgress.setProgress(item.getProgress());
        }
    }

    @Override
    public int getItemCount() {
        return DownloadUtils.missionList.size();
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder{
        private TextView tvTitle;
        private ImageView ivCover;
        private ProgressBar pbProgress;

        public SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            ivCover = itemView.findViewById(R.id.ivCover);
            pbProgress = itemView.findViewById(R.id.progress);
        }
    }
}
