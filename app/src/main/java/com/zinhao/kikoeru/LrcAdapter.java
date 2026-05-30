package com.zinhao.kikoeru;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class LrcAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "LrcAdapter";
    private Lrc mLrc;
    private int oldIndex = -1;

    private View.OnClickListener onToHereClickListener;

    public LrcAdapter(Lrc mText) {
        this.mLrc = mText;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TextRowHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lrc_row, parent, false));
    }

    public void update(){
        int index = mLrc.getCurrentIndex();
        if(oldIndex != -1){
            notifyItemChanged(oldIndex);
        }
        notifyItemChanged(index);
        oldIndex = index;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Lrc.LrcRow lrcRow = mLrc.getLrcRows().get(position);
        if (holder instanceof TextRowHolder) {
            ((TextRowHolder) holder).textView.setText(lrcRow.content);
            if(mLrc.getCurrentIndex() == position){
                ((TextRowHolder) holder).textView.setTextSize(36);
                ((TextRowHolder) holder).textView.setAlpha(1.0f);
                ((TextRowHolder) holder).ivToHere.setVisibility(View.GONE);
            }else{
                ((TextRowHolder) holder).textView.setTextSize(15);
                ((TextRowHolder) holder).textView.setAlpha(0.5f);
                ((TextRowHolder) holder).ivToHere.setVisibility(View.VISIBLE);
                ((TextRowHolder) holder).ivToHere.setTag(lrcRow);
                ((TextRowHolder) holder).ivToHere.setOnClickListener(onToHereClickListener);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mLrc.getLrcRows().size();
    }

    public void setOnToHereClickListener(View.OnClickListener onToHereClickListener) {
        this.onToHereClickListener = onToHereClickListener;
    }

    static class TextRowHolder extends RecyclerView.ViewHolder {
        private TextView textView;
        private ImageView ivToHere;

        public TextRowHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView3);
            ivToHere = itemView.findViewById(R.id.toHere);
        }
    }
}
