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

        // Item 사이즈 조절
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);

        // ViewHolder 생성
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.txtShopName.setText(this.items.get(position).shop.getName());                // 상호
        holder.txtFoodCategory.setText(this.items.get(position).shop.getFoodCategory());    // 음식 카테고리

        // 평점
        double pointAvg = 0;
        if (this.items.get(position).shop.getReviewPoint() > 0) {
            // 평점 표시 (소수점 한자리까지 표시 (반올림))
            pointAvg = (Math.round(((double) this.items.get(position).shop.getReviewPoint() / this.items.get(position).shop.getReviewCount()) * 10) / 10.0);
        }
        holder.txtReviewPoint.setText(String.valueOf(pointAvg));

        holder.txtReviewCount.setText(String.valueOf(this.items.get(position).shop.getReviewCount()));  // 리뷰수

        if (this.items.get(position).distance != -1) {
            // 거리 표시
            holder.txtDistance.setText(Utils.getDistanceStr(this.items.get(position).distance));
            holder.txtDistance.setVisibility(View.VISIBLE);
            holder.imgMap.setVisibility(View.VISIBLE);
        } else {
            // 거리정보가 없으면
            holder.txtDistance.setVisibility(View.GONE);
            holder.imgMap.setVisibility(View.GONE);
        }
        holder.txtAddress.setText(this.items.get(position).shop.getAddress());      // 주소

        // 이미지 체크
        if (TextUtils.isEmpty(this.items.get(position).shop.getImageFileName())) {
            // 이미지 없음 표시
            holder.imgPhoto.setImageResource(R.drawable.ic_camera_48_gray);
            this.items.get(position).download = true;
        } else {
            // 이미지 있음
            if (TextUtils.isEmpty(this.items.get(position).filePath)) {
                // 다운로드를 시도했는데 파일이 없으면 오류
                if (this.items.get(position).download) {
                    holder.imgPhoto.setImageResource(R.drawable.ic_alert_circle_24_gray);
                } else {
                    holder.imgPhoto.setImageResource(R.drawable.ic_progress_download_24_gray);

                    // 다운로드 하기
                    downloadFile(position, holder.imgPhoto);
                }
            } else {
                // 이미지 표시
                displayImage(holder.imgPhoto, this.items.get(position).filePath);
            }
        }
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    /* (대표사진) 파일 다운로드 */
    private void downloadFile(final int position, final ImageView imgFile) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        // 파일 확장자명 얻기
        String fileExtension = Utils.getFileExtension(this.items.get(position).shop.getImageFileName());

        // Storage 경로
        String storagePath = Constants.StorageFolderName.SHOP + "/" + this.items.get(position).id + "/" + this.items.get(position).shop.getImageFileName();
        StorageReference islandRef = storageRef.child(storagePath);

        try {
            // 캐시 활용 (임시저장)
            final File localFile = File.createTempFile(Constants.TEMP_FILE_PREFIX_NAME, "." + fileExtension);
            islandRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
                // 성공
                this.items.get(position).filePath = localFile.getAbsolutePath();
                this.items.get(position).download = true;

                // 이미지 표시 하기
                displayImage(imgFile, this.items.get(position).filePath);
                Log.d(TAG, "path: " + localFile.getAbsolutePath());
            }).addOnFailureListener(exception -> {
                // 실패
                imgFile.setImageResource(R.drawable.ic_alert_circle_24_gray);
                this.items.get(position).download = true;
                Log.d(TAG, "실패: " + exception.toString());
            });
        } catch (IOException e) {
            // 오류
            imgFile.setImageResource(R.drawable.ic_alert_circle_24_gray);
            this.items.get(position).download = true;
            Log.d(TAG, "오류: " + e.toString());
        }
    }

    /* 이미지 표시 */
    private void displayImage(ImageView imgFile, String filePath) {
        try {
            // 로드 되기전에 Activity 종료 될때 오류 발생을 막기 위해 (try catch 문 적용)
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
            // 가게 선택
            Bundle bundle = new Bundle();
            int position = getAdapterPosition();

            bundle.putInt("position", position);
            listener.onItemClick(bundle, view.getId());
        }
    }
}
