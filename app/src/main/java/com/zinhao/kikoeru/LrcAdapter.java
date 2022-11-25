package com.zinhao.kikoeru;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class LrcAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Lrc lrc;
    private View.OnClickListener onClickListener;
    private int unCacheItemBackgroundColor = -1;
    private int cachedItemBackgroundColor = -1;

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public LrcAdapter(Lrc lrc) {
        this.lrc = lrc;
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
        return new LrcRowHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lrc_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Lrc.LrcRow lrcRow = lrc.getLrcRows().get(position);
        if (holder instanceof LrcRowHolder) {
            ((LrcRowHolder) holder).textView.setText(String.format("[%s] %s", lrcRow.strTime, lrcRow.content));
            holder.itemView.setTag(lrcRow);
            holder.itemView.setOnClickListener(onClickListener);
            if (lrc.getCurrentIndex() == position) {
                holder.itemView.setBackgroundColor(cachedItemBackgroundColor);
            } else {
                holder.itemView.setBackgroundColor(unCacheItemBackgroundColor);
            }
        }
    }

    @Override
    public int getItemCount() {
        return lrc.getLrcRows().size();
    }

    static class LrcRowHolder extends RecyclerView.ViewHolder {
        private TextView textView;

        public LrcRowHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView3);
        }
    }
}
