package com.example.matzip.adapter;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.matzip.R;
import com.example.matzip.entity.ShopItem;
import com.example.matzip.listener.IAdapterOnClickListener;
import com.example.matzip.util.Constants;
import com.example.matzip.util.Utils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ViewHolder> {
    private static final String TAG = ShopAdapter.class.getSimpleName();

    private IAdapterOnClickListener listener;
    private ArrayList<ShopItem> items;

    public ShopAdapter(IAdapterOnClickListener listener, ArrayList<ShopItem> items) {
        this.listener = listener;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop, null);


        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);


        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtShopName.setText(this.items.get(position).shop.getName());
        holder.txtFoodCategory.setText(this.items.get(position).shop.getFoodCategory());

        // Score
        double pointAvg = 0;
        if (this.items.get(position).shop.getReviewPoint() > 0) {
            // Display Score
            pointAvg = (Math.round(((double) this.items.get(position).shop.getReviewPoint() / this.items.get(position).shop.getReviewCount()) * 10) / 10.0);
        }
        holder.txtReviewPoint.setText(String.valueOf(pointAvg));

        holder.txtReviewCount.setText(String.valueOf(this.items.get(position).shop.getReviewCount()));

        if (this.items.get(position).distance != -1) {

            holder.txtDistance.setText(Utils.getDistanceStr(this.items.get(position).distance));
            holder.txtDistance.setVisibility(View.VISIBLE);
            holder.imgMap.setVisibility(View.VISIBLE);
        } else {

            holder.txtDistance.setVisibility(View.GONE);
            holder.imgMap.setVisibility(View.GONE);
        }
        holder.txtAddress.setText(this.items.get(position).shop.getAddress());

        // check image
        if (TextUtils.isEmpty(this.items.get(position).shop.getImageFileName())) {
            // There
            holder.imgPhoto.setImageResource(R.drawable.ic_camera_48_gray);
            this.items.get(position).download = true;
        } else {

            if (TextUtils.isEmpty(this.items.get(position).filePath)) {

                if (this.items.get(position).download) {
                    holder.imgPhoto.setImageResource(R.drawable.ic_alert_circle_24_gray);
                } else {
                    holder.imgPhoto.setImageResource(R.drawable.ic_progress_download_24_gray);


                    downloadFile(position, holder.imgPhoto);
                }
            } else {

                displayImage(holder.imgPhoto, this.items.get(position).filePath);
            }
        }
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }


    private void downloadFile(final int position, final ImageView imgFile) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();


        String fileExtension = Utils.getFileExtension(this.items.get(position).shop.getImageFileName());


        String storagePath = Constants.StorageFolderName.SHOP + "/" + this.items.get(position).id + "/" + this.items.get(position).shop.getImageFileName();
        StorageReference islandRef = storageRef.child(storagePath);

        try {

            final File localFile = File.createTempFile(Constants.TEMP_FILE_PREFIX_NAME, "." + fileExtension);
            islandRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {

                this.items.get(position).filePath = localFile.getAbsolutePath();
                this.items.get(position).download = true;


                displayImage(imgFile, this.items.get(position).filePath);
                Log.d(TAG, "path: " + localFile.getAbsolutePath());
            }).addOnFailureListener(exception -> {

                imgFile.setImageResource(R.drawable.ic_alert_circle_24_gray);
                this.items.get(position).download = true;
                Log.d(TAG, "Fail: " + exception.toString());
            });
        } catch (IOException e) {

            imgFile.setImageResource(R.drawable.ic_alert_circle_24_gray);
            this.items.get(position).download = true;
            Log.d(TAG, "Error: " + e.toString());
        }
    }


    private void displayImage(ImageView imgFile, String filePath) {
        try {

            Glide.with(imgFile.getContext())
                    .load("file://" + filePath)
                    .error(R.drawable.ic_alert_circle_24_gray)
                    .apply(new RequestOptions().centerCrop())
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(imgFile);
        } catch (Exception ignored) {}
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView imgPhoto, imgMap;
        TextView txtShopName, txtFoodCategory, txtReviewPoint, txtReviewCount, txtDistance, txtAddress;

        private ViewHolder(View view) {
            super(view);

            this.imgPhoto = view.findViewById(R.id.imgPhoto);
            this.imgMap = view.findViewById(R.id.imgMap);

            this.txtShopName = view.findViewById(R.id.txtShopName);
            this.txtFoodCategory = view.findViewById(R.id.txtFoodCategory);
            this.txtReviewPoint = view.findViewById(R.id.txtReviewPoint);
            this.txtReviewCount = view.findViewById(R.id.txtReviewCount);
            this.txtDistance = view.findViewById(R.id.txtDistance);
            this.txtAddress = view.findViewById(R.id.txtAddress);

            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

            Bundle bundle = new Bundle();
            int position = getAdapterPosition();

            bundle.putInt("position", position);
            listener.onItemClick(bundle, view.getId());
        }
    }
}
