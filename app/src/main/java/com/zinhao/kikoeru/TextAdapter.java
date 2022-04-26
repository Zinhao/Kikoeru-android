package com.zinhao.kikoeru;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TextAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Text mText;

    public TextAdapter(Text mText) {
        this.mText = mText;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TextRowHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_row,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Text.TextRow textRow = mText.getLrcRows().get(position);
        if(holder instanceof TextRowHolder){
            ((TextRowHolder) holder).textView.setText(textRow.content);
        }
    }

    @Override
    public int getItemCount() {
        return mText.getLrcRows().size();
    }

    static class TextRowHolder extends RecyclerView.ViewHolder{
        private TextView textView;

        public TextRowHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView3);
        }
    }
}
