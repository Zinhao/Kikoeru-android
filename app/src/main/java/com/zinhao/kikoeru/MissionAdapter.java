package com.zinhao.kikoeru;

import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpResponse;
import org.json.JSONObject;

public class MissionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnLongClickListener {
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SimpleViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mission_progress, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DownloadUtils.Mission item = DownloadUtils.getInstance().missionList.get(position);
        if (holder instanceof SimpleViewHolder) {
            SimpleViewHolder simpleViewHolder = (SimpleViewHolder) holder;
            simpleViewHolder.tvTitle.setText(item.getTitle());
            simpleViewHolder.ivCover.setImageResource(item.getTypeCover());
            simpleViewHolder.pbProgress.setMax(100);
            simpleViewHolder.pbProgress.setProgress(item.getProgress());
            if(item.getMissionException()!=null){
                simpleViewHolder.tvProgress.setText(item.getMissionException().getMessage());
            }else{
                simpleViewHolder.tvProgress.setText(item.getFormatProgressText());
            }

            simpleViewHolder.itemView.setTag(item);
            if (item.isCompleted()) {
                simpleViewHolder.itemView.setOnLongClickListener(this);
                simpleViewHolder.ibPause.setVisibility(View.INVISIBLE);
                simpleViewHolder.ibPause.setOnClickListener(null);
            } else {
                simpleViewHolder.itemView.setOnLongClickListener(null);
                simpleViewHolder.ibPause.setVisibility(View.VISIBLE);
                simpleViewHolder.ibPause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (item.isDownloading()) {
                            item.stop();
                        } else if (!item.isCompleted()) {
                            item.start();
                        }
                        notifyItemChanged(holder.getAdapterPosition());
                    }
                });
            }
            simpleViewHolder.ibPause.setImageResource(item.isDownloading() ? R.drawable.ic_baseline_pause_24 : R.drawable.ic_baseline_play_arrow_24);
        }
    }

    @Override
    public int getItemCount() {
        return DownloadUtils.getInstance().missionList.size();
    }

    @Override
    public boolean onLongClick(View v) {
        DownloadUtils.Mission item = (DownloadUtils.Mission) v.getTag();
        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setTitle("操作");
        builder.setMessage(item.getFormatProgressText());
        builder.setNegativeButton("打开作品页", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LocalFileCache.getInstance().readLocalWorkById(item.getWorkId(), new AsyncHttpClient.JSONObjectCallback() {
                    @Override
                    public void onCompleted(Exception e, AsyncHttpResponse asyncHttpResponse, JSONObject jsonObject) {
                        Intent intent = new Intent(v.getContext(), WorkTreeActivity.class);
                        intent.putExtra("work_json_str", jsonObject.toString());
                        ActivityCompat.startActivity(v.getContext(), intent, null);
                        dialog.dismiss();
                    }
                });
            }
        });
        builder.setPositiveButton("移除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int index = DownloadUtils.getInstance().removeMission(item);
                if (index != -1) {
                    notifyItemRemoved(index);
                    notifyItemRangeChanged(index, getItemCount() - index);
                }
                dialog.dismiss();
            }
        });
        builder.create().show();
        return true;
    }

    class SimpleViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle;
        private ImageView ivCover;
        private ProgressBar pbProgress;
        private ImageButton ibPause;
        private TextView tvProgress;

        public SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            ivCover = itemView.findViewById(R.id.ivCover);
            pbProgress = itemView.findViewById(R.id.progress);
            ibPause = itemView.findViewById(R.id.imageView3);
            tvProgress = itemView.findViewById(R.id.textView9);
        }
    }
}
