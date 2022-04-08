package com.zinhao.kikoeru;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class LrcAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Lrc lrc;

    public LrcAdapter(Lrc lrc) {
        this.lrc = lrc;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LrcRowHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lrc_row,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Lrc.LrcRow lrcRow = lrc.getLrcRows().get(position);
        if(holder instanceof LrcRowHolder){
            ((LrcRowHolder) holder).textView.setText(lrcRow.content);
            if(lrc.getCurrentIndex() == position){
                holder.itemView.setBackgroundColor(Color.GREEN);
            }else {
                holder.itemView.setBackgroundColor(Color.WHITE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return lrc.getLrcRows().size();
    }

    static class LrcRowHolder extends RecyclerView.ViewHolder{
        private TextView textView;

        public LrcRowHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView3);
        }
    }
}
