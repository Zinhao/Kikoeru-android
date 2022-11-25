package com.zinhao.kikoeru;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public abstract class SuperRecyclerAdapter<T> extends RecyclerView.Adapter<SuperRecyclerAdapter.SuperVHolder> {
    List<T> data;

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public SuperRecyclerAdapter(List<T> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public SuperVHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SuperVHolder(LayoutInflater.from(parent.getContext()).inflate(setLayout(viewType), parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull SuperVHolder holder, int position) {
        bindData(holder, position);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class SuperVHolder extends RecyclerView.ViewHolder {
        public SuperVHolder(@NonNull View itemView) {
            super(itemView);
        }

        public View getView(int id) {
            return itemView.findViewById(id);
        }

        public void setText(String str, int id) {
            TextView textView = (TextView) getView(id);
            textView.setText(str);
        }

        public void setImage(String str, int id) {
            ImageView imageView = (ImageView) getView(id);
            Glide.with(imageView.getContext()).load(str).apply(new RequestOptions().override(imageView.getWidth(), imageView.getHeight()))
                    .into(imageView);
        }
    }

    public void setData(ArrayList<T> data) {
        this.data = data;
    }

    public abstract void bindData(@NonNull SuperVHolder holder, int position);

    public abstract int setLayout(int viewType);
}
