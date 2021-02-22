package com.example.matzip.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.matzip.R;
import com.example.matzip.listener.IAdapterOnClickListener;

import java.util.ArrayList;

public class FoodCategoryAdapter extends RecyclerView.Adapter<FoodCategoryAdapter.ViewHolder> {
    private static final String TAG = FoodCategoryAdapter.class.getSimpleName();

    private IAdapterOnClickListener listener;
    private ArrayList<String> items;

    public FoodCategoryAdapter(IAdapterOnClickListener listener, ArrayList<String> items) {
        this.listener = listener;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_category, null);


        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);

        // Create ViewHolder
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtCategory.setText(this.items.get(position));
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView txtCategory;

        private ViewHolder(View view) {
            super(view);

            this.txtCategory = view.findViewById(R.id.txtCategory);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // Select category
            Bundle bundle = new Bundle();
            int position = getAdapterPosition();

            bundle.putString("category", items.get(position));
            listener.onItemClick(bundle, view.getId());
        }
    }
}
